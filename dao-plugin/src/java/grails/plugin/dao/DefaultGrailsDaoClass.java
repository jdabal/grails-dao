/*
 * Copyright 2004-2005 the original author or authors.
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
package grails.plugin.dao;

import org.codehaus.groovy.grails.commons.AbstractInjectableGrailsClass;

/**
 * @author Joshua Burnett
 */
public class DefaultGrailsDaoClass extends AbstractInjectableGrailsClass implements GrailsDaoClass {

    public static final String DAO = "Dao";
    private static final String TRANSACTIONAL = "transactional";

    private boolean transactional = true;

    public DefaultGrailsDaoClass(Class<?> clazz) {
        super(clazz, DAO);

        Object tmpTransactional = getPropertyOrStaticPropertyOrFieldValue(TRANSACTIONAL, Boolean.class);
        transactional = tmpTransactional == null || tmpTransactional.equals(Boolean.TRUE);
    }

    public boolean isTransactional() {
        return transactional;
    }
}
