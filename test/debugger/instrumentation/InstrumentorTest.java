package debugger.instrumentation;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import debugger.event.EventLogger;

public class InstrumentorTest {
	public static class InstanceVariablesClass {
		private boolean a = true;
		private byte b = 1;
		private char c = '2';
		private short d = 3;
		private int e = 4;
		private long f = 5;
		private float g = 6;
		private double h = 7;
		private Object i = null;
		private Object[] j = null;
	}

	public static class TestMethodsClass {
		public void localVariableTypes() {
			boolean a = true;
			byte b = 1;
			char c = '2';
			short d = 3;
			int e = 4;
			long f = 5;
			float g = 6;
			double h = 7;
			Object i = null;
			Object[] j = null;
		}
		
		public void simpleVoidMethod() {
			return;
		}
		
		public int simpleIntMethod() {
			return 2;
		}
		
		public float simpleFloatMethod() {
			return 2;
		}
		
		public double simpleDoubleMethod() {
			return 2;
		}
		
		public long simpleLongMethod() {
			return 2;
		}
		
		public String simpleStringMethod() {
			return "2";
		}
		
		public void callSimpleVoidMethod() {
			simpleVoidMethod();
		}
		
		public void callSimpleIntMethod() {
			simpleIntMethod();
		}
		
		public void callSimpleFloatMethod() {
			simpleFloatMethod();
		}
		
		public void callSimpleDoubleMethod() {
			simpleDoubleMethod();
		}
		
		public void callSimpleLongMethod() {
			simpleLongMethod();
		}
		
		public void callSimpleStringMethod() {
			simpleStringMethod();
		}
		
		public int incrementVariable() {
			int x = 1;
			x++;
			return x;
		}
		
		public void writeArray() {
			int[] a = new int[5];
			for(int x = 0; x < a.length; x++) {
				a[x] = x + 10;
			}
		}
		
		public void writeOtherArrays() {
			int[] a = { 1 };
			float[] b = { 2 };
			double[] c = { 3 };
			long[] d = { 4 };
			Object[] e = { "5" };
		}
		
		public int recursiveMethod(int n, boolean b) {
			return (n <= 1) ? 1 : n * recursiveMethod(n - 1, b);
		}
		
		public int intParameterMethod(int x, int y) {
			return x + y;
		}
		
		public static int staticIntParameterMethod(int x, int y) {
			return x + y;
		}
		
		public int callParameterTypes() {
			return parameterTypes(true, (byte) 1, '2', (short) 3, 4, 5, 6, 7, null, null);
		}
		
		public void callStaticMethod() {
			Integer.valueOf(1);
			Integer.valueOf(2);
			Integer.valueOf(3);
		}
		
		public void callConstructor() {
			new String("abc");
		}
		
		public int parameterTypes(boolean a, byte b, char c, short d, int e, long f, float g, double h, Object i, Object[] j) {
			return 1;
		}
		
		public int throwException() {
			try {
				throw new RuntimeException("Test Exception");
			} catch(RuntimeException e) {
				return 2;
			}
		}
		
		public int throwUncaughtException() {
			throw new RuntimeException("Test Exception");
		}
	}
	
	@Before
	public void before() {
		EventLogger.clear();
		new Instrumentor().instrumentClass(InstanceVariablesClass.class);
		new Instrumentor().instrumentClass(TestMethodsClass.class);
	}
	
	@Test
	public void writeLocalVariables() {
		new TestMethodsClass().localVariableTypes();
		
		assertLog(Arrays.asList("STORE:"), Arrays.asList(
			"STORE: 1, 1",
			"STORE: 2, 1",
			"STORE: 3, 50",
			"STORE: 4, 3",
			"STORE: 5, 4",
			"STORE: 6, 5",
			"STORE: 8, 6.0",
			"STORE: 9, 7.0",
			"STORE: 11, null",
			"STORE: 12, null"
		));
	}
	
	@Test
	public void writeInstanceVariables() {
		new InstanceVariablesClass();
		assertLog(Arrays.asList("PUT FIELD:"), Arrays.asList(
			"PUT FIELD: InstanceVariablesClass-1, a, true",
			"PUT FIELD: InstanceVariablesClass-1, b, 1",
			"PUT FIELD: InstanceVariablesClass-1, c, 2",
			"PUT FIELD: InstanceVariablesClass-1, d, 3",
			"PUT FIELD: InstanceVariablesClass-1, e, 4",
			"PUT FIELD: InstanceVariablesClass-1, f, 5",
			"PUT FIELD: InstanceVariablesClass-1, g, 6.0",
			"PUT FIELD: InstanceVariablesClass-1, h, 7.0",
			"PUT FIELD: InstanceVariablesClass-1, i, null",
			"PUT FIELD: InstanceVariablesClass-1, j, null"
		));
	}
	
