/*
 * Copyright 2002-2004 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.webflow.builder;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.springframework.binding.mapping.ParameterizableAttributeMapper;
import org.springframework.binding.method.MethodKey;
import org.springframework.core.enums.LabeledEnum;
import org.springframework.core.io.ClassPathResource;
import org.springframework.webflow.Action;
import org.springframework.webflow.ActionState;
import org.springframework.webflow.DecisionState;
import org.springframework.webflow.EndState;
import org.springframework.webflow.Event;
import org.springframework.webflow.Flow;
import org.springframework.webflow.FlowArtifactException;
import org.springframework.webflow.FlowAttributeMapper;
import org.springframework.webflow.FlowSessionStatus;
import org.springframework.webflow.RequestContext;
import org.springframework.webflow.SubflowState;
import org.springframework.webflow.Transition;
import org.springframework.webflow.ViewState;
import org.springframework.webflow.action.FlowVariableCreatingAction;
import org.springframework.webflow.action.LocalBeanInvokingAction;
import org.springframework.webflow.action.MultiAction;
import org.springframework.webflow.registry.NoSuchFlowDefinitionException;
import org.springframework.webflow.support.ParameterizableFlowAttributeMapper;
import org.springframework.webflow.support.SimpleViewSelector;
import org.springframework.webflow.support.StaticTransitionTargetStateResolver;
import org.springframework.webflow.support.TransitionExecutingStateExceptionHandler;
import org.springframework.webflow.test.MockRequestContext;

/**
 * Test case for XML flow builder.
 * 
 * @see org.springframework.webflow.builder.XmlFlowBuilder
 * 
 * @author Erwin Vervaet
 */
public class XmlFlowBuilderTests extends TestCase {

	private Flow flow;

	private MockRequestContext context;

	protected void setUp() throws Exception {
		XmlFlowBuilder builder = new XmlFlowBuilder(new ClassPathResource("testFlow1.xml", XmlFlowBuilderTests.class),
				new TestFlowArtifactFactory());
		new FlowAssembler("testFlow1", builder).assembleFlow();
		flow = builder.getResult();
		context = new MockRequestContext();
	}

	private Event createEvent(String id) {
		return new Event(this, id);
	}

	public void testBuildResult() {
		assertNotNull(flow);
		assertEquals("testFlow1", flow.getId());
		assertEquals("actionState1", flow.getStartState().getId());
		assertEquals(14, flow.getStateIds().length);

		assertEquals(1, flow.getStartActionList().size());
		assertTrue(flow.getStartActionList().get(0) instanceof FlowVariableCreatingAction);
		assertEquals(1, flow.getExceptionHandlerSet().size());
		assertTrue(flow.getExceptionHandlerSet().toArray()[0] instanceof TransitionExecutingStateExceptionHandler);

		ActionState actionState1 = (ActionState)flow.getState("actionState1");
		assertNotNull(actionState1);
		assertEquals(2, actionState1.getActionList().size());
		assertEquals(null, actionState1.getActionList().getAnnotated(0).getCaption());
		assertEquals(Boolean.TRUE, (Boolean)actionState1.getProperty("propBoolean"));
		assertEquals("aString", actionState1.getProperty("propString"));
		assertEquals("action2Name", actionState1.getActionList().getAnnotated(1).getName());
		assertEquals(3, actionState1.getTransitions().length);
		context.setLastEvent(createEvent("event1"));
		assertTrue(actionState1.hasTransitionFor(context));
		Transition transition = actionState1.getRequiredTransition(context);
		assertEquals("viewState1", getTargetStateId(transition));
		context.setLastEvent(createEvent("action2Name.event2"));
		assertTrue(actionState1.hasTransitionFor(context));
		transition = actionState1.getRequiredTransition(context);
		assertEquals("viewState2",  getTargetStateId(transition));
		assertEquals("prop1Value", actionState1.getActionList().getAnnotated(0).getProperties().get("prop1"));
		assertEquals("prop2Value", actionState1.getActionList().getAnnotated(0).getProperties().get("prop2"));

		ActionState actionState2 = (ActionState)flow.getState("actionState2");
		assertEquals(1, actionState2.getExceptionHandlerSet().size());

		ViewState viewState1 = (ViewState)flow.getState("viewState1");
		assertNotNull(viewState1);
		assertFalse(viewState1.isMarker());
		assertEquals("view1", ((SimpleViewSelector)viewState1.getViewSelector()).getViewName());
		assertEquals(1, viewState1.getTransitions().length);
		context.setLastEvent(createEvent("event1"));
		assertTrue(viewState1.hasTransitionFor(context));
		transition = viewState1.getRequiredTransition(context);
		assertEquals("subFlowState1",  getTargetStateId(transition));

		ViewState viewState2 = (ViewState)flow.getState("viewState2");
		assertNotNull(viewState2);
		assertTrue(viewState2.isMarker());
		assertNull(viewState2.getViewSelector());
		assertEquals(1, viewState2.getTransitions().length);
		context.setLastEvent(createEvent("event2"));
		assertTrue(viewState2.hasTransitionFor(context));
		transition = viewState2.getRequiredTransition(context);
		assertEquals("subFlowState2",  getTargetStateId(transition));

		SubflowState subFlowState1 = (SubflowState)flow.getState("subFlowState1");
		assertNotNull(subFlowState1);
		assertNotNull(subFlowState1.getSubflow());
		assertEquals("subFlow1", subFlowState1.getSubflow().getId());
		assertNotNull(subFlowState1.getAttributeMapper());
		assertEquals(1, subFlowState1.getTransitions().length);
		context.setLastEvent(createEvent("finish"));
		assertTrue(subFlowState1.hasTransitionFor(context));
		transition = subFlowState1.getRequiredTransition(context);
		assertEquals("spawnInlineFlow",  getTargetStateId(transition));

		SubflowState subFlowState2 = (SubflowState)flow.getState("subFlowState2");
		assertNotNull(subFlowState2);
		assertNotNull(subFlowState2.getSubflow());
		assertEquals("subFlow2", subFlowState2.getSubflow().getId());
		assertNotNull(subFlowState2.getAttributeMapper());
		assertEquals(1, subFlowState2.getTransitions().length);
		context.setLastEvent(createEvent("finish"));
		assertTrue(subFlowState2.hasTransitionFor(context));
		transition = subFlowState2.getRequiredTransition(context);
		assertEquals("decisionState1",  getTargetStateId(transition));

		ParameterizableFlowAttributeMapper mapper = (ParameterizableFlowAttributeMapper)subFlowState2
				.getAttributeMapper();
		assertEquals(3, ((ParameterizableAttributeMapper)mapper.getInputMapper()).getMappings().length);
		assertEquals(4, ((ParameterizableAttributeMapper)mapper.getOutputMapper()).getMappings().length);

		DecisionState decisionState1 = (DecisionState)flow.getState("decisionState1");
		assertTrue(decisionState1.getTransitions().length == 2);
		assertNotNull(decisionState1);
		assertNull(decisionState1.getAction());

		DecisionState decisionState2 = (DecisionState)flow.getState("decisionState2");
		assertTrue(decisionState2.getTransitions().length == 2);
		assertNotNull(decisionState2);
		assertNull(decisionState2.getAction());

		DecisionState decisionState3 = (DecisionState)flow.getState("decisionState3");
		assertTrue(decisionState3.getTransitions().length == 2);
		assertNotNull(decisionState3);
		assertNotNull(decisionState3.getAction());
		assertEquals(new MethodKey("booleanMethod"), decisionState3.getAnnotatedAction().getProperty("method"));
		assertTrue(decisionState3.getAnnotatedAction().getTargetAction() instanceof LocalBeanInvokingAction);

		DecisionState decisionState4 = (DecisionState)flow.getState("decisionState4");
		assertTrue(decisionState4.getTransitions().length == 2);
		assertNotNull(decisionState4);
		assertNotNull(decisionState4.getAction());
		assertEquals(new MethodKey("enumMethod"), decisionState4.getAnnotatedAction().getProperty("method"));
		assertTrue(decisionState4.getAnnotatedAction().getTargetAction() instanceof LocalBeanInvokingAction);

		EndState endState1 = (EndState)flow.getState("endState1");
		assertNotNull(endState1);
		assertFalse(endState1.isMarker());
		assertEquals("endView1", ((SimpleViewSelector)endState1.getViewSelector()).getViewName());

		EndState endState2 = (EndState)flow.getState("endState2");
		assertNotNull(endState2);
		assertTrue(endState2.isMarker());
		assertNull(endState2.getViewSelector());

		Flow inlineFlow = flow.getInlineFlow("inline-flow");
		assertNotNull(inlineFlow);
		EndState endState3 = (EndState)inlineFlow.getState("end");
		assertNotNull(endState3);
		assertEquals(1, endState3.getOutputAttributeNames().length);
		assertEquals("foo", endState3.getOutputAttributeNames()[0]);
	}

