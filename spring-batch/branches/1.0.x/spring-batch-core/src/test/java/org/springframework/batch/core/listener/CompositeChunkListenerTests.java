/*
 * Copyright 2006-2008 the original author or authors.
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
package org.springframework.batch.core.listener;

import org.easymock.MockControl;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.listener.CompositeChunkListener;

import junit.framework.TestCase;

/**
 * @author Lucas Ward
 *
 */
public class CompositeChunkListenerTests extends TestCase {

	MockControl listenerControl = MockControl.createControl(ChunkListener.class);
	
	ChunkListener listener;
	CompositeChunkListener compositeListener;
	
	protected void setUp() throws Exception {
		super.setUp();
	
		listener = (ChunkListener)listenerControl.getMock();
		compositeListener = new CompositeChunkListener();
		compositeListener.register(listener);
	}
	
	public void testBeforeChunk(){
		
		listener.beforeChunk();
		listenerControl.replay();
		compositeListener.beforeChunk();
		listenerControl.verify();
	}
	
	public void testAfterChunk(){
		
		listener.afterChunk();
		listenerControl.replay();
		compositeListener.afterChunk();
		listenerControl.verify();
	}
}