	@Test
	public void invokeRecursiveMethod() {
		new TestMethodsClass().recursiveMethod(7, true);
		assertLog(Arrays.asList("INVOKE:"), Arrays.asList(
			"INVOKE: TestMethodsClass-1, recursiveMethod, (IZ)I, [6, true]",
			"INVOKE: TestMethodsClass-1, recursiveMethod, (IZ)I, [5, true]",
			"INVOKE: TestMethodsClass-1, recursiveMethod, (IZ)I, [4, true]",
			"INVOKE: TestMethodsClass-1, recursiveMethod, (IZ)I, [3, true]",
			"INVOKE: TestMethodsClass-1, recursiveMethod, (IZ)I, [2, true]",
			"INVOKE: TestMethodsClass-1, recursiveMethod, (IZ)I, [1, true]"
		));
	}
	
	@Test
	public void returnSimpleIntMethod() {
		new TestMethodsClass().simpleIntMethod();
		assertLog(Arrays.asList("RETURN:"), Arrays.asList(
			"RETURN: null",
			"RETURN: 2"
		));
	}
	
	@Test
	public void returnSimpleLongMethod() {
		new TestMethodsClass().simpleLongMethod();
		assertLog(Arrays.asList("RETURN:"), Arrays.asList(
			"RETURN: null",
			"RETURN: 2"
		));
	}
	
	@Test
	public void returnSimpleFloatMethod() {
		new TestMethodsClass().simpleFloatMethod();
		assertLog(Arrays.asList("RETURN:"), Arrays.asList(
			"RETURN: null",
			"RETURN: 2.0"
		));
	}
	
	@Test
	public void returnSimpleDoubleMethod() {
		new TestMethodsClass().simpleDoubleMethod();
		assertLog(Arrays.asList("RETURN:"), Arrays.asList(
			"RETURN: null",
			"RETURN: 2.0"
		));
	}
	
	@Test
	public void returnSimpleStringMethod() {
		new TestMethodsClass().simpleStringMethod();
		assertLog(Arrays.asList("RETURN:"), Arrays.asList(
			"RETURN: null",
			"RETURN: 2"
		));
	}
	
	@Test
	public void returnSimpleVoidMethod() {
		new TestMethodsClass().simpleVoidMethod();
		assertLog(Arrays.asList("RETURN:"), Arrays.asList(
			"RETURN: null",
			"RETURN: null"
		));
	}
	
	@Test
	public void returnedSimpleIntMethod() {
		new TestMethodsClass().callSimpleIntMethod();
		assertLog(Arrays.asList("RETURNED:"), Arrays.asList(
			"RETURNED: null",
			"RETURNED: 2"
		));
	}
	
	@Test
	public void returnedSimpleFloatMethod() {
		new TestMethodsClass().callSimpleFloatMethod();
		assertLog(Arrays.asList("RETURNED:"), Arrays.asList(
			"RETURNED: null",
			"RETURNED: 2.0"
		));
	}
	
	@Test
	public void returnedSimpleDoubleMethod() {
		new TestMethodsClass().callSimpleDoubleMethod();
		assertLog(Arrays.asList("RETURNED:"), Arrays.asList(
			"RETURNED: null",
			"RETURNED: 2.0"
		));
	}
	
	@Test
	public void returnedSimpleLongMethod() {
		new TestMethodsClass().callSimpleLongMethod();
		assertLog(Arrays.asList("RETURNED:"), Arrays.asList(
			"RETURNED: null",
			"RETURNED: 2"
		));
	}
	
	@Test
	public void returnedSimpleVoidMethod() {
		new TestMethodsClass().callSimpleVoidMethod();
		assertLog(Arrays.asList("RETURNED:"), Arrays.asList(
			"RETURNED: null",
			"RETURNED: null"
		));
	}
	
	@Test
	public void returnRecursiveMethod() {
		new TestMethodsClass().recursiveMethod(7, true);
		assertLog(Arrays.asList("RETURN:"), Arrays.asList(
			"RETURN: null",
			"RETURN: 1",
			"RETURN: 2",
			"RETURN: 6",
			"RETURN: 24",
			"RETURN: 120",
			"RETURN: 720",
			"RETURN: 5040"
		));
	}
	
	@Test
	public void incrementVariable() {
		new TestMethodsClass().incrementVariable();
		assertLog(Arrays.asList("STORE:"), Arrays.asList(
			"STORE: 1, 1",
			"STORE: 1, 2"
		));
	}
	
