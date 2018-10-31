package debugger.model;

import static debugger.util.CollectionUtil.getLast;
import static debugger.util.CollectionUtil.lastIndexOf;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import debugger.event.EventLogger;
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
import debugger.event.SnapshotCreator;
import debugger.event.SnapshotEvent;
import debugger.instrumentation.util.AsmUtil;

public class EventLog {
	private final List<Event> events;
	private final List<Runnable> changeCallbacks = new ArrayList<>();
	
	private File sourceFile;
	
	private int currentIndex;
	private Thread currentThread;
	
	public EventLog(List<Event> events) {
		this.events = events;
	}
	
	public Runnable addChangeCallback(Runnable callback) {
		changeCallbacks.add(callback);
		callback.run();
		return () -> changeCallbacks.remove(callback);
	}
	
	private void fireChangeCallbacks() {
		new ArrayList<>(changeCallbacks).forEach(Runnable::run);
	}
	
	public List<Thread> getThreads() {
		return events.stream()
			.map(Event::getThread)
			.distinct()
			.sorted(Comparator.comparing(Thread::getName))
			.collect(Collectors.toList());
	}

	public List<CallStackNode> getCallStack() {
		CallStackNode rootNode = new CallStackNode();
		
		CallStackNode currentNode = rootNode;
		
		for(Event event:events) {
			if(event.thread != currentThread) {
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

	public Map<String, Object> getLocalVariables() {
		List<Map<Integer, Object>> localsStack = new ArrayList<>();
		List<Map<Integer, String>> localsNameStack = new ArrayList<>();
		
		for(Event event:getCurrentEvents()) {
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
	
	public Optional<Object> getObjectSnapshot(Object object, List<Event> events) {
		Stream<SnapshotCreator> compatibleSnapshotCreators = EventLogger.snapshotCreators.stream().filter(creator -> creator.isCompatibleType(object));
		return compatibleSnapshotCreators.findFirst().map(snapshotCreator -> {
			Object snapshotObject = snapshotCreator.createObject();
			
			Set<String> forwardedMethods = snapshotCreator.getForwardedMethods();
			
			for(Event event:getCurrentEvents()) {
				if(event instanceof InvokeMethodEvent) {
					InvokeMethodEvent invokeMethodEvent = (InvokeMethodEvent) event;
					if(object == invokeMethodEvent.object && snapshotCreator.isCompatibleType(invokeMethodEvent.object)) {
						if(forwardedMethods.contains(invokeMethodEvent.name + invokeMethodEvent.descriptor)) {
							try {
								Method method = AsmUtil.getMethod(snapshotObject.getClass(), invokeMethodEvent.name, invokeMethodEvent.descriptor);
								method.invoke(snapshotObject, invokeMethodEvent.args);
							} catch(ReflectiveOperationException e) {
								throw new RuntimeException("Error invoking method", e);
							}
						}
					}
				}
				
				if(event instanceof SnapshotEvent) {
					SnapshotEvent snapshotEvent = (SnapshotEvent) event;
					if(snapshotEvent.matchesObject(object)) {
						snapshotObject = snapshotEvent.getSnapshotObject();
					}
				}
			}
			
			return snapshotObject;
		});
	}

	private List<Event> getCurrentEvents() {
		List<Event> currentEvents = new ArrayList<>();
	
		for(int i = 0; i < currentIndex; i++) {
			Event event = events.get(i);
			
			if(event.thread == currentThread) {
				currentEvents.add(event);
			}
		}
		
		return currentEvents;
	}
	
	public int getLastIndex(Thread thread) {
		return lastIndexOf(events, e -> e.thread == thread && e instanceof ReturnValueEvent);
	}

	public void setIndex(int index) {
		this.currentIndex = index;
	}
	
	public void setThread(Thread thread) {
		this.currentThread = thread;
	}
	
	public void setSourceFile(File file) {
		this.sourceFile = file;
	}
	
	public File getSourceFile() {
		return sourceFile;
	}
	
	public List<Event> getEvents() {
		return events;
	}
}