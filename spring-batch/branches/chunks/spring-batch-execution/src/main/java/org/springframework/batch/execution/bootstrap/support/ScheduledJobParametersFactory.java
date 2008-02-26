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
package org.springframework.batch.execution.bootstrap.support;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import org.springframework.batch.core.domain.JobParameters;
import org.springframework.batch.core.domain.JobParametersBuilder;
import org.springframework.batch.core.runtime.JobParametersFactory;

/**
 * @author Lucas Ward
 * 
 */
public class ScheduledJobParametersFactory implements JobParametersFactory {

	public static String SCHEDULE_DATE_KEY = "schedule.date";

	private DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");

	/*
	 * (non-Javadoc)
	 * @see org.springframework.batch.core.runtime.JobParametersFactory#getJobParameters(java.util.Properties)
	 */
	public JobParameters getJobParameters(Properties props) {

		if(props == null || props.isEmpty()){
			return new JobParameters();
		}

		JobParametersBuilder propertiesBuilder = new JobParametersBuilder();

		for (Iterator it = props.entrySet().iterator(); it.hasNext();) {
			Entry entry = (Entry) it.next();
			if (entry.getKey().equals(SCHEDULE_DATE_KEY)) {
				Date scheduleDate;
				try {
					scheduleDate = dateFormat.parse(entry.getValue().toString());
				}
				catch (ParseException ex) {
					throw new IllegalArgumentException("Date format is invalid: [" + entry.getValue() + "]",
							ex);
				}
				propertiesBuilder.addDate(entry.getKey().toString(), scheduleDate);
			}
			else {
				propertiesBuilder.addString(entry.getKey().toString(), entry.getValue().toString());
			}
		}

		return propertiesBuilder.toJobParameters();
	}

	/**
	 * Convert schedule date to Date, and assume all other parameters can be
	 * represented by their default string value.
	 * 
	 * @see org.springframework.batch.core.runtime.JobParametersFactory#getProperties(org.springframework.batch.core.domain.JobParameters)
	 */
	public Properties getProperties(JobParameters params) {
		
		if(params == null || params.isEmpty()){
			return new Properties();
		}
		
		Map parameters = params.getParameters();
		Properties result = new Properties();
		for (Iterator iterator = parameters.keySet().iterator(); iterator.hasNext();) {
			String key = (String) iterator.next();
			Object value = parameters.get(key);
			if (key.equals(SCHEDULE_DATE_KEY)) {
				result.setProperty(key, dateFormat.format(value));
			} else {
				result.setProperty(key,""+value);
			}
		}
		return result;
	}

	/**
	 * Public setter for injecting a date format.
	 * @param dateFormat a {@link DateFormat}, defaults to "yyyy/MM/dd"
	 */
	public void setDateFormat(DateFormat dateFormat) {
		this.dateFormat = dateFormat;
	}
}