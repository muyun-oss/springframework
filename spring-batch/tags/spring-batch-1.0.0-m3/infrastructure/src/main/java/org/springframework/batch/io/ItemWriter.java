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

package org.springframework.batch.io;

/**
 * Basic interface for generic output operations. Class implementing this
 * interface will be responsible for serializing objects. Generally, it is
 * responsibility of implementing class to decide which technology to use for
 * mapping and how it should be configured.
 * 
 * @author Dave Syer
 */
public interface ItemWriter {

	/**
	 * Writes provided object to an output stream or similar.
	 * 
	 * @param item
	 *            the object to write.
	 */
	public void write(Object item);
}
