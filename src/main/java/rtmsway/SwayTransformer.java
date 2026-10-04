package rtmsway;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Wrap the complete body render, including light passes and rollsigns. Bogies are separate entities. */
public final class SwayTransformer implements IClassTransformer {
    public byte[] transform(String name, String mappedName, byte[] bytes) {
        if (bytes == null || !"jp.ngt.rtm.entity.vehicle.RenderVehicleBase".equals(mappedName)) return bytes;
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        MethodNode original = null;
        for (MethodNode m : node.methods) {
            if (m.name.equals("renderVehicleMain") && m.desc.equals("(Ljp/ngt/rtm/entity/vehicle/EntityVehicleBase;Ljp/ngt/rtm/modelpack/modelset/ModelSetVehicleBaseClient;F)V")) original = m;
        }
        if (original == null) throw new IllegalStateException("RTMClientSway: incompatible RenderVehicleBase; renderVehicleMain not found");
        String oldName = original.name;
        original.name = "rtmsway$originalRenderVehicleMain";
        MethodNode wrapper = new MethodNode(original.access, oldName, original.desc, original.signature, null);
        Label start = new Label(), end = new Label(), handler = new Label();
        wrapper.visitCode();
        wrapper.visitVarInsn(Opcodes.ALOAD, 1);
        wrapper.visitVarInsn(Opcodes.FLOAD, 3);
        wrapper.visitMethodInsn(Opcodes.INVOKESTATIC, "rtmsway/SwayHook", "begin", "(Ljava/lang/Object;F)V", false);
        wrapper.visitLabel(start);
        wrapper.visitVarInsn(Opcodes.ALOAD, 0);
        wrapper.visitVarInsn(Opcodes.ALOAD, 1);
        wrapper.visitVarInsn(Opcodes.ALOAD, 2);
        wrapper.visitVarInsn(Opcodes.FLOAD, 3);
        wrapper.visitMethodInsn(Opcodes.INVOKEVIRTUAL, node.name, original.name, original.desc, false);
        wrapper.visitLabel(end);
        wrapper.visitMethodInsn(Opcodes.INVOKESTATIC, "rtmsway/SwayHook", "end", "()V", false);
        wrapper.visitInsn(Opcodes.RETURN);
        wrapper.visitLabel(handler);
        wrapper.visitVarInsn(Opcodes.ASTORE, 4);
        wrapper.visitMethodInsn(Opcodes.INVOKESTATIC, "rtmsway/SwayHook", "end", "()V", false);
        wrapper.visitVarInsn(Opcodes.ALOAD, 4);
        wrapper.visitInsn(Opcodes.ATHROW);
        wrapper.visitTryCatchBlock(start, end, handler, null);
        wrapper.visitMaxs(4, 5);
        wrapper.visitEnd();
        node.methods.add(wrapper);
        // The added handler needs a frame; preserve existing frames without loading RTM classes here.
        AbstractInsnNode label = wrapper.tryCatchBlocks.get(0).handler;
        Object[] locals = {node.name, "jp/ngt/rtm/entity/vehicle/EntityVehicleBase", "jp/ngt/rtm/modelpack/modelset/ModelSetVehicleBaseClient", Opcodes.FLOAT};
        wrapper.instructions.insert(label, new FrameNode(Opcodes.F_FULL, 4, locals, 1, new Object[]{"java/lang/Throwable"}));
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }
}
