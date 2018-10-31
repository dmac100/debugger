package debugger.event;

public interface SnapshotEvent {
	public Object getSnapshotObject();
	public boolean matchesObject(Object object);
}