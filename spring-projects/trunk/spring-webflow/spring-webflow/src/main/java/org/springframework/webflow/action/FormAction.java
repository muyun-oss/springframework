/*
 * Copyright 2002-2006 the original author or authors.
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

import java.lang.reflect.Method;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.propertyeditors.PropertiesEditor;
import org.springframework.core.style.StylerUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.DataBinder;
import org.springframework.validation.Errors;
import org.springframework.validation.MessageCodesResolver;
import org.springframework.validation.Validator;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.springframework.webflow.execution.ScopeType;
import org.springframework.webflow.util.DispatchMethodInvoker;
import org.springframework.webflow.util.ReflectionUtils;

/**
 * Multi-action that implements common logic dealing with input forms. This
 * class leverages the Spring Web data binding code to do binding and
 * validation.
 * <p>
 * Several action execution methods are provided:
 * <ul>
 * <li> {@link #setupForm(RequestContext)} - Prepares the form object for
 * display on a form, {@link #createFormObject(RequestContext) creating it} and
 * an associated {@link Errors errors object} if necessary, then caching them in
 * the configured {@link #getFormObjectScope() form object scope} and
 * {@link #getFormErrorsScope() form errors scope}, respectively. Also
 * {@link #registerPropertyEditors(PropertyEditorRegistry) installs} any custom
 * property editors for formatting form object field values. This action method
 * will return the success() event unless an exception is thrown. </li>
 * <li> {@link #bindAndValidate(RequestContext)} - Binds all incoming request
 * parameters to the form object and then validates the form object using a
 * {@link #setValidator(Validator) registered validator}. This action method
 * will return the success() event if there are no binding or validation errors,
 * otherwise it will return the error() event. </li>
 * <li> {@link #bind(RequestContext)} - Binds all incoming request parameters to
 * the form object. No additional validation is performed. This action method
 * will return the success() event if there are no binding errors, otherwise it
 * will return the error() event. </li>
 * <li> {@link #validate(RequestContext)} - Validates the form object using a
 * registered validator. No data binding is performed. This action method will
 * return the success() event if there are no validation errors, otherwise it
 * will return the error() event. </li>
 * <li> {@link #resetForm(RequestContext)} - Resets the form by reloading the
 * backing form object and reinstalling any custom property editors. Returns
 * success() on completion, an exception is thrown when a failure occurs. </li>
 * </ul>
 * <p>
 * Since this is a multi-action a subclass could add any number of additional
 * action execution methods, e.g. "setupReferenceData(RequestContext)", or
 * "processSubmit(RequestContext)".
 * <p>
 * Using this action, it becomes very easy to implement form preparation and
 * submission logic in your flow. One way to do this follows:
 * <ol>
 * <li> Create a view state to display the form. In an entry action of that
 * state, invoke {@link #setupForm(RequestContext) setupForm} to prepare the new
 * form for display. </li>
 * <li> On a matching "submit" transition execute an action that invokes
 * {@link #bindAndValidate(RequestContext) bindAndValidate} to bind incoming
 * request parameters to the form object and validate the form object. </li>
 * <li> If there are binding or validation errors, the transition will not be
 * allowed and the view state will automatically be re-entered.
 * <li> If binding and validation is successful go to an action state called
 * "processSubmit" (or any other appropriate name). This will invoke an action
 * method called "processSubmit" you must provide on a subclass to process form
 * submission, e.g. interacting with the business logic. </li>
 * <li> If business processing is ok, continue to a view state to display the
 * success view. </li>
 * </ol>
 * <p>
 * Here is an example implementation of such a compact form flow:
 * 
 * <pre>
 *     &lt;view-state id=&quot;displayCriteria&quot; view=&quot;searchCriteria&quot;&gt;
 *         &lt;entry-actions&gt;
 *             &lt;action bean=&quot;searchFormAction&quot; method=&quot;setupForm&quot;/&gt;
 *         &lt;/entry-actions&gt;
 *         &lt;transition on=&quot;search&quot; to=&quot;executeSearch&quot;&gt;
 *             &lt;action bean=&quot;searchFormAction&quot; method=&quot;bindAndValidate&quot;/&gt;
 *         &lt;/transition&gt;
 *     &lt;/view-state&gt;
 *                                                                                
 *     &lt;action-state id=&quot;executeSearch&quot;&gt;
 *         &lt;action bean=&quot;searchFormAction&quot;/&gt;
 *         &lt;transition on=&quot;success&quot; to=&quot;displayResults&quot;/&gt;
 *     &lt;/action-state&gt;
 * </pre>
 * 
 * </p>
 * <p>
 * When you need additional flexibility consider splitting the view state above
 * acting as a single logical <i>form state</i> into multiple states. For
 * example, you could have one action state handle form setup, a view state
 * trigger form display, another action state handle data binding and
 * validation, and another process form submission. This would be a bit more
 * verbose but would also give you more control over how you respond to specific
 * results of fine-grained actions that occur within the flow.
 * <p>
 * <b>Subclassing hooks:</b>
 * <ul>
 * <li>A important hook is
 * {@link #createFormObject(RequestContext) createFormObject}. You may override
 * this to customize where the backing form object instance comes from (e.g
 * instantiated transiently in memory or loaded from a database).</li>
 * <li>An optional hook method provided by this class is
 * {@link #initBinder(RequestContext, DataBinder) initBinder}. This is called
 * after a new data binder is created by any of the action execution methods. It
 * allows you to install any custom property editors required to format
 * richly-typed form object property values.
 * <p>
 * Note: consider setting an explicit
 * {@link org.springframework.beans.PropertyEditorRegistrar} strategy as a more
 * reusable way to encapsulate custom PropertyEditor installation logic.</li>
 * </ul>
 * <p>
 * Note that this action does not provide a <i>referenceData()</i> hook method
 * similar to that of Spring MVC's <code>SimpleFormController</code>. If you
 * wish to expose reference data to populate form drop downs you can define a
 * custom action method in your FormAction subclass that does just that. Simply
 * invoke it as either a chained action as part of the setupForm state, or as a
 * fine grained state definition itself.
 * <p>
 * For example, you might create this method in your subclass:
 * 
 * <pre>
 * public Event setupReferenceData(RequestContext context) throws Exception {
 *     MutableAttributeMap requestScope = context.getRequestScope();
 *     requestScope.put(&quot;refData&quot;, referenceDataDao.getSupportingFormData());
 *     return success();
 * }
 * </pre>
 * 
 * ... and then invoke it like this:
 * 
 * <pre>
 *     &lt;view-state id=&quot;displayCriteria&quot; view=&quot;searchCriteria&quot;&gt;
 *         &lt;render-actions&gt;
 *             &lt;action bean=&quot;searchFormAction&quot; method=&quot;setupForm&quot;/&gt;
 *             &lt;action bean=&quot;searchFormAction&quot; method=&quot;setupReferenceData&quot;/&gt;
 *         &lt;/render-actions&gt;
 *         ...
 *     &lt;/view-state&gt;
 * </pre>
 * 
 * This style of calling multiple action methods in a chain (Chain of
 * Responsibility) is preferred to overridding a single action method. In
 * general, action method overriding is discouraged.
 * <p>
 * When it comes to validating submitted input data using a registered
 * {@link org.springframework.validation.Validator}, this class offers the
 * following options:
 * <ul>
 * <li>If you don't want validation at all, just call
 * {@link #bind(RequestContext)} instead of
 * {@link #bindAndValidate(RequestContext)}.</li>
 * <li>If you want piecemeal validation, e.g. in a multi-page wizard, call
 * {@link #bindAndValidate(RequestContext)} or {@link #validate(RequestContext)}
 * and specify a {@link #VALIDATOR_METHOD_ATTRIBUTE validatorMethod} action
 * execution attribute. This will invoke the identified custom validator method
 * on the validator. The validator method signature should follow the following
 * pattern:
 * 
 * <pre>
 *     public void ${validateMethodName}(${formObjectClass}, Errors)
 * </pre>
 * 
 * For instance, having a action definition like this:
 * 
 * <pre>
 *     &lt;action bean=&quot;searchFormAction&quot; method=&quot;bindAndValidate&quot;&gt;
 *         &lt;attribute name=&quot;validatorMethod&quot; value=&quot;validateSearchCriteria&quot;/&gt;
 *     &lt;/action&gt;
 * </pre>
 * 
 * Would result in the
 * <tt>public void validateSearchCriteria(SearchCriteria, Errors)</tt> method
 * of the registered validator being called if the form object class would be
 * <code>SearchCriteria</code>.</li>
 * <li>If you want to do full validation using the
 * {@link org.springframework.validation.Validator#validate(java.lang.Object, org.springframework.validation.Errors) validate}
 * method of the registered validator, call
 * {@link #bindAndValidate(RequestContext)} or {@link #validate(RequestContext)}
 * without specifying a "validatorMethod" action execution attribute.</li>
 * </ul>
 * 
 * <p>
 * <b>FormAction configurable properties</b><br>
 * <table border="1">
 * <tr>
 * <td><b>name</b></td>
 * <td><b>default</b></td>
 * <td><b>description</b></td>
 * </tr>
 * <tr>
 * <td>formObjectName</td>
 * <td>formObject</td>
 * <td>The name of the form object. The form object will be set in the
 * configured scope using this name.</td>
 * </tr>
 * <tr>
 * <td>formObjectClass</td>
 * <td>null</td>
 * <td>The form object class for this action. An instance of this class will
 * get populated and validated.</td>
 * </tr>
 * <tr>
 * <td>formObjectScope</td>
 * <td>{@link org.springframework.webflow.execution.ScopeType#FLOW flow}</td>
 * <td>The scope in which the form object will be put. If put in flow scope the
 * object will be cached and reused over the life of the flow, preserving
 * previous values. Request scope will cause a new fresh form object instance to
 * be created on each request into the flow execution.</td>
 * </tr>
 * <tr>
 * <td>formErrorsScope</td>
 * <td>{@link org.springframework.webflow.execution.ScopeType#FLOW flow}</td>
 * <td>The scope in which the form object errors instance will be put. If put
 * in flow scope the errors will be cached and reused over the life of the flow.
 * Request scope will cause a new errors instance to be created each request.</td>
 * </tr>
 * <tr>
 * <td>propertyEditorRegistrar</td>
 * <td>null</td>
 * <td>The strategy used to register custom property editors with the data
 * binder. This is an alternative to overriding the
 * {@link #initBinder(RequestContext, DataBinder) initBinder} hook method. </td>
 * </tr>
 * <tr>
 * <td>validator</td>
 * <td>null</td>
 * <td>The validator for this action. The validator must support the specified
 * form object class. </td>
 * </tr>
 * <tr>
 * <td>messageCodesResolver</td>
 * <td>null</td>
 * <td>Set the strategy to use for resolving errors into message codes. </td>
 * </tr>
 * </table>
 * 
 * @see org.springframework.beans.PropertyEditorRegistrar
 * @see org.springframework.validation.DataBinder
 * @see ScopeType
 * 
 * @author Erwin Vervaet
 * @author Keith Donald
 */
