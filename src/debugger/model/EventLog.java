package debugger.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

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
				node.setMethodIndex(enterMethodEvent.methodIndex);
				node.setParentNode(currentNode);
				node.setMethodName(enterMethodEvent.name);
				node.setDescriptor(enterMethodEvent.descriptor);
				node.setClassName(enterMethodEvent.className);
				node.setArguments(Arrays.asList(enterMethodEvent.args));
				if(currentNode == null) {
					nodes.add(node);
					currentNode = node;
				} else {
					if(sameMethod(currentNode, node)) {
						currentNode.setMethodIndex(node.getMethodIndex());
					} else {
						currentNode.addChild(node);
						currentNode = node;
					}
				}
			}
			
			if(event instanceof ExitWithValueEvent) {
				ExitWithValueEvent exitWithValueEvent = ((ExitWithValueEvent) event);
				if(currentNode != null) {
					while(currentNode.getMethodIndex() != exitWithValueEvent.getMethodIndex()) {
						currentNode = currentNode.getParentNode();
					}
					currentNode.setReturnValue(exitWithValueEvent.value);
					currentNode = currentNode.getParentNode();
				}
			}
			
			if(event instanceof ExitWithExceptionEvent) {
				ExitWithExceptionEvent exitWithExceptionEvent = ((ExitWithExceptionEvent) event);
				if(currentNode != null) {
					while(currentNode.getMethodIndex() != exitWithExceptionEvent.getMethodIndex()) {
						currentNode = currentNode.getParentNode();
					}
					currentNode.setException(exitWithExceptionEvent.throwable);
					currentNode = currentNode.getParentNode();
				}
			}
			
			if(event instanceof InvokeStaticMethodEvent) {
				InvokeStaticMethodEvent invokeStaticMethodEvent = ((InvokeStaticMethodEvent) event);
				CallStackNode node = new CallStackNode();
				node.setMethodIndex(-1);
				node.setParentNode(currentNode);
				node.setMethodName(invokeStaticMethodEvent.name);
				node.setDescriptor(invokeStaticMethodEvent.descriptor);
				node.setClassName(invokeStaticMethodEvent.className);
				node.setArguments(Arrays.asList(invokeStaticMethodEvent.args));
				if(currentNode == null) {
					nodes.add(node);
					currentNode = node;
				} else {
					currentNode.addChild(node);
					currentNode = node;
				}
			}
			
			if(event instanceof InvokeMethodEvent) {
				InvokeMethodEvent invokeMethodEvent = ((InvokeMethodEvent) event);
				CallStackNode node = new CallStackNode();
				node.setMethodIndex(-1);
				node.setParentNode(currentNode);
				node.setMethodName(invokeMethodEvent.name);
				node.setDescriptor(invokeMethodEvent.descriptor);
				node.setClassName(invokeMethodEvent.object.getClass().getName().replace('.', '/'));
				node.setArguments(Arrays.asList(invokeMethodEvent.args));
				if(currentNode == null) {
					nodes.add(node);
					currentNode = node;
				} else {
					currentNode.addChild(node);
					currentNode = node;
				}
			}
			
			if(event instanceof InvokeSpecialMethodEvent) {
				InvokeSpecialMethodEvent invokeSpecialMethodEvent = ((InvokeSpecialMethodEvent) event);
				CallStackNode node = new CallStackNode();
				node.setMethodIndex(-1);
				node.setParentNode(currentNode);
				node.setMethodName(invokeSpecialMethodEvent.name);
				node.setDescriptor(invokeSpecialMethodEvent.descriptor);
				node.setClassName(invokeSpecialMethodEvent.className);
				node.setArguments(Arrays.asList(invokeSpecialMethodEvent.args));
				if(currentNode == null) {
					nodes.add(node);
					currentNode = node;
				} else {
					currentNode.addChild(node);
					currentNode = node;
				}
			}
			
			if(event instanceof ReturnedValueEvent) {
				ReturnedValueEvent returnedValueEvent = ((ReturnedValueEvent) event);
				if(currentNode != null && !currentNode.isInstrumented()) {
					currentNode.setReturnValue(returnedValueEvent.value);
					currentNode = currentNode.getParentNode();
				}
			}
		}
		
		return nodes;
	}

	private static boolean sameMethod(CallStackNode node1, CallStackNode node2) {
		if(!node1.getMethodName().equals(node2.getMethodName())) return false;
		if(!node1.getClassName().equals(node2.getClassName())) return false;
		if(!node1.getDescriptor().equals(node2.getDescriptor())) return false;
		return true;
	}
}