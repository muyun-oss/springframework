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

package org.springframework.config.java.listener;

import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.config.java.annotation.Bean;
import org.springframework.config.java.process.ConfigurationProcessor;

/**
 * Convenient base class for implementation of the ConfigurationListener
 * interface, offer no op implementations of all methods.
 * 
 * @author Rod Johnson
 * 
 */
public class ConfigurationListenerSupport implements ConfigurationListener {

	protected final Log log = LogFactory.getLog(getClass());

	public boolean understands(Class<?> configurerClass) {
		return false;
	}

	public int configurationClass(ConfigurationProcessor configurationProcessor, 
			String configurerBeanName, Class<?> configurerClass) {
		return 0;
	}

	public int beanCreationMethod(BeanDefinitionRegistration beanDefinitionRegistration,
			ConfigurationProcessor configurationProcessor,
			String configurerBeanName, Class<?> configurerClass, Method m, Bean beanAnnotation) {
		return 0;
	}

	public int otherMethod(ConfigurationProcessor configurationProcessor,
			String configurerBeanName, Class<?> configurerClass, Method m) {
		return 0;
	}

	public boolean processBeanMethodReturnValue(ConfigurationProcessor configurationProcessor, Object originallyCreatedBean, Method method, ProxyFactory pf) {
		return false;
	}
}
