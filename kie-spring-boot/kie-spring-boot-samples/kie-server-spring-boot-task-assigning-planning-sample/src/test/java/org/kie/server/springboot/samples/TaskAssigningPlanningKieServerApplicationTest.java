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

package org.kie.server.springboot.samples;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.server.services.api.KieServer;
import org.kie.server.services.api.KieServerExtension;
import org.kie.server.services.impl.KieServerImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {TaskAssigningPlanningKieServerApplication.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:task-assigning-planning-server-test.properties")
public class TaskAssigningPlanningKieServerApplicationTest {

    private static final String TASK_ASSIGNING_PLANNING = "TaskAssigningPlanning";

    @Autowired
    private KieServer kieServer;

    @Test
    public void taskAssigningPlanningExtensionStarted() {
        KieServerExtension planningExtension = ((KieServerImpl) kieServer).getServerExtensions().stream()
                .filter(extension -> TASK_ASSIGNING_PLANNING.equals(extension.getExtensionName()))
                .findFirst().orElseThrow(() -> new RuntimeException("Expected : " + TASK_ASSIGNING_PLANNING + " extension was not found in current server."));
        assertTrue(TASK_ASSIGNING_PLANNING + " extension is expected to be active", planningExtension.isActive());
    }
}
