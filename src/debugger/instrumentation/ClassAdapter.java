package debugger.instrumentation;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ClassAdapter extends ClassVisitor {
	public ClassAdapter(ClassVisitor classVisitor) {
		super(Opcodes.ASM7, classVisitor);
	}
	
	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		MethodVisitor methodVisitor = cv.visitMethod(access, name, descriptor, signature, exceptions);
		if(methodVisitor == null) {
			return null;
		}
		
		methodVisitor = new MethodAdapter(access, name, descriptor, methodVisitor);
		
		return methodVisitor;
	}
}