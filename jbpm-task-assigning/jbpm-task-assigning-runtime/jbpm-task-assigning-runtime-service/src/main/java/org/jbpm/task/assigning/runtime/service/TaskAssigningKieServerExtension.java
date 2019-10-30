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

package org.jbpm.task.assigning.runtime.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.jbpm.task.assigning.process.runtime.integration.client.ProcessRuntimeIntegrationClient;
import org.jbpm.task.assigning.process.runtime.integration.client.ProcessRuntimeIntegrationClientFactory;
import org.jbpm.task.assigning.user.system.integration.impl.WildflyUserSystemService;
import org.kie.server.api.model.KieServerConfig;
import org.kie.server.services.api.KieContainerInstance;
import org.kie.server.services.api.KieServerExtension;
import org.kie.server.services.api.KieServerRegistry;
import org.kie.server.services.api.SupportedTransports;
import org.kie.server.services.impl.KieServerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jbpm.task.assigning.runtime.service.TaskAssigningConstants.JBPM_TASK_ASSIGNING_EXT_DISABLED;
import static org.jbpm.task.assigning.runtime.service.TaskAssigningConstants.JBPM_TASK_ASSIGNING_PROCESS_RUNTIME_PWD;
import static org.jbpm.task.assigning.runtime.service.TaskAssigningConstants.JBPM_TASK_ASSIGNING_PROCESS_RUNTIME_URL;
import static org.jbpm.task.assigning.runtime.service.TaskAssigningConstants.JBPM_TASK_ASSIGNING_PROCESS_RUNTIME_USER;
import static org.jbpm.task.assigning.runtime.service.util.PropertyUtil.readSystemProperty;

public class TaskAssigningKieServerExtension implements KieServerExtension {

    private static Logger LOGGER = LoggerFactory.getLogger(TaskAssigningKieServerExtension.class);

    /**
     * Business Resource Planning Task Assigning
     */
    public static final String CAPABILITY_BRP_TASK_ASSIGNING = "BRP-TASK-ASSIGNING";

    private static final boolean DISABLED = readSystemProperty(JBPM_TASK_ASSIGNING_EXT_DISABLED, true, value -> !Boolean.FALSE.toString().equals(value));

    public static final String EXTENSION_NAME = "OptaPlannerTaskAssigning";

    private final List<Object> services = new ArrayList<>();
    private boolean initialized = false;
    private PlanningDataService dataService;
    private TaskAssigningService taskAssigningService;
    private ExecutorService executorService;

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public boolean isActive() {
        return !DISABLED;
    }

    @Override
    public void init(KieServerImpl kieServer, KieServerRegistry registry) {
        LOGGER.debug("Initializing " + EXTENSION_NAME + " extension.");
        if (DISABLED) {
            LOGGER.debug(EXTENSION_NAME + " is currently disabled. Use the " + JBPM_TASK_ASSIGNING_EXT_DISABLED + " to enable it if needed.");
            return;
        }

        startDataService(registry.getConfig());

        //TODO, review this initialization
        ProcessRuntimeIntegrationClient runtimeClient = getRuntimeIntegrationClient();
        ProcessRuntimeIntegrationDelegate runtimeClientDelegate = new ProcessRuntimeIntegrationDelegate(runtimeClient, dataService);

        //TODO, review this initialization
        WildflyUserSystemService userSystemService = getUserSystemService();

        this.executorService = new ThreadPoolExecutor(
                3,
                3,
                10, // thread keep alive time
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(3));

        this.taskAssigningService = new TaskAssigningService(new SolverDefRegistryImpl(),
                                                             runtimeClientDelegate,
                                                             userSystemService,
                                                             executorService);
        taskAssigningService.init();
        this.services.add(taskAssigningService);
        initialized = true;
    }

    @Override
    public void destroy(KieServerImpl kieServer, KieServerRegistry registry) {
        LOGGER.debug("Destroying " + EXTENSION_NAME + " extension.");
        taskAssigningService.destroy();
    }

    @Override
    public void createContainer(String id, KieContainerInstance kieContainerInstance, Map<String, Object> parameters) {
        LOGGER.debug("Create container: " + id + " at " + EXTENSION_NAME + " extension.");
    }

    @Override
    public void updateContainer(String id, KieContainerInstance kieContainerInstance, Map<String, Object> parameters) {
        LOGGER.debug("Update container: " + id + " at " + EXTENSION_NAME + " extension.");
    }

    @Override
    public boolean isUpdateContainerAllowed(String id, KieContainerInstance kieContainerInstance, Map<String, Object> parameters) {
        return true;
    }

    @Override
    public void disposeContainer(String id, KieContainerInstance kieContainerInstance, Map<String, Object> parameters) {
        LOGGER.debug("Dispose container: " + id + " at " + EXTENSION_NAME + " extension.");
    }

    @Override
    public List<Object> getAppComponents(SupportedTransports type) {
        return new ArrayList<>();
    }

    @Override
    public <T> T getAppComponents(Class<T> serviceType) {
        if (serviceType.isAssignableFrom(taskAssigningService.getClass())) {
            return (T) taskAssigningService;
        }
        return null;
    }

    @Override
    public String getImplementedCapability() {
        return CAPABILITY_BRP_TASK_ASSIGNING;
    }

    @Override
    public List<Object> getServices() {
        return services;
    }

    @Override
    public String getExtensionName() {
        return EXTENSION_NAME;
    }

    @Override
    public Integer getStartOrder() {
        return 26;
    }

    private WildflyUserSystemService getUserSystemService() {
        //TODO, move this initialization to SPI and add proper parametrization
        return new WildflyUserSystemService();
    }

    private ProcessRuntimeIntegrationClient getRuntimeIntegrationClient() {
        //TODO, future iteration will add the ability of getting the available process runtime url by asking he controller.
        String url = readSystemProperty(JBPM_TASK_ASSIGNING_PROCESS_RUNTIME_URL, "http://localhost:8080/kie-server/services/rest/server", value -> value);
        String user = readSystemProperty(JBPM_TASK_ASSIGNING_PROCESS_RUNTIME_USER, "wbadmin", value -> value);
        String pwd = readSystemProperty(JBPM_TASK_ASSIGNING_PROCESS_RUNTIME_PWD, null, value -> value);
        return ProcessRuntimeIntegrationClientFactory.newIntegrationClient(url, user, pwd);
    }

    private void startDataService(KieServerConfig config) {
        dataService = new PlanningDataService();
        dataService.init(config);
    }
}
