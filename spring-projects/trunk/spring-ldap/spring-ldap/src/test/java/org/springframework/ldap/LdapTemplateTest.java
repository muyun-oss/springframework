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

package org.springframework.ldap;

import java.util.List;

import javax.naming.Binding;
import javax.naming.Name;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.PartialResultException;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;

import junit.framework.TestCase;

import org.easymock.AbstractMatcher;
import org.easymock.MockControl;
import org.springframework.ldap.AttributesMapper;
import org.springframework.ldap.ContextExecutor;
import org.springframework.ldap.ContextMapper;
import org.springframework.ldap.ContextSource;
import org.springframework.ldap.EntryNotFoundException;
import org.springframework.ldap.LdapTemplate;
import org.springframework.ldap.NamingExceptionTranslator;
import org.springframework.ldap.SearchExecutor;
import org.springframework.ldap.SearchResultCallbackHandler;
import org.springframework.ldap.support.DistinguishedName;

public class LdapTemplateTest extends TestCase {

    private MockControl contextSourceControl;

    private ContextSource contextSourceMock;

    private MockControl dirContextControl;

    private DirContext dirContextMock;

    private MockControl attributesMapperControl;

    private AttributesMapper attributesMapperMock;

    private MockControl namingEnumerationControl;

    private NamingEnumeration namingEnumerationMock;

    private MockControl nameControl;

    private Name nameMock;

    private MockControl handlerControl;

    private SearchResultCallbackHandler handlerMock;

    private MockControl contextMapperControl;

    private ContextMapper contextMapperMock;

    private MockControl exceptionTranslatorControl;

    private NamingExceptionTranslator exceptionTranslatorMock;

    private MockControl contextExecutorControl;

    private ContextExecutor contextExecutorMock;

    private MockControl searchExecutorControl;

    private SearchExecutor searchExecutorMock;

    private LdapTemplate tested;

    protected void setUp() throws Exception {
        super.setUp();

        // Setup ContextSource mock
        contextSourceControl = MockControl.createControl(ContextSource.class);
        contextSourceMock = (ContextSource) contextSourceControl.getMock();

        // Setup LdapContext mock
        dirContextControl = MockControl.createControl(LdapContext.class);
        dirContextMock = (LdapContext) dirContextControl.getMock();

        // Setup NamingEnumeration mock
        namingEnumerationControl = MockControl
                .createControl(NamingEnumeration.class);
        namingEnumerationMock = (NamingEnumeration) namingEnumerationControl
                .getMock();

        // Setup Name mock
        nameControl = MockControl.createControl(Name.class);
        nameMock = (Name) nameControl.getMock();

        // Setup Handler mock
        handlerControl = MockControl
                .createControl(SearchResultCallbackHandler.class);
        handlerMock = (SearchResultCallbackHandler) handlerControl.getMock();

        contextMapperControl = MockControl.createControl(ContextMapper.class);
        contextMapperMock = (ContextMapper) contextMapperControl.getMock();

        attributesMapperControl = MockControl
                .createControl(AttributesMapper.class);
        attributesMapperMock = (AttributesMapper) attributesMapperControl
                .getMock();

        exceptionTranslatorControl = MockControl
                .createControl(NamingExceptionTranslator.class);
        exceptionTranslatorMock = (NamingExceptionTranslator) exceptionTranslatorControl
                .getMock();

        contextExecutorControl = MockControl
                .createControl(ContextExecutor.class);
        contextExecutorMock = (ContextExecutor) contextExecutorControl
                .getMock();

        searchExecutorControl = MockControl.createControl(SearchExecutor.class);
        searchExecutorMock = (SearchExecutor) searchExecutorControl.getMock();

        tested = new LdapTemplate(contextSourceMock);
        tested.setExceptionTranslator(exceptionTranslatorMock);
    }

    protected void tearDown() throws Exception {
        super.tearDown();

        contextSourceControl = null;
        contextSourceMock = null;

        dirContextControl = null;
        dirContextMock = null;

        namingEnumerationControl = null;
        namingEnumerationMock = null;

        nameControl = null;
        nameMock = null;

        handlerControl = null;
        handlerMock = null;

        contextMapperControl = null;
        contextMapperMock = null;

        attributesMapperControl = null;
        attributesMapperMock = null;

        exceptionTranslatorControl = null;
        exceptionTranslatorMock = null;

        contextExecutorControl = null;
        contextExecutorMock = null;

        searchExecutorControl = null;
        searchExecutorMock = null;

    }

    protected void replay() {
        contextSourceControl.replay();
        dirContextControl.replay();
        namingEnumerationControl.replay();
        nameControl.replay();
        handlerControl.replay();
        contextMapperControl.replay();
        attributesMapperControl.replay();
        exceptionTranslatorControl.replay();
        contextExecutorControl.replay();
        searchExecutorControl.replay();
    }