public class FormAction extends MultiAction implements InitializingBean {

	/*
	 * Implementation note: Uses deprecated DataBinder.getErrors() to remain
	 * compatible with Spring 1.2.x.
	 */

	/*
	 * Implementation note: Introspects BindException at class init time to
	 * preserve 1.2.x compatability
	 */
	private static boolean hasPropertyEditorRegistryAccessor;

	static {
		hasPropertyEditorRegistryAccessor = ClassUtils
				.hasMethod(BindException.class, "getPropertyEditorRegistry", null);
	}

	/**
	 * The default form object name ("formObject").
	 */
	public static final String DEFAULT_FORM_OBJECT_NAME = "formObject";

	/**
	 * Optional attribute that identifies the method that should be invoked on
	 * the configured validator instance, to support piecemeal wizard page
	 * validation ("validatorMethod").
	 */
	public static final String VALIDATOR_METHOD_ATTRIBUTE = "validatorMethod";

	/**
	 * The name the form object should be exposed under. Default is
	 * {@link #DEFAULT_FORM_OBJECT_NAME}.
	 */
	private String formObjectName = DEFAULT_FORM_OBJECT_NAME;

	/**
	 * The type of form object, typically an instantiable class. Required if
	 * {@link #createFormObject(RequestContext)} is not overidden.
	 */
	private Class formObjectClass;

