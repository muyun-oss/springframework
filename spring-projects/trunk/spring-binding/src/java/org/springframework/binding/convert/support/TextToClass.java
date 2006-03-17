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
package org.springframework.binding.convert.support;

import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Converts a textual representation of a class object to a <code>Class</code>
 * instance.
 * @author Keith Donald
 */
public class TextToClass extends ConversionServiceAwareConverter {

	private static final String ALIAS_PREFIX = "type:";

	private static final String CLASS_PREFIX = "class:";

	public Class[] getSourceClasses() {
		return new Class[] { String.class };
	}

	public Class[] getTargetClasses() {
		return new Class[] { Class.class };
	}

	protected Object doConvert(Object source, Class targetClass, Map context) throws Exception {
		String text = (String)source;
		if (StringUtils.hasText(text)) {
			String classNameOrAlias = text.trim();
			if (classNameOrAlias.startsWith(CLASS_PREFIX)) {
				return ClassUtils.forName(text.substring(CLASS_PREFIX.length()));
			}
			else if (classNameOrAlias.startsWith(ALIAS_PREFIX)) {
				Class clazz = getConversionService().getClassByAlias(text);
				Assert.notNull(clazz, "No class found associated with type alias '" + classNameOrAlias + "'");
				return clazz;
			}
			else {
				// try first an aliased based lookup
				if (getConversionService() != null) {
					Class aliasedClass = getConversionService().getClassByAlias(text);
					if (aliasedClass != null) {
						return aliasedClass;
					}
				}
				// treat as a class name
				return ClassUtils.forName(classNameOrAlias);
			}
		}
		else {
			return null;
		}
	}
}