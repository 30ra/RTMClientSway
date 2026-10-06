package rtmsway;


/** RTMBodyMotion (C-TREC & 月島重工) の設計を参考にした描画専用サスペンション。
 * 元の利用条件は THIRD_PARTY_NOTICES.md を参照。乗客荷重は含めない。
 */
final class BodyMotion {
    final Spring roll = new Spring(.75, .10, 3);
    final Spring sway = new Spring(.85, .10, .085);
    final Spring bounce = new Spring(2.10, .62, .012);
    final Spring branchBounce = new Spring(2.10, .1297, .012);
    final Spring pitch = new Spring(.72, .14, .35);
    final Spring shift = new Spring(.78, .28, .025);
    final Spring brakeShift = new Spring(1, .62, .040);
    private long seed;
    private double lateral, accel, decel, peak, reference, injectedRoll, injectedSway;
    private int cool, candidate, stable, curveDir, stageSign, stageTicks, exitTicks;
    private boolean isArmed, isCurve;
    double dt = .05;

    BodyMotion(long seed, double speed) {
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
        double input = speed >= 4 && deficit >= 5 ? limit(Math.signum(lat) * 9.80665 * deficit / 1067, 3) : 0;
        int sign = Math.abs(input) > .0001 ? (int)Math.signum(input) : 0;
        if (sign == 0) { candidate = 0; stable = 0; }
        else if (candidate == sign) stable += ticks;
        else { candidate = sign; stable = ticks; }
        if (stable < 2) input = 0;
        lateral += (input - lateral) * Math.min(1, dt * Math.PI * 2 * 1.6);
        double ratio = Math.signum(lateral) * response(lateral);
        double rawAccel = limit((speed - prevSpeed) / dt, 3.5);
        accel += (rawAccel - accel) * Math.min(1, dt * 5);
        double nextDecel = Math.max(0, -accel), jerk = (nextDecel - decel) / dt;
        cool = Math.max(0, cool - (int)Math.round(dt * 20));
        if (nextDecel > 1.15 && jerk > 2 && cool == 0) {
            pitch.v += dir * .0376 * stop;
            brakeShift.v += dir * .020 * stop;
            cool = 20;
        }
        if (!isArmed && speed > .25) { isArmed = true; peak = 0; }
        if (isArmed) peak = Math.max(peak, nextDecel);
        if (isArmed && speed < .06) {
            if (brake >= 5 && peak >= .75) {
                double[] factors = {0, 0, 0, 0, 0, .50, .75, 1.05, 1.45};
                double strength = Math.min(1, (peak - .75) / 1.5);
                pitch.v += dir * .098 * factors[Math.min(8, brake)] * (1 + strength * .15) * stop;
                shift.v += dir * .026 * factors[Math.min(8, brake)] * (1 + strength * .10) * stop;
            }
            isArmed = false; peak = 0;
        }
        decel = nextDecel;
        // 参考元と同じく、速度で重み付けした確率的入力をばねへ渡す。
        double gain = .70 * (1 - Math.exp(-Math.max(0, speed * 3.6 - 3) / 50)) * run;
        double root = Math.sqrt(dt) * gain;
        curveEvent(ticks, curve);
        if (gain > 0) {
            roll.v += noise(.12, .75, .10) * root * gaussian();
            sway.v += noise(.004, .85, .10) * root * gaussian();
            bounce.v += noise(.0015, 2.10, .62) * root * gaussian();
        }
        roll.step(ratio * 1.20 * curve, dt);
        sway.step(-ratio * .030 * curve, dt);
        bounce.step(0, dt); branchBounce.step(0, dt); pitch.step(0, dt); shift.step(0, dt); brakeShift.step(0, dt);
    }

    private double random() { seed = seed * 16807 % 2147483647; return seed / 2147483647.0; }
    private double gaussian() { return Math.sqrt(-2 * Math.log(Math.max(random(), 1e-12))) * Math.cos(2 * Math.PI * random()); }
    private static double response(double value) { return Math.tanh(Math.abs(value) * 1067 / (9.80665 * 50)); }
    private void stage(double from, double to, double gain) {
        double delta = response(to) - response(from);
        double r = peakVelocity(.40, .75, .10), x = peakVelocity(.012, .85, .10);
        double ri = limit(delta * r, Math.max(0, r * 2 - injectedRoll));
        double xi = limit(delta * x, Math.max(0, x * 2 - injectedSway));
        roll.v += curveDir * ri * gain; sway.v -= curveDir * xi * gain;
        injectedRoll += Math.abs(ri); injectedSway += Math.abs(xi);
    }
    private void exit(double factor, double gain) {
        double ratio = response(reference) * factor;
        roll.v -= curveDir * ratio * peakVelocity(.40, .75, .10) * gain;
        sway.v += curveDir * ratio * peakVelocity(.012, .85, .10) * gain;
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
            if (Math.abs(delta) >= .018) {
                int sign = (int)Math.signum(delta);
                if (stageSign == sign) stageTicks += ticks;
                else { stageSign = sign; stageTicks = ticks; }
                if (stageTicks >= 2) { stage(reference, magnitude, gain); reference = magnitude; stageSign = stageTicks = 0; }
            } else stageSign = stageTicks = 0;
            return;
        }
        if (dir == -curveDir && magnitude >= .008) { exit(0, gain); curveEvent(ticks, gain); return; }
        exitTicks += ticks;
        if (exitTicks >= 6) exit(.62, gain);
    }

    void impact(boolean isFrog, int dir, double speed, double gain) {
        double factor = .45 + .55 / (1 + Math.pow(speed * 3.6 / 60, 2));
        roll.v += dir * peakVelocity(isFrog ? .60 : .20, .75, .10) * factor * gain;
        sway.v -= dir * peakVelocity(isFrog ? .015 : .005, .85, .10) * factor * gain;
        branchBounce.v += (isFrog ? .128 : .038) * factor * gain;
    }

    static double limit(double v, double max) { return Math.max(-max, Math.min(max, v)); }
    private static double noise(double std, double hz, double damping) {
        double w = Math.PI * 2 * hz;
        return std * Math.sqrt(4 * damping * w * w * w);
    }
    private static double peakVelocity(double peak, double hz, double damping) {
        double w = Math.PI * 2 * hz, wd = w * Math.sqrt(1 - damping * damping);
        double t = Math.atan(wd / (damping * w)) / wd;
        return peak * wd * Math.exp(damping * w * t) / Math.sin(wd * t);
    }

    static final class Spring {
        double x, v, prev, prevV;
        final double hz, damping, max;
        Spring(double hz, double damping, double max) { this.hz = hz; this.damping = damping; this.max = max; }
        void save() { prev = x; prevV = v; }
        void step(double target, double dt) {
            double w = Math.PI * 2 * hz;
            int steps = Math.max(1, (int)Math.ceil(dt / .005));
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
        double absolute = Math.abs(value), knee = max * .72;
        if (absolute <= knee) return value;
        double room = max - knee;
        return Math.signum(value) * Math.min(max, knee + room * (1 - Math.exp(-(absolute - knee) / room)));
    }
}
