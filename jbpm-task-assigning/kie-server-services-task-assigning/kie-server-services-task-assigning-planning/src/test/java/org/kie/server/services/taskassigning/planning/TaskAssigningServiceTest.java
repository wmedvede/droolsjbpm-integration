/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
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

package org.kie.server.services.taskassigning.planning;

import java.util.concurrent.ExecutorService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.server.services.api.KieServerRegistry;
import org.kie.server.services.taskassigning.user.system.api.UserSystemService;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class TaskAssigningServiceTest {

    @Mock
    private UserSystemService userSystemService;

    @Mock
    private ExecutorService executorService;

    @Mock
    private TaskAssigningRuntimeDelegate delegate;

    @Mock
    private SolverDef solverDef;

    @Mock
    private KieServerRegistry registry;

    @Mock
    private SolverHandler solverHandler;

    private TaskAssigningService taskAssigningService;

    @Before
    public void setUp() {
        taskAssigningService = new TaskAssigningServiceMock();
        taskAssigningService.setUserSystemService(userSystemService);
        taskAssigningService.setDelegate(delegate);
        taskAssigningService.setExecutorService(executorService);
    }

    @Test
    public void start() {
        taskAssigningService.start(solverDef, registry);
        verify(solverHandler).start();
    }

    public void destroy() {
        taskAssigningService.destroy();
        verify(solverHandler).destroy();
    }

    private class TaskAssigningServiceMock extends TaskAssigningService {

        @Override
        protected SolverHandler createSolverHandler(SolverDef solverDef,
                                                    KieServerRegistry registry,
                                                    TaskAssigningRuntimeDelegate delegate,
                                                    UserSystemService userSystemService,
                                                    ExecutorService executorService) {
            return solverHandler;
        }
    }
}
