package org.springframework.webflow.execution.factory;

import org.springframework.webflow.execution.FlowExecution;
import org.springframework.webflow.execution.FlowExecutionKey;

public interface FlowExecutionKeyFactory {
	public FlowExecutionKey getKey(FlowExecution execution);
}
