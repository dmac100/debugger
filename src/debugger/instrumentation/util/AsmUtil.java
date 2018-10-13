package debugger.instrumentation.util;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class AsmUtil {
	private final MethodVisitor mv;

	public AsmUtil(MethodVisitor mv) {
		this.mv = mv;
	}
	
	public void loadVariable(Type type, int variable) {
		switch(type.getDescriptor().charAt(0)) {
			case 'Z':
			case 'B':
			case 'C':
			case 'S':
			case 'I':
				mv.visitVarInsn(Opcodes.ILOAD, variable);
				break;
			case 'J':
				mv.visitVarInsn(Opcodes.LLOAD, variable);
				break;
			case 'F':
				mv.visitVarInsn(Opcodes.FLOAD, variable);
				break;
			case 'D':
				mv.visitVarInsn(Opcodes.DLOAD, variable);
				break;
			case 'L':
			case '[':
				mv.visitVarInsn(Opcodes.ALOAD, variable);
				break;
			default:
				throw new RuntimeException("Not implemented: " + type.getDescriptor());
		}
	}
	
	public void storeVariable(Type type, int variable) {
		switch(type.getDescriptor().charAt(0)) {
			case 'Z':
			case 'B':
			case 'C':
			case 'S':
			case 'I':
				mv.visitVarInsn(Opcodes.ISTORE, variable);
				break;
			case 'J':
				mv.visitVarInsn(Opcodes.LSTORE, variable);
				break;
			case 'F':
				mv.visitVarInsn(Opcodes.FSTORE, variable);
				break;
			case 'D':
				mv.visitVarInsn(Opcodes.DSTORE, variable);
				break;
			case 'L':
			case '[':
				mv.visitVarInsn(Opcodes.ASTORE, variable);
				break;
			default:
				throw new RuntimeException("Not implemented: " + type.getDescriptor());
		}
	}
	
	public void boxValue(Type type) {
		switch(type.getDescriptor().charAt(0)) {
			case 'Z':
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
				break;
			case 'B':
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
				break;
			case 'C':
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
				break;
			case 'S':
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
				break;
			case 'I':
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
				break;
			case 'J':
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
				break;
			case 'F':
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
				break;
			case 'D':
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
				break;
			case 'L':
				break;
			case '[':
				break;
			default:
				throw new RuntimeException("Not implemented: " + type.getDescriptor());
		}
	}
	
	public void iconst(final int intValue) {
		if(intValue >= -1 && intValue <= 5) {
			mv.visitInsn(Opcodes.ICONST_0 + intValue);
		} else if(intValue >= Byte.MIN_VALUE && intValue <= Byte.MAX_VALUE) {
			mv.visitIntInsn(Opcodes.BIPUSH, intValue);
		} else if(intValue >= Short.MIN_VALUE && intValue <= Short.MAX_VALUE) {
			mv.visitIntInsn(Opcodes.SIPUSH, intValue);
		} else {
			mv.visitLdcInsn(intValue);
		}
	}
	
	public void duplicate(Type type) {
		if(type.getSize() == 1) {
			mv.visitInsn(Opcodes.DUP);
		} else {
			mv.visitInsn(Opcodes.DUP2);
		}
	}
	
	public void swap(Type typeBelow) {
		if(typeBelow.getSize() == 1) {
			mv.visitInsn(Opcodes.SWAP);
		} else {
			mv.visitInsn(Opcodes.DUP_X2);
			mv.visitInsn(Opcodes.POP);
		}
	}
	
	public void duplicate(int topSize, int belowSize) {
		if(belowSize != 1) {
			throw new IllegalArgumentException("Not implemented");
		}
		
		if(topSize == 1) {
			mv.visitInsn(Opcodes.SWAP);
			mv.visitInsn(Opcodes.DUP_X1);
			mv.visitInsn(Opcodes.SWAP);
			mv.visitInsn(Opcodes.DUP_X1);
		} else {
			swap(2, 1);
			mv.visitInsn(Opcodes.DUP_X2);
			swap(1, 2);
			mv.visitInsn(Opcodes.DUP2_X1);
		}
	}
	
	public void swap(int topSize, int belowSize) {
	    if (topSize == 1) {
	        if (belowSize == 1) {
	            mv.visitInsn(Opcodes.SWAP);
	        } else {
	            mv.visitInsn(Opcodes.DUP_X2);
	            mv.visitInsn(Opcodes.POP);
	        }
	    } else {
	    	if (belowSize == 1) {
	            mv.visitInsn(Opcodes.DUP2_X1);
	        } else {
	            mv.visitInsn(Opcodes.DUP2_X2);
	        }
	        mv.visitInsn(Opcodes.POP2);
	    }
	}
	
	public static String getAsmClassName(Class<?> clazz) {
		return clazz.getCanonicalName().replace(".", "/");
	}
}