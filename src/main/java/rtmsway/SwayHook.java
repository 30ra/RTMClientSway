package rtmsway;

import org.lwjgl.opengl.GL11;
import java.lang.reflect.*;
import java.util.*;
import java.util.logging.Logger;

/** 車両ごとの動揺状態とレール形状をクライアント内で管理する。 */
public final class SwayHook {
    private static final Map<Object, State> states = new WeakHashMap<Object, State>();
    private static final Map<String, AccessibleObject> members = new HashMap<String, AccessibleObject>();
    private static final Map<Object, double[][]> points = new WeakHashMap<Object, double[][]>();
    private static Class<?> trainClass, switchClass;
    private static boolean isFailed;
    public static boolean isInstalled() { return true; }
    public static void clear() { states.clear(); points.clear(); isFailed = false; }

    public static void begin(Object entity, float frame) {
        GL11.glPushMatrix();
        if (!SwayMod.isOn || isFailed || entity == null || !Float.isFinite(frame)) return;
        try {
            if (trainClass == null) trainClass = Class.forName("jp.ngt.rtm.entity.train.EntityTrainBase");
            if (!trainClass.isInstance(entity)) return;
            int tick = number(field(entity, "ticksExisted", "field_70173_aa")).intValue();
            State s = states.get(entity);
            if (s == null || s.tick != tick) {
                double speed = Math.abs(num(call(entity, "getSpeed")) * 20);
                double x = pos(entity,0), z = pos(entity,2), yaw = numField(entity,"rotationYaw","field_70177_z");
                Object[] bogies = {call(entity,"getBogie",0),call(entity,"getBogie",1)};
                if (s == null) {
                    s = new State();
                    s.id = number(call(entity,"func_145782_y")).intValue();
                    s.motion = new Motion(s.id,speed);
                    s.dir = number(call(entity,"getTrainDirection")).intValue() == 1 ? -1 : 1;
                    prime(s,bogies);
                    states.put(entity,s);
                } else {
                    double dt = (tick-s.tick)*.05;
                    double move = Math.hypot(x-s.x,z-s.z), dyaw = wrap(yaw-s.yaw);
                    boolean isGap = dt <= 0 || dt > .25;
                    boolean isBad = isGap || move > speed*Math.max(0,dt)*3+3 || Math.abs(dyaw)>45;
                    if (isBad) {
                        s.motion.reset(speed); prime(s,bogies);
                    } else {
                        double projection=(x-s.x)*Math.sin(Math.toRadians(yaw))+(z-s.z)*Math.cos(Math.toRadians(yaw));
                        if(Math.abs(projection)>.0005)s.dir=projection>0?1:-1;
                        s.dist += move * s.dir;
                        double lat=speed>.20 ? speed*s.dir*Motion.limit(Math.toRadians(dyaw)/dt,.80) : 0;
                        double cant=((bogies[0]==null?0:numField(bogies[0],"rotationRoll"))
                            -(bogies[1]==null?0:numField(bogies[1],"rotationRoll")))*.5;
                        int brake=Math.max(0,Math.min(8,-number(call(entity,"getNotch")).intValue()));
                        s.motion.save();
                        impacts(entity,s,bogies,speed,lat,tick-s.tick,false);
                        s.motion.update(dt,s.dist,speed,s.speed,lat,cant,s.dir,brake,false);
                    }
                }
                s.tick=tick;s.x=x;s.z=z;s.yaw=yaw;s.speed=speed;
            }
            Motion m=s.motion;
            double f=Math.max(0,Math.min(1,frame)),dt=m.dt;
            double y=SwayMod.isVertical ? Motion.soft(m.bounce.raw(f,dt)+m.branchY.raw(f,dt),.012) : 0;
            GL11.glTranslated(m.sway.pose(f,dt)*SwayMod.gain,y*SwayMod.gain,m.shift.pose(f,dt)*SwayMod.gain);
            GL11.glTranslated(0,SwayMod.pivot,0);
            GL11.glRotated(m.pitch.pose(f,dt)*SwayMod.gain,1,0,0);
            GL11.glRotated(m.roll.pose(f,dt)*SwayMod.gain,0,0,1);
            GL11.glTranslated(0,-SwayMod.pivot,0);
        } catch(Exception e) {
            isFailed=true;
            Logger.getLogger("RTMClientSway").log(java.util.logging.Level.SEVERE,"Sway disabled: incompatible RTM API",e);
        }
    }
    public static void end(){GL11.glPopMatrix();}
    private static void prime(State s,Object[] bogies)throws Exception{
        for(int i=0;i<2;i++){
            s.isBogie[i]=false;s.core[i]=null;
            Arrays.fill(s.isInside[i],false);Arrays.fill(s.cool[i],0);
            if(bogies[i]!=null){s.bx[i]=pos(bogies[i],0);s.bz[i]=pos(bogies[i],2);}
        }
    }
    private static void impacts(Object entity, State s, Object[] bogies, double speed, double lat, int ticks, boolean isBad) throws Exception {
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) s.cool[i][j] = Math.max(0, s.cool[i][j] - ticks);
            Object bogie = bogies[i];
            if (bogie == null) { Arrays.fill(s.isInside[i], false); s.isBogie[i] = false; s.core[i] = null; continue; }
            double x = pos(bogie, 0), z = pos(bogie, 2);
            if (isBad || speed <= .10 || SwayMod.branch == 0) { s.bx[i] = x; s.bz[i] = z; continue; }
            boolean isFirst = !s.isBogie[i];
            if (isFirst) { s.bx[i] = x; s.bz[i] = z; s.isBogie[i] = true; }
            double[] distance = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY};
            Object core = railCore(entity, bogie);
            Object previous = s.core[i];
            s.core[i] = core;
            Object[] cores = previous == core ? new Object[]{core} : new Object[]{core, previous};
            for (Object active : cores) if (active != null) {
                Object sw = call(active, "getSwitch");
                if (sw != null) for (Object point : (Object[])call(sw, "getPoints")) if (point != null && field(point, "rmBranch") != null) {
                    double[][] ps;
                    try { ps = impactPoints(point); }
                    catch (Exception e) {
                        // 特殊形状ではトング位置を使って通過を判定する。
                        Object root = field(point, "rpRoot");
                        if (root == null) continue;
                        ps = new double[][]{{numField(root, "posX"), numField(root, "posZ")}};
                        if (points.size() >= 512) points.clear();
                        points.put(point, ps);
                    }
                    for (int j = 0; j < ps.length; j++) {
                        int kind = j == 0 ? 0 : 1;
                        distance[kind] = Math.min(distance[kind], segmentDistance(s.bx[i], s.bz[i], x, z, ps[j][0], ps[j][1]));
                    }
                }
            }
            for (int j = 0; j < 2; j++) {
                boolean isNear = distance[j] <= 1.25 * 1.25;
                if (!isFirst && speed > .10 && isNear && !s.isInside[i][j] && s.cool[i][j] == 0) {
                    int dir = Math.abs(lat) > .01 ? (int)Math.signum(lat) : ((s.id + i) & 1) == 0 ? 1 : -1;
                    s.motion.impact(j == 1, dir, speed);
                    s.cool[i][j] = j == 0 ? 10 : 12;
                }
                s.isInside[i][j] = isNear;
            }
            s.bx[i] = x; s.bz[i] = z;
        }
    }

    private static Object railCore(Object entity, Object bogie) throws Exception {
        if (switchClass == null) switchClass = Class.forName("jp.ngt.rtm.rail.TileEntityLargeRailSwitchCore");
        // 台車の現在レールを優先し、未取得の場合は直下のレールを照会する。
        String key = bogie.getClass().getName() + ":currentRailObj";
        if (!members.containsKey(key)) {
            Field found = null;
            for (Class<?> c = bogie.getClass(); c != null; c = c.getSuperclass()) {
                try { found = c.getDeclaredField("currentRailObj"); found.setAccessible(true); break; }
                catch (NoSuchFieldException ignored) {}
            }
            members.put(key, found);
        }
        Field f = (Field)members.get(key);
        Object current = f == null ? null : f.get(bogie);
        if (current != null) return switchClass.isInstance(current) ? current : null;
        Object world = field(entity, "worldObj", "field_70170_p");
        if (world == null) return null;
        Class<?> rail = Class.forName("jp.ngt.rtm.rail.TileEntityLargeRailBase");
        Object r = invoke(rail, null, "getRailFromCoordinates", world, pos(bogie, 0), pos(bogie, 1), pos(bogie, 2));
        Object core = r == null ? null : call(r, "getRailCore");
        return switchClass.isInstance(core) ? core : null;
    }

    // RTMBodyMotionの軌間オフセット付き交点近似。Point単位にキャッシュし毎Tickの二重走査を避ける。
    private static double[][] impactPoints(Object point) throws Exception {
        double[][] cached = points.get(point);
        if (cached != null) return cached;
        Object root = field(point, "rpRoot"), main = field(point, "rmMain"), branch = field(point, "rmBranch");
        int id = number(field(field(point, "branchDir"), "id")).intValue();
        boolean isMain = (Boolean)field(point, "mainDirIsPositive"), isBranch = (Boolean)field(point, "branchDirIsPositive");
        double mOffset = .5335 * ((isMain && id == 1) || (!isMain && id == -1) ? 1 : -1);
        double bOffset = .5335 * ((isBranch && id == -1) || (!isBranch && id == 1) ? 1 : -1);
        double[][] m = sample(main, mOffset), b = sample(branch, bOffset);
        int mi = 0, bi = 0; double best = 1e9;
        for (int i = 0; i < m.length; i++) for (int j = 0; j < b.length; j++) {
            double dx = m[i][0] - b[j][0], dz = m[i][1] - b[j][1];
            double d = dx * dx + dz * dz;
            if (d < best) { best = d; mi = i; bi = j; }
        }
        double[] mp = (double[])call(main, "getRailPos", 96, mi), bp = (double[])call(branch, "getRailPos", 96, bi);
        cached = new double[][] {{numField(root, "posX"), numField(root, "posZ")}, {mp[1], mp[0]}, {bp[1], bp[0]}};
        if (points.size() >= 512) points.clear();
        points.put(point, cached);
        return cached;
    }
    private static double[][] sample(Object rail, double offset) throws Exception {
        double[][] result = new double[97][2];
        for (int i = 0; i <= 96; i++) {
            double[] p = (double[])call(rail, "getRailPos", 96, i);
            double yaw = Math.toRadians(num(call(rail, "getRailRotation", 96, i)));
            result[i][0] = p[1] + offset * Math.cos(yaw); result[i][1] = p[0] - offset * Math.sin(yaw);
        }
        return result;
    }
    static double segmentDistance(double x0, double z0, double x1, double z1, double px, double pz) {
        double dx = x1 - x0, dz = z1 - z0, length = dx*dx + dz*dz;
        double t = length < 1e-7 ? 1 : Math.max(0, Math.min(1, ((px-x0)*dx + (pz-z0)*dz) / length));
        return Math.pow(px - x0 - dx*t, 2) + Math.pow(pz - z0 - dz*t, 2);
    }
    private static Number number(Object value) { return (Number)value; }
    private static double num(Object value) {
        double v = number(value).doubleValue();
        if (!Double.isFinite(v)) throw new IllegalArgumentException("Non-finite RTM motion sample");
        return v;
    }
    private static double numField(Object obj, String... names) throws Exception { return num(field(obj, names)); }
    private static double pos(Object obj, int axis) throws Exception {
        return axis == 0 ? numField(obj, "posX", "field_70165_t") : axis == 1 ? numField(obj, "posY", "field_70163_u") : numField(obj, "posZ", "field_70161_v");
    }
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
            for (Method candidate : type.getMethods()) {
                boolean isName = candidate.getName().equals(name) || (name.equals("func_145782_y") && candidate.getName().equals("getEntityId"));
                if (isName && candidate.getParameterTypes().length == args.length) { m = candidate; break; }
            }
            if (m == null) throw new NoSuchMethodException(key);
            members.put(key, m);
        }
        return m.invoke(obj, args);
    }
    private static double wrap(double v) { while (v > 180) v -= 360; while (v < -180) v += 360; return v; }
    private static final class State {
        int tick, dir, id;
        double x, z, yaw, speed, dist;
        Motion motion;
        final double[] bx = new double[2], bz = new double[2];
        final Object[] core = new Object[2];
        final boolean[] isBogie = new boolean[2];
        final boolean[][] isInside = new boolean[2][2];
        final int[][] cool = new int[2][2];
    }
}
