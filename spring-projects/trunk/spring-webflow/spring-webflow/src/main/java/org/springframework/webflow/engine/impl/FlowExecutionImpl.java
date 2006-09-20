/*
 * Copyright 2002-2006 the original author or authors.
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
package org.springframework.webflow.engine.impl;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.style.ToStringCreator;
import org.springframework.util.Assert;
import org.springframework.webflow.context.ExternalContext;
import org.springframework.webflow.core.collection.AttributeMap;
import org.springframework.webflow.core.collection.CollectionUtils;
import org.springframework.webflow.core.collection.LocalAttributeMap;
import org.springframework.webflow.core.collection.MutableAttributeMap;
import org.springframework.webflow.definition.FlowDefinition;
import org.springframework.webflow.engine.Flow;
import org.springframework.webflow.engine.RequestControlContext;
import org.springframework.webflow.engine.State;
import org.springframework.webflow.engine.ViewState;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.FlowExecution;
import org.springframework.webflow.execution.FlowExecutionException;
import org.springframework.webflow.execution.FlowExecutionListener;
import org.springframework.webflow.execution.FlowSession;
import org.springframework.webflow.execution.FlowSessionStatus;
import org.springframework.webflow.execution.ViewSelection;

/**
 * <p>
 * Default implementation of FlowExecution that uses a stack-based data
 * structure to manage spawned flow sessions. This class is closely coupled with
 * package-private <code>FlowSessionImpl</code> and
 * <code>RequestControlContextImpl</code>. The three classes work together to
 * form a complete flow execution implementation based on a finite state
 * machine.
 * </p>
 * <p>
 * This implementation of FlowExecution is serializable so it can be safely
 * stored in an HTTP session or other persistent store such as a file, database,
 * or client-side form field. Once deserialized, the
 * {@link FlowExecutionImplStateRestorer} strategy is expected to be used to
 * restore the execution to a usable state.
 * </p>
 * 
 * @see FlowExecutionImplFactory
 * @see FlowExecutionImplStateRestorer
 * 
 * @author Keith Donald
 * @author Erwin Vervaet
 */
public class FlowExecutionImpl implements FlowExecution, Externalizable {

	private static final Log logger = LogFactory.getLog(FlowExecutionImpl.class);

	/**
	 * The execution's root flow; the top level flow that acts as the starting
	 * point for this flow execution.
	 * <p>
	 * Transient to support restoration by the
	 * {@link FlowExecutionImplStateRestorer}.
	 */
	private transient Flow flow;

	/**
	 * The stack of active, currently executing flow sessions. As subflows are
	 * spawned, they are pushed onto the stack. As they end, they are popped off
	 * the stack.
	 */
	private LinkedList flowSessions;

	/**
	 * A thread-safe listener list, holding listeners monitoring the lifecycle
	 * of this flow execution.
	 * <p>
	 * Transient to support restoration by the
	 * {@link FlowExecutionImplStateRestorer}.
	 */
	private transient FlowExecutionListeners listeners;

	/**
	 * A data structure for attributes shared by all flow sessions.
	 * <p>
	 * Transient to support restoration by the
	 * {@link FlowExecutionImplStateRestorer}.
	 */
	private transient MutableAttributeMap conversationScope;

	/**
	 * A data structure for runtime system execution attributes.
	 * <p>
	 * Transient and package private to support restoration by the
	 * {@link FlowExecutionImplStateRestorer}.
	 */
	private transient AttributeMap attributes;

	/**
	 * Set so the transient {@link #flow} field can be restored by the
	 * {@link FlowExecutionImplStateRestorer}.
	 */
	private String flowId;

	/**
	 * Default constructor required for externalizable serialization. Should NOT
	 * be called programmatically.
	 */
	public FlowExecutionImpl() {
	}

	/**
	 * Create a new flow execution executing the provided flow. This constructor
	 * is mainly used for testing
	 * @param flow the root flow of this flow execution
	 */
	public FlowExecutionImpl(Flow flow) {
		this(flow, new FlowExecutionListener[0], null);
	}

	/**
	 * Create a new flow execution executing the provided flow.
	 * @param flow the root flow of this flow execution
	 * @param listeners the listeners interested in flow execution lifecycle
	 * events
	 * @param attributes flow execution system attributes
	 */
	public FlowExecutionImpl(Flow flow, FlowExecutionListener[] listeners, AttributeMap attributes) {
		setFlow(flow);
		this.flowSessions = new LinkedList();
		this.listeners = new FlowExecutionListeners(listeners);
		this.attributes = (attributes != null ? attributes : CollectionUtils.EMPTY_ATTRIBUTE_MAP);
		this.conversationScope = new LocalAttributeMap();
		if (logger.isDebugEnabled()) {
			logger.debug("Created new execution of flow '" + flow.getId() + "'");
		}
	}

	public String getCaption() {
		return "execution of '" + flowId + "'";
	}

	// implementing FlowExecutionContext

	public FlowDefinition getDefinition() {
		return flow;
	}

