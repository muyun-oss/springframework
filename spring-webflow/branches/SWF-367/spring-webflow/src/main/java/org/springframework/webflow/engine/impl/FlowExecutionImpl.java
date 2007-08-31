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
package org.springframework.webflow.engine.impl;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.LinkedList;
import java.util.ListIterator;

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
import org.springframework.webflow.execution.FlowExecution;
import org.springframework.webflow.execution.FlowExecutionException;
import org.springframework.webflow.execution.FlowExecutionKey;
import org.springframework.webflow.execution.FlowExecutionListener;
import org.springframework.webflow.execution.FlowSession;
import org.springframework.webflow.execution.FlowSessionStatus;
import org.springframework.webflow.execution.RequestContext;
import org.springframework.webflow.execution.FlowExecutionRequestRedirector;
import org.springframework.webflow.execution.ViewSelection;
import org.springframework.webflow.execution.factory.FlowExecutionKeyFactory;

/**
 * Default implementation of FlowExecution that uses a stack-based data structure to manage spawned flow sessions. This
 * class is closely coupled with package-private <code>FlowSessionImpl</code> and
 * <code>RequestControlContextImpl</code>. The three classes work together to form a complete flow execution
 * implementation based on a finite state machine.
 * <p>
 * This implementation of FlowExecution is serializable so it can be safely stored in an HTTP session or other
 * persistent store such as a file, database, or client-side form field. Once deserialized, the
 * {@link FlowExecutionImplStateRestorer} strategy is expected to be used to restore the execution to a usable state.
 * 
 * @see FlowExecutionImplFactory
 * @see FlowExecutionImplStateRestorer
 * 
 * @author Keith Donald
 * @author Erwin Vervaet
 * @author Ben Hale
 */
public class FlowExecutionImpl implements FlowExecution, Externalizable {

	private static final Log logger = LogFactory.getLog(FlowExecutionImpl.class);

	/**
	 * The execution's root flow; the top level flow that acts as the starting point for this flow execution.
	 * <p>
	 * Transient to support restoration by the {@link FlowExecutionImplStateRestorer}.
	 */
	private transient Flow flow;

	/**
	 * The stack of active, currently executing flow sessions. As subflows are spawned, they are pushed onto the stack.
	 * As they end, they are popped off the stack.
	 */
	private LinkedList flowSessions;

	/**
	 * A thread-safe listener list, holding listeners monitoring the lifecycle of this flow execution.
	 * <p>
	 * Transient to support restoration by the {@link FlowExecutionImplStateRestorer}.
	 */
	private transient FlowExecutionListeners listeners;

	/**
	 * The factory for getting the key to assign this flow execution when needed for persistence.
	 */
	private transient FlowExecutionKeyFactory keyFactory;

	/**
	 * The key assigned to this flow execution. May be null if a key has not been assigned.
	 */
	private FlowExecutionKey key;

	/**
	 * The flash map ("flash scope").
	 */
	private MutableAttributeMap flashScope = new LocalAttributeMap();

	/**
	 * A data structure for attributes shared by all flow sessions.
	 * <p>
	 * Transient to support restoration by the {@link FlowExecutionImplStateRestorer}.
	 */
	private transient MutableAttributeMap conversationScope;

	/**
	 * A data structure for runtime system execution attributes.
	 * <p>
	 * Transient to support restoration by the {@link FlowExecutionImplStateRestorer}.
	 */
	private transient AttributeMap attributes;

	/**
	 * Set so the transient {@link #flow} field can be restored by the {@link FlowExecutionImplStateRestorer}.
	 */
	private String flowId;

	/**
	 * A redirector for sending redirects to the calling agent asking them to perform another action.
	 */
	private transient FlowExecutionRequestRedirector redirector;

	/**
	 * Default constructor required for externalizable serialization. Should NOT be called programmatically.
	 */
	public FlowExecutionImpl() {
	}