	@Test
	public void writeArray() {
		new TestMethodsClass().writeArray();
		assertLog(Arrays.asList("STORE ARRAY:"), Arrays.asList(
			"STORE ARRAY: int[]-1, 0, 10",
			"STORE ARRAY: int[]-1, 1, 11",
			"STORE ARRAY: int[]-1, 2, 12",
			"STORE ARRAY: int[]-1, 3, 13",
			"STORE ARRAY: int[]-1, 4, 14"
		));
	}
	
	@Test
	public void writeOtherArrays() {
		new TestMethodsClass().writeOtherArrays();
		assertLog(Arrays.asList("STORE ARRAY:"), Arrays.asList(
			"STORE ARRAY: int[]-1, 0, 1",
			"STORE ARRAY: float[]-1, 0, 2.0",
			"STORE ARRAY: double[]-1, 0, 3.0",
			"STORE ARRAY: long[]-1, 0, 4",
			"STORE ARRAY: Object[]-1, 0, 5"
		));
	}
	
	@Test
	public void invokeStaticMethod() {
		new TestMethodsClass().callStaticMethod();
		assertLog(Arrays.asList("INVOKE STATIC:"), Arrays.asList(
			"INVOKE STATIC: java/lang/Integer, valueOf, (I)Ljava/lang/Integer;, [1]",
			"INVOKE STATIC: java/lang/Integer, valueOf, (I)Ljava/lang/Integer;, [2]",
			"INVOKE STATIC: java/lang/Integer, valueOf, (I)Ljava/lang/Integer;, [3]"
		));
	}
	
	@Test
	public void invokeConstructor() {
		new TestMethodsClass().callConstructor();
		assertLog(Arrays.asList("INVOKE SPECIAL:"), Arrays.asList(
			"INVOKE SPECIAL: String-1, <init>, ()V, []",
			"INVOKE SPECIAL: String-2, <init>, (Ljava/lang/String;)V, [abc]"
		));
	}
	
	@Test
	public void invokeParameterTypes() {
		new TestMethodsClass().callParameterTypes();
		assertLog(Arrays.asList("INVOKE:"), Arrays.asList(
			"INVOKE: TestMethodsClass-1, parameterTypes, (ZBCSIJFDLjava/lang/Object;[Ljava/lang/Object;)I, [true, 1, 2, 3, 4, 5, 6.0, 7.0, null, null]"
		));
	}
	
	@Test
	public void throwException() {
		new TestMethodsClass().throwException();
		assertLog(Arrays.asList("THROW:"), Arrays.asList(
			"THROW: java.lang.RuntimeException, Test Exception"
		));
	}
	
	@Test
	public void catchException() {
		new TestMethodsClass().throwException();
		assertLog(Arrays.asList("CATCH:"), Arrays.asList(
			"CATCH: java.lang.RuntimeException, Test Exception"
		));
	}
	
	@Test
	public void enter() {
		new TestMethodsClass().intParameterMethod(3, 4);
		assertLog(Arrays.asList("ENTER:"), Arrays.asList(
			"ENTER: TestMethodsClass-1, <init>, ()V, []",
			"ENTER: TestMethodsClass-1, intParameterMethod, (II)I, [3, 4]"
		));
	}
	
	@Test
	public void enterStatic() {
		TestMethodsClass.staticIntParameterMethod(3, 4);
		assertLog(Arrays.asList("ENTER STATIC:"), Arrays.asList(
			"ENTER STATIC: debugger/instrumentation/InstrumentorTest$TestMethodsClass, staticIntParameterMethod, (II)I, [3, 4]"
		));
	}
	
	@Test
	public void exitWithException() {
		try {
			new TestMethodsClass().throwUncaughtException();
		} catch(Exception e) {
		}
		assertLog(Arrays.asList("EXIT EXCEPTION:"), Arrays.asList(
			"EXIT EXCEPTION: java.lang.RuntimeException, Test Exception"
		));
	}
	
	@Test
	public void exitWithValue() {
		new TestMethodsClass().simpleIntMethod();
		assertLog(Arrays.asList("EXIT VALUE:"), Arrays.asList(
			"EXIT VALUE: null",
			"EXIT VALUE: 2"
		));
	}

	private void assertLog(List<String> filter, List<String> expectedLog) {
		Predicate<String> lineBeginsWithFilter = line -> filter.stream().anyMatch(f -> line.startsWith(f));
		List<String> filteredLog = EventLogger.getLog().stream().filter(lineBeginsWithFilter).collect(Collectors.toList());
		assertEquals(expectedLog, filteredLog);
	}
}