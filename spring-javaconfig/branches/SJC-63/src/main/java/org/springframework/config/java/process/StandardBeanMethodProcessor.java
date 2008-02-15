/*
 * Copyright 2002-2008 the original author or authors.
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

import java.lang.reflect.Method;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.config.java.annotation.Bean;
import org.springframework.config.java.naming.BeanNamingStrategy;

class StandardBeanMethodProcessor extends AbstractBeanMethodProcessor {
	private static final Log log = LogFactory.getLog(StandardBeanMethodProcessor.class);

	private final ConfigurableListableBeanFactory owningBeanFactory;

	private final BeanNamingStrategy namingStrategy;

	private final BeanNameTrackingDefaultListableBeanFactory childTrackingFactory;

	private final MethodBeanWrapper beanWrapper;

	public StandardBeanMethodProcessor(ProcessingContext pc) {
		super(Bean.class, pc);
		this.owningBeanFactory = pc.owningBeanFactory;
		this.childTrackingFactory = pc.childFactory;
		this.namingStrategy = pc.beanNamingStrategy;
		this.beanWrapper = new MethodBeanWrapper(pc);
	}

	public Object processMethod(Method targetMethod) {
		throw new UnsupportedOperationException("not implemented");
	}

	public String getBeanName(Method m) {
		return namingStrategy.getBeanName(m);
	}

	boolean isCurrentlyInCreation(String beanName) {
		return owningBeanFactory.isCurrentlyInCreation(beanName)
				|| childTrackingFactory.isCurrentlyInCreation(beanName);
	}

	public Object createNewOrGetCachedSingletonBean(String beanName, EnhancerMethodInvoker callback) {

		if (isCurrentlyInCreation(beanName)) {
			if (log.isDebugEnabled())
				log.debug(beanName + " currenty in creation, created one");
			try {
				return beanWrapper.wrapResult(beanName, callback);
			}
			catch (Throwable t) {
				throw new RuntimeException(t);
			}
		}

		if (log.isDebugEnabled())
			log.debug(beanName + " not in creation, asked the BF for one");

		return childTrackingFactory.getBean(beanName);

	}

	public static boolean isBeanCreationMethod(Method candidateMethod) {
		return new StandardBeanMethodProcessor(new ProcessingContext()).understands(candidateMethod);
	}

	public static Collection<Method> findBeanCreationMethods(Class<?> configurationClass) {
		return new StandardBeanMethodProcessor(new ProcessingContext()).findMatchingMethods(configurationClass);
	}
}