	/**
	 * Create a new flow execution executing the provided flow. Flow executions are normally created by a flow execution
	 * factory.
	 * @param flow the root flow of this flow execution
	 * @param listeners the listeners interested in flow execution lifecycle events
	 * @param attributes flow execution system attributes
	 */
	public FlowExecutionImpl(Flow flow, FlowExecutionListener[] listeners, AttributeMap attributes,
			FlowExecutionKeyFactory keyFactory, FlowExecutionRequestRedirector redirector) {
		setFlow(flow);
		this.flowSessions = new LinkedList();
		this.listeners = new FlowExecutionListeners(listeners);
		this.attributes = (attributes != null ? attributes : CollectionUtils.EMPTY_ATTRIBUTE_MAP);
		this.keyFactory = keyFactory;
		this.redirector = redirector;
		this.conversationScope = new LocalAttributeMap();
	}

	public String getCaption() {
		return "execution of '" + flowId + "'";
	}

	// implementing FlowExecutionContext

	public FlowExecutionKey getKey() {
		return key;
	}

	public FlowDefinition getDefinition() {
		return flow;
	}

	public boolean isActive() {
		return !flowSessions.isEmpty();
	}

	public FlowSession getActiveSession() {
		return getActiveSessionInternal();
	}

	public MutableAttributeMap getFlashScope() {
		return flashScope;
	}

	public MutableAttributeMap getConversationScope() {
		return conversationScope;
	}

	public AttributeMap getAttributes() {
		return attributes;
	}

	// methods implementing FlowExecution

	public void start(MutableAttributeMap input, ExternalContext externalContext) throws FlowExecutionException {
		Assert.state(!isActive(), "This flow is already executing; you cannot call 'start()' more than once");
		if (logger.isDebugEnabled()) {
			logger.debug("Starting execution with input '" + input + "'");
		}
		RequestControlContext context = createControlContext(externalContext);
		getListeners().fireRequestSubmitted(context);
		try {
			// launch a flow session for the root flow
			context.start(flow, input);
		} catch (FlowExecutionException e) {
			handleException(e, context);
		} catch (Exception e) {
			handleException(wrap(e, context), context);
		} finally {
			if (isActive()) {
				getActiveSessionInternal().setStatus(FlowSessionStatus.PAUSED);
				getListeners().firePaused(context);
			}
			getListeners().fireRequestProcessed(context);
		}
	}

	public void resume(ExternalContext externalContext) throws FlowExecutionException {
		assertActive();
		RequestControlContext context = createControlContext(externalContext);
		getListeners().fireRequestSubmitted(context);
		flashScope.clear();
		try {
			getActiveSessionInternal().setStatus(FlowSessionStatus.ACTIVE);
			getListeners().fireResumed(context);
			getActiveFlow().resume(context);
		} catch (FlowExecutionException e) {
			handleException(e, context);
		} catch (Exception e) {
			handleException(wrap(e, context), context);
		} finally {
			if (isActive()) {
				getActiveSessionInternal().setStatus(FlowSessionStatus.PAUSED);
				getListeners().firePaused(context);
			}
			getListeners().fireRequestProcessed(context);
		}
	}

	private FlowExecutionException wrap(Exception e, RequestContext context) {
		if (context.getFlowExecutionContext().isActive()) {
			String flowId = context.getActiveFlow().getId();
			String stateId = context.getCurrentState().getId();
			return new FlowExecutionException(flowId, stateId, "Exception thrown in state '" + stateId + "' of flow '"
					+ flowId + "'", e);
		} else {
			return new FlowExecutionException(flowId, null, "Exception thrown within inactive flow '" + flowId + "'");
		}
	}

	/**
	 * Returns the listener list.
	 * @return the attached execution listeners.
	 */
	FlowExecutionListeners getListeners() {
		return listeners;
	}

