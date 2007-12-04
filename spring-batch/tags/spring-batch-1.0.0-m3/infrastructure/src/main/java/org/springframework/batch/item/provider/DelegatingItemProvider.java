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

package org.springframework.batch.item.provider;

import org.springframework.batch.item.ItemProvider;
import org.springframework.batch.support.AbstractDelegator;

/**
 * Invokes a custom method which provides an item.
 * 
 * @author Robert Kasanicky
 */
public class DelegatingItemProvider extends AbstractDelegator implements ItemProvider {
	
	/**
	 * @return return value of the target method.
	 */
    public Object next() throws Exception {
		return invokeDelegateMethod();
    }

    //harmless implementation of method required by ItemProvider interface
	public Object getKey(Object item) {
		return item;
	}

	//harmless implementation of method required by ItemProvider interface
	public boolean recover(Object data, Throwable cause) {
		return false;
	}

}

