/*
 * Copyright 2006 the original author or authors.
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

package org.springframework.ws.soap.saaj.support;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.Name;
import javax.xml.soap.SOAPMessage;

import org.custommonkey.xmlunit.XMLTestCase;
import org.w3c.dom.Document;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

public class SaajUtilsTest extends XMLTestCase {

    private MessageFactory messageFactory;

    protected void setUp() throws Exception {
        messageFactory = MessageFactory.newInstance();
    }

    public void testToName() throws Exception {
        SOAPMessage message = messageFactory.createMessage();
        QName qName = new QName("localPart");
        Name name = SaajUtils.toName(qName, message.getSOAPPart().getEnvelope());
        assertNotNull("Invalid name", name);
        assertEquals("Invalid local part", qName.getLocalPart(), name.getLocalName());
        assertFalse("Invalid prefix", StringUtils.hasLength(name.getPrefix()));
        assertFalse("Invalid namespace", StringUtils.hasLength(name.getURI()));
    }

    public void testToNameNamespacePrefix() throws Exception {
        SOAPMessage message = messageFactory.createMessage();
        QName qName = new QName("namespace", "localPart", "prefix");
        Name name = SaajUtils.toName(qName, message.getSOAPPart().getEnvelope());
        assertNotNull("Invalid name", name);
        assertEquals("Invalid namespace", qName.getNamespaceURI(), name.getURI());
        assertEquals("Invalid local part", qName.getLocalPart(), name.getLocalName());
        assertEquals("Invalid prefix", qName.getPrefix(), name.getPrefix());
    }

    public void testToQName() throws Exception {
        SOAPMessage message = messageFactory.createMessage();
        Name name = message.getSOAPPart().getEnvelope().createName("localPart", null, null);
        QName qName = SaajUtils.toQName(name);
        assertNotNull("Invalid qName", qName);
        assertEquals("Invalid namespace", name.getURI(), qName.getNamespaceURI());
        assertFalse("Invalid prefix", StringUtils.hasLength(qName.getPrefix()));
        assertFalse("Invalid namespace", StringUtils.hasLength(qName.getNamespaceURI()));
    }

    public void testToQNameNamespace() throws Exception {
        SOAPMessage message = messageFactory.createMessage();
        Name name = message.getSOAPPart().getEnvelope().createName("localPart", null, "namespace");
        QName qName = SaajUtils.toQName(name);
        assertNotNull("Invalid qName", qName);
        assertEquals("Invalid namespace", name.getURI(), qName.getNamespaceURI());
        assertEquals("Invalid local part", name.getLocalName(), qName.getLocalPart());
        assertFalse("Invalid prefix", StringUtils.hasLength(qName.getPrefix()));
    }

    public void testToQNamePrefixNamespace() throws Exception {
        SOAPMessage message = messageFactory.createMessage();
        Name name = message.getSOAPPart().getEnvelope().createName("localPart", "prefix", "namespace");
        QName qName = SaajUtils.toQName(name);
        assertNotNull("Invalid qName", qName);
        assertEquals("Invalid namespace", name.getURI(), qName.getNamespaceURI());
        assertEquals("Invalid local part", name.getLocalName(), qName.getLocalPart());
        assertEquals("Invalid prefix", name.getPrefix(), qName.getPrefix());
    }

    public void testLoadMessage() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(getClass().getResourceAsStream("soapMessage.xml"));
        SOAPMessage soapMessage =
                SaajUtils.loadMessage(new ClassPathResource("soapMessage.xml", getClass()), messageFactory);
        assertXMLEqual(soapMessage.getSOAPPart(), document);
    }
}