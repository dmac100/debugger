package debugger.model;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import debugger.event.EventLogger;
import debugger.instrumentation.Instrumentor;

class InstrumentedClass {
	NonInstrumentedClass nonInstrumentedClass = new NonInstrumentedClass();
	
	public InstrumentedClass() {
	}
	
	public InstrumentedClass(int x) {
	}

	public static int f() {
		return g() + 1;
	}
	
	public static int g() {
		return NonInstrumentedClass.f() + 1;
	}
	
	public static int h() {
		return 2;
	}
	
	public int a() {
		return b() + 1;
	}
	
	public int b() {
		return nonInstrumentedClass.b(this) + 1;
	}

	public int c() {
		return 2;
	}
	
	public static int throwException1() {
		try {
			throwException2();
		} catch(Exception e) {
		}
		return 1;
	}
	
	public static void throwException2() {
		throwException3();
	}
	
	public static void throwException3() {
		throw new RuntimeException();
	}
	
	public static int throwException4() {
		try {
			NonInstrumentedClass.throwException5();
		} catch(Exception e) {
		}
		return 1;
	}
}

class InstrumentedSubclass extends InstrumentedClass {
	public InstrumentedSubclass() {
		super(1);
	}
}

class NonInstrumentedClass {
	public static int f() {
		return InstrumentedClass.h() + 1;
	}

	public static void throwException5() {
		throw new RuntimeException();
	}

	public int b(InstrumentedClass instrumentedClass) {
		return instrumentedClass.c() + 1;
	}
}

public class EventLogTest {
	private EventLog eventLog;
	
	@Before
	public void before() {
		new Instrumentor().instrumentClass(QuickSort.class);
		new Instrumentor().instrumentClass(InstrumentedClass.class);
		new Instrumentor().instrumentClass(InstrumentedSubclass.class);
	}
	
	@After
	public void after() {
		EventLogger.clear();
	}
	
	private Thread getThread() {
		return eventLog.getThreads().get(0);
	}
	
	@Test
	public void getThreads() {
		QuickSort.sort(Arrays.asList(5, 2, 7, 5, 9, 8, 7, 1, 3));
		eventLog = new EventLog(EventLogger.getEvents());
		
		List<Thread> threads = eventLog.getThreads();
		assertEquals(1, threads.size());
		assertEquals("main", threads.get(0).getName());
	}
	
