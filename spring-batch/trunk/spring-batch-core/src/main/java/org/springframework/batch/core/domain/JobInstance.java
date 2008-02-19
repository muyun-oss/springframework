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

package org.springframework.batch.core.domain;

import java.util.ArrayList;
import java.util.List;


/**
 * Batch domain object representing a job instance. A job instance is defined as
 * a logical container for steps with unique identification of the unit as a
 * whole. A job can be executed many times with the same instance, usually if it
 * fails and is restarted, or if it is launched on an ad-hoc basis "on demand".
 * 
 * @author Lucas Ward
 * @author Dave Syer
 */
public class JobInstance extends Entity {

	private List stepNames = new ArrayList();

	private JobParameters jobParameters;
	
	private Job job;

	private int jobExecutionCount;
	
	private JobExecution lastExecution;
	
	public JobInstance(Long id, JobParameters jobParameters) {
		super(id);
		this.jobParameters = jobParameters==null ? new JobParameters() : jobParameters;
	}

	public JobInstance(Long id, JobParameters jobParameters, Job job){
		this(id, jobParameters);
		this.job = job;
	}
	
	public void setLastExecution(JobExecution lastExecution) {
		this.lastExecution = lastExecution;
	}
	
	public JobExecution getLastExecution() {
		return lastExecution;
	}
	
	public List getStepNames() {
		return stepNames;
	}

	public void setStepNames(List stepInstances) {
		this.stepNames = stepInstances;
	}

	public void addStepName(String stepName) {
		this.stepNames.add(stepName);
	}

	public int getJobExecutionCount() {
		return jobExecutionCount;
	}

	public void setJobExecutionCount(int jobExecutionCount) {
		this.jobExecutionCount = jobExecutionCount;
	}

	/**
	 * @return {@link JobParameters}
	 */
	public JobParameters getJobParameters() {
		return jobParameters;
	}

	/**
	 * @return the job name. (Equivalent to getJob().getName())
	 */
	public String getJobName() {
		return job==null ? null : job.getName();
	}
	
	public JobExecution createJobExecution() {
		JobExecution newExecution = new JobExecution(this);
		this.setLastExecution(newExecution);
		return newExecution;
	}
	
	public String toString() {
		return super.toString()+", JobParameters=["+ jobParameters +"]" +
			", Job=[" + job + "]";
	}
	
	public Job getJob() {
		return job;
	}

	public void setJob(Job job) {
		this.job = job;
	}
}
