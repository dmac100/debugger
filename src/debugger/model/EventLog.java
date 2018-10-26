package debugger.model;

import static debugger.util.CollectionUtil.getLast;
import static debugger.util.CollectionUtil.lastIndexOf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import debugger.event.Events.EnterMethodEvent;
import debugger.event.Events.Event;
import debugger.event.Events.ExitWithExceptionEvent;
import debugger.event.Events.ExitWithValueEvent;
import debugger.event.Events.InvokeMethodEvent;
import debugger.event.Events.InvokeSpecialMethodEvent;
import debugger.event.Events.InvokeStaticMethodEvent;
import debugger.event.Events.ReturnValueEvent;
import debugger.event.Events.ReturnedValueEvent;
import debugger.event.Events.SetLocalNameEvent;
import debugger.event.Events.StoreEvent;

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

	public List<CallStackNode> getCallStack(Thread thread) {
		CallStackNode rootNode = new CallStackNode();
		
		CallStackNode currentNode = rootNode;
		
		for(Event event:events) {
			if(event.thread != thread) {
				continue;
			}
			
			if(event instanceof EnterMethodEvent) {
				EnterMethodEvent enterMethodEvent = ((EnterMethodEvent) event);
				CallStackNode node = new CallStackNode();
				node.setMethodIndex(enterMethodEvent.methodIndex);
				node.setParentNode(currentNode);
				node.setMethodName(enterMethodEvent.name);
				node.setDescriptor(enterMethodEvent.descriptor);
				node.setClassName(enterMethodEvent.className);
				node.setArguments(Arrays.asList(enterMethodEvent.args));
				
				if(sameMethod(currentNode, node)) {
					currentNode.setMethodIndex(node.getMethodIndex());
				} else {
					currentNode = currentNode.addChild(node);
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
				currentNode = currentNode.addChild(node);
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
				currentNode = currentNode.addChild(node);
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
				currentNode = currentNode.addChild(node);
			}
			
			if(event instanceof ReturnedValueEvent) {
				ReturnedValueEvent returnedValueEvent = ((ReturnedValueEvent) event);
				if(currentNode != null && !currentNode.isInstrumented()) {
					currentNode.setReturnValue(returnedValueEvent.value);
					currentNode = currentNode.getParentNode();
				}
			}
		}
		
		return rootNode.getChildren();
	}

	private static boolean sameMethod(CallStackNode node1, CallStackNode node2) {
		if(!Objects.equals(node1.getMethodName(), node2.getMethodName())) return false;
		if(!Objects.equals(node1.getClassName(), node2.getClassName())) return false;
		if(!Objects.equals(node1.getDescriptor(), node2.getDescriptor())) return false;
		return true;
	}

	public Map<String, Object> getLocalVariablesAt(Thread thread, int index) {
		List<Map<Integer, Object>> localsStack = new ArrayList<>();
		List<Map<Integer, String>> localsNameStack = new ArrayList<>();
		
		for(int i = 0; i < index; i++) {
			Event event = events.get(i);
			
			if(event.thread != thread) {
				continue;
			}
			
			if(event instanceof EnterMethodEvent) {
				localsStack.add(new HashMap<>());
				localsNameStack.add(new HashMap<>());
			}
			
			if(event instanceof ExitWithValueEvent || event instanceof ExitWithExceptionEvent) {
				localsStack.remove(localsStack.size() - 1);
				localsNameStack.remove(localsNameStack.size() - 1);
			}
			
			if(event instanceof StoreEvent) {
				StoreEvent storeEvent = ((StoreEvent) event);
				getLast(localsStack).put(storeEvent.varIndex, storeEvent.value);
			}
			
			if(event instanceof SetLocalNameEvent) {
				SetLocalNameEvent setLocalNameEvent = ((SetLocalNameEvent) event);
				getLast(localsNameStack).put(setLocalNameEvent.index, setLocalNameEvent.name);
			}
		}
		
		Map<Integer, Object> locals = getLast(localsStack);
		Map<Integer, String> localsName = getLast(localsNameStack);
		
		Map<String, Object> localsByName = new LinkedHashMap<>();
		locals.forEach((k, v) -> {
			localsByName.put(localsName.getOrDefault(k, "local-" + k), v);
		});
		
		return localsByName;
	}

	public int getLastIndex(Thread thread) {
		return lastIndexOf(events, e -> e.thread == thread && e instanceof ReturnValueEvent);
	}
}