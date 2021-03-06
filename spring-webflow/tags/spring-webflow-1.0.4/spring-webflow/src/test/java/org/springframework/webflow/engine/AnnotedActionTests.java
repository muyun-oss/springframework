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
package org.springframework.webflow.engine;

import junit.framework.TestCase;

import org.springframework.webflow.action.AbstractAction;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.springframework.webflow.execution.TestAction;
import org.springframework.webflow.test.MockRequestContext;

public class AnnotedActionTests extends TestCase {

	private AnnotatedAction action = new AnnotatedAction(new TestAction());

	private MockRequestContext context = new MockRequestContext();

	protected void setUp() throws Exception {
	}

	public void testBasicExecute() throws Exception {
		assertEquals("success", action.execute(context).getId());
	}

	public void testExecuteWithCustomAttribute() throws Exception {
		action.getAttributeMap().put("attr", "value");
		action.setTargetAction(new AbstractAction() {
			protected Event doExecute(RequestContext context) throws Exception {
				assertEquals("value", context.getAttributes().getString("attr"));
				return success();
			}
		});
		assertEquals("success", action.execute(context).getId());
	}

	public void testExecuteWithName() throws Exception {
		action.getAttributeMap().put("name", "foo");
		action.setTargetAction(new AbstractAction() {
			protected Event doExecute(RequestContext context) throws Exception {
				assertEquals("foo", context.getAttributes().getString("name"));
				return success();
			}
		});
		assertEquals("foo.success", action.execute(context).getId());
	}
}