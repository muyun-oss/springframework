/*
 * Copyright 2002-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.config.java;

import static org.junit.Assert.*;

import java.awt.Point;
import java.lang.reflect.Method;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.junit.After;
import org.junit.Test;
import org.springframework.aop.MethodBeforeAdvice;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.target.HotSwappableTargetSource;
import org.springframework.beans.BeansException;
import org.springframework.beans.DependsOnTestBean;
import org.springframework.beans.ITestBean;
import org.springframework.beans.TestBean;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.config.java.AspectJConfigurationProcessorTests.AroundAdviceWithNamedPointcut;
import org.springframework.config.java.ConfigurationProcessorTests.HiddenBeans.BFAwareBean;
import org.springframework.config.java.annotation.AutoBean;
import org.springframework.config.java.annotation.Bean;
import org.springframework.config.java.annotation.Configuration;
import org.springframework.config.java.annotation.Lazy;
import org.springframework.config.java.annotation.aop.targetsource.HotSwappable;
import org.springframework.config.java.context.ConfigurableJavaConfigApplicationContext;
import org.springframework.config.java.context.JavaConfigApplicationContext;
import org.springframework.config.java.context.LegacyJavaConfigApplicationContext;
import org.springframework.config.java.model.ValidationError;
import org.springframework.config.java.process.ConfigurationProcessor;
import org.springframework.config.java.process.MalformedJavaConfigurationException;
import org.springframework.config.java.support.ConfigurationSupport;
import org.springframework.config.java.util.DefaultScopes;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.GenericApplicationContext;

/**
 * Tests for {@link ConfigurationProcessor}
 *
 * @author Rod Johnson
 * @author Chris Beams
 */
public class ConfigurationProcessorTests {

	private ConfigurableJavaConfigApplicationContext ctx;

	/** Context should be initialized by each test */
	@After
	public void nullOutContext() {
		ctx = null;
	}

	public @Test void testSingletonNature() {
		ctx = new JavaConfigApplicationContext(BaseConfiguration.class);
		assertTrue(ctx.containsBean(BaseConfiguration.class.getName()));

		ITestBean tb = (ITestBean) ctx.getBean("tom");
		assertEquals("tom", tb.getName());
		assertEquals("becky", tb.getSpouse().getName());
		ITestBean tomsBecky = tb.getSpouse();
		ITestBean factorysBecky = (ITestBean) ctx.getBean("becky");

		// given that becky is a singleton-scoped bean, these objects should be same instance
		assertSame(tomsBecky, factorysBecky);
	}

	/**
	 * Beans that implement {@link BeanNameAware} should be injected
	 * with their bean names appropriately
	 */
	public @Test void testBeanNameAware() {
		ctx = new JavaConfigApplicationContext(BaseConfiguration.class);
		assertTrue(ctx.containsBean(BaseConfiguration.class.getName()));

		TestBean tom = (TestBean) ctx.getBean("tom");
		assertEquals("tom", tom.getName());
		assertEquals("tom", tom.getBeanName());
	}


	public @Test void testMethodOverrideWithJava() {
		ctx = new JavaConfigApplicationContext(MethodOverrideConfiguration.class);

		assertTrue(ctx.containsBean(MethodOverrideConfiguration.class.getName()));

		TestBean tom = ctx.getBean(TestBean.class, "tom");
		assertEquals("overridden", tom.getName());
	}
	public static class MethodOverrideConfiguration extends BaseConfiguration {
		@Override
		public TestBean tom() {
			return new TestBean() {
				@Override
				public String getName() {
					return "overridden";
				}
			};
		}
	}


	public @Test void testAfterPropertiesSetInvokedBeforeExplicitWiring() {
		ctx = new JavaConfigApplicationContext(AfterPropertiesConfiguration.class);

		// This is enough to run the test - see assertions in test() bean method below
		ctx.getBean(TestBean.class, "test");
	}
	public static class AfterPropertiesConfiguration {
		public @Bean TestBean test() {
			assertEquals("AfterPropertiesSet must have been called by now", 5, apt().sum());
			return new TestBean();
		}

		public @Bean AfterPropertiesTest apt() {
			AfterPropertiesTest apt = new AfterPropertiesTest();
			apt.setA(2);
			apt.setB(3);
			return apt;
		}
	}


