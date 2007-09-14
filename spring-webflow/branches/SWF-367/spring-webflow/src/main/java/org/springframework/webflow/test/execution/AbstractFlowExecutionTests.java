/*
 * Copyright 2004-2007 the original author or authors.
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
package org.springframework.webflow.test.execution;

import junit.framework.TestCase;

import org.springframework.util.Assert;
import org.springframework.webflow.context.ExternalContext;
import org.springframework.webflow.core.collection.MutableAttributeMap;
import org.springframework.webflow.core.collection.ParameterMap;
import org.springframework.webflow.definition.FlowDefinition;
import org.springframework.webflow.engine.impl.FlowExecutionImplFactory;
import org.springframework.webflow.execution.FlowExecution;
import org.springframework.webflow.execution.FlowExecutionException;
import org.springframework.webflow.execution.FlowExecutionFactory;
import org.springframework.webflow.test.MockExternalContext;

/**
 * Base class for integration tests that verify a flow executes as expected. Flow execution tests captured by subclasses
 * should test that a flow responds to all supported transition criteria correctly, transitioning to the correct states
 * and producing the expected results on the occurrence of possible external (user) events.
 * <p>
 * More specifically, a typical flow execution test case will test:
 * <ul>
 * <li>That the flow execution starts as expected given a request from an external context containing potential input
 * attributes (see the {@link #startFlow(MutableAttributeMap, ExternalContext)} variants).
 * <li>That given the set of supported state transition criteria a state executes the appropriate transition when a
 * matching event is signaled (with potential input request parameters, see the {@link #resume(ExternalContext)}
 * variants). A test case should be coded for each logical event that can occur, where an event drives a possible path
 * through the flow. The goal should be to exercise all possible paths of the flow. Use a test coverage tool like Clover
 * or Emma to assist with measuring your test's effectiveness.
 * <li>That given a transition that leads to an interactive state type (a view state or an end state) that the view
 * selection returned to the client matches what was expected and the current state of the flow matches what is
 * expected.
 * </ul>
 * <p>
 * A flow execution test can effectively automate and validate the orchestration required to drive an end-to-end
 * business task that spans several steps involving the user to complete. Such tests are a good way to test your system
 * top-down starting at the web-tier and pushing through all the way to the DB without having to deploy to a servlet or
 * portlet container. In addition, they can be used to effectively test a flow's execution (the web layer) standalone,
 * typically with a mock service layer. Both styles of testing are valuable and supported.
 * 
 * @author Keith Donald
 */
public abstract class AbstractFlowExecutionTests extends TestCase {

	/**
	 * The factory that will create the flow execution to test.
	 */
	private FlowExecutionFactory flowExecutionFactory;

	/**
	 * The flow execution running the flow when the test is active (runtime object).
	 */
	private FlowExecution flowExecution;

	/**
	 * Constructs a default flow execution test.
	 * @see #setName(String)
	 */
	public AbstractFlowExecutionTests() {
		super();
	}

	/**
	 * Constructs a flow execution test with given name.
	 * @param name the name of the test
	 * @since 1.0.2
	 */
	public AbstractFlowExecutionTests(String name) {
		super(name);
	}

	/**
	 * Gets the factory that will create the flow execution to test. This method will create the factory if it is not
	 * already set.
	 * @return the flow execution factory
	 * @see #createFlowExecutionFactory()
	 */
	protected FlowExecutionFactory getFlowExecutionFactory() {
		if (flowExecutionFactory == null) {
			flowExecutionFactory = createFlowExecutionFactory();
		}
		return flowExecutionFactory;
	}

	/**
	 * Creates an ExternalContext instance. Defaults to using {@link MockExternalContext}. Subclasses can override if
	 * they which to use another external context implementation.
	 * @param requestParameters request parameters to put into the external context (optional)
	 * @return a new ExternalContext instance
	 */
	protected ExternalContext createExternalContext(ParameterMap requestParameters) {
		return new MockExternalContext(requestParameters);
	}

	/**
	 * Start the flow execution to be tested.
	 * <p>
	 * Convenience operation that starts the execution with:
	 * <ul>
	 * <li>no input attributes
	 * <li>an empty {@link ExternalContext} with no environmental request parameters set
	 * </ul>
	 * @throws FlowExecutionException if an exception was thrown while starting the flow execution
	 */
	protected void startFlow() throws FlowExecutionException {
		startFlow(null, createExternalContext(null));
	}

	/**
	 * Start the flow execution to be tested.
	 * <p>
	 * Convenience operation that starts the execution with:
	 * <ul>
	 * <li>the specified input attributes, eligible for mapping by the root flow
	 * <li>an empty {@link ExternalContext} with no environmental request parameters set
	 * </ul>
	 * @param input the flow execution input attributes eligible for mapping by the root flow
	 * @throws FlowExecutionException if an exception was thrown while starting the flow execution
	 */
	protected void startFlow(MutableAttributeMap input) throws FlowExecutionException {
		startFlow(input, createExternalContext(null));
	}

