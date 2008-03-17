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

package org.springframework.batch.core;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;

import org.springframework.batch.repeat.ExitStatus;

/**
 * Batch domain object representing the execution of a job.
 * 
 * @author Lucas Ward
 * 
 */
public class JobExecution extends Entity {

	private JobInstance jobInstance;

	private transient Collection stepExecutions = new HashSet();

	private BatchStatus status = BatchStatus.STARTING;

	private Date startTime = null;

	private Date endTime = null;

	private ExitStatus exitStatus = ExitStatus.UNKNOWN;

	// Package private constructor for testing
	JobExecution() {
	}

	/**
	 * Because a JobExecution isn't valid unless the job is set, this
	 * constructor is the only valid one from a modelling point of view.
	 * 
	 * @param job the job of which this execution is a part
	 */
	public JobExecution(JobInstance job, Long id) {
		super(id);
		this.jobInstance = job;
	}

	/**
	 * Constructor for transient (unsaved) instances.
	 * 
	 * @param job the enclosing {@link JobInstance}
	 */
	public JobExecution(JobInstance job) {
		this(job, null);
	}

	public Date getEndTime() {
		return endTime;
	}

	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}

	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public BatchStatus getStatus() {
		return status;
	}

	public void setStatus(BatchStatus status) {
		this.status = status;
	}

	/**
	 * Convenience getter for for the id of the enclosing job. Useful for DAO
	 * implementations.
	 * 
	 * @return the id of the enclosing job
	 */
	public Long getJobId() {
		if (jobInstance != null) {
			return jobInstance.getId();
		}
		return null;
	}

	/**
	 * @param exitStatus
	 */
	public void setExitStatus(ExitStatus exitStatus) {
		this.exitStatus = exitStatus;
	}

	/**
	 * @return the exitCode
	 */
	public ExitStatus getExitStatus() {
		return exitStatus;
	}

	/**
	 * @return the Job that is executing.
	 */
	public JobInstance getJobInstance() {
		return jobInstance;
	}

	public void setJobInstance(JobInstance jobInstance) {
		this.jobInstance = jobInstance;
	}

	/**
	 * Accessor for the step executions.
	 * 
	 * @return the step executions that were registered
	 */
	public Collection getStepExecutions() {
		return stepExecutions;
	}

	/**
	 * Register a step execution with the current job execution.
	 * 
	 * @param stepExecution
	 */
	public StepExecution createStepExecution(Step step) {
		StepExecution stepExecution = new StepExecution(step, this, null);
		this.stepExecutions.add(stepExecution);
		return stepExecution;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.batch.core.domain.Entity#toString()
	 */
	public String toString() {
		return super.toString() + ", startTime=" + startTime + ", endTime=" + endTime + ", job=[" + jobInstance + "]";
	}

	/**
	 * Test if this {@link JobExecution} indicates that it is running. It should
	 * be noted that this does not necessarily mean that it has been persisted
	 * as such yet.
	 * @return true if the end time is null
	 */
	public boolean isRunning() {
		return endTime == null;
	}

	/**
	 * Test if this {@link JobExecution} indicates that it has been signalled to
	 * stop.
	 * @return true if the status is {@link BatchStatus#STOPPING}
	 */
	public boolean isStopping() {
		return status == BatchStatus.STOPPING;
	}

	/**
	 * Signal the {@link JobExecution} to stop. Iterates through the associated
	 * {@link StepExecution}s, calling {@link StepExecution#setTerminateOnly()}.
	 * 
	 */
	public void stop() {
		for (Iterator it = stepExecutions.iterator(); it.hasNext();) {
			StepExecution stepExecution = (StepExecution) it.next();
			stepExecution.setTerminateOnly();
		}
		status = BatchStatus.STOPPING;
	}
}
