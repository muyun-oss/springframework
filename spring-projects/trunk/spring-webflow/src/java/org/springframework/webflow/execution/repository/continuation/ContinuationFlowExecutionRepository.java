package org.springframework.webflow.execution.repository.continuation;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.springframework.webflow.execution.FlowExecution;
import org.springframework.webflow.execution.repository.FlowExecutionContinuationKey;
import org.springframework.webflow.execution.repository.FlowExecutionRepository;
import org.springframework.webflow.execution.repository.InvalidConversationContinuationException;
import org.springframework.webflow.execution.repository.NoSuchConversationException;
import org.springframework.webflow.util.RandomGuidUidGenerator;
import org.springframework.webflow.util.UidGenerator;

/**
 * A flow execution repository implementation that stores instances of flow
 * executions representing stateful user conversations in a map structured in
 * the following manner:
 * <p>
 * <ul>
 * <li>Each map entry key is an assigned conversationId, uniquely identifying
 * an ongoing conversation between a client and the Spring Web Flow system.
 * <li>Each map entry value is a {@link Conversation} object, providing the
 * details about an ongoing logical conversation or "application transaction"
 * between a client and the Spring Web Flow system. Each
 * <code>Conversation</code> maintains a stack of "continuations", where each
 * continuation represents the state of a conversation at a point in time
 * relevant to the user. These continuations can be restored to support users
 * going back in their browser to continue a conversation from a previous point.
 * </ul>
 * @author Keith Donald
 */
public class ContinuationFlowExecutionRepository implements FlowExecutionRepository, Serializable {

	private static final long serialVersionUID = -602931852676786766L;

	/**
	 * The conversation map, where each entry key is a conversationId and each
	 * entry value is a {@link Conversation} object.
	 */
	private Map conversations = new HashMap();

	/**
	 * The continuation factory that will be used to create new continuations to
	 * be added to active conversations.
	 */
	private FlowExecutionContinuationFactory continuationFactory = new SerializedFlowExecutionContinuationFactory();

	/**
	 * The uid generation strategy used to generate unique conversation and
	 * continuation identifiers.
	 */
	private UidGenerator uidGenerator = new RandomGuidUidGenerator();

	/**
	 * The maximum number of continuations that can be active per conversation.
	 */
	private int maxContinuations = 25;

	/**
	 * Returns the continuation factory that encapsulates the construction of
	 * continuations stored in this repository.
	 */
	public FlowExecutionContinuationFactory getContinuationFactory() {
		return continuationFactory;
	}

	/**
	 * Sets the continuation factory that encapsulates the construction of
	 * continuations stored in this repository.
	 */
	public void setContinuationFactory(FlowExecutionContinuationFactory continuationFactory) {
		this.continuationFactory = continuationFactory;
	}

	/**
	 * Returns the uid generator that generates unique identifiers for entries
	 * placed into this repository.
	 */
	public UidGenerator getUidGenerator() {
		return uidGenerator;
	}

	/**
	 * Sets the uid generator that generates unique identifiers for entries
	 * placed into this repository.
	 */
	public void setUidGenerator(UidGenerator uidGenerator) {
		this.uidGenerator = uidGenerator;
	}

	/**
	 * Returns the maximum number of continuations allowed per conversation in
	 * this repository.
	 */
	public int getMaxContinuations() {
		return maxContinuations;
	}

	/**
	 * Sets the maximum number of continuations allowed per conversation in this
	 * repository.
	 */
	public void setMaxContinuations(int maxContinuations) {
		this.maxContinuations = maxContinuations;
	}

	public FlowExecutionContinuationKey generateContinuationKey(FlowExecution flowExecution) {
		return new FlowExecutionContinuationKey(uidGenerator.generateId(), uidGenerator.generateId());
	}

	public FlowExecutionContinuationKey generateContinuationKey(FlowExecution flowExecution, Serializable conversationId) {
		return new FlowExecutionContinuationKey(conversationId, uidGenerator.generateId());
	}

	public FlowExecution getFlowExecution(FlowExecutionContinuationKey key) {
		return getConversationContinuation(key).getFlowExecution();
	}

	public void putFlowExecution(FlowExecutionContinuationKey key, FlowExecution flowExecution) {
		Conversation conversation = (Conversation)getOrCreateConversation(key.getConversationId());
		conversation.addContinuation(continuationFactory.createContinuation(key.getContinuationId(), flowExecution));
	}

	public void invalidateConversation(Serializable conversationId) {
		conversations.remove(conversationId);
	}

	private Conversation getOrCreateConversation(Serializable conversationId) {
		Conversation conversation = getConversation(conversationId);
		if (conversation == null) {
			conversation = createConversation();
			conversations.put(conversationId, conversation);
		}
		return conversation;
	}

	/**
	 * Factory method that returns a new conversation. Subclasses may override.
	 */
	protected Conversation createConversation() {
		return new Conversation(maxContinuations);
	}

	private Conversation getConversation(Serializable conversationId) {
		return (Conversation)conversations.get(conversationId);
	}

	private FlowExecutionContinuation getConversationContinuation(FlowExecutionContinuationKey key)
			throws NoSuchConversationException, InvalidConversationContinuationException {
		Conversation conversation = getConversation(key.getConversationId());
		if (conversation == null) {
			throw new NoSuchConversationException(this, key.getConversationId());
		}
		FlowExecutionContinuation continuation = conversation.getContinuation(key.getContinuationId());
		if (continuation == null) {
			throw new InvalidConversationContinuationException(this, key);
		}
		return continuation;
	}
}