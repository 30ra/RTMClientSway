package rtmsway;

import javax.script.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;

/** 固定コミットの未改変JSをNashornで実行し、同じ入力でJavaの全姿勢を比較する。 */
public final class VerifyReference {
    private static double maxError;
    private static long checks;
    private static void close(double a, double b, String where) {
        double err = Math.abs(a-b); maxError = Math.max(maxError,err); checks++;
        if (!Double.isFinite(a) || !Double.isFinite(b) || err > 1e-9)
            throw new AssertionError(where + ": Java="+a+", JS="+b+", error="+err);
    }
    public static void main(String[] args) throws Exception {
        String source = new String(Files.readAllBytes(Paths.get("tools/reference/RTMBodyMotion.js")),StandardCharsets.UTF_8);
        byte[] digest = java.security.MessageDigest.getInstance("SHA-256").digest(source.replace("\r\n","\n").getBytes(StandardCharsets.UTF_8));
        StringBuilder hash = new StringBuilder(); for(byte b:digest)hash.append(String.format("%02x",b&255));
        if(!hash.toString().equals("6eb4a61315e2b899ce9ae6bb16f1a3cc7a431efd6b4697da675406b60b264a5e"))
            throw new AssertionError("Reference JS fixture was modified");
        for (int dir : new int[]{1,-1}) for (boolean isCustom : new boolean[]{false,true}) run(source,dir,isCustom);
        System.out.println("PASS: original RTMBodyMotion JS vs Java; "+checks+" channel/pose comparisons; max error="+maxError+"; load disabled, identical supplied alpha");
    }
    private static void run(String source, int initialDir, boolean isCustom) throws Exception {
        ScriptEngine js = new ScriptEngineManager().getEngineByName("nashorn");
        if(js==null) throw new IllegalStateException("Java 8 Nashorn is required for reference comparison");
        js.eval("var GL11={}, NGTUtil={}, MCWrapper={}, NGTLog={}, RTMCore={}, TileEntityLargeRailBase={}, TileEntityLargeRailSwitchCore={};"+
            "var MOTION_TUNING={load:{enabled:false}}; var e={tick:0,x:0,z:0,yaw:0,speed:20,cant:0,brake:0,adjust:0,kind:0,"+
            "getTrainDirection:function(){return "+(initialDir<0?1:0)+";},getNotch:function(){return -this.brake;}};"+
            "var RTMBodyMotionAdapter={getEntityId:function(){return 23;},getEntityTick:function(e){return e.tick;},"+
            "getX:function(e){return e.x;},getY:function(e){return 0;},getZ:function(e){return e.z;},getYaw:function(e){return e.yaw;},"+
            "getSpeedMps:function(e){return e.speed;},unwrapEntity:function(e){return e;},readCantFromSamples:function(e){return e.cant;},"+
            "readBogie:function(e,i){return {index:i,x:e.x,z:e.z};},readDataMapDouble:function(e){return e.adjust;},"+
            "normalizePartialTick:function(a){return a;}};");
        Tuning tuning = new Tuning();
        if(isCustom) {
            tuning.values.put("curve.sluggishnessScale",1.6); tuning.values.put("curve.amplitudeScale",.4);
            tuning.values.put("straight.defaultScale",.3); tuning.values.put("stop.pitchDamping",.3);
            js.eval("MOTION_TUNING.curve={sluggishnessScale:1.6,amplitudeScale:.4}; MOTION_TUNING.straight={defaultScale:.3}; MOTION_TUNING.stop={pitchDamping:.3};");
        }
        js.eval(source);
        js.eval("motionResolveAlpha=function(s,a){return a;};"+
            "motionGetSwitchImpactDistancesSq=function(b){return {toe:b.index===0&&e.kind===1?0:1e9,frog:b.index===0&&e.kind===2?0:1e9};};"+
            "function sample(t,x,z,yaw,speed,cant,brake,adjust,kind,a){e.tick=t;e.x=x;e.z=z;e.yaw=yaw;e.speed=speed;e.cant=cant;e.brake=brake;e.adjust=adjust;e.kind=kind;"+
            "var p=motionGetPose(e,a),s=motionStates.e23;return Java.to([s.roll,s.sway,s.stopPitch,s.stopShift,s.shift,s.switchBounce,s.bounce,"+
            "s.rollVelocity,s.swayVelocity,s.stopPitchVelocity,s.stopShiftVelocity,s.shiftVelocity,s.switchBounceVelocity,s.bounceVelocity,"+
            "p.roll,p.sway,p.pitch,p.shift,p.bounce],'double[]');}");
        Invocable oracle=(Invocable)js;
        BodyMotion m=new BodyMotion(23,20,tuning);
        double initialAdjust = isCustom ? .8 : 0; m.initialAdjust(initialAdjust);
        double x=0,z=0,yaw=0,prevSpeed=20*initialDir;
        int dir=initialDir,tick=0; int[] cool={0,0}; boolean[] inside={false,false};
        oracle.invokeFunction("sample",0,0,0,0,20,0,0,initialAdjust,0,.5);
        for(int i=1;i<=4000;i++) {
            double speed = i<1800?20:i<2000?Math.max(0,20-(i-1800)*.1):i<2200?0:16;
            int brake=i>=1800&&i<2200?7:0;
            int step=i==2500?8:i%233==0?3:1;
            double dt=step*.05,dyaw=(i>100&&i<600?.07:i>=600&&i<900?-.11:0)*step;
            if(i==2650)dyaw=60;
            double nextYaw=wrap(yaw+dyaw);
            int movingDir=i>=2800?-initialDir:initialDir;
            double nx=x+Math.sin(nextYaw*Math.PI/180)*speed*dt*movingDir;
            double nz=z+Math.cos(nextYaw*Math.PI/180)*speed*dt*movingDir;
            if(i==2700)nx+=100;
            double move=Math.sqrt((nx-x)*(nx-x)+(nz-z)*(nz-z));
            boolean isBad=move>speed*dt*3+3||Math.abs(dyaw)>45;
            if(!isBad && step<=5) {
                double projected=(nx-x)*Math.sin(nextYaw*Math.PI/180)+(nz-z)*Math.cos(nextYaw*Math.PI/180);
                if(Math.abs(projected)>.0005)dir=projected>0?1:-1;
            }
            double lat=!isBad&&speed>.2?speed*dir*BodyMotion.limit(dyaw*Math.PI/180/dt,.8):0;
            double cant=i>400&&i<550?4:0,adjust=i>1000&&i<1300?.7:0;
            int kind=i%211==10?1:i%211==20?2:0;
            if(step>5) {m.reset(speed);inside[0]=inside[1]=false;}
            else {
                double rv=m.roll.v,sv=m.sway.v,bv=m.branchBounce.v;
                for(int j=0;j<2;j++)cool[j]=Math.max(0,cool[j]-step);
                if(!isBad&&speed>.1) for(int j=0;j<2;j++) {
                    boolean isNear=kind==j+1;
                    if(isNear&&!inside[j]&&cool[j]==0){m.impact(j==1,Math.abs(lat)>.01?(int)Math.signum(lat):-1,speed);cool[j]=j==0?10:12;}
                    inside[j]=isNear;
                }
                m.straightAdjust(adjust,dt);
                m.update(dt,speed*dir,prevSpeed,lat,cant,dir,brake,isBad);
                m.roll.prevV=rv;m.sway.prevV=sv;m.branchBounce.prevV=bv;
            }
            tick+=step;
            for(double alpha:new double[]{0,.25,.5,.75,1}) {
                double[] ref=(double[])oracle.invokeFunction("sample",tick,nx,nz,nextYaw,speed,cant,brake,adjust,kind,alpha);
                double[] out={m.roll.x,m.sway.x,m.pitch.x,m.shift.x,m.brakeShift.x,m.branchBounce.x,m.bounce.x,
                    m.roll.v,m.sway.v,m.pitch.v,m.shift.v,m.brakeShift.v,m.branchBounce.v,m.bounce.v,
                    m.roll.pose(alpha,m.dt),m.sway.pose(alpha,m.dt),BodyMotion.softLimit(m.pitch.rawPose(alpha,m.dt),tuning.get("stop.maxPitchDeg")),
                    BodyMotion.softLimit(m.shift.rawPose(alpha,m.dt)+m.brakeShift.rawPose(alpha,m.dt),tuning.get("stop.maxShiftM")),
                    BodyMotion.softLimit(m.bounce.rawPose(alpha,m.dt)+m.branchBounce.rawPose(alpha,m.dt),.012)};
                for(int j=0;j<out.length;j++)close(out[j],ref[j],"dir="+initialDir+" custom="+isCustom+" tick="+tick+" channel="+j+" alpha="+alpha);
            }
            x=nx;z=nz;yaw=nextYaw;prevSpeed=speed*dir;
        }
    }
    private static double wrap(double v){while(v>180)v-=360;while(v<-180)v+=360;return v;}
}
