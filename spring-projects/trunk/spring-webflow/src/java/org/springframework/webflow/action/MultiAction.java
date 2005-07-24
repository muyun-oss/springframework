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
package org.springframework.webflow.action;

import org.springframework.binding.AttributeSource;
import org.springframework.webflow.Event;
import org.springframework.webflow.RequestContext;
import org.springframework.webflow.util.DispatchMethodInvoker;

/**
 * Action implementation that bundles two or more action execution methods into a
 * single class. Action execution methods defined by subclasses must adhere
 * to the following signature:
 * 
 * <pre>
 *     public Event ${method}(RequestContext context) throws Exception;
 * </pre>
 * 
 * When this action is invoked, by default the <code>id</code> of the calling 
 * action state state is treated as the action execution method name.  Alternatively,
 * the execution method name may be explicitly specified as a property of
 * the calling action state.  
 * <p>
 * For example, the following action state definition:
 * <pre>
 *     &lt;action-state id=&quot;search&quot;&gt;
 *          &lt;action bean=&quot;my.search.action&quot;/&gt;
 *          &lt;transition on=&quot;success&quot; to=&quot;results&quot;/&gt;
 *     &lt;/action-state&gt;
 * </pre>
 * 
 * ... when entered, executes the method:
 * 
 * <pre>
 *     public Event search(RequestContext context) throws Exception;
 * </pre>
 * 
 * Alternatively you may explictly specify the method name:
 * 
 * <pre>
 *     &lt;action-state id=&quot;searchState&quot;&gt;
 *         &lt;action bean=&quot;my.search.action&quot method=&quot;search&quot;/&gt;
 *         &lt;transition on=&quot;success&quot; to=&quot;results&quot;/&gt;
 *     &lt;/action-state&gt;
 * </pre>
 * 
 * <p>
 * A typical use of the MultiAction is to centralize all command logic for a flow
 * in one place. Another common use is to centralize form setup and submit logic
 * into one place, or CRUD (create/read/update/delete) operations for a single
 * domain object in one place. 
 * <p>
 * <b>Exposed configuration properties:</b> <br>
 * <table border="1">
 * <tr>
 * <td><b>Name </b></td>
 * <td><b>Default </b></td>
 * <td><b>Description </b></td>
 * </tr>
 * <tr>
 * <td>delegate</td>
 * <td><i>this</i></td>
 * <td>Set the delegate object holding the action execution methods.</td>
 * </tr>
 * <tr>
 * <td>executeMethodNameResolver</td>
 * <td><i>{@link MultiAction.DefaultActionExecuteMethodNameResolver default}</i></td>
 * <td>Set the strategy used to resolve the name of an action execution method.  Allows
 * full control over the method resolution algorithm.</td>
 * </tr>
 * </table>
 * 
 * @see MultiAction.ActionExecuteMethodNameResolver
 * @see MultiAction.DefaultActionExecuteMethodNameResolver
 * 
 * @author Keith Donald
 * @author Erwin Vervaet
 */
public class MultiAction extends AbstractAction {

	/**
	 * A cache for dispatched action execute methods.
	 */
	private DispatchMethodInvoker executeMethodDispatcher = new DispatchMethodInvoker(this,
			new Class[] { RequestContext.class }, Event.class, "action");

	/**
	 * The action execute method name resolver strategy.
	 */
	private ActionExecuteMethodNameResolver executeMethodNameResolver = new DefaultActionExecuteMethodNameResolver();

	/**
	 * Returns the delegate object holding the action execution methods.
	 * Defaults to this object.
	 */
	public Object getDelegate() {
		return this.executeMethodDispatcher.getTarget();
	}

	/**
	 * Set the delegate object holding the action execution methods.
	 * @param delegate the delegate to set
	 */
	public void setDelegate(Object delegate) {
		this.executeMethodDispatcher.setTarget(delegate);
	}

	/**
	 * Get the strategy used to resolve action execution method names. Defaults
	 * to {@link MultiAction.DefaultActionExecuteMethodNameResolver}.
	 */
	public ActionExecuteMethodNameResolver getExecuteMethodNameResolver() {
		return executeMethodNameResolver;
	}

	/**
	 * Set the strategy used to resolve action execution method names.
	 */
	public void setExecuteMethodNameResolver(ActionExecuteMethodNameResolver methodNameResolver) {
		this.executeMethodNameResolver = methodNameResolver;
	}

	protected Event doExecute(RequestContext context) throws Exception {
		String actionExecuteMethodName = this.executeMethodNameResolver.getMethodName(context);
		return (Event)this.executeMethodDispatcher.dispatch(actionExecuteMethodName, new Object[] { context });
	}

	/**
	 * Strategy interface used by the MultiAction to map a request context to
	 * the name of an action execution method.
	 * 
	 * @author Keith Donald
	 * @author Erwin Vervaet
	 */
	public interface ActionExecuteMethodNameResolver {

		/**
		 * Resolve a method name from given flow execution request context.
		 * @param context the flow execution request context
		 * @return the name of the method that should handle action execution
		 */
		public String getMethodName(RequestContext context);
	}

	/**
	 * Default method name resolver used by the MultiAction class.
	 * It uses the following algorithm to calculate a method name:
	 * <ol>
	 * <li>If the currently executing action has a "method" property
	 * defined, use the value as method name.</li>
	 * <li>Else, use the name of the current state of the flow execution as a
	 * method name.</li>
	 * </ol>
	 * 
	 * @author Erwin Vervaet
	 */
	public static class DefaultActionExecuteMethodNameResolver implements ActionExecuteMethodNameResolver {
		
		/**
		 * Name of the property defining the execute method name.
		 */
		public static final String METHOD_PROPERTY = "method";

		public String getMethodName(RequestContext context) {
			AttributeSource properties = context.getProperties();
			if (properties.containsAttribute(METHOD_PROPERTY)) {
				// use specified execute method name
				return (String)properties.getAttribute(METHOD_PROPERTY);
			}
			else {
				// use current state name as method name
				return context.getFlowExecutionContext().getCurrentState().getId();
			}
		}
	}
}