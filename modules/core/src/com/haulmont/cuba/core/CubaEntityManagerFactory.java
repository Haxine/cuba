/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Konstantin Krivopustov
 * Created: 31.10.2008 16:55:55
 * $Id$
 */
package com.haulmont.cuba.core;

import org.apache.openjpa.persistence.OpenJPAEntityManagerFactory;
import org.apache.openjpa.persistence.OpenJPAEntityManager;

import java.io.Serializable;

public class CubaEntityManagerFactory implements Serializable
{
    private OpenJPAEntityManagerFactory jpaFactory;

   CubaEntityManagerFactory(OpenJPAEntityManagerFactory jpaFactory) {
        this.jpaFactory = jpaFactory;
    }

    public CubaEntityManager createEntityManager() {
        OpenJPAEntityManager em = jpaFactory.createEntityManager();
        return new CubaEntityManager(em);
    }
}
