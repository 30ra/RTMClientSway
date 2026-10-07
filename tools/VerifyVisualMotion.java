package rtmsway;

import java.lang.reflect.Method;

/** 動揺の方向・時間特性と分岐形状を検証する。 */
public final class VerifyVisualMotion {
    private static void check(boolean isOk,String message){if(!isOk)throw new AssertionError(message);}
    private static void step(Motion m,double dist,double speed,double prev,double lat,int dir,int brake){
        m.save();m.update(.05,dist,speed,prev,lat,0,dir,brake,false);
    }
    public static void main(String[] args)throws Exception{
        SwayMod.run=0;SwayMod.curve=1;SwayMod.branch=1;SwayMod.stop=1;
        Motion left=new Motion(3,20),right=new Motion(3,20);
        for(int i=0;i<400;i++){
            step(left,i,20,20,.5,1,0);step(right,i,20,20,-.5,1,0);
            check(Math.abs(left.roll.x+right.roll.x)<1e-10,"curve mirror");
        }
        check(left.roll.x>.5&&left.sway.x<-.01,"sustained outward lean");
        double curveMax=0;
        for(int i=0;i<300;i++){step(left,i,20,20,0,1,0);curveMax=Math.max(curveMax,Math.abs(left.roll.x));}
        check(curveMax>.1&&Math.abs(left.roll.x)<.01,"curve exit and settling");
        Motion toe=new Motion(2,15),frog=new Motion(2,15),opposite=new Motion(2,15);
        toe.impact(false,1,15);frog.impact(true,1,15);opposite.impact(true,-1,15);
        check(frog.roll.v>toe.roll.v&&toe.roll.v>0&&frog.sway.v<0,"frog stronger than toe and direction");
        check(frog.branchY.v>toe.branchY.v&&toe.branchY.v>0,"vertical branch impulse");
        double max=0;
        for(int i=0;i<400;i++){
            step(frog,i,15,15,0,1,0);step(opposite,i,15,15,0,-1,0);
            max=Math.max(max,Math.abs(frog.roll.x));
            check(Math.abs(frog.roll.x+opposite.roll.x)<1e-10,"turnout mirror");
        }
        check(max>.3&&Math.abs(frog.roll.x)<.01,"turnout rebound and decay");
        Motion weak=stop(3,1),strong=stop(7,1),reverse=stop(7,-1);
        check(weak.pitch.v==0&&strong.pitch.v>0&&reverse.pitch.v<0,"brake-strength gating and reverse stop");
        Motion.Spring a=new Motion.Spring(.75,.1,3),b=new Motion.Spring(.75,.1,3);
        a.v=b.v=1;a.step(.2,.15);for(int i=0;i<3;i++)b.step(.2,.05);
        check(Math.abs(a.x-b.x)<1e-12&&Math.abs(a.v-b.v)<1e-12,"elapsed time independent spring");
        check(a.raw(0,.05)==a.prev&&Math.abs(a.raw(1,.05)-a.x)<1e-12,"interpolation endpoints");
        SwayMod.run=1;SwayMod.curve=0;SwayMod.branch=0;SwayMod.stop=0;
        Motion low=new Motion(1,2),high=new Motion(1,25);double lowSum=0,highSum=0;
        for(int i=0;i<1200;i++){
            step(low,i*.1,2,2,0,1,0);step(high,i*1.25,25,25,0,1,0);
            lowSum+=low.roll.x*low.roll.x;highSum+=high.roll.x*high.roll.x;
            check(Double.isFinite(high.roll.x)&&Math.abs(high.roll.x)<=3&&Math.abs(high.sway.x)<=.085,"finite bounds");
        }
        check(highSum>lowSum,"running grows with speed");
        for(int i=0;i<400;i++)step(high,1500,0,0,0,1,0);
        check(Math.abs(high.roll.x)<.01,"running settles at rest");
        geometry();passage();
        System.out.println("PASS: motion directions, curve exit, turnout peaks/decay, stop notches, reverse, timing, interpolation, bounds and geometry cache");
    }
    private static Motion stop(int brake,int dir){
        Motion m=new Motion(4,5);
        for(int i=1;i<=100;i++)step(m,i,Math.max(0,5-i*.05),Math.max(0,5-(i-1)*.05),0,dir,brake);
        return m;
    }
    private static void geometry()throws Exception{
        check(SwayHook.segmentDistance(0,0,10,0,5,1)==1,"swept segment hit");
        check(SwayHook.segmentDistance(2,0,2,0,3,0)==1,"stationary segment");
        Method method=SwayHook.class.getDeclaredMethod("impactPoints",Object.class);method.setAccessible(true);
        Point p=new Point();double[][] first=(double[][])method.invoke(null,p);
        int calls=p.rmMain.calls+p.rmBranch.calls;
        check(first.length==3&&first[0][0]==0&&first[1][1]>1,"toe and frog locations");
        check(first==(double[][])method.invoke(null,p)&&calls==p.rmMain.calls+p.rmBranch.calls,"cached geometry");
    }
    private static void passage()throws Exception{
        SwayMod.branch=1;
        Class<?> type=Class.forName("rtmsway.SwayHook$State");
        java.lang.reflect.Constructor<?> constructor=type.getDeclaredConstructor();constructor.setAccessible(true);
        Object state=constructor.newInstance();
        java.lang.reflect.Field field=type.getDeclaredField("motion");field.setAccessible(true);
        Motion m=new Motion(2,15);field.set(state,m);
        Method method=SwayHook.class.getDeclaredMethod("impacts",Object.class,type,Object[].class,double.class,double.class,int.class,boolean.class);
        method.setAccessible(true);
        Point p=new Point();Bogie lead=new Bogie(p),trail=new Bogie(p);
        Object[] bogies={lead,trail};trail.posZ=-15;
        method.invoke(null,new Object(),state,bogies,15.0,.1,1,false);
        check(m.roll.v==0,"initial observation silent");
        lead.posZ=0;method.invoke(null,new Object(),state,bogies,15.0,.1,1,false);
        double first=m.roll.v;check(first>0,"lead toe hit");
        method.invoke(null,new Object(),state,bogies,15.0,.1,1,false);
        check(m.roll.v==first,"no repeated hit while inside");
        lead.posZ=5.3;method.invoke(null,new Object(),state,bogies,15.0,.1,1,false);
        check(m.roll.v>first*2,"frog has independent cooldown");
        double second=m.roll.v;trail.posZ=0;
        method.invoke(null,new Object(),state,bogies,15.0,.1,1,false);
        check(m.roll.v>second,"trailing bogie independent hit");
        double last=m.roll.v;lead.posZ=0;
        method.invoke(null,new Object(),state,bogies,0.0,.1,1,false);
        check(m.roll.v==last,"stationary suppresses hit");
        method.invoke(null,new Object(),state,bogies,15.0,.1,1,true);
        check(m.roll.v==last,"bad sample suppresses hit");
    }
    public static final class Bogie {
        public double posX=0,posY=1,posZ=-3;
        private final Object currentRailObj;
        Bogie(Point p){currentRailObj=new jp.ngt.rtm.rail.TileEntityLargeRailSwitchCore(p);}
    }
    public static final class Root{public double posX=0,posZ=0;}
    public static final class Dir{public int id=1;}
    public static final class Point{
        public Root rpRoot=new Root();public Dir branchDir=new Dir();
        public Rail rmMain=new Rail(0),rmBranch=new Rail(.2);
        public boolean mainDirIsPositive=true,branchDirIsPositive=true;
    }
    public static final class Rail{
        final double slope;int calls;Rail(double slope){this.slope=slope;}
        public double[] getRailPos(int split,int index){calls++;double z=index*20.0/split;return new double[]{z,slope*z};}
        public float getRailRotation(int split,int index){return (float)Math.toDegrees(Math.atan(slope));}
    }
}
