package debugger.model;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import debugger.event.Events.Event;

public class EventLog {
	private final List<Event> events;

	public EventLog(List<Event> events) {
		this.events = events;
	}
	
	public List<Thread> getThreads() {
		return events.stream()
			.map(Event::getThread)
			.distinct()
			.sorted(Comparator.comparing(Thread::getName))
			.collect(Collectors.toList());
	}
}