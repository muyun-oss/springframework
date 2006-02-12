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
package org.springframework.webflow.execution.repository.support;

import org.springframework.webflow.ExternalContext;
import org.springframework.webflow.ExternalContext.SharedMap;
import org.springframework.webflow.execution.repository.FlowExecutionRepository;
import org.springframework.webflow.execution.repository.FlowExecutionRepositoryCreator;

/**
 * Retrieves flow execution repositories from a shared, externally managed map.
 * <p>
 * The map access strategy is configurable by setting the
 * {@link #setSharedMapLocator(SharedMapLocator) sharedMapLocator} property. By
 * default the {@link SessionMapLocator} is used which pulls in the
 * {@link ExternalContext#getSessionMap()}, a shared map backed by a user's
 * HTTP session in a Servlet environment and a Portlet Session in a Portlet
 * environment.
 * <p>
 * When a repository lookup request is initiated if a
 * {@link FlowExecutionRepository} is not present in the retrieved shared map,
 * one will be created by having this object delegate to the configured
 * {@link FlowExecutionRepositoryCreator}, a creational strategy. The newly
 * created repository will then be placed in the shared map where it can be
 * accessed at a later point in time. Synchronization will occur on the mutex of
 * the {@link SharedMap} to ensure thread safety.
 * 
 * @author Keith Donald
 */
public class SharedMapFlowExecutionRepositoryFactory extends AbstractFlowExecutionRepositoryFactory {

	/**
	 * The map locator that returns a <code>java.util.Map</code> that allows
	 * this storage implementation to access a FlowExecutionRepository by a
	 * unique key.
	 * <p>
	 * The default is the {@link SessionMapLocator} which returns a map backed
	 * by the {@link ExternalContext#getSessionMap}.
	 */
	private SharedMapLocator sharedMapLocator = new SessionMapLocator();

	/**
	 * Creates a new shared map repository factory.
	 * @param repositoryCreator the repository creational strategy
	 */
	public SharedMapFlowExecutionRepositoryFactory(FlowExecutionRepositoryCreator repositoryCreator) {
		super(repositoryCreator);
	}

	/**
	 * Returns the shared, external map locator.
	 */
	public SharedMapLocator getSharedMapLocator() {
		return sharedMapLocator;
	}

	/**
	 * Sets the shared, external map locator.
	 */
	public void setSharedMapLocator(SharedMapLocator sharedMapLocator) {
		this.sharedMapLocator = sharedMapLocator;
	}

	public FlowExecutionRepository getRepository(ExternalContext context) {
		SharedMap repositoryMap = sharedMapLocator.getMap(context);
		// synchronize on the shared map's mutex for thread safety
		synchronized (repositoryMap.getMutex()) {
			Object repositoryKey = getRepositoryKey();
			FlowExecutionRepository repository = (FlowExecutionRepository)repositoryMap.get(repositoryKey);
			if (repository == null) {
				repository = getRepositoryCreator().createRepository();
				repositoryMap.put(repositoryKey, repository);
			}
			else {
				getRepositoryCreator().rehydrateRepository(repository);
			}
			return repository;
		}
	}

	/**
	 * Returns the shared map repository attribute key.
	 */
	protected Object getRepositoryKey() {
		return getRepositoryCreator().getClass().getName();
	}

	/**
	 * Strategy interface for objects that can lookup externally managed data
	 * map shared by multiple threads.
	 * <p>
	 * Objects implementing this interface act as factories for attribute
	 * sources that when invoked pull attributes from an externally managed
	 * source.
	 * <p>
	 * Used by
	 * {@link org.springframework.webflow.execution.repository.support.SharedMapFlowExecutionRepositoryFactory}
	 * to make the underlying storage map of an flow execution repository
	 * pluggable.
	 * 
	 * @author Keith Donald
	 * @author Erwin Vervaet
	 */
	public interface SharedMapLocator {

		/**
		 * Returns a mutable attribute map providing access to an underlying
		 * data store.
		 * @param context an external user context object which may provide
		 * assistance in locating the datastore.
		 * @return the shared, mutable attribute source providing access to the
		 * data store
		 */
		public SharedMap getMap(ExternalContext context);
	}

	/**
	 * A {@link SharedMapLocator} that returns the external context session map.
	 * @author Keith Donald
	 */
	public static class SessionMapLocator implements SharedMapLocator {
		public SharedMap getMap(ExternalContext context) {
			return context.getSessionMap();
		}
	}
}