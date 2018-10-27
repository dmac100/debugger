package debugger.instrumentation;

import java.util.LinkedHashMap;
import java.util.Map;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;

class LineNumberMethodNode extends MethodNode {
	private final MethodVisitor methodVisitor;
	private final Map<Label, Integer> lineNumbers = new LinkedHashMap<>();
	private final LineNumbersHandler lineNumbersHandler;

	public LineNumberMethodNode(MethodVisitor methodVisitor, int access, String name, String descriptor, String signature, String[] exceptions, LineNumbersHandler lineNumbersHandler) {
		super(Opcodes.ASM7, access, name, descriptor, signature, exceptions);

		this.methodVisitor = methodVisitor;
		this.lineNumbersHandler = lineNumbersHandler;
	}
	
	@Override
	public void visitLineNumber(int line, Label start) {
		super.visitLineNumber(line, start);
		
		lineNumbers.put(start, line);
	}

	@Override
	protected LabelNode getLabelNode(final Label label) {
		return new LabelNode(label);
	}

	@Override
	public void visitEnd() {
		super.visitEnd();

		lineNumbersHandler.setLineNumbers(lineNumbers);

		this.accept(methodVisitor);
	}
}