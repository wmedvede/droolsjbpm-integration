package org.jbpm.task.assigning.runtime.service;

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

import org.jbpm.task.assigning.process.runtime.integration.client.PlanningData;
import org.kie.server.api.KieServerConstants;
import org.kie.server.api.model.KieServerConfig;
import org.kie.server.services.jbpm.jpa.PersistenceUnitInfoImpl;
import org.kie.server.services.jbpm.jpa.PersistenceUnitInfoLoader;

public class PlanningDataService {

    private static final String TASK_ASSIGNING_PERSISTENCE_XML_LOCATION = "/jpa/taskAssigning/META-INF/persistence.xml";
    private static final String TASK_ASSIGNING_CFG_PERSISTANCE_DS = "org.kie.server.persistence.taskAssigning.ds";

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
            throw new RuntimeException("Unable to start PlanningDataService: " + e.getMessage(), e);
        }
    }

    public void addOrUpdate(PlanningData planningData) {
        try {
            tx.begin();
            em.merge(planningData);
            tx.commit();
        } catch (Exception e) {
            throw new RuntimeException("An error was produced during planningData addOrUpdate: " + e.getMessage(), e);
        }
    }

    public PlanningData read(long taskId) {
        return em.find(PlanningDataImpl.class, taskId);
    }

    protected EntityManagerFactory build(InitialContext ctx, Map<String, String> properties) {
        try {
            InputStream inputStream = PlanningDataService.class.getResourceAsStream(TASK_ASSIGNING_PERSISTENCE_XML_LOCATION);
            PersistenceUnitInfo info = PersistenceUnitInfoLoader.load(inputStream, ctx, this.getClass().getClassLoader());
            // prepare persistence unit root location
            URL root = PersistenceUnitInfoLoader.class.getResource(TASK_ASSIGNING_PERSISTENCE_XML_LOCATION);
            String jarLocation = root.toExternalForm().split("!")[0].replace(TASK_ASSIGNING_PERSISTENCE_XML_LOCATION, "");
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
        persistenceProperties.put("javax.persistence.jtaDataSource", config.getConfigItemValue(TASK_ASSIGNING_CFG_PERSISTANCE_DS, "java:jboss/datasources/ExampleDS"));

        System.getProperties().stringPropertyNames()
                .stream()
                .filter(PersistenceUnitInfoLoader::isValidPersistenceKey)
                .forEach(name -> persistenceProperties.put(name, System.getProperty(name)));

        return persistenceProperties;
    }
}
