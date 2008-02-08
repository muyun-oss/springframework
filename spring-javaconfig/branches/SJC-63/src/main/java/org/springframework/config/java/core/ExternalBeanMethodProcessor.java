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
package org.springframework.config.java.core;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.config.java.annotation.ExternalBean;
import org.springframework.config.java.naming.BeanNamingStrategy;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;

/**
 * {@link BeanMethodProcessor} capable of processing {@link ExternalBean}-annotated
 * methods.
 * 
 * @author Chris Beams
 */
public class ExternalBeanMethodProcessor extends AbstractBeanMethodProcessor {

	public ExternalBeanMethodProcessor(ConfigurableListableBeanFactory owningBeanFactory,
			BeanNamingStrategy namingStrategy, ProcessingContext pc) {
		this(pc);
		getProcessingContext().owningBeanFactory = owningBeanFactory;
		getProcessingContext().beanNamingStrategy = namingStrategy;
	}

	public ExternalBeanMethodProcessor(ProcessingContext pc) {
		super(ExternalBean.class, pc);
	}

	/**
	 * @param targetMethod must be non-private and annotated with
	 * {@link ExternalBean}
	 */
	public Object processMethod(Method targetMethod) {
		ExternalBean externalBean = AnnotationUtils.findAnnotation(targetMethod, ExternalBean.class);

		Assert.notNull(externalBean, "method must be annotated with @ExternalBean");
		Assert.isTrue(!Modifier.isPrivate(targetMethod.getModifiers()), "@ExternalBean methods may not be private");

		String beanName;

		if (!"".equals(externalBean.value()))
			beanName = externalBean.value();
		else
			beanName = getNamingStrategy().getBeanName(targetMethod);

		return getOwningBeanFactory().getBean(beanName);
	}

	private BeanNamingStrategy getNamingStrategy() {
		return getProcessingContext().beanNamingStrategy;
	}

	private ConfigurableListableBeanFactory getOwningBeanFactory() {
		return getProcessingContext().owningBeanFactory;
	}

	/**
	 * Find all methods that are annotated with {@link ExternalBean}.
	 * 
	 * @param configurationClass
	 * @return collection of all methods annotated with {@link ExternalBean}
	 */
	public static Collection<Method> findExternalBeanCreationMethods(Class<?> configurationClass) {
		return new ExternalBeanMethodProcessor(new ProcessingContext()).findMatchingMethods(configurationClass);
	}

	/**
	 * Check whether <var>candidateMethod</var> is an {@link ExternalBean}
	 * method. Method may be annotated directly or in the case of overriding,
	 * may inherit the declaration from a superclass/superinterface.
	 * 
	 * @param candidateMethod
	 * @return true if non-private and annotated directly or indirectly with
	 * {@link ExternalBean}
	 */
	public static boolean isExternalBeanCreationMethod(Method candidateMethod) {
		return new ExternalBeanMethodProcessor(new ProcessingContext()).understands(candidateMethod);
	}

}
