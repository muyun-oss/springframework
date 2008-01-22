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

package org.springframework.batch.sample;

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.core.domain.BatchStatus;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstanceProperties;
import org.springframework.batch.core.executor.StepInterruptedException;
import org.springframework.batch.core.runtime.SimpleJobIdentifier;

/**
 * Functional test for graceful shutdown.  A batch container is started in a new thread,
 * then it's stopped via the Lifecycle interface.  
 * 
 * @author Lucas Ward
 *
 */
public class GracefulShutdownFunctionalTest extends AbstractBatchLauncherTests {

	protected String[] getConfigLocations(){
		return new String[] {"jobs/infiniteLoopJob.xml"};
	}

	public void testLaunchJob() throws Exception {
		final List errors = new ArrayList();
		final JobInstanceProperties jobInstanceProperties = new JobInstanceProperties();
		
		
//		Thread jobThread = new Thread(){
//			public void run(){
//				try {
//					launcher.run(getJob(), jobInstanceProperties);
//				}
//				catch (RuntimeException e) {
//					if (!(e.getCause() instanceof StepInterruptedException)) {
//						errors.add(e);						
//					}
//				}
//				catch (Exception e) {
//					errors.add(e);
//				}
//			}
//		};
//		
//		jobThread.start();
//		
//		//give the thread a second to start up
//		Thread.sleep(200);
//		
//		assertTrue(launcher.isRunning());
//		assertTrue(jobThread.isAlive());
//		
//		//stop the job
//		
//		launcher.stop();
//		
//		//it takes a little while for it to shut down.
//		Thread.sleep(1000);
//		
//		assertFalse(launcher.isRunning());
//		assertFalse(jobThread.isAlive());
//		
//		if (!errors.isEmpty()) {
//			Exception e = (Exception) errors.get(0);
//			e.printStackTrace();
//			fail("Unexpected Exception: "+e);
//		}
		
		JobExecution jobExecution = launcher.run(getJob(), jobInstanceProperties);
		
		assertEquals(BatchStatus.STARTED, jobExecution.getExitStatus());
		
		jobExecution.stop();
		
		int count = 0;
		while(jobExecution.isRunning() && count <= 10){
			Thread.sleep(10);
		}
		
		assertFalse(jobExecution.isRunning());
	}
	
}
