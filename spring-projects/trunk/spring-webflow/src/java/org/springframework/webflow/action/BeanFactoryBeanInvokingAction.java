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
package org.springframework.webflow.action;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.util.Assert;
import org.springframework.webflow.RequestContext;

/**
 * Thin action proxy that delegates to an arbitrary bean managed in a Spring
 * bean factory. The bean does not have to implement any special interface to be
 * invoked.
 * <p>
 * To use this class, you configure the name of the bean you wish to invoke and
 * what method on that bean should be invoked with what arguments, typically using
 * flow request context attributes.
 * <p>
 * Example configuration and usage:
 * <pre>
 * BeanFactoryInvokingAction action = new BeanFactoryBeanInvokingAction();
 * action.setBeanFactory(...);
 * MockRequestContext context = new MockRequestContext();
 * context.setAttribute("method", new MethodSignature("myMethod"));
 * context.setAttribute("bean", "myBean");
 * action.execute(context);
 * </pre>
 * 
 * @author Keith Donald
 */
public class BeanFactoryBeanInvokingAction extends AbstractBeanInvokingAction implements BeanFactoryAware {

	/**
	 * The bean name action execution property.
	 */
	public static final String BEAN_PROPERTY = "bean";

	/**
	 * The name of the default bean to invoke when this action is executed.
	 * <p>
	 * Optional: the bean to invoke may instead be specified as an
	 * {@link RequestContext#getAttributes() context attribute}.
	 */
	private String beanName;

	/**
	 * The bean factory for loading beans to invoke by <code>id</code>.
	 */
	private BeanFactory beanFactory;

	/**
	 * Returns the target bean name.
	 */
	public String getBeanName() {
		return beanName;
	}

	/**
	 * Set the name of the target bean to invoke. The bean will be looked up in
	 * the bean factory on action execution.
	 * 
	 * @param beanName the target bean name
	 */
	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	/**
	 * Returns the configured bean factory member.
	 * @return the bean factory
	 */
	protected BeanFactory getBeanFactory() {
		return beanFactory;
	}

	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	/**
	 * Retrieves the bean to invoke. This implementation loads the bean by name
	 * from the BeanFactory. The name to use may be specified in this class, or
	 * alternatively as a action execution property, allowing this action
	 * instance to be parameterized to invoke many different beans throughout
	 * its life.
	 */
	protected Object getBean(RequestContext context) {
		String beanName = context.getAttributes().getString(BEAN_PROPERTY, getBeanName());
		Assert.hasText(beanName,
				"The bean name to invoke was not specified: set the 'beanName' property or 'bean' context attribute");
		return getBeanFactory().getBean(beanName);
	}
}