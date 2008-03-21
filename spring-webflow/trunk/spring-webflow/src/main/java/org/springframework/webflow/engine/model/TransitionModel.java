/*
 * Copyright 2004-2007 the original author or authors.
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
package org.springframework.webflow.engine.model;

import java.util.LinkedList;

import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Model support for transitions.
 * <p>
 * A path from this state to another state triggered by an event. Transitions may execute one or more actions. All
 * transition actions must execute successfully for the transition itself to complete. If no transition target is
 * specified, the transition acts as a simple event handler and does not change the state of the flow.
 * 
 * @author Scott Andrews
 */
public class TransitionModel extends AbstractModel {
	private String on;
	private String onException;
	private String to;
	private String bind;
	private LinkedList attributes;
	private SecuredModel secured;
	private LinkedList actions;

	/**
	 * Create a transition model
	 * @param on the matching criteria
	 */
	public TransitionModel(String on) {
		setOn(on);
	}

	/**
	 * Create a transition model
	 * @param on the matching criteria
	 * @param to the identifier of the state to target
	 */
	public TransitionModel(String on, String to) {
		setOn(on);
		setTo(to);
	}

	/**
	 * Create a transition model
	 * @param on the matching criteria
	 * @param to the identifier of the state to target
	 * @param onException class name of the exception to handle
	 * @param bind if the transition should bind to the defined model. Valid only for view state transitions
	 * @param attributes meta attributes for the transition
	 * @param secured security settings for the transition
	 * @param actions actions to be executed after matching the transition
	 */
	public TransitionModel(String on, String to, String onException, String bind, LinkedList attributes,
			SecuredModel secured, LinkedList actions) {
		setOn(on);
		setTo(to);
		setOnException(onException);
		setBind(bind);
		setAttributes(attributes);
		setSecured(secured);
		setActions(actions);
	}

	/**
	 * Merge properties
	 * @param model the transition to merge into this transition
	 */
	public void merge(Model model) {
		if (isMergeableWith(model)) {
			TransitionModel transition = (TransitionModel) model;
			setOnException(merge(getOnException(), transition.getOnException()));
			setTo(merge(getTo(), transition.getTo()));
			setBind(merge(getBind(), transition.getBind()));
			setAttributes(merge(getAttributes(), transition.getAttributes()));
			setSecured((SecuredModel) merge(getSecured(), transition.getSecured()));
			setActions(merge(getActions(), transition.getActions(), false));
		}
	}

	/**
	 * Tests if the model is able to be merged with this transition
	 * @param model the model to test
	 */
	public boolean isMergeableWith(Model model) {
		if (model == null) {
			return false;
		}
		if (!(model instanceof TransitionModel)) {
			return false;
		}
		TransitionModel transition = (TransitionModel) model;
		return ObjectUtils.nullSafeEquals(getOn(), transition.getOn());
	}

	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (!(obj instanceof TransitionModel)) {
			return false;
		}
		TransitionModel transition = (TransitionModel) obj;
		if (transition == null) {
			return false;
		} else if (!ObjectUtils.nullSafeEquals(getOn(), transition.getOn())) {
			return false;
		} else if (!ObjectUtils.nullSafeEquals(getOnException(), transition.getOnException())) {
			return false;
		} else if (!ObjectUtils.nullSafeEquals(getTo(), transition.getTo())) {
			return false;
		} else if (!ObjectUtils.nullSafeEquals(getBind(), transition.getBind())) {
			return false;
		} else if (!ObjectUtils.nullSafeEquals(getAttributes(), transition.getAttributes())) {
			return false;
		} else if (!ObjectUtils.nullSafeEquals(getSecured(), transition.getSecured())) {
			return false;
		} else if (!ObjectUtils.nullSafeEquals(getActions(), transition.getActions())) {
			return false;
		} else {
			return true;
		}
	}

	public int hashCode() {
		return ObjectUtils.nullSafeHashCode(getOn()) * 27 + ObjectUtils.nullSafeHashCode(getOnException()) * 27
				+ ObjectUtils.nullSafeHashCode(getTo()) * 27 + ObjectUtils.nullSafeHashCode(getBind()) * 27
				+ ObjectUtils.nullSafeHashCode(getAttributes()) * 27 + ObjectUtils.nullSafeHashCode(getSecured()) * 27
				+ ObjectUtils.nullSafeHashCode(getActions()) * 27;
	}

	/**
	 * @return the on
	 */
	public String getOn() {
		return on;
	}

	/**
	 * @param on the on to set
	 */
	public void setOn(String on) {
		if (StringUtils.hasText(on)) {
			this.on = on;
		} else {
			this.on = null;
		}
	}

	/**
	 * @return the on exception
	 */
	public String getOnException() {
		return onException;
	}

	/**
	 * @param onException the on exception to set
	 */
	public void setOnException(String onException) {
		if (StringUtils.hasText(onException)) {
			this.onException = onException;
		} else {
			this.onException = null;
		}
	}

	/**
	 * @return the to
	 */
	public String getTo() {
		return to;
	}

	/**
	 * @param to the to to set
	 */
	public void setTo(String to) {
		if (StringUtils.hasText(to)) {
			this.to = to;
		} else {
			this.to = null;
		}
	}

	/**
	 * @return the bind
	 */
	public String getBind() {
		return bind;
	}

	/**
	 * @param bind the bind to set
	 */
	public void setBind(String bind) {
		if (StringUtils.hasText(bind)) {
			this.bind = bind;
		} else {
			this.bind = null;
		}
	}

	/**
	 * @return the attributes
	 */
	public LinkedList getAttributes() {
		return attributes;
	}

	/**
	 * @param attributes the attributes to set
	 */
	public void setAttributes(LinkedList attributes) {
		this.attributes = attributes;
	}

	/**
	 * @param attribute the attribute to add
	 */
	public void addAttribute(AttributeModel attribute) {
		if (attribute == null) {
			return;
		}
		if (attributes == null) {
			attributes = new LinkedList();
		}
		attributes.add(attribute);
	}

	/**
	 * @param attributes the attributes to add
	 */
	public void addAttributes(LinkedList attributes) {
		if (attributes == null || attributes.isEmpty()) {
			return;
		}
		if (this.attributes == null) {
			this.attributes = new LinkedList();
		}
		this.attributes.addAll(attributes);
	}

	/**
	 * @return the secured
	 */
	public SecuredModel getSecured() {
		return secured;
	}

	/**
	 * @param secured the secured to set
	 */
	public void setSecured(SecuredModel secured) {
		this.secured = secured;
	}

	/**
	 * @return the actions
	 */
	public LinkedList getActions() {
		return actions;
	}

	/**
	 * @param actions the actions to set
	 */
	public void setActions(LinkedList actions) {
		this.actions = actions;
	}

	/**
	 * @param action the action to add
	 */
	public void addAction(AbstractActionModel action) {
		if (action == null) {
			return;
		}
		if (actions == null) {
			actions = new LinkedList();
		}
		actions.add(action);
	}

	/**
	 * @param actions the actions to add
	 */
	public void addActions(LinkedList actions) {
		if (actions == null || actions.isEmpty()) {
			return;
		}
		if (this.actions == null) {
			this.actions = new LinkedList();
		}
		this.actions.addAll(actions);
	}
}
