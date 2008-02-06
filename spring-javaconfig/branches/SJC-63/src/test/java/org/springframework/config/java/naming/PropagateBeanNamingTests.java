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
package org.springframework.config.java.naming;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;

import javax.annotation.Resource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.config.java.annotation.Bean;
import org.springframework.config.java.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration(locations = { "classpath:org/springframework/config/java/naming/PropagateBeanNamingTest.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
public class PropagateBeanNamingTests {
	/**
	 * We are autowired by name and the naming strategy will name our bean
	 * "test" + [method name]. So we expect this property to be set with
	 * TestConfiguration.bean().
	 */
	private Object testbean;

	@Configuration
	public static class TestConfiguration {
		@Bean
		public Object bean() {
			return "Test";
		}
	}

	public static class TestNamingStrategy implements BeanNamingStrategy {
		public String getBeanName(Method pBeanCreationMethod) {
			return "test" + pBeanCreationMethod.getName();
		}
	}

	@Test
	public void testNamingStrategy() {
		assertEquals("Test", testbean);
	}

	public Object getTestbean() {
		return testbean;
	}

	@Resource
	public void setTestbean(Object pTestbean) {
		testbean = pTestbean;
	}
}