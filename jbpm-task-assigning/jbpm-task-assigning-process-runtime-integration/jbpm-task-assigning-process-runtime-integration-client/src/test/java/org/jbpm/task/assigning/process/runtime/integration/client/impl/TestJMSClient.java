package org.jbpm.task.assigning.process.runtime.integration.client.impl;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import javax.jms.ConnectionFactory;
import javax.jms.Queue;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.junit.Test;
import org.kie.server.api.model.KieServerStateInfo;
import org.kie.server.api.model.KieServiceResponse;
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.api.model.definition.ProcessDefinition;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.KieServicesConfiguration;
import org.kie.server.client.KieServicesFactory;
import org.kie.server.client.ProcessServicesClient;

import static org.junit.Assert.assertEquals;

public class TestJMSClient {

    private static final String REMOTE_SERVER = "http://localhost:8080/kie-server/services/rest/server";
    private static final String USER = "wbadmin";
    private static final String PASSWORD = "wbadmin";

    // org.jboss.naming.remote.client.InitialContextFactory
    private static final String INITIAL_CONTEXT_FACTORY = "org.wildfly.naming.client.WildFlyInitialContextFactory";
    private static final String REMOTING_URL = "remote://" + "%s" + ":4447";

    private static final String REQUEST_QUEUE_JNDI = "jms/queue/KIE.SERVER.REQUEST";
    private static final String RESPONSE_QUEUE_JNDI = "jms/queue/KIE.SERVER.RESPONSE";
    private static final String CONNECTION_FACTORY = "jms/RemoteConnectionFactory";

    public static KieServicesConfiguration createKieServicesJmsConfiguration() {
        try {
            InitialContext context = getInitialRemoteContext();

            Queue requestQueue = (Queue) context.lookup(REQUEST_QUEUE_JNDI);
            Queue responseQueue = (Queue) context.lookup(RESPONSE_QUEUE_JNDI);
            ConnectionFactory connectionFactory = (ConnectionFactory) context.lookup(CONNECTION_FACTORY);

            KieServicesConfiguration jmsConfiguration = KieServicesFactory.newJMSConfiguration(
                    connectionFactory, requestQueue, responseQueue, USER, PASSWORD);

            return jmsConfiguration;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create JMS client configuration!", e);
        }
    }

    /**
     * @return Initial context for connecting to remote server.
     */
    public static InitialContext getInitialRemoteContext() {
        InitialContext context = null;
        try {
            final Properties env = new Properties();
            env.put(Context.INITIAL_CONTEXT_FACTORY, INITIAL_CONTEXT_FACTORY);
            URL url = new URL(REMOTE_SERVER);
            env.put(Context.PROVIDER_URL, "remote://" + url.getHost() + ":4447");
            env.put(Context.SECURITY_PRINCIPAL, USER);
            env.put(Context.SECURITY_CREDENTIALS, PASSWORD);
            context = new InitialContext(env);
        } catch (NamingException | MalformedURLException e) {
            throw new RuntimeException("Failed to create initial context!", e);
        }
        return context;
    }

    @Test
    public void testConnection() {

        KieServicesConfiguration conf = createKieServicesJmsConfiguration();

        KieServicesClient kieServicesClient = KieServicesFactory.newKieServicesClient(conf);

        ServiceResponse<KieServerStateInfo> response = kieServicesClient.getServerState();

        assertEquals(KieServiceResponse.ResponseType.SUCCESS, response.getType());

        ProcessServicesClient processServicesClient = kieServicesClient.getServicesClient(ProcessServicesClient.class);
        ProcessDefinition definition = processServicesClient.getProcessDefinition("task-assignments_3.0.0-SNAPSHOT", "com.myspace.task_assignments.TestUserTaskSLA");

        int i = 0;


    }
}
