/*
 * Copyright 2007 the original author or authors.
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

package org.springframework.ws.transport.jms;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Iterator;
import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.springframework.jms.support.JmsUtils;
import org.springframework.util.Assert;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.transport.AbstractReceiverConnection;
import org.springframework.ws.transport.WebServiceConnection;
import org.springframework.ws.transport.support.EnumerationIterator;

/**
 * Implementation of {@link WebServiceConnection} that is used for server-side JMS access. Exposes a {@link
 * BytesMessage} or {@link TextMessage} request and response message.
 * <p/>
 * The response message type is equal to the request message type, i.e. if a <code>BytesMessage</code> is received as
 * request, a <code>BytesMessage</code> is created as response, and if a <code>TextMessage</code> is received, a
 * <code>TextMessage</code> response is created.
 *
 * @author Arjen Poutsma
 * @since 1.5.0
 */
public class JmsReceiverConnection extends AbstractReceiverConnection {

    private final Message requestMessage;

    private final Session session;

    private Message responseMessage;

    private String textMessageEncoding;

    private JmsReceiverConnection(Message requestMessage, Session session) {
        Assert.notNull(requestMessage, "requestMessage must not be null");
        Assert.notNull(session, "session must not be null");
        this.requestMessage = requestMessage;
        this.session = session;
    }

    /**
     * Constructs a new JMS connection with the given {@link BytesMessage}.
     *
     * @param requestMessage the JMS request message
     * @param session        the JMS session
     */
    protected JmsReceiverConnection(BytesMessage requestMessage, Session session) {
        this((Message) requestMessage, session);
    }

    /**
     * Constructs a new JMS connection with the given {@link TextMessage}.
     *
     * @param requestMessage the JMS request message
     * @param session        the JMS session
     */
    protected JmsReceiverConnection(TextMessage requestMessage, String encoding, Session session) {
        this(requestMessage, session);
        this.textMessageEncoding = encoding;
    }

    /** Returns the request message for this connection. Returns either a {@link BytesMessage} or a {@link TextMessage}. */
    public Message getRequestMessage() {
        return requestMessage;
    }

    /**
     * Returns the response message, if any, for this connection. Returns either a {@link BytesMessage} or a {@link
     * TextMessage}.
     */
    public Message getResponseMessage() {
        return responseMessage;
    }

    /*
     * Errors
     */

    public String getErrorMessage() throws IOException {
        return null;
    }

    public boolean hasError() throws IOException {
        return false;
    }

    /*
     * Receiving
     */

    protected Iterator getRequestHeaderNames() throws IOException {
        try {
            return new EnumerationIterator(requestMessage.getPropertyNames());
        }
        catch (JMSException ex) {
            throw new JmsTransportException("Could not get property names", ex);
        }
    }

    protected Iterator getRequestHeaders(String name) throws IOException {
        try {
            String value = requestMessage.getStringProperty(name);
            return Collections.singletonList(value).iterator();
        }
        catch (JMSException ex) {
            throw new JmsTransportException("Could not get property value", ex);
        }
    }

    protected InputStream getRequestInputStream() throws IOException {
        if (requestMessage instanceof BytesMessage) {
            return new BytesMessageInputStream((BytesMessage) requestMessage);
        }
        else {
            TextMessage textMessage = (TextMessage) requestMessage;
            try {
                String text = textMessage.getText();
                byte[] contents = text != null ? text.getBytes(textMessageEncoding) : new byte[0];
                return new ByteArrayInputStream(contents);
            }
            catch (JMSException ex) {
                throw new JmsTransportException(ex);
            }
        }
    }

    /*
     * Sending
     */

    protected void onSendBeforeWrite(WebServiceMessage message) throws IOException {
        try {
            if (requestMessage instanceof BytesMessage) {
                responseMessage = session.createBytesMessage();
            }
            else {
                responseMessage = session.createTextMessage();
            }
            responseMessage.setJMSCorrelationID(requestMessage.getJMSMessageID());
        }
        catch (JMSException ex) {
            throw new JmsTransportException("Could not create response message", ex);
        }
    }

    protected void addResponseHeader(String name, String value) throws IOException {
        try {
            responseMessage.setStringProperty(name, value);
        }
        catch (JMSException ex) {
            throw new JmsTransportException("Could not set property", ex);
        }
    }

    protected OutputStream getResponseOutputStream() throws IOException {
        if (responseMessage instanceof BytesMessage) {
            return new BytesMessageOutputStream((BytesMessage) responseMessage);
        }
        else {
            return new ByteArrayOutputStream() {

                public void close() throws IOException {
                    String text = new String(toByteArray(), textMessageEncoding);
                    try {
                        ((TextMessage) responseMessage).setText(text);
                    }
                    catch (JMSException ex) {
                        throw new JmsTransportException(ex);
                    }
                }
            };
        }
    }

    protected void onSendAfterWrite(WebServiceMessage message) throws IOException {
        MessageProducer messageProducer = null;
        try {
            if (requestMessage.getJMSReplyTo() != null) {
                messageProducer = session.createProducer(requestMessage.getJMSReplyTo());
                messageProducer.setDeliveryMode(requestMessage.getJMSDeliveryMode());
                messageProducer.setPriority(requestMessage.getJMSPriority());
                messageProducer.send(responseMessage);
            }
        }
        catch (JMSException ex) {
            throw new JmsTransportException(ex);
        }
        finally {
            JmsUtils.closeMessageProducer(messageProducer);
        }
    }

}
