package org.springframework.batch.execution.repository;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.batch.core.domain.Job;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobParameters;
import org.springframework.batch.core.domain.JobSupport;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.test.AbstractTransactionalDataSourceSpringContextTests;
import org.springframework.util.ClassUtils;

/**
 * Repository tests using JDBC DAOs (rather than mocks).
 * 
 * @author Robert Kasanicky
 */
public class SimpleJobRepositoryIntegrationTests extends AbstractTransactionalDataSourceSpringContextTests {

	protected String[] getConfigLocations() {
		return new String[] { ClassUtils.addResourcePathToPackagePath(getClass(), "dao/sql-dao-test.xml") };
	}

	private SimpleJobRepository jobRepository;

	public void setJobRepository(SimpleJobRepository jobRepository) {
		this.jobRepository = jobRepository;
	}

	/**
	 * Create two job executions for same job+parameters tuple. Check both
	 * executions belong to the same job instance and job.
	 */
	public void testCreateAndFind() throws Exception {

		JobSupport job = new JobSupport("testJob");
		job.setRestartable(true);

		Map stringParams = new HashMap() {
			{
				put("stringKey", "stringValue");
			}
		};
		Map longParams = new HashMap() {
			{
				put("longKey", new Long(1));
			}
		};
		Map dateParams = new HashMap() {
			{
				put("dateKey", new Date(1));
			}
		};
		JobParameters jobParams = new JobParameters(stringParams, longParams, dateParams);

		JobExecution firstExecution = jobRepository.createJobExecution(job, jobParams);

		assertEquals(job, firstExecution.getJobInstance().getJob());

		jobRepository.saveOrUpdate(firstExecution);
		firstExecution.stop();
		jobRepository.saveOrUpdate(firstExecution);
		JobExecution secondExecution = jobRepository.createJobExecution(job, jobParams);

		assertEquals(firstExecution.getJobInstance(), secondExecution.getJobInstance());
		assertEquals(job, secondExecution.getJobInstance().getJob());
	}
}