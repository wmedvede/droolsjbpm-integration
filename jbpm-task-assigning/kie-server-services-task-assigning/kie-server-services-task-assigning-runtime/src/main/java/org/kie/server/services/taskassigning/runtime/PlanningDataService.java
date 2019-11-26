/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
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
 */

package org.kie.server.services.taskassigning.runtime;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.InitialContext;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceProviderResolverHolder;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.transaction.UserTransaction;

import org.jbpm.task.assigning.process.runtime.integration.client.PlanningTask;
import org.kie.server.api.KieServerConstants;
import org.kie.server.api.model.KieServerConfig;
import org.kie.server.services.jbpm.jpa.PersistenceUnitInfoImpl;
import org.kie.server.services.jbpm.jpa.PersistenceUnitInfoLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.kie.server.services.taskassigning.runtime.TaskAssigningConstants.JBPM_TASK_ASSIGNING_CFG_PERSISTANCE_DS;

public class PlanningDataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlanningDataService.class);
    private static final String JBPM_TASK_ASSIGNING_PERSISTENCE_XML_LOCATION = "/jpa/taskAssigning/META-INF/persistence.xml";

    private EntityManagerFactory emf;
    private EntityManager em;
    private UserTransaction tx;

    public void init(KieServerConfig config) {
        try {
            InitialContext ctx = new InitialContext();
            emf = build(ctx, getPersistenceProperties(config));
            em = emf.createEntityManager();
            tx = (UserTransaction) ctx.lookup("java:jboss/UserTransaction");
        } catch (Exception e) {
            LOGGER.error("PlanningDataService initialization failed.", e);
            throw new RuntimeException("PlanningDataService initialization failed: " + e.getMessage(), e);
        }
    }

    public void saveOrUpdate(PlanningTask planningTask) {
        try {
            tx.begin();
            em.merge(new PlanningTaskImpl(planningTask.getTaskId(), planningTask.getAssignedUser(),
                                          planningTask.getIndex(), planningTask.isPublished()));
            tx.commit();
        } catch (Exception e) {
            throw new RuntimeException("An error was produced during planningTask addOrUpdate: " + e.getMessage(), e);
        }
    }

    public PlanningTask read(long taskId) {
        return em.find(PlanningTaskImpl.class, taskId);
    }

    protected EntityManagerFactory build(InitialContext ctx, Map<String, String> properties) {
        try {
            InputStream inputStream = PlanningDataService.class.getResourceAsStream(JBPM_TASK_ASSIGNING_PERSISTENCE_XML_LOCATION);
            PersistenceUnitInfo info = PersistenceUnitInfoLoader.load(inputStream, ctx, this.getClass().getClassLoader());
            // prepare persistence unit root location
            URL root = PersistenceUnitInfoLoader.class.getResource(JBPM_TASK_ASSIGNING_PERSISTENCE_XML_LOCATION);
            String jarLocation = root.toExternalForm().split("!")[0].replace(JBPM_TASK_ASSIGNING_PERSISTENCE_XML_LOCATION, "");
            try {
                ((PersistenceUnitInfoImpl) info).setPersistenceUnitRootUrl(new URL(jarLocation));
            } catch (Exception e) {
                // in case setting URL to jar file location only fails, fallback to complete URL
                ((PersistenceUnitInfoImpl) info).setPersistenceUnitRootUrl(root);
            }
            // Need to explicitly set jtaDataSource here, its value is fetched in Hibernate logger before configuration
            ((PersistenceUnitInfoImpl) info).setJtaDataSource(properties.get("javax.persistence.jtaDataSource"));
            List<PersistenceProvider> persistenceProviders = PersistenceProviderResolverHolder.getPersistenceProviderResolver().getPersistenceProviders();
            PersistenceProvider selectedProvider = null;
            if (persistenceProviders != null) {
                for (PersistenceProvider provider : persistenceProviders) {
                    if (provider.getClass().getName().equals(info.getPersistenceProviderClassName())) {
                        selectedProvider = provider;
                        break;
                    }
                }
            }

            return selectedProvider.createContainerEntityManagerFactory(info, properties);
        } catch (Exception e) {
            throw new RuntimeException("Unable to create EntityManagerFactory due to " + e.getMessage(), e);
        }
    }

    protected Map<String, String> getPersistenceProperties(KieServerConfig config) {
        Map<String, String> persistenceProperties = new HashMap<String, String>();

        persistenceProperties.put("hibernate.dialect", config.getConfigItemValue(KieServerConstants.CFG_PERSISTANCE_DIALECT, "org.hibernate.dialect.H2Dialect"));
        persistenceProperties.put("hibernate.default_schema", config.getConfigItemValue(KieServerConstants.CFG_PERSISTANCE_DEFAULT_SCHEMA));
        persistenceProperties.put("hibernate.transaction.jta.platform", config.getConfigItemValue(KieServerConstants.CFG_PERSISTANCE_TM, "JBossAS"));
        persistenceProperties.put("javax.persistence.jtaDataSource", config.getConfigItemValue(JBPM_TASK_ASSIGNING_CFG_PERSISTANCE_DS, "java:jboss/datasources/ExampleDS"));

        System.getProperties().stringPropertyNames()
                .stream()
                .filter(PersistenceUnitInfoLoader::isValidPersistenceKey)
                .forEach(name -> persistenceProperties.put(name, System.getProperty(name)));

        return persistenceProperties;
    }
}
