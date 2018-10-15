package debugger.instrumentation;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import debugger.event.EventLogger;
import debugger.instrumentation.util.AsmUtil;

public class ExitMethodAdapter extends FinallyMethodVisitor {
	private int methodIndexVar = -1;
	
	public ExitMethodAdapter(MethodVisitor methodVisitor, int access, String name, String descriptor) {
		super(methodVisitor, access, name, descriptor);
	}
	
	@Override
	public void visitCode() {
		super.visitCode();
		
		methodIndexVar = newLocal(Type.INT_TYPE);
		invokeEventLogger("nextMethodIndex", "()I");
		storeLocal(methodIndexVar);
	}
	
	@Override
	protected void onReturn(int opcode) {
		if(opcode == Opcodes.RETURN) {
			super.visitInsn(Opcodes.ACONST_NULL);
		} else {
			if(AsmUtil.getOperandType(opcode).getSize() == 1) {
				super.visitInsn(Opcodes.DUP);
			} else {
				super.visitInsn(Opcodes.DUP2);
			}
			box(AsmUtil.getOperandType(opcode));
		}
		//loadLocal(methodIndexVar);
		super.visitInsn(Opcodes.ICONST_M1);
		invokeEventLogger("exitWithValue", "(Ljava/lang/Object;I)V");
	}
	
	@Override
	protected void onThrow() {
		super.visitInsn(Opcodes.DUP);
		//loadLocal(methodIndexVar);
		super.visitInsn(Opcodes.ICONST_M1);
		invokeEventLogger("exitWithException", "(Ljava/lang/Throwable;I)V");
	}
	
	private void invokeEventLogger(String method, String descriptor) {
		super.visitMethodInsn(Opcodes.INVOKESTATIC, AsmUtil.getAsmClassName(EventLogger.class), method, descriptor, false);
	}
}