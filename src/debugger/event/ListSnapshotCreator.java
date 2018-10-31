package debugger.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import debugger.event.Events.Event;
import debugger.event.Events.InvokeMethodEvent;
import debugger.event.Events.InvokeSpecialMethodEvent;
import debugger.event.Events.InvokeStaticMethodEvent;

public class ListSnapshotCreator implements SnapshotCreator {
	public static class ListSnapshotEvent extends Event implements SnapshotEvent {
		public final Object object;
		public final List<Object> list;

		public ListSnapshotEvent(List<Object> list, int lineNumber, Thread thread, int methodIndex) {
			super(lineNumber, thread, methodIndex);
			this.object = list;
			this.list = new ArrayList<>(list);
		}
		
		@Override
		public Object getSnapshotObject() {
			return new ArrayList<>(list);
		}
		
		@Override
		public boolean matchesObject(Object object) {
			return object == this.object;
		}
		
		public String toString() {
			return "LIST SNAPSHOT: " + list;
		}
	}
	
	@Override
	public Object createObject() {
		return new ArrayList<>();
	}
	
	@Override
	public boolean isCompatibleType(Object object) {
		return object instanceof List;
	}
	
	@Override
	public Set<String> getForwardedMethods() {
		 return Set.of(
			"add(Ljava/lang/Object;)Z",
			"add(ILjava/lang/Object;)V",
			"clear()V",
			"remove(I)Ljava/lang/Object;",
			"remove(Ljava/lang/Object;)Z",
			"removeAll(Ljava/util/Collection;)Z",
			"retainAll(Ljava/util/Collection;)Z",
			"set(ILjava/lang/Object;)Ljava/lang/Object;"
		);
	}
	
	@Override
	public Event createSnapshotEvent(Object value, int lineNumber, Thread thread, int methodIndex) {
		return new ListSnapshotEvent((List) value, lineNumber, thread, methodIndex);
	}
	
	@Override
	public List<Event> createSnapshotEvent(InvokeStaticMethodEvent event) {
		if(event.className.equals("java/util/Collections") && event.name.equals("sort") && event.descriptor.equals("(Ljava/util/List;)V")) {
			List<Object> list = (List) event.args[0];
			return List.of(new ListSnapshotEvent(list, event.lineNumber, event.thread, event.methodIndex));
		}
		return List.of();
	}

	@Override
	public List<Event> createSnapshotEvent(InvokeMethodEvent invokeMethodEvent) {
		return List.of();
	}

	@Override
	public List<Event> createSnapshotEvent(InvokeSpecialMethodEvent invokeSpecialMethodEvent) {
		return List.of();
	}
}