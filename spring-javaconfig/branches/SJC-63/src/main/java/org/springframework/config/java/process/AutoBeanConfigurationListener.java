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
import java.lang.reflect.Modifier;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.config.java.annotation.AutoBean;
import org.springframework.core.annotation.AnnotationUtils;

/**
 * Configuration listener that registers autowired bean definitions in the
 * factory for &#64;AutoBean methods.
 * 
 * @author Rod Johnson
 */
class AutoBeanConfigurationListener extends ConfigurationListenerSupport {

	@Override
	public boolean understands(Class<?> configurationClass) {
		return AutoBeanMethodProcessor.findAutoBeanCreationMethods(configurationClass).size() > 0;
	}

	@Override
	public void handleEvent(Reactor reactor, MethodEvent event) {

		Method m = event.method;
		ProcessingContext pc = event.processingContext;

		AutoBean autoBean = AnnotationUtils.findAnnotation(m, AutoBean.class);
		if (autoBean != null) {
			// Create a bean definition for this class
			if (m.getReturnType().isInterface()) {
				throw new BeanDefinitionStoreException("Cannot use AutoBean of interface type " + m.getReturnType()
						+ ": don't know what class to instantiate; processing @AutoBean method " + m);
			}

			RootBeanDefinition bd = new RootBeanDefinition(m.getReturnType());
			bd.setAutowireMode(autoBean.autowire().value());
			pc.registerBeanDefinition(m.getName(), bd, !Modifier.isPublic(m.getModifiers()));
			// one bean definition created
			pc.beanDefsGenerated++;
		}

	}
}
