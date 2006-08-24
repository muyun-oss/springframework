/*
 * Copyright 2002-2006 the original author or authors.
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
package org.springframework.webflow.core.collection;

import java.util.EventObject;

/**
 * Holder for information about the binding or unbinding event in an
 * <code>AttributeMap</code>.
 * 
 * @author Ben Hale
 * @see AttributeMap
 */
public class AttributeMapBindingEvent extends EventObject {

	private String attributeName;

	private Object attributeValue;

	/**
	 * Creates an event for map binding that contains information about the
	 * event
	 * @param source The source map that this attribute was bound in
	 * @param attributeName The name that this attribute was bound with
	 * @param attributeValue The attribute
	 */
	public AttributeMapBindingEvent(AttributeMap source, String attributeName, Object attributeValue) {
		super(source);
		this.source = source;
		this.attributeName = attributeName;
		this.attributeValue = attributeValue;
	}

	public String getAttributeName() {
		return attributeName;
	}

	public Object getAttributeValue() {
		return attributeValue;
	}
}