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

package org.springframework.ws.transport.http;

import java.net.URI;

import org.apache.commons.httpclient.ConnectTimeoutException;

import org.springframework.ws.MockWebServiceMessage;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.transport.WebServiceConnection;

public class CommonsHttpMessageSenderIntegrationTest extends AbstractHttpWebServiceMessageSenderIntegrationTestCase {

    protected AbstractHttpWebServiceMessageSender createMessageSender() {
        return new CommonsHttpMessageSender();
    }

    public void testConnectionTimeout() throws Exception {
        CommonsHttpMessageSender messageSender = new CommonsHttpMessageSender();
        messageSender.setConnectionTimeout(1);
        WebServiceConnection connection = messageSender.createConnection(new URI("http://example.com/"));
        WebServiceMessage message = new MockWebServiceMessage();
        try {
            connection.send(message);
            fail("ConnectTimeoutException expected");
        }
        catch (ConnectTimeoutException ex) {
            // expected
        }
    }

}
