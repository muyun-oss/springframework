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
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.config.java.annotation.Bean;
import org.springframework.config.java.annotation.Configuration;
import org.springframework.config.java.annotation.DependencyCheck;
import org.springframework.config.java.annotation.Lazy;
import org.springframework.config.java.annotation.Meta;
import org.springframework.config.java.annotation.Primary;
import org.springframework.util.StringUtils;

/**
 * Processing utility class.
 * 
 * @author Costin Leau
 * @author Chris Beams
 */
final class ProcessUtils {
	private ProcessUtils() {
	}

	/**
	 * Validation for the bean creation method. Checks that the method is not
	 * final (so it can be proxied) and that a type is being returned (the
	 * return instance becoming the actual bean).
	 * 
	 * @param beanCreationMethod
	 * @throws BeanDefinitionStoreException
	 */
	static void validateBeanCreationMethod(Method beanCreationMethod) throws BeanDefinitionStoreException {
		if (Modifier.isFinal(beanCreationMethod.getModifiers())) {
			throw new BeanDefinitionStoreException("Bean creation method " + methodIdentifier(beanCreationMethod)
					+ " may not be final");
		}
		if (Modifier.isPrivate(beanCreationMethod.getModifiers())) {
			throw new BeanDefinitionStoreException("Bean creation method " + methodIdentifier(beanCreationMethod)
					+ " may not be private");
		}
		if (beanCreationMethod.getReturnType() == Void.TYPE) {
			throw new BeanDefinitionStoreException("Bean creation method " + methodIdentifier(beanCreationMethod)
					+ " may not have void return");
		}
	}

	private static String methodIdentifier(Method m) {
		return m.getDeclaringClass().getName() + "." + m.getName();
	}

	/**
	 * Create the bean definition based on the annotation properties.
	 * 
	 * @param beanName name of the bean we're creating (not the factory bean)
	 * @param beanAnnotation bean annotation
	 * @param configuration configuration on the configuration class. Sets
	 * defaults. May be null as this annotation is not required.
	 * @param rbd bean definition, in Spring IoC container internal metadata
	 * @param beanFactory bean factory we are executing in
	 */
	static void copyAttributes(String beanName, Bean beanAnnotation, Configuration configuration,
			RootBeanDefinition rbd, ConfigurableListableBeanFactory beanFactory) {

		// singleton/scope
		rbd.setScope(beanAnnotation.scope());

		// depends-on
		rbd.setDependsOn(beanAnnotation.dependsOn());

		// aliases
		for (String alias : beanAnnotation.aliases()) {
			beanFactory.registerAlias(beanName, alias);
		}

		// metadata
		for (Meta meta : beanAnnotation.meta()) {
			rbd.setAttribute(meta.name(), meta.value());
		}

		// lifecycle methods
		if (StringUtils.hasText(beanAnnotation.initMethodName())) {
			rbd.setInitMethodName(beanAnnotation.initMethodName());
		}

		if (StringUtils.hasText(beanAnnotation.destroyMethodName())) {
			rbd.setDestroyMethodName(beanAnnotation.destroyMethodName());
		}

		// primary
		if (beanAnnotation.primary() != Primary.UNSPECIFIED) {
			rbd.setPrimary(beanAnnotation.primary().booleanValue());
		}

		// configuration, fallback methods

		if (beanAnnotation.dependencyCheck() != DependencyCheck.UNSPECIFIED) {
			rbd.setDependencyCheck(beanAnnotation.dependencyCheck().value());
		}
		else if (configuration != null && configuration.defaultDependencyCheck() != DependencyCheck.UNSPECIFIED) {
			rbd.setDependencyCheck(configuration.defaultDependencyCheck().value());
		}

		if (beanAnnotation.lazy() != Lazy.UNSPECIFIED) {
			rbd.setLazyInit(beanAnnotation.lazy().booleanValue());
		}
		else if (configuration != null && configuration.defaultLazy() != Lazy.UNSPECIFIED) {
			rbd.setLazyInit(configuration.defaultLazy().booleanValue());
		}

		if (beanAnnotation.autowire() != Autowire.INHERITED) {
			rbd.setAutowireMode(beanAnnotation.autowire().value());
		}
		else if (configuration != null && configuration.defaultAutowire() != Autowire.INHERITED) {
			rbd.setAutowireMode(configuration.defaultAutowire().value());
		}

	}

	/**
	 * Return the class useful for processing for the given bean. Will return
	 * null if no bean is not valid or if no class can be found. Will throw an
	 * exception if the found class is invalid.
	 * 
	 * No check for
	 * @Bean or
	 * @Configuration annotations is made.
	 * 
	 * @param beanName
	 * @param clbf
	 * @return
	 */
	static Class<?> getBeanClass(String beanName, ConfigurableListableBeanFactory clbf) {

		BeanDefinition bd = clbf.getBeanDefinition(beanName);

		if (!(isEligibleForConfigurationProcessing(bd) && bd instanceof AbstractBeanDefinition))
			return null;

		// required for updating the bean class
		AbstractBeanDefinition definition = (AbstractBeanDefinition) bd;

		// TODO: check for FactoryBean/factory-method type of beans
		// hard since we are a BFPP and it's impossible to get the actual
		// configuration instance/class w/o initilizing the factory-method/FB
		// even for non @Configuration cases.

		if (definition.hasBeanClass())
			return definition.getBeanClass();

		// load the class (changes in the lazy loading code part of
		// spring core)

		// TODO: add support for factory-method beans (and other not-normal
		// beans) this requires transforming the BFPP into a BPP and might
		// require multiple instantion of the configuration class.
		if (bd.getBeanClassName() != null) {
			try {
				return org.springframework.util.ClassUtils.forName(bd.getBeanClassName());
			}
			catch (ClassNotFoundException e) {
				throw new IllegalArgumentException("Bean class '" + bd.getBeanClassName() + "' not found");
			}
		}

		return null;
	}

	/**
	 * Determines if the given bean definition is eligible for configuration
	 * processsing by a ConfigurationProcessor. Abstract beans as well as bean
	 * definitions w/o classes are excluded.
	 * 
	 * @param def
	 * @return
	 */
	private static boolean isEligibleForConfigurationProcessing(BeanDefinition def) {
		if (def.isAbstract() || (!StringUtils.hasText(def.getBeanClassName()))) {
			return false;
		}

		return true;
	}
}