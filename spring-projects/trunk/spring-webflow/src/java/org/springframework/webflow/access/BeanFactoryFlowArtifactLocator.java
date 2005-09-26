package org.springframework.webflow.access;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.util.Assert;
import org.springframework.webflow.Action;
import org.springframework.webflow.Flow;
import org.springframework.webflow.FlowAttributeMapper;
import org.springframework.webflow.action.LocalBeanInvokingAction;

/**
 * A flow artifact locator that pulls its artifacts from a standard Spring
 * BeanFactory.
 * @author Keith Donald
 */
public class BeanFactoryFlowArtifactLocator implements FlowArtifactLocator {

	/**
	 * The Spring bean factory.
	 */
	private BeanFactory beanFactory;

	/**
	 * An segregated flow locator to delegate to for retrieving flow definitions.
	 */
	private FlowLocator flowLocator;

	/**
	 * Creates a flow artifact locator that retrieves artifacts from the 
	 * provided bean factory
	 * @param beanFactory The spring bean factory, may not be null.
	 */
	public BeanFactoryFlowArtifactLocator(BeanFactory beanFactory) {
		Assert.notNull(beanFactory, "The beanFactory is required");
		this.beanFactory = beanFactory;
		this.flowLocator = new BeanFactoryFlowLocator(beanFactory);
	}

	public Flow getFlow(String id) throws FlowArtifactLookupException {
		return flowLocator.getFlow(id);
	}

	public Action getAction(String id) throws FlowArtifactLookupException {
		try {
			return toAction(beanFactory.getBean(id));
		}
		catch (BeansException e) {
			throw new FlowArtifactLookupException(Action.class, id, e);
		}
	}

	/**
	 * Turn the given service object into an action. If the given service object
	 * implements the <code>Action</code> interface, it is returned as is,
	 * otherwise it is wrapped in an action that can invoke a method on the
	 * service bean.
	 * @param service the service bean
	 * @return the action
	 */
	protected Action toAction(Object service) {
		if (service instanceof Action) {
			return (Action)service;
		}
		else {
			return new LocalBeanInvokingAction(service);
		}
	}

	public FlowAttributeMapper getFlowAttributeMapper(String id) {
		try {
			return (FlowAttributeMapper)beanFactory.getBean(id);
		}
		catch (BeansException e) {
			throw new FlowArtifactLookupException(FlowAttributeMapper.class, id, e);
		}
	}
}