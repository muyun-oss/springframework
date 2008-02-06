package org.springframework.batch.io.driving.support;

import java.util.List;
import java.util.Properties;

import org.springframework.batch.item.ExecutionAttributes;
import org.springframework.test.AbstractTransactionalDataSourceSpringContextTests;

/**
 * 
 * @author Lucas Ward
 *
 */
public class SingleColumnJdbcKeyGeneratorIntegrationTests extends AbstractTransactionalDataSourceSpringContextTests {

	SingleColumnJdbcKeyGenerator keyStrategy;
	
	protected String[] getConfigLocations(){
		return new String[] { "org/springframework/batch/io/sql/data-source-context.xml"};
	}

	
	protected void onSetUpBeforeTransaction() throws Exception {
		super.onSetUpBeforeTransaction();
		
		keyStrategy = new SingleColumnJdbcKeyGenerator(getJdbcTemplate(),
		"SELECT ID from T_FOOS order by ID");
		
		keyStrategy.setRestartSql("SELECT ID from T_FOOS where ID > ? order by ID");
	}
	
	public void testRetrieveKeys(){
		
		List keys = keyStrategy.retrieveKeys();
		
		for (int i = 0; i < keys.size(); i++) {
			Long id = (Long)keys.get(i);
			assertEquals(new Long(i + 1), id);
		}
	}
	
	public void testRestoreKeys(){
		
		Properties props = new Properties();
		props.setProperty(SingleColumnJdbcKeyGenerator.RESTART_KEY, "3");
		ExecutionAttributes streamContext = new ExecutionAttributes(props);
		
		List keys = keyStrategy.restoreKeys(streamContext);
		
		assertEquals(2, keys.size());
		assertEquals(new Long(4), keys.get(0));
		assertEquals(new Long(5), keys.get(1));
	}
	
	public void testGetKeyAsStreamContext(){
		
		ExecutionAttributes streamContext = keyStrategy.getKeyAsExecutionAttributes(new Long(3));
		Properties props = streamContext.getProperties();
		
		assertEquals(1, props.size());
		assertEquals("3", props.get(SingleColumnJdbcKeyGenerator.RESTART_KEY));
	}
	
	public void testGetNullKeyAsStreamContext(){
		
		try{
			keyStrategy.getKeyAsExecutionAttributes(null);
			fail();
		}catch(IllegalArgumentException ex){
			//expected
		}
	}
	
	public void testRestoreKeysFromNull(){
		
		try{
			keyStrategy.getKeyAsExecutionAttributes(null);
		}catch(IllegalArgumentException ex){
			//expected
		}
	}
}