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
package org.springframework.webflow.test;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.webflow.config.DefaultFlowServiceLocator;
import org.springframework.webflow.definition.FlowDefinition;
import org.springframework.webflow.definition.registry.FlowDefinitionConstructionException;
import org.springframework.webflow.definition.registry.FlowDefinitionHolder;
import org.springframework.webflow.definition.registry.FlowDefinitionRegistry;
import org.springframework.webflow.definition.registry.FlowDefinitionRegistryImpl;
import org.springframework.webflow.engine.Flow;

/**
 * A stub flow service locator implementation suitable for a test environment.
 * <p>
 * Allows programmatic registration of subflows needed by a flow execution being tested, see
 * {@link #registerSubflow(Flow)}. Subflows registered are typically stubs that verify parent flow input and output
 * scenarios.
 * <p>
 * Also supports programmatic registration of additional custom services needed by a flow (such as Actions) managed in a
 * backing Spring {@link ConfigurableBeanFactory}. See the {@link #registerBean(String, Object)} method. Beans
 * registered are typically mocks or stubs of business services invoked by the flow.
 * 
 * @author Keith Donald
 */
public class MockFlowServiceLocator extends DefaultFlowServiceLocator {

	/**
	 * Creates a new mock flow service locator.
	 */
	public MockFlowServiceLocator() {
		super(new FlowDefinitionRegistryImpl(), new StaticListableBeanFactory());
	}

	/**
	 * Register a subflow definition in the backing flow registry, typically to support a flow execution test. For test
	 * scenarios, the subflow is often a stub used to verify parent flow input and output mapping behavior.
	 * @param subflow the subflow
	 */
	public void registerSubflow(Flow subflow) {
		getMockSubflowRegistry().registerFlowDefinition(new StaticFlowDefinitionHolder(subflow));
	}

	/**
	 * Register a bean in the backing bean factory, typically to support a flow execution test. For test scenarios, if
	 * the bean is a service invoked by a bean invoking action it is often a stub or dynamic mock implementation of the
	 * service's business interface.
	 * @param beanName the bean name
	 * @param bean the singleton instance
	 */
	public void registerBean(String beanName, Object bean) {
		((StaticListableBeanFactory) getBeanFactory()).addBean(beanName, bean);
	}

	public FlowDefinitionRegistry getMockSubflowRegistry() {
		return (FlowDefinitionRegistry) getSubflowLocator();
	}

	private static class StaticFlowDefinitionHolder implements FlowDefinitionHolder {
		private Flow flow;

		public StaticFlowDefinitionHolder(Flow flow) {
			this.flow = flow;
		}

		public String getFlowDefinitionId() {
			return flow.getId();
		}

		public FlowDefinition getFlowDefinition() throws FlowDefinitionConstructionException {
			return flow;
		}

		public void refresh() throws FlowDefinitionConstructionException {
			// nothing to do
		}

		public boolean equals(Object o) {
			if (!(o instanceof StaticFlowDefinitionHolder)) {
				return false;
			}
			StaticFlowDefinitionHolder other = (StaticFlowDefinitionHolder) o;
			return flow.equals(other.flow);
		}

		public int hashCode() {
			return flow.hashCode();
		}
	}
}