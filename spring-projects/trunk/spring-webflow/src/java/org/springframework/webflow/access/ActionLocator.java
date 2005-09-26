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
package org.springframework.webflow.access;

import org.springframework.webflow.Action;

/**
 * Service locator interface for retrieving an action by id. Typically used
 * needed at configuration time to wire in Action implementations with Flow
 * Action States. May also be used at runtime to lookup action prototypes by id.
 * 
 * @author Keith Donald
 * @author Erwin Vervaet
 */
public interface ActionLocator {

	/**
	 * Lookup an action with specified id.
	 * @param id the action id
	 * @return the action
	 * @throws FlowArtifactLookupException when the action cannot be found
	 */
	public Action getAction(String id) throws FlowArtifactLookupException;
}