    protected void verify() {
        contextSourceControl.verify();
        dirContextControl.verify();
        namingEnumerationControl.verify();
        nameControl.verify();
        handlerControl.verify();
        contextMapperControl.verify();
        attributesMapperControl.verify();
        exceptionTranslatorControl.verify();
        contextExecutorControl.verify();
        searchExecutorControl.verify();
    }

    private void expectGetReadWriteContext() {
        contextSourceControl.expectAndReturn(contextSourceMock
                .getReadWriteContext(), dirContextMock);
    }

    private void expectGetReadOnlyContext() {
        contextSourceControl.expectAndReturn(contextSourceMock
                .getReadOnlyContext(), dirContextMock);
    }

    public void testSearch_CallbackHandler() throws NamingException {
        expectGetReadOnlyContext();

        SearchControls controls = new SearchControls();
        controls.setSearchScope(1);
        controls.setReturningObjFlag(true);

        SearchResult searchResult = new SearchResult("", new Object(),
                new BasicAttributes());

        setupSearchAndNamingEnumeration(controls, searchResult);

        handlerMock.handleSearchResult(searchResult);

        dirContextMock.close();

        replay();

        tested.search(nameMock, "(ou=somevalue)", 1, true, handlerMock);

        verify();
    }

    public void testSearch_StringBase_CallbackHandler() throws NamingException {
        expectGetReadOnlyContext();

        SearchControls controls = new SearchControls();
        controls.setSearchScope(1);
        controls.setReturningObjFlag(true);

        SearchResult searchResult = new SearchResult("", new Object(),
                new BasicAttributes());

        setupStringSearchAndNamingEnumeration(controls, searchResult);

        handlerMock.handleSearchResult(searchResult);

        dirContextMock.close();

        replay();

        tested.search("o=example.com", "(ou=somevalue)", 1, true, handlerMock);

        verify();
    }

    private void setupStringSearchAndNamingEnumeration(SearchControls controls,
            SearchResult searchResult) throws NamingException {
        dirContextControl.setDefaultMatcher(new SearchControlsMatcher());
        dirContextControl.expectAndReturn(dirContextMock.search(
                "o=example.com", "(ou=somevalue)", controls),
                namingEnumerationMock);

        namingEnumerationControl.expectAndReturn(namingEnumerationMock
                .hasMore(), true);
        namingEnumerationControl.expectAndReturn(namingEnumerationMock.next(),
                searchResult);
        namingEnumerationControl.expectAndReturn(namingEnumerationMock
                .hasMore(), false);
        namingEnumerationMock.close();
    }

    public void testSearch_CallbackHandler_Defaults() throws NamingException {
        expectGetReadOnlyContext();

        SearchControls controls = new SearchControls();
        controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        controls.setReturningObjFlag(false);

        SearchResult searchResult = new SearchResult("", new Object(),
                new BasicAttributes());

        setupSearchAndNamingEnumeration(controls, searchResult);

        handlerMock.handleSearchResult(searchResult);

        dirContextMock.close();

        replay();

        tested.search(nameMock, "(ou=somevalue)", handlerMock);

        verify();
    }

    public void testSearch_String_CallbackHandler_Defaults()
            throws NamingException {
        expectGetReadOnlyContext();

        SearchControls controls = new SearchControls();
        controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        controls.setReturningObjFlag(false);

        SearchResult searchResult = new SearchResult("", new Object(),
                new BasicAttributes());

        setupStringSearchAndNamingEnumeration(controls, searchResult);

        handlerMock.handleSearchResult(searchResult);

        dirContextMock.close();

        replay();

        tested.search("o=example.com", "(ou=somevalue)", handlerMock);

        verify();
    }

    private void setupSearchAndNamingEnumeration(SearchControls controls,
            SearchResult searchResult) throws NamingException {
        dirContextControl.setDefaultMatcher(new SearchControlsMatcher());
        dirContextControl.expectAndReturn(dirContextMock.search(nameMock,
                "(ou=somevalue)", controls), namingEnumerationMock);

        namingEnumerationControl.expectAndReturn(namingEnumerationMock
                .hasMore(), true);
        namingEnumerationControl.expectAndReturn(namingEnumerationMock.next(),
                searchResult);
        namingEnumerationControl.expectAndReturn(namingEnumerationMock
                .hasMore(), false);
        namingEnumerationMock.close();
    }

    public void testSearch_NameNotFoundException() throws NamingException {
        expectGetReadOnlyContext();

        SearchControls controls = new SearchControls();
        controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        controls.setReturningObjFlag(false);

        dirContextControl.setDefaultMatcher(new SearchControlsMatcher());
        dirContextControl.expectAndThrow(dirContextMock.search(nameMock,
                "(ou=somevalue)", controls), new NameNotFoundException());

        dirContextMock.close();

        replay();

        tested.search(nameMock, "(ou=somevalue)", handlerMock);

        verify();
    }

