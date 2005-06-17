/*
 * Copyright 2002-2005 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.springframework.binding.format.support;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;

import org.springframework.binding.format.Formatter;
import org.springframework.binding.format.Style;

/**
 * FormatterLocator that caches Formatters in thread-local storage.
 * 
 * @author Keith Donald
 */
public class SimpleFormatterLocator extends AbstractFormatterLocator {

	public SimpleFormatterLocator() {
	}

	public Formatter getDateFormatter(Style style) {
		return new DateFormatter(SimpleDateFormat.getDateInstance(style.getShortCode(), getLocale()));
	}

	public Formatter getDateTimeFormatter(Style dateStyle, Style timeStyle) {
		return new DateFormatter(SimpleDateFormat.getDateTimeInstance(dateStyle.getShortCode(), timeStyle
				.getShortCode(), getLocale()));
	}

	public Formatter getTimeFormatter(Style style) {
		return new DateFormatter(SimpleDateFormat.getTimeInstance(style.getShortCode(), getLocale()));
	}

	public Formatter getNumberFormatter(Class numberClass) {
		return new NumberFormatter(NumberFormat.getNumberInstance(getLocale()));
	}
}