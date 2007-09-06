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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.style.ToStringCreator;
import org.springframework.util.Assert;
import org.springframework.webflow.context.ExternalContext;
import org.springframework.webflow.core.collection.AttributeMap;
import org.springframework.webflow.core.collection.CollectionUtils;
import org.springframework.webflow.core.collection.LocalAttributeMap;
import org.springframework.webflow.core.collection.MutableAttributeMap;
import org.springframework.webflow.core.collection.ParameterMap;
import org.springframework.webflow.definition.FlowDefinition;
import org.springframework.webflow.definition.StateDefinition;
import org.springframework.webflow.definition.TransitionDefinition;
import org.springframework.webflow.engine.Flow;
import org.springframework.webflow.engine.RequestControlContext;
import org.springframework.webflow.engine.State;
import org.springframework.webflow.engine.Transition;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.FlowExecutionContext;
import org.springframework.webflow.execution.FlowExecutionException;
import org.springframework.webflow.execution.FlowSession;
import org.springframework.webflow.execution.FlowSessionStatus;

/**
 * Default request control context implementation used internally by the web flow system. This class is closely coupled
 * with <code>FlowExecutionImpl</code> and <code>FlowSessionImpl</code>. The three classes work together to form a
 * complete flow execution implementation based on a finite state machine.
 * 
 * @see FlowExecutionImpl
 * 
 * @author Keith Donald
 * @author Erwin Vervaet
 */
class RequestControlContextImpl implements RequestControlContext {

	private static final Log logger = LogFactory.getLog(RequestControlContextImpl.class);

	/**
	 * The owning flow execution carrying out this request.
	 */
	private FlowExecutionImpl flowExecution;

	/**
	 * A source context for the caller who initiated this request.
	 */
	private ExternalContext externalContext;

	/**
	 * The request scope data map. Never null, initially empty.
	 */
	private LocalAttributeMap requestScope = new LocalAttributeMap();

	/**
	 * Holder for contextual properties describing the currently executing request; never null, initially empty and
	 * immutable.
	 */
	private AttributeMap attributes = CollectionUtils.EMPTY_ATTRIBUTE_MAP;

	/**
	 * The last event that occurred in this request context; initially null.
	 */
	private Event lastEvent;

	/**
	 * The last transition that executed in this request context; initially null.
	 */
	private Transition lastTransition;

	/**
	 * Create a new request context.
	 * @param flowExecution the owning flow execution
	 * @param externalContext the external context that originated the flow execution request
	 */
	public RequestControlContextImpl(FlowExecutionImpl flowExecution, ExternalContext externalContext) {
		Assert.notNull(flowExecution, "The owning flow execution is required");
		this.flowExecution = flowExecution;
		this.externalContext = externalContext;
	}

	// implementing RequestContext

	public FlowDefinition getActiveFlow() {
		return flowExecution.getActiveSession().getDefinition();
	}

	public StateDefinition getCurrentState() {
		return flowExecution.getActiveSession().getState();
	}

	public MutableAttributeMap getRequestScope() {
		return requestScope;
	}

	public MutableAttributeMap getFlashScope() {
		return flowExecution.getFlashScope();
	}

	public MutableAttributeMap getFlowScope() {
		return flowExecution.getActiveSession().getScope();
	}

	public MutableAttributeMap getConversationScope() {
		return flowExecution.getConversationScope();
	}

	public ParameterMap getRequestParameters() {
		return externalContext.getRequestParameterMap();
	}

	public ExternalContext getExternalContext() {
		return externalContext;
	}

	public FlowExecutionContext getFlowExecutionContext() {
		return flowExecution;
	}

	public Event getLastEvent() {
		return lastEvent;
	}

	public TransitionDefinition getLastTransition() {
		return lastTransition;
	}

	public AttributeMap getAttributes() {
		return attributes;
	}

	public void setAttributes(AttributeMap attributes) {
		if (attributes == null) {
			this.attributes = CollectionUtils.EMPTY_ATTRIBUTE_MAP;
		} else {
			this.attributes = attributes;
		}
	}

