package debugger.event;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;

import debugger.event.Events.Event;

public class EventLogger {
	private static int methodIndex = 0;
	
	private static List<Event> events = new ArrayList<>();
	
	public static List<String> getLog() {
		return events.stream().map(Event::toString).collect(toList());
	}
	
	public static List<Event> getEvents() {
		return new ArrayList<>(events);
	}
	
	public static void clear() {
		Events.clear();
		events.clear();
		methodIndex = 0;
	}
	
	public static int nextMethodIndex() {
		return methodIndex++;
	}

	public static void putField(Object object, String name, Object value, Thread thread, int methodIndex) {
		events.add(new Events.PutFieldEvent(object, name, value, thread, methodIndex));
	}

	public static void store(int varIndex, Object value, Thread thread, int methodIndex) {
		events.add(new Events.StoreEvent(varIndex, value, thread, methodIndex));
	}
	
	public static void storeArray(Object array, int index, Object value, Thread thread, int methodIndex) {
		events.add(new Events.StoreArrayEvent(array, index, value, thread, methodIndex));
	}

	public static void invokeMethod(Object object, String name, String descriptor, Object[] args, Thread thread, int methodIndex) {
		events.add(new Events.InvokeMethodEvent(object, name, descriptor, args, thread, methodIndex));
	}
	
	public static void invokeSpecialMethod(String className, String name, String descriptor, Object[] args, Thread thread, int methodIndex) {
		events.add(new Events.InvokeSpecialMethodEvent(className, name, descriptor, args, thread, methodIndex));
	}
	
	public static void invokeStaticMethod(String className, String name, String descriptor, Object[] args, Thread thread, int methodIndex) {
		events.add(new Events.InvokeStaticMethodEvent(className, name, descriptor, args, thread, methodIndex));
	}
	
	public static void returnValue(Object value, Thread thread, int methodIndex) {
		events.add(new Events.ReturnValueEvent(value, thread, methodIndex));
	}
	
	public static void returnedValue(Object value, Thread thread, int methodIndex) {
		events.add(new Events.ReturnedValueEvent(value, thread, methodIndex));
	}
	
	public static void throwException(Throwable t, Thread thread, int methodIndex) {
		events.add(new Events.ThrowExceptionEvent(t, thread, methodIndex));
	}
	
	public static void catchException(Throwable t, Thread thread, int methodIndex) {
		events.add(new Events.CatchExceptionEvent(t, thread, methodIndex));
	}

	public static void setThis(Object object, Thread thread, int methodIndex) {
		events.add(new Events.SetThisEvent(object, thread, methodIndex));
	}
	
	public static void enterMethod(String className, String methodName, String descriptor, Object[] args, Thread thread, int methodIndex) {
		events.add(new Events.EnterMethodEvent(className, methodName, descriptor, args, thread, methodIndex));
	}
	
	public static void exitWithException(Throwable t, Thread thread, int methodIndex) {
		events.add(new Events.ExitWithExceptionEvent(t, thread, methodIndex));
	}
	
	public static void exitWithValue(Object value, Thread thread, int methodIndex) {
		events.add(new Events.ExitWithValueEvent(value, thread, methodIndex));
	}
}