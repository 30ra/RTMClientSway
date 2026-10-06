package rtmsway;


/** RTMBodyMotion (C-TREC & 月島重工) の設計を参考にした描画専用サスペンション。
 * 元の利用条件は THIRD_PARTY_NOTICES.md を参照。乗客荷重は含めない。
 */
final class BodyMotion {
    final Tuning cfg;
    final Spring roll, sway;
    final Spring bounce = new Spring(2.10, .62, .012);
    final Spring branchBounce, pitch, shift, brakeShift;
    final double curveRollV, curveSwayV, rollNoise, swayNoise, bounceNoise;
    private long seed;
    private double lateral, accel, decel, peak, reference, injectedRoll, injectedSway;
    private int cool, candidate, stable, curveDir, stageSign, stageTicks, exitTicks;
    private boolean isArmed, isCurve;
    double dt = .05;
    private double straightScale;

    BodyMotion(long seed, double speed) {
        this(seed, speed, new Tuning());
    }
    BodyMotion(long seed, double speed, Tuning cfg) {
        this.cfg = cfg;
        straightScale = cfg.get("straight.defaultScale");
        roll = new Spring(cfg.rollHz(), cfg.get("curve.rollDamping"), cfg.get("curve.maxRollDeg"));
        sway = new Spring(cfg.swayHz(), cfg.get("curve.swayDamping"), cfg.get("curve.maxSwayM"));
        branchBounce = new Spring(cfg.get("turnout.bounceFrequencyHz"), cfg.get("turnout.bounceDamping"), .012);
        pitch = new Spring(cfg.get("stop.pitchFrequencyHz"), cfg.get("stop.pitchDamping"), .35);
        shift = new Spring(cfg.get("stop.shiftFrequencyHz"), cfg.get("stop.shiftDamping"), .025);
        brakeShift = new Spring(1, .62, cfg.get("stop.maxShiftM"));
        curveRollV = peakVelocity(cfg.get("curve.peakRollDeg") * cfg.get("curve.amplitudeScale"), roll.hz, roll.damping);
        curveSwayV = peakVelocity(cfg.get("curve.peakSwayM") * cfg.get("curve.amplitudeScale"), sway.hz, sway.damping);
        rollNoise = noise(cfg.get("straight.rollStdDeg"), roll.hz, roll.damping);
        swayNoise = noise(cfg.get("straight.swayStdM"), sway.hz, sway.damping);
        bounceNoise = noise(cfg.get("straight.bounceStdM"), bounce.hz, bounce.damping);
        this.seed = Math.abs(seed % 2147483646L) + 1;
        isArmed = speed > .25;
    }

