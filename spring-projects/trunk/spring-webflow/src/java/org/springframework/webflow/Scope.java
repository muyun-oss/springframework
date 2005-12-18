/*
 * Copyright 2002-2005 the original author or authors.
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
package org.springframework.webflow;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.core.style.StylerUtils;
import org.springframework.core.style.ToStringCreator;
import org.springframework.util.Assert;

/**
 * Holder for data placed in a specific scope, for example "request scope" or
 * "flow scope". Clients should invoke operations on this class to access
 * attributes placed in a specific scope by <code>attributeName</code>.
 * <p>
 * This class is simply a thin wrapper around a <code>java.util.HashMap</code>.
 * <p>
 * Usage example:
 * 
 * <pre>
 * context.getFlowScope().getAttribute(&quot;foo&quot;);
 * context.getFlowScope().setAttribute(&quot;foo&quot;, &quot;bar&quot;);
 * </pre>
 * 
 * @author Keith Donald
 * @author Erwin Vervaet
 */
public class Scope implements Map, Serializable {

	/**
	 * Serialization id.
	 */
	private static final long serialVersionUID = -8075142903027393405L;

	/**
	 * The scope type; e.g FLOW or REQUEST.
	 */
	private ScopeType scopeType;

	/**
	 * The data holder map.
	 */
	private Map attributes;

	/**
	 * Creates a 'scoped' attribute map.
	 * @param scopeType the scope type
	 */
	public Scope(ScopeType scopeType) {
		this.attributes = new HashMap();
		this.scopeType = scopeType;
	}

	/**
	 * Creates a 'scoped' attribute map.
	 * @param size the initial map size
	 * @param scopeType the scope type
	 */
	public Scope(int size, ScopeType scopeType) {
		this.attributes = new HashMap(size);
		this.scopeType = scopeType;
	}

	/**
	 * Returns this scope's scope type.
	 */
	public ScopeType getScopeType() {
		return scopeType;
	}

	public boolean containsAttribute(String attributeName) {
		return attributes.containsKey(attributeName);
	}

	/**
	 * Does the attribute with the provided name exist in this scope, and is its
	 * value of the specified class?
	 * @param attributeName the attribute name
	 * @param requiredType the required class of the attribute value
	 * @return true if so, false otherwise
	 */
	public boolean containsAttribute(String attributeName, Class requiredType) {
		if (containsAttribute(attributeName)) {
			Assert.notNull(requiredType, "The required type to assert is required");
			return requiredType.isInstance(attributes.get(attributeName));
		}
		else {
			return false;
		}
	}

	public Object getAttribute(String attributeName) {
		return attributes.get(attributeName);
	}

	/**
	 * Get an attribute value and make sure it is of the required type.
	 * @param attributeName name of the attribute to get
	 * @param requiredType the required type of the attribute value
	 * @return the attribute value, or null if not found
	 * @throws IllegalStatetException when the value is not of the required type
	 */
	public Object getAttribute(String attributeName, Class requiredType) throws IllegalStateException {
		Object value = getAttribute(attributeName);
		if (value != null) {
			Assert.notNull(requiredType, "The required type to assert is required");
			Assert.isInstanceOf(requiredType, value);
		}
		return value;
	}

	/**
	 * Get the value of a required attribute.
	 * @param attributeName name of the attribute to get
	 * @return the attribute value
	 * @throws IllegalStateException when the attribute is not found
	 */
	public Object getRequiredAttribute(String attributeName) throws IllegalStateException {
		Object value = getAttribute(attributeName);
		if (value == null) {
			throw new IllegalStateException("Required attribute '" + attributeName + "' is not present in " + this
					+ "; attributes present are = " + StylerUtils.style(getAttributeMap()));
		}
		return value;
	}

	/**
	 * Get the value of a required attribute and make sure it is of the required
	 * type.
	 * @param attributeName name of the attribute to get
	 * @param requiredType the required type of the attribute value
	 * @return the attribute value
	 * @throws IllegalStateException when the attribute is not found or not of
	 * the required type
	 */
	public Object getRequiredAttribute(String attributeName, Class requiredType) throws IllegalStateException {
		Assert.notNull(requiredType, "The required type to assert is required");
		Object value = getRequiredAttribute(attributeName);
		Assert.isInstanceOf(requiredType, value);
		return value;
	}

	/**
	 * Gets the value of the specified <code>attributeName</code>, if such an
	 * attribute exists in this scope. If the attribute does not exist, a new
	 * instance will be created of the type <code>attributeClass</code>,
	 * which will be set in this scope and returned.
	 * @param attributeName the attribute name
	 * @param attributeClass the attribute class
	 * @return the value
	 * @throws IllegalStateException when the attribute is not of the required
	 * type
	 * @throws BeansException if the attribute could not be created
	 */
	public Object getOrCreateAttribute(String attributeName, Class attributeClass) throws IllegalStateException,
			BeansException {
		if (!containsAttribute(attributeName)) {
			setAttribute(attributeName, BeanUtils.instantiateClass(attributeClass));
		}
		return getAttribute(attributeName, attributeClass);
	}

	/**
	 * Assert that the attribute is contained in this scope.
	 * @param attributeName the attribute
	 * @throws IllegalStateException the assertion failed; the attribute is not
	 * present
	 */
	public void assertAttributePresent(String attributeName) throws IllegalStateException {
		getRequiredAttribute(attributeName);
	}

	/**
	 * Returns the contents of this scope as an unmodifiable map.
	 */
	public Map getAttributeMap() {
		return Collections.unmodifiableMap(this.attributes);
	}

	public Object setAttribute(String attributeName, Object attributeValue) {
		return attributes.put(attributeName, attributeValue);
	}

	/**
	 * Set all given attributes in this scope.
	 */
	public void setAttributes(Map attributes) {
		Iterator it = attributes.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry entry = (Map.Entry)it.next();
			Assert.isInstanceOf(String.class, entry.getKey());
			setAttribute((String)entry.getKey(), entry.getValue());
		}
	}

	/**
	 * Remove an attribute from this scope.
	 * @param attributeName the name of the attribute to remove
	 * @return previous value associated with specified attribute name, or
	 * <tt>null</tt> if there was no mapping for the name
	 */
	public Object removeAttribute(String attributeName) {
		return attributes.remove(attributeName);
	}

	// implementing Map

	public int size() {
		return attributes.size();
	}

	public boolean isEmpty() {
		return attributes.isEmpty();
	}

	public boolean containsKey(Object key) {
		return attributes.containsKey(key);
	}

	public boolean containsValue(Object value) {
		return attributes.containsValue(value);
	}

	public Object get(Object key) {
		return attributes.get(key);
	}

	public Object put(Object key, Object value) {
		return attributes.put(key, value);
	}

	public Object remove(Object key) {
		return removeAttribute(String.valueOf(key));
	}

	public void putAll(Map t) {
		attributes.putAll(t);
	}

	public void clear() {
		attributes.clear();
	}

	public Set keySet() {
		return attributes.keySet();
	}

	public Collection values() {
		return attributes.values();
	}

	public Set entrySet() {
		return attributes.entrySet();
	}

	public String toString() {
		return new ToStringCreator(this).append("scopeType", scopeType).append("attributes", attributes).toString();
	}
}