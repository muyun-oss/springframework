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

package org.springframework.config.java;

import junit.framework.TestCase;

import org.springframework.beans.TestBean;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.config.java.annotation.Bean;
import org.springframework.config.java.annotation.Configuration;
import org.springframework.config.java.annotation.ExternalValue;
import org.springframework.config.java.annotation.ResourceBundles;
import org.springframework.config.java.process.ConfigurationProcessor;

/**
 * Test for properties resolution
 * 
 * @author Rod Johnson
 * 
 */
public class ExternalValueTests extends TestCase {

	@Configuration
	@ResourceBundles("classpath:/org/springframework/config/java/simple") 
	static abstract class AbstractConfigurationDependsOnProperties {
		@Bean
		public TestBean rod() {
			TestBean rod = new TestBean();
			rod.setName(getName());
			rod.setAge(age());
			return rod;
		}
		
		@ExternalValue
		public abstract String getName();
		
		@ExternalValue
		public abstract int age();
		
	}

	public void testStringProperty() throws Exception {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		ConfigurationProcessor configurationProcessor = new ConfigurationProcessor(bf);
		configurationProcessor.processClass(AbstractConfigurationDependsOnProperties.class);
		TestBean rod = (TestBean) bf.getBean("rod");
		assertEquals("String property must be resolved correctly", "Rod", rod.getName());
	}
	
	public void testIntProperty() throws Exception {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		ConfigurationProcessor configurationProcessor = new ConfigurationProcessor(bf);
		configurationProcessor.processClass(AbstractConfigurationDependsOnProperties.class);
		TestBean rod = (TestBean) bf.getBean("rod");
		assertEquals("int property must be resolved correctly", 37, rod.getAge());
	}

}
