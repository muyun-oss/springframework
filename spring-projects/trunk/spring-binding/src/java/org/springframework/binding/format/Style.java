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
package org.springframework.binding.format;

import org.springframework.core.enums.ShortCodedLabeledEnum;

/**
 * Format styles.
 * @author Keith Donald
 */
public class Style extends ShortCodedLabeledEnum {
	public static final Style FULL = new Style(0, "Full");

	public static final Style LONG = new Style(1, "Long");

	public static final Style MEDIUM = new Style(2, "Medium");

	public static final Style SHORT = new Style(3, "Short");

	private Style(int code, String label) {
		super(code, label);
	}
}