    public void testSearch_NamingException() throws NamingException {
        expectGetReadOnlyContext();

        SearchControls controls = new SearchControls();
        controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        controls.setReturningObjFlag(false);

        dirContextControl.setDefaultMatcher(new SearchControlsMatcher());
        NamingException ne = new NamingException();
        dirContextControl.expectAndThrow(dirContextMock.search(nameMock,
                "(ou=somevalue)", controls), ne);

        dirContextMock.close();

        EntryNotFoundException expectedException = new EntryNotFoundException(
                "dummy");
        exceptionTranslatorControl.expectAndReturn(exceptionTranslatorMock
                .translate(ne), expectedException);

        replay();

        try {
            tested.search(nameMock, "(ou=somevalue)", handlerMock);
            fail("EntryNotFoundException expected");
        } catch (EntryNotFoundException expected) {
            assertSame(expectedException, expected);
        }

        verify();
    }

    public void testSearch_AttributesMapper() throws NamingException {
        expectGetReadOnlyContext();

        SearchControls controls = new SearchControls();
        controls.setSearchScope(1);
        controls.setReturningObjFlag(false);

        BasicAttributes expectedAttributes = new BasicAttributes();
        SearchResult searchResult = new SearchResult("", null,
                expectedAttributes);

        setupSearchAndNamingEnumeration(controls, searchResult);

        Object expectedResult = new Object();
        attributesMapperControl.expectAndReturn(attributesMapperMock
                .mapFromAttributes(expectedAttributes), expectedResult);

        dirContextMock.close();

        replay();

        List list = tested.search(nameMock, "(ou=somevalue)", 1,
                attributesMapperMock);

        verify();

        assertNotNull(list);
        assertEquals(1, list.size());
        assertSame(expectedResult, list.get(0));
    }

    public void testSearch_String_AttributesMapper() throws NamingException {
        expectGetReadOnlyContext();

        SearchControls controls = new SearchControls();
        controls.setSearchScope(1);
        controls.setReturningObjFlag(false);

        BasicAttributes expectedAttributes = new BasicAttributes();
        SearchResult searchResult = new SearchResult("", null,
                expectedAttributes);

        setupStringSearchAndNamingEnumeration(controls, searchResult);

        Object expectedResult = new Object();
        attributesMapperControl.expectAndReturn(attributesMapperMock
                .mapFromAttributes(expectedAttributes), expectedResult);

        dirContextMock.close();

        replay();

        List list = tested.search("o=example.com", "(ou=somevalue)", 1,
                attributesMapperMock);

        verify();

        assertNotNull(list);
        assertEquals(1, list.size());
        assertSame(expectedResult, list.get(0));
    }

    public void testSearch_AttributesMapper_Default() throws NamingException {
        expectGetReadOnlyContext();

        SearchControls controls = new SearchControls();
        controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        controls.setReturningObjFlag(false);

        BasicAttributes expectedAttributes = new BasicAttributes();
        SearchResult searchResult = new SearchResult("", null,
                expectedAttributes);

        setupSearchAndNamingEnumeration(controls, searchResult);

        Object expectedResult = new Object();
        attributesMapperControl.expectAndReturn(attributesMapperMock
                .mapFromAttributes(expectedAttributes), expectedResult);

        dirContextMock.close();

        replay();

        List list = tested.search(nameMock, "(ou=somevalue)",
                attributesMapperMock);

        verify();

        assertNotNull(list);
        assertEquals(1, list.size());
        assertSame(expectedResult, list.get(0));
    }

    public void testSearch_String_AttributesMapper_Default()
            throws NamingException {
        expectGetReadOnlyContext();

        SearchControls controls = new SearchControls();
        controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        controls.setReturningObjFlag(false);

        BasicAttributes expectedAttributes = new BasicAttributes();
        SearchResult searchResult = new SearchResult("", null,
                expectedAttributes);

        setupStringSearchAndNamingEnumeration(controls, searchResult);

        Object expectedResult = new Object();
        attributesMapperControl.expectAndReturn(attributesMapperMock
                .mapFromAttributes(expectedAttributes), expectedResult);

        dirContextMock.close();

        replay();

        List list = tested.search("o=example.com", "(ou=somevalue)",
                attributesMapperMock);

        verify();

        assertNotNull(list);
        assertEquals(1, list.size());
        assertSame(expectedResult, list.get(0));
    }

    public void testSearch_ContextMapper() throws NamingException {
        expectGetReadOnlyContext();

        SearchControls controls = new SearchControls();
        controls.setSearchScope(1);
        controls.setReturningObjFlag(true);

        Object expectedObject = new Object();
        SearchResult searchResult = new SearchResult("", expectedObject,
                new BasicAttributes());

        setupSearchAndNamingEnumeration(controls, searchResult);

        Object expectedResult = expectedObject;
        contextMapperControl.expectAndReturn(contextMapperMock
                .mapFromContext(expectedObject), expectedResult);

        dirContextMock.close();

        replay();

        List list = tested.search(nameMock, "(ou=somevalue)", 1,
                contextMapperMock);

        verify();

        assertNotNull(list);
        assertEquals(1, list.size());
        assertSame(expectedResult, list.get(0));
    }

