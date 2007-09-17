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
package org.springframework.webflow.executor;

import org.springframework.util.Assert;
import org.springframework.webflow.context.ExternalContext;
import org.springframework.webflow.context.ExternalContextHolder;
import org.springframework.webflow.core.FlowException;
import org.springframework.webflow.core.collection.MutableAttributeMap;
import org.springframework.webflow.definition.FlowDefinition;
import org.springframework.webflow.definition.FlowId;
import org.springframework.webflow.definition.registry.FlowDefinitionLocator;
import org.springframework.webflow.execution.FlowExecution;
import org.springframework.webflow.execution.FlowExecutionFactory;
import org.springframework.webflow.execution.FlowExecutionKey;
import org.springframework.webflow.execution.repository.FlowExecutionLock;
import org.springframework.webflow.execution.repository.FlowExecutionRepository;

/**
 * The default implementation of the central facade for <i>driving</i> the execution of flows within an application.
 * <p>
 * This object is responsible for creating and starting new flow executions as requested by clients, as well as
 * signaling events for processing by existing, paused executions (that are waiting to be resumed in response to a user
 * event).
 * <p>
 * This object is a facade or entry point into the Spring Web Flow execution system and makes the overall system easier
 * to use. The name <i>executor</i> was chosen as <i>executors drive executions</i>.
 * <p>
 * <b>Commonly used configurable properties</b><br>
 * <table border="1">
 * <tr>
 * <td><b>name</b></td>
 * <td><b>description</b></td>
 * <td><b>default</b></td>
 * </tr>
 * <tr>
 * <td>definitionLocator</td>
 * <td>The service locator responsible for loading flow definitions to execute.</td>
 * <td>None</td>
 * </tr>
 * <tr>
 * <td>executionFactory</td>
 * <td>The factory responsible for creating new flow executions.</td>
 * <td>None</td>
 * </tr>
 * <tr>
 * <td>executionRepository</td>
 * <td>The repository responsible for managing flow execution persistence.</td>
 * <td>None</td>
 * </tr>
 * </table>
 * </p>
 * 
 * @see FlowDefinitionLocator
 * @see FlowExecutionFactory
 * @see FlowExecutionRepository
 * 
 * @author Erwin Vervaet
 * @author Keith Donald
 * @author Colin Sampaleanu
 */
public class FlowExecutorImpl implements FlowExecutor {

	/**
	 * A locator to access flow definitions registered in a central registry.
	 */
	private FlowDefinitionLocator definitionLocator;

	/**
	 * An abstract factory for creating a new execution of a flow definition.
	 */
	private FlowExecutionFactory executionFactory;

	/**
	 * An repository used to save, update, and load existing flow executions to/from a persistent store.
	 */
	private FlowExecutionRepository executionRepository;

	/**
	 * Create a new flow executor.
	 * @param definitionLocator the locator for accessing flow definitions to execute
	 * @param executionFactory the factory for creating executions of flow definitions
	 * @param executionRepository the repository for persisting paused flow executions
	 */
	public FlowExecutorImpl(FlowDefinitionLocator definitionLocator, FlowExecutionFactory executionFactory,
			FlowExecutionRepository executionRepository) {
		Assert.notNull(definitionLocator, "The locator for accessing flow definitions is required");
		Assert.notNull(executionFactory, "The execution factory for creating new flow executions is required");
		Assert.notNull(executionRepository, "The repository for persisting flow executions is required");
		this.definitionLocator = definitionLocator;
		this.executionFactory = executionFactory;
		this.executionRepository = executionRepository;
	}

	public FlowExecutionResult launchExecution(FlowId id, MutableAttributeMap input, ExternalContext context) {
		// expose external context as a thread-bound service
		ExternalContextHolder.setExternalContext(context);
		try {
			FlowDefinition flowDefinition = definitionLocator.getFlowDefinition(id);
			FlowExecution flowExecution = executionFactory.createFlowExecution(flowDefinition);
			flowExecution.start(input, context);
			if (flowExecution.isActive()) {
				executionRepository.putFlowExecution(flowExecution);
				return FlowExecutionResult.getPausedResult(flowExecution.getKey());
			} else {
				return FlowExecutionResult.getEndedResult();
			}
		} catch (FlowException e) {
			handleException(e, context);
			return FlowExecutionResult.getExceptionResult(e);
		} finally {
			ExternalContextHolder.setExternalContext(null);
		}
	}

	public FlowExecutionResult resumeExecution(String encodedKey, ExternalContext context) {
		// expose external context as a thread-bound service
		ExternalContextHolder.setExternalContext(context);
		try {
			FlowExecutionKey key = executionRepository.parseFlowExecutionKey(encodedKey);
			FlowExecutionLock lock = executionRepository.getLock(key);
			// make sure we're the only one manipulating the flow execution
			lock.lock();
			try {
				FlowExecution flowExecution = executionRepository.getFlowExecution(key);
				flowExecution.resume(context);
				if (flowExecution.isActive()) {
					executionRepository.putFlowExecution(flowExecution);
					return FlowExecutionResult.getPausedResult(flowExecution.getKey());
				} else {
					// execution ended => remove it from the repository
					executionRepository.removeFlowExecution(flowExecution);
					return FlowExecutionResult.getEndedResult();
				}
			} finally {
				lock.unlock();
			}
		} catch (FlowException e) {
			handleException(e, context);
			return FlowExecutionResult.getExceptionResult(e);
		} finally {
			ExternalContextHolder.setExternalContext(null);
		}
	}

	private void handleException(FlowException e, ExternalContext context) {
		// TODO
	}
}