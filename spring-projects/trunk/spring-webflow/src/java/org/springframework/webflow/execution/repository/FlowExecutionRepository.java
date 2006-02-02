package org.springframework.webflow.execution.repository;

import java.io.Serializable;

import org.springframework.webflow.FlowException;
import org.springframework.webflow.ViewSelection;
import org.springframework.webflow.execution.FlowExecution;

/**
 * A repository for storing managed flow executions. Flow execution repositories
 * are responsible for managing the creation, storage, and restoration of
 * ongoing conversations between clients and the Spring Web Flow system.
 * <p>
 * When placed in a repository, a {@link FlowExecution} object representing the
 * state of a conversation at a point in time is indexed under a unique
 * {@link FlowExecutionContinuationKey}. This key provides enough information
 * to track a single active user conversation with the server, as well as
 * provide an index into one or more snapshots taken at points in time relative
 * to the user during conversation execution. These conversational snapshots are
 * called <i>continuations</i>.
 * 
 * @author Keith Donald
 */
public interface FlowExecutionRepository {

	/**
	 * Create a new flow execution persistable by this repository.
	 * <p>
	 * The returned flow execution logically represents the state of a new
	 * conversation before it has been started. The execution is eligible for
	 * persistence by this repository if it still active after startup request
	 * processing.
	 * @param flowId the flow definition identifier defining the blueprint for a
	 * conversation
	 * @return the flow execution, representing the state of a new conversation
	 * that has not yet been started
	 */
	public FlowExecution createFlowExecution(String flowId);

	/**
	 * Generate a unique flow execution continuation key to be used as an index
	 * into an active flow execution representing the start of a new user
	 * conversation in this repository. Both the <code>conversationId</code>
	 * and <code>continuationId</code> key parts are guaranteed to be unique.
	 * @return the continuation key
	 * @throws FlowExecutionStorageException a problem occured generating the
	 * key
	 */
	public FlowExecutionContinuationKey generateContinuationKey(FlowExecution flowExecution)
			throws FlowExecutionRepositoryException;

	/**
	 * Generate a unique flow execution continuation key to be used as an index
	 * into a new flow execution continuation associated with an <i>existing</i>
	 * user conversation managed in this repository. The returned key consists
	 * of the provided <code>conversationId</code> provided and a new, unique
	 * <code>continuationId</code>.
	 * @return the continuation key
	 * @throws FlowExecutionStorageException a problem occured generating the
	 * key
	 */
	public FlowExecutionContinuationKey generateContinuationKey(FlowExecution flowExecution, Serializable conversationId)
			throws FlowExecutionRepositoryException;

	/**
	 * Return the <code>FlowExecution</code> indexed by the provided
	 * continuation key. The returned flow execution represents the restored
	 * state of a user conversation captured by the indexed continuation at a
	 * point in time.
	 * @param continuationKey the continuation key
	 * @return the flow execution, representing the state of a conversation at a
	 * point in time, fully hydrated and ready to signal an event against.
	 * @throws FlowExecutionStorageException if no flow execution was indexed
	 * with the key provided
	 */
	public FlowExecution getFlowExecution(FlowExecutionContinuationKey continuationKey)
			throws FlowExecutionRepositoryException;

	/**
	 * Place the <code>FlowExecution</code> in this repository, indexed under
	 * the provided continuation key.
	 * <p>
	 * If this flow execution represents the start of a new conversation, that
	 * conversation will begin to be tracked and a continuation capturing the
	 * current state of the conversation will be created.
	 * <p>
	 * If this flow execution represents a change in the state of an existing,
	 * ongoing conversation, a new continuation capturing this most recent state
	 * of the conversation will be created.
	 * 
	 * @param continuationKey the continuation key
	 * @param flowExecution the flow execution
	 * @throws FlowExecutionStorageException the flow execution could not be
	 * stored
	 */
	public void putFlowExecution(FlowExecutionContinuationKey continuationKey, FlowExecution flowExecution)
			throws FlowExecutionRepositoryException;

	/**
	 * Returns the current view selection for the specified conversation, or
	 * <code>null</code> if no such view selection exists.
	 * <p>
	 * The "current view selection" is simply a descriptor for the last response
	 * issued to the actor participating with this conversation. This method
	 * facilitates access of that descriptor for purposes of re-issuing the same
	 * response multiple times, for example to support browser refresh.
	 * @param conversationId the id of an existing conversation
	 * @return the current view selection
	 * @throws FlowExecutionRepositoryException if an exception occured
	 * retrieving the current view selection
	 */
	public ViewSelection getCurrentViewSelection(Serializable conversationId) throws FlowException;

	/**
	 * Sets the current view selection for the specified conversation.
	 * @param conversationId the id of an existing conversation
	 * @param viewSelection the view selection, to be set as the current
	 * @throws FlowExecutionRepositoryException if an exception occured
	 * retrieving the current view selection
	 */
	public void setCurrentViewSelection(Serializable conversationId, ViewSelection viewSelection) throws FlowException;

	/**
	 * Invalidate the executing conversation with the specified id. This method
	 * will remove all data associated with the conversation, including any
	 * managed continuations. Any future clients that reference this
	 * conversation in a flow execution continuation key will be thrown a
	 * FlowExecutionRepositoryException on any access attempt.
	 * @param conversationId the conversationId
	 * @throws FlowExecutionStorageException the conversation could not be
	 * invalidated
	 */
	public void invalidateConversation(Serializable conversationId) throws FlowExecutionRepositoryException;

}