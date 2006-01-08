/*
 * Copyright 2002-2005 the original author or authors.
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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.binding.format.Formatter;
import org.springframework.core.CollectionFactory;
import org.springframework.core.style.StylerUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.webflow.ExternalContext;
import org.springframework.webflow.Flow;
import org.springframework.webflow.FlowExecutionContext;
import org.springframework.webflow.StateException;
import org.springframework.webflow.ViewSelection;

/**
 * A central facade for the execution of flows within an application. This
 * object is responsible for creating and starting new flow executions as
 * requested by clients, as well as signaling events for processing by existing,
 * paused executions (that are waiting to be resumed in response to a user
 * event). This object is a facade or entry point into the flow execution
 * subsystem, and makes the overall subsystem easier to use.
 * <p>
 * The {@link #onEvent(ExternalContext)} method is the central facade operation
 * and implements the following algorithm:
 * <ol>
 * <li>Search for a flow execution id in the external context (in a request
 * parameter named {@link #getFlowExecutionIdParameterName()).</li>
 * <li>If no flow execution id was submitted, create a new flow execution. The
 * top-level flow definition for which an execution is created for is determined
 * by the value of the {@link #getFlowIdParameterName()} request parameter. If
 * this parameter parameter is not present, an exception is thrown.</li>
 * <li>If a flow execution id <em>was</em> submitted, load the previously
 * saved FlowExecution with that id from a repository ({@link #getRepository(ExternalContext)}).</li>
 * <li>If a new flow execution was created in the previous steps, start that
 * execution.</li>
 * <li>If an existing flow execution was loaded from a repository, extract the
 * value of the event id ({@link #getEventIdParameterName()). Signal the occurence of the user event, resuming the flow
 * execution in the current state.</li>
 * <li>If the flow execution is still active after event processing, save it
 * out to the repository. This process generates a unique flow execution id that
 * will be exposed to the caller for identifying the same FlowExecution
 * (conversation) on subsequent requests. The caller will also be given access
 * to the flow execution context and any data placed in request or flow scope.</li>
 * </ol>
 * <p>
 * By default, this class will use the flow execution implementation provided by
 * the <code>FlowExecutionImpl</code> class. If you would like to use a
 * different implementation, override the {@link #createFlowExecution(Flow)}
 * method in a subclass.
 * 
 * <p>
 * <b>Typical FlowExecutionManager configurable properties</b><br>
 * <table border="1">
 * <tr>
 * <td><b>name</b></td>
 * <td><b>default</b></td>
 * <td><b>description</b></td>
 * </tr>
 * <tr>
 * <td>flowLocator (required)</td>
 * <td>None</td>
 * <td>The locator that will load Flow definitions as needed for execution by
 * this manager</td>
 * </tr>
 * <tr>
 * <td>repositoryFactory</td>
 * <td>A server-side, stateful-session-based repository factory</td>
 * <td>The strategy for accessing managed flow execution repositories</td>
 * </tr>
 * <tr>
 * <td>listeners</td>
 * <td>None</td>
 * <td>The listeners to observe the lifecycle of managed flow executions</td>
 * </tr>
 * </table>
 * </p>
 * @see org.springframework.webflow.execution.FlowExecution
 * @see org.springframework.webflow.execution.FlowExecutionRepositoryFactory
 * @see org.springframework.webflow.execution.FlowExecutionListener
 * 
 * @author Erwin Vervaet
 * @author Keith Donald
 * @author Colin Sampaleanu
 */
public class FlowExecutionManager implements FlowExecutionListenerLoader {

	/**
	 * Clients can send the id (name) of the flow to be started using an event
	 * parameter with this name ("_flowId").
	 */
	public static final String FLOW_ID_PARAMETER = "_flowId";

	/**
	 * Clients can send the flow execution id using an event parameter with this
	 * name ("_flowExecutionId").
	 */
	public static final String FLOW_EXECUTION_ID_PARAMETER = "_flowExecutionId";

	/**
	 * Clients can send the event to be signaled in an event parameter with this
	 * name ("_eventId").
	 */
	public static final String EVENT_ID_PARAMETER = "_eventId";

	/**
	 * The default delimiter used when a parameter value is sent as part of the
	 * name of an event parameter (e.g. "_eventId_submit").
	 */
	public static final String PARAMETER_VALUE_DELIMITER = "_";

