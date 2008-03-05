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

import java.util.List;

import org.springframework.batch.io.driving.KeyGenerator;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ExecutionContextUserSupport;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * <p>
 * Jdbc {@link KeyGenerator} implementation that only works for a single column
 * key. A sql query must be passed in which will be used to return a list of
 * keys. Each key will be mapped by a {@link RowMapper} that returns a mapped
 * key. By default, the {@link SingleColumnRowMapper} is used, and will convert
 * keys into well known types at runtime. It is extremely important to note that
 * only one column should be mapped to an object and returned as a key. If
 * multiple columns are returned as a key in this strategy, then restart will
 * not function properly. Instead a strategy that supports keys comprised of
 * multiple columns should be used.
 * 
 * <p>
 * Restartability: Because the key is only one column, restart is made much more
 * simple. Before each commit, the last processed key is returned to be stored
 * as restart data. Upon restart, that same key is given back to restore from,
 * using a separate 'RestartQuery'. This means that only the keys remaining to
 * be processed are returned, rather than returning the original list of keys
 * and iterating forward to that last committed point.
 * </p>
 * 
 * @author Lucas Ward
 * @since 1.0
 */
public class SingleColumnJdbcKeyGenerator extends ExecutionContextUserSupport implements KeyGenerator {

	private static final String RESTART_KEY = "key";

	private JdbcTemplate jdbcTemplate;

	private String sql;

	private String restartSql;

	private RowMapper keyMapper = new SingleColumnRowMapper();

	public SingleColumnJdbcKeyGenerator() {
		setName(SingleColumnJdbcKeyGenerator.class.getSimpleName());
	}

	/**
	 * Constructs a new instance using the provided jdbcTemplate and string
	 * representing the sql statement that should be used to retrieve keys.
	 * 
	 * @param jdbcTemplate
	 * @param sql
	 * @throws IllegalArgumentException if jdbcTemplate is null.
	 * @throws IllegalArgumentException if sql string is empty or null.
	 */
	public SingleColumnJdbcKeyGenerator(JdbcTemplate jdbcTemplate, String sql) {
		this();
		Assert.notNull(jdbcTemplate, "JdbcTemplate must not be null.");
		Assert.hasText(sql, "The sql statement must not be null or empty.");
		this.jdbcTemplate = jdbcTemplate;
		this.sql = sql;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.batch.io.driving.KeyGenerationStrategy#retrieveKeys()
	 */
	public List retrieveKeys(ExecutionContext executionContext) {
		
		Assert.notNull(executionContext, "The restart data must not be null.");

		if (executionContext.containsKey(RESTART_KEY)) {
			Assert.state(StringUtils.hasText(restartSql), "The RestartQuery must not be null or empty"
					+ " in order to restart.");
			return jdbcTemplate.query(restartSql, new Object[] { executionContext.getString(RESTART_KEY) }, keyMapper);
		}
		else{
			return jdbcTemplate.query(sql, keyMapper);
		}
	}

	/**
	 * Get the restart data representing the last processed key.
	 * 
	 * @see KeyGenerator#saveState(Object)
	 * @throws IllegalArgumentException if key is null.
	 */
	public void saveState(Object key, ExecutionContext executionContext) {
		Assert.notNull(key, "The key must not be null.");
		executionContext.putString(RESTART_KEY, key.toString());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(jdbcTemplate, "JdbcTemplate must not be null.");
		Assert.hasText(sql, "The DrivingQuery must not be null or empty.");
	}

	/**
	 * Set the {@link RowMapper} to be used to map each key to an object.
	 * 
	 * @param keyMapper
	 */
	public void setKeyMapper(RowMapper keyMapper) {
		this.keyMapper = keyMapper;
	}

	/**
	 * Set the SQL query to be used to return the remaining keys to be
	 * processed.
	 * 
	 * @param restartSql
	 */
	public void setRestartSql(String restartSql) {
		this.restartSql = restartSql;
	}

	/**
	 * Set the SQL statement to be used to return the keys to be processed.
	 * 
	 * @param sql
	 */
	public void setSql(String sql) {
		this.sql = sql;
	}

}
