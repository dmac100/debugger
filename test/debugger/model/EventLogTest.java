package debugger.model;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import debugger.event.EventLogger;
import debugger.instrumentation.Instrumentor;

public class EventLogTest {
	@Before
	public void before() {
		EventLogger.clear();
		new Instrumentor().instrumentClass(QuickSort.class);
		QuickSort.sort(Arrays.asList(5, 2, 7, 5, 9, 8, 7, 1, 3));
	}
	
	@Test
	public void test() {
	}
}
