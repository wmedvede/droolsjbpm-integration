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

package org.jbpm.task.assigning.process.runtime.integration.client.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.jbpm.task.assigning.process.runtime.integration.client.PotentialOwner;
import org.jbpm.task.assigning.process.runtime.integration.client.ProcessRuntimeIntegrationClient;
import org.jbpm.task.assigning.process.runtime.integration.client.ProcessRuntimeIntegrationClientFactory;
import org.jbpm.task.assigning.process.runtime.integration.client.TaskInfo;
import org.jbpm.task.assigning.process.runtime.integration.client.TaskPlanningInfo;
import org.jbpm.task.assigning.process.runtime.integration.client.TaskStatus;
import org.junit.Test;
import org.kie.server.api.model.instance.ProcessInstance;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.ProcessServicesClient;

public class ProcessRuntimeIntegrationClientImplTest {

    @Test
    public void testClient() {

        ProcessRuntimeIntegrationClient client = ProcessRuntimeIntegrationClientFactory.newIntegrationClient("http://localhost:8080/kie-server/services/rest/server",
                                                                                                             "wbadmin",
                                                                                                             "wbadmin");

        List<TaskInfo> result = client.findTasks(Arrays.asList(TaskStatus.Ready, TaskStatus.Reserved), 0, 10000);

        TaskPlanningInfo planningInfo1 = new TaskPlanningInfo();
        planningInfo1.setTaskId(63);
        planningInfo1.setProcessInstanceId(43);
        planningInfo1.setContainerId("task-assignments_18.0.0-SNAPSHOT");
        planningInfo1.getPlanningParameters().setAssignedUser("maciek");
        planningInfo1.getPlanningParameters().setIndex(789);
        planningInfo1.getPlanningParameters().setPinned(false);
        planningInfo1.getPlanningParameters().setPublished(true);

        TaskPlanningInfo planningInfo2 = new TaskPlanningInfo();
        planningInfo2.setTaskId(64);
        planningInfo2.setProcessInstanceId(43);
        planningInfo2.setContainerId("task-assignments_18.0.0-SNAPSHOT");
        planningInfo2.getPlanningParameters().setAssignedUser("mary");
        planningInfo2.getPlanningParameters().setIndex(456);
        planningInfo2.getPlanningParameters().setPinned(true);
        planningInfo2.getPlanningParameters().setPublished(false);

        client.applyPlanning(Arrays.asList(planningInfo1, planningInfo2), "planning_user");
    }

    private ProcessRuntimeIntegrationClient newClient() {
        return ProcessRuntimeIntegrationClientFactory.newIntegrationClient("http://localhost:8080/kie-server/services/rest/server",
                                                                           "wbadmin",
                                                                           "wbadmin");
    }

    private KieServicesClient newKieServicesClient() {
        return ProcessRuntimeIntegrationClientFactory.newKieServicesClient("http://localhost:8080/kie-server/services/rest/server",
                                                                           "wbadmin",
                                                                           "wbadmin");
    }

    private static String CONTAINER_ID = "task-assignments_24.0.0-SNAPSHOT";
    private static String PROCESS_ID = "task-assignments.EmulatePlanningParams";

    @Test
    public void createProcessInstances() {
        int processInstancesSize = 10;
        List<Long> processInstances = new ArrayList<>();

        ProcessServicesClient processServices = newKieServicesClient().getServicesClient(ProcessServicesClient.class);

        long processInstanceId;
        for (int i = 0; i < processInstancesSize; i++) {
            processInstanceId = processServices.startProcess(CONTAINER_ID, PROCESS_ID);
            processInstances.add(processInstanceId);
        }
        String ids = processInstances.stream().map(Object::toString).collect(Collectors.joining(", "));
        System.out.println("Created process instances: [" + ids + "]");
    }

    @Test
    public void destroyProcessInstances() {
        List<Long> processInstanceIds = new ArrayList<>();

        ProcessServicesClient processServices = newKieServicesClient().getServicesClient(ProcessServicesClient.class);

        List<ProcessInstance> processInstances = processServices.findProcessInstances(CONTAINER_ID, 0, 10000);
        processInstances.forEach(processInstance -> {
            if (PROCESS_ID.equals(processInstance.getProcessId()) &&
                    processInstance.getState() == org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE) {
                processInstanceIds.add(processInstance.getId());
                processServices.abortProcessInstance(CONTAINER_ID, processInstance.getId());
            }
        });
        String ids = processInstanceIds.stream().map(Object::toString).collect(Collectors.joining(", "));
        System.out.println("Destroyed process instances: [" + ids + "]");
    }

    @Test
    public void doRandomAssignment() {
        Random random = new Random();
        ProcessRuntimeIntegrationClient client = newClient();

        List<TaskInfo> tasks = client.findTasks(Arrays.asList(TaskStatus.Ready, TaskStatus.Reserved), 0, 1000);
        Set<String> potentialOwnersSet = new HashSet<>();
        tasks.forEach(taskInfo -> {
            taskInfo.getPotentialOwners().stream().filter(PotentialOwner::isUser).forEach(user -> potentialOwnersSet.add(user.getEntityId()));
        });
        String[] potentialOwners = potentialOwnersSet.toArray(new String[0]);
        List<TaskPlanningInfo> planningInfos = new ArrayList<>();

        tasks.forEach(taskInfo -> {
            TaskPlanningInfo planningInfo = new TaskPlanningInfo(taskInfo.getContainerId(), taskInfo.getTaskId(), taskInfo.getProcessInstanceId());
            planningInfo.getPlanningParameters().setAssignedUser(potentialOwners[random.nextInt(potentialOwners.length)]);
            planningInfo.getPlanningParameters().setIndex(1234);
            planningInfos.add(planningInfo);
        });
        client.applyPlanning(planningInfos, "planning_user");
    }
}
