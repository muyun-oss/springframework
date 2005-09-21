/*
 * Copyright 2002-2005 the original author or authors.
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
package org.springframework.webflow.access;

import org.springframework.binding.convert.ConversionService;
import org.springframework.webflow.Action;
import org.springframework.webflow.Flow;
import org.springframework.webflow.FlowAttributeMapper;
import org.springframework.webflow.State;
import org.springframework.webflow.Transition;

/**
 * Service locator interface used by flow builders at configuration time to
 * retrieve needed artifacts.
 * <p>
 * Note that this service locator is a configuration time object. It is not used
 * during flow execution!
 * 
 * @author Keith Donald
 * @author Erwin Vervaet
 */
public interface FlowServiceLocator extends FlowLocator, ActionLocator {
	
	// dealing with flows
	
	/**
	 * Request that the registry backed by this locator instantiate the default
	 * flow implementation class, using the specified autowire policy.
	 * Implementing this method is optional.
	 * @param autowireMode the autowire policy
	 * @return the instantiated (and possibly autowired) flow
	 * @throws ServiceLookupException when the flow cannot be created
	 */
	public Flow createFlow(AutowireMode autowireMode) throws ServiceLookupException;
	
	/**
	 * Request that the registry backed by this locator instantiate the flow
	 * of the specified implementation class, using the specified autowire policy.
	 * Implementing this method is optional.
	 * @param implementationClass the flow implementation class
	 * @param autowireMode the autowire policy
	 * @return the instantiated (and possibly autowired) flow
	 * @throws ServiceLookupException when the flow cannot be created
	 */
	public Flow createFlow(Class implementationClass, AutowireMode autowireMode) throws ServiceLookupException;
	
	/**
	 * Lookup a flow of specified implementation class; there must be exactly one
	 * flow implementation of the specified implementation class in the registry this
	 * locator queries.
	 * Implementing this method is optional.
	 * @param implementationClass the required implementation class
	 * @return the flow
	 * @throws ServiceLookupException when the flow cannot be found, or more
	 *         than one flow of the specified type exists
	 */
	public Flow getFlow(Class implementationClass) throws ServiceLookupException;

	// dealing with states

	/**
	 * Request that the registry backed by this locator instantiate the state
	 * of the specified implementation class, using the specified autowire policy.
	 * Implementing this method is optional.
	 * @param implementationClass the state implementation class
	 * @param autowireMode the autowire policy
	 * @return the instantiated (and possibly autowired) state
	 * @throws ServiceLookupException when the state cannot be created
	 */
	public State createState(Class implementationClass, AutowireMode autowireMode) throws ServiceLookupException;

	/**
	 * Lookup a Flow state with specified id.
	 * Implementing this method is optional.
	 * @param id the state id
	 * @return the state
	 * @throws ServiceLookupException when the state cannot be found
	 */
	public State getState(String id) throws ServiceLookupException;

	/**
	 * Lookup a state of the specified implementation class.
	 * Implementing this method is optional.
	 * @param implementationClass the required implementation class
	 * @return the state
	 * @throws ServiceLookupException when the state cannot be found
	 */
	public State getState(Class implementationClass) throws ServiceLookupException;

	// dealing with transitions

	/**
	 * Request that the registry backed by this locator instantiate the transition
	 * of the specified implementation class, using the specified autowire policy.
	 * Implementing this method is optional.
	 * @param implementationClass the transition implementation class
	 * @param autowireMode the autowire policy
	 * @return the instantiated (and possibly autowired) transition
	 * @throws ServiceLookupException when the transition object
	 *         cannot be created
	 */
	public Transition createTransition(Class implementationClass, AutowireMode autowireMode) throws ServiceLookupException;

	/**
	 * Lookup a transition with specified id.
	 * Implementing this method is optional.
	 * @param id the transition id
	 * @return the transition
	 * @throws ServiceLookupException when the transition cannot be found
	 */
	public Transition getTransition(String id) throws ServiceLookupException;

	/**
	 * Lookup a transition of specified implementation class.
	 * Implementing this method is optional.
	 * @param implementationClass the required implementation class
	 * @return the transition
	 * @throws ServiceLookupException when the transition cannot be found
	 */
	public Transition getTransition(Class implementationClass) throws ServiceLookupException;

	// dealing with actions

	/**
	 * Request that the registry backed by this locator instantiate the action
	 * of the specified implementation class, using the specified autowire policy.
	 * Implementing this method is optional.
	 * @param implementationClass the action implementation class
	 * @param autowireMode the autowire policy
	 * @return the instantiated (and possibly autowired) action
	 * @throws ServiceLookupException when the action cannot be created
	 */
	public Action createAction(Class implementationClass, AutowireMode autowireMode) throws ServiceLookupException;

	/**
	 * Lookup an action instance by type that implements the specified implementation class.
	 * Implementing this method is optional.
	 * @param implementationClass the required implementation class
	 * @return the action
	 * @throws ServiceLookupException when the action cannot be found
	 */
	public Action getAction(Class implementationClass) throws ServiceLookupException;
	
	// dealing with beans
	
	/**
	 * Request that the registry backed by this locator instantiate an instance of
	 * an abitrary bean (POJO) of the specified implementation class using the given 
	 * autowire policy.
	 * @param implementationClass the bean class
	 * @param autowireMode the autowire policy
	 * @return the newly created bean (possibly autowired)
	 * @throws ServiceLookupException when the bean cannot be created
	 */
	public Object createBean(Class implementationClass, AutowireMode autowireMode) throws ServiceLookupException;

	/**
	 * Request that the registry backed by this locator return an instance of
	 * an abitrary bean (POJO) with the provided id.
	 * @param beanId the bean id
	 * @return the retrieved bean
	 * @throws ServiceLookupException when the bean cannot be found
	 */
	public Object getBean(String beanId) throws ServiceLookupException;

	/**
	 * Request that the registry backed by this locator return an instance of
	 * an abitrary bean (POJO) of the specified implementation.
	 * @param implementationClass the bean class
	 * @return the retrieved bean
	 * @throws ServiceLookupException when the bean cannot be found
	 */
	public Object getBean(Class implementationClass) throws ServiceLookupException;

	// dealing with attribute mappers
	
	/**
	 * Request that the registry backed by this locator instantiate the flow
	 * attribute mapper of the specified implementation class, using the given
	 * autowire policy.
	 * @param attributeMapperImplementationClass the implementation class
	 * @param autowireMode the autowire policy
	 * @return the instantiated (and possibly autowired) attribute mapper
	 */
	public FlowAttributeMapper createFlowAttributeMapper(Class attributeMapperImplementationClass,
			AutowireMode autowireMode) throws ServiceLookupException;

	/**
	 * Lookup a flow model attribute mapper with specified id.
	 * @param id the flow model mapper id
	 * @return the flow model mapper
	 * @throws ServiceLookupException when the flow model mapper cannot be found
	 */
	public FlowAttributeMapper getFlowAttributeMapper(String id) throws ServiceLookupException;

	/**
	 * Lookup a flow model attribute mapper of the specified implementation class.
	 * @param implementationClass the required implementation class
	 * @return the flow model mapper
	 * @throws ServiceLookupException when the flow model mapper cannot be found
	 */
	public FlowAttributeMapper getFlowAttributeMapper(Class implementationClass)
			throws ServiceLookupException;
	
	// the conversion service
	
	/**
	 * Returns the service responsible for performing type conversion for the
	 * webflow system.
	 * @return the web flow system type conversion service
	 */
	public ConversionService getConversionService();
}