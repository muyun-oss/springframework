package org.springframework.binding.mapping.results;

import org.springframework.binding.mapping.Result;
import org.springframework.core.style.ToStringCreator;

/**
 * The "required" error result--indicates a required mapping could not be performed because the source value to map was
 * empty.
 * @author Keith Donald
 */
public class RequiredError extends Result {

	private Object originalValue;

	/**
	 * Creates a new required error result
	 * @param originalValue the original source value (empty)
	 */
	public RequiredError(Object originalValue) {
		this.originalValue = originalValue;
	}

	public Object getOriginalValue() {
		return originalValue;
	}

	public Object getMappedValue() {
		return null;
	}

	public boolean isError() {
		return true;
	}

	public String getErrorCode() {
		return "required";
	}

	public String toString() {
		return new ToStringCreator(this).append("originalValue", originalValue).toString();
	}
}
