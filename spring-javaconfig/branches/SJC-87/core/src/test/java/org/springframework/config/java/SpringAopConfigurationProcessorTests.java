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
package org.springframework.config.java;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.TestBean;
import org.springframework.config.java.AspectJConfigurationProcessorTests.CountingConfiguration;
import org.springframework.config.java.AspectJConfigurationProcessorTests.SingletonCountingAdvice;
import org.springframework.config.java.annotation.Bean;
import org.springframework.config.java.annotation.Configuration;
import org.springframework.config.java.context.ConfigurableJavaConfigApplicationContext;
import org.springframework.config.java.context.JavaConfigApplicationContext;

/**
 * @author Rod Johnson
 * @author Chris Beams
 */
public class SpringAopConfigurationProcessorTests {

	/**
	 * Test fixture: each test method must initialize
	 */
	private ConfigurableJavaConfigApplicationContext ctx;

	/**
	 * It is up to each individual test to initialize the context;
	 * null it out before each subsequent test just to be safe
	 */
	@After
	public void nullOutContext() { ctx = null; }


	// XXX: [aop]
	public @Test void testNoAroundAdvice() throws Exception {
		// Superclass doesn't have around advice
		ctx = new JavaConfigApplicationContext(SingletonCountingAdvice.class);

		TestBean advised1 = ctx.getBean(TestBean.class, "advised");
		int newAge = 24;
		advised1.setAge(newAge);
		assertEquals("Invocations must work on target without around advice", newAge, advised1.getAge());
		assertEquals("tony", advised1.getName());
	}


	// XXX: [aop, bean scoping]
	public @Test void testPerInstanceAdviceAndSharedAdvice() throws Exception {
		ctx = new JavaConfigApplicationContext(SpringAroundPerInstanceAdvice.class);

		TestBean advised1 = ctx.getBean(TestBean.class, "advised");
		Object target1 = ((Advised) advised1).getTargetSource().getTarget();
		TestBean advised2 = ctx.getBean(TestBean.class, "advised");

		// Hashcode works on this
		advised2.setAge(35);

		assertNotSame(advised1, advised2);
		Object target2 = ((Advised) advised2).getTargetSource().getTarget();
		assertNotSame(target1, target2);

		assertEquals("advised", ctx.getBeanNamesForType(TestBean.class)[0]);

		assertEquals(0, CountingConfiguration.getCount(target1));
		advised1.absquatulate();
		assertEquals(0, CountingConfiguration.getCount(target1));
		advised1.getSpouse();
		assertEquals(1, CountingConfiguration.getCount(target1));
		assertEquals(0, CountingConfiguration.getCount(target2));

		advised2.getSpouse();
		assertEquals(1, CountingConfiguration.getCount(target1));
		assertEquals(1, CountingConfiguration.getCount(target2));
	}
	// XXX: [aop]
	public @Test void testAroundAdvice() throws Exception {
		ctx = new JavaConfigApplicationContext(SpringAroundPerInstanceAdvice.class);

		TestBean advised1 = ctx.getBean(TestBean.class, "advised");
		int newAge = 24;
		advised1.setAge(newAge);
		assertEquals("Invocations must work on target without around advice", newAge, advised1.getAge());
		assertEquals("around", advised1.getName());
	}
	@Aspect @Configuration
	public static class SpringAroundPerInstanceAdvice extends CountingConfiguration {
		@Before("execution(* getSpouse()) && target(target)")
		public void doesntMatter(Object target) {
			Integer count = counts.get(target);
			if (count == null) {
				count = 0;
			}
			++count;
			counts.put(target, count);
		}
		@Around("execution(* *.getName())")
		public Object around() { return "around"; }
	}


	// XXX: [aop]
	@Ignore // interception on self causes circular dependency - can this be prevented?
	public @Test void testInterceptAll() throws Exception {
		ctx = new JavaConfigApplicationContext(InterceptAllAdvice.class);

		TestBean kaare = ctx.getBean(TestBean.class, "kaare");

		// Can't start at 0 because of factory methods such as setBeanFactory()
		int invocations = InterceptAllAdvice.count = 0;

		kaare.absquatulate();
		assertEquals(++invocations, InterceptAllAdvice.count);
		kaare.getAge();
		assertEquals(++invocations, InterceptAllAdvice.count);
	}
	public static class InterceptAllAdvice {
		public static int count;

		@Before("* *(..)")
		protected void count() { ++count; }

		public @Bean TestBean kaare() { return new TestBean(); }
	}

}
