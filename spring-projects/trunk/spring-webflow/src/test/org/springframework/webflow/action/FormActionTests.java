/*
 * Copyright 2002-2004 the original author or authors.
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
package org.springframework.webflow.action;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;
import org.springframework.webflow.Event;
import org.springframework.webflow.RequestContext;
import org.springframework.webflow.ScopeType;
import org.springframework.webflow.test.MockRequestContext;

/**
 * Unit test for the FormAction class.
 * 
 * @see org.springframework.webflow.action.FormAction
 * 
 * @author Erwin Vervaet
 */
public class FormActionTests extends TestCase {
	
	private static class TestBean {
		
		private String prop;
		
		public TestBean() {
		}
		
		public TestBean(String prop) {
			this.prop = prop;
		}
		
		public String getProp() {
			return prop;
		}
		
		public void setProp(String prop) {
			this.prop = prop;
		}
	}
	
	private static class OtherTestBean {
		
		private String otherProp;
		
		public String getOtherProp() {
			return otherProp;
		}
		
		public void setOtherProp(String otherProp) {
			this.otherProp = otherProp;
		}
	}
	
	public static class TestBeanValidator implements Validator {
		public boolean supports(Class clazz) {
			return TestBean.class.equals(clazz);
		}
		
		public void validate(Object formObject, Errors errors) {
			ValidationUtils.rejectIfEmptyOrWhitespace(errors, "prop", "Prop cannot be empty");
		}
		
		public void validateTestBean(TestBean formObject, Errors errors) {
			ValidationUtils.rejectIfEmptyOrWhitespace(errors, "prop", "Prop cannot be empty");
		}
	}
	
	private FormAction action;
	
	protected void setUp() throws Exception {
		action = createFormAction("test");
	}
	
	public void testSetupForm() throws Exception {
		MockRequestContext context = new MockRequestContext(new Event(this, "test", "prop", "value"));
		
		// setupForm() should initialize the form object and the Errors
		// instance, but no bind & validate should happen since bindOnSetupForm
		// is not set
		
		assertEquals(AbstractAction.SUCCESS_EVENT_ID, action.setupForm(context).getId());
		
		assertEquals(2, context.getRequestScope().size());
		assertEquals(2, context.getFlowScope().size());
		assertFalse(getErrors(context).hasErrors());
		assertNull(getFormObject(context).getProp());
	}
	
	public void testSetupFormWithBinding() throws Exception {
		MockRequestContext context = new MockRequestContext(new Event(this, "test", "prop", "value"));

		action.setBindOnSetupForm(true);
		
		// setupForm() should initialize the form object and the Errors
		// instance and do a bind & validate (bindOnSetupForm == true)
		
		assertEquals(AbstractAction.SUCCESS_EVENT_ID, action.setupForm(context).getId());
		
		assertEquals(2, context.getRequestScope().size());
		assertEquals(2, context.getFlowScope().size());
		assertFalse(getErrors(context).hasErrors());
		assertEquals("value", getFormObject(context).getProp());
	}
	
	public void testSetupFormWithExistingFormObject() throws Exception {
		MockRequestContext context = new MockRequestContext(new Event(this, "test", "prop", "value"));
		
		assertEquals(AbstractAction.SUCCESS_EVENT_ID, action.setupForm(context).getId());
		
		Errors errors = getErrors(context);
		errors.reject("dummy");
		TestBean formObject = getFormObject(context);
		formObject.setProp("bla");
		
		// setupForm() should leave the existing form object and Errors instance
		// untouched, at least when no bind & validate is done (bindOnSetupForm == false)

		assertEquals(AbstractAction.SUCCESS_EVENT_ID, action.setupForm(context).getId());

		assertEquals(2, context.getRequestScope().size());
		assertEquals(2, context.getFlowScope().size());
		assertSame(errors, getErrors(context));
		assertSame(formObject, getFormObject(context));
		assertTrue(getErrors(context).hasErrors());
		assertEquals("bla", getFormObject(context).getProp());
	}
	
	public void testSetupFormWithExistingFormObjectAndWithBinding() throws Exception {
		MockRequestContext context = new MockRequestContext(new Event(this, "test", "prop", "value"));
		
		assertEquals(AbstractAction.SUCCESS_EVENT_ID, action.setupForm(context).getId());
		
		Errors errors = getErrors(context);
		errors.reject("dummy");
		TestBean formObject = getFormObject(context);
		formObject.setProp("bla");
		
		action.setBindOnSetupForm(true);
		
		// setupForm() should leave the existing form object untouched but should
		// generate a new errors instance since we did binding (bindOnSetupForm == true)

		assertEquals(AbstractAction.SUCCESS_EVENT_ID, action.setupForm(context).getId());

		assertEquals(2, context.getRequestScope().size());
		assertEquals(2, context.getFlowScope().size());
		assertNotSame(errors, getErrors(context));
		assertSame(formObject, getFormObject(context));
		assertFalse(getErrors(context).hasErrors());
		assertEquals("value", getFormObject(context).getProp());
	}
	
