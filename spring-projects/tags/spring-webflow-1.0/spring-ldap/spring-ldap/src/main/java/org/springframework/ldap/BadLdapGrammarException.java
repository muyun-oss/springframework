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

import org.springframework.dao.InvalidDataAccessResourceUsageException;

/**
 * Thrown to indicate that an invalid value has been supplied to an LDAP
 * operation. This could be an invalid filter or dn.
 * 
 * @author Mattias Arthursson
 */
public class BadLdapGrammarException extends
        InvalidDataAccessResourceUsageException {

    private static final long serialVersionUID = 961612585331409470L;

    public BadLdapGrammarException(String message) {
        super(message);
    }

    public BadLdapGrammarException(String message, Throwable cause) {
        super(message, cause);
    }
}
