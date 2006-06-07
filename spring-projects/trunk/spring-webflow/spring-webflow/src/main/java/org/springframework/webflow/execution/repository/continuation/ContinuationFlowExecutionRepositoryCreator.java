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

import org.springframework.util.Assert;
import org.springframework.webflow.execution.repository.FlowExecutionKey;
import org.springframework.webflow.execution.repository.FlowExecutionRepository;
import org.springframework.webflow.execution.repository.support.AbstractFlowExecutionRepositoryCreator;
import org.springframework.webflow.execution.repository.support.FlowExecutionRepositoryServices;
import org.springframework.webflow.util.RandomGuidUidGenerator;
import org.springframework.webflow.util.UidGenerator;

/**
 * Creates continuation-based flow execution repositories.
 * <p>
 * All properties are optional. If a property is not set, the default values
 * will be used.
 * 
 * @author Keith Donald
 */
public class ContinuationFlowExecutionRepositoryCreator extends AbstractFlowExecutionRepositoryCreator {

	/**
	 * The flow execution continuation factory to use.
	 */
	private FlowExecutionContinuationFactory continuationFactory = new SerializedFlowExecutionContinuationFactory();

	/**
	 * The continuation uid generation strategy to use.
	 */
	private transient UidGenerator continuationIdGenerator = new RandomGuidUidGenerator();

	/**
	 * The maximum number of continuations allowed per conversation.
	 */
	private int maxContinuations;

	/**
	 * Creates a new continuation repository creator.
	 * @param repositoryServices the repository services holder
	 */
	public ContinuationFlowExecutionRepositoryCreator(FlowExecutionRepositoryServices repositoryServices) {
		super(repositoryServices);
	}

	/**
	 * Sets the continuation factory that encapsulates the construction of
	 * continuations stored in repositories created by this creator.
	 */
	public void setContinuationFactory(FlowExecutionContinuationFactory continuationFactory) {
		Assert.notNull(continuationFactory, "The continuation factory is required");
		this.continuationFactory = continuationFactory;
	}

	/**
	 * Returns the configured continuation uid generation strategy.
	 */
	public UidGenerator getContinuationIdGenerator() {
		return continuationIdGenerator;
	}

	/**
	 * Sets the continuation uid strategto use generate unique continuation
	 * identifiers for {@link FlowExecutionKey flow execution keys}.
	 */
	public void setContinuationIdGenerator(UidGenerator continuationIdGenerator) {
		Assert.notNull(continuationIdGenerator, "The continuation id generator is required");
		this.continuationIdGenerator = continuationIdGenerator;
	}

	/**
	 * Sets the maximum number of continuations allowed per conversation in
	 * repositories created by this creator.
	 */
	public void setMaxContinuations(int maxContinuations) {
		this.maxContinuations = maxContinuations;
	}

	public FlowExecutionRepository createRepository() {
		ContinuationFlowExecutionRepository repository = new ContinuationFlowExecutionRepository(
				getRepositoryServices());
		repository.setContinuationFactory(continuationFactory);
		repository.setMaxContinuations(maxContinuations);
		repository.setContinuationIdGenerator(continuationIdGenerator);
		return repository;
	}

	public FlowExecutionRepository rehydrateRepository(FlowExecutionRepository repository) {
		ContinuationFlowExecutionRepository impl = (ContinuationFlowExecutionRepository)repository;
		impl.setRepositoryServices(getRepositoryServices());
		impl.setContinuationFactory(continuationFactory);
		impl.setContinuationIdGenerator(continuationIdGenerator);
		return impl;
	}
}