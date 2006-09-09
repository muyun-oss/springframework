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

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.Name;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;
import org.springframework.xml.namespace.QNameUtils;
import org.w3c.dom.Element;

/**
 * Collection of generic utility methods to work with SAAJ. Includes conversion from <code>Name</code>s to
 * <code>QName</code>s and vice-versa, and SAAJ version checking.
 *
 * @author Arjen Poutsma
 * @see Name
 * @see QName
 */
public abstract class SaajUtils {

    public static final int SAAJ_11 = 0;

    public static final int SAAJ_12 = 1;

    public static final int SAAJ_13 = 2;

    private static final String SAAJ_13_CLASS_NAME = "javax.xml.soap.SAAJMetaFactory";

    private static int saajVersion = SAAJ_12;

    static {
        try {
            Class.forName(SAAJ_13_CLASS_NAME);
            saajVersion = SAAJ_13;
        }
        catch (ClassNotFoundException ex) {
            if (Element.class.isAssignableFrom(SOAPElement.class)) {
                saajVersion = SAAJ_12;
            }
            else {
                saajVersion = SAAJ_11;
            }
        }
    }

    /**
     * Gets the SAAJ version.
     *
     * @return a code comparable to the SAAJ_XX codes in this class
     * @see #SAAJ_12
     * @see #SAAJ_13
     */
    public static int getSaajVersion() {
        return saajVersion;
    }

    /**
     * Converts a <code>javax.xml.namespace.QName</code> to a <code>javax.xml.soap.Name</code>. A
     * <code>SOAPEnvelope</code> is required to create the name.
     *
     * @param qName          the <code>QName</code> to convert
     * @param resolveElement a <code>SOAPElement</code> used to resolve namespaces to prefixes
     * @param envelope       a <code>SOAPEnvelope</code> necessary to create the <code>Name</code>
     * @return the converted SAAJ Name
     * @throws SOAPException            if conversion is unsuccessful
     * @throws IllegalArgumentException if <code>qName</code> is not fully qualified
     */
    public static Name toName(QName qName, SOAPElement resolveElement, SOAPEnvelope envelope) throws SOAPException {
        String qNamePrefix = QNameUtils.getPrefix(qName);
        if (StringUtils.hasLength(qName.getNamespaceURI()) && StringUtils.hasLength(qNamePrefix)) {
            return envelope.createName(qName.getLocalPart(), qNamePrefix, qName.getNamespaceURI());
        }
        else if (StringUtils.hasLength(qName.getNamespaceURI())) {
            Iterator prefixes = resolveElement.getVisibleNamespacePrefixes();
            while (prefixes.hasNext()) {
                String prefix = (String) prefixes.next();
                if (qName.getNamespaceURI().equals(resolveElement.getNamespaceURI(prefix))) {
                    return envelope.createName(qName.getLocalPart(), prefix, qName.getNamespaceURI());
                }
            }
            throw new IllegalArgumentException("Could not resolve namespace of QName [" + qName + "]");
        }
        else {
            return envelope.createName(qName.getLocalPart());
        }
    }

    /**
     * Converts a <code>javax.xml.soap.Name</code> to a <code>javax.xml.namespace.QName</code>.
     *
     * @param name the <code>Name</code> to convert
     * @return the converted <code>QName</code>
     */
    public static QName toQName(Name name) {
        if (StringUtils.hasLength(name.getURI()) && StringUtils.hasLength(name.getPrefix())) {
            return QNameUtils.createQName(name.getURI(), name.getLocalName(), name.getPrefix());
        }
        else if (StringUtils.hasLength(name.getURI())) {
            return new QName(name.getURI(), name.getLocalName());
        }
        else {
            return new QName(name.getLocalName());
        }
    }

    /**
     * Loads a SAAJ <code>SOAPMessage</code> from the given resource.
     *
     * @param resource the resource to read from
     * @return the loaded SAAJ message
     * @throws SOAPException if the message cannot be constructed
     * @throws IOException   if the input stream resource cannot be loaded
     */
    public static SOAPMessage loadMessage(Resource resource) throws SOAPException, IOException {
        return loadMessage(resource, MessageFactory.newInstance());
    }

    /**
     * Loads a SAAJ <code>SOAPMessage</code> from the given resource with a given message factory.
     *
     * @param resource       the resource to read from
     * @param messageFactory SAAJ message factory used to construct the message
     * @return the loaded SAAJ message
     * @throws SOAPException if the message cannot be constructed
     * @throws IOException   if the input stream resource cannot be loaded
     */
    public static SOAPMessage loadMessage(Resource resource, MessageFactory messageFactory)
            throws SOAPException, IOException {
        InputStream is = resource.getInputStream();
        try {
            MimeHeaders mimeHeaders = new MimeHeaders();
            mimeHeaders.addHeader("Content-Type", "text/xml");
            mimeHeaders.addHeader("Content-Length", Long.toString(resource.getFile().length()));
            return messageFactory.createMessage(mimeHeaders, is);
        }
        finally {
            is.close();
        }
    }
}