	public boolean isActive() {
		return !flowSessions.isEmpty();
	}

	public FlowSession getActiveSession() {
		return getActiveSessionInternal();
	}

	public MutableAttributeMap getConversationScope() {
		return conversationScope;
	}

	public AttributeMap getAttributes() {
		return attributes;
	}

	// methods implementing FlowExecution

	public ViewSelection start(MutableAttributeMap input, ExternalContext externalContext)
			throws FlowExecutionException {
		Assert.state(!isActive(),
				"This flow is already executing -- you cannot call 'start(ExternalContext)' more than once");
		RequestControlContext context = createControlContext(externalContext);
		getListeners().fireRequestSubmitted(context);
		try {
			try {
				ViewSelection selectedView = context.start(flow, input);
				return pause(context, selectedView);
			}
			catch (FlowExecutionException e) {
				return pause(context, handleException(e, context));
			}
		}
		finally {
			getListeners().fireRequestProcessed(context);
		}
	}

	public ViewSelection signalEvent(String eventId, ExternalContext externalContext) throws FlowExecutionException {
		assertActive();
		if (logger.isDebugEnabled()) {
			logger.debug("Resuming this execution on user event '" + eventId + "'");
		}
		RequestControlContext context = createControlContext(externalContext);
		getListeners().fireRequestSubmitted(context);
		try {
			try {
				resume(context);
				Event event = new Event(externalContext, eventId, externalContext.getRequestParameterMap()
						.asAttributeMap());
				ViewSelection selectedView = context.signalEvent(event);
				return pause(context, selectedView);
			}
			catch (FlowExecutionException e) {
				return pause(context, handleException(e, context));
			}
		}
		finally {
			getListeners().fireRequestProcessed(context);
		}
	}

	public ViewSelection refresh(ExternalContext externalContext) throws FlowExecutionException {
		assertActive();
		if (logger.isDebugEnabled()) {
			logger.debug("Resuming this execution for refresh");
		}
		RequestControlContext context = createControlContext(externalContext);
		getListeners().fireRequestSubmitted(context);
		try {
			try {
				resume(context);
				State currentState = getCurrentState();
				if (!(currentState instanceof ViewState)) {
					throw new IllegalStateException("Current state is not a view state - cannot refresh; "
							+ "perhaps an unhandled exception occured in another state?");
				}
				return ((ViewState)currentState).refresh(context);
			}
			catch (FlowExecutionException e) {
				return pause(context, handleException(e, context));
			}
		}
		finally {
			getListeners().fireRequestProcessed(context);
		}
	}

	/**
	 * Returns the listener list.
	 * @return the attached execution listeners.
	 */
	public FlowExecutionListeners getListeners() {
		return listeners;
	}

	/**
	 * Resume this flow execution.
	 * @param context the state request context
	 */
	protected void resume(RequestControlContext context) {
		getActiveSessionInternal().setStatus(FlowSessionStatus.ACTIVE);
		getListeners().fireResumed(context);
	}

	/**
	 * Pause this flow execution.
	 * @param context the state request context
	 * @param selectedView the initial selected view to render
	 * @return the selected view to render
	 */
	protected ViewSelection pause(RequestControlContext context, ViewSelection selectedView) {
		if (!isActive()) {
			return selectedView;
		}
		getActiveSessionInternal().setStatus(FlowSessionStatus.PAUSED);
		getListeners().firePaused(context, selectedView);
		if (logger.isDebugEnabled()) {
			if (selectedView != null) {
				logger.debug("Paused to render " + selectedView + " and wait for user input");
			}
			else {
				logger.debug("Paused to wait for user input");
			}
		}
		return selectedView;
	}

	/**
	 * Handles an exception that occured performing an operation on this flow
	 * execution. First trys the set of exception handlers associated with the
	 * offending state, then the handlers at the flow level.
	 * @param exception the exception that occured
	 * @param context the state context the exception occured in
	 * @return the selected error view
	 * @throws FlowExecutionException rethrows the exception it was not handled
	 * at the state or flow level
	 */
	protected ViewSelection handleException(FlowExecutionException exception, RequestControlContext context)
			throws FlowExecutionException {
		getListeners().fireExceptionThrown(context, exception);
		if (logger.isDebugEnabled()) {
			logger.debug("Attempting to handle [" + exception + "]");
		}
		// the state could be null if the flow was attempting a start operation
		ViewSelection selectedView = tryStateHandlers(exception, context);
		if (selectedView != null) {
			return selectedView;
		}
		selectedView = tryFlowHandlers(exception, context);
		if (selectedView != null) {
			return selectedView;
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Rethrowing unhandled flow execution exception");
		}
		throw exception;
	}

	private ViewSelection tryStateHandlers(FlowExecutionException exception, RequestControlContext context) {
		ViewSelection selectedView = null;
		if (exception.getStateId() != null) {
			selectedView = getActiveFlow().getStateInternal(exception.getStateId()).handleException(exception, context);
			if (selectedView != null) {
				if (logger.isDebugEnabled()) {
					logger.debug("State '" + exception.getStateId() + "' handled exception");
				}
			}
		}
		return selectedView;
	}

