/*
 * Copyright 2002-2004 the original author or authors.
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
package org.springframework.webflow.action;

import java.util.HashMap;
import java.util.Map;

import org.springframework.webflow.Event;
import org.springframework.webflow.test.MockRequestContext;

import junit.framework.TestCase;

/**
 * Unit test for the AttributeMapperAction.
 * 
 * @see org.springframework.webflow.action.AttributeMapperAction
 * 
 * @author Erwin Vervaet
 */
public class AttributeMapperActionTests extends TestCase {

	public void testMapping() throws Exception {
		AttributeMapperAction ama = new AttributeMapperAction();
		ama.setSourceExpression("${sourceEvent.parameters.foo}");
		ama.setTargetExpression("${flowScope.bar}");
		ama.initAction();
		
		MockRequestContext context = new MockRequestContext();
		Map params = new HashMap(1);
		params.put("foo", "value");
		Event sourceEvent = new Event(this, "test", params);
		context.setSourceEvent(sourceEvent);
		
		assertTrue(context.getFlowScope().isEmpty());
		
		ama.execute(context);
		
		assertEquals(1, context.getFlowScope().size());
		assertEquals("value", context.getFlowScope().getAttribute("bar"));
	}
}
