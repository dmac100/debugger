package debugger.event;

import java.util.List;
import java.util.Set;

import debugger.event.Events.Event;
import debugger.event.Events.InvokeMethodEvent;
import debugger.event.Events.InvokeSpecialMethodEvent;
import debugger.event.Events.InvokeStaticMethodEvent;

public interface SnapshotCreator {
	public Object createObject();
	public boolean isCompatibleType(Object object);
	public Set<String> getForwardedMethods();
	public Event createSnapshotEvent(Object value, int lineNumber, Thread thread, int methodIndex);
	public List<Event> createSnapshotEvent(InvokeStaticMethodEvent invokeStaticMethodEvent);
	public List<Event> createSnapshotEvent(InvokeMethodEvent invokeMethodEvent);
	public List<Event> createSnapshotEvent(InvokeSpecialMethodEvent invokeSpecialMethodEvent);
}