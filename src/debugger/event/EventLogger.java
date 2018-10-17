package debugger.event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EventLogger {
	private static Map<Object, Integer> objects = new ConcurrentHashMap<>();
	private static Map<Class<?>, Integer> classes = new ConcurrentHashMap<>();
	
	private static int methodIndex = 0;
	
	private static boolean writeLog = true;
	
	private static List<String> log = new ArrayList<>();
	
	public static List<String> getLog() {
		return log;
	}
	
	public static void clear() {
		objects.clear();
		classes.clear();
		log.clear();
		methodIndex = 0;
	}
	
	private static void log(String value) {
		if(writeLog) {
			log.add(value);
		}
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
	
	public static int nextMethodIndex() {
		return methodIndex++;
	}

	public static void putField(Object object, String name, Object value, Thread thread, int methodIndex) {
		log("PUT FIELD: " + getObjectName(object) + ", " + name + ", " + value);
	}

	public static void store(int varIndex, Object value, Thread thread, int methodIndex) {
		log("STORE: " + varIndex + ", " + value);
	}
	
	public static void storeArray(Object array, int index, Object value, Thread thread, int methodIndex) {
		log("STORE ARRAY: " + getObjectName(array) + ", " + index + ", " + value);
	}

	public static void invokeMethod(Object object, String name, String descriptor, Object[] args, Thread thread, int methodIndex) {
		log("INVOKE: " + getObjectName(object) + ", " + name + ", " + descriptor + ", " + Arrays.toString(args));
	}
	
	public static void invokeSpecialMethod(Object object, String name, String descriptor, Object[] args, Thread thread, int methodIndex) {
		log("INVOKE SPECIAL: " + getObjectName(object) + ", " + name + ", " + descriptor + ", " + Arrays.toString(args));
	}
	
	public static void invokeStaticMethod(String className, String name, String descriptor, Object[] args, Thread thread, int methodIndex) {
		log("INVOKE STATIC: " + className + ", " + name + ", " + descriptor + ", " + Arrays.toString(args));
	}
	
	public static void returnValue(Object value, Thread thread, int methodIndex) {
		log("RETURN: " + value);
	}
	
	public static void returnedValue(Object value, Thread thread, int methodIndex) {
		log("RETURNED: " + value);
	}
	
	public static void throwException(Throwable t, Thread thread, int methodIndex) {
		log("THROW: " + t.getClass().getName() + ", " + t.getMessage());
	}
	
	public static void catchException(Throwable t, Thread thread, int methodIndex) {
		log("CATCH: " + t.getClass().getName() + ", " + t.getMessage());
	}
	
	public static void exitWithException(Throwable t, Thread thread, int methodIndex) {
		log("EXIT EXCEPTION: " + t.getClass().getName() + ", " + t.getMessage());
	}
	
	public static void exitWithValue(Object value, Thread thread, int methodIndex) {
		log("EXIT VALUE: " + value);
	}
}