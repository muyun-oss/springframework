/*
 * Copyright 2004-2007 the original author or authors.
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
package org.springframework.webflow.engine.builder;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.Resource;
import org.springframework.webflow.definition.FlowDefinition;
import org.springframework.webflow.definition.FlowId;
import org.springframework.webflow.definition.registry.FlowDefinitionConstructionException;
import org.springframework.webflow.definition.registry.FlowDefinitionHolder;
import org.springframework.webflow.engine.Flow;
import org.springframework.webflow.util.ResourceHolder;

/**
 * A flow definition holder that can detect changes on an underlying flow definition resource and refresh that resource
 * automatically.
 * <p>
 * This class is thread-safe.
 * <p>
 * Note that this {@link FlowDefinition} holder uses a {@link Flow} assembler. This is normal since a {@link Flow} is a
 * {@link FlowDefinition}! This class bridges the <i>abstract</i> world of {@link FlowDefinition flow definitions}
 * with the <i>concrete</i> world of {@link Flow flow implementations}.
 * 
 * @see FlowAssembler
 * @see FlowDefinition
 * @see Flow
 * 
 * @author Keith Donald
 */
public class RefreshableFlowDefinitionHolder implements FlowDefinitionHolder {

	private static final Log logger = LogFactory.getLog(RefreshableFlowDefinitionHolder.class);

	/**
	 * The flow definition assembled by this assembler.
	 */
	private FlowDefinition flowDefinition;

	/**
	 * The flow assembler.
	 */
	private FlowAssembler assembler;

	/**
	 * A last modified date for the backing flow definition resource, used to support automatic reassembly on resource
	 * change.
	 */
	private long lastModified;

	/**
	 * A flag indicating whether or not this holder is in the middle of the assembly process.
	 */
	private boolean assembling;

	/**
	 * Creates a new refreshable flow definition holder that uses the configured assembler (GOF director) to drive flow
	 * assembly, on initial use and on any resource change or refresh.
	 * @param assembler the flow assembler to use
	 */
	public RefreshableFlowDefinitionHolder(FlowAssembler assembler) {
		this.assembler = assembler;
	}

	public FlowId getFlowDefinitionId() {
		return assembler.getFlowId();
	}

	public synchronized FlowDefinition getFlowDefinition() throws FlowDefinitionConstructionException {
		if (assembling) {
			// must return early assembly result
			return getFlowBuilder().getFlow();
		}
		if (flowDefinition == null) {
			lastModified = calculateLastModified();
			assembleFlow();
		} else {
			refreshIfChanged();
		}
		return flowDefinition;
	}

	public synchronized void refresh() throws FlowBuilderException {
		assembleFlow();
	}

	// internal helpers

	/**
	 * Helper that retrieves the last modified date by querying the backing flow resource.
	 * @return the last modified date, or 0L if it could not be retrieved
	 */
	private long calculateLastModified() {
		if (getFlowBuilder() instanceof ResourceHolder) {
			Resource resource = ((ResourceHolder) getFlowBuilder()).getResource();
			try {
				if (logger.isDebugEnabled()) {
					logger.debug("Calculating last modified timestamp for flow definition resource '" + resource + "'");
				}
				return resource.getFile().lastModified();
			} catch (IOException e) {
				// ignore, last modified checks not supported
			}
		}
		return 0L;
	}

	/**
	 * Assemble the held flow definition, delegating to the configured FlowAssembler (director).
	 */
	private void assembleFlow() throws FlowBuilderException {
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Assembling flow definition with id '" + assembler.getFlowId() + "'");
			}
			assembling = true;
			flowDefinition = assembler.assembleFlow();
		} finally {
			assembling = false;
		}
	}

	/**
	 * Reassemble the flow if its underlying resource has changed.
	 */
	private void refreshIfChanged() {
		long calculatedLastModified = calculateLastModified();
		if (calculatedLastModified > lastModified) {
			if (logger.isDebugEnabled()) {
				logger.debug("Resource modification detected, reloading flow definition with id '"
						+ assembler.getFlowId() + "'");
			}
			assembleFlow();
			lastModified = calculatedLastModified;
		}
	}

	/**
	 * Returns the flow builder that actually builds the Flow definition.
	 */
	private FlowBuilder getFlowBuilder() {
		return assembler.getFlowBuilder();
	}

}