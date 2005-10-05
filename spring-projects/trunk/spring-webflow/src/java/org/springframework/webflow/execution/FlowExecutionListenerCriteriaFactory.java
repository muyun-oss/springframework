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
package org.springframework.webflow.execution;

import org.springframework.core.style.StylerUtils;
import org.springframework.core.style.ToStringCreator;
import org.springframework.util.Assert;
import org.springframework.webflow.Flow;

/**
 * Static factory for producing common flow execution listener criteria.
 * 
 * @see org.springframework.webflow.execution.FlowExecutionListenerCriteria
 * 
 * @author Keith Donald
 * @author Erwin Vervaet
 */
public class FlowExecutionListenerCriteriaFactory {

	/**
	 * A flow execution listener criteria implementation that matches for all
	 * flows.
	 */
	public static class WildcardFlowExecutionListenerCriteria implements FlowExecutionListenerCriteria {

		/**
		 * The string representation of the wildcard flow id.
		 */
		public static final String WILDCARD_FLOW_ID = "*";

		public boolean appliesTo(Flow flow) {
			return true;
		}

		public String toString() {
			return WILDCARD_FLOW_ID;
		}
	}

	/**
	 * A flow execution listener criteria implementation that matches flows with
	 * a specified id.
	 */
	public static class FlowIdFlowExecutionListenerCriteria implements FlowExecutionListenerCriteria {

		private String[] flowIds;

		/**
		 * Create a new flow id matching flow execution listener criteria
		 * implemenation.
		 * @param flowId the flow id to match
		 */
		public FlowIdFlowExecutionListenerCriteria(String flowId) {
			Assert.notNull(flowId, "The flow id is required");
			this.flowIds = new String[] { flowId };
		}

		/**
		 * Create a new flow id matching flow execution listener criteria
		 * implemenation.
		 * @param flowId the flow ids to match
		 */
		public FlowIdFlowExecutionListenerCriteria(String[] flowIds) {
			Assert.notEmpty(flowIds, "The flow id is required");
			this.flowIds = flowIds;
		}

		public boolean appliesTo(Flow flow) {
			for (int i = 0; i < flowIds.length; i++) {
				if (flowIds[i].equals(flow.getId())) {
					return true;
				}
			}
			return false;
		}

		public String toString() {
			return new ToStringCreator(this).append("flowIds", StylerUtils.style(flowIds)).toString();
		}
	}

	/**
	 * Returns a wild card criteria that matches all flows.
	 */
	public static FlowExecutionListenerCriteria allFlows() {
		return new WildcardFlowExecutionListenerCriteria();
	}

	/**
	 * Returns a criteria that just matches a flow with the specified id.
	 * @param flowId the flow id to match
	 */
	public static FlowExecutionListenerCriteria flow(String flowId) {
		return new FlowIdFlowExecutionListenerCriteria(flowId);
	}

	/**
	 * Returns a criteria that just matches a flow if it is identified by one of
	 * the specified ids.
	 * @param flowId the flow id to match
	 */
	public static FlowExecutionListenerCriteria flows(String[] flowIds) {
		return new FlowIdFlowExecutionListenerCriteria(flowIds);
	}
}