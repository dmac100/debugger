package debugger.instrumentation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import debugger.event.EventLogger;
import debugger.instrumentation.util.AsmUtil;

public class EventLoggerMethodVisitor extends GeneratorAdapter implements MethodExitHandler {
	private final AsmUtil asmUtil;
	
	private final int access;
	private final String className;
	private final String methodName;
	private final String descriptor;
	
	private final Set<Label> exceptionHandlers = new HashSet<>();
	private int methodIndexVar = -1;

	public EventLoggerMethodVisitor(int access, String className, String methodName, String descriptor, MethodVisitor methodVisitor) {
		super(Opcodes.ASM7, methodVisitor, access, methodName, descriptor);
		this.asmUtil = new AsmUtil(methodVisitor);
		this.access = access;
		this.className = className;
		this.methodName = methodName;
		this.descriptor = descriptor;
	}

	@Override
	public void visitCode() {
		super.visitCode();
		
		methodIndexVar = newLocal(Type.INT_TYPE);
		invokeEventLogger("nextMethodIndex", "()I");
		storeLocal(methodIndexVar);
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
		Type[] argTypes = Type.getArgumentTypes(descriptor);
		
		switch(opcode) {
		case Opcodes.INVOKESPECIAL:
			visitInvokeMethod(owner, name, descriptor, argTypes, false, true);
			break;
		case Opcodes.INVOKEVIRTUAL:
			visitInvokeMethod(owner, name, descriptor, argTypes, false, false);
			break;
		case Opcodes.INVOKESTATIC:
			visitInvokeMethod(owner, name, descriptor, argTypes, true, false);
			break;
		}

		super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);

