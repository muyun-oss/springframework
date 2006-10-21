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
package org.springframework.osgi.context;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

import org.osgi.framework.Bundle;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.osgi.mock.MockBundle;

/**
 * @author Costin Leau
 * 
 */
public class OsgiBundleResourcePatternResolverTest extends TestCase {

	OsgiBundleResourcePatternResolver resolver;
	Bundle bundle;

	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		bundle = new MockBundle();
		resolver = new OsgiBundleResourcePatternResolver(bundle);

	}

	/**
	 * Test method for
	 * {@link org.springframework.osgi.context.OsgiBundleResourcePatternResolver#OsgiBundleResourcePatternResolver(org.osgi.framework.Bundle)}.
	 */
	public void testOsgiBundleResourcePatternResolverBundle() {
		ResourceLoader res = resolver.getResourceLoader();
		assertTrue(res instanceof OsgiBundleResourceLoader);
		Resource resource = res.getResource("foo");
		assertSame(bundle, ((OsgiBundleResource) resource).getBundle());
		assertEquals(res.getResource("foo"), resolver.getResource("foo"));
	}

	/**
	 * Test method for
	 * {@link org.springframework.osgi.context.OsgiBundleResourcePatternResolver#OsgiBundleResourcePatternResolver(org.springframework.core.io.ResourceLoader)}.
	 */
	public void testOsgiBundleResourcePatternResolverResourceLoader() {
		ResourceLoader resLoader = new DefaultResourceLoader();
		resolver = new OsgiBundleResourcePatternResolver(resLoader);
		ResourceLoader res = resolver.getResourceLoader();

		assertSame(resLoader, res);
		assertEquals(resLoader.getResource("foo"), resolver.getResource("foo"));
	}

	/**
	 * Test method for
	 * {@link org.springframework.osgi.context.OsgiBundleResourcePatternResolver#getResources(java.lang.String)}.
	 */
	public void testGetResourcesString() throws Exception {
		Resource[] res;

		try {
			res = resolver.getResources("**/*");
			fail("expected exception");
		}
		catch (Exception ex) {
			// expected
		}
		
		res = resolver.getResources("bundle://foo");
		assertNotNull(res);
		assertEquals(1, res.length);
		assertEquals(new OsgiBundleResource(bundle, "bundle://foo"), res[0]);

		// TODO: need a functional test here
		res = resolver.getResources("bundle:org/springframework/osgi/context/OsgiBundleResourcePatternResolverT*");
	}
}
