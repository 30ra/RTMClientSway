import java.nio.file.*;
import java.util.jar.*;
import java.io.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
import rtmsway.SwayTransformer;

public final class VerifyTransformer {
    public static void main(String[] args) throws Exception {
        String name = "jp.ngt.rtm.entity.vehicle.RenderVehicleBase";
        byte[] bytes;
        try (JarFile jar = new JarFile(args[0]); InputStream in = jar.getInputStream(jar.getJarEntry(name.replace('.', '/') + ".class"))) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192]; int n;
            while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
            bytes = out.toByteArray();
        }
        SwayTransformer transformer = new SwayTransformer();
        byte[] patched = transformer.transform(name, name, bytes);
        ClassNode node = new ClassNode();
        new ClassReader(patched).accept(node, 0);
        int wrappers = 0, originals = 0;
        for (MethodNode m : node.methods) {
            new Analyzer<BasicValue>(new BasicVerifier()).analyze(node.name, m);
            if (m.name.equals("renderVehicleMain")) {
                wrappers++;
                if (m.tryCatchBlocks.size() != 1) throw new AssertionError("missing finally handler");
                int begin = 0, end = 0, frames = 0;
                for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext()) {
                    if (i instanceof FrameNode) frames++;
                    if (i instanceof MethodInsnNode) {
                        MethodInsnNode call = (MethodInsnNode)i;
                        if (call.owner.equals("rtmsway/SwayHook") && call.name.equals("begin")) begin++;
                        if (call.owner.equals("rtmsway/SwayHook") && call.name.equals("end")) end++;
                    }
                }
                if (begin != 1 || end != 2 || frames != 1) throw new AssertionError("bad hook/frame counts: begin=" + begin + " end=" + end + " frames=" + frames);
            }
            if (m.name.equals("rtmsway$originalRenderVehicleMain")) originals++;
        }
        if (wrappers != 1 || originals != 1) throw new AssertionError("wrong target count");
        if (transformer.transform("other", "other", bytes) != bytes) throw new AssertionError("unrelated class changed");
        System.out.println("PASS: installed KaizPatchX bytecode; every method analyzed; normal/exception GL cleanup; stack frame; unrelated classes unchanged");
    }
}
