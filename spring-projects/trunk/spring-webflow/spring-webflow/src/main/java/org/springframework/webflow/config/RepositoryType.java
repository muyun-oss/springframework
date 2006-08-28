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
package org.springframework.webflow.config;

import org.springframework.core.enums.StaticLabeledEnum;

/**
 * Type-safe enumeration of logical flow execution repository types.
 * 
 * @see org.springframework.webflow.execution.repository.FlowExecutionRepository
 * 
 * @author Keith Donald
 */
public class RepositoryType extends StaticLabeledEnum {

	/**
	 * The 'default' flow execution repository type.
	 */
	public static RepositoryType DEFAULT = new RepositoryType(0, "Default");

	/**
	 * The 'continuation' flow execution repository type.
	 */
	public static RepositoryType CONTINUATION = new RepositoryType(1, "Continuation");

	/**
	 * The 'client' flow execution repository type.
	 */
	public static RepositoryType CLIENT = new RepositoryType(2, "Client");

	/**
	 * The 'singleKey' flow execution repository type.
	 */
	public static RepositoryType SINGLEKEY = new RepositoryType(3, "Single Key");

	/**
	 * Private constructor because this is a typesafe enum!
	 */
	private RepositoryType(int code, String label) {
		super(code, label);
	}
}