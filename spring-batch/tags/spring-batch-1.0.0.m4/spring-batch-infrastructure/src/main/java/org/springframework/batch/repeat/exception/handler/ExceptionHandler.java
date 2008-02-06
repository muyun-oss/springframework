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

package org.springframework.batch.repeat.exception.handler;

import org.springframework.batch.repeat.CompletionPolicy;
import org.springframework.batch.repeat.RepeatContext;

/**
 * Handler to allow strategies for rethrowing exceptions. Normally a
 * {@link CompletionPolicy} will be used to decide whether to end a batch when
 * there is no exception, and the {@link ExceptionHandler} is used to signal an
 * abnormal ending - an abnormal ending would result in an
 * {@link ExceptionHandler} throwing an exception. The caller will catch and
 * rethrow it if necessary.
 * 
 * @author Dave Syer
 * 
 */
public interface ExceptionHandler {

	/**
	 * Deal with a Throwable during a batch. The input might be
	 * RuntimeException or other unchecked exceptions.
	 * 
	 * @param context
	 *            the current {@link RepeatContext}. Can be used to store state
	 *            (via attributes), for example to count the number of
	 *            occurrences of a particular exception type and implement a
	 *            threshold policy.
	 * @param throwable
	 *            an exception.
	 * @throws RuntimeException
	 *             implementations must wrap and rethrow other Throwables
	 *             appropriately.
	 */
	void handleException(RepeatContext context, Throwable throwable)
			throws RuntimeException;

}