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
package org.springframework.webflow;

/**
 * A facade that provides access to the state of an external system that has
 * interacted with Spring Web Flow.
 * <p>
 * This context object provides a single, consistent interface for internal SWF
 * artifacts to use to reason on and manipulate the state of an external actor
 * calling into SWF to execute flows. It represents the context about a single,
 * <i>external</i> request to manipulate a flow execution.
 * <p>
 * The design of this interface was inspired by JSF's own ExternalContext
 * abstraction, and thus shares the same name for consistency. If a particular
 * external client type does not support all methods defined by this interface,
 * they can just be implemented as returning an empty map.
 * 
 * @author Keith Donald
 */
public interface ExternalContext {

	/**
	 * Returns the path (or identifier) of the application that is executing.
	 * @return the application context path
	 */
	public String getContextPath();

	/**
	 * Returns the path (or identifier) of the dispatcher within the application
	 * that dispatched this request.
	 * @return the dispatcher path
	 */
	public String getDispatcherPath();

	/**
	 * Returns the path info of this external request.
	 * @return the request path info
	 */
	public String getRequestPathInfo();

	/**
	 * Provides access to the parameters associated with the user request that
	 * led to SWF being called. This map is expected to be immutable and cannot
	 * be changed.
	 * @return the immutable request parameter map
	 */
	public ParameterMap getRequestParameterMap();

	/**
	 * Provides access to the external request attribute map, providing a
	 * storage for data local to the current user request and accessible to both
	 * internal and external SWF artifacts.
	 * @return the mutable request attribute map
	 */
	public AttributeMap getRequestMap();

	/**
	 * Provides access to the external session map, providing a storage for data
	 * local to the current user session and accessible to both internal and
	 * external SWF artifacts.
	 * @return the mutable session attribute map
	 */
	public SharedAttributeMap getSessionMap();

	/**
	 * Provides access to the external application map, providing a storage for
	 * data local to the current user application and accessible to both
	 * internal and external SWF artifacts.
	 * @return the mutable application attribute map
	 */
	public SharedAttributeMap getApplicationMap();

}