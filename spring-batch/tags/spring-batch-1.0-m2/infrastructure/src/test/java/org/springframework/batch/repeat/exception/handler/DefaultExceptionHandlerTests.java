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

import java.util.Collections;

import junit.framework.TestCase;

import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.exception.RepeatException;

public class DefaultExceptionHandlerTests extends TestCase {

	private DefaultExceptionHandler handler = new DefaultExceptionHandler();
	private RepeatContext context = null;
	
	public void testRuntimeException() throws Exception {
		try {
			handler.handleExceptions(context, Collections.singleton(new RuntimeException("Foo")));
			fail("Expected RuntimeException");
		} catch (RuntimeException e) {
			assertEquals("Foo", e.getMessage());
		}
	}

	public void testError() throws Exception {
		try {
			handler.handleExceptions(context, Collections.singleton(new Error("Foo")));
			fail("Expected BatchException");
		} catch (RepeatException e) {
			assertEquals("Foo", e.getCause().getMessage());
		}
	}
}
