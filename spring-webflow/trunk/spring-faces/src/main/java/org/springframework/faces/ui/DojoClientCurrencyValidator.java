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
package org.springframework.faces.ui;

import javax.el.ValueExpression;
import javax.faces.context.FacesContext;

public class DojoClientCurrencyValidator extends DojoAdvisor {

	private static final String DOJO_COMPONENT_TYPE = "dijit.form.CurrencyTextBox";

	private static final String[] DOJO_ATTRS_INTERNAL = new String[] { "currency" };

	private static final String[] DOJO_ATTRS;

	static {
		DOJO_ATTRS = new String[DojoAdvisor.DOJO_ATTRS.length + DOJO_ATTRS_INTERNAL.length];
		System.arraycopy(DojoAdvisor.DOJO_ATTRS, 0, DOJO_ATTRS, 0, DojoAdvisor.DOJO_ATTRS.length);
		System.arraycopy(DOJO_ATTRS_INTERNAL, 0, DOJO_ATTRS, DojoAdvisor.DOJO_ATTRS.length, DOJO_ATTRS_INTERNAL.length);
	}

	private String currency;

	public String getCurrency() {
		if (currency != null) {
			return currency;
		}
		ValueExpression exp = getValueExpression("currency");
		return exp != null ? (String) exp.getValue(getFacesContext().getELContext()) : null;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	protected String[] getDojoAttributes() {
		return DOJO_ATTRS;
	}

	public String getDojoComponentType() {
		return DOJO_COMPONENT_TYPE;
	}

	public Object saveState(FacesContext context) {
		Object[] values = new Object[2];
		values[0] = super.saveState(context);
		values[1] = this.currency;
		return values;
	}

	public void restoreState(FacesContext context, Object state) {
		Object values[] = (Object[]) state;
		super.restoreState(context, values[0]);
		this.currency = (String) values[1];
	}

}