	/**
	 * The scope in which the form object should be exposed. Default is
	 * {@link ScopeType#FLOW}.
	 */
	private ScopeType formObjectScope = ScopeType.FLOW;

	/**
	 * The scope in which the form object errors holder should be exposed.
	 * Default is {@link ScopeType#FLOW}.
	 */
	private ScopeType formErrorsScope = ScopeType.FLOW;

	/**
	 * A centralized service for property editor registration, for applying type
	 * conversion during form object data binding.
	 */
	private PropertyEditorRegistrar propertyEditorRegistrar;

	/**
	 * A validator for the form's form object.
	 */
	private Validator validator;

	/**
	 * Strategy for resolving error message codes.
	 */
	private MessageCodesResolver messageCodesResolver;

	/**
	 * A cache for dispatched action execute methods.
	 */
	private DispatchMethodInvoker validateMethodInvoker;

	/**
	 * Bean-style default constructor; creates a initially unconfigured
	 * FormAction instance relying on default property values. Clients invoking
	 * this constructor directly must set the {@link #formObjectClass} property.
	 * @see #setFormObjectClass(Class)
	 */
	public FormAction() {
	}

	/**
	 * Creates a new form action that manages instance(s) of the specified form
	 * object class.
	 * @param formObjectClass the class of the form object (must be
	 * instantiable)
	 */
	public FormAction(Class formObjectClass) {
		setFormObjectClass(formObjectClass);
	}

	/**
	 * Return the name of the form object in the configured scope.
	 */
	public String getFormObjectName() {
		return formObjectName;
	}

	/**
	 * Set the name of the form object in the configured scope. The form object
	 * will be included in the configured scope under this name.
	 */
	public void setFormObjectName(String formObjectName) {
		this.formObjectName = formObjectName;
	}

	/**
	 * Return the form object class for this action.
	 */
	public Class getFormObjectClass() {
		return formObjectClass;
	}

