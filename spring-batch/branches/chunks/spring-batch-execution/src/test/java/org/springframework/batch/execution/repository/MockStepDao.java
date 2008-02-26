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

package org.springframework.batch.execution.repository;

import java.util.List;

import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.Step;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.execution.repository.dao.StepExecutionDao;
import org.springframework.batch.item.ExecutionContext;

public class MockStepDao implements StepExecutionDao {

	private List newSteps;

	private int currentNewStep = 0;

	public List findStepInstances(JobInstance job) {
		return newSteps;
	}

	public void saveStepExecution(StepExecution stepExecution) {
	}

	public void updateStepExecution(StepExecution stepExecution) {
	}

	public void setStepsToReturnOnCreate(List steps) {
		this.newSteps = steps;
	}

	public void resetCurrentNewStep() {
		currentNewStep = 0;
	}

	public ExecutionContext findExecutionContext(StepExecution stepExecution) {
		return null;
	}

	public void saveExecutionContext(StepExecution stepExecution) {
	}

	public void updateExecutionContext(StepExecution stepExecution) {
	}

	public StepExecution getStepExecution(JobExecution jobExecution, Step step) {
		return null;
	}

}