/*
 * Copyright 2006-2007 the original author or authors.
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
package org.springframework.batch.execution.step.simple;

import org.springframework.batch.io.exception.BatchCriticalException;

/**
 * Exception indicating that the skip limit for a particular {@Step} has
 * been exceeded.
 * 
 * @author Ben Hale
 * @author Lucas Ward
 */
public class SkipLimitExceededException extends BatchCriticalException {

	private final int skipLimit;
	
	public SkipLimitExceededException(int skipLimit, Exception ex) {
		super("Skip limit of '" + skipLimit + "' exceeded", ex);
		this.skipLimit = skipLimit;
	}
	
	public int getSkipLimit() {
	    return skipLimit;
    }
}