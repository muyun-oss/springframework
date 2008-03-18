package org.springframework.webflow.engine;

import org.springframework.binding.mapping.MappingResults;

public class FlowInputMappingException extends FlowAttributeMappingException {
	public FlowInputMappingException(String flowId, MappingResults results) {
		super(flowId, null, results, "Errors occured during flow input mapping; errors = " + results.getErrorResults());
	}

	public FlowInputMappingException(String flowId, String stateId, MappingResults results) {
		super(flowId, stateId, results, "Errors occured during flow input mapping; errors = "
				+ results.getErrorResults());
	}
}
