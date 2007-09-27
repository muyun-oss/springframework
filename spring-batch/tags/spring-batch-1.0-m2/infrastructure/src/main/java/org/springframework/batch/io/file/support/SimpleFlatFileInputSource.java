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

package org.springframework.batch.io.file.support;

import java.io.IOException;

import org.springframework.batch.io.InputSource;
import org.springframework.batch.io.exception.ValidationException;
import org.springframework.batch.io.file.FieldSet;
import org.springframework.batch.io.file.FieldSetInputSource;
import org.springframework.batch.io.file.support.separator.RecordSeparatorPolicy;
import org.springframework.batch.io.file.support.transform.DelimitedLineTokenizer;
import org.springframework.batch.io.file.support.transform.LineTokenizer;
import org.springframework.batch.item.ResourceLifecycle;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * This class represents a basic input source, that reads data from the file and
 * returns it as structured tuples in the form of{@link FieldSet} instances.
 * The location of the file is defined by the resource property. To separate the
 * structure of the file, {@link LineTokenizer} is used to parse data obtained
 * from the file. <br/>
 * 
 * A {@link SimpleFlatFileInputSource} is not thread safe because it maintains
 * state in the form of a {@link ResourceLineReader}. Be careful to configure a
 * {@link SimpleFlatFileInputSource} using an appropiate factory or scope so
 * that it is not shared between threads.<br/>
 * 
 * @see FieldSetInputSource
 * 
 * @author Dave Syer
 */
public class SimpleFlatFileInputSource implements FieldSetInputSource, InitializingBean, DisposableBean {

	// default encoding for input files - set to ISO-8859-1
	public static final String DEFAULT_CHARSET = "ISO-8859-1";

	private Resource resource;

	/**
	 * Encapsulates the state of the input source. If it is null then we are
	 * uninitialized.
	 */
	private ResourceLineReader reader;

	private RecordSeparatorPolicy recordSeparatorPolicy;

	private LineTokenizer tokenizer = new DelimitedLineTokenizer();

	private String encoding = DEFAULT_CHARSET;

	/**
	 * Setter for resource property. The location of an input stream that can be
	 * read.
	 * @param resource
	 * @throws IOException
	 */
	public void setResource(Resource resource) throws IOException {
		this.resource = resource;
	}

	/**
	 * Public setter for the recordSeparatorPolicy. Used to determine where the
	 * line endings are and do things like continue over a line ending if inside
	 * a quoted string.
	 * 
	 * @param recordSeparatorPolicy the recordSeparatorPolicy to set
	 */
	public void setRecordSeparatorPolicy(RecordSeparatorPolicy recordSeparatorPolicy) {
		this.recordSeparatorPolicy = recordSeparatorPolicy;
	}

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(resource);
		Assert.state(resource.exists(), "Resource must exist: [" + resource + "]");
	}

	/**
	 * Initialize the reader if necessary.
	 */
	public void open() {
		if (reader == null) {
			reader = new ResourceLineReader(resource, encoding);
			if (recordSeparatorPolicy!=null) {
				reader.setRecordSeparatorPolicy(recordSeparatorPolicy);
			}
			reader.open();
		}
	}

	/**
	 * Close and null out the reader.
	 * 
	 * @see ResourceLifecycle
	 */
	public void close() {
		try {
			if (reader != null) {
				reader.close();
			}
		}
		finally {
			reader = null;
		}
	}

	/**
	 * Calls close to ensure that bean factories can close and always release
	 * resources.
	 * 
	 * @see org.springframework.beans.factory.DisposableBean#destroy()
	 */
	public void destroy() throws Exception {
		close();
	}

	// Reads first valid line.
	protected String readLine() {
		return (String) getReader().read();
	}

	/**
	 * A wrapper for {@link #readFieldSet()} to make this into a real
	 * {@link InputSource}.
	 * 
	 * @see org.springframework.batch.io.InputSource#read()
	 */
	public final Object read() {
		return readFieldSet();
	}

	/**
	 * Get the next {@link FieldSet} from the input.
	 * 
	 * @see org.springframework.batch.io.file.FieldSetInputSource#readFieldSet()
	 */
	public FieldSet readFieldSet() {
		String line = readLine();

		if (line != null) {
			try {
				return this.tokenizer.tokenize(line);
			}
			catch (RuntimeException ve) {
				// add current line count to message and re-throw
				// TODO: wrap the exception more carefully to preserve type etc.
				ValidationException newVe = new ValidationException("Validation error at line "
						+ getReader().getCurrentLineCount() + ": " + ve.getMessage());
				throw newVe;
			}
		}
		return null;
	}

	/**
	 * Setter for the encoding for this input source. Default value is
	 * {@value #DEFAULT_CHARSET}.
	 * 
	 * @param encoding a properties object which possibly contains the encoding
	 * for this input file;
	 */
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	/**
	 * Sets descriptor for this input template.
	 */
	public void setTokenizer(LineTokenizer lineTokenizer) {
		this.tokenizer = lineTokenizer;
	}

	// Returns object representing state of the input template.
	protected ResourceLineReader getReader() {
		if (reader == null) {
			open();
			// reader is now not null, or else an exception is thrown
		}
		return reader;
	}

}
