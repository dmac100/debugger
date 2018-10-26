package debugger.event;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Events {
	private static final Map<Object, Integer> objects = new ConcurrentHashMap<>();
	private static final Map<Class<?>, Integer> classes = new ConcurrentHashMap<>();
	
	public static void clear() {
		objects.clear();
		classes.clear();
	}
	
	public static String getObjectName(Object object) {
		if(object == null) {
			return "null";
		}
		
		Integer id = objects.get(object);
		if(id == null) {
			id = classes.merge(object.getClass(), 1, Integer::sum);
			objects.put(object, id);
		}
		return object.getClass().getSimpleName() + "-" + id;
	}
	
	public static abstract class Event {
		public Thread thread;
		public final int methodIndex;
		
		public Event(Thread thread, int methodIndex) {
			this.thread = thread;
			this.methodIndex = methodIndex;
		}
		
		public Thread getThread() {
			return thread;
		}
		
		public int getMethodIndex() {
			return methodIndex;
		}
	}
	
	public static class PutFieldEvent extends Event {
		public final Object object;
		public final String name;
		public final Object value;
		
		public PutFieldEvent(Object object, String name, Object value, Thread thread, int methodIndex) {
			super(thread, methodIndex);
			this.object = object;
			this.name = name;
			this.value = value;
		}
		
		public String toString() {
			return "PUT FIELD: " + getObjectName(object) + ", " + name + ", " + value;
		}
	}
	
	public static class StoreEvent extends Event {
		public final int varIndex;
		public final Object value;
		
		public StoreEvent(int varIndex, Object value, Thread thread, int methodIndex) {
			super(thread, methodIndex);
			this.varIndex = varIndex;
			this.value = value;
		}
		
		public String toString() {
			return "STORE: " + varIndex + ", " + value;
		}
	}
	
	public static class StoreArrayEvent extends Event {
		public final Object array;
		public final int index;
		public final Object value;
		
		public StoreArrayEvent(Object array, int index, Object value, Thread thread, int methodIndex) {
			super(thread, methodIndex);
			this.array = array;
			this.index = index;
			this.value = value;
		}
		
		public String toString() {
			return "STORE ARRAY: " + getObjectName(array) + ", " + index + ", " + value;
		}
	}

	public static class InvokeMethodEvent extends Event {
		public final Object object;
		public final String name;
		public final String descriptor;
		public final Object[] args;
		
		public InvokeMethodEvent(Object object, String name, String descriptor, Object[] args, Thread thread, int methodIndex) {
			super(thread, methodIndex);
			this.object = object;
			this.name = name;
			this.descriptor = descriptor;
			this.args = args;
		}
		
		public String toString() {
			return "INVOKE: " + getObjectName(object) + ", " + name + ", " + descriptor + ", " + Arrays.toString(args);
		}
	}
	
	public static class InvokeSpecialMethodEvent extends Event {
		public final String className;
		public final String name;
		public final String descriptor;
		public final Object[] args;
		
		public InvokeSpecialMethodEvent(String className, String name, String descriptor, Object[] args, Thread thread, int methodIndex) {
			super(thread, methodIndex);
			this.className = className;
			this.name = name;
			this.descriptor = descriptor;
			this.args = args;
		}
		
		public String toString() {
			return "INVOKE SPECIAL: " + className + ", " + name + ", " + descriptor + ", " + Arrays.toString(args);
		}
	}
	
	public static class InvokeStaticMethodEvent extends Event {
		public final String className;
		public final String name;
		public final String descriptor;
		public final Object[] args;
		
		public InvokeStaticMethodEvent(String className, String name, String descriptor, Object[] args, Thread thread, int methodIndex) {
			super(thread, methodIndex);
			this.className = className;
			this.name = name;
			this.descriptor = descriptor;
			this.args = args;
		}
		
		public String toString() {
			return "INVOKE STATIC: " + className + ", " + name + ", " + descriptor + ", " + Arrays.toString(args);
		}
	}
	
	public static class ReturnValueEvent extends Event {
		public final Object value;
		
		public ReturnValueEvent(Object value, Thread thread, int methodIndex) {
			super(thread, methodIndex);
			this.value = value;
		}
		
		public String toString() {
			return "RETURN: " + value;
		}
	}
	
	public static class ReturnedValueEvent extends Event {
		public final Object value;
		
		public ReturnedValueEvent(Object value, Thread thread, int methodIndex) {
			super(thread, methodIndex);
			this.value = value;
		}
		
		public String toString() {
			return "RETURNED: " + value;
		}
	}
	
	public static class ThrowExceptionEvent extends Event {
		public final Throwable throwable;
		
		public ThrowExceptionEvent(Throwable throwable, Thread thread, int methodIndex) {
			super(thread, methodIndex);
			this.throwable = throwable;
		}
		
		public String toString() {
			return "THROW: " + throwable.getClass().getName() + ", " + throwable.getMessage();
		}
	}
	
	public static class CatchExceptionEvent extends Event {
		public final Throwable throwable;
		
		public CatchExceptionEvent(Throwable throwable, Thread thread, int methodIndex) {
			super(thread, methodIndex);
			this.throwable = throwable;
		}
		
		public String toString() {
			return "CATCH: " + throwable.getClass().getName() + ", " + throwable.getMessage();
		}
	}
	
	public static class SetThisEvent extends Event {
		private final Object object;

		public SetThisEvent(Object object, Thread thread, int methodIndex) {
			super(thread, methodIndex);
			this.object = object;
		}
		
		public String toString() {
			return "SETTHIS: " + getObjectName(object);
		}
	}
	
	public static class EnterMethodEvent extends Event {
		public final String className;
		public final String name;
		public final String descriptor;
		public final Object[] args;

		public EnterMethodEvent(String className, String name, String descriptor, Object[] args, Thread thread, int methodIndex) {
			super(thread, methodIndex);
			this.className = className;
			this.name = name;
			this.descriptor = descriptor;
			this.args = args;
			this.thread = thread;
		}
		
		public String toString() {
			return "ENTER METHOD: " + className + ", " + name + ", " + descriptor + ", " + Arrays.asList(args);
		}
	}
	
	public static class ExitWithExceptionEvent extends Event {
		public final Throwable throwable;
		
		public ExitWithExceptionEvent(Throwable throwable, Thread thread, int methodIndex) {
			super(thread, methodIndex);
			this.throwable = throwable;
		}
		
		public String toString() {
			return "EXIT EXCEPTION: " + throwable.getClass().getName() + ", " + throwable.getMessage();
		}
	}
	
	public static class ExitWithValueEvent extends Event {
		public final Object value;
		
		public ExitWithValueEvent(Object value, Thread thread, int methodIndex) {
			super(thread, methodIndex);
			this.value = value;
		}
		
		public String toString() {
			return "EXIT VALUE: " + value;
		}
	}
	
	public static class SetLocalNameEvent extends Event {
		public final String name;
		public final int index;

		public SetLocalNameEvent(String name, int index, Thread thread, int methodIndex) {
			super(thread, methodIndex);
			this.name = name;
			this.index = index;
		}
		
		public String toString() {
			return "SET LOCAL NAME: " + name + ", " + index;
		}
	}
}