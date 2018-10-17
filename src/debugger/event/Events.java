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
	
	private static String getObjectName(Object object) {
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
	
	public interface Event {
	}
	
	public static class PutFieldEvent implements Event {
		public final Object object;
		public final String name;
		public final Object value;
		public final Thread thread;
		public final int methodIndex;
		
		public PutFieldEvent(Object object, String name, Object value, Thread thread, int methodIndex) {
			this.object = object;
			this.name = name;
			this.value = value;
			this.thread = thread;
			this.methodIndex = methodIndex;
		}
		
		public String toString() {
			return "PUT FIELD: " + getObjectName(object) + ", " + name + ", " + value;
		}
	}
	
	public static class StoreEvent implements Event {
		public final int varIndex;
		public final Object value;
		public final Thread thread;
		public final int methodIndex;
		
		public StoreEvent(int varIndex, Object value, Thread thread, int methodIndex) {
			this.varIndex = varIndex;
			this.value = value;
			this.thread = thread;
			this.methodIndex = methodIndex;
		}
		
		public String toString() {
			return "STORE: " + varIndex + ", " + value;
		}
	}
	
	public static class StoreArrayEvent implements Event {
		public final Object array;
		public final int index;
		public final Object value;
		public final Thread thread;
		public final int methodIndex;
		
		public StoreArrayEvent(Object array, int index, Object value, Thread thread, int methodIndex) {
			this.array = array;
			this.index = index;
			this.value = value;
			this.thread = thread;
			this.methodIndex = methodIndex;
		}
		
		public String toString() {
			return "STORE ARRAY: " + getObjectName(array) + ", " + index + ", " + value;
		}
	}

	public static class InvokeMethodEvent implements Event {
		public final Object object;
		public final String name;
		public final String descriptor;
		public final Object[] args;
		public final Thread thread;
		public final int methodIndex;
		
		public InvokeMethodEvent(Object object, String name, String descriptor, Object[] args, Thread thread, int methodIndex) {
			this.object = object;
			this.name = name;
			this.descriptor = descriptor;
			this.args = args;
			this.thread = thread;
			this.methodIndex = methodIndex;
		}
		
		public String toString() {
			return "INVOKE: " + getObjectName(object) + ", " + name + ", " + descriptor + ", " + Arrays.toString(args);
		}
	}
	
	public static class InvokeSpecialMethod implements Event {
		public final Object object;
		public final String name;
		public final String descriptor;
		public final Object[] args;
		public final Thread thread;
		public final int methodIndex;
		
		public InvokeSpecialMethod(Object object, String name, String descriptor, Object[] args, Thread thread, int methodIndex) {
			this.object = object;
			this.name = name;
			this.descriptor = descriptor;
			this.args = args;
			this.thread = thread;
			this.methodIndex = methodIndex;
		}
		
		public String toString() {
			return "INVOKE SPECIAL: " + getObjectName(object) + ", " + name + ", " + descriptor + ", " + Arrays.toString(args);
		}
	}
	
	public static class InvokeStaticMethodEvent implements Event {
		public final String className;
		public final String name;
		public final String descriptor;
		public final Object[] args;
		public final Thread thread;
		public final int methodIndex;
		
		public InvokeStaticMethodEvent(String className, String name, String descriptor, Object[] args, Thread thread, int methodIndex) {
			this.className = className;
			this.name = name;
			this.descriptor = descriptor;
			this.args = args;
			this.thread = thread;
			this.methodIndex = methodIndex;
		}
		
		public String toString() {
			return "INVOKE STATIC: " + className + ", " + name + ", " + descriptor + ", " + Arrays.toString(args);
		}
	}
	
	public static class ReturnValueEvent implements Event {
		public final Object value;
		public final Thread thread;
		public final int methodIndex;
		
		public ReturnValueEvent(Object value, Thread thread, int methodIndex) {
			this.value = value;
			this.thread = thread;
			this.methodIndex = methodIndex;
		}
		
		public String toString() {
			return "RETURN: " + value;
		}
	}
	
	public static class ReturnedValueEvent implements Event {
		public final Object value;
		public final Thread thread;
		public final int methodIndex;
		
		public ReturnedValueEvent(Object value, Thread thread, int methodIndex) {
			this.value = value;
			this.thread = thread;
			this.methodIndex = methodIndex;
		}
		
		public String toString() {
			return "RETURNED: " + value;
		}
	}
	
	public static class ThrowExceptionEvent implements Event {
		public final Throwable throwable;
		public final Thread thread;
		public final int methodIndex;
		
		public ThrowExceptionEvent(Throwable throwable, Thread thread, int methodIndex) {
			this.throwable = throwable;
			this.thread = thread;
			this.methodIndex = methodIndex;
		}
		
		public String toString() {
			return "THROW: " + throwable.getClass().getName() + ", " + throwable.getMessage();
		}
	}
	
	public static class CatchExceptionEvent implements Event {
		public final Throwable throwable;
		public final Thread thread;
		public final int methodIndex;
		
		public CatchExceptionEvent(Throwable throwable, Thread thread, int methodIndex) {
			this.throwable = throwable;
			this.thread = thread;
			this.methodIndex = methodIndex;
		}
		
		public String toString() {
			return "CATCH: " + throwable.getClass().getName() + ", " + throwable.getMessage();
		}
	}
	
	public static class ExitWithExceptionEvent implements Event {
		public final Throwable throwable;
		public final Thread thread;
		public final int methodIndex;
		
		public ExitWithExceptionEvent(Throwable throwable, Thread thread, int methodIndex) {
			this.throwable = throwable;
			this.thread = thread;
			this.methodIndex = methodIndex;
		}
		
		public String toString() {
			return "EXIT EXCEPTION: " + throwable.getClass().getName() + ", " + throwable.getMessage();
		}
	}
	
	public static class ExitWithValueEvent implements Event {
		public final Object value;
		public final Thread thread;
		public final int methodIndex;
		
		public ExitWithValueEvent(Object value, Thread thread, int methodIndex) {
			this.value = value;
			this.thread = thread;
			this.methodIndex = methodIndex;
		}
		
		public String toString() {
			return "EXIT VALUE: " + value;
		}
	}
}