	/**
	 * The flow context itself will be exposed to the view in a model attribute
	 * with this name ("flowExecutionContext").
	 */
	public static final String FLOW_EXECUTION_CONTEXT_ATTRIBUTE = "flowExecutionContext";

	/**
	 * The id of the flow execution will be exposed to the view in a model
	 * attribute with this name ("flowExecutionId").
	 */
	public static final String FLOW_EXECUTION_ID_ATTRIBUTE = "flowExecutionId";

	/**
	 * The current state of an executing flow will be exposed to the view in a
	 * model attribute with this name ("currentSstateId").
	 */
	public static final String CURRENT_STATE_ID_ATTRIBUTE = "currentStateId";

	/**
	 * Event id value indicating that the event has not been set ("@NOT_SET@").
	 */
	public static final String NOT_SET_EVENT_ID = "@NOT_SET@";

	/**
	 * Logger, usable by subclasses.
	 */
	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * The flow locator strategy for retrieving a flow definition using a flow
	 * id provided by the client. Defaults to a bean factory based lookup
	 * strategy.
	 */
	private FlowLocator flowLocator;

	/**
	 * The flow execution repository factoring, for obtaining repository
	 * instances to save paused executions that require user input and load
	 * resuming executions that will process user events.
	 */
	private FlowExecutionRepositoryFactory repositoryFactory = new ExternalMapFlowExecutionRepositoryFactory();

	/**
	 * The formatter that will parse encoded _flowExecutionId strings into
	 * {@link FlowExecutionContinuationKey} objects.
	 */
	private Formatter continuationKeyFormatter = new FlowExecutionContinuationKeyFormatter();

	/**
	 * A set of flow execution listeners to a list of flow execution listener
	 * criteria objects. The criteria list determines the conditions in which a
	 * single flow execution listener applies.
	 */
	private Set listenerSet = CollectionFactory.createLinkedSetIfPossible(6);

	/**
	 * Identifies a flow definition to launch a new execution for, defaults to
	 * ("_flowId").
	 */
	private String flowIdParameterName = FLOW_ID_PARAMETER;

	/**
	 * Identifies an existing flow execution to participate in, defaults to
	 * ("_flowExecutionId").
	 */
	private String flowExecutionIdParameterName = FLOW_EXECUTION_ID_PARAMETER;

	/**
	 * Identifies an event that occured in an existing flow execution, defaults
	 * to ("_eventId_submit").
	 */
	private String eventIdParameterName = EVENT_ID_PARAMETER;

	/**
	 * The embedded parameter name/value delimiter value, used to parse a
	 * parameter value when a value is embedded in a parameter name (e.g.
	 * "_eventId_bar").
	 */
	private String parameterDelimiter = PARAMETER_VALUE_DELIMITER;

	/**
	 * Create a new flow execution manager using the specified flow locator for
	 * loading Flow definitions.
	 * @param flowLocator the flow locator to use
	 * 
	 * @see #setFlowLocator(FlowLocator)
	 * @see #setRepositoryFactory(FlowExecutionRepositoryFactory)
	 * @see #setListener(FlowExecutionListener)
	 * @see #setListenerCriteria(FlowExecutionListener,
	 * FlowExecutionListenerCriteria)
	 * @see #setListenerMap(Map)
	 * @see #setListeners(Collection)
	 * @see #setListenersCriteria(Collection, FlowExecutionListenerCriteria)
	 */
	public FlowExecutionManager(FlowLocator flowLocator) {
		setFlowLocator(flowLocator);
	}

	/**
	 * Returns the flow locator to use for lookup of flows specified using the
	 * {@link #FLOW_ID_PARAMETER} event parameter.
	 */
	protected FlowLocator getFlowLocator() {
		return flowLocator;
	}

	/**
	 * Set the flow locator to use for lookup of flows specified using the
	 * {@link #FLOW_ID_PARAMETER} parameter.
	 */
	public void setFlowLocator(FlowLocator flowLocator) {
		this.flowLocator = flowLocator;
	}

	/**
	 * Set the repository factory used by the flow execution manager.
	 */
	public void setRepositoryFactory(FlowExecutionRepositoryFactory repositoryLocator) {
		this.repositoryFactory = repositoryLocator;
	}

