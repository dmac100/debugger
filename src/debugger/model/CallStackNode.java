package debugger.model;

import java.util.ArrayList;
import java.util.List;

public class CallStackNode {
	private final List<CallStackNode> children = new ArrayList<>();
	private CallStackNode parentNode;
	private int methodIndex;
	private String className;
	private String methodName;
	private String descriptor;
	private List<Object> arguments;
	private Object returnValue;
	private Throwable exception;

	public CallStackNode getParentNode() {
		return parentNode;
	}

	public void setParentNode(CallStackNode parentNode) {
		this.parentNode = parentNode;
	}

	public int getMethodIndex() {
		return methodIndex;
	}

	public void setMethodIndex(int methodIndex) {
		this.methodIndex = methodIndex;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public String getMethodName() {
		return methodName;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	public String getDescriptor() {
		return descriptor;
	}

	public void setDescriptor(String descriptor) {
		this.descriptor = descriptor;
	}

	public List<Object> getArguments() {
		return arguments;
	}

	public void setArguments(List<Object> arguments) {
		this.arguments = arguments;
	}

	public Object getReturnValue() {
		return returnValue;
	}

	public void setReturnValue(Object returnValue) {
		this.returnValue = returnValue;
	}

	public Throwable getException() {
		return exception;
	}

	public void setException(Throwable exception) {
		this.exception = exception;
	}

	public List<CallStackNode> getChildren() {
		return children;
	}
	
	public CallStackNode addChild(CallStackNode node) {
		children.add(node);
		return node;
	}
	
	public boolean isInstrumented() {
		return methodIndex >= 0;
	}
	
	public String toString() {
		return className + "." + methodName + "(" + ToStringImpl.toString(arguments) + ")"
			+ (exception == null ? " - " + ToStringImpl.toString(returnValue) : "")
			+ (exception != null ? " - " + exception.getClass().getName().replace('.', '/') : "");
	}
}