    public void testSearch_String_ContextMapper() throws NamingException {
        expectGetReadOnlyContext();

        SearchControls controls = new SearchControls();
        controls.setSearchScope(1);
        controls.setReturningObjFlag(true);

        Object expectedObject = new Object();
        SearchResult searchResult = new SearchResult("", expectedObject,
                new BasicAttributes());

        setupStringSearchAndNamingEnumeration(controls, searchResult);

        Object expectedResult = expectedObject;
        contextMapperControl.expectAndReturn(contextMapperMock
                .mapFromContext(expectedObject), expectedResult);

        dirContextMock.close();

        replay();

        List list = tested.search("o=example.com", "(ou=somevalue)", 1,
                contextMapperMock);

        verify();

        assertNotNull(list);
        assertEquals(1, list.size());
        assertSame(expectedResult, list.get(0));
    }

    public void testSearch_ContextMapper_Default() throws NamingException {
        expectGetReadOnlyContext();

        SearchControls controls = new SearchControls();
        controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        controls.setReturningObjFlag(true);

        Object expectedObject = new Object();
        SearchResult searchResult = new SearchResult("", expectedObject,
                new BasicAttributes());

        setupSearchAndNamingEnumeration(controls, searchResult);

        Object expectedResult = expectedObject;
        contextMapperControl.expectAndReturn(contextMapperMock
                .mapFromContext(expectedObject), expectedResult);

        dirContextMock.close();

        replay();

        List list = tested
                .search(nameMock, "(ou=somevalue)", contextMapperMock);

        verify();

        assertNotNull(list);
        assertEquals(1, list.size());
        assertSame(expectedResult, list.get(0));
    }

    public void testSearch_String_ContextMapper_Default()
            throws NamingException {
        expectGetReadOnlyContext();

        SearchControls controls = new SearchControls();
        controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        controls.setReturningObjFlag(true);

        Object expectedObject = new Object();
        SearchResult searchResult = new SearchResult("", expectedObject,
                new BasicAttributes());

        setupStringSearchAndNamingEnumeration(controls, searchResult);

        Object expectedResult = expectedObject;
        contextMapperControl.expectAndReturn(contextMapperMock
                .mapFromContext(expectedObject), expectedResult);

        dirContextMock.close();

        replay();

        List list = tested.search("o=example.com", "(ou=somevalue)",
                contextMapperMock);

        verify();

        assertNotNull(list);
        assertEquals(1, list.size());
        assertSame(expectedResult, list.get(0));
    }

    public void testSearch_String_SearchControls_ContextMapper()
            throws Exception {
        expectGetReadOnlyContext();

        SearchControls controls = new SearchControls();
        controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        controls.setReturningObjFlag(true);

        Object expectedObject = new Object();
        SearchResult searchResult = new SearchResult("", expectedObject,
                new BasicAttributes());

        setupStringSearchAndNamingEnumeration(controls, searchResult);

        Object expectedResult = expectedObject;
        contextMapperControl.expectAndReturn(contextMapperMock
                .mapFromContext(expectedObject), expectedResult);

        dirContextMock.close();

        replay();

        List list = tested.search("o=example.com", "(ou=somevalue)", controls,
                contextMapperMock);

        verify();

        assertNotNull(list);
        assertEquals(1, list.size());
        assertSame(expectedResult, list.get(0));
    }

    public void testSearch_String_SearchControls_ContextMapper_ReturningObjFlagNotSet()
            throws Exception {
        expectGetReadOnlyContext();

        SearchControls controls = new SearchControls();
        controls.setSearchScope(SearchControls.SUBTREE_SCOPE);

        SearchControls expectedControls = new SearchControls();
        expectedControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        expectedControls.setReturningObjFlag(true);

        Object expectedObject = new Object();
        SearchResult searchResult = new SearchResult("", expectedObject,
                new BasicAttributes());

        setupStringSearchAndNamingEnumeration(expectedControls, searchResult);

        Object expectedResult = expectedObject;
        contextMapperControl.expectAndReturn(contextMapperMock
                .mapFromContext(expectedObject), expectedResult);

        dirContextMock.close();

        replay();

        List list = tested.search("o=example.com", "(ou=somevalue)", controls,
                contextMapperMock);

        verify();

        assertNotNull(list);
        assertEquals(1, list.size());
        assertSame(expectedResult, list.get(0));
    }

