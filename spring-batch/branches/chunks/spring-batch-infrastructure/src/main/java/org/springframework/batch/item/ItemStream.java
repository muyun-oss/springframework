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

package org.springframework.batch.item;

import org.springframework.batch.item.exception.StreamException;

/**
 * <p>
 * Marker interface defining a contract for periodically storing state and
 * restoring from that state should an error occur.
 * <p>
 * 
 * <p>
 * The state that is stored is represented as {@link ExecutionContext} which
 * enforces a requirement that any restart data can be represented by a
 * Properties object. In general, the contract is that
 * {@link ExecutionContext} that is returned via the
 * {@link #getExecutionContext()} method will be given back to the
 * {@link #restoreFrom(ExecutionContext)} method, exactly as it was provided.
 * </p>
 * 
 * @author Dave Syer
 * 
 */
public interface ItemStream extends ExecutionContextProvider {

	/**
	 * Restore to the state given the provided {@link ExecutionContext}.
	 * This can be used to restart after a failure - hence not normally used
	 * more than once per call to {@link #open()}.
	 * 
	 * @param context
	 */
	void restoreFrom(ExecutionContext context);

	/**
	 * If any resources are needed for the stream to operate they need to be
	 * initialised here.
	 */
	void open() throws StreamException;

	/**
	 * If any resources are needed for the stream to operate they need to be
	 * destroyed here. Once this method has been called all other methods
	 * (except open) may throw an exception.
	 */
	void close() throws StreamException;
}