	/**
	 * Set the form object class for this action. An instance of this class will
	 * get populated and validated. This is a required property!
	 * <p>
	 * If not form object name is set at the moment this method is called, a
	 * form object name will be automatically generated based on the provided
	 * form object class using
	 * {@link ClassUtils#getShortNameAsProperty(java.lang.Class)}.
	 */
	public void setFormObjectClass(Class formObjectClass) {
		this.formObjectClass = formObjectClass;
		// generate a default form object name
		if ((formObjectName == null || formObjectName == DEFAULT_FORM_OBJECT_NAME) && formObjectClass != null) {
			formObjectName = ClassUtils.getShortNameAsProperty(formObjectClass);
		}
	}

	/**
	 * Get the scope in which the form object will be placed. Defaults to flow
	 * scope.
	 */
	public ScopeType getFormObjectScope() {
		return formObjectScope;
	}

	/**
	 * Set the scope in which the form object will be placed.
	 */
	public void setFormObjectScope(ScopeType scopeType) {
		this.formObjectScope = scopeType;
	}

	/**
	 * Get the scope in which the Errors object will be placed. Defaults to
	 * request scope.
	 */
	public ScopeType getFormErrorsScope() {
		return formErrorsScope;
	}

	/**
	 * Set the scope in which the Errors object will be placed. Defaults to
	 * request scope.
	 */
	public void setFormErrorsScope(ScopeType errorsScope) {
		this.formErrorsScope = errorsScope;
	}

	/**
	 * Get the property editor registration strategy for this action's data
	 * binders.
	 */
	public PropertyEditorRegistrar getPropertyEditorRegistrar() {
		return propertyEditorRegistrar;
	}

	/**
	 * Set a property editor registration strategy for this action's data
	 * binders. This is an alternative to overriding the initBinder() method.
	 */
	public void setPropertyEditorRegistrar(PropertyEditorRegistrar propertyEditorRegistrar) {
		this.propertyEditorRegistrar = propertyEditorRegistrar;
	}

	/**
	 * Returns the validator for this action.
	 */
	public Validator getValidator() {
		return validator;
	}

	/**
	 * Set the validator for this action. The validator must support the
	 * specified form object class.
	 */
	public void setValidator(Validator validator) {
		this.validator = validator;
	}

	/**
	 * Return the strategy to use for resolving errors into message codes.
	 */
	public MessageCodesResolver getMessageCodesResolver() {
		return messageCodesResolver;
	}

	/**
	 * Set the strategy to use for resolving errors into message codes. Applies
	 * the given strategy to all data binders used by this action.
	 * <p>
	 * Default is null, i.e. using the default strategy of the data binder.
	 * @see #createBinder(RequestContext, Object)
	 * @see org.springframework.validation.DataBinder#setMessageCodesResolver(org.springframework.validation.MessageCodesResolver)
	 */
	public void setMessageCodesResolver(MessageCodesResolver messageCodesResolver) {
		this.messageCodesResolver = messageCodesResolver;
	}

	protected void initAction() {
		if (getValidator() != null) {
			if (getFormObjectClass() != null && !getValidator().supports(getFormObjectClass())) {
				throw new IllegalArgumentException("Validator [" + getValidator()
						+ "] does not support form object class [" + getFormObjectClass() + "]");
			}
			Assert.notNull(getFormObjectClass(), "When using a validator, the form object class is required");
			validateMethodInvoker = new DispatchMethodInvoker(validator, new Class[] { getFormObjectClass(),
					Errors.class });
		}
	}

	// action methods

