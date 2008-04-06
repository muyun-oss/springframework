/*
 * Copyright 2002-2007 the original author or authors.
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
package org.springframework.config.java.process;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.config.java.annotation.Configuration;
import org.springframework.config.java.aspects.RequiredMethodInvocationRegistry;
import org.springframework.config.java.aspects.RequiredMethodInvocationTracker;

/**
 * Serves the same purpose as Core Spring's post processor of the same name, but
 * performs Required method-invocation checking with a very different strategy -
 * used in conjunction with the {@link RequiredMethodInvocationTracker} aspect
 * and {@link RequiredMethodInvocationRegistry}.
 * 
 * Note that this bean post processor is not designed to be included in a
 * configuration by the user (thus the package-private visibility). Rather, it
 * is automatically added to configurations if
 * {@link Configuration#checkRequired()} has been set to true.
 * 
 * @see Configuration#checkRequired()
 * @see RequiredMethodInvocationTracker
 * @see RequiredMethodInvocationRegistry
 * 
 * @author Chris Beams
 */
class RequiredAnnotationBeanPostProcessor implements BeanPostProcessor {
	private final RequiredMethodInvocationRegistry invocationRegistry = RequiredMethodInvocationTracker
			.getInvocationRegistry();

	/** no-op */
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	/**
	 * Checks {@link RequiredMethodInvocationRegistry} to ensure that this
	 * bean's required methods have been invoked during dependency injection
	 */
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		invocationRegistry.interrogateRequiredMethods(bean, beanName);
		return bean;
	}

}