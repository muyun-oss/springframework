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
package org.springframework.batch.item.database;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ExecutionContextUserSupport;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * {@link ItemReader} for reading database records built on top of Hibernate.
 * 
 * It executes the HQL {@link #setQueryString(String)} when initialized and
 * iterates over the result set as {@link #read()} method is called, returning
 * an object corresponding to current row.
 * 
 * Input source can be configured to use either {@link StatelessSession}
 * sufficient for simple mappings without the need to cascade to associated
 * objects or standard hibernate {@link Session} for more advanced mappings or
 * when caching is desired.
 * 
 * When stateful session is used it will be cleared after successful commit
 * without being flushed (no inserts or updates are expected).
 * 
 * @author Robert Kasanicky
 * @author Dave Syer
 */
public class HibernateCursorItemReader extends ExecutionContextUserSupport implements ItemReader, ItemStream,
		InitializingBean {

	private static final String RESTART_DATA_ROW_NUMBER_KEY = "row.number";

	private SessionFactory sessionFactory;

	private StatelessSession statelessSession;

	private Session statefulSession;

	private ScrollableResults cursor;

	private String queryString;

	private boolean useStatelessSession = true;

	private int lastCommitRowNumber = 0;

	/* Current count of processed records. */
	private int currentProcessedRow = 0;

	private boolean initialized = false;

	private boolean saveState = false;

	private boolean shouldReadBuffer = false;

	private ListIterator itemBufferIterator = null;

	private List itemBuffer = new ArrayList();
	
	private int lastMarkedBufferIndex = 0;

	public HibernateCursorItemReader() {
		setName(HibernateCursorItemReader.class.getSimpleName());
	}

	public Object read() {

		currentProcessedRow++;

		if (shouldReadBuffer) {
			if (itemBufferIterator.hasNext()) {
				return itemBufferIterator.next();
			}
			else {
				// buffer is exhausted, continue reading from file
				shouldReadBuffer = false;
				itemBufferIterator = null;
			}
		}

		if (cursor.next()) {
			Object[] data = cursor.get();
			Object item;

			if (data.length > 1) {
				item = data;
			}
			else {
				item = data[0];
			}

			itemBuffer.add(item);
			return item;
		}
		return null;
	}

	/**
	 * Closes the result set cursor and hibernate session.
	 */
	public void close(ExecutionContext executionContext) {
		initialized = false;
		if (cursor != null) {
			cursor.close();
		}
		currentProcessedRow = 0;
		if (useStatelessSession) {
			if (statelessSession != null) {
				statelessSession.close();
			}
		}
		else {
			if (statefulSession != null) {
				statefulSession.close();
			}
		}
	}

	/**
	 * Creates cursor for the query.
	 */
	public void open(ExecutionContext executionContext) {
		Assert.state(!initialized, "Cannot open an already opened ItemReader, call close first");

		if (useStatelessSession) {
			statelessSession = sessionFactory.openStatelessSession();
			cursor = statelessSession.createQuery(queryString).scroll(ScrollMode.FORWARD_ONLY);
		}
		else {
			statefulSession = sessionFactory.openSession();
			cursor = statefulSession.createQuery(queryString).scroll(ScrollMode.FORWARD_ONLY);
		}
		initialized = true;

		if (executionContext.containsKey(getKey(RESTART_DATA_ROW_NUMBER_KEY))) {
			currentProcessedRow = Integer.parseInt(executionContext.getString(getKey(RESTART_DATA_ROW_NUMBER_KEY)));
			for (int i = 0; i < currentProcessedRow; i++) {
				cursor.next();
			}
		}

	}

	/**
	 * @param sessionFactory hibernate session factory
	 */
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(sessionFactory);
		Assert.hasLength(queryString);
	}

	/**
	 * @param queryString HQL query string
	 */
	public void setQueryString(String queryString) {
		this.queryString = queryString;
	}

	/**
	 * Can be set only in uninitialized state.
	 * 
	 * @param useStatelessSession <code>true</code> to use
	 * {@link StatelessSession} <code>false</code> to use standard hibernate
	 * {@link Session}
	 */
	public void setUseStatelessSession(boolean useStatelessSession) {
		Assert.state(!initialized);
		this.useStatelessSession = useStatelessSession;
	}

	/**
	 */
	public void update(ExecutionContext executionContext) {
		if (saveState) {
			Assert.notNull(executionContext, "ExecutionContext must not be null");
			executionContext.putString(getKey(RESTART_DATA_ROW_NUMBER_KEY), "" + currentProcessedRow);
		}
	}

	/**
	 * Mark is supported as long as this {@link ItemStream} is used in a
	 * single-threaded environment. The state backing the mark is a single
	 * counter, keeping track of the current position, so multiple threads
	 * cannot be accommodated.
	 * 
	 * @see org.springframework.batch.item.ItemReader#mark()
	 */
	public void mark() {

		if (!shouldReadBuffer) {
			itemBuffer.clear();
			itemBufferIterator = null;
			lastMarkedBufferIndex = 0;
		}
		else {
			lastMarkedBufferIndex = itemBufferIterator.nextIndex();
		}
		
		if (!useStatelessSession) {
			statefulSession.clear();
		}
		lastCommitRowNumber = currentProcessedRow;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.item.stream.ItemStreamAdapter#reset(org.springframework.batch.item.ExecutionContext)
	 */
	public void reset() {
		currentProcessedRow = lastCommitRowNumber;
		shouldReadBuffer = true;
		itemBufferIterator = itemBuffer.listIterator(lastMarkedBufferIndex);
	}

	public void setSaveState(boolean saveState) {
		this.saveState = saveState;
	}
}
