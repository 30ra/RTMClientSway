package rtmsway;

/** ゲーム・GLに依存しない動揺数値テスト。 */
public final class VerifyMotion {
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    private static void update(BodyMotion m, double speed, double prev, double lat, double cant, int dir, int brake, double run) {
        m.update(.05, speed, prev, lat, cant, dir, brake, run, 1, 1);
    }
    public static void main(String[] args) {
        BodyMotion still = new BodyMotion(1, 0);
        for (int i = 0; i < 100; i++) update(still, 0, 0, 0, 0, 1, 8, 1);
        check(still.roll.x == 0 && still.pitch.x == 0 && still.bounce.x == 0, "initial stop must not shake");
        BodyMotion left = new BodyMotion(1, 20), right = new BodyMotion(1, 20), canted = new BodyMotion(1, 20);
        for (int i = 0; i < 500; i++) {
            update(left, 20, 20, .8, 0, 1, 0, 0);
            update(right, 20, 20, -.8, 0, 1, 0, 0);
            update(canted, 20, 20, .8, 6, 1, 0, 0);
        }
        check(Math.abs(left.roll.x + right.roll.x) < 1e-10, "curve direction symmetry");
        check(left.roll.x > .5 && left.sway.x < 0, "curve lean and offset direction");
        check(Math.abs(canted.roll.x) < 1e-8, "sufficient cant removes added lean");
        for (int i = 0; i < 3000; i++) update(left, 0, 0, 0, 0, 1, 0, 0);
        check(Math.abs(left.roll.x) < 1e-8, "curve rebound must settle");
        BodyMotion toe = new BodyMotion(2, 15), frog = new BodyMotion(2, 15);
        toe.impact(false, 1, 15, 1); frog.impact(true, 1, 15, 1);
        check(frog.branchBounce.v > toe.branchBounce.v && toe.branchBounce.v > 0, "frog/toe vertical impulses");
        double max = 0;
        for (int i = 0; i < 100; i++) { update(frog, 15, 15, 0, 0, 1, 0, 0); max = Math.max(max, Math.abs(frog.branchBounce.x)); }
        check(max > .001 && max <= .012, "turnout bounce bounded and nonzero");
        BodyMotion weak = stop(3, 1), strong = stop(7, 1), reverse = stop(7, -1);
        check(weak.pitch.x == 0, "weak notch must not create stop kick");
        check(strong.pitch.x > 0 && strong.shift.x > 0, "strong stop kick");
        check(Math.abs(strong.pitch.x + reverse.pitch.x) < 1e-10, "reverse stop direction");
        BodyMotion noise = new BodyMotion(5, 20);
        for (int i = 0; i < 10000; i++) {
            update(noise, 20, 20, i % 500 < 250 ? .5 : -.5, 0, 1, 0, 5);
            check(Double.isFinite(noise.roll.x) && Math.abs(noise.roll.x) <= 3, "finite/bounded long run");
        }
        BodyMotion.Spring spring = new BodyMotion.Spring(.75, .1, 3);
        spring.prev = .1; spring.x = .2; spring.prevV = .3; spring.v = -.2;
        check(Math.abs(spring.rawPose(0, .05) - .1) < 1e-12 && Math.abs(spring.rawPose(1, .05) - .2) < 1e-12, "Hermite endpoints");
        check(SwayHook.segmentDistance(0, 0, 10, 0, 5, 1) == 1, "swept path prevents skipped impacts");
        System.out.println("PASS: motion symmetry, cant, settling, turnout bounce, stop notches, reverse, bounds, interpolation, swept path");
    }
    private static BodyMotion stop(int brake, int dir) {
        BodyMotion m = new BodyMotion(1, 2);
        double prev = 2;
        for (int i = 1; i <= 40; i++) { double speed = Math.max(0, 2 - i * .05); update(m, speed, prev, 0, 0, dir, brake, 0); prev = speed; }
        return m;
    }
}