	/**
	 * Start the flow execution to be tested.
	 * <p>
	 * This is the most flexible of the start methods. It allows you to specify:
	 * <ol>
	 * <li>a map of input attributes to pass to the flow execution, eligible for mapping by the root flow definition
	 * <li>an external context that provides the flow execution being tested access to the calling environment for this
	 * request
	 * </ol>
	 * @param input the flow execution input attributes eligible for mapping by the root flow
	 * @param context the external context providing information about the caller's environment, used by the flow
	 * execution during the start operation
	 * @throws FlowExecutionException if an exception was thrown while starting the flow execution
	 */
	protected void startFlow(MutableAttributeMap input, ExternalContext context) throws FlowExecutionException {
		flowExecution = getFlowExecutionFactory().createFlowExecution(getFlowDefinition());
		flowExecution.start(input, context);
	}

	/**
	 * Signal an occurrence of an event in the current state of the flow execution being tested.
	 * @param requestParameters request parameters needed by the flow execution to complete event processing
	 * @throws FlowExecutionException if an exception was thrown within a state of the resumed flow execution during
	 * event processing
	 */
	protected void resume(ParameterMap requestParameters) throws FlowExecutionException {
		resume(createExternalContext(requestParameters));
	}

	/**
	 * Signal an occurrence of an event in the current state of the flow execution being tested.
	 * <p>
	 * Note: signaling an event will cause state transitions to occur in a chain until control is returned to the
	 * caller. Control is returned once an "interactive" state type is entered: either a view state when the flow is
	 * paused or an end state when the flow terminates. Action states are executed without returning control, as their
	 * result always triggers another state transition, executed internally. Action states can also be executed in a
	 * chain like fashion (e.g. action state 1 (result), action state 2 (result), action state 3 (result), view state
	 * <control returns so view can be rendered>).
	 * <p>
	 * If you wish to verify expected behavior on each state transition (and not just when the view state triggers
	 * return of control back to the client), you have a few options:
	 * <p>
	 * First, you may implement standalone unit tests for your {@link org.springframework.webflow.execution.Action}
	 * implementations. There you can verify that an Action executes its logic properly in isolation. When you do this,
	 * you may mock or stub out services the Action implementation needs that are expensive to initialize. You can also
	 * verify there that the action puts everything in the flow or request scope it was expected to (to meet its
	 * contract with the view it is prepping for display, for example).
	 * <p>
	 * Second, you can attach one or more FlowExecutionListeners to the flow execution at start time within your test
	 * code, which will allow you to receive a callback on each state transition (among other points). It is recommended
	 * you extend {@link org.springframework.webflow.execution.FlowExecutionListenerAdapter} and only override the
	 * callback methods you are interested in.
	 * @param context the external context providing information about the caller's environment, used by the flow
	 * execution during the signal event operation
	 * @throws FlowExecutionException if an exception was thrown within a state of the resumed flow execution during
	 * event processing
	 */
	protected void resume(ExternalContext context) throws FlowExecutionException {
		Assert.state(flowExecution != null, "The flow execution to test is [null]; "
				+ "you must start the flow execution before you can signal an event against it!");
		flowExecution.resume(context);
	}

	// convenience accessors

	/**
	 * Returns the flow execution being tested.
	 * @return the flow execution
	 * @throws IllegalStateException the execution has not been started
	 */
	protected FlowExecution getFlowExecution() throws IllegalStateException {
		Assert.state(flowExecution != null,
				"The flow execution to test is [null]; you must start the flow execution before you can query it!");
		return flowExecution;
	}

	/**
	 * Returns the attribute in conversation scope. Conversation-scoped attributes are shared by all flow sessions.
	 * @param attributeName the name of the attribute
	 * @return the attribute value
	 */
	protected Object getConversationAttribute(String attributeName) {
		return getFlowExecution().getConversationScope().get(attributeName);
	}

	/**
	 * Returns the required attribute in conversation scope; asserts the attribute is present. Conversation-scoped
	 * attributes are shared by all flow sessions.
	 * @param attributeName the name of the attribute
	 * @return the attribute value
	 * @throws IllegalStateException if the attribute was not present
	 */
	protected Object getRequiredConversationAttribute(String attributeName) throws IllegalStateException {
		return getFlowExecution().getConversationScope().getRequired(attributeName);
	}

	/**
	 * Returns the required attribute in conversation scope; asserts the attribute is present and of the required type.
	 * Conversation-scoped attributes are shared by all flow sessions.
	 * @param attributeName the name of the attribute
	 * @return the attribute value
	 * @throws IllegalStateException if the attribute was not present or not of the required type
	 */
	protected Object getRequiredConversationAttribute(String attributeName, Class requiredType)
			throws IllegalStateException {
		return getFlowExecution().getConversationScope().getRequired(attributeName, requiredType);
	}