    void update(double seconds, double speed, double prevSpeed, double lat,
                double cant, int dir, int brake, double run, double curve, double stop) {
        dt = seconds;
        roll.save(); sway.save(); bounce.save(); branchBounce.save(); pitch.save(); shift.save(); brakeShift.save();
        // カント不足だけを追加動揺にする。既存RTMのカント描画を二重に加えない。
        int ticks = (int)Math.round(dt * 20);
        double radius = Math.abs(lat) > .000001 ? speed * speed / Math.abs(lat) : 1e9;
        double deficit = Math.max(0, 1067 * Math.pow(speed * 3.6, 2) / (127 * radius)
            - 1067 * Math.tan(Math.abs(cant) * Math.PI / 180));
        double input = speed >= cfg.get("curve.minSpeedMps") && deficit >= cfg.get("curve.minCantDeficiencyMm") ? limit(Math.signum(lat) * 9.80665 * deficit / 1067, 3) : 0;
        int sign = Math.abs(input) > .0001 ? (int)Math.signum(input) : 0;
        if (sign == 0) { candidate = 0; stable = 0; }
        else if (candidate == sign) stable += ticks;
        else { candidate = sign; stable = ticks; }
        if (stable < 2) input = 0;
        lateral += (input - lateral) * Math.min(1, dt * Math.PI * 2 * cfg.get("curve.inputFilterHz"));
        double ratio = Math.signum(lateral) * response(lateral);
        double rawAccel = limit((speed - prevSpeed) / dt, 3.5);
        accel += (rawAccel - accel) * Math.min(1, dt * 5);
        double nextDecel = Math.max(0, -accel), jerk = (nextDecel - decel) / dt;
        cool = Math.max(0, cool - (int)Math.round(dt * 20));
        if (nextDecel > cfg.get("stop.emergencyDecelMps2") && jerk > cfg.get("stop.emergencyJerkMps3") && cool == 0) {
            pitch.v += dir * cfg.get("stop.emergencyPitchImpulse") * stop;
            brakeShift.v += dir * cfg.get("stop.emergencyShiftImpulseM") * stop;
            cool = 20;
        }
        if (!isArmed && speed > .25) { isArmed = true; peak = 0; }
        if (isArmed) peak = Math.max(peak, nextDecel);
        if (isArmed && speed < .06) {
            if (brake >= cfg.get("stop.minimumBrakeLevel") && peak >= cfg.get("stop.minimumDecelMps2")) {
                double strength = Math.min(1, (peak - cfg.get("stop.minimumDecelMps2")) / 1.5);
                pitch.v += dir * cfg.get("stop.pitchImpulse") * cfg.notch[Math.min(8, brake)] * (1 + strength * .15) * stop;
                shift.v += dir * cfg.get("stop.shiftImpulseM") * cfg.notch[Math.min(8, brake)] * (1 + strength * .10) * stop;
            }
            isArmed = false; peak = 0;
        }
        decel = nextDecel;
        // 参考元と同じく、速度で重み付けした確率的入力をばねへ渡す。
        double gain = Math.min(straightScale, cfg.get("straight.maxScale"))
            * (1 - Math.exp(-Math.max(0, speed * 3.6 - cfg.get("straight.minSpeedKmh")) / cfg.get("straight.referenceSpeedKmh"))) * run;
        double root = Math.sqrt(dt) * gain;
        curveEvent(ticks, curve);
        if (gain > 0) {
            roll.v += rollNoise * root * gaussian();
            sway.v += swayNoise * root * gaussian();
            bounce.v += bounceNoise * root * gaussian();
        }
        roll.step(ratio * cfg.get("curve.leanRollDeg") * cfg.get("curve.amplitudeScale") * curve, dt);
        sway.step(-ratio * cfg.get("curve.leanSwayM") * cfg.get("curve.amplitudeScale") * curve, dt);
        bounce.step(0, dt); branchBounce.step(0, dt); pitch.step(0, dt); shift.step(0, dt); brakeShift.step(0, dt);
    }
    void straightAdjust(double adjust, double seconds) {
        double target = Math.max(0, Math.min(cfg.get("straight.maxScale"), cfg.get("straight.defaultScale") + adjust));
        straightScale += (target - straightScale) * Math.min(1, seconds * 2);
    }

    private double random() { seed = seed * 16807 % 2147483647; return seed / 2147483647.0; }
    private double gaussian() { return Math.sqrt(-2 * Math.log(Math.max(random(), 1e-12))) * Math.cos(2 * Math.PI * random()); }
    private double response(double value) { return Math.tanh(Math.abs(value) * 1067 / (9.80665 * cfg.get("curve.saturationMm"))); }
    private void stage(double from, double to, double gain) {
        double delta = response(to) - response(from);
        double r = curveRollV, x = curveSwayV;
        double ri = limit(delta * r, Math.max(0, r * 2 - injectedRoll));
        double xi = limit(delta * x, Math.max(0, x * 2 - injectedSway));
        roll.v += curveDir * ri * gain; sway.v -= curveDir * xi * gain;
        injectedRoll += Math.abs(ri); injectedSway += Math.abs(xi);
    }
    private void exit(double factor, double gain) {
        double ratio = response(reference) * factor;
        roll.v -= curveDir * ratio * curveRollV * gain;
        sway.v += curveDir * ratio * curveSwayV * gain;
        isCurve = false; curveDir = stageSign = stageTicks = exitTicks = 0;
        reference = injectedRoll = injectedSway = 0;
    }
    private void curveEvent(int ticks, double gain) {
        int dir = Math.abs(lateral) > .0001 ? (int)Math.signum(lateral) : 0;
        double magnitude = Math.abs(lateral);
        if (!isCurve) {
            if (dir == 0 || magnitude < .008) return;
            isCurve = true; curveDir = dir; reference = magnitude;
            injectedRoll = injectedSway = 0; stageSign = stageTicks = exitTicks = 0;
            stage(0, magnitude, gain);
        }
        if (dir == curveDir && magnitude >= .004) {
            exitTicks = 0;
            double delta = magnitude - reference;
            if (Math.abs(delta) >= cfg.get("curve.stageThreshold")) {
                int sign = (int)Math.signum(delta);
                if (stageSign == sign) stageTicks += ticks;
                else { stageSign = sign; stageTicks = ticks; }
                if (stageTicks >= cfg.get("curve.stageHoldTicks")) { stage(reference, magnitude, gain); reference = magnitude; stageSign = stageTicks = 0; }
            } else stageSign = stageTicks = 0;
            return;
        }
        if (dir == -curveDir && magnitude >= .008) { exit(cfg.get("curve.reverseExitFactor"), gain); curveEvent(ticks, gain); return; }
        exitTicks += ticks;
        if (exitTicks >= cfg.get("curve.exitHoldTicks")) exit(cfg.get("curve.exitResponseFactor"), gain);
    }

