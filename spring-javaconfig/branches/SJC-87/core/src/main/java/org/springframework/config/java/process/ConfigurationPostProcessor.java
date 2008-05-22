package org.springframework.config.java.process;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.config.java.context.ConfigurationBeanDefinitionDecoratingBeanFactoryPostProcessor;
import org.springframework.config.java.context.ConfigurationClassParsingBeanFactoryPostProcessor;
import org.springframework.config.java.context.ConfigurationEnhancingBeanFactoryPostProcessor;
import org.springframework.config.java.context.InternalBeanFactoryEstablishingBeanFactoryPostProcessor;
import org.springframework.config.java.context.JavaConfigInternalPostProcessor;
import org.springframework.config.java.naming.BeanNamingStrategy;
import org.springframework.config.java.naming.MethodNameStrategy;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.util.Assert;

public class ConfigurationPostProcessor implements BeanFactoryPostProcessor, ApplicationContextAware, Ordered, JavaConfigInternalPostProcessor {

	private AbstractApplicationContext ctx;
	private BeanNamingStrategy namingStrategy = new MethodNameStrategy();

	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		new ConfigurationBeanDefinitionDecoratingBeanFactoryPostProcessor().postProcessBeanFactory(beanFactory);
		new InternalBeanFactoryEstablishingBeanFactoryPostProcessor(ctx).postProcessBeanFactory(beanFactory);
		new ConfigurationClassParsingBeanFactoryPostProcessor(namingStrategy).postProcessBeanFactory(beanFactory);
		new ConfigurationEnhancingBeanFactoryPostProcessor(namingStrategy).postProcessBeanFactory(beanFactory);
	}

	public void setApplicationContext(ApplicationContext ctx) throws BeansException {
		Assert.isInstanceOf(AbstractApplicationContext.class, ctx);
		this.ctx = (AbstractApplicationContext) ctx;
	}

	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

	public void setNamingStrategy(BeanNamingStrategy namingStrategy) {
		this.namingStrategy = namingStrategy;
	}

}
