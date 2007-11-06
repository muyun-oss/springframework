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

package org.springframework.config.java.support;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.interceptor.ExposeInvocationInterceptor;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.config.java.annotation.Bean;
import org.springframework.config.java.listener.ConfigurationListener;
import org.springframework.config.java.process.ConfigurationProcessor;
import org.springframework.config.java.util.ClassUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;

/**
 * Wrapper for the result returned by the method backing a bean instance. Uses
 * the getBean() calls tracker and if needed, proxies the returned value if
 * there are advisors that might apply to it.
 * 
 * @author Rod Johnson
 * @author Costin Leau
 * 
 */
public class MethodBeanWrapper {

	private final ConfigurationProcessor configurationProcessor;

	private final BeanNameTrackingDefaultListableBeanFactory childTrackingFactory;

	/**
	 * Constructor.
	 * 
	 * @param owningBeanFactory
	 * @param configurationListenerRegistry
	 * @param childTrackingFactory
	 */
	public MethodBeanWrapper(ConfigurationProcessor configurationProcessor,
			BeanNameTrackingDefaultListableBeanFactory childTrackingFactory) {
		this.configurationProcessor = configurationProcessor;
		this.childTrackingFactory = childTrackingFactory;
	}

	/**
	 * Wrap the result of a bean definition method in a Spring AOP proxy if
	 * there are advisors in the current factory that would apply to it. Note
	 * that the advisors may have been added explicitly by the user or may have
	 * resulted from Advisor generation on this class processing a Pointcut
	 * annotation.
	 * 
	 * @param beanName the bean to which the method result will be binded
	 * @param invoker hook for calling the java code
	 * @return the wrapped object
	 * @throws Throwable if the method invocation fails
	 */
	public Object wrapResult(String beanName, EnhancerMethodInvoker invoker) throws Throwable {
		Assert.hasText(beanName, "a non-empty beanName is required");
		Assert.notNull(invoker, "a not-null invoker is required");

		// If we are in our first call to getBean() with this name and were
		// not
		// called by the factory (which would have tracked the call), call
		// the factory
		// to get the bean. We need to do this to ensure that lifecycle
		// callbacks are invoked,
		// so that calls made within a factory method in otherBean() style
		// still
		// get fully configured objects.

		String lastRequestedBeanName = childTrackingFactory.lastRequestedBeanName();

		boolean newBeanRequested = (lastRequestedBeanName == null || beanName.equals(lastRequestedBeanName));

		Method method = invoker.getMethod();

		try {
			// first call - start tracking
			if (newBeanRequested) {
				// Remember the getBean() method we're now executing,
				// if we were invoked from within the factory method in the
				// configuration class than through the BeanFactory
				childTrackingFactory.recordRequestForBeanName(beanName);
			}

			// get a bean instance in case the @Bean was overriden
			Object originallyCreatedBean = null;

			BeanDefinition beanDef = null;
			boolean hidden = false;
			// consider hidden beans also (contained only in the children
			// factory)
			if (childTrackingFactory.containsBeanDefinition(beanName)) {
				beanDef = childTrackingFactory.getBeanDefinition(beanName);
				hidden = true;
			}
			else {
				beanDef = configurationProcessor.getOwningBeanFactory().getBeanDefinition(beanName);
				hidden = false;
			}

			// the definition was overriden (use that one)
			if (beanDef.getAttribute(ClassUtils.JAVA_CONFIG_PKG) == null) {
				originallyCreatedBean = configurationProcessor.getOwningBeanFactory().getBean(beanName);
			}

			if (originallyCreatedBean == null) {
				// nothing was found so call the original code
				originallyCreatedBean = invoker.invokeOriginalClass();
			}

			if (!configurationProcessor.getConfigurationListenerRegistry().getConfigurationListeners().isEmpty()) {
				// We know we have advisors that may affect this object
				// Prepare to proxy it
				ProxyFactory pf = new ProxyFactory(originallyCreatedBean);

				if (shouldProxyBeanCreationMethod(method)) {
					pf.setProxyTargetClass(true);
				}
				else {
					pf.setInterfaces(new Class[] { method.getReturnType() });
					pf.setProxyTargetClass(false);
				}

				boolean customized = false;
				for (ConfigurationListener cml : configurationProcessor.getConfigurationListenerRegistry()
						.getConfigurationListeners()) {
					customized = customized
							|| cml.processBeanMethodReturnValue(configurationProcessor, originallyCreatedBean, method,
									pf);
				}

				// Only proxy if we know that advisors apply to this bean
				if (customized || pf.getAdvisors().length > 0) {
					// Ensure that AspectJ pointcuts will work
					pf.addAdvice(0, ExposeInvocationInterceptor.INSTANCE);
					if (pf.isProxyTargetClass() && Modifier.isFinal(method.getReturnType().getModifiers())) {
						throw new BeanDefinitionStoreException(method + " is eligible for proxying target class "
								+ "but return type " + method.getReturnType().getName() + " is final");
					}
					return pf.getProxy();
				}
				else {
					return originallyCreatedBean;
				}
			}
			else {
				// There can be no advisors
				return originallyCreatedBean;
			}
		}
		finally {
			// be sure to clean tracking
			if (newBeanRequested) {
				childTrackingFactory.pop();
			}
		}
	}

	/**
	 * Use class proxies only if the return type isn't an interface or if
	 * autowire is required (as an interface based proxy excludes the setters).
	 * 
	 * @param m
	 * @param c
	 * @return
	 */
	private boolean shouldProxyBeanCreationMethod(Method m) {
		Bean bean = AnnotationUtils.findAnnotation(m, Bean.class);

		// TODO need to consider autowiring enabled at factory level - reuse the
		// detection from ConfigurationProcessor
		return !m.getReturnType().isInterface() || bean.autowire().isAutowire();
	}

}
