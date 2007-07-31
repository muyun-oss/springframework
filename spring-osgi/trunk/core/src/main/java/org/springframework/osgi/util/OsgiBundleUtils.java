/*
 * Copyright 2002-2007 the original author or authors.
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
package org.springframework.osgi.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.springframework.core.ConstantException;
import org.springframework.core.Constants;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldCallback;
import org.springframework.util.ReflectionUtils.FieldFilter;

/**
 * Utility class for OSGi {@link Bundle}s.
 * 
 * @author Adrian Colyer
 * @author Costin Leau
 * 
 */
public abstract class OsgiBundleUtils {

	public static final Constants BUNDLE_STATES = new Constants(Bundle.class);

	/**
	 * Return the underlying BundleContext for the given Bundle. This uses
	 * reflection and highly dependent of the OSGi implementation. Should be
	 * removed if OSGi 4.1 is being used.
	 * 
	 * @param bundle
	 * @return
	 */
	public static BundleContext getBundleContext(final Bundle bundle) {
		if (bundle == null)
			return null;

		// try Equinox getContext
		Method meth = ReflectionUtils.findMethod(bundle.getClass(), "getContext", new Class[0]);

		// fallback to getBundleContext (OSGi 4.1)
		if (meth == null)
			meth = ReflectionUtils.findMethod(bundle.getClass(), "getBundleContext", new Class[0]);

		final Method m = meth;
		if (meth != null) {
			AccessController.doPrivileged(new PrivilegedAction() {
				public Object run() {
					m.setAccessible(true);
					return null;
				}
			});

			return (BundleContext) ReflectionUtils.invokeMethod(m, bundle);
		}

		// fallback to field inspection (KF and Prosyst)
		final BundleContext[] ctx = new BundleContext[1];

		ReflectionUtils.doWithFields(bundle.getClass(), new FieldCallback() {
			public void doWith(final Field field) throws IllegalArgumentException, IllegalAccessException {
				AccessController.doPrivileged(new PrivilegedAction() {
					public Object run() {
						field.setAccessible(true);
						return null;
					}
				});
				ctx[0] = (BundleContext) field.get(bundle);
			}
		}, new FieldFilter() {
			public boolean matches(Field field) {
				return BundleContext.class.isAssignableFrom(field.getType());
			}
		});
		
		return ctx[0];
	}

	/**
	 * Return true if the given bundle is active or not.
	 * 
	 * @param bundle
	 * @return
	 */
	public static boolean isBundleActive(Bundle bundle) {
		Assert.notNull(bundle, "bundle is required");
		int state = bundle.getState();
		return (state == Bundle.ACTIVE || state == Bundle.STARTING);
	}

	/**
	 * Return the Bundle state as a String.
	 * 
	 * @param bundle
	 * @return
	 */
	public static String getBundleStateAsString(Bundle bundle) {
		Assert.notNull(bundle, "bundle is required");
		int state = bundle.getState();

		try {
			return BUNDLE_STATES.toCode(new Integer(state), "");
		}
		catch (ConstantException cex) {
			return "UNKNOWN STATE";
		}
	}

	public static String getNullSafeSymbolicName(Bundle bundle) {
		Assert.notNull(bundle, "bundle is required");
		return (String) (bundle.getSymbolicName() == null ? bundle.getHeaders().get(
			org.osgi.framework.Constants.BUNDLE_NAME) : bundle.getSymbolicName());
	}

	public static boolean isFragment(Bundle bundle) {
		Assert.notNull(bundle, "bundle is required");
		return bundle.getHeaders().get(org.osgi.framework.Constants.FRAGMENT_HOST) != null;
	}

	public static boolean isSystemBundle(Bundle bundle) {
		Assert.notNull(bundle);
		return (bundle.getBundleId() == 0);
	}
}