	/**
	 * Returns the attribute in flow scope. Flow-scoped attributes are local to the active flow session.
	 * @param attributeName the name of the attribute
	 * @return the attribute value
	 */
	protected Object getFlowAttribute(String attributeName) {
		return getFlowExecution().getActiveSession().getScope().get(attributeName);
	}

	/**
	 * Returns the required attribute in flow scope; asserts the attribute is present. Flow-scoped attributes are local
	 * to the active flow session.
	 * @param attributeName the name of the attribute
	 * @return the attribute value
	 * @throws IllegalStateException if the attribute was not present
	 */
	protected Object getRequiredFlowAttribute(String attributeName) throws IllegalStateException {
		return getFlowExecution().getActiveSession().getScope().getRequired(attributeName);
	}

	/**
	 * Returns the required attribute in flow scope; asserts the attribute is present and of the correct type.
	 * Flow-scoped attributes are local to the active flow session.
	 * @param attributeName the name of the attribute
	 * @return the attribute value
	 * @throws IllegalStateException if the attribute was not present or was of the wrong type
	 */
	protected Object getRequiredFlowAttribute(String attributeName, Class requiredType) throws IllegalStateException {
		return getFlowExecution().getActiveSession().getScope().getRequired(attributeName, requiredType);
	}

	/**
	 * Returns the attribute in flash scope. Flash-scoped attributes are local to the active flow session and cleared on
	 * the next user event.
	 * @param attributeName the name of the attribute
	 * @return the attribute value
	 */
	protected Object getFlashAttribute(String attributeName) {
		return getFlowExecution().getFlashScope().get(attributeName);
	}

	/**
	 * Returns the required attribute in flash scope; asserts the attribute is present. Flash-scoped attributes are
	 * local to the active flow session and cleared on the next user event.
	 * @param attributeName the name of the attribute
	 * @return the attribute value
	 * @throws IllegalStateException if the attribute was not present
	 */
	protected Object getRequiredFlashAttribute(String attributeName) throws IllegalStateException {
		return getFlowExecution().getFlashScope().getRequired(attributeName);
	}

	/**
	 * Returns the required attribute in flash scope; asserts the attribute is present and of the correct type.
	 * Flash-scoped attributes are local to the active flow session and cleared on the next user event.
	 * @param attributeName the name of the attribute
	 * @return the attribute value
	 * @throws IllegalStateException if the attribute was not present or was of the wrong type
	 */
	protected Object getRequiredFlashAttribute(String attributeName, Class requiredType) throws IllegalStateException {
		return getFlowExecution().getFlashScope().getRequired(attributeName, requiredType);
	}

	// assert helpers

	/**
	 * Assert that the active flow session is for the flow with the provided id.
	 * @param expectedActiveFlowId the flow id that should have a session active in the tested flow execution
	 */
	protected void assertActiveFlowEquals(String expectedActiveFlowId) {
		assertEquals("The active flow id '" + getFlowExecution().getActiveSession().getDefinition().getId()
				+ "' does not equal the expected active flow id '" + expectedActiveFlowId + "'", expectedActiveFlowId,
				getFlowExecution().getActiveSession().getDefinition().getId());
	}

	/**
	 * Assert that the entire flow execution is active; that is, it has not ended and has been started.
	 */
	protected void assertFlowExecutionActive() {
		assertTrue("The flow execution is not active but it should be", getFlowExecution().isActive());
	}

	/**
	 * Assert that the entire flow execution has ended; that is, it is no longer active.
	 */
	protected void assertFlowExecutionEnded() {
		assertTrue("The flow execution is still active but it should have ended", !getFlowExecution().isActive());
	}

	/**
	 * Assert that the current state of the flow execution equals the provided state id.
	 * @param expectedCurrentStateId the expected current state
	 */
	protected void assertCurrentStateEquals(String expectedCurrentStateId) {
		assertEquals("The current state '" + getFlowExecution().getActiveSession().getState().getId()
				+ "' does not equal the expected state '" + expectedCurrentStateId + "'", expectedCurrentStateId,
				getFlowExecution().getActiveSession().getState().getId());
	}

	/**
	 * Factory method to create the flow execution factory. Subclasses could override this if they want to use a custom
	 * flow execution factory or custom configuration of the flow execution factory, registering flow execution
	 * listeners for instance. The default implementation just returns a {@link FlowExecutionImplFactory} instance.
	 * @return the flow execution factory
	 */
	protected FlowExecutionFactory createFlowExecutionFactory() {
		return new FlowExecutionImplFactory();
	}

	/**
	 * Directly update the flow execution used by the test by setting it to given flow execution. Use this if you have
	 * somehow manipulated the flow execution being tested and want to continue the test with another flow execution.
	 * @param flowExecution the flow execution to use
	 */
	protected void updateFlowExecution(FlowExecution flowExecution) {
		this.flowExecution = flowExecution;
	}

	/**
	 * Returns the flow definition to be tested. Subclasses must implement.
	 * @return the flow definition
	 */
	protected abstract FlowDefinition getFlowDefinition();
}