    public void testSearch_Name_SearchControls_ContextMapper() throws Exception {
        expectGetReadOnlyContext();

        SearchControls controls = new SearchControls();
        controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        controls.setReturningObjFlag(true);

        Object expectedObject = new Object();
        SearchResult searchResult = new SearchResult("", expectedObject,
                new BasicAttributes());

        setupSearchAndNamingEnumeration(controls, searchResult);

        Object expectedResult = expectedObject;
        contextMapperControl.expectAndReturn(contextMapperMock
                .mapFromContext(expectedObject), expectedResult);

        dirContextMock.close();

        replay();

        List list = tested.search(nameMock, "(ou=somevalue)", controls,
                contextMapperMock);

        verify();

        assertNotNull(list);
        assertEquals(1, list.size());
        assertSame(expectedResult, list.get(0));
    }

    public void testSearch_String_SearchControls_AttributesMapper()
            throws NamingException {
        expectGetReadOnlyContext();

        SearchControls controls = new SearchControls();
        controls.setSearchScope(1);
        controls.setReturningObjFlag(false);

        BasicAttributes expectedAttributes = new BasicAttributes();
        SearchResult searchResult = new SearchResult("", null,
                expectedAttributes);

        setupStringSearchAndNamingEnumeration(controls, searchResult);

        Object expectedResult = new Object();
        attributesMapperControl.expectAndReturn(attributesMapperMock
                .mapFromAttributes(expectedAttributes), expectedResult);

        dirContextMock.close();

        replay();

        List list = tested.search("o=example.com", "(ou=somevalue)", controls,
                attributesMapperMock);

        verify();

        assertNotNull(list);
        assertEquals(1, list.size());
        assertSame(expectedResult, list.get(0));
    }

    public void testSearch_Name_SearchControls_AttributesMapper()
            throws NamingException {
        expectGetReadOnlyContext();

        SearchControls controls = new SearchControls();
        controls.setSearchScope(1);
        controls.setReturningObjFlag(false);

        BasicAttributes expectedAttributes = new BasicAttributes();
        SearchResult searchResult = new SearchResult("", null,
                expectedAttributes);

        setupSearchAndNamingEnumeration(controls, searchResult);

        Object expectedResult = new Object();
        attributesMapperControl.expectAndReturn(attributesMapperMock
                .mapFromAttributes(expectedAttributes), expectedResult);

        dirContextMock.close();

        replay();

        List list = tested.search(nameMock, "(ou=somevalue)", controls,
                attributesMapperMock);

        verify();

        assertNotNull(list);
        assertEquals(1, list.size());
        assertSame(expectedResult, list.get(0));
    }

    public void testLookup() throws NamingException {
        expectGetReadOnlyContext();

        Object expected = new Object();
        dirContextControl.expectAndReturn(dirContextMock.lookup(nameMock),
                expected);

        dirContextMock.close();

        replay();

        Object actual = tested.lookup(nameMock);

        verify();

        assertSame(expected, actual);
    }

    public void testLookup_String() throws NamingException {
        expectGetReadOnlyContext();

        Object expected = new Object();
        dirContextControl.expectAndReturn(dirContextMock
                .lookup("o=example.com"), expected);

        dirContextMock.close();

        replay();

        Object actual = tested.lookup("o=example.com");

        verify();

        assertSame(expected, actual);
    }

    public void testLookup_NamingException() throws NamingException {
        expectGetReadOnlyContext();

        NamingException ne = new NamingException();
        dirContextControl.expectAndThrow(dirContextMock.lookup(nameMock), ne);

        dirContextMock.close();

        exceptionTranslatorControl.expectAndReturn(exceptionTranslatorMock
                .translate(ne), new EntryNotFoundException("dummy"));

        replay();

        try {
            tested.lookup(nameMock);
            fail("EntryNotFoundException expected");
        } catch (EntryNotFoundException expected) {
            assertTrue(true);
        }

        verify();
    }

    public void testLookup_AttributesMapper() throws Exception {
        expectGetReadOnlyContext();

        BasicAttributes expectedAttributes = new BasicAttributes();
        dirContextControl.expectAndReturn(dirContextMock
                .getAttributes(nameMock), expectedAttributes);
        dirContextMock.close();

        Object expected = new Object();
        attributesMapperControl.expectAndReturn(attributesMapperMock
                .mapFromAttributes(expectedAttributes), expected);

        replay();

        Object actual = tested.lookup(nameMock, attributesMapperMock);

        verify();

        assertSame(expected, actual);
    }

    public void testLookup_String_AttributesMapper() throws Exception {
        expectGetReadOnlyContext();

        BasicAttributes expectedAttributes = new BasicAttributes();
        dirContextControl.expectAndReturn(dirContextMock
                .getAttributes("o=example.com"), expectedAttributes);
        dirContextMock.close();

        Object expected = new Object();
        attributesMapperControl.expectAndReturn(attributesMapperMock
                .mapFromAttributes(expectedAttributes), expected);

        replay();

        Object actual = tested.lookup("o=example.com", attributesMapperMock);

        verify();

        assertSame(expected, actual);
    }

