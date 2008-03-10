/*
 * Copyright 2004-2008 the original author or authors.
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
package org.springframework.webflow.engine.builder;

import org.springframework.binding.expression.Expression;
import org.springframework.core.io.ResourceLoader;
import org.springframework.webflow.execution.View;
import org.springframework.webflow.execution.ViewFactory;

/**
 * A factory for ViewFactory objects. This is an SPI interface and conceals specific types of view factories from the
 * flow builder infrastructure.
 */
public interface ViewFactoryCreator {

	/**
	 * Create a view factory capable of creating {@link View} objects that can render the view template with the
	 * provided identifier.
	 * @param viewIdExpression an expression that resolves the id of the view template
	 * @param viewResourceLoader an optional resource loader to use to load the view template from an input stream
	 * @return the view factory
	 */
	public ViewFactory createViewFactory(Expression viewIdExpression, ResourceLoader viewResourceLoader);

	/**
	 * Get the default id of the view to render in the provided view state by convention.
	 * @param viewStateId the view state id
	 * @return the default view id
	 */
	public String getViewIdByConvention(String viewStateId);
}
