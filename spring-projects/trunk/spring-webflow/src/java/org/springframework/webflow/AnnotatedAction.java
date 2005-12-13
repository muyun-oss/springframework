/*
 * Copyright 2002-2005 the original author or authors.
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
package org.springframework.webflow;

import java.util.Map;

import org.springframework.binding.method.MethodKey;
import org.springframework.core.style.ToStringCreator;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.webflow.action.MultiAction;

/**
 * An action proxy/decorator that stores arbitrary properties about a target
 * <code>Action</code> implementation for use within a specific Action
 * execution context, for example an <code>ActionState</code> definition, a
 * <code>TransitionCriteria</code> definition, or in a test environment.
 * <p>
 * An annotated action is an action that wraps another action (referred to as
 * the <i>target action), setting up target action's execution properties before
 * invoking execute.
 * 
 * @author Keith Donald
 */
public class AnnotatedAction extends AnnotatedObject implements Action {

	// well known properties

	/**
	 * The action name property ("name").
	 * <p>
	 * The name property is often used as a qualifier for an action's result
	 * event, and is typically used to allow the flow to respond to a specific
	 * action's outcome within a larger action execution chain.
	 * <p>
	 * {@see ActionState} for more information.
	 */
	public static final String NAME_PROPERTY = "name";

	/**
	 * The action execution method property ("method").
	 * <p>
	 * The method property is the name of a specific method on a
	 * <code>{@link MultiAction}</code> to execute, or the name of a specific
	 * method on a arbitrary POJO (plain old java.lang.Object.
	 * <p>
	 * {@see ActionState} for more information.
	 */
	public static final String METHOD_PROPERTY = "method";

	/**
	 * The action execution method result attribute property ("resultName");
	 */
	public static final String RESULT_NAME_PROPERTY = "resultName";

	/**
	 * The action execution method result attribute scope property
	 * ("resultScope");
	 */
	public static final String RESULT_SCOPE_PROPERTY = "resultScope";

	/**
	 * The target action to execute.
	 */
	private Action targetAction;

	/**
	 * Creates a new annotated action object for the specified action. No
	 * contextual properties are provided.
	 * @param targetAction the action
	 */
	public AnnotatedAction(Action targetAction) {
		setTargetAction(targetAction);
	}

	/**
	 * Creates a new annotated action object for the specified action. No
	 * contextual properties are provided.
	 * @param targetAction the action
	 */
	public AnnotatedAction(Action targetAction, Map properties) {
		setTargetAction(targetAction);
		setProperties(properties);
	}

	/**
	 * Returns the wrapped target action.
	 * @return the action
	 */
	public Action getTargetAction() {
		return targetAction;
	}

	/**
	 * Set the target action wrapped by this decorator.
	 */
	private void setTargetAction(Action targetAction) {
		Assert.notNull(targetAction, "The target Action instance is required");
		this.targetAction = targetAction;
	}

	/**
	 * Returns the name of a named action, or <code>null</code> if the action
	 * is unnamed. Used when mapping action result events to transitions.
	 * @see #isNamed()
	 * @see #postProcessResult(Event)
	 */
	public String getName() {
		return (String)getProperty(NAME_PROPERTY);
	}

	/**
	 * Sets the name of a named action. This is optional and can be
	 * <code>null</code>.
	 * @param name the action name
	 */
	public void setName(String name) {
		setProperty(NAME_PROPERTY, name);
	}

	/**
	 * Returns whether or not the wrapped target action is a named action.
	 * @see #setName(String)
	 */
	public boolean isNamed() {
		return StringUtils.hasText(getName());
	}

	/**
	 * Returns the name of the action method to invoke when the target action is
	 * executed.
	 */
	public MethodKey getMethod() {
		return (MethodKey)getProperty(METHOD_PROPERTY);
	}

	/**
	 * Sets the name of the action method to invoke when the target action is
	 * executed.
	 * @param method the action method name.
	 */
	public void setMethod(MethodKey method) {
		setProperty(METHOD_PROPERTY, method);
	}

	/**
	 * Returns the name of the attribute to export the action method return
	 * value under.
	 */
	public String getResultName() {
		return (String)getProperty(RESULT_NAME_PROPERTY);
	}

	/**
	 * Sets the name of the action method to invoke when the target action is
	 * executed.
	 * @param resultName the action return value attribute name
	 */
	public void setResultName(String resultName) {
		setProperty(RESULT_NAME_PROPERTY, resultName);
	}

	/**
	 * Returns the scope of the attribute to export the action method return
	 * value under.
	 */
	public ScopeType getResultScope() {
		return (ScopeType)getProperty(RESULT_SCOPE_PROPERTY);
	}

	/**
	 * Sets the scope of the attribute storing the action method return value.
	 * @param resultScope the result scope
	 */
	public void setResultScope(ScopeType resultScope) {
		setProperty(RESULT_SCOPE_PROPERTY, resultScope);
	}

	public Event execute(RequestContext context) throws Exception {
		try {
			context.setProperties(this);
			Event result = getTargetAction().execute(context);
			return postProcessResult(result);
		}
		finally {
			context.setProperties(null);
		}
	}

	/**
	 * Get the event id to be used as grounds for a transition in the containing
	 * state, based on given result returned from action execution.
	 * <p>
	 * If the wrapped action is named, the name will be used as a qualifier for
	 * the event (e.g. "myAction.success").
	 * @param resultEvent the action result event
	 */
	protected Event postProcessResult(Event resultEvent) {
		if (resultEvent == null) {
			return null;
		}
		if (isNamed()) {
			// qualify result event id with action name for a named action
			resultEvent.setId(getName() + "." + resultEvent.getId());
		}
		return resultEvent;
	}

	public String toString() {
		return new ToStringCreator(this).append("targetAction", getTargetAction())
				.append("properties", getProperties()).toString();
	}
}