	public void testBindAndValidate() throws Exception {
		MockRequestContext context = new MockRequestContext(new Event(this, "test", "prop", "value"));
		
		// bindAndValidate() should setup a new form object and errors instance
		// and do a bind & validate
		
		context.setProperty("validatorMethod", "validateTestBean");
		assertEquals(AbstractAction.SUCCESS_EVENT_ID, action.bindAndValidate(context).getId());
		
		assertEquals(2, context.getRequestScope().size());
		assertEquals(2, context.getFlowScope().size());
		assertFalse(getErrors(context).hasErrors());
		assertEquals("value", getFormObject(context).getProp());
	}
	
	public void testBindAndValidateFailure() throws Exception {
		MockRequestContext context = new MockRequestContext(new Event(this));
		
		// bindAndValidate() should setup a new form object and errors instance
		// and do a bind & validate, which fails because the provided value is empty
		
		assertEquals(AbstractAction.ERROR_EVENT_ID, action.bindAndValidate(context).getId());
		
		assertEquals(2, context.getRequestScope().size());
		assertEquals(2, context.getFlowScope().size());
		assertTrue(getErrors(context).hasErrors());
		assertNull(getFormObject(context).getProp());
	}
	
	public void testBindAndValidateWithExistingFormObject() throws Exception {
		MockRequestContext context = new MockRequestContext(new Event(this, "test", "prop", "value"));
		
		assertEquals(AbstractAction.SUCCESS_EVENT_ID, action.setupForm(context).getId());
		
		Errors errors = getErrors(context);
		errors.reject("dummy");
		TestBean formObject = getFormObject(context);
		formObject.setProp("bla");
		
		// bindAndValidate() should leave the existing form object untouched
		// but should setup a new Errors instance during bind & validate

		assertEquals(AbstractAction.SUCCESS_EVENT_ID, action.bindAndValidate(context).getId());
		
		assertEquals(2, context.getRequestScope().size());
		assertEquals(2, context.getFlowScope().size());
		assertNotSame(errors, getErrors(context));
		assertSame(formObject, getFormObject(context));
		assertFalse(getErrors(context).hasErrors());
		assertEquals("value", getFormObject(context).getProp());
	}
	
	// this is what happens in a 'form state'
	public void testBindAndValidateFailureThenSetupForm() throws Exception {
		MockRequestContext context = new MockRequestContext(new Event(this, "test", "prop", ""));
		
		// setup existing form object & errors
		assertEquals(AbstractAction.SUCCESS_EVENT_ID, action.setupForm(context).getId());		
		TestBean formObject = getFormObject(context);
		formObject.setProp("bla");
		
		assertEquals(AbstractAction.ERROR_EVENT_ID, action.bindAndValidate(context).getId());

		assertEquals(2, context.getRequestScope().size());
		assertEquals(2, context.getFlowScope().size());
		assertSame(formObject, getFormObject(context));
		assertTrue(getErrors(context).hasErrors());
		assertEquals("", getFormObject(context).getProp());

		Errors errors = getErrors(context);
		
		// the setupForm() should leave the form object and error info setup by the
		// bind & validate untouched

		assertEquals(AbstractAction.SUCCESS_EVENT_ID, action.setupForm(context).getId());
		
		assertEquals(2, context.getRequestScope().size());
		assertEquals(2, context.getFlowScope().size());
		assertSame(errors, getErrors(context));
		assertSame(formObject, getFormObject(context));
		assertTrue(getErrors(context).hasErrors());
		assertEquals("", getFormObject(context).getProp());
	}
	
