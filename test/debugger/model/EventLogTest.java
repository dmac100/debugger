package debugger.model;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import debugger.event.EventLogger;
import debugger.instrumentation.Instrumentor;
import debugger.model.EventLog.CallStackNode;

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
	
	@Test
	public void getCallStack() {
		String callStack = printCallStack(eventLog.getCallStack());
		
		String expectedCallStack = "debugger/model/QuickSort.sort([[5, 2, 7, 5, 9, 8, 7, 1, 3]]) - [1, 2, 3, 5, 5, 7, 7, 8, 9]\n";
		expectedCallStack += "  debugger/model/QuickSort.sort([[2, 1, 3]]) - [1, 2, 3]\n";
		expectedCallStack += "    debugger/model/QuickSort.sort([[1]]) - [1]\n";
		expectedCallStack += "    debugger/model/QuickSort.sort([[3]]) - [3]\n";
		expectedCallStack += "  debugger/model/QuickSort.sort([[7, 5, 9, 8, 7]]) - [5, 7, 7, 8, 9]\n";
		expectedCallStack += "    debugger/model/QuickSort.sort([[5]]) - [5]\n";
		expectedCallStack += "    debugger/model/QuickSort.sort([[9, 8, 7]]) - [7, 8, 9]\n";
		expectedCallStack += "      debugger/model/QuickSort.sort([[8, 7]]) - [7, 8]\n";
		expectedCallStack += "        debugger/model/QuickSort.sort([[7]]) - [7]\n";
		expectedCallStack += "        debugger/model/QuickSort.sort([[]]) - []\n";
		expectedCallStack += "      debugger/model/QuickSort.sort([[]]) - []\n";
		
		assertEquals(expectedCallStack, callStack);
	}

	private String printCallStack(List<CallStackNode> callStack) {
		StringBuilder s = new StringBuilder();
		printCallStack(callStack, s, "");
		return s.toString();
	}
	
	private void printCallStack(List<CallStackNode> callStack, StringBuilder s, String indent) {
		for(CallStackNode node:callStack) {
			s.append(indent).append(node).append("\n");
			printCallStack(node.getChildren(), s, indent + "  ");
		}
	}
}
