package jp.ngt.rtm.rail;

/** 分岐判定試験用のAPI代替。検証クラスにだけコンパイルする。 */
public final class TileEntityLargeRailSwitchCore {
    private final Switch rail;
    public TileEntityLargeRailSwitchCore(Object point){rail=new Switch(point);}
    public Switch getSwitch(){return rail;}
    public static final class Switch {
        private final Object[] points;
        Switch(Object point){points=new Object[]{point};}
        public Object[] getPoints(){return points;}
    }
}
