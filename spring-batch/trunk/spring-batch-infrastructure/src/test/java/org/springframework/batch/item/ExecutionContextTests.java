/*
 * Copyright 2006-2008 the original author or authors.
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

import junit.framework.TestCase;

/**
 * @author Lucas Ward
 *
 */
public class ExecutionContextTests extends TestCase{

	ExecutionContext context;
	
	protected void setUp() throws Exception {
		super.setUp();
		
		context = new ExecutionContext();
	}
	
	public void testNormalUsage(){
		
		context.putString("1", "testString1");
		context.putString("2", "testString2");
		context.putLong("3", 3);
		context.putDouble("4", 4.4);
		
		assertEquals("testString1", context.getString("1"));
		assertEquals("testString2", context.getString("2"));
		assertEquals(4.4, context.getDouble("4"), 0);
		assertEquals(3, context.getLong("3"));
	}
	
	public void testInvalidCast(){
		
		context.putLong("1", 1);
		try{
			context.getDouble("1");
			fail();
		}
		catch(ClassCastException ex){
			//expected
		}
	}
	
	public void testIsEmpty(){
		assertTrue(context.isEmpty());
		context.putString("1", "test");
		assertFalse(context.isEmpty());
	}
	
	public void testDirtyFlag(){
		assertFalse(context.isDirty());
		context.putString("1", "test");
		assertTrue(context.isDirty());
		context.clearDirtyFlag();
		assertFalse(context.isDirty());
	}
	
	public void testContains(){
		context.putString("1", "testString");
		assertTrue(context.containsKey("1"));
		assertTrue(context.containsValue("testString"));
	}
	
	public void testEquals(){
		context.putString("1", "testString");
		ExecutionContext tempContext = new ExecutionContext();
		assertFalse(tempContext.equals(context));
		tempContext.putString("1", "testString");
		assertTrue(tempContext.equals(context));
	}
	
	public void testSerializationCheck(){
		//adding a non serializable object should cause an error.
		try{
			context.put("1", new Object());
			fail();
		}
		catch(IllegalArgumentException ex){
			//expected
		}
	}
	
}