		visitMethodReturned(descriptor);
	}

	private void visitInvokeMethod(String owner, String name, String descriptor, Type[] argTypes, boolean isStatic, boolean isSpecial) {
		List<Integer> locals = createArrayFromArgs(argTypes);
		
		if(isStatic || isSpecial) {
			push(owner);
			swap();
		} else {
			swap();
			dupX1();
			swap();
		}

		push(name);
		swap();
		push(descriptor);
		swap();

		loadCurrentThread();
		loadLocal(methodIndexVar);

		if(isStatic) {
			invokeEventLogger("invokeStaticMethod", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;Ljava/lang/Thread;I)V");
		} else if(isSpecial) {
			invokeEventLogger("invokeSpecialMethod", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;Ljava/lang/Thread;I)V");
		} else {
			invokeEventLogger("invokeMethod", "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;Ljava/lang/Thread;I)V");
		}

		for(int i = 0; i < locals.size(); i++) {
			loadLocal(locals.get(i));
		}
	}

	private void visitMethodReturned(String descriptor) {
		Type returnType = Type.getReturnType(descriptor);
		if(returnType == Type.VOID_TYPE) {
			visitInsn(Opcodes.ACONST_NULL);
			loadCurrentThread();
			loadLocal(methodIndexVar);
		} else {
			asmUtil.duplicate(returnType);
			box(returnType);
			loadCurrentThread();
			loadLocal(methodIndexVar);
		}
		invokeEventLogger("returnedValue", "(Ljava/lang/Object;Ljava/lang/Thread;I)V");
	}

	private void invokeEventLogger(String method, String descriptor) {
		super.visitMethodInsn(Opcodes.INVOKESTATIC, AsmUtil.getAsmClassName(EventLogger.class), method, descriptor, false);
	}

	private List<Integer> createArrayFromArgs(Type[] argTypes) {
		List<Integer> locals = new ArrayList<>();
		for(Type type : argTypes) {
			locals.add(newLocal(type));
		}
		for(int i = locals.size() - 1; i >= 0; i--) {
			storeLocal(locals.get(i));
		}

		asmUtil.iconst(locals.size());
		visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");

		for(int i = 0; i < locals.size(); i++) {
			dup();
			push(i);
			Type type = argTypes[i];
			loadLocal(locals.get(i));
			box(type);
			visitInsn(Opcodes.AASTORE);
		}

		return locals;
	}

	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
		switch(opcode) {
		case Opcodes.PUTFIELD:
			// Duplicate [..., objectRef, value] at top of stack.
			asmUtil.duplicate(Type.getType(descriptor).getSize(), 1);
			box(Type.getType(descriptor));

			// Add name so that stack is [..., objectRef, name, value]
			push(name);
			swap();

			loadCurrentThread();
			loadLocal(methodIndexVar);

			invokeEventLogger("putField", "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Thread;I)V");
			break;
		}

		super.visitFieldInsn(opcode, owner, name, descriptor);
	}

	@Override
	public void visitInsn(int opcode) {
		switch(opcode) {
		case Opcodes.IRETURN:
		case Opcodes.LRETURN:
		case Opcodes.FRETURN:
		case Opcodes.DRETURN:
		case Opcodes.ARETURN:
			asmUtil.duplicate(getOperandType(opcode));
			box(getOperandType(opcode));
			loadCurrentThread();
			loadLocal(methodIndexVar);
			invokeEventLogger("returnValue", "(Ljava/lang/Object;Ljava/lang/Thread;I)V");
			break;
		case Opcodes.RETURN:
			visitInsn(Opcodes.ACONST_NULL);
			loadCurrentThread();
			loadLocal(methodIndexVar);
			invokeEventLogger("returnValue", "(Ljava/lang/Object;Ljava/lang/Thread;I)V");
			break;
		case Opcodes.ATHROW:
			dup();
			loadCurrentThread();
			loadLocal(methodIndexVar);
			invokeEventLogger("throwException", "(Ljava/lang/Throwable;Ljava/lang/Thread;I)V");
			break;
		case Opcodes.IASTORE:
		case Opcodes.FASTORE:
		case Opcodes.DASTORE:
		case Opcodes.LASTORE:
		case Opcodes.AASTORE:
			visitArrayStore(getOperandType(opcode));
			break;
		}

		super.visitInsn(opcode);
	}

	private void visitArrayStore(Type type) {
		int valueVariable = super.newLocal(type);
		int indexVariable = super.newLocal(Type.INT_TYPE);
		int arrayRefVariable = super.newLocal(Type.getType(Object.class));

		storeLocal(valueVariable);
		storeLocal(indexVariable);
		storeLocal(arrayRefVariable);

		loadLocal(arrayRefVariable);
		loadLocal(indexVariable);
		loadLocal(valueVariable);
		box(type);

		loadCurrentThread();
		loadLocal(methodIndexVar);

		invokeEventLogger("storeArray", "(Ljava/lang/Object;ILjava/lang/Object;Ljava/lang/Thread;I)V");

		loadLocal(arrayRefVariable);
		loadLocal(indexVariable);
		loadLocal(valueVariable);
	}

	@Override
	public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
		exceptionHandlers.add(handler);

		super.visitTryCatchBlock(start, end, handler, type);
	}

	@Override
	public void visitLabel(Label label) {
		super.visitLabel(label);

		if(exceptionHandlers.contains(label)) {
			dup();
			loadCurrentThread();
			loadLocal(methodIndexVar);
			invokeEventLogger("catchException", "(Ljava/lang/Throwable;Ljava/lang/Thread;I)V");
		}
	}

	@Override
	public void visitIincInsn(int var, int increment) {
		super.visitIincInsn(var, increment);

		push(var);
		visitVarInsn(Opcodes.ILOAD, var);
		box(Type.INT_TYPE);

		loadCurrentThread();
		loadLocal(methodIndexVar);

		invokeEventLogger("store", "(ILjava/lang/Object;Ljava/lang/Thread;I)V");
	}

	@Override
	public void visitVarInsn(int opcode, int var) {
		switch(opcode) {
		case Opcodes.ISTORE:
		case Opcodes.FSTORE:
		case Opcodes.ASTORE:
		case Opcodes.DSTORE:
		case Opcodes.LSTORE:
			asmUtil.duplicate(getOperandType(opcode));
			box(getOperandType(opcode));
			push(var);
			asmUtil.swap(1, 1);
			loadCurrentThread();
			loadLocal(methodIndexVar);
			invokeEventLogger("store", "(ILjava/lang/Object;Ljava/lang/Thread;I)V");
			break;
		}

		super.visitVarInsn(opcode, var);
	}

	private static Type getOperandType(int opcode) {
		switch(opcode) {
		case Opcodes.ISTORE:
			return Type.INT_TYPE;
		case Opcodes.FSTORE:
			return Type.FLOAT_TYPE;
		case Opcodes.DSTORE:
			return Type.DOUBLE_TYPE;
		case Opcodes.LSTORE:
			return Type.LONG_TYPE;
		case Opcodes.ASTORE:
			return Type.getType(Object.class);
		case Opcodes.IRETURN:
			return Type.INT_TYPE;
		case Opcodes.LRETURN:
			return Type.LONG_TYPE;
		case Opcodes.FRETURN:
			return Type.FLOAT_TYPE;
		case Opcodes.DRETURN:
			return Type.DOUBLE_TYPE;
		case Opcodes.ARETURN:
			return Type.getType(Object.class);
		case Opcodes.IASTORE:
			return Type.INT_TYPE;
		case Opcodes.FASTORE:
			return Type.FLOAT_TYPE;
		case Opcodes.DASTORE:
			return Type.DOUBLE_TYPE;
		case Opcodes.LASTORE:
			return Type.LONG_TYPE;
		case Opcodes.AASTORE:
			return Type.getType(Object.class);
		default:
			throw new IllegalArgumentException("Unknown opcode: " + opcode);
		}
	}
	
	@Override
	public void onEnter() {
		if((access & Opcodes.ACC_STATIC) == 0) {
			loadThis();
			push(className);
			push(methodName);
			push(descriptor);
			loadArgArray();
			loadCurrentThread();
			loadLocal(methodIndexVar);
			invokeEventLogger("enterVirtual", "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;Ljava/lang/Thread;I)V");
		} else {
			push(className);
			push(methodName);
			push(descriptor);
			loadArgArray();
			loadCurrentThread();
			loadLocal(methodIndexVar);
			invokeEventLogger("enterStatic", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;Ljava/lang/Thread;I)V");
		}
	}

	@Override
	public void onReturn(int opcode) {
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
		loadCurrentThread();
		loadLocal(methodIndexVar);
		invokeEventLogger("exitWithValue", "(Ljava/lang/Object;Ljava/lang/Thread;I)V");
	}

	@Override
	public void onThrow() {
		super.visitInsn(Opcodes.DUP);
		loadCurrentThread();
		loadLocal(methodIndexVar);
		invokeEventLogger("exitWithException", "(Ljava/lang/Throwable;Ljava/lang/Thread;I)V");
	}

	private void loadCurrentThread() {
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", false);
	}
}