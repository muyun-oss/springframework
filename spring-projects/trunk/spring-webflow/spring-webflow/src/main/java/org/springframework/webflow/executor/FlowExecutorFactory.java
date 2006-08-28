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
package org.springframework.webflow.executor;

/**
 * <p>
 * A factory for creating fully-initialized {@link FlowExecutor flow executors}.
 * </p>
 * <p>
 * A flow executor implementation is typically a complex object with several
 * associations with other lower-level services. This factory provides
 * encapsulation of that complexity.
 * 
 * @author Keith Donald
 */
public interface FlowExecutorFactory {
	
	/**
	 * Creates a new flow executor.
	 * @return the flow executor.
	 */
	public FlowExecutor createFlowExecutor();
}