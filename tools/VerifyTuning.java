package rtmsway;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class VerifyTuning {
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    private static Tuning parse(String text) {
        Tuning t = new Tuning(); t.overlay(TuningParser.parse(text), new ArrayList<String>()); return t;
    }
    private static void reject(String text) {
        boolean isRejected = false;
        try { parse(text); } catch (IllegalArgumentException e) { isRejected = true; }
        check(isRejected, "Must reject: " + text);
    }
    public static void main(String[] args) throws Exception {
        Tuning partial = parse("\ufeff// test\nvar MOTION_TUNING={curve:{rollFrequencyHz:1.2,stageHoldTicks:3,},stop:{notchFactors:[0,0,0,0,0,1,2,3,4]},}; // eof");
        check(partial.get("curve.rollFrequencyHz") == 1.2 && partial.get("curve.leanRollDeg") == 1.2 && partial.notch[8] == 4, "partial/default/array");
        check(parse("var MOTION_TUNING={straight:{dataMapKey:'Hello//World'}};").dataMapKey.equals("Hello//World"), "string comments");
        reject("var MOTION_TUNING={curve:{rollFrequencyHz:0}};");
        reject("var MOTION_TUNING={curve:{stageHoldTicks:1.5}};");
        reject("var MOTION_TUNING={stop:{notchFactors:[1,2]}};");
        reject("var MOTION_TUNING={curve:{rollFrequencyHz:1e309}};");
        reject("var MOTION_TUNING={curve:{rollFrequencyHz:1,rollFrequencyHz:2}};");
        reject("var MOTION_TUNING={curve:{rollFrequencyHz:Java.type('java.lang.Runtime').getRuntime().exec('test')}};");
        reject("var MOTION_TUNING={}; print('not allowed');");
        reject("var MOTION_TUNING={}; /* incomplete");
        Tuning zero = parse("var MOTION_TUNING={curve:{maxRollDeg:0,maxSwayM:0,rollDamping:0,swayDamping:1},stop:{maxPitchDeg:0,maxShiftM:0}};");
        BodyMotion z = new BodyMotion(1, 20, zero);
        for (int i = 0; i < 100; i++) z.update(.05,20,20,.8,0,1,0,false);
        check(z.roll.pose(.5,.05)==0 && z.sway.pose(.5,.05)==0 && Double.isFinite(z.sway.v), "zero limits and critical damping");
        reject("var MOTION_TUNING={curve:{rollFrequencyHz:10,swayFrequencyHz:10,sluggishnessScale:0.05}};");
        Tuning high = parse("var MOTION_TUNING={curve:{rollFrequencyHz:10,swayFrequencyHz:10,sluggishnessScale:1,rollDamping:1,swayDamping:1}};");
        BodyMotion h = new BodyMotion(1,20,high);
        for (int i=0;i<500;i++) h.update(.05,20,20,.8,0,1,0,false);
        check(Double.isFinite(h.roll.x) && Double.isFinite(h.roll.v), "high-frequency stable integration");
        Tuning normal = new Tuning(), doubled = new Tuning();
        normal.values.put("straight.defaultScale",0.0); doubled.values.put("straight.defaultScale",0.0);
        doubled.values.put("curve.leanRollDeg",2.4);
        BodyMotion a = new BodyMotion(1,20,normal), b = new BodyMotion(1,20,doubled);
        for(int i=0;i<500;i++){ a.update(.05,20,20,.8,0,1,0,false); b.update(.05,20,20,.8,0,1,0,false); }
        check(b.roll.x > a.roll.x * 1.9, "imported parameters change calculation");
        if(args.length>0) {
            Tuning exported = new Tuning(); List<String> ignored=new ArrayList<String>();
            exported.overlay(TuningParser.parse(new String(Files.readAllBytes(Paths.get(args[0])),StandardCharsets.UTF_8)),ignored);
            check(ignored.equals(Arrays.asList("load","debug")), "only load/debug ignored in supplied export: "+ignored);
            for(Tuning.Spec s:Tuning.SPECS.values()) check(Math.abs(exported.get(s.path)-s.def)<1e-12, "supplied defaults: "+s.path);
            System.out.println("PASS: supplied Previewer export; "+Tuning.SPECS.size()+" numeric fields, notchFactors and dataMapKey; load/debug excluded");
        }
        System.out.println("PASS: parser, partial import, invalid/code rejection, zero/critical damping, high-frequency stability, parameter effects");
    }
}