	private ViewSelection tryFlowHandlers(FlowExecutionException exception, RequestControlContext context) {
		ViewSelection selectedView = getActiveFlow().handleException(exception, context);
		if (selectedView != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Flow '" + exception.getFlowId() + "' handled exception");
			}
		}
		return selectedView;
	}

	// flow session management helpers

	/**
	 * Create a flow execution control context for given event.
	 * <p>
	 * The default implementation uses the <code>FlowControlContextImpl</code>
	 * class. Subclasses can override this to use a custom class.
	 * @param externalContext the external context
	 */
	protected RequestControlContext createControlContext(ExternalContext externalContext) {
		return new RequestControlContextImpl(this, externalContext);
	}

	/**
	 * Returns the currently active flow session.
	 * @throws IllegalStateException this execution is not active
	 */
	protected FlowSessionImpl getActiveSessionInternal() throws IllegalStateException {
		assertActive();
		return (FlowSessionImpl)flowSessions.getLast();
	}

	/**
	 * Set the state that is currently active in this flow execution.
	 * @param newState the new current state
	 */
	protected void setCurrentState(State newState) {
		getActiveSessionInternal().setState(newState);
	}

	/**
	 * Activate a new <code>FlowSession</code> for the flow definition with
	 * the input provided. Pushes the new flow session onto the stack.
	 * @param flow the flow definition
	 * @return the new flow session
	 */
	protected FlowSession activateSession(Flow flow) {
		FlowSessionImpl session;
		if (!flowSessions.isEmpty()) {
			FlowSessionImpl parent = getActiveSessionInternal();
			parent.setStatus(FlowSessionStatus.SUSPENDED);
			session = createFlowSession(flow, parent);
		}
		else {
			session = createFlowSession(flow, null);
		}
		flowSessions.add(session);
		session.setStatus(FlowSessionStatus.STARTING);
		if (logger.isDebugEnabled()) {
			logger.debug("Starting " + session);
		}
		return session;
	}

	/**
	 * Create a new flow session object. Subclasses can override this to return
	 * a special implementation if required.
	 * @param flow the flow that should be associated with the flow session
	 * @param parent the flow session that should be the parent of the newly
	 * created flow session (may be null)
	 * @return the newly created flow session
	 */
	protected FlowSessionImpl createFlowSession(Flow flow, FlowSessionImpl parent) {
		return new FlowSessionImpl(flow, parent);
	}

	public FlowSession endActiveFlowSession() {
		FlowSessionImpl endingSession = (FlowSessionImpl)flowSessions.removeLast();
		endingSession.setStatus(FlowSessionStatus.ENDED);
		if (!flowSessions.isEmpty()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Resuming session '" + getActiveSessionInternal().getDefinition().getId() + "' in state '"
						+ getActiveSessionInternal().getState().getId() + "'");
			}
			getActiveSessionInternal().setStatus(FlowSessionStatus.ACTIVE);
		}
		else {
			if (logger.isDebugEnabled()) {
				logger.debug("[Ended] - this execution is now inactive");
			}
		}
		return endingSession;
	}

	/**
	 * Check that this flow execution is active and throw an exception if it's
	 * not.
	 */
	private void assertActive() throws IllegalStateException {
		if (!isActive()) {
			throw new IllegalStateException(
					"This flow execution is not active, it has either ended or has never been started.");
		}
	}

	private Flow getActiveFlow() {
		return (Flow)getActiveSessionInternal().getDefinition();
	}

	private State getCurrentState() {
		return (State)getActiveSessionInternal().getState();
	}

	// custom serialization (implementation of Externalizable for optimized
	// storage)

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		flowId = (String)in.readObject();
		flowSessions = (LinkedList)in.readObject();
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(flowId);
		out.writeObject(flowSessions);
	}

	public String toString() {
		if (!isActive()) {
			return "[Inactive " + getCaption() + "]";
		}
		else {
			if (flow != null) {
				return new ToStringCreator(this).append("flow", flow.getId()).append("flowSessions", flowSessions)
						.toString();
			}
			else {
				return "[Unhydrated " + getCaption() + "]";
			}
		}
	}

	// package private setters for restoring transient state

	void setFlow(Flow flow) {
		Assert.notNull(flow, "The root flow definition is required");
		this.flow = flow;
		this.flowId = flow.getId();
	}

	void setListeners(FlowExecutionListeners listeners) {
		Assert.notNull(listeners, "The execution listener list is required");
		this.listeners = listeners;
	}

	void setAttributes(AttributeMap attributes) {
		Assert.notNull(conversationScope, "The execution attribute map is required");
		this.attributes = attributes;
	}

	void setConversationScope(MutableAttributeMap conversationScope) {
		Assert.notNull(conversationScope, "The conversation scope map is required");
		this.conversationScope = conversationScope;
	}

	String getFlowId() {
		return flowId;
	}

	List getFlowSessions() {
		return flowSessions;
	}
}