    public void testLookup_AttributesMapper_NamingException() throws Exception {
        expectGetReadOnlyContext();

        NamingException ne = new NamingException();
        dirContextControl.expectAndThrow(
                dirContextMock.getAttributes(nameMock), ne);
        dirContextMock.close();

        exceptionTranslatorControl.expectAndReturn(exceptionTranslatorMock
                .translate(ne), new EntryNotFoundException("dummy"));

        replay();

        try {
            tested.lookup(nameMock, attributesMapperMock);
            fail("EntryNotFoundException expected");
        } catch (EntryNotFoundException expected) {
            assertTrue(true);
        }

        verify();
    }

    public void testLookup_ContextMapper() throws Exception {
        expectGetReadOnlyContext();

        Object transformed = new Object();
        Object expected = new Object();
        dirContextControl.expectAndReturn(dirContextMock.lookup(nameMock),
                expected);

        dirContextMock.close();

        contextMapperControl.expectAndReturn(contextMapperMock
                .mapFromContext(expected), transformed);

        replay();

        Object actual = tested.lookup(nameMock, contextMapperMock);

        verify();

        assertSame(transformed, actual);
    }

    public void testLookup_String_ContextMapper() throws Exception {
        expectGetReadOnlyContext();

        Object transformed = new Object();
        Object expected = new Object();
        dirContextControl.expectAndReturn(dirContextMock
                .lookup("o=example.com"), expected);

        dirContextMock.close();

        contextMapperControl.expectAndReturn(contextMapperMock
                .mapFromContext(expected), transformed);

        replay();

        Object actual = tested.lookup("o=example.com", contextMapperMock);

        verify();

        assertSame(transformed, actual);
    }

    public void testLookup_ContextMapper_NamingException() throws Exception {
        expectGetReadOnlyContext();

        NameNotFoundException ne = new NameNotFoundException();
        dirContextControl.expectAndThrow(dirContextMock.lookup(nameMock), ne);

        dirContextMock.close();

        exceptionTranslatorControl.expectAndReturn(exceptionTranslatorMock
                .translate(ne), new EntryNotFoundException("dummy"));

        replay();

        try {
            tested.lookup(nameMock, contextMapperMock);
            fail("EntryNotFoundException expected");
        } catch (EntryNotFoundException expected) {
            assertTrue(true);
        }

        verify();
    }

    public void testModifyAttributes() throws Exception {
        expectGetReadWriteContext();

        ModificationItem[] mods = new ModificationItem[0];
        dirContextMock.modifyAttributes(nameMock, mods);

        dirContextMock.close();

        replay();

        tested.modifyAttributes(nameMock, mods);

        verify();
    }

    public void testModifyAttributes_String() throws Exception {
        expectGetReadWriteContext();

        ModificationItem[] mods = new ModificationItem[0];
        dirContextMock.modifyAttributes("o=example.com", mods);

        dirContextMock.close();

        replay();

        tested.modifyAttributes("o=example.com", mods);

        verify();
    }

    public void testModifyAttributes_NamingException() throws Exception {
        expectGetReadWriteContext();

        ModificationItem[] mods = new ModificationItem[0];
        dirContextMock.modifyAttributes(nameMock, mods);
        NamingException ne = new NamingException();
        dirContextControl.setThrowable(ne);

        exceptionTranslatorControl.expectAndReturn(exceptionTranslatorMock
                .translate(ne), new EntryNotFoundException("dummy"));

        dirContextMock.close();

        replay();

        try {
            tested.modifyAttributes(nameMock, mods);
            fail("EntryNotFoundException expected");
        } catch (EntryNotFoundException expected) {
            assertTrue(true);
        }

        verify();
    }

    public void testBind() throws Exception {
        expectGetReadWriteContext();

        Object expectedObject = new Object();
        BasicAttributes expectedAttributes = new BasicAttributes();
        dirContextMock.bind(nameMock, expectedObject, expectedAttributes);
        dirContextMock.close();

        replay();

        tested.bind(nameMock, expectedObject, expectedAttributes);

        verify();

    }

    public void testBind_String() throws Exception {
        expectGetReadWriteContext();

        Object expectedObject = new Object();
        BasicAttributes expectedAttributes = new BasicAttributes();
        dirContextMock
                .bind("o=example.com", expectedObject, expectedAttributes);
        dirContextMock.close();

        replay();

        tested.bind("o=example.com", expectedObject, expectedAttributes);

        verify();

    }

    public void testBind_NamingException() throws Exception {
        expectGetReadWriteContext();

        Object expectedObject = new Object();
        BasicAttributes expectedAttributes = new BasicAttributes();
        dirContextMock.bind(nameMock, expectedObject, expectedAttributes);
        NamingException ne = new NamingException();
        dirContextControl.setThrowable(ne);
        dirContextMock.close();

        exceptionTranslatorControl.expectAndReturn(exceptionTranslatorMock
                .translate(ne), new EntryNotFoundException("dummy"));

        replay();

        try {
            tested.bind(nameMock, expectedObject, expectedAttributes);
            fail("EntryNotFoundException expected");
        } catch (EntryNotFoundException expected) {
            assertTrue(true);
        }

        verify();

    }

