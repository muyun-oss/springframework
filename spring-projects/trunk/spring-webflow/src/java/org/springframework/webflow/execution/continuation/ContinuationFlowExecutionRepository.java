package org.springframework.webflow.execution.continuation;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.springframework.webflow.execution.FlowExecution;
import org.springframework.webflow.execution.FlowExecutionContinuationKey;
import org.springframework.webflow.execution.FlowExecutionRepository;
import org.springframework.webflow.execution.InvalidConversationContinuationException;
import org.springframework.webflow.execution.NoSuchConversationException;
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
 * relative to the user. These continuations allow users to go back in their
 * browser and continue a conversation from a previous point.
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

	public FlowExecutionContinuationFactory getContinuationFactory() {
		return continuationFactory;
	}

	public void setContinuationFactory(FlowExecutionContinuationFactory continuationFactory) {
		this.continuationFactory = continuationFactory;
	}

	public UidGenerator getUidGenerator() {
		return uidGenerator;
	}

	public void setUidGenerator(UidGenerator keyGenerator) {
		this.uidGenerator = keyGenerator;
	}

	public int getMaxContinuations() {
		return maxContinuations;
	}

	public void setMaxContinuations(int maxContinuations) {
		this.maxContinuations = maxContinuations;
	}

	public FlowExecutionContinuationKey generateContinuationKey() {
		return new FlowExecutionContinuationKey(uidGenerator.generateId(), uidGenerator.generateId());
	}

	public FlowExecutionContinuationKey generateContinuationKey(Serializable conversationId) {
		return new FlowExecutionContinuationKey(conversationId, uidGenerator.generateId());
	}

	public FlowExecution getFlowExecution(FlowExecutionContinuationKey key) {
		return getConversationContinuation(key).getFlowExecution();
	}

	public void putFlowExecution(FlowExecutionContinuationKey key, FlowExecution flowExecution) {
		Conversation conversation = (Conversation)conversations.get(key.getConversationId());
		if (conversation != null) {
			conversation.addContinuation(continuationFactory.createContinuation(key, flowExecution));
		}
		else {
			conversation = new Conversation(maxContinuations);
			conversation.addContinuation(continuationFactory.createContinuation(key, flowExecution));
			conversations.put(key.getConversationId(), conversation);
		}
	}

	public void invalidateConversation(Serializable conversationId) {
		conversations.remove(conversationId);
	}

	private FlowExecutionContinuation getConversationContinuation(FlowExecutionContinuationKey key)
			throws NoSuchConversationException {
		Conversation conversation = getConversation(key.getConversationId());
		if (conversation == null) {
			throw new NoSuchConversationException(key.getConversationId());
		}
		FlowExecutionContinuation continuation = conversation.getContinuation(key.getContinuationId());
		if (continuation == null) {
			throw new InvalidConversationContinuationException(key);
		}
		return continuation;
	}

	private Conversation getConversation(Serializable conversationId) {
		return (Conversation)conversations.get(conversationId);
	}
}