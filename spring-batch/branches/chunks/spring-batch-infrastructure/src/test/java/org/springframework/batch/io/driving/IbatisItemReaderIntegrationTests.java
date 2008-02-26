package org.springframework.batch.io.driving;

import org.springframework.batch.io.driving.support.IbatisKeyGenerator;
import org.springframework.batch.io.support.AbstractDataSourceItemReaderIntegrationTests;
import org.springframework.batch.item.ItemReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.orm.ibatis.SqlMapClientFactoryBean;

import com.ibatis.sqlmap.client.SqlMapClient;

/**
 * Tests for {@link IbatisDrivingQueryItemReader}
 *
 * @author Robert Kasanicky
 */
public class IbatisItemReaderIntegrationTests extends AbstractDataSourceItemReaderIntegrationTests {

	protected ItemReader createItemReader() throws Exception {

		SqlMapClientFactoryBean factory = new SqlMapClientFactoryBean();
		factory.setConfigLocation(new ClassPathResource("ibatis-config.xml", getClass()));
		factory.setDataSource(super.getJdbcTemplate().getDataSource());
		factory.afterPropertiesSet();
		SqlMapClient sqlMapClient = (SqlMapClient) factory.getObject();

		IbatisDrivingQueryItemReader inputSource = new IbatisDrivingQueryItemReader();
		IbatisKeyGenerator keyGenerator = new IbatisKeyGenerator();
		keyGenerator.setDrivingQueryId("getAllFooIds");
		inputSource.setDetailsQueryId("getFooById");
		keyGenerator.setRestartQueryId("getAllFooIdsRestart");
		keyGenerator.setSqlMapClient(sqlMapClient);
		inputSource.setSqlMapClient(sqlMapClient);
		inputSource.setKeyGenerator(keyGenerator);

		return inputSource;
	}


}