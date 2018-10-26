package debugger.model;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import debugger.event.Events;
import debugger.event.Events.EnterMethodEvent;
import debugger.event.Events.Event;
import debugger.event.Events.ExitWithExceptionEvent;
import debugger.event.Events.ExitWithValueEvent;
import debugger.event.Events.InvokeMethodEvent;
import debugger.event.Events.InvokeSpecialMethodEvent;
import debugger.event.Events.InvokeStaticMethodEvent;
import debugger.event.Events.ReturnedValueEvent;

public class EventLog {
	private final List<Event> events;
	
	public static class CallStackNode {
		private final List<CallStackNode> children = new ArrayList<>();
		private CallStackNode parentNode;
		private int methodIndex;
		private String className;
		private String methodName;
		private String descriptor;
		private List<Object> arguments;
		private Object returnValue;
		private Throwable exception;
		
		public List<CallStackNode> getChildren() {
			return children;
		}
		
		public int getMethodIndex() {
			return methodIndex;
		}
		
		public boolean isInstrumented() {
			return methodIndex >= 0;
		}
		
		public String toString() {
			return className + "." + methodName + "(" + toString(arguments) + ")"
				+ (exception == null ? " - " + toString(returnValue) : "")
				+ (exception != null ? " - " + exception.getClass().getName().replace('.', '/') : "");
		}

		private static String toString(List<Object> list) {
			return list.stream()
				.map(value -> toString(value))
				.collect(toList())
				.toString();
		}
		
		private static String toString(Object value) {
			if(value == null) {
				return "null";
			}
			if(value instanceof List) {
				return toString((List<Object>) value);
			}
			return simpleType(value.getClass()) ? String.valueOf(value) : Events.getObjectName(value);
		}
		
		private static boolean simpleType(Class<?> type) {
			if(type.isPrimitive()) return true;
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

	public List<CallStackNode> getCallStack() {
		List<CallStackNode> nodes = new ArrayList<>();
		
		CallStackNode currentNode = null;
		
		for(Event event:events) {
			if(event instanceof EnterMethodEvent) {
				EnterMethodEvent enterMethodEvent = ((EnterMethodEvent) event);
				CallStackNode node = new CallStackNode();
				node.methodIndex = enterMethodEvent.methodIndex;
				node.parentNode = currentNode;
				node.methodName = enterMethodEvent.name;
				node.descriptor = enterMethodEvent.descriptor;
				node.className = enterMethodEvent.className;
				node.arguments = Arrays.asList(enterMethodEvent.args);
				if(currentNode == null) {
					nodes.add(node);
					currentNode = node;
				} else {
					if(sameMethod(currentNode, node)) {
						currentNode.methodIndex = node.methodIndex;
					} else {
						currentNode.children.add(node);
						currentNode = getLast(currentNode.children);
					}
				}
			}
			
			if(event instanceof ExitWithValueEvent) {
				ExitWithValueEvent exitWithValueEvent = ((ExitWithValueEvent) event);
				if(currentNode != null) {
					while(currentNode.methodIndex != exitWithValueEvent.methodIndex) {
						currentNode = currentNode.parentNode;
					}
					currentNode.returnValue = exitWithValueEvent.value;
					currentNode = currentNode.parentNode;
				}
			}
			
			if(event instanceof ExitWithExceptionEvent) {
				ExitWithExceptionEvent exitWithExceptionEvent = ((ExitWithExceptionEvent) event);
				if(currentNode != null) {
					while(currentNode.methodIndex != exitWithExceptionEvent.methodIndex) {
						currentNode = currentNode.parentNode;
					}
					currentNode.exception = exitWithExceptionEvent.throwable;
					currentNode = currentNode.parentNode;
				}
			}
			
			if(event instanceof InvokeStaticMethodEvent) {
				InvokeStaticMethodEvent invokeStaticMethodEvent = ((InvokeStaticMethodEvent) event);
				CallStackNode node = new CallStackNode();
				node.methodIndex = -1;
				node.parentNode = currentNode;
				node.methodName = invokeStaticMethodEvent.name;
				node.descriptor = invokeStaticMethodEvent.descriptor;
				node.className = invokeStaticMethodEvent.className;
				node.arguments = Arrays.asList(invokeStaticMethodEvent.args);
				if(currentNode == null) {
					nodes.add(node);
					currentNode = node;
				} else {
					currentNode.children.add(node);
					currentNode = getLast(currentNode.children);
				}
			}
			
			if(event instanceof InvokeMethodEvent) {
				InvokeMethodEvent invokeMethodEvent = ((InvokeMethodEvent) event);
				CallStackNode node = new CallStackNode();
				node.methodIndex = -1;
				node.parentNode = currentNode;
				node.methodName = invokeMethodEvent.name;
				node.descriptor = invokeMethodEvent.descriptor;
				node.className = invokeMethodEvent.object.getClass().getName().replace('.', '/');
				node.arguments = Arrays.asList(invokeMethodEvent.args);
				if(currentNode == null) {
					nodes.add(node);
					currentNode = node;
				} else {
					currentNode.children.add(node);
					currentNode = getLast(currentNode.children);
				}
			}
			
			if(event instanceof InvokeSpecialMethodEvent) {
				InvokeSpecialMethodEvent invokeSpecialMethodEvent = ((InvokeSpecialMethodEvent) event);
				CallStackNode node = new CallStackNode();
				node.methodIndex = -1;
				node.parentNode = currentNode;
				node.methodName = invokeSpecialMethodEvent.name;
				node.descriptor = invokeSpecialMethodEvent.descriptor;
				node.className = invokeSpecialMethodEvent.className;
				node.arguments = Arrays.asList(invokeSpecialMethodEvent.args);
				if(currentNode == null) {
					nodes.add(node);
					currentNode = node;
				} else {
					currentNode.children.add(node);
					currentNode = getLast(currentNode.children);
				}
			}
			
			if(event instanceof ReturnedValueEvent) {
				ReturnedValueEvent returnedValueEvent = ((ReturnedValueEvent) event);
				if(currentNode != null && !currentNode.isInstrumented()) {
					currentNode.returnValue = returnedValueEvent.value;
					currentNode = currentNode.parentNode;
				}
			}
		}
		
		return nodes;
	}

	private static boolean sameMethod(CallStackNode node1, CallStackNode node2) {
		if(!node1.methodName.equals(node2.methodName)) return false;
		if(!node1.className.equals(node2.className)) return false;
		if(!node1.descriptor.equals(node2.descriptor)) return false;
		return true;
	}

	private static <T> T getLast(List<T> list) {
		return list.get(list.size() - 1);
	}
}