    void impact(boolean isFrog, int dir, double speed, double gain) {
        double min = cfg.get("turnout.minSpeedFactor");
        double factor = min + (1 - min) / (1 + Math.pow(speed * 3.6 / cfg.get("turnout.speedFalloffKmh"), 2));
        String kind = isFrog ? "frog" : "toe";
        roll.v += dir * peakVelocity(cfg.get("turnout." + kind + "PeakRollDeg"), roll.hz, roll.damping) * factor * gain;
        sway.v -= dir * peakVelocity(cfg.get("turnout." + kind + "PeakSwayM"), sway.hz, sway.damping) * factor * gain;
        branchBounce.v += cfg.get("turnout." + kind + "BounceImpulse") * factor * gain;
    }

    static double limit(double v, double max) { return Math.max(-max, Math.min(max, v)); }
    private static double noise(double std, double hz, double damping) {
        double w = Math.PI * 2 * hz;
        return std * Math.sqrt(4 * Math.max(damping, .001) * w * w * w);
    }
    private static double peakVelocity(double peak, double hz, double damping) {
        double w = Math.PI * 2 * hz, zeta = Math.max(0, Math.min(.999, damping));
        double root = Math.sqrt(1 - zeta * zeta);
        double attenuation = zeta > 0 ? Math.exp(-zeta / root * Math.atan(root / zeta)) : 1;
        return peak * w / attenuation;
    }

    static final class Spring {
        double x, v, prev, prevV;
        final double hz, damping, max;
        Spring(double hz, double damping, double max) { this.hz = hz; this.damping = damping; this.max = max; }
        void save() { prev = x; prevV = v; }
        void step(double target, double dt) {
            double w = Math.PI * 2 * hz;
            double maxStep = Math.min(.005, .25 / (w * (1 + 2 * damping)));
            int steps = Math.max(1, (int)Math.ceil(dt / maxStep));
            double h = dt / steps;
            for (int i = 0; i < steps; i++) {
                v += ((target - x) * w * w - v * 2 * damping * w) * h;
                x += v * h;
            }
            if (Math.abs(x) > max) { x = limit(x, max); if (x * v > 0) v = 0; }
        }
        double rawPose(double f, double dt) {
            // 位置と速度が連続するHermite補間。描画Passでは状態を進めない。
            double t2 = f * f, t3 = t2 * f;
            double value = (2*t3 - 3*t2 + 1)*prev + (t3 - 2*t2 + f)*prevV*dt
                + (-2*t3 + 3*t2)*x + (t3 - t2)*v*dt;
            return value;
        }
        double pose(double f, double dt) { return softLimit(rawPose(f, dt), max); }
    }
    static double softLimit(double value, double max) {
        if (max <= 0) return 0;
        double absolute = Math.abs(value), knee = max * .72;
        if (absolute <= knee) return value;
        double room = max - knee;
        return Math.signum(value) * Math.min(max, knee + room * (1 - Math.exp(-(absolute - knee) / room)));
    }
}
