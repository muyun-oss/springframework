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
package org.springframework.webflow.execution.repository.continuation;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

class FlowExecutionContinuationGroup implements Serializable {

	private static final long serialVersionUID = 664189712064790043L;

	/**
	 * A map of continuations; the key is the continuation id, and the value is
	 * the {@link FlowExecutionContinuation} object.
	 */
	private Map continuations = new HashMap();

	/**
	 * A stack of conversation continuations. Each continuation represents a
	 * restorable snapshot of this conversation at a point in time relevant to
	 * the user.
	 */
	private LinkedList continuationIds = new LinkedList();

	/**
	 * The maximum number of continuations allowed in this group.
	 */
	private int maxContinuations;

	public FlowExecutionContinuationGroup(int maxContinuations) {
		this.maxContinuations = maxContinuations;
	}

	/**
	 * Returns the continuation with the provided <code>id</code>, or
	 * <code>null</code> if no such continuation exists with that id.
	 * @param id the continuation id
	 * @return the continuation
	 */
	public FlowExecutionContinuation get(Serializable id) {
		return (FlowExecutionContinuation)continuations.get(id);
	}

	public void add(Serializable continuationId, FlowExecutionContinuation continuation) {
		continuations.put(continuationId, continuation);
		continuationIds.add(continuationId);
		// remove the first continuation if them maximium number of
		// continuations has been reached
		if (maxExceeded()) {
			removeOldestContinuation();
		}
	}

	private void removeOldestContinuation() {
		continuations.remove(continuationIds.removeFirst());
	}

	private boolean maxExceeded() {
		return maxContinuations > 0 && continuationIds.size() > maxContinuations;
	}

	/**
	 * Returns the count of continuations in this repository.
	 */
	public int getContinuationCount() {
		return continuationIds.size();
	}
}