    public void testUnbind() throws Exception {
        expectGetReadWriteContext();

        dirContextMock.unbind(nameMock);
        dirContextMock.close();
        replay();

        tested.unbind(nameMock);

        verify();
    }

    public void testUnbind_String() throws Exception {
        expectGetReadWriteContext();

        dirContextMock.unbind("o=example.com");
        dirContextMock.close();
        replay();

        tested.unbind("o=example.com");

        verify();
    }

    public void testUnbindRecursive() throws Exception {
        expectGetReadWriteContext();

        namingEnumerationControl.expectAndReturn(namingEnumerationMock
                .hasMore(), true);
        Binding binding = new Binding("cn=Some name", null);
        namingEnumerationControl.expectAndReturn(namingEnumerationMock.next(),
                binding);
        namingEnumerationControl.expectAndReturn(namingEnumerationMock
                .hasMore(), false);
        namingEnumerationMock.close();

        namingEnumerationControl.expectAndReturn(namingEnumerationMock
                .hasMore(), false);
        namingEnumerationMock.close();

        dirContextMock.listBindings(new DistinguishedName("o=example.com"));
        dirContextControl.setReturnValue(namingEnumerationMock);
        dirContextMock.listBindings(new DistinguishedName(
                "cn=Some name, o=example.com"));
        dirContextControl.setReturnValue(namingEnumerationMock);

        dirContextMock.unbind(new DistinguishedName(
                "cn=Some name, o=example.com"));
        dirContextMock.unbind(new DistinguishedName("o=example.com"));
        dirContextMock.close();
        replay();

        tested.unbind("o=example.com", true);

        verify();
    }

    public void testRebind() throws NamingException {
        expectGetReadWriteContext();

        Object expectedObject = new Object();
        BasicAttributes expectedAttributes = new BasicAttributes();
        dirContextMock.rebind(nameMock, expectedObject, expectedAttributes);

        dirContextMock.close();

        replay();

        tested.rebind(nameMock, expectedObject, expectedAttributes);

        verify();
    }

    public void testRebind_String() throws NamingException {
        expectGetReadWriteContext();

        Object expectedObject = new Object();
        BasicAttributes expectedAttributes = new BasicAttributes();
        dirContextMock.rebind("o=example.com", expectedObject,
                expectedAttributes);

        dirContextMock.close();

        replay();

        tested.rebind("o=example.com", expectedObject, expectedAttributes);

        verify();
    }

    public void testUnbind_NamingException() throws Exception {
        expectGetReadWriteContext();

        dirContextMock.unbind(nameMock);
        NamingException ne = new NamingException();
        dirContextControl.setThrowable(ne);
        dirContextMock.close();

        exceptionTranslatorControl.expectAndReturn(exceptionTranslatorMock
                .translate(ne), new EntryNotFoundException("dummy"));

        replay();

        try {
            tested.unbind(nameMock);
            fail("EntryNotFoundException expected");
        } catch (EntryNotFoundException expected) {
            assertTrue(true);
        }

        verify();
    }

    public void testExecuteReadOnly() throws Exception {
        expectGetReadOnlyContext();

        Object object = new Object();
        contextExecutorControl.expectAndReturn(contextExecutorMock
                .executeWithContext(dirContextMock), object);

        dirContextMock.close();

        replay();

        Object result = tested.executeReadOnly(contextExecutorMock);

        verify();

        assertSame(object, result);
    }

    public void testExecuteReadOnly_NamingException() throws Exception {
        expectGetReadOnlyContext();

        NamingException ne = new NamingException();
        contextExecutorControl.expectAndThrow(contextExecutorMock
                .executeWithContext(dirContextMock), ne);

        dirContextMock.close();

        exceptionTranslatorControl.expectAndReturn(exceptionTranslatorMock
                .translate(ne), new EntryNotFoundException("dummy"));

        replay();

        try {
            tested.executeReadOnly(contextExecutorMock);
            fail("EntryNotFoundException expected");
        } catch (EntryNotFoundException expected) {
            assertTrue(true);
        }

        verify();
    }

    public void testExecuteReadWrite() throws Exception {
        expectGetReadWriteContext();

        Object object = new Object();
        contextExecutorControl.expectAndReturn(contextExecutorMock
                .executeWithContext(dirContextMock), object);

        dirContextMock.close();

        replay();

        Object result = tested.executeReadWrite(contextExecutorMock);

        verify();

        assertSame(object, result);
    }

