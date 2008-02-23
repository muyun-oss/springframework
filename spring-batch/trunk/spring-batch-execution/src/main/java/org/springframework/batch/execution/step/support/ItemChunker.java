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
package org.springframework.batch.execution.step.support;

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.core.domain.Chunk;
import org.springframework.batch.core.domain.Chunker;
import org.springframework.batch.core.domain.ChunkingResult;
import org.springframework.batch.core.domain.ItemSkipPolicy;
import org.springframework.batch.core.domain.StepContribution;
import org.springframework.batch.io.exception.ReadFailureException;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.exception.MarkFailedException;
import org.springframework.batch.item.exception.ResetFailedException;
import org.springframework.batch.item.exception.StreamException;
import org.springframework.util.Assert;

/**
 * Implementation of the {@link Chunker} interface that creates chunks from
 * an {@link ItemReader}.  <strong>It does not buffer chunks</strong>.  If 
 * the underlying reader has been rolled back, and
 * 
 * @author Ben Hale
 * @author Lucas Ward
 */
public class ItemChunker implements Chunker {

	private final ItemReader itemReader;
	private long chunkCounter = 0;

	private ItemSkipPolicy itemSkipPolicy = new NeverSkipItemSkipPolicy();

	public ItemChunker(ItemReader itemReader) {
		Assert.notNull(itemReader, "ItemReader must not be null");
		this.itemReader = itemReader;
	}

	public void setItemSkipPolicy(ItemSkipPolicy itemSkipPolicy) {
		this.itemSkipPolicy = itemSkipPolicy;
	}

	public ChunkingResult chunk(int size, StepContribution stepContribution) throws ReadFailureException {
		Assert.isTrue(size > 0, "Chunk size must be greater than 0");

		int counter = 0;
		List items = new ArrayList(size);
		List exceptions = new ArrayList();

		Object item;
		while (counter < size) {
			try {
				item = itemReader.read();
				if (item == null) {
					break;
				}
				items.add(item);
				counter++;
			} catch (Exception ex) {
				exceptions.add(ex);
				if(!itemSkipPolicy.shouldSkip(ex, stepContribution)){
					rethrow(ex);
				}
			}
		}

		if (items.size() == 0) {
			return null;
		}

		return new ChunkingResult(new Chunk(getChunkId(), items), exceptions);
	}
	
	private void rethrow(Exception ex){
		if(ex instanceof RuntimeException){
			throw (RuntimeException)ex;
		}
		else{
			throw new ReadFailureException("Error encountered while reading", ex);
		}
	}

	private synchronized Long getChunkId() {
		return new Long(chunkCounter++);
	}

	//These methods are temporary hacks until something can be done 
	//about the ItemStream interface.
	public void close() throws StreamException {
		if(itemReader instanceof ItemStream){
			((ItemStream)itemReader).close();
		}
	}

	public boolean isMarkSupported() {
		if(itemReader instanceof ItemStream){
			return ((ItemStream)itemReader).isMarkSupported();
		}
		
		return false;
	}

	public void mark() throws MarkFailedException {
		if(itemReader instanceof ItemStream){
			((ItemStream)itemReader).mark();
		}
	}

	public void open() throws StreamException {
		if(itemReader instanceof ItemStream){
			((ItemStream)itemReader).open();
		}
	}

	public void reset() throws ResetFailedException {
		if(itemReader instanceof ItemStream){
			((ItemStream)itemReader).reset();
		}
	}

	public void restoreFrom(ExecutionContext context) {
		if(itemReader instanceof ItemStream){
			((ItemStream)itemReader).restoreFrom(context);
		}
	}

	public ExecutionContext getExecutionContext() {
		if(itemReader instanceof ItemStream){
			return ((ItemStream)itemReader).getExecutionContext();
		}
		
		return new ExecutionContext();
	}

}