package org.springframework.webflow.support;

import junit.framework.TestCase;

import org.springframework.webflow.EndState;
import org.springframework.webflow.Flow;
import org.springframework.webflow.FlowExecutionControlContext;
import org.springframework.webflow.StateException;
import org.springframework.webflow.Transition;
import org.springframework.webflow.TransitionTargetStateResolver;
import org.springframework.webflow.TransitionableState;
import org.springframework.webflow.ViewSelection;
import org.springframework.webflow.builder.MyCustomException;
import org.springframework.webflow.execution.impl.FlowExecutionImpl;
import org.springframework.webflow.test.MockExternalContext;

public class TransitionExecutingStateExceptionHandlerTests extends TestCase {

	public void testTransitionExecutorHandlesException() {
		Flow flow = new Flow("myFlow");
		TransitionableState state1 = new TransitionableState(flow, "state1") {
			protected ViewSelection doEnter(FlowExecutionControlContext context) {
				throw new StateException(this, "Oops!", new MyCustomException());
			}
		};
		state1.addTransition(new Transition(to("end")));
		TransitionExecutingStateExceptionHandler handler = new TransitionExecutingStateExceptionHandler();
		handler.add(MyCustomException.class, "state1");
		StateException e = new StateException(state1, "Oops", new MyCustomException());
		assertTrue("Doesn't handle exception", handler.handles(e));
	}

	public void testFlowStateExceptionHandlingTransition() {
		Flow flow = new Flow("myFlow");
		TransitionableState state1 = new TransitionableState(flow, "exception") {
			protected ViewSelection doEnter(FlowExecutionControlContext context) {
				throw new StateException(this, "Oops!", new MyCustomException());
			}
		};
		state1.addTransition(new Transition(to("end")));
		EndState state2 = new EndState(flow, "end");
		state2.setViewSelector(new SimpleViewSelector("view"));
		TransitionExecutingStateExceptionHandler handler = new TransitionExecutingStateExceptionHandler();
		handler.add(MyCustomException.class, "end");
		flow.addExceptionHandler(handler);
		FlowExecutionImpl execution = new FlowExecutionImpl(flow);
		execution.start(new MockExternalContext());
		assertTrue("Should have ended", !execution.isActive());
	}

	public void testStateExceptionHandlingTransition() {
		Flow flow = new Flow("myFlow");
		TransitionableState state1 = new TransitionableState(flow, "state1") {
			protected ViewSelection doEnter(FlowExecutionControlContext context) {
				throw new StateException(this, "Oops!", new MyCustomException());
			}
		};
		state1.addTransition(new Transition(to("end")));
		EndState state2 = new EndState(flow, "end");
		state2.setViewSelector(new SimpleViewSelector("view"));
		TransitionExecutingStateExceptionHandler handler = new TransitionExecutingStateExceptionHandler();
		handler.add(MyCustomException.class, "end");
		state1.addExceptionHandler(handler);
		FlowExecutionImpl execution = new FlowExecutionImpl(flow);
		execution.start(new MockExternalContext());
		assertTrue("Should have ended", !execution.isActive());
	}

	public void testStateExceptionHandlingRethrow() {
		Flow flow = new Flow("myFlow");
		TransitionableState state1 = new TransitionableState(flow, "exception") {
			protected ViewSelection doEnter(FlowExecutionControlContext context) {
				throw new StateException(this, "Oops!", new MyCustomException());
			}
		};
		state1.addTransition(new Transition(to("end")));
		FlowExecutionImpl execution = new FlowExecutionImpl(flow);
		try {
			execution.start(new MockExternalContext());
			fail("Should have rethrown");
		}
		catch (StateException e) {
			// expected
		}
	}
	
	public static TransitionTargetStateResolver to(String stateId) {
		return new StaticTransitionTargetStateResolver(stateId);
	}
}