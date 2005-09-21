/*
 * Copyright 2002-2004 the original author or authors.
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
import org.springframework.webflow.support.FlowConversionService;

/**
 * Simple helper adapter for implementing the flow service locator interface.
 * Useful since many of these methods are optional. The method implementations
 * provided by this adapter will throw UnsupportedOperationException, except
 * for the {@link #getConversionService()} method, which will return the default
 * {@link org.springframework.webflow.support.FlowConversionService}.
 * 
 * @see org.springframework.webflow.support.FlowConversionService
 * 
 * @author Keith Donald
 * @author Erwin Vervaet
 */
public class FlowServiceLocatorAdapter implements FlowServiceLocator {

	public Flow createFlow(AutowireMode autowireMode) throws ServiceLookupException {
		throw new UnsupportedOperationException();
	}
	
	public Flow createFlow(Class implementationClass, AutowireMode autowireMode) throws ServiceLookupException {
		throw new UnsupportedOperationException();
	}

	public Flow getFlow(String id) throws ServiceLookupException {
		throw new UnsupportedOperationException();
	}

	public Flow getFlow(Class implementationClass) throws ServiceLookupException {
		throw new UnsupportedOperationException();
	}
	
	public State createState(Class implementationClass, AutowireMode autowireMode) throws ServiceLookupException {
		throw new UnsupportedOperationException();
	}

	public State getState(String id) throws ServiceLookupException {
		throw new UnsupportedOperationException();
	}

	public State getState(Class implementationClass) throws ServiceLookupException {
		throw new UnsupportedOperationException();
	}
	
	public Transition createTransition(Class implementationClass, AutowireMode autowireMode) throws ServiceLookupException {
		throw new UnsupportedOperationException();
	}
	
	public Transition getTransition(String id) throws ServiceLookupException {
		throw new UnsupportedOperationException();
	}
	
	public Transition getTransition(Class implementationClass) throws ServiceLookupException {
		throw new UnsupportedOperationException();
	}
	
	public Action createAction(Class implementationClass, AutowireMode autowireMode) throws ServiceLookupException {
		throw new UnsupportedOperationException();
	}
	
	public Action getAction(String id) throws ServiceLookupException {
		throw new UnsupportedOperationException();
	}

	public Action getAction(Class implementationClass) throws ServiceLookupException {
		throw new UnsupportedOperationException();
	}

	public Object createBean(Class implementationClass, AutowireMode autowireMode) throws ServiceLookupException {
		throw new UnsupportedOperationException();
	}

	public Object getBean(String beanId) throws ServiceLookupException {
		throw new UnsupportedOperationException();
	}
	
	public Object getBean(Class implementationClass) throws ServiceLookupException {
		throw new UnsupportedOperationException();
	}

	public FlowAttributeMapper createFlowAttributeMapper(Class attributeMapperImplementationClass, AutowireMode autowireMode)
			throws ServiceLookupException {
		throw new UnsupportedOperationException();
	}
	
	public FlowAttributeMapper getFlowAttributeMapper(String id) throws ServiceLookupException {
		throw new UnsupportedOperationException();
	}

	public FlowAttributeMapper getFlowAttributeMapper(Class implementationClass) throws ServiceLookupException {
		throw new UnsupportedOperationException();
	}

	public ConversionService getConversionService() {
		return new FlowConversionService();
	}
}