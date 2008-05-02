package org.springframework.config.java.context;

import static java.lang.String.format;
import static org.springframework.util.ClassUtils.convertClassNameToResourcePath;

import java.io.IOException;
import java.util.ArrayList;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.config.java.model.ReflectiveJavaConfigBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.support.AbstractRefreshableApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * re-implementing a simplified version of the context.  This will be swapped
 * in for the existing {@link LegacyJavaConfigApplicationContext} when feature-complete.
 */
public class JavaConfigApplicationContext extends AbstractRefreshableApplicationContext
                                          implements ConfigurableJavaConfigApplicationContext {

	// TODO: is the factory really necessary?
	private final ClassPathScanningCandidateComponentProvider scanner =
		new ClassPathScanningConfigurationProviderFactory().getProvider(this);

	// TODO: should be LinkedHashSet?
	private ArrayList<ClassPathResource> configClassResources = new ArrayList<ClassPathResource>();

	private ArrayList<ClassPathResource> aspectClassResources = new ArrayList<ClassPathResource>();

	/** context is configurable until refresh() is called */
	private boolean openForConfiguration = true;

	public JavaConfigApplicationContext() { }

	public JavaConfigApplicationContext(Class<?>... configClasses) {
		addConfigClasses(configClasses);
		refresh();
	}

	public JavaConfigApplicationContext(String... basePackages) {
		addBasePackages(basePackages);
		refresh();
	}

	public JavaConfigApplicationContext(Class<?>[] classes, String[] packages) {
		addConfigClasses(classes);
		addBasePackages(packages);
		refresh();
	}

	/**
	 * Create a new context with <var>parent</var> as the parent.  Requires an explicit call to refresh().
	 *
	 * @param parent
	 */
	public JavaConfigApplicationContext(JavaConfigApplicationContext parent) {
		super(parent);
	}

	public JavaConfigApplicationContext(ApplicationContext parent, Class<?>... classes) {
		super(parent);
		addConfigClasses(classes);
		refresh();
	}

	@Override
	protected void prepareRefresh() {
		super.prepareRefresh();
		addBeanFactoryPostProcessor(new ConfigurationEnhancingBeanFactoryPostProcessor(this));
	}

	@Override
	protected void finishRefresh() {
		super.finishRefresh();
		openForConfiguration = false;
	}

	@Override
	protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) throws IOException, BeansException {
		BeanDefinitionReader reader = new ReflectiveJavaConfigBeanDefinitionReader(beanFactory, aspectClassResources);
		reader.loadBeanDefinitions(configClassResources.toArray(new Resource[configClassResources.size()]));
	}

	@Override
	public void setParent(ApplicationContext parent) {
		assertOpenForConfiguration("setParent");
		super.setParent(parent);
	}

	public void addConfigClasses(Class<?>... classes) {
		assertOpenForConfiguration("addConfigClasses");
		for(Class<?> configClass : classes)
			addConfigClassAsResource(configClass.getName());
	}

	public void addBasePackages(String... basePackages) {
		assertOpenForConfiguration("addBasePackages");
		for (String basePackage : basePackages)
			for(BeanDefinition beanDef : scanner.findCandidateComponents(basePackage))
				addConfigClassAsResource(beanDef.getBeanClassName());
	}

	public void addAspectClasses(Class<?>... classes) {
		assertOpenForConfiguration("addAspectClasses");
		for (Class<?> aspectClass : classes)
			addAspectClassAsResource(aspectClass.getName());
	}

	private void assertOpenForConfiguration(String attemptedMethod) {
		if(!openForConfiguration)
    		throw new IllegalStateException(format(
    				"context is closed for configuration. %s() must be called " +
    				"before refresh(), consider using the no-arg constructor for %s",
    				attemptedMethod, this.getClass().getSimpleName()
    			));
	}

	private void addConfigClassAsResource(String fqClassName) {
		configClassResources.add(new ClassPathResource(convertClassNameToResourcePath(fqClassName)));
	}

	private void addAspectClassAsResource(String fqClassName) {
		aspectClassResources.add(new ClassPathResource(convertClassNameToResourcePath(fqClassName)));
	}

	@Deprecated
	public void addConfigClass(Class<?> cls) { addConfigClasses(cls); }

	@Deprecated
	public void setBasePackages(String... basePackages) { addBasePackages(basePackages); }

	@Deprecated
	public void setConfigClasses(Class<?>... classes) { addConfigClasses(classes); }

	public <T> T getBean(Class<T> type) {
		return TypeSafeBeanFactoryUtils.getBean(this.getBeanFactory(), type);
	}

	public <T> T getBean(Class<T> type, String beanName) {
		return TypeSafeBeanFactoryUtils.getBean(this.getBeanFactory(), type, beanName);
	}

}