	public void testMultipleFormObjectsInOneFlow() throws Exception {
		MockRequestContext context = new MockRequestContext(new Event(this, "test", "prop", "value"));

		FormOperations otherAction = createFormAction("otherTest");
		
		assertEquals(AbstractAction.SUCCESS_EVENT_ID, action.setupForm(context).getId());
		assertEquals(AbstractAction.SUCCESS_EVENT_ID, otherAction.setupForm(context).getId());

		assertEquals(3, context.getRequestScope().size());
		assertEquals(3, context.getFlowScope().size());
		assertNotSame(getErrors(context), getErrors(context, "otherTest"));
		assertNotSame(getFormObject(context), getFormObject(context, "otherTest"));
		assertFalse(getErrors(context).hasErrors());
		assertFalse(getErrors(context, "otherTest").hasErrors());
		assertNull(getFormObject(context).getProp());
		assertNull(getFormObject(context, "otherTest").getProp());
		
		assertEquals(AbstractAction.SUCCESS_EVENT_ID, action.bindAndValidate(context).getId());
		
		assertEquals(3, context.getRequestScope().size());
		assertEquals(3, context.getFlowScope().size());
		assertNotSame(getErrors(context), getErrors(context, "otherTest"));
		assertNotSame(getFormObject(context), getFormObject(context, "otherTest"));
		assertFalse(getErrors(context).hasErrors());
		assertFalse(getErrors(context, "otherTest").hasErrors());
		assertEquals("value", getFormObject(context).getProp());
		assertNull(getFormObject(context, "otherTest").getProp());
		
		context.setLastEvent(new Event(this, "test", "prop", ""));
		
		assertEquals(AbstractAction.ERROR_EVENT_ID, otherAction.bindAndValidate(context).getId());
		
		assertEquals(3, context.getRequestScope().size());
		assertEquals(3, context.getFlowScope().size());
		assertNotSame(getErrors(context), getErrors(context, "otherTest"));
		assertNotSame(getFormObject(context), getFormObject(context, "otherTest"));
		assertFalse(getErrors(context).hasErrors());
		assertTrue(getErrors(context, "otherTest").hasErrors());
		assertEquals("value", getFormObject(context).getProp());
		assertEquals("", getFormObject(context, "otherTest").getProp());
	}
	
	public void testGetFormObject() throws Exception {
		MockRequestContext context = new MockRequestContext(new Event(this));
		FormAction action = createFormAction("test");
		TestBean formObject = (TestBean)action.getFormObject(context);
		assertNotNull(formObject);
		formObject = new TestBean();
		TestBean testBean = formObject;
		new FormObjectAccessor(context).setFormObject(formObject, action.getFormObjectName(), action.getFormObjectScope());
		formObject = (TestBean)action.getFormObject(context);
		assertSame(formObject, testBean);
	}
	
	public void testGetFormErrors() throws Exception {
		MockRequestContext context = new MockRequestContext(new Event(this));
		FormAction action = createFormAction("test");
		action.setupForm(context);
		Errors errors = action.getFormErrors(context);
		assertNotNull(errors);
		assertTrue(!errors.hasErrors());
		errors = new BindException(getFormObject(context), "test");
		Errors testErrors = errors;
		new FormObjectAccessor(context).setFormErrors(errors, action.getFormErrorsScope());
		errors = (Errors)action.getFormErrors(context);
		assertSame(errors, testErrors);
	}
	
	public void testFormObjectAccessUsingAlias() throws Exception {
		MockRequestContext context = new MockRequestContext(new Event(this));

		FormOperations otherAction = createFormAction("otherTest");
		
		assertEquals(AbstractAction.SUCCESS_EVENT_ID, action.setupForm(context).getId());
		
		assertSame(getFormObject(context), new FormObjectAccessor(context).getFormObject());
		assertSame(getErrors(context), new FormObjectAccessor(context).getFormErrors());
		
		assertEquals(AbstractAction.SUCCESS_EVENT_ID, otherAction.setupForm(context).getId());

		assertSame(getFormObject(context, "otherTest"), new FormObjectAccessor(context).getFormObject());
		assertSame(getErrors(context, "otherTest"), new FormObjectAccessor(context).getFormErrors());
		
		assertEquals(AbstractAction.ERROR_EVENT_ID, action.bindAndValidate(context).getId());
		
		assertSame(getFormObject(context), new FormObjectAccessor(context).getFormObject());
		assertSame(getErrors(context), new FormObjectAccessor(context).getFormErrors());
		
		context.setLastEvent(new Event(this, "test", "prop", "value"));

		assertEquals(AbstractAction.SUCCESS_EVENT_ID, otherAction.bindAndValidate(context).getId());

		assertSame(getFormObject(context, "otherTest"), new FormObjectAccessor(context).getFormObject());
		assertSame(getErrors(context, "otherTest"), new FormObjectAccessor(context).getFormErrors());		
	}
	