	// implementing RequestControlContext

	public void setCurrentState(State state) {
		getExecutionListeners().fireStateEntering(this, state);
		State previousState = getCurrentStateInternal();
		flowExecution.setCurrentState(state);
		if (previousState == null) {
			getActiveSession().setStatus(FlowSessionStatus.ACTIVE);
		}
		getExecutionListeners().fireStateEntered(this, previousState);
	}

	public void setLastEvent(Event lastEvent) {
		this.lastEvent = lastEvent;
	}

	public void setLastTransition(Transition lastTransition) {
		this.lastTransition = lastTransition;
	}

	public void assignFlowExecutionKey() {
		this.flowExecution.assignKey();
	}

	public boolean getAlwaysRedirectOnPause() {
		Boolean redirectOnPause = this.flowExecution.getAttributes().getBoolean("alwaysRedirectOnPause");
		return redirectOnPause != null ? redirectOnPause.booleanValue() : false;
	}

	public void start(Flow flow, MutableAttributeMap input) throws FlowExecutionException {
		if (input == null) {
			// create a mutable map so entries can be added by listeners!
			input = new LocalAttributeMap();
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Activating new session for flow '" + flow.getId() + "' in state '"
					+ flow.getStartState().getId() + "' with input " + input);
		}
		getExecutionListeners().fireSessionCreating(this, flow, input);
		FlowSession session = flowExecution.activateSession(flow);
		getExecutionListeners().fireSessionStarting(this, session);
		flow.start(this, input);
		getExecutionListeners().fireSessionStarted(this, session);
	}

	public void handleEvent(Event event) throws FlowExecutionException {
		if (logger.isDebugEnabled()) {
			logger.debug("Signaling event '" + event.getId() + "' in state '" + getCurrentState().getId()
					+ "' of flow '" + getActiveFlow().getId() + "'");
		}
		setLastEvent(event);
		getExecutionListeners().fireEventSignaled(this, event);
		getActiveFlowInternal().handleEvent(this);
	}

	public FlowSession endActiveFlowSession(MutableAttributeMap output) throws IllegalStateException {
		FlowSession session = getFlowExecutionContext().getActiveSession();
		getExecutionListeners().fireSessionEnding(this, session, output);
		getActiveFlowInternal().end(this, output);
		if (logger.isDebugEnabled()) {
			logger.debug("Ending active session " + session + "; exposed session output is " + output);
		}
		session = flowExecution.endActiveFlowSession();
		getExecutionListeners().fireSessionEnded(this, session, output);
		return session;
	}

	public void execute(Transition transition) {
		transition.execute(getCurrentStateInternal(), this);
	}

	public void sendFlowExecutionRedirect() {
		this.flowExecution.sendFlowExecutionRedirect();
	}

	public void sendFlowDefinitionRedirect(String flowId, MutableAttributeMap input) {
		this.flowExecution.sendFlowDefinitionRedirect(flowId, input);
	}

	public void sendExternalRedirect(String resourceUri) {
		this.flowExecution.sendExternalRedirect(resourceUri);
	}

	// internal helpers

	/**
	 * Returns the execution listeners for the flow execution of this request context.
	 */
	protected FlowExecutionListeners getExecutionListeners() {
		return flowExecution.getListeners();
	}

	/**
	 * Returns the active flow in the flow execution of this request context.
	 */
	protected Flow getActiveFlowInternal() {
		return (Flow) getActiveSession().getDefinition();
	}

	/**
	 * Returns the current state in the flow execution of this request context.
	 */
	protected State getCurrentStateInternal() {
		return (State) getActiveSession().getState();
	}

	/**
	 * Returns the active flow session in the flow execution of this request context.
	 */
	protected FlowSessionImpl getActiveSession() {
		return flowExecution.getActiveSessionInternal();
	}

	public String toString() {
		return new ToStringCreator(this).append("externalContext", externalContext)
				.append("requestScope", requestScope).append("attributes", attributes).append("flowExecution",
						flowExecution).toString();
	}

}