    public void testExecuteReadWrite_NamingException() throws Exception {
        expectGetReadWriteContext();

        NamingException ne = new NamingException();
        contextExecutorControl.expectAndThrow(contextExecutorMock
                .executeWithContext(dirContextMock), ne);

        dirContextMock.close();

        exceptionTranslatorControl.expectAndReturn(exceptionTranslatorMock
                .translate(ne), new EntryNotFoundException("dummy"));

        replay();

        try {
            tested.executeReadWrite(contextExecutorMock);
            fail("EntryNotFoundException expected");
        } catch (EntryNotFoundException expected) {
            assertTrue(true);
        }

        verify();
    }

    public void testDoSearch() throws Exception {
        expectGetReadOnlyContext();

        SearchResult searchResult = new SearchResult(null, null, null);

        searchExecutorControl.expectAndReturn(searchExecutorMock
                .executeSearch(dirContextMock), namingEnumerationMock);

        namingEnumerationControl.expectAndReturn(namingEnumerationMock
                .hasMore(), true);
        namingEnumerationControl.expectAndReturn(namingEnumerationMock.next(),
                searchResult);
        namingEnumerationControl.expectAndReturn(namingEnumerationMock
                .hasMore(), false);
        namingEnumerationMock.close();

        handlerMock.handleSearchResult(searchResult);

        dirContextMock.close();

        replay();

        tested.search(searchExecutorMock, handlerMock);

        verify();
    }

    public void testDoSearch_NamingException() throws Exception {
        expectGetReadOnlyContext();

        NamingException ne = new NamingException();
        searchExecutorControl.expectAndThrow(searchExecutorMock
                .executeSearch(dirContextMock), ne);

        dirContextMock.close();

        exceptionTranslatorControl.expectAndReturn(exceptionTranslatorMock
                .translate(ne), new EntryNotFoundException("dummy"));

        replay();

        try {
            tested.search(searchExecutorMock, handlerMock);
            fail("EntryNotFoundException expected");
        } catch (EntryNotFoundException expected) {
            assertTrue(true);
        }

        verify();
    }

    public void testDoSearch_NamingException_NamingEnumeration()
            throws Exception {
        expectGetReadOnlyContext();

        searchExecutorControl.expectAndReturn(searchExecutorMock
                .executeSearch(dirContextMock), namingEnumerationMock);

        NamingException ne = new NamingException();
        namingEnumerationControl.expectAndThrow(
                namingEnumerationMock.hasMore(), ne);
        namingEnumerationMock.close();

        dirContextMock.close();

        exceptionTranslatorControl.expectAndReturn(exceptionTranslatorMock
                .translate(ne), new EntryNotFoundException("dummy"));

        replay();

        try {
            tested.search(searchExecutorMock, handlerMock);
            fail("EntryNotFoundException expected");
        } catch (EntryNotFoundException expected) {
            assertTrue(true);
        }

        verify();
    }

    public void testDoSearch_NameNotFoundException() throws Exception {
        expectGetReadOnlyContext();

        searchExecutorControl.expectAndThrow(searchExecutorMock
                .executeSearch(dirContextMock), new NameNotFoundException());
        dirContextMock.close();

        replay();

        tested.search(searchExecutorMock, handlerMock);

        verify();
    }

    public void testSearch_PartialResult_IgnoreNotSet() throws NamingException {
        expectGetReadOnlyContext();

        PartialResultException ex = new PartialResultException();
        searchExecutorControl.expectAndThrow(searchExecutorMock
                .executeSearch(dirContextMock), ex);
        dirContextMock.close();

        exceptionTranslatorControl.expectAndReturn(exceptionTranslatorMock
                .translate(ex), new EntryNotFoundException("dummy"));

        replay();

        try {
            tested.search(searchExecutorMock, handlerMock);
            fail("EntryNotFoundException expected");
        } catch (EntryNotFoundException expected) {
            assertTrue(true);
        }

        verify();
    }

    public void testSearch_PartialResult_IgnoreSet() throws NamingException {
        tested.setIgnorePartialResultException(true);

        expectGetReadOnlyContext();

        searchExecutorControl.expectAndThrow(searchExecutorMock
                .executeSearch(dirContextMock), new PartialResultException());
        dirContextMock.close();

        replay();

        tested.search(searchExecutorMock, handlerMock);

        verify();
    }

    /**
     * Needed to verify search control values.
     * 
     * @author Mattias Arthursson
     */
    private static class SearchControlsMatcher extends AbstractMatcher {
        protected boolean argumentMatches(Object arg0, Object arg1) {
            if (arg0 instanceof SearchControls
                    && arg1 instanceof SearchControls) {
                SearchControls s0 = (SearchControls) arg0;
                SearchControls s1 = (SearchControls) arg1;

                return s0.getSearchScope() == s1.getSearchScope()
                        && s0.getReturningObjFlag() == s1.getReturningObjFlag()
                        && s0.getDerefLinkFlag() == s1.getDerefLinkFlag()
                        && s0.getCountLimit() == s1.getCountLimit()
                        && s0.getTimeLimit() == s1.getTimeLimit();
            } else {
                return super.argumentMatches(arg0, arg1);
            }
        }
    }
}
