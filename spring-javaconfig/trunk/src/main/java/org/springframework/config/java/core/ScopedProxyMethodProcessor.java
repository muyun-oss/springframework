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

import org.springframework.config.java.annotation.aop.ScopedProxy;
import org.springframework.config.java.util.ScopeUtils;
import org.springframework.config.java.valuesource.ValueResolutionException;
import org.springframework.util.Assert;

public class ScopedProxyMethodProcessor extends AbstractBeanMethodProcessor {

	private final StandardBeanMethodProcessor delegate;

	public ScopedProxyMethodProcessor(StandardBeanMethodProcessor delegate) {
		super(ScopedProxy.class);
		Assert.notNull(delegate, "BeanMethodProcessor argument is required");
		this.delegate = delegate;
	}

	private ScopedProxyMethodProcessor() {
		super(ScopedProxy.class);
		this.delegate = null;
	}

	public String processMethod(Method m) throws ValueResolutionException {
		String beanToReturn = delegate.getBeanName(m);
		String scopedBean = ScopeUtils.getScopedHiddenName(beanToReturn);

		if (delegate.isCurrentlyInCreation(scopedBean))
			beanToReturn = scopedBean;

		return beanToReturn;
	}

	public static boolean isScopedProxyMethod(Method candidateMethod) {
		return new ScopedProxyMethodProcessor().understands(candidateMethod);
	}
}
