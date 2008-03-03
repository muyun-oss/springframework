/*
 * Copyright 2006-2008 the original author or authors.
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

package org.springframework.osgi.web.jetty.internal;

import java.net.URL;
import java.util.Properties;

import org.mortbay.component.LifeCycle;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.HandlerContainer;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.HandlerWrapper;
import org.mortbay.util.Attributes;
import org.mortbay.xml.XmlConfiguration;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

/**
 * Simple activator for starting Jetty similar to <code>start.jar</code>. The
 * standard is not used since it expects a file system structure and thus it is
 * not usable inside OSGi. Moreover, a hook to the server lifecycle is required.
 * 
 * @author Costin Leau
 * 
 */
public class Activator implements BundleActivator {

	/** standard jetty configuration file */
	private static final String ETC_LOCATION = "/etc/jetty.xml";

	private XmlConfiguration xmlConfig;

	private Server server;

	private BundleContext bundleContext;

	private ServiceRegistration registration;

	private Thread startupThread;


	public void start(BundleContext context) throws Exception {
		this.bundleContext = context;

		final URL config = context.getBundle().getResource(ETC_LOCATION);

		if (config == null)
			throw new IllegalArgumentException("cannot find a suitable jetty configuration at " + ETC_LOCATION);

		// do the initialization on a different thread
		// so the activator finishes fast
		startupThread = new Thread(new Runnable() {

			public void run() {
				// default startup procedure
				ClassLoader cl = Activator.class.getClassLoader();
				Thread current = Thread.currentThread();
				ClassLoader old = current.getContextClassLoader();

				try {
					//current.setContextClassLoader(cl);
					//reset CCL 
					current.setContextClassLoader(null);
					xmlConfig = new XmlConfiguration(config);
					Object root = xmlConfig.configure();
					if (!(root instanceof Server)) {
						throw new IllegalArgumentException(
							"expected a Server object as a root for server configuration");
					}
					server = (Server) root;
					server.start();

					// publish server as an OSGi service
					registration = publishServerAsAService(server);

					server.join();
				}
				catch (Exception ex) {
					throw new RuntimeException("cannot start server", ex);
				}
				finally {
					current.setContextClassLoader(old);
				}
			}
		}, "Jetty Start Thread");

		startupThread.start();
	}

	public void stop(BundleContext context) throws Exception {
		// unpublish service first
		registration.unregister();

		// default startup procedure
		ClassLoader cl = Activator.class.getClassLoader();
		Thread current = Thread.currentThread();
		ClassLoader old = current.getContextClassLoader();

		try {
			//current.setContextClassLoader(cl);
			//reset CCL 
			current.setContextClassLoader(null);
			server.stop();
		}
		finally {
			current.setContextClassLoader(old);
		}
	}

	private ServiceRegistration publishServerAsAService(Server server) {
		Properties props = new Properties();
		// put some extra properties to easily identify the service
		props.put(Constants.SERVICE_VENDOR, "Spring Framework");
		props.put(Constants.SERVICE_DESCRIPTION, "Jetty " + server.getVersion());
		props.put(Constants.BUNDLE_VERSION, server.getVersion());
		props.put(Constants.BUNDLE_NAME, bundleContext.getBundle().getSymbolicName());

		// spring-dm specific property
		props.put("org.springframework.osgi.bean.name", "jetty-server");

		// publish just the interfaces and the major classes (server/handlerWrapper)
		String[] classes = new String[] { Server.class.getName(), HandlerWrapper.class.getName(),
			Attributes.class.getName(), HandlerContainer.class.getName(), Handler.class.getName(),
			LifeCycle.class.getName() };
		return bundleContext.registerService(classes, server, props);
	}
}