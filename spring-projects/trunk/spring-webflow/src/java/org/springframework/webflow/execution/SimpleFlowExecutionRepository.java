package org.springframework.webflow.execution;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.style.ToStringCreator;
import org.springframework.webflow.util.RandomGuidUidGenerator;
import org.springframework.webflow.util.UidGenerator;

/**
 * A simple flow execution repository implementation that stores single
 * instances of flow executions representing stateful user conversations in map
 * entries structured in the following manner:
 * <p>
 * <ul>
 * <li>Each entry key is an assigned conversationId, uniquely identifying an
 * ongoing conversation between a client and the Spring Web Flow system in this
 * repository.
 * <li>Each entry value is a {@link FlowExecutionEntry} object, consisting of a
 * {@link FlowExecution} representing the state and behavior of a conversation
 * and a <code>continuationId</code> acting as a token required for accessing
 * that conversation.
 * </ul>
 * @author Keith Donald
 */
public class SimpleFlowExecutionRepository implements FlowExecutionRepository, Serializable {

	private static final long serialVersionUID = -8138465280185005691L;

	/**
	 * The map of flow execution entries in this repository.
	 */
	private Map flowExecutionEntries = new HashMap(128);

	/**
	 * The uid generation strategy used to generate unique conversation and
	 * continuation identifiers.
	 */
	private UidGenerator uidGenerator = new RandomGuidUidGenerator();

	/**
	 * Returns the uid generation strategy used to generate unique conversation
	 * and continuation identifiers.
	 */
	public UidGenerator getUidGenerator() {
		return uidGenerator;
	}

	/**
	 * Sets the uid generation strategy used to generate unique conversation and
	 * continuation identifiers.
	 */
	public void setUidGenerator(UidGenerator uidGenerator) {
		this.uidGenerator = uidGenerator;
	}

	public FlowExecutionContinuationKey generateContinuationKey(FlowExecution flowExecution) {
		return new FlowExecutionContinuationKey(uidGenerator.generateId(), uidGenerator.generateId());
	}

	public FlowExecutionContinuationKey generateContinuationKey(FlowExecution flowExecution, Serializable conversationId) {
		return new FlowExecutionContinuationKey(conversationId, uidGenerator.generateId());
	}

	public FlowExecution getFlowExecution(FlowExecutionContinuationKey key) {
		FlowExecutionEntry entry = (FlowExecutionEntry)flowExecutionEntries.get(key.getConversationId());
		if (entry == null) {
			throw new NoSuchConversationException(this, key.getConversationId());
		}
		else {
			// assert that the provided continuationId matches the entry's
			// continuationId
			// if they do not match, access to the conversation is not allowed.
			if (!key.getContinuationId().equals(entry.getId())) {
				throw new InvalidConversationContinuationException(this, key, "The continuation id '"
						+ key.getContinuationId() + "' associated with conversation '" + key.getConversationId()
						+ "' is invalid.  This will happen when accessing browser history "
						+ "(typically via the back button) that references a previously used continuation id, "
						+ "as this simple repository implementation does not support multiple continuations.  "
						+ "Consider using another repository implementation or "
						+ "restrict use of the browser back button.");
			}
			return entry.getFlowExecution();
		}
	}

	public void putFlowExecution(FlowExecutionContinuationKey key, FlowExecution flowExecution) {
		flowExecutionEntries.put(key.getConversationId(),
				new FlowExecutionEntry(key.getContinuationId(), flowExecution));
	}

	public void invalidateConversation(Serializable conversationId) {
		flowExecutionEntries.remove(conversationId);
	}

	/**
	 * A holder for a flow execution representing a user conversation with
	 * Spring Web Flow. Is also assigned an <code>id</code> used as a key for
	 * accessing the conversation.
	 * @author Keith Donald
	 */
	protected static class FlowExecutionEntry implements Serializable {
		private Serializable id;

		private FlowExecution flowExecution;

		public FlowExecutionEntry(Serializable id, FlowExecution flowExecution) {
			this.id = id;
			this.flowExecution = flowExecution;
		}

		public Serializable getId() {
			return id;
		}

		public FlowExecution getFlowExecution() {
			return flowExecution;
		}

		public String toString() {
			return new ToStringCreator(this).append("id", id).append("flowExecution", flowExecution).toString();
		}
	}
}