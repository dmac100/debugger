package debugger.model;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Test;

public class ToStringImplTest {
	@Test
	public void simpleToString() {
		assertEquals("null", ToStringImpl.toString(null));
		assertEquals("1", ToStringImpl.toString(1));
		assertEquals("1", ToStringImpl.toString(1L));
		assertEquals("1.0", ToStringImpl.toString(1.0));
		assertEquals("1.0", ToStringImpl.toString(1.0f));
		assertEquals("false", ToStringImpl.toString(false));
		assertEquals("abc", ToStringImpl.toString("abc"));
	}
	
	@Test
	public void objectToString() {
		assertEquals("Object-1", ToStringImpl.toString(new Object()));
		assertEquals("Object-2", ToStringImpl.toString(new Object()));
		
		assertEquals("Exception-1", ToStringImpl.toString(new Exception()));
		assertEquals("Exception-2", ToStringImpl.toString(new Exception()));
	}

	@Test
	public void collectionsToString() {
		assertEquals("[1, 2, 3]", ToStringImpl.toString(Arrays.asList(1, 2, 3)));
		assertEquals("{a=1, b=2}", ToStringImpl.toString(new TreeMap<>(Map.of("a", 1, "b", 2))));
	}
}