	/**
	 * Handles an exception that occurred performing an operation on this flow execution. First trys the set of
	 * exception handlers associated with the offending state, then the handlers at the flow level.
	 * @param exception the exception that occurred
	 * @param context the request control context the exception occurred in
	 * @return the selected error view, never null
	 * @throws FlowExecutionException rethrows the exception if it was not handled at the state or flow level
	 */
	protected ViewSelection handleException(FlowExecutionException exception, RequestControlContext context)
			throws FlowExecutionException {
		getListeners().fireExceptionThrown(context, exception);
		if (logger.isDebugEnabled()) {
			logger.debug("Attempting to handle [" + exception + "]");
		}
		try {
			// the state could be null if the flow was attempting a start operation
			ViewSelection selectedView = tryStateHandlers(exception, context);
			if (selectedView != null) {
				return selectedView;
			}
			selectedView = tryFlowHandlers(exception, context);
			if (selectedView != null) {
				return selectedView;
			}
		} catch (FlowExecutionException newException) {
			// exception handling resulted in a new FlowExecutionException, try to handle it
			return handleException(newException, context);
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Rethrowing unhandled flow execution exception");
		}
		throw exception;
	}

	/**
	 * Try to handle given exception using execution exception handlers registered at the state level. Returns null if
	 * no handler handled the exception.
	 */
	private ViewSelection tryStateHandlers(FlowExecutionException exception, RequestControlContext context) {
		ViewSelection selectedView = null;
		if (exception.getStateId() != null) {
			selectedView = getActiveFlow().getStateInstance(exception.getStateId()).handleException(exception, context);
			if (selectedView != null) {
				if (logger.isDebugEnabled()) {
					logger.debug("State '" + exception.getStateId() + "' handled exception");
				}
			}
		}
		return selectedView;
	}