	public @Test void testBeanFactoryAware() {
		ctx = new JavaConfigApplicationContext(BaseConfiguration.class);

		assertTrue(ctx.containsBean(BaseConfiguration.class.getName()));

		TestBean becky = (TestBean) ctx.getBean("becky");
		assertEquals("becky", becky.getName());
		assertSame(ctx.getBeanFactory(), becky.getBeanFactory());
	}


	// TODO: handle hidden beans - JavaConfigApplicationContext is not yet capable of this
	public @Test void testHidden() {
		ctx = new LegacyJavaConfigApplicationContext(BaseConfiguration.class);

		assertTrue(ctx.containsBean(BaseConfiguration.class.getName()));

		ITestBean dependsOnHidden = (ITestBean) ctx.getBean("dependsOnHidden");
		ITestBean hidden = dependsOnHidden.getSpouse();
		assertFalse("hidden bean 'hidden' should not be available via ctx", ctx.containsBean("hidden"));
		assertEquals("hidden", hidden.getName());
		assertEquals("becky", hidden.getSpouse().getName());
		ITestBean hiddenBecky = hidden.getSpouse();
		ITestBean factorysBecky = (ITestBean) ctx.getBean("becky");
		assertSame(hiddenBecky, factorysBecky);
	}


	// TODO: handle aspect configurations - JavaConfigApplicationContext is not yet capable of this
	public @Test void testAutowireOnBeanDefinition() {
		ctx = new LegacyJavaConfigApplicationContext(AroundAdviceWithNamedPointcut.class);

		ctx.getBean("dotb");
		DependsOnTestBean dotb1 = (DependsOnTestBean) ctx.getBean("dotb");
		DependsOnTestBean dotb2 = (DependsOnTestBean) ctx.getBean("dotb");
		assertSame(dotb1, dotb2);
		assertSame(dotb1.getTestBean(), dotb2.getTestBean());
		assertNotNull("Autowire works", dotb1.getTestBean());
	}


	// TODO: complete autowiring support for JavaConfigApplicationContext
	public @Test void testAutowireOnProxiedBeanDefinition() {
		ctx = new LegacyJavaConfigApplicationContext(ProxiesDotb.class);

		ProxiesDotb.count = 0;
		DependsOnTestBean sarah = (DependsOnTestBean) ctx.getBean("sarah");
		assertTrue("bean 'sarah' should have been an AOP proxy", AopUtils.isAopProxy(sarah));
		assertTrue("bean 'sarah' should have been an CGLIB proxy", AopUtils.isCglibProxy(sarah));
		assertNotNull("autowiring did not complete successfully", sarah.getTestBean());

		assertEquals(1, ProxiesDotb.count);
	}


	@Test(expected=MalformedJavaConfigurationException.class)
	public void testInvalidFinalConfigurationClass() {
		try {
			// should throw, rejecting final configuration class;
			new JavaConfigApplicationContext(InvalidFinalConfigurationClass.class);
		} catch (MalformedJavaConfigurationException ex) {
			assertTrue(ex.getMessage().contains(ValidationError.CONFIGURATION_MUST_BE_NON_FINAL.toString()));
			throw ex;
		}
	}
	@Configuration
	public final static class InvalidFinalConfigurationClass {
		@Bean
		public DummyFactory factoryBean() {
			return new DummyFactory();
		}
	}


