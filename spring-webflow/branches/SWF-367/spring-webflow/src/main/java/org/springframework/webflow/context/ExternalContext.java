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
package org.springframework.webflow.context;

import org.springframework.webflow.core.FlowException;
import org.springframework.webflow.core.collection.MutableAttributeMap;
import org.springframework.webflow.core.collection.ParameterMap;
import org.springframework.webflow.core.collection.SharedAttributeMap;

/**
 * A facade that provides normalized access to an external system that has interacted with Spring Web Flow.
 * <p>
 * This context object provides a normalized interface for internal web flow artifacts to use to reason on and
 * manipulate the state of an external actor calling into SWF to execute flows. It represents the context about a
 * single, <i>external</i> client request to manipulate a flow execution.
 * <p>
 * The design of this interface was inspired by JSF's own ExternalContext abstraction and shares the same name for
 * consistency. If a particular external client type does not support all methods defined by this interface, they can
 * just be implemented as returning an empty map or <code>null</code>.
 * 
 * @author Keith Donald
 * @author Erwin Vervaet
 */
public interface ExternalContext {

	/**
	 * Returns the id of the flow the client requested be launched or resumed.
	 * @return the flow id, never null
	 */
	public String getFlowId();

	/**
	 * Returns the execution the client requested to be resumed.
	 * @return the flow execution key, may be null if this is not a resume request
	 */
	public String getFlowExecutionKey();

	/**
	 * Returns the type of this external request.
	 * @return the request method.
	 */
	public String getRequestMethod();

	/**
	 * Returns the path of this request as a ordered list of fields.
	 * @return the elements of the request path
	 */
	public String[] getRequestElements();

	/**
	 * Provides access to the parameters associated with the user request that led to SWF being called. This map is
	 * expected to be immutable and cannot be changed.
	 * @return the immutable request parameter map
	 */
	public ParameterMap getRequestParameterMap();

	/**
	 * Provides access to the external request attribute map, providing a storage for data local to the current user
	 * request and accessible to both internal and external SWF artifacts.
	 * @return the mutable request attribute map
	 */
	public MutableAttributeMap getRequestMap();

	/**
	 * Provides access to the external session map, providing a storage for data local to the current user session and
	 * accessible to both internal and external SWF artifacts.
	 * @return the mutable session attribute map
	 */
	public SharedAttributeMap getSessionMap();

	/**
	 * Provides access to the <i>global</i> external session map, providing a storage for data globally accross the
	 * user session and accessible to both internal and external SWF artifacts.
	 * <p>
	 * Note: most external context implementations do not distinguish between the concept of a "local" user session
	 * scope and a "global" session scope. The Portlet world does, but not the Servlet for example. In those cases
	 * calling this method returns the same map as calling {@link #getSessionMap()}.
	 * @return the mutable global session attribute map
	 */
	public SharedAttributeMap getGlobalSessionMap();

	/**
	 * Provides access to the external application map, providing a storage for data local to the current user
	 * application and accessible to both internal and external SWF artifacts.
	 * @return the mutable application attribute map
	 */
	public SharedAttributeMap getApplicationMap();

	/**
	 * Provides access to the context object for the current environment.
	 * @return the environment specific context object
	 */
	public Object getContext();

	/**
	 * Provides access to the request object for the current environment.
	 * @return the environment specific request object.
	 */
	public Object getRequest();

	/**
	 * Provides access to the response object for the current environment.
	 * @return the environment specific response object.
	 */
	public Object getResponse();

	/**
	 * Request that a flow execution redirect be sent as the response. A flow execution redirect tells the caller to
	 * refresh this flow execution in a new request.
	 */
	public void sendFlowExecutionRedirect();

	/**
	 * Request that a flow definition redirect be sent as the response. A flow definition redirect tells the caller to
	 * start a new execution of the flow definition with the input provided.
	 */
	public void sendFlowDefinitionRedirect(String flowId, String[] requestElements, ParameterMap requestParameters);

	/**
	 * Request that a external redirect be sent as the response. An external redirect tells the caller to access the
	 * resource at the given resource URI.
	 * @param resourceUri the resource URI string
	 */
	public void sendExternalRedirect(String resourceUri);

	public void setPausedResult(String flowExecutionKey);

	public void setEndedResult();

	public void setExceptionResult(FlowException e);

}