	/**
	 * Try to handle given exception using execution exception handlers registered at the flow level. Returns null if no
	 * handler handled the exception.
	 */
	private ViewSelection tryFlowHandlers(FlowExecutionException exception, RequestControlContext context) {
		ViewSelection selectedView = getActiveFlow().handleException(exception, context);
		if (selectedView != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Flow '" + exception.getFlowId() + "' handled exception");
			}
		}
		return selectedView;
	}

	// internal helpers

	/**
	 * Create a flow execution control context.
	 * @param externalContext the external context triggering this request
	 */
	protected RequestControlContext createControlContext(ExternalContext externalContext) {
		return new RequestControlContextImpl(this, externalContext);
	}

	/**
	 * Returns the currently active flow session.
	 * @throws IllegalStateException this execution is not active
	 */
	FlowSessionImpl getActiveSessionInternal() throws IllegalStateException {
		assertActive();
		return (FlowSessionImpl) flowSessions.getLast();
	}

	/**
	 * Set the state that is currently active in this flow execution.
	 * @param newState the new current state
	 */
	protected void setCurrentState(State newState) {
		getActiveSessionInternal().setState(newState);
	}

	/**
	 * Activate a new <code>FlowSession</code> for the flow definition. Creates the new flow session and pushes it
	 * onto the stack.
	 * @param flow the flow definition
	 * @return the new flow session
	 */
	protected FlowSession activateSession(Flow flow) {
		FlowSessionImpl session;
		if (!flowSessions.isEmpty()) {
			FlowSessionImpl parent = getActiveSessionInternal();
			parent.setStatus(FlowSessionStatus.SUSPENDED);
			session = createFlowSession(flow, parent);
		} else {
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
	 * Create a new flow session object. Subclasses can override this to return a special implementation if required.
	 * @param flow the flow that should be associated with the flow session
	 * @param parent the flow session that should be the parent of the newly created flow session (may be null)
	 * @return the newly created flow session
	 */
	FlowSessionImpl createFlowSession(Flow flow, FlowSessionImpl parent) {
		return new FlowSessionImpl(flow, parent);
	}

	/**
	 * End the active flow session, popping it of the stack.
	 * @return the ended session
	 */
	public FlowSession endActiveFlowSession() {
		FlowSessionImpl endingSession = (FlowSessionImpl) flowSessions.removeLast();
		endingSession.setStatus(FlowSessionStatus.ENDED);
		if (!flowSessions.isEmpty()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Resuming session '" + getActiveSessionInternal().getDefinition().getId() + "' in state '"
						+ getActiveSessionInternal().getState().getId() + "'");
			}
			getActiveSessionInternal().setStatus(FlowSessionStatus.ACTIVE);
		} else {
			if (logger.isDebugEnabled()) {
				logger.debug("[Ended] - this execution is now inactive");
			}
		}
		return endingSession;
	}

	/**
	 * Make sure that this flow execution is active and throw an exception if it's not.
	 */
	private void assertActive() throws IllegalStateException {
		if (!isActive()) {
			throw new IllegalStateException(
					"This flow execution is not active, it has either ended or has never been started.");
		}
	}

	/**
	 * Returns the currently active flow.
	 */
	private Flow getActiveFlow() {
		return (Flow) getActiveSessionInternal().getDefinition();
	}

	/**
	 * Returns the current state of this flow execution.
	 */
	private State getCurrentState() {
		return (State) getActiveSessionInternal().getState();
	}

	// custom serialization (implementation of Externalizable for optimized storage)

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		flowId = (String) in.readObject();
		flowSessions = (LinkedList) in.readObject();
		flashScope = (MutableAttributeMap) in.readObject();
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(flowId);
		out.writeObject(flowSessions);
		out.writeObject(flashScope);
	}

	public String toString() {
		if (!isActive()) {
			return "[Inactive " + getCaption() + "]";
		} else {
			if (flow != null) {
				return new ToStringCreator(this).append("flow", flow.getId()).append("flowSessions", flowSessions)
						.append("flashScope", flashScope).toString();
			} else {
				return "[Unhydrated " + getCaption() + "]";
			}
		}
	}

	// package private setters for restoring transient state
	// used by FlowExecutionImplStateRestorer

	/**
	 * Restore the flow definition of this flow execution.
	 */
	void setFlow(Flow flow) {
		Assert.notNull(flow, "The root flow definition is required");
		this.flow = flow;
		this.flowId = flow.getId();
	}

	/**
	 * Restore the listeners of this flow execution.
	 */
	void setListeners(FlowExecutionListeners listeners) {
		Assert.notNull(listeners, "The execution listener list is required");
		this.listeners = listeners;
	}

	/**
	 * Restore the execution attributes of this flow execution.
	 */
	void setAttributes(AttributeMap attributes) {
		Assert.notNull(conversationScope, "The execution attribute map is required");
		this.attributes = attributes;
	}

	/**
	 * Restore conversation scope for this flow execution.
	 */
	void setConversationScope(MutableAttributeMap conversationScope) {
		Assert.notNull(conversationScope, "The conversation scope map is required");
		this.conversationScope = conversationScope;
	}

	/**
	 * Returns the flow definition id of this flow execution.
	 */
	String getFlowId() {
		return flowId;
	}

	/**
	 * Returns the list of flow session maintained by this flow execution.
	 */
	LinkedList getFlowSessions() {
		return flowSessions;
	}

	/**
	 * Are there any flow sessions in this flow execution?
	 */
	boolean hasSessions() {
		return !flowSessions.isEmpty();
	}

	/**
	 * Are there any sessions for sub flows in this flow execution?
	 */
	boolean hasSubflowSessions() {
		return flowSessions.size() > 1;
	}

	/**
	 * Returns the flow session for the root flow of this flow execution.
	 */
	FlowSessionImpl getRootSession() {
		return (FlowSessionImpl) flowSessions.getFirst();
	}

	/**
	 * Returns an iterator looping over the subflow sessions in this flow execution.
	 */
	ListIterator getSubflowSessionIterator() {
		return flowSessions.listIterator(1);
	}

	void assignKey() {
		this.key = keyFactory.getKey();
	}

	public void sendFlowExecutionRedirect() {
		redirector.sendFlowExecutionRedirect(key);
	}

	public void sendFlowDefinitionRedirect(String flowId, MutableAttributeMap input) {
		redirector.sendFlowDefinitionRedirect(flowId, input);
	}

	public void sendExternalRedirect(String resourceUri) {
		redirector.sendExternalRedirect(resourceUri);
	}

}