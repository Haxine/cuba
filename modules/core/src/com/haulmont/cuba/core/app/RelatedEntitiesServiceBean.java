/*
 * Copyright (c) 2008-2016 Haulmont.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.haulmont.cuba.core.app;

import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.cuba.core.*;
import com.haulmont.cuba.core.entity.BaseUuidEntity;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.ExtendedEntities;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.core.global.ViewRepository;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.*;

import static com.haulmont.bali.util.Preconditions.checkNotNullArgument;

/**
 */
@Service(RelatedEntitiesService.NAME)
public class RelatedEntitiesServiceBean implements RelatedEntitiesService {

    @Inject
    protected Persistence persistence;

    @Inject
    protected Metadata metadata;

    @Inject
    protected ViewRepository viewRepository;

    @Inject
    protected ExtendedEntities extendedEntities;

    @SuppressWarnings("unchecked")
    @Override
    public List<UUID> getRelatedIds(List<UUID> parents, String parentMetaClass, String relationProperty) {
        checkNotNullArgument(parents, "parents argument is null");
        checkNotNullArgument(parentMetaClass, "parentMetaClass argument is null");
        checkNotNullArgument(relationProperty, "relationProperty argument is null");

        MetaClass metaClass = extendedEntities.getEffectiveMetaClass(metadata.getClassNN(parentMetaClass));
        Class parentClass = metaClass.getJavaClass();

        MetaProperty metaProperty = metaClass.getPropertyNN(relationProperty);

        // return empty list only after all argument checks
        if (parents.isEmpty()) {
            return Collections.emptyList();
        }

        MetaClass propertyMetaClass = extendedEntities.getEffectiveMetaClass(metaProperty.getRange().asClass());
        Class propertyClass = propertyMetaClass.getJavaClass();

        List<UUID> relatedIds = new ArrayList<>();

        Transaction tx = persistence.createTransaction();
        try {
            EntityManager em = persistence.getEntityManager();
            String queryString = "select x from " + parentMetaClass + " x where x.id in :ids";
            Query query = em.createQuery(queryString);

            View view = new View(parentClass);
            view.addProperty(relationProperty, new View(propertyClass).addProperty("id"));

            query.setView(view);
            query.setParameter("ids", parents);

            List<Entity> resultList = query.getResultList();
            for (Entity e : resultList) {
                Object value = e.getValue(relationProperty);
                if (value instanceof BaseUuidEntity) {
                    relatedIds.add(((BaseUuidEntity) value).getId());
                } else if (value instanceof Collection) {
                    for (Object collectionItem : (Collection)value) {
                        relatedIds.add(((BaseUuidEntity) collectionItem).getId());
                    }
                }
            }

            tx.commit();
        } finally {
            tx.end();
        }

        return relatedIds;
    }
}