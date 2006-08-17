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
package org.springframework.webflow.execution;

import org.springframework.webflow.context.ExternalContext;
import org.springframework.webflow.core.collection.MutableAttributeMap;
import org.springframework.webflow.definition.FlowDefinition;
import org.springframework.webflow.definition.StateDefinition;

/**
 * A instance of a flow definition that can carry out definition execution on
 * behalf of a single client. Typically used to support the orchestration of a
 * web conversation.
 * <p>
 * This is the central facade interface for manipulating one runtime execution
 * of a flow definition. Implementations of this interface are the finite state
 * machine that is the heart of Spring Web Flow.
 * <p>
 * Typically, when a client wants to launch a flow execution at runtime, she
 * passes in the id of the appropriate {@link FlowDefinition definining flow} to
 * a coordinating {@link org.springframework.webflow.executor.FlowExecutor}.
 * This coordinator then creates an instance of an object implementing this
 * interface, and initializes it with the requested Flow definition which
 * becomes the execution's "root", or top-level flow. After creation, the
 * {@link #start(MutableAttributeMap, ExternalContext) start} operation is
 * called, which causes this execution to activate a new session for its root
 * flow definition. That session is then pushed onto a stack and its definition
 * becomes the <i>active flow</i>. An execution ({@link org.springframework.webflow.execution.RequestContext request context})
 * is then created and the Flow's
 * {@link org.springframework.webflow.engine.State start state} is then entered.
 * <p>
 * In a distributed environment such as HTTP, after a call into this object has
 * completed and control returns to the caller, this execution object (if still
 * active) is typically saved out to a repository before the server request
 * ends. For example it might be saved out to the HttpSession, a Database, or a
 * client-side hidden form field for later restoration and manipulation. This
 * execution persistence is the responsibility of the
 * {@link org.springframework.webflow.execution.repository.FlowExecutionRepository Flow Execution Repository}
 * subsystem.
 * <p>
 * Subsequent requests from the client to manipuate this flow execution trigger
 * restoration of this object, followed by an invocation of the
 * {@link #signalEvent(EventId, ExternalContext) signal event} operation. The
 * signalEvent operation tells this execution what action the user took from
 * within the the current state; for example, the user may have pressed pressed
 * the "submit" button, or pressed "cancel". After the user event is processed,
 * control again goes back to the caller and if this execution is still active,
 * it is saved out to the repository. This continues until a client event causes
 * this flow execution to end (by the root flow reaching an EndState). At that
 * time, this object is removed from the repository and discarded.
 * 
 * @see FlowDefinition
 * @see StateDefinition
 * @see FlowSession
 * @see RequestContext
 * @see org.springframework.webflow.execution.repository.FlowExecutionRepository
 * @see org.springframework.webflow.executor.FlowExecutor
 * 
 * @author Keith Donald
 * @author Erwin Vervaet
 */
public interface FlowExecution extends FlowExecutionContext {

	/**
	 * Start this flow execution, transitioning it to the root flow's start
	 * state and returning the starting view selection needed to issue a
	 * suitable response. Typically called by a flow executor on behalf of a
	 * browser client, but also from test code.
	 * @param input input attributes to pass to the flow, which the flow may
	 * choose to map into its scope
	 * @param context the external context in which the event occured
	 * @return the starting view selection, a value object to be used to issue a
	 * suitable response to the caller
	 * @throws FlowExecutionException if an exception was thrown within a state
	 * of the flow execution during request processing
	 * @see FlowExecutionContext#getDefinition()
	 */
	public ViewSelection start(MutableAttributeMap input, ExternalContext context) throws FlowExecutionException;

	/**
	 * Signal an occurrence of the specified user event in the current state of
	 * this executing flow. The event will be processed in full and control will
	 * be returned once event processing is complete.
	 * @param eventId the identifier of the user event that occured
	 * @param context the external context in which the event occured
	 * @return the next view selection to render, used by the calling executor
	 * to issue a suitable response to the client.
	 * @throws FlowExecutionException if an exception was thrown within a state
	 * of the resumed flow execution during event processing
	 */
	public ViewSelection signalEvent(EventId eventId, ExternalContext context) throws FlowExecutionException;

	/**
	 * Refresh this flow execution, asking the current view selection to be
	 * reconstituted to support reissuing the last response. This is idempotent
	 * operation that may be safely called on a paused execution.
	 * @param context the externa context in which the refresh event occured
	 * @return the current view selection for this flow execution
	 * @throws FlowExecutionException if an exception was thrown within a state
	 * of the resumed flow execution during event processing
	 */
	public ViewSelection refresh(ExternalContext context) throws FlowExecutionException;
}