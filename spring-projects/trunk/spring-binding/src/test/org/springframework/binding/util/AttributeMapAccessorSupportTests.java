package org.springframework.binding.util;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.springframework.binding.map.MapAccessor;

public class AttributeMapAccessorSupportTests extends TestCase {
	private MapAccessor tested;
	
	public void setUp() {
		Map attributes = new HashMap();
		attributes.put("boolean", Boolean.TRUE);
		tested = new MapAccessor(attributes);
	}
	
	public void testGetBoolean() {
		boolean result = tested.getBoolean("boolean", Boolean.FALSE).booleanValue();
		assertEquals(true, result);
	}
}
