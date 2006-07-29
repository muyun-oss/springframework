/*
 * Copyright 2005 the original author or authors.
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

package org.springframework.ws.endpoint;

import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.ws.EndpointInterceptor;
import org.springframework.ws.EndpointInvocationChain;
import org.springframework.ws.EndpointMapping;
import org.springframework.ws.context.MessageContext;

/**
 * Abstract base class for EndpointMapping implementations. Supports a default endpoint, and endpoint interceptors.
 *
 * @author Arjen Poutsma
 * @see #getEndpointInternal(org.springframework.ws.context.MessageContext)
 * @see org.springframework.ws.EndpointInterceptor
 */
public abstract class AbstractEndpointMapping extends ApplicationObjectSupport implements EndpointMapping {

    private Object defaultEndpoint;

    private EndpointInterceptor[] interceptors;

    /**
     * Returns the default endpoint for this endpoint mapping.
     *
     * @return the default endpoint mapping, or null if none
     */
    protected final Object getDefaultEndpoint() {
        return defaultEndpoint;
    }

    /**
     * Sets the default endpoint for this endpoint mapping. This endpoint will be returned if no specific mapping was
     * found.
     * <p/>
     * Default is <code>null</code>, indicating no default endpoint.
     *
     * @param defaultEndpoint the default endpoint, or null if none
     */
    public final void setDefaultEndpoint(Object defaultEndpoint) {
        this.defaultEndpoint = defaultEndpoint;
        logger.info("Default mapping to endpoint [" + this.defaultEndpoint + "]");
    }

    /**
     * Returns the the endpoint interceptors to apply to all endpoints mapped by this endpoint mapping.
     *
     * @return array of endpoint interceptors, or <code>null</code> if none
     */
    public EndpointInterceptor[] getInterceptors() {
        return interceptors;
    }

    /**
     * Sets the endpoint interceptors to apply to all endpoints mapped by this endpoint mapping.
     *
     * @param interceptors array of endpoint interceptors, or <code>null</code> if none
     */
    public final void setInterceptors(EndpointInterceptor[] interceptors) {
        this.interceptors = interceptors;
    }

    /**
     * Look up an endpoint for the given message context, falling back to the default endpoint if no specific one is
     * found.
     *
     * @return the looked up endpoint instance, or the default endpoint
     * @see #getEndpointInternal(org.springframework.ws.context.MessageContext)
     */
    public final EndpointInvocationChain getEndpoint(MessageContext messageContext) throws Exception {
        Object endpoint = getEndpointInternal(messageContext);
        if (endpoint == null) {
            endpoint = defaultEndpoint;
        }
        if (endpoint == null) {
            return null;
        }
        if (endpoint instanceof String) {
            String endpointName = (String) endpoint;
            endpoint = resolveStringEndpoint(endpointName);
            if (endpoint == null) {
                return null;
            }
        }
        return createEndpointInvocationChain(messageContext, endpoint, interceptors);
    }

    /**
     * Creates a new <code>EndpointInvocationChain</code> based on the given message context, endpoint, and
     * interceptors. Default implementation creates a chain based on the set interceptors.
     *
     * @param endpoint     the endpoint
     * @param interceptors the endpoint interceptors
     * @return the created invocation chain
     * @see #setInterceptors(org.springframework.ws.EndpointInterceptor[])
     */
    protected EndpointInvocationChain createEndpointInvocationChain(MessageContext messageContext,
                                                                    Object endpoint,
                                                                    EndpointInterceptor[] interceptors) {
        return new EndpointInvocationChain(endpoint, interceptors);
    }

    /**
     * Resolves an endpoint string. If the given string can is a bean name, it is resolved using the application
     * context.
     *
     * @param endpointName the endpoint name
     * @return the resolved enpoint, or <code>null</code> if the name could not be resolved
     */
    protected Object resolveStringEndpoint(String endpointName) {
        if (getApplicationContext().containsBean(endpointName)) {
            return getApplicationContext().getBean(endpointName);
        }
        else {
            return null;
        }
    }

    /**
     * Lookup an endpoint for the given request, returning <code>null</code> if no specific one is found. This template
     * method is called by getEndpoint, a <code>null</code> return value will lead to the default handler, if one is
     * set.
     * <p/>
     * The returned endpoint can be a string, in which case it is resolved as a bean name. Also, it can take the form
     * <code>beanName#method</code>, in which case the method is resolved.
     *
     * @return the looked up endpoint instance, or null
     * @throws Exception if there is an error
     */
    protected abstract Object getEndpointInternal(MessageContext messageContext) throws Exception;
}
