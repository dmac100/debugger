package debugger.instrumentation;

public interface MethodExitHandler {
	public default void onFinally() {};
	public default void onReturn(int opcode) {};
	public default void onThrow() {};
}