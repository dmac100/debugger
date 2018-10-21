package debugger.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import debugger.event.Events.EnterStaticEvent;
import debugger.event.Events.EnterVirtualEvent;
import debugger.event.Events.Event;
import debugger.event.Events.ExitWithExceptionEvent;
import debugger.event.Events.ExitWithValueEvent;
import debugger.event.Events.InvokeMethodEvent;

public class EventLog {
	private final List<Event> events;
	
	public static class CallStackNode {
		private final List<CallStackNode> children = new ArrayList<>();
		private CallStackNode parentNode;
		private int methodIndex;
		private String className;
		private String methodName;
		private List<Object> arguments;
		private Optional<Object> returnValue = Optional.empty();
		private Optional<Throwable> exception = Optional.empty();
		
		public List<CallStackNode> getChildren() {
			return children;
		}
		
		public String toString() {
			return className + "." + methodName + "(" + arguments + ")"
				+ returnValue.map(v -> " - " + v).orElse("")
				+ exception.map(e -> " - " + e.getClass().getName()).orElse("");
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
			if(event instanceof EnterVirtualEvent) {
				EnterVirtualEvent enterVirtualEvent = ((EnterVirtualEvent) event);
				CallStackNode node = new CallStackNode();
				node.methodIndex = enterVirtualEvent.methodIndex;
				node.parentNode = currentNode;
				node.methodName = enterVirtualEvent.name;
				node.className = enterVirtualEvent.object.getClass().getName();
				node.arguments = Arrays.asList(enterVirtualEvent.args);
				if(currentNode == null) {
					nodes.add(node);
					currentNode = node;
				} else {
					currentNode.children.add(node);
					currentNode = getLast(currentNode.children);
				}
			}
			if(event instanceof EnterStaticEvent) {
				EnterStaticEvent enterStaticEvent = ((EnterStaticEvent) event);
				CallStackNode node = new CallStackNode();
				node.methodIndex = enterStaticEvent.methodIndex;
				node.parentNode = currentNode;
				node.methodName = enterStaticEvent.name;
				node.className = enterStaticEvent.className;
				node.arguments = Arrays.asList(enterStaticEvent.args);
				if(currentNode == null) {
					nodes.add(node);
					currentNode = node;
				} else {
					currentNode.children.add(node);
					currentNode = getLast(currentNode.children);
				}
			}
			if(event instanceof ExitWithValueEvent) {
				ExitWithValueEvent exitWithValueEvent = ((ExitWithValueEvent) event);
				currentNode.methodIndex = exitWithValueEvent.methodIndex;
				currentNode.returnValue = Optional.of(exitWithValueEvent.value);
				currentNode = currentNode.parentNode;
			}
			if(event instanceof ExitWithExceptionEvent) {
				ExitWithExceptionEvent exitWithExceptionEvent = ((ExitWithExceptionEvent) event);
				currentNode.methodIndex = exitWithExceptionEvent.methodIndex;
				currentNode.exception = Optional.of(exitWithExceptionEvent.throwable);
				currentNode = currentNode.parentNode;
			}
			/*
			if(event instanceof InvokeMethodEvent) {
				InvokeMethodEvent invokeMethodEvent = ((InvokeMethodEvent) event);
				CallStackNode node = new CallStackNode();
				node.methodName = invokeMethodEvent.name;
				node.className = invokeMethodEvent.object.getClass().getName();
				node.arguments = Arrays.asList(invokeMethodEvent.args);
				if(currentNode == null) {
					nodes.add(node);
				} else {
					currentNode.children.add(node);
				}
			}
			if(event instanceof InvokeSpecialMethodEvent) {
				InvokeSpecialMethodEvent invokeSpecialMethodEvent = ((InvokeSpecialMethodEvent) event);
				CallStackNode node = new CallStackNode();
				node.methodName = invokeSpecialMethodEvent.name;
				node.className = invokeSpecialMethodEvent.object.getClass().getName();
				node.arguments = Arrays.asList(invokeSpecialMethodEvent.args);
				nodes.add(node);
			}
			if(event instanceof InvokeStaticMethodEvent) {
				InvokeStaticMethodEvent invokeStaticMethodEvent = ((InvokeStaticMethodEvent) event);
				CallStackNode node = new CallStackNode();
				node.methodName = invokeStaticMethodEvent.name;
				node.className = invokeStaticMethodEvent.className;
				node.arguments = Arrays.asList(invokeStaticMethodEvent.args);
				nodes.add(node);
			}
			*/
		}
		
		return nodes;
	}

	private static <T> T getLast(List<T> list) {
		return list.get(list.size() - 1);
	}
}