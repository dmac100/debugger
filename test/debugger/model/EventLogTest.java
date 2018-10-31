package debugger.model;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import debugger.event.EventLogger;
import debugger.event.SnapshotCreator;
import debugger.instrumentation.Instrumentor;
import debugger.instrumentation.util.AsmUtil;

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
	
	public int callOverloadedMethod() {
		return overloadedMethod();
	}

	public int overloadedMethod() {
		return 2;
	}
}

class InstrumentedSubclass extends InstrumentedClass {
	public InstrumentedSubclass() {
		super(1);
	}
	
	public int overloadedMethod() {
		return 3;
	}
}

class NonInstrumentedSubclass extends InstrumentedClass {
	public NonInstrumentedSubclass() {
		super(1);
	}
	
	public int overloadedMethod() {
		return 3;
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

class ArrayListClass {
	public static void f1(List<Integer> list) {
		list.add(1);
		list.clear();
		list.add(3);
		list.add(0, 2);
		list.add(0, 1);
		list.remove(0);
		list.set(0, 3);
		list.add(4);
		list.add(5);
		list.retainAll(Arrays.asList(3, 4));
	}
	
	public static void f2(List<Integer> list) {
		list.add(3);
		list.add(2);
		list.add(1);
		Collections.sort(list);
	}
	
	public static void f3(List<Integer> list) {
		List<Integer> otherList = new ArrayList<>();
		list.add(3);
		list.add(2);
		list.add(1);
		otherList.add(4);
		otherList.add(5);
		otherList.add(6);
		Collections.sort(otherList);
	}
}

public class EventLogTest {
	private EventLog eventLog;
	
	@Before
	public void before() {
		new Instrumentor().instrumentClass(QuickSort.class);
		new Instrumentor().instrumentClass(InstrumentedClass.class);
		new Instrumentor().instrumentClass(InstrumentedSubclass.class);
		new Instrumentor().instrumentClass(ArrayListClass.class);
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
		
		eventLog.setThread(getThread());
		String callStack = printCallStack(eventLog.getCallStack());
		
		callStack = callStack
			.replaceAll(".*<init>.*[\r\n?]", "")
			.replaceAll(".*ArrayList.*[\r\n?]", "")
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
		
		eventLog.setThread(getThread());
		String callStack = printCallStack(eventLog.getCallStack());
		
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
		
		eventLog.setThread(getThread());
		String callStack = printCallStack(eventLog.getCallStack())
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
		
		eventLog.setThread(getThread());
		String callStack = printCallStack(eventLog.getCallStack());
		
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
		
		eventLog.setThread(getThread());
		String callStack = printCallStack(eventLog.getCallStack())
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
		
		eventLog.setThread(getThread());
		String callStack = printCallStack(eventLog.getCallStack());
		
		String expectedCallStack = "debugger/model/InstrumentedClass.throwException4([]) - 1\n";
		expectedCallStack += "  debugger/model/NonInstrumentedClass.throwException5([]) - null\n";
		
		assertEquals(expectedCallStack, callStack);
	}
	
	@Test
	public void getCallStack_withOverloadedMethods() {
		new InstrumentedSubclass().callOverloadedMethod();
		
		eventLog = new EventLog(EventLogger.getEvents());
		
		eventLog.setThread(getThread());
		String callStack = printCallStack(eventLog.getCallStack())
			.replaceAll(".*<init>.*[\r\n?]", "");
		
		String expectedCallStack = "debugger/model/InstrumentedClass.callOverloadedMethod([]) - 3\n";
		expectedCallStack += "  debugger/model/InstrumentedSubclass.overloadedMethod([]) - 3\n";
		
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
		
		eventLog.setThread(getThread());
		eventLog.setIndex(index);
		
		Map<String, Object> locals = new HashMap<>(eventLog.getLocalVariables());
		locals.replaceAll((k, v) -> ToStringImpl.toString(v));
		
		assertEquals(Map.of(
			"pivot", "5",
			"left", "[2, 1, 3]",
			"right", "[7, 5, 9, 8, 7]",
			"result", "[1, 2, 3, 5, 5, 7, 7, 8, 9]"
		), locals);
	}
	
	@Test
	public void getArrayListSnapshot() {
		List<Integer> arrayList = new ArrayList<>();
		
		ArrayListClass.f1(arrayList);
		eventLog = new EventLog(EventLogger.getEvents());
		eventLog.setThread(getThread());
		eventLog.setIndex(eventLog.getLastIndex(getThread()));
		arrayList.clear();
		
		assertEquals(Arrays.asList(3, 3, 4), eventLog.getObjectSnapshot(arrayList, eventLog.getEvents()).get());
	}
	
	@Test
	public void getArrayListSnapshot_collectionsMethod() {
		List<Integer> arrayList = new ArrayList<>();
		
		ArrayListClass.f2(arrayList);
		eventLog = new EventLog(EventLogger.getEvents());
		eventLog.setThread(getThread());
		eventLog.setIndex(eventLog.getLastIndex(getThread()));
		arrayList.clear();
		
		assertEquals(Arrays.asList(1, 2, 3), eventLog.getObjectSnapshot(arrayList, eventLog.getEvents()).get());
	}
	
	@Test
	public void getArrayListSnapshot_withOtherList() {
		List<Integer> arrayList = new ArrayList<>();
		
		ArrayListClass.f3(arrayList);
		eventLog = new EventLog(EventLogger.getEvents());
		eventLog.setThread(getThread());
		eventLog.setIndex(eventLog.getLastIndex(getThread()));
		arrayList.clear();
		
		assertEquals(Arrays.asList(3, 2, 1), eventLog.getObjectSnapshot(arrayList, eventLog.getEvents()).get());
	}
	
	@Test
	public void getArrayListSnapshot_withInitialValues() {
		List<Integer> arrayList = new ArrayList<>(List.of(4, 5, 6));
		
		ArrayListClass.f3(arrayList);
		eventLog = new EventLog(EventLogger.getEvents());
		eventLog.setThread(getThread());
		eventLog.setIndex(eventLog.getLastIndex(getThread()));
		arrayList.clear();
		
		assertEquals(Arrays.asList(4, 5, 6, 3, 2, 1), eventLog.getObjectSnapshot(arrayList, eventLog.getEvents()).get());
	}
	
	@Test
	public void validForwardedMethods() {
		for(SnapshotCreator snapshotCreator:EventLogger.snapshotCreators) {
			for(String method:snapshotCreator.getForwardedMethods()) {
				String name = method.replaceAll("\\(.*", "");
				String descriptor = method.replaceAll(".*?\\(", "(");
				
				if(AsmUtil.getMethod(snapshotCreator.createObject().getClass(), name, descriptor) == null) {
					throw new AssertionError("Invalid method: " + method);
				}
			}
		}
	}
}