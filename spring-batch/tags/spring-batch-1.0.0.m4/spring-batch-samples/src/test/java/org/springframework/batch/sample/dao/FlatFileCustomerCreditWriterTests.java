package org.springframework.batch.sample.dao;

import java.math.BigDecimal;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.sample.domain.CustomerCredit;

public class FlatFileCustomerCreditWriterTests extends TestCase {

	private MockControl outputControl;
	private ResourceLifecycleItemWriter output;
	private FlatFileCustomerCreditWriter writer;
	
	public void setUp() throws Exception {
		super.setUp();
		
		//create mock for OutputSource
		outputControl = MockControl.createControl(ResourceLifecycleItemWriter.class);
		output = (ResourceLifecycleItemWriter)outputControl.getMock();
		
		//create new writer
		writer = new FlatFileCustomerCreditWriter();
		writer.setOutputSource(output);
	}

	public void testOpen() throws Exception {
		
		//set-up outputSource mock
		output.open();
		outputControl.replay();
		
		//call tested method
		writer.open();
		
		//verify method calls
		outputControl.verify();
	}
	
	public void testClose() throws Exception{
		
		//set-up outputSource mock
		output.close();
		outputControl.replay();
		
		//call tested method
		writer.close();
		
		//verify method calls
		outputControl.verify();
	}
	
	public void testWrite() throws Exception {
		
		//Create and set-up CustomerCredit
		CustomerCredit credit = new CustomerCredit();
		credit.setCredit(new BigDecimal(1));
		credit.setName("testName");
		
		//set separator
		writer.setSeparator(";");
		
		//set-up OutputSource mock
		output.write("testName;1");
		output.open();
		outputControl.replay();
		
		//call tested method
		writer.writeCredit(credit);
		
		//verify method calls
		outputControl.verify();
	}
	
	private interface ResourceLifecycleItemWriter extends ItemWriter, ItemStream{
		
	}
}