	/**
	 * <p>
	 * Prepares a form object for display in a new form, creating it and caching
	 * it in the {@link #getFormObjectScope()} if necessary. Also installs
	 * custom property editors for formatting form object values in UI controls
	 * such as text fields.
	 * </p>
	 * <p>
	 * NOTE: This is action method is not designed to be overidden and will
	 * become <code>final</code> in a future version of Spring Web Flow. If
	 * you need to execute custom form setup logic have your flow call this
	 * method along with your own custom methods as part of a single action
	 * chain.
	 * </p>
	 * 
	 * @see #createFormObject(RequestContext)
	 * 
	 * @param context the action execution context, for accessing and setting
	 * data in "flow scope" or "request scope"
	 * @return "success" when binding and validation is successful
	 * @throws Exception an <b>unrecoverable</b> exception occurs, either
	 * checked or unchecked
	 */
	public Event setupForm(RequestContext context) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("Executing setupForm");
		}
		// ensure the form object and errors collection is created and
		// exposed to the flow
		if (!formErrorsExposed(context)) {
			// initialize and expose a fresh errors instance to the flow with
			// editors applied
			initFormErrors(context, getFormObject(context));
		}
		else {
			// reapply property editors against the existing errors instance
			// if necessary
			reinstallPropertyEditorsIfNecessary(context);
		}
		return success();
	}

	/**
	 * <p>
	 * Bind incoming request parameters to allowed fields of the form object and
	 * then validate the bound form object if a validator is configured.
	 * </p>
	 * <p>
	 * NOTE: This is action method is not designed to be overidden and will
	 * become <code>final</code> in a future version of Spring Web Flow. If
	 * you need to execute custom bind and validate logic have your flow call
	 * this method along with your own custom methods as part of a single action
	 * chain. Alternatively, override the
	 * {@link #doBind(RequestContext, DataBinder)} or
	 * {@link #doValidate(RequestContext, DataBinder)} hook.
	 * </p>
	 * @param context the action execution context, for accessing and setting
	 * data in "flow scope" or "request scope"
	 * @return "success" when binding and validation is successful, "error" if
	 * ther were binding and/or validation errors
	 * @throws Exception an <b>unrecoverable</b> exception occured, either
	 * checked or unchecked
	 */
	public Event bindAndValidate(RequestContext context) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("Executing bind");
		}
		Object formObject = getFormObject(context);
		DataBinder binder = createBinder(context, formObject);
		doBind(context, binder);
		putFormErrors(context, binder.getErrors());
		if (getValidator() != null && validationEnabled(context)) {
			if (logger.isDebugEnabled()) {
				logger.debug("Executing validate");
			}
			doValidate(context, binder);
		}
		else {
			if (logger.isDebugEnabled()) {
				if (validator == null) {
					logger.debug("No validator is configured, no validation will occur after binding");
				}
				else {
					logger.debug("Validation was disabled for this bindAndValidate request");
				}
			}
		}
		return binder.getErrors().hasErrors() ? error() : success();
	}

	/**
	 * <p>
	 * Bind incoming request parameters to allowed fields of the form object.
	 * </p>
	 * <p>
	 * NOTE: This is action method is not designed to be overidden and will
	 * become <code>final</code> in a future version of Spring Web Flow. If
	 * you need to execute custom data binding logic have your flow call this
	 * method along with your own custom methods as part of a single action
	 * chain. Alternatively, override the
	 * {@link #doBind(RequestContext, DataBinder)} hook.
	 * </p>
	 * @param context the action execution context, for accessing and setting
	 * data in "flow scope" or "request scope"
	 * @return "success" if there are no binding errors, "error" otherwise
	 * @throws Exception an <b>unrecoverable</b> exception occured, either
	 * checked or unchecked
	 */
	public Event bind(RequestContext context) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("Executing bind");
		}
		Object formObject = getFormObject(context);
		DataBinder binder = createBinder(context, formObject);
		doBind(context, binder);
		putFormErrors(context, binder.getErrors());
		return binder.getErrors().hasErrors() ? error() : success();
	}

	/**
	 * Validate the form object by invoking the validator if configured.
	 * <p>
	 * NOTE: This is action method is not designed to be overidden and will
	 * become <code>final</code> in a future version of Spring Web Flow. If
	 * you need to execute custom validation logic have your flow call this
	 * method along with your own custom methods as part of a single action
	 * chain. Alternatively, override the
	 * {@link #doValidate(RequestContext, DataBinder)} hook.
	 * </p>
	 * 
	 * @see #getValidator()
	 * 
	 * @param context the action execution context, for accessing and setting
	 * data in "flow scope" or "request scope"
	 * @return "success" if there are no validation errors, "error" otherwise
	 * @throws Exception an <b>unrecoverable</b> exception occured, either
	 * checked or unchecked
	 */
	public Event validate(RequestContext context) throws Exception {
		if (getValidator() != null && validationEnabled(context)) {
			if (logger.isDebugEnabled()) {
				logger.debug("Executing validate");
			}
			Object formObject = getFormObject(context);
			DataBinder binder = createBinder(context, formObject);
			doValidate(context, binder);
			putFormErrors(context, binder.getErrors());
			return binder.getErrors().hasErrors() ? error() : success();
		}
		else {
			if (logger.isDebugEnabled()) {
				if (validator == null) {
					logger.debug("No validator is configured, no validation will occur");
				}
				else {
					logger.debug("Validation was disabled for this request");
				}
			}
			return success();
		}
	}

	/**
	 * <p>
	 * Resets the form by clearing out the form object in the specified scope
	 * and recreating it.
	 * </p>
	 * <p>
	 * NOTE: This is action method is not designed to be overidden and will
	 * become <code>final</code> in a future version of Spring Web Flow. If
	 * you need to execute custom reset logic have your flow call this method
	 * along with your own custom methods as part of a single action chain.
	 * </p>
	 * 
	 * @see #createFormObject(RequestContext)
	 * 
	 * @param context the request context
	 * @return "success" if the reset action completed successfully
	 * @throws Exception if an exception occured
	 */
	public Event resetForm(RequestContext context) throws Exception {
		Object formObject = initFormObject(context);
		initFormErrors(context, formObject);
		return success();
	}

	// accessible helpers

	/**
	 * Convenience method that returns the form object for this form action. If
	 * not found in the configured scope, a new form object will be created by a
	 * call to {@link #createFormObject(RequestContext)} and exposed in the
	 * configured {@link #getFormObjectScope() scope}.
	 * @param context the flow execution request context
	 * @return the form object
	 * @throws Exception when an unrecoverable exception occurs
	 */
	protected final Object getFormObject(RequestContext context) throws Exception {
		FormObjectAccessor accessor = getFormObjectAccessor(context);
		Object formObject = accessor.getFormObject(getFormObjectName(), getFormObjectClass(), getFormObjectScope());
		if (formObject == null) {
			formObject = initFormObject(context);
		}
		else {
			if (logger.isDebugEnabled()) {
				logger.debug("Found existing form object with name '" + getFormObjectName() + "' of type ["
						+ formObject.getClass() + "] in scope " + getFormObjectScope());
			}
			accessor.setCurrentFormObject(formObject, getFormObjectScope());
		}
		return formObject;
	}

	/**
	 * Convenience method that returns the form object errors for this form
	 * action. If not found in the configured scope, a new form object errors
	 * will be created, initialized, and exposed in the confgured
	 * {@link #getFormErrorsScope() scope}.
	 * @param context the flow request context
	 * @return the form errors
	 * @throws Exception when an unrecoverable exception occurs
	 */
	protected final Errors getFormErrors(RequestContext context) throws Exception {
		Object formObject = getFormObject(context);
		if (!formErrorsExposed(context, formObject)) {
			initFormErrors(context, formObject);
		}
		return getFormObjectAccessor(context).getFormErrors(getFormObjectName(), getFormErrorsScope());
	}

	/**
	 * Create a new binder instance for the given form object and request
	 * context. Can be overridden to plug in custom DataBinder subclasses.
	 * <p>
	 * Default implementation creates a standard WebDataBinder, and invokes
	 * {@link #initBinder(RequestContext, DataBinder)}. Note that initBinder
	 * will not be invoked if you override this method!
	 * @param context the action execution context, for accessing and setting
	 * data in "flow scope" or "request scope"
	 * @param formObject the form object to bind onto
	 * @return the new binder instance
	 * @see #initBinder(RequestContext, DataBinder)
	 * @see #setMessageCodesResolver(MessageCodesResolver)
	 */
	protected DataBinder createBinder(RequestContext context, Object formObject) {
		DataBinder binder = new WebDataBinder(formObject, getFormObjectName());
		if (messageCodesResolver != null) {
			binder.setMessageCodesResolver(messageCodesResolver);
		}
		initBinder(context, binder);
		return binder;
	}

	/**
	 * Bind allowed parameters in the external context request parameter map to
	 * the form object using given binder.
	 * @param context the action execution context, for accessing and setting
	 * data in "flow scope" or "request scope"
	 * @param binder the data binder to use
	 */
	protected void doBind(RequestContext context, DataBinder binder) {
		if (logger.isDebugEnabled()) {
			logger.debug("Binding allowed request parameters in "
					+ StylerUtils.style(context.getExternalContext().getRequestParameterMap())
					+ " to form object with name '" + binder.getObjectName() + "', pre-bind formObject toString = "
					+ binder.getTarget());
			if (binder.getAllowedFields() != null && binder.getAllowedFields().length > 0) {
				logger.debug("(Allowed fields are " + StylerUtils.style(binder.getAllowedFields()) + ")");
			}
			else {
				logger.debug("(Any field is allowed)");
			}
		}
		binder.bind(new MutablePropertyValues(context.getRequestParameters().asMap()));
		if (logger.isDebugEnabled()) {
			logger.debug("Binding completed for form object with name '" + binder.getObjectName()
					+ "', post-bind formObject toString = " + binder.getTarget());
			logger.debug("There are [" + binder.getErrors().getErrorCount() + "] errors, details: "
					+ binder.getErrors().getAllErrors());
		}
	}

	/**
	 * Validate given form object using a registered validator. If a
	 * "validatorMethod" action property is specified for the currently
	 * executing action, the identified validator method will be invoked. When
	 * no such property is found, the defualt <code>validate()</code> method
	 * is invoked.
	 * @param context the action execution context, for accessing and setting
	 * data in "flow scope" or "request scope"
	 * @param binder the data binder to use
	 * @throws Exception when an unrecoverable exception occurs
	 */
	protected void doValidate(RequestContext context, DataBinder binder) throws Exception {
		Assert.notNull(validator, "The validator must not be null when attempting validation -- programmer error");
		String validatorMethodName = context.getAttributes().getString(VALIDATOR_METHOD_ATTRIBUTE);
		if (StringUtils.hasText(validatorMethodName)) {
			invokeValidatorMethod(validatorMethodName, binder.getTarget(), binder.getErrors());
		}
		else {
			if (logger.isDebugEnabled()) {
				logger.debug("Invoking validator " + validator);
			}
			getValidator().validate(binder.getTarget(), binder.getErrors());
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Validation completed for form object with name '" + binder.getObjectName() + "'");
			logger.debug("There are [" + binder.getErrors().getErrorCount() + "] errors, details: "
					+ binder.getErrors().getAllErrors());
		}
	}

	// internal helpers

	/**
	 * Create the new form object and set it in the configured
	 * {@link #getFormObjectScope() scope}.
	 * @param context the flow execution request context
	 * @return the new form object
	 * @throws Exception an exception occured creating the form object
	 */
	private Object initFormObject(RequestContext context) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("Creating new form object with name '" + getFormObjectName() + "'");
		}
		Object formObject = createFormObject(context);
		putFormObject(context, formObject);
		return formObject;
	}

	/**
	 * Put given form object in the configured scope of given context.
	 */
	private void putFormObject(RequestContext context, Object formObject) {
		if (logger.isDebugEnabled()) {
			logger.debug("Putting form object of type [" + formObject.getClass() + "] in scope " + getFormObjectScope()
					+ " with name '" + getFormObjectName() + "'");
		}
		getFormObjectAccessor(context).putFormObject(formObject, getFormObjectName(), getFormObjectScope());
	}

	/**
	 * Initialize a new form object {@link Errors errors} instance in the
	 * configured {@link #getFormErrorsScope() scope}. This method also
	 * registers any {@link PropertiesEditor property editors} used to format
	 * form object property values.
	 * @param context the current flow execution request context
	 * @param formObject the form object for which errors will be tracked.
	 */
	private BindException initFormErrors(RequestContext context, Object formObject) {
		if (logger.isDebugEnabled()) {
			logger.debug("Creating new form errors for object with name '" + getFormObjectName() + "'");
		}
		BindException errors = createBindException(context, formObject);
		registerPropertyEditors(getPropertyEditorRegistry(errors));
		putFormErrors(context, errors);
		return errors;
	}

	/**
	 * Create an empty errors implementation of type {@link BindException}.
	 * @param context the flow execution request context
	 * @param formObject the object
	 * @return the new errors instance
	 */
	private BindException createBindException(RequestContext context, Object formObject) {
		return new BindException(formObject, getFormObjectName());
	}

	/**
	 * Put given errors instance in the configured scope of given context.
	 */
	private void putFormErrors(RequestContext context, Errors errors) {
		if (logger.isDebugEnabled()) {
			logger.debug("Putting form errors instance in scope " + getFormErrorsScope());
		}
		getFormObjectAccessor(context).putFormErrors(errors, getFormErrorsScope());
	}

	private boolean formErrorsExposed(RequestContext context) throws Exception {
		return formErrorsExposed(context, getFormObject(context));
	}

	private boolean formErrorsExposed(RequestContext context, Object formObject) {
		Errors errors = getFormObjectAccessor(context).getFormErrors(getFormObjectName(), getFormErrorsScope());
		if (errors instanceof BindException) {
			// make sure the existing form errors are consistent with the form
			// object
			BindException be = (BindException)errors;
			if (be.getTarget() != formObject) {
				if (logger.isInfoEnabled()) {
					logger.info("Inconsistency detected: the Errors instance in '" + getFormErrorsScope()
							+ "' does NOT wrap the current form object " + formObject + " of class "
							+ formObject.getClass()
							+ "; instead this Errors instance unexpectedly wraps the target object " + be.getTarget()
							+ " of class: " + be.getTarget().getClass() + ". "
							+ "[Taking corrective action: overwriting the existing Errors instance with "
							+ "an empty one for the current form object]");
				}
				// fall through below
				errors = null;
			}
		}
		return errors != null;
	}

	/**
	 * Re-registers property editors against the current form errors instance if
	 * necessary (e.g. they are not currently installed).
	 * @param context the flow execution request context
	 */
	private void reinstallPropertyEditorsIfNecessary(RequestContext context) {
		BindException errors = (BindException)getFormObjectAccessor(context).getFormErrors(getFormObjectName(),
				getFormErrorsScope());
		registerPropertyEditors(getPropertyEditorRegistry(errors));
	}

	private PropertyEditorRegistry getPropertyEditorRegistry(BindException errors) {
		Method accessor;
		try {
			if (hasPropertyEditorRegistryAccessor) {
				accessor = errors.getClass().getMethod("getPropertyEditorRegistry", null);
			}
			else {
				// only way to get at the registry in 1.2.8 or <.
				accessor = errors.getClass().getDeclaredMethod("getBeanWrapper", null);
				accessor.setAccessible(true);
			}
		}
		catch (NoSuchMethodException e) {
			throw new IllegalStateException("Unable to resolve property editor registry accessor method as expected - this should not happen");
		}
		return (PropertyEditorRegistry)ReflectionUtils.invokeMethod(accessor, errors);
	}

	/**
	 * Invoke specified validator method on the validator registered with this
	 * action. The validator method for piecemeal validation should have the
	 * following signature:
	 * 
	 * <pre>
	 *     public void ${validateMethodName}(${formObjectClass}, Errors)
	 * </pre>
	 * 
	 * @param validatorMethod the name of the validator method to invoke
	 * @param formObject the form object
	 * @param errors possible binding errors
	 * @throws Exception when an unrecoverable exception occurs
	 */
	private void invokeValidatorMethod(String validatorMethod, Object formObject, Errors errors) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("Invoking piecemeal validator method '" + validatorMethod + "(" + getFormObjectClass()
					+ ", Errors)'");
		}
		getValidateMethodInvoker().invoke(validatorMethod, new Object[] { formObject, errors });
	}

	// common subclassing hook methods

	/**
	 * Returns a dispatcher to invoke validation methods. Subclasses could
	 * override this to return a custom dispatcher.
	 */
	protected DispatchMethodInvoker getValidateMethodInvoker() {
		return validateMethodInvoker;
	}

	/**
	 * Factory method that returns a new form object accessor for accessing form
	 * objects in the provided request context.
	 * @param context the flow request context
	 * @return the accessor
	 */
	protected FormObjectAccessor getFormObjectAccessor(RequestContext context) {
		return new FormObjectAccessor(context);
	}

	/**
	 * <p>
	 * Create the backing form object instance that should be managed by this
	 * {@link FormAction form action}. By default, will attempt to instantiate
	 * a new form object instance of type {@link #getFormObjectClass()}
	 * transiently in memory.
	 * </p>
	 * <p>
	 * Subclasses should override if they need to load the form object from a
	 * specific location or resource such as a database or filesystem.
	 * </p>
	 * <p>
	 * Subclasses should override if they need to customize how a transient form
	 * object is assembled during creation.
	 * </p>
	 * @param context the action execution context for accessing flow data
	 * @return the form object
	 * @throws IllegalStateException if the {@link #getFormObjectClass()}
	 * property is not set and this method has not been overridden
	 * @throws Exception when an unrecoverable exception occurs
	 */
	protected Object createFormObject(RequestContext context) throws Exception {
		if (formObjectClass == null) {
			throw new IllegalStateException("Cannot create form object without formObjectClass property being set -- "
					+ "either set formObjectClass or override this method");
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Creating new instance of form object class [" + formObjectClass + "]");
		}
		return formObjectClass.newInstance();
	}

	/**
	 * Initialize the new binder instance. This hook allows for registration of
	 * custom property editors for performing type conversion on binding, as
	 * well as customization of other binder settings such as the
	 * {@link DataBinder#getAllowedFields() allowed fields} and 
	 * {@link DataBinder#getRequiredFields() required fields}. Called by
	 * {@link #createBinder(RequestContext, Object)}
	 * <p>
	 * @param context the action execution context, for accessing and setting
	 * data in "flow scope" or "request scope"
	 * @param binder new binder instance
	 * @see #createBinder(RequestContext, Object)
	 */
	protected void initBinder(RequestContext context, DataBinder binder) {
		registerPropertyEditors(binder);
	}

	/**
	 * <p>
	 * Register custom editors to perform type conversion on fields of your form
	 * object during data binding and form display. This method is called on
	 * form errors initialization and
	 * {@link #initBinder(RequestContext, DataBinder) data binder}
	 * initialization.
	 * </p>
	 * <p>
	 * Property editors give you full control over how objects are transformed
	 * to and from a formatted String form for display on a user interface such
	 * as a HTML page.
	 * <p>
	 * This default implementation will simply call registerCustomEditors on the
	 * {@link #getPropertyEditorRegistrar() propertyEditorRegistrar} object that
	 * has been set for the action, if any.
	 * </p>
	 * @param registry the property editor registry to register editors in
	 */
	protected void registerPropertyEditors(PropertyEditorRegistry registry) {
		if (propertyEditorRegistrar != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Registering custom property editors using configured registrar");
			}
			propertyEditorRegistrar.registerCustomEditors(registry);
		}
		else {
			if (logger.isDebugEnabled()) {
				logger.debug("No property editor registrar set, no custom editors to register");
			}
		}
	}

	/**
	 * Return whether validation should be performed given the state of the flow
	 * request context. Default implementation always returns true.
	 * @param context the request context, for accessing and setting data in
	 * "flow scope" or "request scope"
	 * @return whether or not validation is enabled
	 */
	protected boolean validationEnabled(RequestContext context) {
		return true;
	}
}