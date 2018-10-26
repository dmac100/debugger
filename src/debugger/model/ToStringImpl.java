package debugger.model;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Map;

import debugger.event.Events;

public class ToStringImpl {
	public static String toString(Object value) {
		if(value == null) {
			return "null";
		}
		if(value instanceof List) {
			return toString((List<?>) value);
		}
		if(value instanceof Map) {
			return toString((Map<?, ?>) value);
		}
		return simpleType(value.getClass()) ? String.valueOf(value) : Events.getObjectName(value);
	}
	
	private static String toString(Map<?, ?> map) {
		StringBuilder s = new StringBuilder();
		map.forEach((k, v) -> {
			if(s.length() != 0) {
				s.append(", ");
			}
			s.append(k);
			s.append("=");
			s.append(v);
		});
		return "{" + s.toString() + "}";
	}
	
	private static String toString(List<?> list) {
		return list.stream()
			.map(value -> toString(value))
			.collect(toList())
			.toString();
	}
	
	private static boolean simpleType(Class<?> type) {
		if(type.isPrimitive()) return true;
		if(type == Boolean.class) return true;
		if(type == Byte.class) return true;
		if(type == Character.class) return true;
		if(type == Short.class) return true;
		if(type == Integer.class) return true;
		if(type == Long.class) return true;
		if(type == Float.class) return true;
		if(type == Double.class) return true;
		if(type == String.class) return true;
		return false;
	}

}
