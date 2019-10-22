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

import java.util.concurrent.ExecutorService;

import org.jbpm.task.assigning.process.runtime.integration.client.ProcessRuntimeIntegrationClient;
import org.jbpm.task.assigning.user.system.integration.UserSystemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskAssigningService {

    private static Logger LOGGER = LoggerFactory.getLogger(TaskAssigningService.class);

    private SolverDefRegistry solverDefRegistry;
    private ProcessRuntimeIntegrationClient runtimeClient;
    private UserSystemService userSystemService;
    private ExecutorService executorService;

    private SolverHandler solverHandler;

    public TaskAssigningService(SolverDefRegistry solverDefRegistry,
                                ProcessRuntimeIntegrationClient runtimeClient,
                                UserSystemService userSystemService,
                                ExecutorService executorService) {
        this.solverDefRegistry = solverDefRegistry;
        this.runtimeClient = runtimeClient;
        this.userSystemService = userSystemService;
        this.executorService = executorService;
        solverDefRegistry.init();
    }

    public void init() {
        SolverDef solverDef = solverDefRegistry.getSolverDef();
        solverHandler = new SolverHandler(solverDef, runtimeClient, userSystemService, executorService);
        solverHandler.init();
        solverHandler.start();
    }

    public void destroy() {
        solverHandler.destroy();
    }
}
