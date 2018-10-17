package debugger.instrumentation;

import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;

class FinallyMethodVisitor extends AdviceAdapter {
	private final MethodExitHandler methodExitHandler;
	
	private boolean inOriginalMethod = false;
	private Label exceptionEnd = null;
	private Label exceptionHandler = new Label();
	
	private Set<Label> existingExceptionStartLabels = new HashSet<>();
	
	public FinallyMethodVisitor(MethodVisitor methodVisitor, int access, String name, String descriptor, MethodExitHandler methodExitHandler) {
		super(Opcodes.ASM7, methodVisitor, access, name, descriptor);
		this.methodExitHandler = methodExitHandler;
	}
	
	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
		startPossibleTryBlock();
		super.visitFieldInsn(opcode, owner, name, descriptor);
	}
	
	@Override
	public void visitIincInsn(int var, int increment) {
		startPossibleTryBlock();
		super.visitIincInsn(var, increment);
	}
	
	@Override
	public void visitIntInsn(int opcode, int operand) {
		startPossibleTryBlock();
		super.visitIntInsn(opcode, operand);
	}
	
	@Override
	public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
		startPossibleTryBlock();
		super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
	}
	
	@Override
	public void visitLdcInsn(Object value) {
		startPossibleTryBlock();
		super.visitLdcInsn(value);
	}
	
	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
		startPossibleTryBlock();
		super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
	}
	
	@Override
	public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
		startPossibleTryBlock();
		super.visitMultiANewArrayInsn(descriptor, numDimensions);
	}
	
	@Override
	public void visitTypeInsn(int opcode, String type) {
		startPossibleTryBlock();
		super.visitTypeInsn(opcode, type);
	}
	
	@Override
	public void visitVarInsn(int opcode, int var) {
		startPossibleTryBlock();
		super.visitVarInsn(opcode, var);
	}
	
	@Override
	public void visitJumpInsn(int opcode, Label label) {
		startPossibleTryBlock();
		super.visitJumpInsn(opcode, label);
	}
	
	@Override
	public void onMethodEnter() {
		super.onMethodEnter();
		inOriginalMethod = true;
	}

	@Override
	public void visitMaxs(int maxStack, int maxLocals) {
		inOriginalMethod = false;
		
		super.visitLabel(exceptionHandler);
		onThrow();
		onFinally();
		super.visitInsn(Opcodes.ATHROW);
		
		super.visitMaxs(0, 0);
	}
	
	@Override
	public void visitInsn(int opcode) {
		startPossibleTryBlock();
		
		if(opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) {
			onReturn(opcode);
			onFinally();
			super.visitInsn(opcode);
			endTryBlock();
		} else if(opcode == Opcodes.ATHROW) {
			super.visitInsn(opcode);
			endTryBlock();
		} else {
			super.visitInsn(opcode);
		}
	}
	
	@Override
	public void visitLabel(Label label) {
		if(existingExceptionStartLabels.contains(label)) {
			if(exceptionEnd != null) {
				endTryBlock();
			}
		}
		super.visitLabel(label);
	}
	
	@Override
	public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
		existingExceptionStartLabels.add(start);
		super.visitTryCatchBlock(start, end, handler, type);
	}
	
	private void startPossibleTryBlock() {
		if(inOriginalMethod) {
			if(exceptionEnd == null) {
				Label exceptionStart = new Label();
				exceptionEnd = new Label();
				super.visitTryCatchBlock(exceptionStart, exceptionEnd, exceptionHandler, null);
				super.visitLabel(exceptionStart);
			}
		}
	}
	
	private void endTryBlock() {
		super.visitLabel(exceptionEnd);
		exceptionEnd = null;
	}
	
	protected void onFinally() {
		methodExitHandler.onFinally();
	}
	
	protected void onReturn(int opcode) {
		methodExitHandler.onReturn(opcode);
	}
	
	protected void onThrow() {
		methodExitHandler.onThrow();
	}
}