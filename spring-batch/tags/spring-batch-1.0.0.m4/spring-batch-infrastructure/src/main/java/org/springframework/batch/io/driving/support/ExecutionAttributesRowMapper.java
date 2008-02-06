/*
 * Copyright 2006-2007 the original author or authors.
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
package org.springframework.batch.io.driving.support;

import org.springframework.batch.item.ExecutionAttributes;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;

/**
 * {@link ExecutionAttributesRowMapper} extends the standard {@link RowMapper} interface to provide for
 * converting an object returned from a RowMapper to {@link ExecutionAttributes} and back again.  One
 * of the most common use cases for this type of functionality is the DrivingQuery approach
 * to sql processing.  Using a {@link ExecutionAttributesRowMapper}, developers can create each unique key
 * to suite their specific needs, and also describe how such a key would be converted to
 * {@link ExecutionAttributes}, so that it can be serialized and stored.
 *
 * @author Lucas Ward
 * @see RowMapper
 * @since 1.0
 */
public interface ExecutionAttributesRowMapper extends RowMapper {

	/**
	 * Given the provided composite key, return a {@link ExecutionAttributes} representation.
	 *
	 * @param key
	 * @return ExecutionAttributes representing the composite key.
	 * @throws IllegalArgumentException if key is null or of an unsupported type.
	 */
	public ExecutionAttributes createExecutionAttributes(Object key);

	/**
	 * Given the provided restart data, return a PreparedStatementSeter that can
	 * be used as parameters to a JdbcTemplate.
	 *
	 * @param executionAttributes
	 * @return an array of objects that can be used as arguments to a JdbcTemplate.
	 */
	public PreparedStatementSetter createSetter(ExecutionAttributes executionAttributes);
}