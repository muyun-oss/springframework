package org.springframework.webflow.samples.sellitem;

import java.io.File;

import org.easymock.MockControl;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.webflow.builder.FlowArtifactFactory;
import org.springframework.webflow.registry.ExternalizedFlowDefinition;
import org.springframework.webflow.support.ApplicationViewSelection;
import org.springframework.webflow.test.AbstractXmlFlowExecutionTests;
import org.springframework.webflow.test.MockFlowArtifactFactory;
import org.springframework.webflow.test.MockParameterMap;

public class SellItemFlowExecutionTests extends AbstractXmlFlowExecutionTests {

	private MockControl saleProcessorControl;

	private SaleProcessor saleProcessor;

	@Override
	protected ExternalizedFlowDefinition getFlowDefinition() {
		File flowDir = new File("src/webapp/WEB-INF");
		Resource resource = new FileSystemResource(new File(flowDir, "sellitem.xml"));
		return new ExternalizedFlowDefinition("search", resource);
	}

	public void testStartFlow() {
		ApplicationViewSelection selectedView = applicationView(startFlow());
		assertModelAttributeNotNull("sale", selectedView);
		assertViewNameEquals("priceAndItemCountForm", selectedView);
	}

	public void testSubmitPriceAndItemCount() {
		testStartFlow();
		MockParameterMap parameters = new MockParameterMap();
		parameters.put("itemCount", "4");
		parameters.put("price", "25");
		ApplicationViewSelection selectedView = applicationView(signalEvent("submit", parameters));
		assertViewNameEquals("categoryForm", selectedView);
	}

	public void testSubmitCategoryForm() {
		testSubmitPriceAndItemCount();
		MockParameterMap parameters = new MockParameterMap();
		parameters.put("category", "A");
		ApplicationViewSelection selectedView = applicationView(signalEvent("submit", parameters));
		assertViewNameEquals("costOverview", selectedView);
		assertFlowExecutionEnded();
	}

	public void testSubmitCategoryFormWithShipping() {
		testSubmitPriceAndItemCount();
		MockParameterMap parameters = new MockParameterMap();
		parameters.put("category", "A");
		parameters.put("shipping", "true");
		ApplicationViewSelection selectedView = applicationView(signalEvent("submit", parameters));
		assertViewNameEquals("shippingDetailsForm", selectedView);
	}

	public void testSubmitShippingDetailsForm() {
		testSubmitCategoryFormWithShipping();

		saleProcessor.process((Sale)getRequiredConversationAttribute("sale", Sale.class));
		saleProcessorControl.replay();

		MockParameterMap parameters = new MockParameterMap();
		parameters.put("shippingType", "E");
		ApplicationViewSelection selectedView = applicationView(signalEvent("submit", parameters));
		assertViewNameEquals("costOverview", selectedView);
		assertFlowExecutionEnded();

		saleProcessorControl.verify();
	}

	@Override
	protected FlowArtifactFactory createFlowArtifactFactory() {
		saleProcessorControl = MockControl.createControl(SaleProcessor.class);
		saleProcessor = (SaleProcessor)saleProcessorControl.getMock();
		MockFlowArtifactFactory flowArtifactFactory = new MockFlowArtifactFactory();
		flowArtifactFactory.registerBean("saleProcessor", saleProcessor);
		return flowArtifactFactory;
	}
}