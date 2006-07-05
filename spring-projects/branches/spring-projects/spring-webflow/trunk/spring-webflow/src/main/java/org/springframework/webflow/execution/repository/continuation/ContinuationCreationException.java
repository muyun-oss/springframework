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
package org.springframework.webflow.execution.repository.continuation;

import org.springframework.core.NestedRuntimeException;
import org.springframework.webflow.execution.FlowExecution;

/**
 * Root exception hierarchy for exceptions that occur during FlowExecution
 * serialization into storage.
 * 
 * @author Keith Donald
 */
public class ContinuationCreationException extends NestedRuntimeException {

	/**
	 * The flow execution that could not be serialized.
	 */
	private FlowExecution flowExecution;

	/**
	 * Creates a new serialization exception.
	 * @param continuationId the continuation id
	 * @param flowExecution the flow execution
	 * @param message a descriptive message
	 * @param cause
	 */
	public ContinuationCreationException(FlowExecution flowExecution, String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Returns the flow execution that could not be serialized.
	 */
	public FlowExecution getFlowExecution() {
		return flowExecution;
	}
}