	protected String getTargetStateId(Transition transition) {
		 return ((StaticTransitionTargetStateResolver)transition.getTargetStateResolver()).getTargetStateId();
	}
	/**
	 * Flow service locator for the services needed by the testFlow (defined in
	 * testFlow.xml)
	 * 
	 * @author Erwin Vervaet
	 */
	public static class TestFlowArtifactFactory extends FlowArtifactFactoryAdapter {

		public Flow getSubflow(String id) throws FlowArtifactException {
			if ("subFlow1".equals(id) || "subFlow2".equals(id)) {
				Flow flow = new Flow(id);
				new EndState(flow, "finish");
				return flow;
			}
			throw new NoSuchFlowDefinitionException(id);
		}

		public Action getAction(FlowArtifactParameters actionParameters) throws FlowArtifactException {
			String id = actionParameters.getId();
			if ("action1".equals(id) || "action2".equals(id)) {
				return new TestAction();
			}
			if ("multiAction".equals(id)) {
				return new TestMultiAction();
			}
			if ("pojoAction".equals(id)) {
				return new LocalBeanInvokingAction(new TestPojo());
			}
			throw new FlowArtifactException(id, Action.class);
		}

		public FlowAttributeMapper getAttributeMapper(String id) throws FlowArtifactException {
			if ("attributeMapper1".equals(id)) {
				return new FlowAttributeMapper() {
					public Map createSubflowInput(RequestContext context) {
						return new HashMap();
					}

					public void mapSubflowOutput(Map subflowOutput, RequestContext context) {
					}
				};
			}
			throw new FlowArtifactException(id, FlowAttributeMapper.class);
		}
	};

	public static class TestAction implements Action {
		public Event execute(RequestContext context) throws Exception {
			if (context.getFlowExecutionContext().getRootFlow().containsProperty("scenario2")) {
				return new Event(this, "event2");
			}
			return new Event(this, "event1");
		}
	}

	public static class TestMultiAction extends MultiAction {
		public Event actionMethod(RequestContext context) throws Exception {
			throw new MyCustomException("Oops!");
		}
	}

	public static class TestPojo {
		public boolean booleanMethod() {
			return true;
		}

		public LabeledEnum enumMethod() {
			return FlowSessionStatus.CREATED;
		}
	}

	public static class TestFlowAttributeMapper implements FlowAttributeMapper {
		public Map createSubflowInput(RequestContext context) {
			return new HashMap();
		}

		public void mapSubflowOutput(Map subflowOutput, RequestContext context) {
		}
	}

}