	/**
	 * Returns the repository instance to be used by the flow execution manager.
	 */
	protected FlowExecutionRepository getRepository(ExternalContext context) {
		return repositoryFactory.getRepository(context);
	}

	/**
	 * Returns the array of flow execution listeners for specified flow.
	 * @param flow the flow definition associated with the execution to be
	 * listened to
	 * @return the flow execution listeners
	 */
	public FlowExecutionListener[] getListeners(Flow flow) {
		Assert.notNull(flow, "The Flow to load listeners for cannot be null");
		List listenersToAttach = new LinkedList();
		for (Iterator it = listenerSet.iterator(); it.hasNext();) {
			ConditionalFlowExecutionListenerHolder listenerHolder = (ConditionalFlowExecutionListenerHolder)it.next();
			if (listenerHolder.listenerAppliesTo(flow)) {
				listenersToAttach.add(listenerHolder.getListener());
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Loaded [" + listenersToAttach.size() + "] of possible " + listenerSet.size()
					+ " listeners to this execution request for flow '" + flow.getId()
					+ "', the listeners to attach are " + StylerUtils.style(listenersToAttach));
		}
		return (FlowExecutionListener[])listenersToAttach.toArray(new FlowExecutionListener[listenersToAttach.size()]);
	}

	/**
	 * Returns a unmodifiable map of the configured flow execution listeners and
	 * the criteria in which those listeners apply.
	 */
	public Set getListenerSet() {
		return Collections.unmodifiableSet(listenerSet);
	}

	/**
	 * Set the flow execution listener that will be notified of managed flow
	 * executions.
	 */
	public void setListener(FlowExecutionListener listener) {
		setListeners(Collections.singleton(listener));
	}

	/**
	 * Set the flow execution listener that will be notified of managed flow
	 * executions for the flows that match given criteria.
	 */
	public void setListenerCriteria(FlowExecutionListener listener, FlowExecutionListenerCriteria criteria) {
		setListenersCriteria(Collections.singleton(listener), criteria);
	}

	/**
	 * Sets the flow execution listeners that will be notified of managed flow
	 * executions.
	 */
	public void setListeners(Collection listeners) {
		setListenersCriteria(listeners, FlowExecutionListenerCriteriaFactory.allFlows());
	}

	/**
	 * Sets the flow execution listeners that will be notified of managed flow
	 * executions for flows that match given criteria.
	 */
	public void setListenersCriteria(Collection listeners, FlowExecutionListenerCriteria criteria) {
		if (logger.isDebugEnabled()) {
			logger.debug("Setting listeners " + listeners + " with criteria " + criteria);
		}
		for (Iterator it = listeners.iterator(); it.hasNext();) {
			FlowExecutionListener listener = (FlowExecutionListener)it.next();
			if (containsListener(listener)) {
				removeListener(listener);
			}
			addListenerCriteria(listener, criteria);
		}
	}