	// as reported in SWF-4
	public void testInconsistentFormObjectAndErrors() throws Exception {
		MockRequestContext context = new MockRequestContext(new Event(this));	
		
		assertEquals(AbstractAction.SUCCESS_EVENT_ID, action.setupForm(context).getId());
		
		Object formObject = getFormObject(context);
		BindException errors = (BindException)getErrors(context);
		
		assertTrue(formObject instanceof TestBean);
		assertTrue(errors.getTarget() instanceof TestBean);
		assertSame(formObject, errors.getTarget());
		
		context = new MockRequestContext();
		context.setLastEvent(new Event(this));
		
		OtherTestBean freshBean = new OtherTestBean();
		context.getFlowScope().setAttribute("test", freshBean);
		context.getRequestScope().setAttribute(BindException.ERROR_KEY_PREFIX + "test",  errors);
		
		FormAction otherAction = createFormAction("test");
		otherAction.setFormObjectClass(OtherTestBean.class);
		
		assertEquals(AbstractAction.SUCCESS_EVENT_ID, otherAction.setupForm(context).getId());

		formObject = context.getFlowScope().getAttribute("test");
		errors = (BindException)getErrors(context);
		
		assertTrue(formObject instanceof OtherTestBean);
		assertSame(freshBean, formObject);
		assertTrue("Expected OtherTestBean, but was " + errors.getTarget().getClass(), errors.getTarget() instanceof OtherTestBean);
		assertSame(formObject, errors.getTarget());
	}
	
	public void testMultipleFormObjects() throws Exception {
		MockRequestContext context = new MockRequestContext(new Event(this));	

		FormOperations action1 = createFormAction("test1");
		action1.setupForm(context);
		TestBean test1 = (TestBean)context.getFlowScope().getAttribute("test1");
		assertNotNull(test1);
		assertSame(test1, new FormObjectAccessor(context).getFormObject());

		FormOperations action2 = createFormAction("test2");
		action2.setupForm(context);
		TestBean test2 = (TestBean)context.getFlowScope().getAttribute("test2");
		assertNotNull(test2);
		assertSame(test2, new FormObjectAccessor(context).getFormObject());
		
		Map props = new HashMap();
		props.put("prop", "12345");
		context.setLastEvent(new Event(this, "submit", props));
		action1.bindAndValidate(context);
		TestBean test11 = (TestBean)context.getFlowScope().getAttribute("test1");
		assertSame(test1, test11);
		assertEquals("12345", test1.getProp());
		assertSame(test1, new FormObjectAccessor(context).getFormObject());

		props = new HashMap();
		props.put("prop", "123456");
		context.setLastEvent(new Event(this, "submit", props));
		action2.bindAndValidate(context);
		TestBean test22 = (TestBean)context.getFlowScope().getAttribute("test2");
		assertSame(test22, test2);
		assertEquals("123456", test2.getProp());
		assertSame(test2, new FormObjectAccessor(context).getFormObject());
	}
	
	public void testFormObjectAndNoErrors() throws Exception {
		// this typically happens with mapping from parent flow to subflow	
		MockRequestContext context = new MockRequestContext(new Event(this, "test", "prop", "value"));
		
		TestBean testBean = new TestBean();
		testBean.setProp("bla");
		context.getFlowScope().setAttribute("test", testBean);
		
		action.setupForm(context);
		
		// should have created a new empty errors instance, but left the form object alone
		// since we didn't to bindOnSetupForm
		
		assertSame(testBean, getFormObject(context));
		assertEquals("bla", getFormObject(context).getProp());
		assertNotNull(getErrors(context));
		assertSame(testBean, ((BindException)getErrors(context)).getTarget());
		assertFalse(getErrors(context).hasErrors());
	}
	
	// helpers
	
	private FormAction createFormAction(String formObjectName) {
		FormAction res = new FormAction();
		res.setFormObjectName(formObjectName);
		res.setFormObjectClass(TestBean.class);
		res.setValidator(new TestBeanValidator());
		res.setFormObjectScope(ScopeType.FLOW);
		res.setFormErrorsScope(ScopeType.REQUEST);
		res.setValidateOnBinding(true);
		res.initAction();
		return res;
	}
	
	private Errors getErrors(RequestContext context) {
		return getErrors(context, "test");
	}
	
	private Errors getErrors(RequestContext context, String formObjectName) {
		return (Errors)context.getRequestScope().get(BindException.ERROR_KEY_PREFIX + formObjectName);
	}
	
	private TestBean getFormObject(RequestContext context) {
		return getFormObject(context, "test");
	}
	
	private TestBean getFormObject(RequestContext context, String formObjectName) {
		return (TestBean)context.getFlowScope().get(formObjectName);
	}
}
