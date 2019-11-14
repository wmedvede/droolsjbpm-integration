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

package org.jbpm.task.assigning.process.runtime.integration.client;

import java.util.Collections;

import org.jbpm.task.assigning.process.runtime.integration.client.impl.ProcessRuntimeIntegrationClientImpl;
import org.kie.server.api.KieServerConstants;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.KieServicesConfiguration;
import org.kie.server.client.KieServicesFactory;
import org.kie.server.client.QueryServicesClient;
import org.kie.server.client.UserTaskServicesClient;

public class ProcessRuntimeIntegrationClientFactory {

    static {
        // Ensure user bypass is on to be able to e.g. let the client "admin" user to claim/delegate tasks on behalf
        // of other users
        System.setProperty("org.kie.server.bypass.auth.user", Boolean.TRUE.toString());
    }

    private static KieServicesClient createKieServicesClient(final String endpoint,
                                                             final String login,
                                                             final String password) {

        final KieServicesConfiguration configuration = KieServicesFactory.newRestConfiguration(endpoint, login, password);
        configuration.setTimeout(60000);
        configuration.setCapabilities(Collections.singletonList(KieServerConstants.CAPABILITY_BPM));
        return KieServicesFactory.newKieServicesClient(configuration);
    }

    public static ProcessRuntimeIntegrationClient newIntegrationClient(final String endpoint,
                                                                       final String login,
                                                                       final String password) {
        KieServicesClient servicesClient = createKieServicesClient(endpoint, login, password);
        UserTaskServicesClient userTaskServicesClient = servicesClient.getServicesClient(UserTaskServicesClient.class);
        QueryServicesClient queryServicesClient = servicesClient.getServicesClient(QueryServicesClient.class);
        return new ProcessRuntimeIntegrationClientImpl(userTaskServicesClient, queryServicesClient);
    }
}
