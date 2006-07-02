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
package org.springframework.ldap.samples.person.dao;

import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;
import net.sf.ldaptemplate.ContextMapper;
import net.sf.ldaptemplate.LdapOperations;
import net.sf.ldaptemplate.support.DirContextOperations;
import net.sf.ldaptemplate.support.DistinguishedName;

import org.easymock.MockControl;
import org.springframework.ldap.samples.person.dao.PersonDaoImpl;
import org.springframework.ldap.samples.person.domain.Person;

/**
 * Unit tests for the PersonDaoImpl class.
 * 
 * @author Mattias Arthursson
 * @author Ulrik Sandberg
 */
public class PersonDaoImplTest extends TestCase {

    private MockControl ldapOperationsControl;

    private LdapOperations ldapOperationsMock;

    private MockControl dirContextOperationsControl;

    private DirContextOperations dirContextOperationsMock;

    private MockControl contextMapperControl;

    private ContextMapper contextMapperMock;

    private PersonDaoImpl tested;

    private Person person;

    protected void setUp() throws Exception {
        super.setUp();
        ldapOperationsControl = MockControl.createControl(LdapOperations.class);
        ldapOperationsMock = (LdapOperations) ldapOperationsControl.getMock();

        dirContextOperationsControl = MockControl
                .createControl(DirContextOperations.class);
        dirContextOperationsMock = (DirContextOperations) dirContextOperationsControl
                .getMock();

        contextMapperControl = MockControl.createControl(ContextMapper.class);
        contextMapperMock = (ContextMapper) contextMapperControl.getMock();

        person = new Person();

        tested = new PersonDaoImpl() {
            DirContextOperations getContextToBind(Person p) {
                assertSame(person, p);
                return dirContextOperationsMock;
            }

            DistinguishedName buildDn(Person p) {
                assertSame(person, p);
                return DistinguishedName.EMPTY_PATH;
            }

            ContextMapper getContextMapper() {
                return contextMapperMock;
            }
        };
        tested.setLdapOperations(ldapOperationsMock);
    }

    protected void tearDown() throws Exception {
        super.tearDown();

        ldapOperationsControl = null;
        ldapOperationsMock = null;

        dirContextOperationsControl = null;
        dirContextOperationsMock = null;

        contextMapperControl = null;
        contextMapperMock = null;

        person = null;
        tested = null;
    }

    protected void replay() {
        ldapOperationsControl.replay();
        dirContextOperationsControl.replay();
        contextMapperControl.replay();
    }

    protected void verify() {
        ldapOperationsControl.verify();
        dirContextOperationsControl.verify();
        contextMapperControl.verify();
    }

    public void testBuildDn() {
        tested = new PersonDaoImpl();
        Person person = new Person();
        person.setCountry("Sweden");
        person.setCompany("Some company");
        person.setFullName("Some Person");

        DistinguishedName dn = tested.buildDn(person);

        assertEquals("cn=Some Person, ou=Some company, c=Sweden", dn.toString());
    }

    /*
     * Test method for
     * 'org.springframework.ldap.samples.person.dao.PersonDaoImpl.create(Person)'
     */
    public void testCreate() {
        ldapOperationsMock.bind(DistinguishedName.EMPTY_PATH,
                dirContextOperationsMock, null);

        replay();

        tested.create(person);

        verify();
    }

    /*
     * Test method for
     * 'org.springframework.ldap.samples.person.dao.PersonDaoImpl.update(Person)'
     */
    public void testUpdate() {
        ldapOperationsMock.rebind(DistinguishedName.EMPTY_PATH,
                dirContextOperationsMock, null);

        replay();

        tested.update(person);

        verify();

    }

    /*
     * Test method for
     * 'org.springframework.ldap.samples.person.dao.PersonDaoImpl.delete(Person)'
     */
    public void testDelete() {
        ldapOperationsMock.unbind(DistinguishedName.EMPTY_PATH);

        replay();

        tested.delete(person);

        verify();
    }

    /*
     * Test method for
     * 'org.springframework.ldap.samples.person.dao.PersonDaoImpl.findAll()'
     */
    public void testFindAll() {
        List expectedList = Collections.singletonList(null);
        ldapOperationsControl.expectAndReturn(ldapOperationsMock.search(
                DistinguishedName.EMPTY_PATH, "(objectclass=person)",
                contextMapperMock), expectedList);

        replay();

        List result = tested.findAll();

        verify();

        assertSame(expectedList, result);
    }

    /*
     * Test method for
     * 'org.springframework.ldap.samples.person.dao.PersonDaoImpl.findByPrimaryKey(String,
     * String, String)'
     */
    public void testFindByPrimaryKey() {
        DistinguishedName dn = new DistinguishedName(
                "cn=Some Person, ou=Some company, c=Sweden");

        ldapOperationsControl.expectAndReturn(ldapOperationsMock.lookup(dn,
                contextMapperMock), person);

        replay();

        Person result = tested.findByPrimaryKey("Sweden", "Some company",
                "Some Person");

        verify();

        assertSame(person, result);

    }

}
