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

package org.springframework.batch.execution.repository.dao;

import java.util.List;

import junit.framework.TestCase;

import org.springframework.batch.core.domain.Job;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.JobParameters;
import org.springframework.batch.core.domain.JobSupport;

public class MapJobDaoTests extends TestCase {

	MapJobDao dao = new MapJobDao();
	
	JobParameters jobParameters = new JobParameters();
	
	Job fooJob = new JobSupport("foo");
	
	protected void setUp() throws Exception {
		MapJobDao.clear();
	}
	
	public void testCreateAndRetrieveSingle() throws Exception {
		JobInstance job = dao.createJobInstance(fooJob, jobParameters);
		List result = dao.findJobInstances(fooJob, jobParameters);
		assertTrue(result.contains(job));
	}
	
	public void testCreateAndRetrieveMultiple() throws Exception {
		JobInstance job = dao.createJobInstance(fooJob, jobParameters);
		Job barJob = new JobSupport("bar");
		job = dao.createJobInstance(barJob, jobParameters);
		List result = dao.findJobInstances(barJob, jobParameters);
		assertEquals(1, result.size());
		assertTrue(result.contains(job));		
	}
	
	public void testNoExecutionsForNewJob() throws Exception {
		JobInstance job = dao.createJobInstance(fooJob, jobParameters);
		assertEquals(0, dao.getJobExecutionCount(job));
	}

	public void testSaveExecutionUpdatesId() throws Exception {
		JobInstance job = dao.createJobInstance(fooJob, jobParameters);
		JobExecution execution = new JobExecution(job);
		assertNull(execution.getId());
		dao.saveJobExecution(execution);
		assertNotNull(execution.getId());
	}
	public void testCorrectExecutionCountForExistingJob() throws Exception {
		JobInstance job = dao.createJobInstance(fooJob, jobParameters);
		dao.saveJobExecution(new JobExecution(job));
		assertEquals(1, dao.getJobExecutionCount(job));
	}

	public void testMultipleExecutionsPerExisting() throws Exception {
		JobInstance job = dao.createJobInstance(fooJob, jobParameters);
		dao.saveJobExecution(new JobExecution(job));
		Thread.sleep(50L); // Hack, hack, hackety, hack - job executions are not unique if created too close together!
		dao.saveJobExecution(new JobExecution(job));
		assertEquals(2, dao.getJobExecutionCount(job));
	}
	
	public void testGetJobExecution(){
		
		JobInstance jobInstance = dao.createJobInstance(fooJob, jobParameters);
		JobExecution jobExecution = new JobExecution(jobInstance);
		dao.saveJobExecution(jobExecution);
		JobExecution tempExecution = dao.getJobExecution(jobExecution.getId());
		assertEquals(jobExecution, tempExecution);
	}
	
	public void testGetNonExistantJobExecution(){
		
		JobInstance jobInstance = dao.createJobInstance(fooJob, jobParameters);
		JobExecution jobExecution = new JobExecution(jobInstance);
		dao.saveJobExecution(jobExecution);
		assertNull(dao.getJobExecution(new Long(999999)));

	}
}