package org.springframework.webflow.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.binding.convert.ConversionException;
import org.springframework.binding.convert.ConversionExecutor;
import org.springframework.binding.convert.ConversionService;
import org.springframework.binding.convert.support.DefaultConversionService;
import org.springframework.webflow.Flow;
import org.springframework.webflow.config.support.TextToTransitionCriteria;
import org.springframework.webflow.config.support.TextToViewSelector;

/**
 * Abstract base implementation of a flow builder defining common functionality
 * needed by most concrete flow builder implementations. All flow related artifacts
 * are expected to be defined in the bean factory defiing this flow builder.
 * Subclasses can use a {@link org.springframework.webflow.access.FlowArtifactFactory}
 * to easily access that bean factory.
 * 
 * @see org.springframework.beans.factory.BeanFactory
 * @see org.springframework.webflow.access.FlowArtifactFactory
 * 
 * @author Keith Donald
 * @author Erwin Vervaet
 */
public abstract class BaseFlowBuilder implements FlowBuilder, BeanFactoryAware {

	/**
	 * A logger instance that can be used in subclasses.
	 */
	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * The <code>Flow</code> built by this builder.
	 */
	private Flow flow;

	/**
	 * The conversion service to convert to flow-related artifacts, typically
	 * from string encoded representations.
	 */
	private ConversionService conversionService;

	/**
	 * The bean factory defining this flow builder.
	 */
	private BeanFactory beanFactory;

	/**
	 * Default constructor for subclassing.
	 */
	protected BaseFlowBuilder() {
	}
	
	/**
	 * Create a new flow builder looking up required flow artifacts
	 * in given bean factory.
	 * @param beanFactory the bean factory to be used, typically the bean
	 * factory defining this flow builder
	 */
	protected BaseFlowBuilder(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}
	
	/**
	 * Returns the bean factory defining this flow builder.
	 */
	protected BeanFactory getBeanFactory() {
		return beanFactory;
	}
	
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	/**
	 * Returns the conversion service.
	 */
	protected ConversionService getConversionService() {
		return conversionService;
	}

	/**
	 * Sets the conversion service.
	 */
	public void setConversionService(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	/**
	 * Initialize this builder's conversion service and register default
	 * converters. Called by subclasses who wish to use the conversion
	 * infrastructure.
	 */
	protected void initConversionService() {
		if (getConversionService() == null) {
			DefaultConversionService service = new DefaultConversionService();
			service.addConverter(new TextToTransitionCriteria());
			service.addConverter(new TextToViewSelector(service));
			setConversionService(service);
		}
	}

	/**
	 * Returns a conversion executor capable of converting string objects to the
	 * target class aliased by the provided alias.
	 * @param targetAlias the target class alias, e.g "long" or "float"
	 * @return the conversion executor, or <code>null</code> if no suitable
	 * converter exists for given alias
	 */
	protected ConversionExecutor fromStringToAliased(String targetAlias) {
		return getConversionService().getConversionExecutorByTargetAlias(String.class, targetAlias);
	}

	/**
	 * Returns a converter capable of converting a string value to the given
	 * type.
	 * @param targetType the type you wish to convert to (from a string)
	 * @return the converter
	 * @throws ConversionException when the converter cannot be found
	 */
	protected ConversionExecutor fromStringTo(Class targetType) throws ConversionException {
		return getConversionService().getConversionExecutor(String.class, targetType);
	}

	/**
	 * Get the flow (result) built by this builder.
	 */
	protected Flow getFlow() {
		return flow;
	}

	/**
	 * Set the flow being built by this builder.
	 */
	protected void setFlow(Flow flow) {
		this.flow = flow;
	}

	public Flow getResult() {
		getFlow().resolveStateTransitionsTargetStates();
		return getFlow();
	}
}