	@Test(expected = BeanDefinitionStoreException.class)
	public void testInvalidDueToFinalBeanMethod() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		ConfigurationProcessor configurationProcessor = new ConfigurationProcessor(bf);
		configurationProcessor.processClass(InvalidDueToFinalBeanMethod.class);
		// should throw, rejecting final Bean method
	}

	// TODO JavaConfigApplicationContext does not yet support this
	// TODO would expect BeanDefinitionStoreException?
	@Test(expected = BeanCreationException.class)
	public void testInvalidDueToFinalBeanClass() {
		ctx = new LegacyJavaConfigApplicationContext(InvalidDueToFinalBeanClass.class);
		// Arguably should spot this earlier
		ctx.getBean("test");
		// should have thrown, rejecting final Bean method
	}
	@Configuration @Aspect
	public static class InvalidDueToFinalBeanClass {
		@Before("execution(* get*())")
		public void empty() { }

		public @Bean FinalTestBean test() { return new FinalTestBean(); }
	}


	@Test(expected=MalformedJavaConfigurationException.class)
	public void testInvalidDueToPrivateBeanMethod() {
		// should throw, rejecting private Bean method
		new JavaConfigApplicationContext(InvalidDuePrivateBeanMethod.class);
	}
	static class InvalidDuePrivateBeanMethod {
		public @Bean Object ok() { return new Object(); }
		@SuppressWarnings("unused")
		private @Bean Object notOk() { return new Object(); }
	}


	// TODO: [aop] JavaConfigApplicationContext does not yet support aspects
	public @Test void testValidWithDynamicProxy() {
		ctx = new LegacyJavaConfigApplicationContext(ValidWithDynamicProxies.class);
		ITestBean tb = (ITestBean) ctx.getBean("test");
		assertTrue(AopUtils.isJdkDynamicProxy(tb));
	}
	@Configuration @Aspect
	public static class ValidWithDynamicProxies {
		@Before("execution(* get*())")
		public void empty() { }

		public @Bean ITestBean test() { return new FinalTestBean(); }
	}


	public @Test void testApplicationContextAwareCallbackWithGenericApplicationContext() {
		ctx = new JavaConfigApplicationContext(ApplicationContextAwareConfiguration.class);
		ApplicationContextAwareImpl acai = (ApplicationContextAwareImpl) ctx.getBean("ai");
		assertNotNull("ApplicationContextAware callback must be honoured", acai.applicationContext);
	}
	public static class ApplicationContextAwareConfiguration {
		public @Bean ApplicationContextAwareImpl ai() { return new ApplicationContextAwareImpl(); }
	}
	public static class ApplicationContextAwareImpl implements ApplicationContextAware {
		public ApplicationContext applicationContext;
		public void setApplicationContext(ApplicationContext ac) { this.applicationContext = ac; }
	}

	// TODO test override in XML: possible
	// TODO conflict in Java config: illegal
	// TODO multiple advice on the one method
	// TODO deep getBeans
	// TODO circular get beans
	// TODO default lazy and other lazy

	public @Test void testDefaultAutowire() {
		ctx = new JavaConfigApplicationContext(DefaultAutowireConfiguration.class);

		DependsOnTestBean sarah = (DependsOnTestBean) ctx.getBean("sarah");
		assertNotNull("autowiring did not occur: sarah should have TestBean", sarah.getTestBean());
		assertEquals("autowiring error", "adrian", sarah.getTestBean().getName());
	}
	@Configuration(defaultAutowire = Autowire.BY_TYPE)
	public static class DefaultAutowireConfiguration {
		public @Bean TestBean adrian() { return new TestBean("adrian", 34); }
		public @Bean DependsOnTestBean sarah() { return new DependsOnTestBean(); }
	}

	public @Test void next() { fail("resume here"); }


	@Test
	public void testFactoryBean() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		ConfigurationProcessor configurationProcessor = new ConfigurationProcessor(bf);
		configurationProcessor.processClass(ContainsFactoryBean.class);

		assertTrue("Factory bean must return created type", bf.getBean("factoryBean") instanceof TestBean);
	}

	@Test
	public void testNewAnnotationNotRequiredOnConcreteMethod() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		ConfigurationProcessor configurationProcessor = new ConfigurationProcessor(bf);
		configurationProcessor.processClass(InheritsWithoutNewAnnotation.class);

		TestBean tom = (TestBean) bf.getBean("tom");
		TestBean becky = (TestBean) bf.getBean("becky");
		assertSame(tom, becky.getSpouse());
		assertSame(becky, bf.getBean("becky"));
	}

	@Test
	public void testProgrammaticProxyCreation() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		ConfigurationProcessor configurationProcessor = new ConfigurationProcessor(bf);
		configurationProcessor.processClass(ProxyConfiguration.class);

		ITestBean proxy = (ITestBean) bf.getBean("proxied");
		assertSame(proxy, bf.getBean("proxied"));
		ProxyConfiguration.count = 0;
		String name = "Shane Warne";
		proxy.setName(name);
		assertEquals(1, ProxyConfiguration.count);
		assertEquals(name, proxy.getName());
		assertEquals(2, ProxyConfiguration.count);
	}

	@Test
	public void testBeanAliases() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		ConfigurationProcessor configurationProcessor = new ConfigurationProcessor(bf);
		configurationProcessor.processClass(AliasesConfiguration.class);

		ITestBean alias = (ITestBean) bf.getBean("aliased");
		assertEquals("Legion", alias.getName());
		assertSame(alias, bf.getBean("aliased"));
		assertSame(alias, bf.getBean("tom"));
		assertSame(alias, bf.getBean("dick"));
		assertSame(alias, bf.getBean("harry"));
		assertFalse(bf.containsBean("Glen"));
	}

	@Test
	public void testHotSwappable() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		ConfigurationProcessor configurationProcessor = new ConfigurationProcessor(bf);
		configurationProcessor.processClass(HotSwapConfiguration.class);

		TestBean hs = (TestBean) bf.getBean("hotSwappable");
		assertTrue(AopUtils.isCglibProxy(hs));
		assertEquals("hotSwappable", hs.getName());
		Advised adv = (Advised) hs;
		assertTrue(adv.getTargetSource() instanceof HotSwappableTargetSource);

		ITestBean ihs = (ITestBean) bf.getBean("hotSwappableInterface");
		assertFalse(ihs instanceof TestBean);
		assertTrue(AopUtils.isAopProxy(ihs));
		assertTrue("Should not proxy target class if return type is an interface", AopUtils.isJdkDynamicProxy(ihs));
		assertEquals("hotSwappableInterface", ihs.getName());
		adv = (Advised) ihs;
		assertTrue(adv.getTargetSource() instanceof HotSwappableTargetSource);
	}

	@Test
	public void testBeanFactoryAwareConfiguration() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		TestBean spouse = new TestBean();
		bf.registerSingleton("spouse", spouse);
		ConfigurationProcessor configurationProcessor = new ConfigurationProcessor(bf);
		configurationProcessor.processClass(BeanFactoryAwareConfiguration.class);

		ITestBean marriedToInjection = (ITestBean) bf.getBean("marriedToInjection");
		assertSame(spouse, marriedToInjection.getSpouse());
	}

	public static class BaseConfiguration {

		// @Bean
		// private TestBean field;

		@Bean(scope = DefaultScopes.SINGLETON, lazy = Lazy.FALSE)
		public TestBean tom() {
			TestBean tom = basePerson();
			tom.setName("tom");
			tom.setSpouse(becky());
			return tom;
		}

		@Bean(scope = DefaultScopes.PROTOTYPE)
		public Point prototypePoint() {
			return new Point(3, 4);
		}

		@Bean(scope = DefaultScopes.PROTOTYPE, lazy = Lazy.FALSE)
		public TestBean prototype() {
			TestBean tom = basePerson();
			tom.setName("prototype");
			tom.setSpouse(becky());
			return tom;
		}

		// Parent template mechanism
		protected TestBean basePerson() {
			return new TestBean();
		}

		@Bean
		public TestBean becky() {
			TestBean becky = new TestBean();
			becky.setName("becky");
			return becky;
		}

		@Bean()
		protected TestBean hidden() {
			TestBean hidden = new TestBean();
			hidden.setName("hidden");
			hidden.setSpouse(becky());
			return hidden;
		}

		@Bean
		public TestBean dependsOnHidden() {
			TestBean t = new TestBean();
			t.setSpouse(hidden());
			return t;
		}
	}


	public static class ProxyConfiguration {

		public static int count;

		@Bean
		public ITestBean proxied() {
			TestBean tb = new TestBean();
			ProxyFactory pf = new ProxyFactory(tb);
			pf.addAdvice(new MethodBeforeAdvice() {
				public void before(Method method, Object[] args, Object target) throws Throwable {
					++count;
				}
			});
			return (ITestBean) pf.getProxy();
		}
	}

	public static class BeanFactoryAwareConfiguration extends ConfigurationSupport {

		@Bean
		public TestBean marriedToInjection() {
			TestBean tb = new TestBean();
			tb.setSpouse((TestBean) getBeanFactory().getBean("spouse"));
			return tb;
		}

	}

	public static class AliasesConfiguration extends ConfigurationSupport {

		@Bean(aliases = { "tom", "dick", "harry" })
		public TestBean aliased() {
			TestBean tb = new TestBean();
			tb.setName("Legion");
			return tb;
		}
	}

	public static class HotSwapConfiguration extends ConfigurationSupport {

		@Bean
		@HotSwappable
		public TestBean hotSwappable() {
			TestBean tb = new TestBean();
			tb.setName("hotSwappable");
			return tb;
		}

		@Bean
		@HotSwappable
		public ITestBean hotSwappableInterface() {
			TestBean tb = new TestBean();
			tb.setName("hotSwappableInterface");
			return tb;
		}
	}

	public static abstract class DefinesAbstractBeanMethod {

		@Bean
		public TestBean becky() {
			TestBean becky = new TestBean();
			becky.setSpouse(tom());
			return becky;
		}

		@Bean
		public abstract TestBean tom();
	}

	public static class InheritsWithoutNewAnnotation extends DefinesAbstractBeanMethod {

		@Override
		public TestBean tom() {
			return new TestBean();
		}
	}

	@Configuration
	@Aspect
	public static class ProxiesDotb {

		public static int count = 0;

		@Bean
		public TestBean adrian() {
			return new TestBean("adrian", 34);
		}

		@Bean(autowire = Autowire.BY_TYPE)
		public DependsOnTestBean sarah() {
			DependsOnTestBean sarah = new DependsOnTestBean();
			// sarah.setTestBean(adrian());
			return sarah;
		}

		@Before("execution(* getTestBean())")
		public void println() {
			++count;
		}

	}

	public static class AfterPropertiesTest implements InitializingBean {
		private int a, b, sum;

		public void setA(int a) {
			this.a = a;
		}

		public void setB(int b) {
			this.b = b;
		}

		public void afterPropertiesSet() throws Exception {
			sum = a + b;
		}

		public int sum() {
			return sum;
		}
	}

	@Configuration
	public static class ContainsFactoryBean {
		@Bean
		public DummyFactory factoryBean() {
			return new DummyFactory();
		}
	}


	@Configuration
	public static class InvalidDueToFinalBeanMethod {
		@Bean
		public final DummyFactory factoryBean() {
			return new DummyFactory();
		}
	}

	private final static class FinalTestBean extends TestBean {
	}



	@Test
	public void testEffectOfHidingOnAutowire() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		ConfigurationProcessor configurationProcessor = new ConfigurationProcessor(bf);
		configurationProcessor.processClass(AutowiringConfiguration.class);

		assertFalse(bf.containsBean("testBean"));
		DependsOnTestBean dotb = (DependsOnTestBean) bf.getBean("autowireCandidate");
		assertNull("Should NOT have autowired with hidden bean", dotb.tb);
	}

	@Test
	public void testHiddenBeansDoNotConfuseAutowireByType() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		ConfigurationProcessor configurationProcessor = new ConfigurationProcessor(bf);
		configurationProcessor.processClass(AutowiringConfigurationWithNonHiddenWinner.class);

		assertFalse(bf.containsBean("testBean"));
		DependsOnTestBean dotb = (DependsOnTestBean) bf.getBean("autowireCandidate");
		assertNotNull("Autowire worked", dotb.tb);
		assertEquals("Autowire winner must be visible", "visible", dotb.tb.getName());
	}

	@Test
	public void testAutowireAmbiguityIsRejected() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		ConfigurationProcessor configurationProcessor = new ConfigurationProcessor(bf);
		try {
			configurationProcessor.processClass(InvalidAutowiringConfigurationWithAmbiguity.class);
			bf.getBean("autowireCandidate");
			fail("Should have detected autowiring ambiguity");
		}
		catch (UnsatisfiedDependencyException ex) {
			assertFalse("Useful error message required", ex.getMessage().indexOf("autowireCandidate") == -1);
		}
	}

	@Configuration
	public static class AutowiringConfiguration {
		@Bean()
		protected TestBean testBean() {
			return new TestBean();
		}

		@Bean(autowire = Autowire.BY_TYPE)
		public DependsOnTestBean autowireCandidate() {
			return new DependsOnTestBean();
		}
	}

	@Configuration
	public static class AutowiringConfigurationWithNonHiddenWinner {
		@Bean
		protected TestBean testBean() {
			return new TestBean();
		}

		@Bean
		public TestBean nonHiddenTestBean() {
			TestBean tb = new TestBean();
			tb.setName("visible");
			return tb;
		}

		@Bean(autowire = Autowire.BY_TYPE)
		public DependsOnTestBean autowireCandidate() {
			return new DependsOnTestBean();
		}
	}

	@Configuration
	public static class InvalidAutowiringConfigurationWithAmbiguity {
		@Bean
		public TestBean testBean() {
			return new TestBean();
		}

		@Bean
		public TestBean nonHiddenTestBean() {
			TestBean tb = new TestBean();
			tb.setName("visible");
			return tb;
		}

		@Bean(autowire = Autowire.BY_TYPE)
		public DependsOnTestBean autowireCandidate() {
			return new DependsOnTestBean();
		}
	}

	@Configuration
	public static class BeanCreationMethodsThrowExceptions {
		public static boolean makeItFail;

		@Bean
		public TestBean throwsException() throws Exception {
			if (makeItFail) {
				throw new Exception();
			}
			return new TestBean();
		}

		@Bean
		public TestBean throwsThrowable() throws Throwable {
			if (makeItFail) {
				throw new Throwable();
			}
			return new TestBean();
		}

		@Bean
		public TestBean throwsOtherCheckedException() throws InterruptedException {
			if (makeItFail) {
				throw new InterruptedException();
			}
			return new TestBean();
		}
	}

	@Test
	public void testBeanCreationMethodsThatMayThrowExceptions() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		ConfigurationProcessor configurationProcessor = new ConfigurationProcessor(bf);
		BeanCreationMethodsThrowExceptions.makeItFail = false;
		configurationProcessor.processClass(BeanCreationMethodsThrowExceptions.class);
		assertNotNull(bf.getBean("throwsException"));
		assertNotNull(bf.getBean("throwsThrowable"));
		assertNotNull(bf.getBean("throwsOtherCheckedException"));
	}

	@Test
	public void testBeanCreationMethodsThatDoThrowExceptions() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		ConfigurationProcessor configurationProcessor = new ConfigurationProcessor(bf);
		BeanCreationMethodsThrowExceptions.makeItFail = true;
		try {
			configurationProcessor.processClass(BeanCreationMethodsThrowExceptions.class);
			bf.getBean("throwsException");
			fail();
		}
		catch (BeanCreationException ex) {
			// TODO what to check
		}
	}

	@Configuration
	public static class BeanCreationMethodReturnsNull {
		@Bean
		public TestBean returnsNull() {
			return null;
		}
	}

	@Test
	public void testBeanCreationMethodReturnsNull() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		ConfigurationProcessor configurationProcessor = new ConfigurationProcessor(bf);
		try {
			configurationProcessor.processClass(BeanCreationMethodReturnsNull.class);
			bf.getBean("returnsNull");
			fail();
		}
		catch (BeanCreationException ex) {
			// TODO what to check
		}
	}

	@Configuration
	public static class BeanCreationMethodReturnsVoid {
		@Bean
		public void invalidReturnsVoid() {
		}
	}

	@Test
	public void testBeanCreationMethodCannotHaveVoidReturn() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		ConfigurationProcessor configurationProcessor = new ConfigurationProcessor(bf);
		try {
			configurationProcessor.processClass(BeanCreationMethodReturnsVoid.class);
			fail();
		}
		catch (BeanDefinitionStoreException ex) {
			// TODO what to check
		}
	}

	@Aspect
	@Configuration
	public static class AdvisedAutowiring {
		@Bean(autowire = Autowire.BY_TYPE)
		public Husband husband() {
			return new HusbandImpl();
		}

		@Bean
		public Wife wife() {
			return new Wife();
		}

		@Before("execution(* getWife())")
		protected void log() {
			// nothing
		}
	}

	public static interface Husband {
		Wife getWife();
	}

	public static class HusbandImpl implements Husband {
		private Wife wife;

		public Wife getWife() {
			return wife;
		}

		public void setWife(Wife wife) {
			this.wife = wife;
		}
	}

	public static class Wife {
	}

	@Test
	public void testAutowiringOnProxiedBean() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		ConfigurationProcessor configurationProcessor = new ConfigurationProcessor(bf);
		configurationProcessor.processClass(AdvisedAutowiring.class);
		Husband husband = (Husband) bf.getBean("husband");
		assertTrue(AopUtils.isAopProxy(husband));
		assertNotNull("Advised object should have still been autowired", husband.getWife());
	}

	public abstract static class ValidAutoBeanTest extends ConfigurationSupport {
		@Bean
		public TestBean rod() {
			TestBean rod = new TestBean();
			rod.setName("Rod");
			// rod.setSpouse(kerry());
			return rod;
		}

		@AutoBean
		public abstract TestBean kerry();
	}

	@Test
	public void testValidAutoBean() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		ConfigurationProcessor configurationProcessor = new ConfigurationProcessor(bf);
		configurationProcessor.processClass(ValidAutoBeanTest.class);

		TestBean kerry = (TestBean) bf.getBean("kerry");
		assertEquals("AutoBean was autowired", "Rod", kerry.getSpouse().getName());

		// TODO will not work due to ordering: document?
		// An @Bean can't always depend on an autobean
		// TestBean rod = (TestBean) bf.getBean("rod");
		// assertNotNull(rod.getSpouse());
		// assertSame(rod, rod.getSpouse().getSpouse());
	}

	public abstract static class InvalidAutoBeanTest extends ConfigurationSupport {
		@Bean
		public TestBean rod() {
			TestBean rod = new TestBean();
			rod.setSpouse(kerry());
			return rod;
		}

		// Invalid, as it's on an interface type
		@AutoBean
		public abstract ITestBean kerry();
	}

	@Test(expected = BeanDefinitionStoreException.class)
	public void testInvalidAutoBean() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		ConfigurationProcessor configurationProcessor = new ConfigurationProcessor(bf);
		configurationProcessor.processClass(InvalidAutoBeanTest.class);
	}

	@Configuration
	public static class HiddenBeans {

		public static class BFAwareBean implements BeanFactoryAware {

			public BeanFactory bf;

			private Object[] beans;

			public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
				this.bf = beanFactory;
			}

			/**
			 * @param beans The beans to set.
			 */
			public void setBeans(Object[] beans) {
				this.beans = beans;
			}

			/**
			 * @return Returns the beans.
			 */
			public Object[] getBeans() {
				return beans;
			}

			// TODO: name never gets used... is this by design?
			@SuppressWarnings("unused")
			private String name;

			public BFAwareBean(String name) {
				this.name = name;
			}

			/**
			 * @return Returns the bf.
			 */
			public BeanFactory getBf() {
				return bf;
			}

		}

		@Bean
		Object packageBean() {
			return new BFAwareBean("package");
		}

		@Bean
		protected Object protectedBean() {
			return new BFAwareBean("protected");
		}

		@Bean
		public BFAwareBean beans() {
			BFAwareBean bean = new BFAwareBean("public");
			bean.setBeans(new Object[] { packageBean(), protectedBean() });
			return bean;
		}
	}

	@Test
	public void testHiddenBeans() {
		GenericApplicationContext ctx = new GenericApplicationContext();
		ConfigurationProcessor configurationProcessor = new ConfigurationProcessor(ctx);

		configurationProcessor.processClass(HiddenBeans.class);

		ctx.refresh();

		BeanFactory bf = ctx.getBeanFactory();
		// hidden beans
		assertFalse(bf.containsBean("protectedBean"));
		assertFalse(bf.containsBean("packageBean"));

		BFAwareBean beans = (BFAwareBean) bf.getBean("beans");
		assertEquals(2, beans.getBeans().length);

		BeanFactory hiddenBF = ((BFAwareBean) beans.getBeans()[1]).bf;
		assertNotSame(bf, hiddenBF);

		assertTrue(hiddenBF.containsBean("protectedBean"));
		assertTrue(hiddenBF.containsBean("packageBean"));
	}

	@Test
	public void testBeanDefinitionCount() throws Exception {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		ConfigurationProcessor configurationProcessor = new ConfigurationProcessor(bf);

		// 3 @Bean + 1 @Configuration
		assertEquals(4, configurationProcessor.processClass(HiddenBeans.class));
		// 2 @Bean + 1 Advice (@Before) + 1 @Configuration
		assertEquals(4, configurationProcessor.processClass(AdvisedAutowiring.class));
		// 3 @Bean + 1 @Configuration
		assertEquals(4, configurationProcessor.processClass(BeanCreationMethodsThrowExceptions.class));
		// 2 @Bean + 1 @Configuration
		assertEquals(3, configurationProcessor.processClass(AutowiringConfiguration.class));
		// 6 @Bean + 1 Configuration
		assertEquals(7, configurationProcessor.processClass(BaseConfiguration.class));
		// 2 @Bean + 1 Configuration
		assertEquals(3, configurationProcessor.processClass(HotSwapConfiguration.class));
		// 1 @Bean + 1 @Autobean + 1 Configuration
		assertEquals(3, configurationProcessor.processClass(ValidAutoBeanTest.class));
	}

	public static class RequiresProperty extends ConfigurationSupport {
		@Bean
		public TestBean costin() {
			TestBean costin = new TestBean();
			costin.getName();
			// costin.setName(getString("costin.name"));
			ITestBean uninterestingBeanThatWillNotBeReturned = new TestBean();
			uninterestingBeanThatWillNotBeReturned.getName();
			return costin;
		}
	}
}