	@Test
	public void getCallStack() {
		QuickSort.sort(Arrays.asList(5, 2, 7, 5, 9, 8, 7, 1, 3));
		eventLog = new EventLog(EventLogger.getEvents());
		
		String callStack = printCallStack(eventLog.getCallStack(getThread()));
		
		callStack = callStack
			.replaceAll(".*<init>.*[\r\n?]", "")
			.replaceAll(".*Integer.valueOf.*[\r\n?]", "")
			.replaceAll(".*Integer.intValue.*[\r\n?]", "");
		
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
	
	@Test
	public void getCallStack_withStaticNonInstrumented() {
		InstrumentedClass.f();
		eventLog = new EventLog(EventLogger.getEvents());
		
		String callStack = printCallStack(eventLog.getCallStack(getThread()));
		
		String expectedCallStack = "debugger/model/InstrumentedClass.f([]) - 5\n";
		expectedCallStack += "  debugger/model/InstrumentedClass.g([]) - 4\n";
		expectedCallStack += "    debugger/model/NonInstrumentedClass.f([]) - 3\n";
		expectedCallStack += "      ...\n";
		expectedCallStack += "        debugger/model/InstrumentedClass.h([]) - 2\n";
		
		assertEquals(expectedCallStack, callStack);
	}
	
	@Test
	public void getCallStack_withVirtualNonInstrumented() {
		new InstrumentedClass().a();
		eventLog = new EventLog(EventLogger.getEvents());
		
		String callStack = printCallStack(eventLog.getCallStack(getThread()))
			.replaceAll(".*<init>.*[\r\n?]", "");
		
		String expectedCallStack = "debugger/model/InstrumentedClass.a([]) - 5\n";
		expectedCallStack += "  debugger/model/InstrumentedClass.b([]) - 4\n";
		expectedCallStack += "    debugger/model/NonInstrumentedClass.b([InstrumentedClass-1]) - 3\n";
		expectedCallStack += "      ...\n";
		expectedCallStack += "        debugger/model/InstrumentedClass.c([]) - 2\n";
		
		assertEquals(expectedCallStack, callStack);
	}
	
	@Test
	public void getCallStack_superCall() {
		new InstrumentedSubclass();
		eventLog = new EventLog(EventLogger.getEvents());
		
		String callStack = printCallStack(eventLog.getCallStack(getThread()));
		
		String expectedCallStack = "debugger/model/InstrumentedSubclass.<init>([]) - null\n";
		expectedCallStack += "  debugger/model/InstrumentedClass.<init>([1]) - null\n";
		expectedCallStack += "    java/lang/Object.<init>([]) - null\n";
		expectedCallStack += "    debugger/model/NonInstrumentedClass.<init>([]) - null\n";
		
		assertEquals(expectedCallStack, callStack);
	}
	
	@Test
	public void getCallStack_withException() {
		InstrumentedClass.throwException1();
		
		eventLog = new EventLog(EventLogger.getEvents());
		
		String callStack = printCallStack(eventLog.getCallStack(getThread()))
			.replaceAll(".*<init>.*[\r\n?]", "");
		
		String expectedCallStack = "debugger/model/InstrumentedClass.throwException1([]) - 1\n";
		expectedCallStack += "  debugger/model/InstrumentedClass.throwException2([]) - java/lang/RuntimeException\n";
		expectedCallStack += "    debugger/model/InstrumentedClass.throwException3([]) - java/lang/RuntimeException\n";
		
		assertEquals(expectedCallStack, callStack);
	}
	
	@Test
	public void getCallStack_withNonInstrumentedException() {
		InstrumentedClass.throwException4();
		
		eventLog = new EventLog(EventLogger.getEvents());
		
		String callStack = printCallStack(eventLog.getCallStack(getThread()));
		
		String expectedCallStack = "debugger/model/InstrumentedClass.throwException4([]) - 1\n";
		expectedCallStack += "  debugger/model/NonInstrumentedClass.throwException5([]) - null\n";
		
		assertEquals(expectedCallStack, callStack);
	}

	public static String printCallStack(List<CallStackNode> callStack) {
		StringBuilder s = new StringBuilder();
		printCallStack(callStack, s, "");
		return s.toString();
	}
	
	private static void printCallStack(List<CallStackNode> callStack, StringBuilder s, String indent) {
		for(CallStackNode node:callStack) {
			s.append(indent).append(node).append("\n");
			if(!node.isInstrumented() && !node.getChildren().isEmpty()) {
				s.append(indent).append("  ...\n");
				printCallStack(node.getChildren(), s, indent + "    ");
			} else {
				printCallStack(node.getChildren(), s, indent + "  ");
			}
		}
	}
	
	@Test
	public void getLocalVariables() {
		QuickSort.sort(Arrays.asList(5, 2, 7, 5, 9, 8, 7, 1, 3));
		eventLog = new EventLog(EventLogger.getEvents());
		
		int index = eventLog.getLastIndex(getThread()) - 1;
		
		Map<String, Object> locals = new HashMap<>(eventLog.getLocalVariablesAt(getThread(), index));
		locals.replaceAll((k, v) -> ToStringImpl.toString(v));
		
		assertEquals(Map.of(
			"pivot", "5",
			"left", "[2, 1, 3]",
			"right", "[7, 5, 9, 8, 7]",
			"result", "[1, 2, 3, 5, 5, 7, 7, 8, 9]"
		), locals);
	}
}