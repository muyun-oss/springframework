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

package org.springframework.batch.item.file.mapping;

/**
 * Interface that is used to map data obtained from a {@link FieldSet} into an
 * object.
 * 
 * @author Tomas Slanina
 * @author Dave Syer
 * 
 */
public interface FieldSetMapper {

	/**
	 * Method used to map data obtained from a {@link FieldSet} into an object.
	 * 
	 * @param fs the {@link FieldSet} to map
	 * @param lineNum the current line number (if known), or negative if not
	 */
	public Object mapLine(FieldSet fs, int lineNum);
}