	/**
	 * Sets the flow execution listeners that will be notified of managed flow
	 * executions. The map keys may be individual flow execution listener
	 * instances or collections of execution listener instances. The map values
	 * can either be string encoded flow execution listener criteria or direct
	 * references to <code>FlowExecutionListenerCriteria</code> objects.
	 */
	public void setListenerMap(Map listenerCriteriaMap) {
		Iterator it = listenerCriteriaMap.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry entry = (Map.Entry)it.next();
			FlowExecutionListenerCriteria criteria;
			if (entry.getValue() instanceof FlowExecutionListenerCriteria) {
				criteria = (FlowExecutionListenerCriteria)entry.getValue();
			}
			else {
				criteria = convertEncodedListenerCriteria((String)entry.getValue());
			}
			if (entry.getKey() instanceof Collection) {
				setListenersCriteria((Collection)entry.getKey(), criteria);
			}
			else {
				setListenerCriteria((FlowExecutionListener)entry.getKey(), criteria);
			}
		}
	}

	/**
	 * Helper that converts from text to a FlowExecutionListenerCriteria
	 * @param encodedCriteria the encoded text
	 * @return the criteria
	 */
	protected FlowExecutionListenerCriteria convertEncodedListenerCriteria(String encodedCriteria) {
		return new TextToFlowExecutionListenerCriteria().convert(encodedCriteria);
	}

	/**
	 * Add a listener that will listen to executions for all flows.
	 * @param listener the listener to add
	 */
	public void addListener(FlowExecutionListener listener) {
		addListenerCriteria(listener, FlowExecutionListenerCriteriaFactory.allFlows());
	}

	/**
	 * Add a listener that will listen to executions to flows matching the
	 * specified criteria.
	 * @param listener the listener
	 * @param criteria the listener criteria
	 */
	public void addListenerCriteria(FlowExecutionListener listener, FlowExecutionListenerCriteria criteria) {
		if (logger.isDebugEnabled()) {
			logger.debug("Adding flow execution listener " + listener + " with criteria " + criteria);
		}
		ConditionalFlowExecutionListenerHolder conditional = getHolder(listener);
		if (conditional == null) {
			conditional = new ConditionalFlowExecutionListenerHolder(listener);
			listenerSet.add(conditional);
		}
		conditional.add(criteria);
	}

	protected ConditionalFlowExecutionListenerHolder getHolder(FlowExecutionListener listener) {
		Iterator it = listenerSet.iterator();
		while (it.hasNext()) {
			ConditionalFlowExecutionListenerHolder next = (ConditionalFlowExecutionListenerHolder)it.next();
			if (next.getListener().equals(listener)) {
				return next;
			}
		}
		return null;
	}

	/**
	 * Is the listener contained by this Flow execution manager?
	 * @param listener the listener
	 * @return true if yes, false otherwise
	 */
	public boolean containsListener(FlowExecutionListener listener) {
		return listenerSet.contains(listener);
	}

	/**
	 * Remove the flow execution listener from the listener list.
	 * @param listener the listener
	 */
	public void removeListener(FlowExecutionListener listener) {
		listenerSet.remove(listener);
	}

	/**
	 * Remove the criteria for the specified listener.
	 * @param listener the listener
	 * @param criteria the criteria
	 */
	public void removeListenerCriteria(FlowExecutionListener listener, FlowExecutionListenerCriteria criteria) {
		if (containsListener(listener)) {
			ConditionalFlowExecutionListenerHolder listenerHolder = getHolder(listener);
			listenerHolder.remove(criteria);
			if (listenerHolder.isCriteriaSetEmpty()) {
				removeListener(listener);
			}
		}
	}

	/**
	 * Returns the continuation key formatting strategy.
	 * @return the continuation key formatter
	 */
	public Formatter getContinuationKeyFormatter() {
		return continuationKeyFormatter;
	}

	/**
	 * Sets the flow execution continuation key formatting strategy.
	 * @param continuationKeyFormatter the continuation key formatter
	 */
	public void setContinuationKeyFormatter(Formatter continuationKeyFormatter) {
		this.continuationKeyFormatter = continuationKeyFormatter;
	}

	/**
	 * Returns the name of the flow id parameter.
	 */
	public String getFlowIdParameterName() {
		return flowIdParameterName;
	}

	/**
	 * Sets the flow id parameter name.
	 */
	public void setFlowIdParameterName(String flowIdParameterName) {
		this.flowIdParameterName = flowIdParameterName;
	}

	/**
	 * Returns the name of the flow execution id parameter.
	 */
	public String getFlowExecutionIdParameterName() {
		return flowExecutionIdParameterName;
	}

	/**
	 * Sets the flow execution id parameter name.
	 */
	public void setFlowExecutionIdParameterName(String flowExecutionIdParameterName) {
		this.flowExecutionIdParameterName = flowExecutionIdParameterName;
	}

	/**
	 * Returns the name of the request parameter that stores the event
	 * identifier.
	 */
	public String getEventIdParameterName() {
		return eventIdParameterName;
	}

	/**
	 * Sets the event id parameter name.
	 */
	public void setEventIdParameterName(String eventIdParameterName) {
		this.eventIdParameterName = eventIdParameterName;
	}

	/**
	 * Returns the embedded eventId parameter delimiter.
	 */
	public String getParameterDelimiter() {
		return parameterDelimiter;
	}

	/**
	 * Sets the embedded eventId parameter delimiter.
	 */
	public void setParameterDelimiter(String parameterDelimiter) {
		this.parameterDelimiter = parameterDelimiter;
	}

	/**
	 * Returns the marker value indicating that the event id parameter was not
	 * set properly in the event because of a view configuration error
	 * ("@NOT_SET@").
	 * <p>
	 * This is useful when a view relies on an dynamic means to set the eventId
	 * event parameter, for example, using javascript. This approach assumes the
	 * "not set" marker value will be a static default (a kind of fallback,
	 * submitted if the eventId does not get set to the proper dynamic value
	 * onClick, for example, if javascript was disabled).
	 */
	public String getNotSetEventIdParameterMarker() {
		return NOT_SET_EVENT_ID;
	}

	// event processing

	/**
	 * Signal the occurrence of an external user event. This is the central
	 * entry point into the webflow system for managing all executing flows.
	 * @param context the context in which the external event occured
	 * @return the view descriptor of the model and view to render
	 */
	public ViewSelection onEvent(ExternalContext context) {
		FlowExecutionContinuationKey continuationKey = parseContinuationKey(context);
		FlowExecutionRepository repository = getRepository(context);
		FlowExecution flowExecution = getFlowExecution(repository, continuationKey, context);
		ViewSelection selectedView;
		if (!flowExecution.isActive()) {
			selectedView = startFlowExecution(flowExecution, context);
		}
		else {
			selectedView = signalEventIn(flowExecution, context);
		}
		continuationKey = manageStorage(repository, continuationKey, flowExecution);
		return prepareSelectedView(selectedView, continuationKey, flowExecution);
	}

	/**
	 * Creates or loads the flow execution to manipulate based on the value of
	 * <code>flowExecutionId</code>.
	 * 
	 * @param continuationKey the assigned repository continuation key
	 * @param context the context in which the external user event occured
	 * @return the flow execution
	 */
	protected FlowExecution getFlowExecution(FlowExecutionRepository repository,
			FlowExecutionContinuationKey continuationKey, ExternalContext context) {
		if (continuationKey == null) {
			return createFlowExecution(getFlow(context));
		}
		else {
			return loadFlowExecution(repository, continuationKey);
		}
	}

	protected FlowExecutionContinuationKey parseContinuationKey(ExternalContext context) {
		String id = extractFlowExecutionId(context);
		if (!StringUtils.hasText(id)) {
			return null;
		}
		return (FlowExecutionContinuationKey)continuationKeyFormatter
				.parseValue(id, FlowExecutionContinuationKey.class);
	}

	/**
	 * Obtain a unique flow execution id from given event.
	 * @param context the context in which the external user event occured
	 * @return the obtained id or <code>null</code> if not found
	 */
	protected String extractFlowExecutionId(ExternalContext context) {
		return verifySingleStringInputParameter(getFlowExecutionIdParameterName(), context.getRequestParameterMap()
				.get(getFlowExecutionIdParameterName()));
	}

	/**
	 * Create a new flow execution for given flow. Subclasses could redefine
	 * this if they wish to use a specialized FlowExecution implementation
	 * class.
	 * @param flow the flow
	 * @return the created flow execution
	 */
	protected FlowExecution createFlowExecution(Flow flow) {
		FlowExecution flowExecution = new FlowExecutionImpl(flow, getListeners(flow));
		if (logger.isDebugEnabled()) {
			logger.debug("Created a new flow execution for flow definition '" + flow.getId() + "'");
		}
		return flowExecution;
	}

	/**
	 * Obtain a flow to launch a new execution of. If there is a
	 * {@link #getFlowIdParameterName()} parameter specified in the event, the
	 * flow with that id will be returned after lookup using the flow locator.
	 * If no {@link #getFlowIdParameterName()} parameter is present in the
	 * event, the default top-level flow will be returned.
	 * @param context the context in which the external user event occured
	 * @return the flow definition to launch
	 */
	protected Flow getFlow(ExternalContext context) {
		String flowId = extractFlowId(context);
		if (StringUtils.hasText(flowId)) {
			return getFlowLocator().getFlow(flowId);
		}
		else {
			throw new IllegalArgumentException("The flow to launch must be provided by the client via the '"
					+ getFlowIdParameterName() + "' parameter, yet no such parameter was provided in this event."
					+ " Parameters provided were " + StylerUtils.style(context.getRequestParameterMap()));
		}
	}

	/**
	 * Obtain a unique flow execution id from given event.
	 * @param context the context in which the external user event occured
	 * @return the obtained id or <code>null</code> if not found
	 */
	protected String extractFlowId(ExternalContext context) {
		return verifySingleStringInputParameter(getFlowIdParameterName(), context.getRequestParameterMap().get(
				getFlowIdParameterName()));

	}

	/**
	 * Start the flow execution
	 * @param flowExecution the execution to start
	 * @param context the external context that trigged flow execution creation
	 * @return the selected starting view
	 * @throws StateException an exception occured during the execution of the
	 * start operation
	 */
	protected ViewSelection startFlowExecution(FlowExecution flowExecution, ExternalContext context)
			throws StateException {
		return flowExecution.start(context);
	}

	/**
	 * Load an existing FlowExecution based on data in the specified source
	 * event.
	 * @param continuationKey the unique id of the flow execution
	 * @param context the context in which the external user event occured
	 */
	public FlowExecution loadFlowExecution(FlowExecutionRepository repository,
			FlowExecutionContinuationKey continuationKey) {
		// client is participating in an existing flow execution, retrieve
		// information about it
		FlowExecution flowExecution = repository.getFlowExecution(continuationKey);
		// rehydrate the execution if neccessary (if it had been serialized out)
		flowExecution.rehydrate(getFlowLocator(), this);
		if (logger.isDebugEnabled()) {
			logger.debug("Loaded existing flow execution from repository with id '" + continuationKey + "'");
		}
		return flowExecution;
	}

	/**
	 * Signal the occurence of the specified event on an existing flow
	 * @param flowExecution the existing flow
	 * @param context the context in which the external user event occured
	 * @return the raw or unprepared view descriptor of the model and view to
	 * render
	 * @throws StateException an exception occured during event processing
	 */
	protected ViewSelection signalEventIn(FlowExecution flowExecution, ExternalContext context) throws StateException {
		return flowExecution.signalEvent(extractEventId(context), context);
	}

	/**
	 * Obtain this event's id from the parameter map.
	 * <p>
	 * This is a multi-step process consisting of:
	 * <ol>
	 * <li>Try the {@link #getEventIdParameterName()} parameter first, if it is
	 * present, return its value as the eventId.
	 * <li>Try a parameter search looking for parameters of the format:
	 * {@link #getEventIdParameterName()}_value. If a match is found, return
	 * the value as the eventId.
	 * </ol>
	 * @param context the context in which the external user event occured
	 * @param request the http servlet request
	 * @return the event id
	 */
	protected String extractEventId(ExternalContext context) throws IllegalArgumentException {
		Object parameter = findParameter(getEventIdParameterName(), context.getRequestParameterMap());
		String eventId = verifySingleStringInputParameter(getEventIdParameterName(), parameter);
		Assert.hasText(eventId, "No eventId could be obtained: make sure the client provides the '"
				+ getEventIdParameterName() + "' parameter as input; "
				+ "the parameters provided for this request were:"
				+ StylerUtils.style(context.getRequestParameterMap()));
		if (eventId.equals(getNotSetEventIdParameterMarker())) {
			throw new IllegalArgumentException("The received eventId was the 'not set' marker '"
					+ getNotSetEventIdParameterMarker()
					+ "' -- this is likely a client view (jsp, etc) configuration error --" + "the '"
					+ getEventIdParameterName() + "' parameter must be set to a valid event");
		}
		return eventId;
	}

	/**
	 * Save the flow execution to the repository.
	 * @param repository the repository
	 * @param continuationKey the assigned repository continuation key
	 * @param flowExecution the execution
	 */
	protected void saveFlowExecution(FlowExecutionRepository repository, FlowExecutionContinuationKey continuationKey,
			FlowExecution flowExecution) {
		repository.putFlowExecution(continuationKey, flowExecution);
		if (logger.isDebugEnabled()) {
			logger.debug("Saved flow execution out to the repository with id '" + continuationKey + "'");
		}
	}

	/**
	 * Remove the flow execution from the repository.
	 * @param repository the repository
	 * @param continuationKey the assigned repository continuation key
	 * @param flowExecution the execution
	 */
	protected void removeFlowExecution(FlowExecutionRepository repository,
			FlowExecutionContinuationKey continuationKey, FlowExecution flowExecution) {
		// event processing resulted in a previously saved flow execution
		// ending, cleanup
		repository.invalidateConversation(continuationKey.getConversationId());
		if (logger.isDebugEnabled()) {
			logger.debug("Removed flow execution from the repository with id '" + continuationKey + "'");
		}
	}

	/**
	 * Handles updating repository as neccessary for the manipulated
	 * FlowExecution. Saves the FlowExecution out to if the execution is still
	 * active. Removes the FlowExecution if it is no longer active.
	 * @param repository the repository
	 * @param continuationKey the assigned repository continuation key
	 * @param flowExecution the execution
	 */
	protected FlowExecutionContinuationKey manageStorage(FlowExecutionRepository repository,
			FlowExecutionContinuationKey continuationKey, FlowExecution flowExecution) {
		if (flowExecution.isActive()) {
			continuationKey = generateContinuationKey(repository, flowExecution, continuationKey);
			saveFlowExecution(repository, continuationKey, flowExecution);
		}
		else {
			if (continuationKey != null) {
				removeFlowExecution(repository, continuationKey, flowExecution);
				continuationKey = null;
			}
		}
		return continuationKey;
	}

	protected FlowExecutionContinuationKey generateContinuationKey(FlowExecutionRepository repository,
			FlowExecution flowExecution, FlowExecutionContinuationKey previousKey) {
		if (previousKey == null) {
			return repository.generateContinuationKey(flowExecution);
		}
		else {
			return repository.generateContinuationKey(flowExecution, previousKey.getConversationId());
		}
	}

	/**
	 * Perform any processing necessary before the view selection made is
	 * returned to the client of the flow execution manager and rendered out.
	 * This implementation adds a number of <i>infrastructure attributes</i> to
	 * the model that will be exposed to the view so clients may record
	 * information about the flow to support participation in the flow on a
	 * subsequent request. More specifically, this method will add the
	 * {@link #FLOW_EXECUTION_CONTEXT_ATTRIBUTE},
	 * {@link #FLOW_EXECUTION_ID_ATTRIBUTE} and
	 * {@link #CURRENT_STATE_ID_ATTRIBUTE}.
	 * @param selectedView the view selection to be prepared
	 * @param continuationKey the assigned repository continuation key
	 * @param continuationKey the unique id of the flow execution
	 * @param flowExecutionContext the flow context providing info about the
	 * flow execution
	 * @return the prepped view selection
	 */
	protected ViewSelection prepareSelectedView(ViewSelection selectedView,
			FlowExecutionContinuationKey continuationKey, FlowExecutionContext flowExecutionContext) {
		if (flowExecutionContext.isActive() && selectedView != null) {
			String id = formatContinuationKey(continuationKey);
			if (selectedView.isRedirect()) {
				selectedView.addObject(getFlowExecutionIdParameterName(), id);
			}
			else {
				exposeFlowExecutionAttributes(selectedView.getModel(), id, flowExecutionContext);
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Returning selected view to client " + selectedView);
		}
		return selectedView;
	}

	protected String formatContinuationKey(FlowExecutionContinuationKey key) {
		return continuationKeyFormatter.formatValue(key);
	}

	/**
	 * Stores contextual info in the view model, including the flow execution
	 * context, flow execution id
	 * @param model the view model
	 * @param flowExecutionId the flow execution id
	 * @param flowExecutionContext the flow execution context
	 */
	protected void exposeFlowExecutionAttributes(Map model, String flowExecutionId,
			FlowExecutionContext flowExecutionContext) {
		// make the entire flow execution context available in the model
		model.put(FLOW_EXECUTION_CONTEXT_ATTRIBUTE, flowExecutionContext);
		// make the unique flow execution id and current state id
		// available in the model as convenience to views
		model.put(FLOW_EXECUTION_ID_ATTRIBUTE, flowExecutionId);
		model.put(CURRENT_STATE_ID_ATTRIBUTE, flowExecutionContext.getCurrentState().getId());
	}

	// utility methods

	/**
	 * Utility method that makes sure the value for the specified parameter, if
	 * present, is a single valued string.
	 * @param parameterName the parameter name
	 * @param parameterValue the parameter value
	 * @return the string value
	 */
	public static String verifySingleStringInputParameter(String parameterName, Object parameterValue) {
		String str = null;
		if (parameterValue != null) {
			try {
				str = (String)parameterValue;
			}
			catch (ClassCastException e) {
				if (parameterValue.getClass().isArray()) {
					throw new IllegalArgumentException("The '" + parameterName
							+ "' parameter was unexpectedly set to an array with values: "
							+ StylerUtils.style(parameterValue) + "; this is likely a view configuration error: "
							+ "make sure you submit a single string value for the '" + parameterName + "' parameter!");
				}
				else {
					throw new IllegalArgumentException("Parameter '" + parameterName
							+ " should have been a single string value but was '" + parameterValue + "' of class + "
							+ parameterValue.getClass());
				}
			}
		}
		return str;
	}

	// support methods

	/**
	 * Obtain a named parameter from the event parameters. This method will try
	 * to obtain a parameter value using the following algorithm:
	 * <ol>
	 * <li>Try to get the parameter value using just the given <i>logical</i>
	 * name. This handles parameters of the form <tt>logicalName = value</tt>.
	 * For normal parameters, e.g. submitted using a hidden HTML form field,
	 * this will return the requested value.</li>
	 * <li>Try to obtain the parameter value from the parameter name, where the
	 * parameter name in the event is of the form
	 * <tt>logicalName_value = xyz</tt> with "_" being the specified
	 * delimiter. This deals with parameter values submitted using an HTML form
	 * submit button.</li>
	 * <li>If the value obtained in the previous step has a ".x" or ".y"
	 * suffix, remove that. This handles cases where the value was submitted
	 * using an HTML form image button. In this case the parameter in the event
	 * would actually be of the form <tt>logicalName_value.x = 123</tt>.
	 * </li>
	 * </ol>
	 * @param logicalParameterName the <i>logical</i> name of the request
	 * parameter
	 * @param parameters the available parameter map
	 * @return the value of the parameter, or <code>null</code> if the
	 * parameter does not exist in given request
	 */
	protected Object findParameter(String logicalParameterName, Map parameters) {
		// first try to get it as a normal name=value parameter
		Object value = parameters.get(logicalParameterName);
		if (value != null) {
			return value;
		}
		// if no value yet, try to get it as a name_value=xyz parameter
		String prefix = logicalParameterName + parameterDelimiter;
		Iterator paramNames = parameters.keySet().iterator();
		while (paramNames.hasNext()) {
			String paramName = (String)paramNames.next();
			if (paramName.startsWith(prefix)) {
				String strValue = paramName.substring(prefix.length());
				// support images buttons, which would submit parameters as
				// name_value.x=123
				if (strValue.endsWith(".x") || strValue.endsWith(".y")) {
					strValue = strValue.substring(0, strValue.length() - 2);
				}
				return strValue;
			}
		}
		// we couldn't find the parameter value
		return null;
	}

	/**
	 * A holder that holds a listener plus a set of criteria defining the flows
	 * in which that listener applies.
	 * @author Keith Donald
	 */
	public static final class ConditionalFlowExecutionListenerHolder {
		private FlowExecutionListener listener;

		private Set criteriaSet = CollectionFactory.createLinkedSetIfPossible(3);

		public ConditionalFlowExecutionListenerHolder(FlowExecutionListener listener) {
			this.listener = listener;
		}

		public FlowExecutionListener getListener() {
			return listener;
		}

		public void add(FlowExecutionListenerCriteria criteria) {
			criteriaSet.add(criteria);
		}

		public void remove(FlowExecutionListenerCriteria criteria) {
			criteriaSet.remove(criteria);
		}

		public boolean isCriteriaSetEmpty() {
			return criteriaSet.isEmpty();
		}

		public boolean equals(Object o) {
			if (!(o instanceof ConditionalFlowExecutionListenerHolder)) {
				return false;
			}
			return listener.equals(((ConditionalFlowExecutionListenerHolder)o).listener);
		}

		public int hashCode() {
			return listener.hashCode();
		}

		public boolean listenerAppliesTo(Flow flow) {
			Iterator it = criteriaSet.iterator();
			while (it.hasNext()) {
				FlowExecutionListenerCriteria criteria = (FlowExecutionListenerCriteria)it.next();
				if (criteria.appliesTo(flow)) {
					return true;
				}
			}
			return false;
		}
	}
}