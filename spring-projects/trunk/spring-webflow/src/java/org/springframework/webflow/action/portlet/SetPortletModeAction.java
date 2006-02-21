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
package org.springframework.webflow.action.portlet;

import javax.portlet.ActionResponse;
import javax.portlet.PortletMode;

import org.springframework.core.Constants;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.webflow.ActionExecutionException;
import org.springframework.webflow.Event;
import org.springframework.webflow.RequestContext;
import org.springframework.webflow.action.AbstractAction;
import org.springframework.webflow.context.portlet.PortletExternalContext;

/**
 * Action implementation that changes a PortletResponse mode.
 * <p>
 * This class is usefullwhen you want to change the current PortletMode before
 * entering a specific state, e.g. it can be the first state in a subflow.
 * <p>
 * Note: if you can, change the PortletMode using Portlet URLs (PortletURL class
 * or portlet TAG).
 * 
 * @author J.Enrique Ruiz
 * @author C�sar Ordi�ana
 * @author Erwin Vervaet
 */
public class SetPortletModeAction extends AbstractAction {

	private static final Constants PORTLET_MODE_CONSTANTS = new Constants(PortletMode.class);

	/**
	 * The portlet mode to set can be specified in an action state action
	 * property with this name.
	 */
	public static final String PORTLET_MODE_ATTRIBUTE = "portletMode";

	/**
	 * The default portlet mode.
	 */
	private PortletMode portletMode = PortletMode.VIEW;

	/**
	 * Returns the mode that will be set in the response.
	 */
	public PortletMode getPortletMode() {
		return portletMode;
	}

	/**
	 * Sets the mode that will be set in the response.
	 */
	public void setPortletMode(PortletMode mode) {
		this.portletMode = mode;
	}

	/**
	 * Sets the PortletMode.
	 * @param context the action execution context, for accessing and setting
	 * data in "flow scope" or "request scope"
	 * @return the action result event
	 * @throws Exception an <b>unrecoverable </b> exception occured, either
	 * checked or unchecked
	 */
	protected Event doExecute(RequestContext context) throws Exception {
		Assert.isInstanceOf(PortletExternalContext.class, context.getExternalContext(), "'"
				+ ClassUtils.getShortName(this.getClass()) + "' can only work with 'PortletExternalContext': ");
		PortletExternalContext portletContext = (PortletExternalContext)context.getExternalContext();
		if (portletContext.getResponse() instanceof ActionResponse) {
			PortletMode mode = (PortletMode)context.getAttributes().getAttribute(PORTLET_MODE_ATTRIBUTE,
					PortletMode.class, getPortletMode());
			((ActionResponse)portletContext.getResponse()).setPortletMode(mode);
			return success();
		}
		else {
			// portlet mode and the window state can be changed through
			// ActionResponse only, if this is not the case, it means that this
			// action has been invoked directly in a RenderRequest
			throw new ActionExecutionException(context.getCurrentState(), this, context.getAttributes(),
					"This SetPortletModeAction can only work with 'ActionResponse' "
							+ "-- make sure you are not invoking it in a RenderRequest", null);
		}
	}
}