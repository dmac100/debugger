package debugger.event;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import debugger.event.Events.Event;

public class EventLogger {
	public static final List<SnapshotCreator> snapshotCreators = List.of(new ListSnapshotCreator());
 	
	private static final AtomicInteger methodIndex = new AtomicInteger();
	private static final Queue<Event> events = new ConcurrentLinkedQueue<>();
	private static final Set<Object> seenObjects = new HashSet<>();
	
	public static List<String> getLog() {
		return events.stream().map(Event::toString).collect(toList());
	}
	
	public static List<Event> getEvents() {
		return new ArrayList<>(events);
	}
	
	public static void clear() {
		Events.clear();
		events.clear();
		methodIndex.set(0);
		seenObjects.clear();
	}
	
	public static int nextMethodIndex() {
		return methodIndex.incrementAndGet();
	}

	public static void putField(Object object, String name, Object value, int lineNumber, Thread thread, int methodIndex) {
		checkObjectSnapshot(value, lineNumber, thread, methodIndex);
		events.add(new Events.PutFieldEvent(object, name, value, lineNumber, thread, methodIndex));
	}

	public static void store(int varIndex, Object value, int lineNumber, Thread thread, int methodIndex) {
		checkObjectSnapshot(value, lineNumber, thread, methodIndex);
		events.add(new Events.StoreEvent(varIndex, value, lineNumber, thread, methodIndex));
	}
	
	public static void storeArray(Object array, int index, Object value, int lineNumber, Thread thread, int methodIndex) {
		checkObjectSnapshot(value, lineNumber, thread, methodIndex);
		events.add(new Events.StoreArrayEvent(array, index, value, lineNumber, thread, methodIndex));
	}

	public static void invokeMethod(Object object, String className, Object[] args, String name, String descriptor, int lineNumber, Thread thread, int methodIndex) {
		events.add(new Events.InvokeMethodEvent(object, name, descriptor, args, lineNumber, thread, methodIndex));
	}
	
	public static void invokeSpecialMethod(Object object, String className, Object[] args, String name, String descriptor, int lineNumber, Thread thread, int methodIndex) {
		events.add(new Events.InvokeSpecialMethodEvent(object, className, name, descriptor, args, lineNumber, thread, methodIndex));
	}
	
	public static void invokeStaticMethod(String className, Object[] args, String name, String descriptor, int lineNumber, Thread thread, int methodIndex) {
		events.add(new Events.InvokeStaticMethodEvent(className, name, descriptor, args, lineNumber, thread, methodIndex));
	}
	
	public static void invokeMethodAfter(Object object, String className, Object[] args, String name, String descriptor, int lineNumber, Thread thread, int methodIndex) {
		for(SnapshotCreator snapshotCreator:snapshotCreators) {
			events.addAll(snapshotCreator.createSnapshotEvent(new Events.InvokeMethodEvent(object, name, descriptor, args, lineNumber, thread, methodIndex)));
		}
	}
	
	public static void invokeSpecialMethodAfter(Object object, String className, Object[] args, String name, String descriptor, int lineNumber, Thread thread, int methodIndex) {
		for(SnapshotCreator snapshotCreator:snapshotCreators) {
			events.addAll(snapshotCreator.createSnapshotEvent(new Events.InvokeSpecialMethodEvent(object, className, name, descriptor, args, lineNumber, thread, methodIndex)));
		}
	}
	
	public static void invokeStaticMethodAfter(String className, Object[] args, String name, String descriptor, int lineNumber, Thread thread, int methodIndex) {
		for(SnapshotCreator snapshotCreator:snapshotCreators) {
			events.addAll(snapshotCreator.createSnapshotEvent(new Events.InvokeStaticMethodEvent(className, name, descriptor, args, lineNumber, thread, methodIndex)));
		}
	}
	
	private static void checkObjectSnapshot(Object value, int lineNumber, Thread thread, int methodIndex) {
		if(!seenObjects.contains(value)) {
			seenObjects.add(value);
			for(SnapshotCreator snapshotCreator:snapshotCreators) {
				if(snapshotCreator.isCompatibleType(value)) {
					events.add(snapshotCreator.createSnapshotEvent(value, lineNumber, thread, methodIndex));
				}
			}
		}
	}
	
	public static void returnValue(Object value, int lineNumber, Thread thread, int methodIndex) {
		checkObjectSnapshot(value, lineNumber, thread, methodIndex);
		events.add(new Events.ReturnValueEvent(value, lineNumber, thread, methodIndex));
	}
	
	public static void returnedValue(Object value, int lineNumber, Thread thread, int methodIndex) {
		checkObjectSnapshot(value, lineNumber, thread, methodIndex);
		events.add(new Events.ReturnedValueEvent(value, lineNumber, thread, methodIndex));
	}
	
	public static void throwException(Throwable t, int lineNumber, Thread thread, int methodIndex) {
		events.add(new Events.ThrowExceptionEvent(t, lineNumber, thread, methodIndex));
	}
	
	public static void catchException(Throwable t, int lineNumber, Thread thread, int methodIndex) {
		events.add(new Events.CatchExceptionEvent(t, lineNumber, thread, methodIndex));
	}

	public static void setThis(Object object, int lineNumber, Thread thread, int methodIndex) {
		events.add(new Events.SetThisEvent(object, lineNumber, thread, methodIndex));
	}
	
	public static void enterMethod(String className, String methodName, String descriptor, Object[] args, int lineNumber, Thread thread, int methodIndex) {
		for(Object arg:args) {
			checkObjectSnapshot(arg, lineNumber, thread, methodIndex);
		}
		events.add(new Events.EnterMethodEvent(className, methodName, descriptor, args, lineNumber, thread, methodIndex));
	}
	
	public static void exitWithException(Throwable t, int lineNumber, Thread thread, int methodIndex) {
		events.add(new Events.ExitWithExceptionEvent(t, lineNumber, thread, methodIndex));
	}
	
	public static void exitWithValue(Object value, int lineNumber, Thread thread, int methodIndex) {
		checkObjectSnapshot(value, lineNumber, thread, methodIndex);
		events.add(new Events.ExitWithValueEvent(value, lineNumber, thread, methodIndex));
	}
	
	public static void setLocalName(String name, int index, int lineNumber, Thread thread, int methodIndex) {
		events.add(new Events.SetLocalNameEvent(name, index, lineNumber, thread, methodIndex));
	}
}