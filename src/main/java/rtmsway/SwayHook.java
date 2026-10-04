package rtmsway;

import org.lwjgl.opengl.GL11;
import java.lang.reflect.*;
import java.util.*;
import java.util.logging.Logger;

/** Client-only, local rendering state. Never writes entity fields or sends packets. */
public final class SwayHook {
    public static boolean isInstalled() { return true; }
    private static final Map<Object, State> states = new WeakHashMap<Object, State>();
    private static final Map<String, AccessibleObject> members = new HashMap<String, AccessibleObject>();
    private static boolean isFailed;
    private static Class<?> trainClass;
    public static void clear() { states.clear(); }
    public static void begin(Object entity, float frame) {
        GL11.glPushMatrix();
        if (!SwayMod.isOn || isFailed || entity == null) return;
        try {
            if (trainClass == null) trainClass = Class.forName("jp.ngt.rtm.entity.train.EntityTrainBase");
            if (!trainClass.isInstance(entity)) return;
            State s = states.get(entity);
            int tick = number(field(entity, "ticksExisted", "field_70173_aa")).intValue();
            if (s == null || s.tick != tick) {
                double raw = number(call(entity, "getSpeed")).doubleValue(), speed = Math.abs(raw * 72);
                Object b0 = call(entity, "getBogie", 0), b1 = call(entity, "getBogie", 1);
                if (s == null || tick < s.tick || tick - s.tick > 20) {
                    s = new State(); s.tick = tick; s.speed = speed; s.isMoving = speed > 1;
                    s.dir = raw < 0 ? -1 : 1; s.mask = switchMask(entity, b0, b1);
                    Object formation = call(entity, "getFormation");
                    if (formation != null) {
                        Object entry = call(formation, "getEntry", entity);
                        if (entry != null) s.dist = numField(entry, "entryId") * 20;
                    }
                    Object cfg = call(call(entity, "getModelSet"), "getConfig");
                    float[][] pos = (float[][])call(cfg, "getBogiePos");
                    if (pos != null && pos.length >= 2) s.half = Math.max(0.1, Math.abs(pos[0][2] - pos[1][2]) * 0.5);
                    states.put(entity, s);
                } else update(entity, s, tick, raw, speed, b0, b1, s.half);
            }
            double f = Math.max(0, Math.min(1, frame));
            double x = lerp(s.x0, s.x, f), y = SwayMod.isVertical ? lerp(s.y0, s.y, f) : 0;
            double pitch = lerp(s.p0, s.p, f), roll = lerp(s.r0, s.r, f);
            GL11.glTranslated(x * SwayMod.gain, y * SwayMod.gain, 0);
            GL11.glTranslated(0, SwayMod.pivot, 0);
            GL11.glRotated(pitch * SwayMod.gain, 1, 0, 0);
            GL11.glRotated(roll * SwayMod.gain, 0, 0, 1);
            GL11.glTranslated(0, -SwayMod.pivot, 0);
        } catch (Exception e) {
            isFailed = true;
            Logger.getLogger("RTMClientSway").log(java.util.logging.Level.SEVERE, "Sway disabled: incompatible RTM API", e);
        }
    }
    public static void end() { GL11.glPopMatrix(); }
    private static void update(Object e, State s, int tick, double raw, double speed, Object b0, Object b1, double half) throws Exception {
        int elapsed = Math.max(1, Math.min(20, tick - s.tick));
        s.dist += raw * elapsed;
        double rate = speed <= 1 ? 0 : Math.min(.35 + (speed - 1) / 60, 1);
        double yt = (wave(s.dist + half, 0) + wave(s.dist - half, 0)) * .007 * rate * SwayMod.run;
        double xt = (wave(s.dist + half, 8.7) + wave(s.dist - half, 8.7)) * .009 * rate * SwayMod.run;
        double trackRoll = wave(s.dist, 19.3) * .120 * rate * SwayMod.run;
        double angle = b0 == null ? 0 : wrap(numField(b0, "rotationYaw", "field_70177_z") - numField(e, "rotationYaw", "field_70177_z"));
        if (Math.abs(angle) < .05) angle = 0;
        double accel = 0, cr = 0;
        if (angle != 0) {
            double radius = half / Math.max(Math.sin(Math.abs(angle) * Math.PI / 180), .0001);
            accel = Math.pow(speed / 3.6, 2) / radius;
            double cant = b1 == null ? 0 : Math.abs((numField(b0, "rotationRoll") - numField(b1, "rotationRoll")) * .5);
            cr = clamp(-Math.signum(angle) * (accel - 9.80665 * Math.tan(cant * Math.PI / 180)) * .055, .16) * SwayMod.curve;
        }
        int mask = switchMask(e, b0, b1), entered = mask & ~s.mask;
        if (mask != 0 && angle != 0) s.side = Math.signum(angle);
        if (entered != 0 && speed > 1) s.pending |= entered;
        if (s.pending != 0 && s.side != 0 && speed > 1) {
            int hits = ((s.pending & 1) != 0 ? 1 : 0) + ((s.pending & 2) != 0 ? 1 : 0);
            s.xv += s.side * hits * (.0075 + .0225 * Math.min(speed / 70, 1)) * SwayMod.branch;
            s.pending = 0;
        }
        double target = mask != 0 && s.side != 0 ? s.side * Math.min(accel * .60, 2.10) * SwayMod.branch : 0;
        s.sr += (target - s.sr) * (mask != 0 ? .14 : .08);
        double rt = clamp(cr + s.sr + trackRoll, 2.5 * Math.max(1, Math.max(SwayMod.run, Math.max(SwayMod.branch, SwayMod.curve))));
        if (mask == 0) { s.side = 0; s.pending = 0; }
        s.mask = mask;
        double decel = (s.speed - speed) * 20 / elapsed, jerk = (decel - s.decel) * 20 / elapsed;
        if (decel > .05) { s.sd = Math.max(s.sd * .94, decel); s.sj = Math.max(s.sj * .90, Math.abs(jerk)); }
        else { s.sd *= .94; s.sj *= .90; }
        if (speed > 1) { s.isMoving = true; s.dir = raw < 0 ? -1 : 1; }
        else if (s.isMoving && speed < .5) {
            double stop = Math.pow(Math.min(s.sd / 4 + s.sj / 100, 1), 2);
            s.pv += s.dir * (.0015 + .022 * stop) * SwayMod.stop;
            s.yv -= (.00015 + .002 * stop) * SwayMod.stop;
            s.isMoving = false; s.sd = s.sj = 0;
        }
        s.speed = speed; s.decel = decel;
        s.p0 = s.p; s.pv = (s.pv - s.p * .10) * .72; s.p = clamp(s.p + s.pv, .035 * Math.max(1, SwayMod.stop));
        s.r0 = s.r; s.rv = (s.rv + (rt - s.r) * .14) * .80; s.r = clamp(s.r + s.rv, 2.5 * Math.max(1, Math.max(SwayMod.run, Math.max(SwayMod.branch, SwayMod.curve))));
        s.x0 = s.x; s.xv = (s.xv + (xt - s.x) * .12) * .76; s.x = clamp(s.x + s.xv, .045 * Math.max(1, Math.max(SwayMod.run, SwayMod.branch)));
        s.y0 = s.y; s.yv = (s.yv + (yt - s.y) * .16) * .74; s.y = clamp(s.y + s.yv, .030 * Math.max(1, Math.max(SwayMod.run, SwayMod.stop)));
        s.tick = tick;
    }
    private static int switchMask(Object entity, Object a, Object b) throws Exception {
        Object world = field(entity, "worldObj", "field_70170_p");
        int mask = 0;
        Class<?> rail = Class.forName("jp.ngt.rtm.rail.TileEntityLargeRailBase");
        Class<?> branch = Class.forName("jp.ngt.rtm.rail.TileEntityLargeRailSwitchCore");
        Object[] bogies = {a, b};
        for (int i = 0; i < 2; i++) if (bogies[i] != null) {
            Object bogie = bogies[i];
            Object r = invoke(rail, null, "getRailFromCoordinates", world, numField(bogie, "posX", "field_70165_t"), numField(bogie, "posY", "field_70163_u"), numField(bogie, "posZ", "field_70161_v"));
            if (r != null && branch.isInstance(call(r, "getRailCore"))) mask |= 1 << i;
        }
        return mask;
    }
    private static Number number(Object value) { return (Number)value; }
    private static double numField(Object obj, String... names) throws Exception { return number(field(obj, names)).doubleValue(); }
    private static Object field(Object obj, String... names) throws Exception {
        String key = obj.getClass().getName() + ":" + names[0];
        Field f = (Field)members.get(key);
        if (f == null) {
            for (String name : names) { try { f = obj.getClass().getField(name); break; } catch (NoSuchFieldException ignored) {} }
            if (f == null) throw new NoSuchFieldException(key);
            members.put(key, f);
        }
        return f.get(obj);
    }
    private static Object call(Object obj, String name, Object... args) throws Exception { return invoke(obj.getClass(), obj, name, args); }
    private static Object invoke(Class<?> type, Object obj, String name, Object... args) throws Exception {
        String key = type.getName() + ":" + name + ":" + args.length;
        Method m = (Method)members.get(key);
        if (m == null) {
            for (Method candidate : type.getMethods()) if (candidate.getName().equals(name) && candidate.getParameterTypes().length == args.length) { m = candidate; break; }
            if (m == null) throw new NoSuchMethodException(key);
            members.put(key, m);
        }
        return m.invoke(obj, args);
    }
    private static double clamp(double v, double max) { return Math.max(-max, Math.min(max, v)); }
    private static double wrap(double v) { while (v > 180) v -= 360; while (v < -180) v += 360; return v; }
    private static double lerp(double a, double b, double f) { return a + (b - a) * f; }
    private static double wave(double distance, double phase) {
        double p = (distance + phase) * Math.PI * 2;
        return Math.sin(p / 5.3) * .34 + Math.sin(p / 9.7) * .28 + Math.sin(p / 17.9) * .23 + Math.sin(p / 37.1) * .15;
    }
    private static final class State {
        int tick, mask, pending, dir;
        boolean isMoving;
        double dist, speed, decel, sd, sj, side, sr, p, p0, pv, r, r0, rv, x, x0, xv, y, y0, yv;
        double half = 6.8;
    }
}
