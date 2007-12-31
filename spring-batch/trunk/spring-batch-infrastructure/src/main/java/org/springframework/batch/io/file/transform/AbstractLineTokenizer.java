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

package org.springframework.batch.io.file.transform;

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.io.file.mapping.FieldSet;


/**
 * @author Dave Syer
 * @author Robert Kasanicky
 *
 */
public abstract class AbstractLineTokenizer implements LineTokenizer {

	protected String[] names = new String[0];

	/**
	 * Setter for column names. Optional, but if set, then all lines must have
	 * as many or fewer tokens.
	 * 
	 * @param names
	 */
	public void setNames(String[] names) {
		this.names = names;
	}
	
	/**
	 * @return <code>true</code> if column names have been specified
	 * @see #setNames(String[])
	 */
	public boolean hasNames() {
		if (names != null && names.length > 0) {
			return true;
		}
		return false;
	}

	/**
	 * Yields the tokens resulting from the splitting of the supplied
	 * <code>line</code>.
	 * 
	 * @param line the line to be tokenised (can be <code>null</code>)
	 * 
	 * @return the resulting tokens
	 */
	public FieldSet tokenize(String line) {

		if (line == null || line.length()==0) {
			return new FieldSet(new String[0]);
		}

		List tokens = new ArrayList(doTokenize(line));
		for (int i=tokens.size(); i<names.length; i++) {
			tokens.add(null);
		}

		String[] values = (String[]) tokens.toArray(new String[tokens.size()]);
		if (names.length==0) {
			return new FieldSet(values);
		}
		return new FieldSet(values, names);
	}
	
	protected abstract List doTokenize(String line);

}
