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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import org.springframework.webflow.execution.FlowExecution;

/**
 * A factory that creates new instances of flow execution continuations based on
 * standard java serialization.
 * 
 * @author Keith Donald
 */
public class SerializedFlowExecutionContinuationFactory implements FlowExecutionContinuationFactory {

	/**
	 * Flag to toggle continuation compression; compression is on by default.
	 */
	private boolean compress = true;

	/**
	 * Returns whether or not the continuations should be compressed.
	 */
	public boolean getCompress() {
		return compress;
	}

	/**
	 * Set whether or not the continuations should be compressed.
	 */
	public void setCompress(boolean compress) {
		this.compress = compress;
	}

	public FlowExecutionContinuation createContinuation(FlowExecution flowExecution) throws ContinuationCreationException {
	    return new SerializedFlowExecutionContinuation(flowExecution, compress);      
	}
	
    public FlowExecutionContinuation createContinuation(byte[] bytes) throws ContinuationUnmarshalException {
        try {
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
            try {
                return (FlowExecutionContinuation)ois.readObject();
            }
            finally {
                ois.close();
            }
        }
        catch (IOException e) {
            throw new ContinuationUnmarshalException("This should not happen", e);
        }
        catch (ClassNotFoundException e) {
            throw new ContinuationUnmarshalException("This should not happen", e);
        }
    }    
}