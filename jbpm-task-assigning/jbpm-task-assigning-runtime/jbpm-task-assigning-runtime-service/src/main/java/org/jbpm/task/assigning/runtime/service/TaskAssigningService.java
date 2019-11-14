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

import org.jbpm.task.assigning.user.system.integration.UserSystemService;

public class TaskAssigningService {

    private final SolverDef solverDef;
    private final ProcessRuntimeIntegrationDelegate runtimeClientDelegate;
    private final UserSystemService userSystemService;
    private final ExecutorService executorService;

    private SolverHandler solverHandler;

    public TaskAssigningService(final SolverDef solverDef,
                                final ProcessRuntimeIntegrationDelegate runtimeClientDelegate,
                                final UserSystemService userSystemService,
                                final ExecutorService executorService) {
        this.solverDef = solverDef;
        this.runtimeClientDelegate = runtimeClientDelegate;
        this.userSystemService = userSystemService;
        this.executorService = executorService;
    }

    public void init() {
        solverHandler = new SolverHandler(solverDef, runtimeClientDelegate, userSystemService, executorService);
        solverHandler.init();
        solverHandler.start();
    }

    public void destroy() {
        solverHandler.destroy();
    }
}
