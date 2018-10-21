package debugger.instrumentation;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class InstrumentorClassVisitor extends ClassVisitor {
	private String className;

	public InstrumentorClassVisitor(ClassVisitor classVisitor) {
		super(Opcodes.ASM7, classVisitor);
	}
	
	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		super.visit(version, access, name, signature, superName, interfaces);
		this.className = name;
	}
	
	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		MethodVisitor methodVisitor = cv.visitMethod(access, name, descriptor, signature, exceptions);
		if(methodVisitor == null) {
			return null;
		}
		
		EventLoggerMethodVisitor eventLoggerMethodVisitor = new EventLoggerMethodVisitor(access, className, name, descriptor, methodVisitor);
		methodVisitor = eventLoggerMethodVisitor;
		
		methodVisitor = new FinallyMethodVisitor(methodVisitor, access, name, descriptor, eventLoggerMethodVisitor);
		
		return methodVisitor;
	}
}