package org.springframework.webflow.engine.impl;

import java.util.LinkedList;

import junit.framework.TestCase;

import org.springframework.webflow.core.collection.LocalAttributeMap;
import org.springframework.webflow.definition.FlowDefinition;
import org.springframework.webflow.definition.FlowId;
import org.springframework.webflow.definition.registry.FlowDefinitionConstructionException;
import org.springframework.webflow.definition.registry.FlowDefinitionLocator;
import org.springframework.webflow.definition.registry.NoSuchFlowDefinitionException;
import org.springframework.webflow.engine.Flow;
import org.springframework.webflow.execution.FlowExecution;
import org.springframework.webflow.execution.FlowExecutionKey;
import org.springframework.webflow.execution.FlowExecutionListener;
import org.springframework.webflow.execution.MockFlowExecutionListener;
import org.springframework.webflow.execution.factory.FlowExecutionKeyFactory;
import org.springframework.webflow.execution.factory.FlowExecutionListenerLoader;
import org.springframework.webflow.execution.factory.StaticFlowExecutionListenerLoader;

public class FlowExecutionStateRestorerImplTests extends TestCase {
	private SimpleFlowDefinitionLocator definitionLocator;
	private FlowExecutionImplStateRestorer stateRestorer;
	private LocalAttributeMap executionAttributes = new LocalAttributeMap();
	private FlowExecutionListener listener = new MockFlowExecutionListener();
	private FlowExecutionListenerLoader executionListenerLoader = new StaticFlowExecutionListenerLoader(listener);
	SimpleFlowExecutionKey newKey = new SimpleFlowExecutionKey();
	private FlowExecutionKeyFactory executionKeyFactory = new FlowExecutionKeyFactory() {
		public FlowExecutionKey getKey(FlowExecution execution) {
			return newKey;
		}
	};

	protected void setUp() {
		definitionLocator = new SimpleFlowDefinitionLocator();
		stateRestorer = new FlowExecutionImplStateRestorer(definitionLocator);
		stateRestorer.setExecutionAttributes(executionAttributes);
		stateRestorer.setExecutionListenerLoader(executionListenerLoader);
		stateRestorer.setExecutionKeyFactory(executionKeyFactory);
	}

	public void testRestoreStateNoSessions() {
		FlowExecutionKey key = new SimpleFlowExecutionKey();
		LocalAttributeMap conversationScope = new LocalAttributeMap();
		FlowExecutionImpl execution = new FlowExecutionImpl(FlowId.valueOf("parent"), new LinkedList());
		stateRestorer.restoreState(execution, key, conversationScope);
		assertSame(definitionLocator.parent, execution.getDefinition());
		assertTrue(execution.getFlowSessions().isEmpty());
		assertSame(conversationScope, execution.getConversationScope());
		assertSame(key, execution.getKey());
		assertSame(executionAttributes, execution.getAttributes());
		assertEquals(1, execution.getListeners().length);
		execution.assignKey();
		assertEquals(newKey, execution.getKey());
	}

	public void testRestoreStateFlowDefinitionIdNotSet() {
		FlowExecutionKey key = new SimpleFlowExecutionKey();
		LocalAttributeMap conversationScope = new LocalAttributeMap();
		FlowExecutionImpl execution = new FlowExecutionImpl();
		try {
			stateRestorer.restoreState(execution, key, conversationScope);
			fail("Should've failed");
		} catch (IllegalStateException e) {

		}
	}

	public void testRestoreStateFlowSessionsNotSet() {
		FlowExecutionKey key = new SimpleFlowExecutionKey();
		LocalAttributeMap conversationScope = new LocalAttributeMap();
		FlowExecutionImpl execution = new FlowExecutionImpl(FlowId.valueOf("parent"), null);
		try {
			stateRestorer.restoreState(execution, key, conversationScope);
			fail("Should've failed");
		} catch (IllegalStateException e) {

		}
	}

	private class SimpleFlowDefinitionLocator implements FlowDefinitionLocator {
		Flow parent = Flow.create("parent");
		Flow child = Flow.create("child");

		public FlowDefinition getFlowDefinition(FlowId flowId) throws NoSuchFlowDefinitionException,
				FlowDefinitionConstructionException {
			if (flowId.equals(parent.getId())) {
				return parent;
			} else if (flowId.equals(child.getId())) {
				return child;
			} else {
				throw new IllegalArgumentException(flowId.toString());
			}
		}
	}

	private class SimpleFlowExecutionKey extends FlowExecutionKey {

		public String toString() {
			return "simple";
		}

	}
}
