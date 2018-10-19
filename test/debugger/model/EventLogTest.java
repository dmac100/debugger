package debugger.model;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import debugger.event.EventLogger;
import debugger.instrumentation.Instrumentor;

public class EventLogTest {
	private EventLog eventLog;
	
	@Before
	public void before() {
		EventLogger.clear();
		new Instrumentor().instrumentClass(QuickSort.class);
		QuickSort.sort(Arrays.asList(5, 2, 7, 5, 9, 8, 7, 1, 3));
		eventLog = new EventLog(EventLogger.getEvents());
	}
	
	@Test
	public void getThreads() {
		List<Thread> threads = eventLog.getThreads();
		assertEquals(1, threads.size());
		assertEquals("main", threads.get(0).getName());
	}
}
