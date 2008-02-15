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

import org.springframework.config.java.annotation.aop.ScopedProxy;
import org.springframework.config.java.valuesource.ValueResolutionException;
import org.springframework.util.Assert;

class ScopedProxyMethodProcessor extends AbstractBeanMethodProcessor {

	private static final String TARGET_NAME_PREFIX = "scopedTarget.";

	private final StandardBeanMethodProcessor delegate;

	public ScopedProxyMethodProcessor(StandardBeanMethodProcessor delegate, ProcessingContext pc) {
		super(ScopedProxy.class, pc);
		Assert.notNull(delegate, "BeanMethodProcessor argument is required");
		this.delegate = delegate;
	}

	private ScopedProxyMethodProcessor(ProcessingContext pc) {
		super(ScopedProxy.class, pc);
		this.delegate = null;
	}

	public String processMethod(Method m) throws ValueResolutionException {
		String beanToReturn = delegate.getBeanName(m);
		String scopedBean = resolveHiddenScopedProxyBeanName(beanToReturn);

		if (delegate.isCurrentlyInCreation(scopedBean))
			beanToReturn = scopedBean;

		return beanToReturn;
	}

	public static boolean isScopedProxyMethod(Method candidateMethod) {
		return new ScopedProxyMethodProcessor(new ProcessingContext()).understands(candidateMethod);
	}

	/**
	 * Check whether <var>candidateMethod</var> is an {@link ScopedProxy}
	 * method. Method may be annotated directly or in the case of overriding,
	 * may inherit the declaration from a superclass/superinterface.
	 * 
	 * @param candidateMethod
	 * @return true if non-private and annotated directly or indirectly with
	 * {@link ScopedProxy}
	 */
	public static boolean isScopedProxyCreationMethod(Method candidateMethod) {
		return new ScopedProxyMethodProcessor(new ProcessingContext()).understands(candidateMethod);
	}

	/**
	 * Find all methods that are annotated with {@link ScopedProxy}.
	 * 
	 * @param configurationClass
	 * @return collection of all methods annotated with {@link ScopedProxy}
	 */
	public static Collection<Method> findScopedProxyCreationMethods(Class<?> configurationClass) {
		return new ScopedProxyMethodProcessor(new ProcessingContext()).findMatchingMethods(configurationClass);
	}

	/**
	 * Return the <i>hidden</i> name based on a scoped proxy bean name.
	 * 
	 * @param originalBeanName the scope proxy bean name as declared in the
	 * Configuration-annotated class
	 * 
	 * @return the internally-used <i>hidden</i> bean name
	 */
	public static String resolveHiddenScopedProxyBeanName(String originalBeanName) {
		Assert.hasText(originalBeanName);
		return TARGET_NAME_PREFIX.concat(originalBeanName);
	}

}