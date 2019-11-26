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

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jbpm.task.assigning.process.runtime.integration.client.ProcessRuntimeIntegrationClient;
import org.jbpm.task.assigning.process.runtime.integration.client.TaskInfo;
import org.jbpm.task.assigning.process.runtime.integration.client.TaskPlanningInfo;
import org.jbpm.task.assigning.process.runtime.integration.client.TaskStatus;

public class ProcessRuntimeIntegrationDelegate {

    private final ProcessRuntimeIntegrationClient runtimeClient;

    private final PlanningDataService dataService;

    public ProcessRuntimeIntegrationDelegate(final ProcessRuntimeIntegrationClient runtimeClient, final PlanningDataService dataService) {
        this.runtimeClient = runtimeClient;
        this.dataService = dataService;
    }

    public List<TaskInfo> findTasks(List<TaskStatus> status) {
        //TODO, paged reading in next iteration.
        //TOOD WM mirar esto
        final List<TaskInfo> taskInfos = runtimeClient.findTasks(0L, new Date(2019 - 1900, 10, 1), status, 0, 100000);
        taskInfos.forEach(taskInfo -> taskInfo.setPlanningTask(dataService.read(taskInfo.getTaskId())));
        return taskInfos;
    }

    public void applyPlanning(List<TaskPlanningInfo> planningInfos, String userId) {
        long minTaskId = planningInfos.stream().mapToLong(TaskPlanningInfo::getTaskId).min().orElse(0);
        long maxTaskId = planningInfos.stream().mapToLong(TaskPlanningInfo::getTaskId).max().orElse(0);

        final Map<Long, TaskPlanningInfo> taskIdToPlanningInfo = planningInfos.stream()
                .collect(Collectors.toMap(TaskPlanningInfo::getTaskId, Function.identity()));

        //TODO, paged reading in next iteration.
        final List<TaskInfo> taskInfos = findTasks(minTaskId,
                                                   maxTaskId,
                                                   Arrays.asList(TaskStatus.Ready, TaskStatus.Reserved, TaskStatus.InProgress, TaskStatus.Suspended),
                                                   0,
                                                   100000);

        TaskPlanningInfo planningInfo;
        for (TaskInfo taskInfo : taskInfos) {
            planningInfo = taskIdToPlanningInfo.get(taskInfo.getTaskId());
            if (planningInfo != null) {
                switch (taskInfo.getStatus()) {
                    case Ready:
                        // Tasks are never let in Ready status by the task assigning so it can be delegated directly.
                        delegateTask(planningInfo, userId);
                        dataService.saveOrUpdate(planningInfo.getPlanningTask());
                        break;

                    case Reserved:
                        if (taskInfo.getPlanningTask() == null) {
                            // It's the first time this task is being managed by the task assigning.
                            if (!taskInfo.getActualOwner().equals(planningInfo.getPlanningTask().getAssignedUser())) {
                                delegateTask(planningInfo, userId);
                            }
                            dataService.saveOrUpdate(planningInfo.getPlanningTask());
                        } else if (!taskInfo.getActualOwner().equals(planningInfo.getPlanningTask().getAssignedUser())) {
                            // Task was already managed by the task assigning, and a different user is provided.
                            if (taskInfo.getActualOwner().equals(taskInfo.getPlanningTask().getAssignedUser())) {
                                // If the task still keeps the previous assignment done by the task assigning the new
                                // assignment can be applied.
                                // In other cases the task was manually assigned from the task list "in the middle" so
                                // just do nothing since the planning data will be properly updated with the next
                                // solution update.
                                delegateTask(planningInfo, userId);
                                dataService.saveOrUpdate(planningInfo.getPlanningTask());
                            }
                        } else {
                            // the assigned user didn't change, there's no need to delegate again. Just update the
                            // planing data to be sure this information is in sync with the solution.
                            dataService.saveOrUpdate(planningInfo.getPlanningTask());
                        }
                        break;

                    case InProgress:
                    case Suspended:
                        if (taskInfo.getActualOwner().equals(planningInfo.getPlanningTask().getAssignedUser())) {
                            // task might have been created, assigned and started/suspended completely out of the task
                            // assigning or the planning data might have changed. Just update the planning data.
                            // If actualOwner != assignedUser the planning data will be properly updated in the next
                            // solution update.
                            dataService.saveOrUpdate(planningInfo.getPlanningTask());
                        }
                        break;
                }
            }
        }
    }

    private void delegateTask(TaskPlanningInfo planningInfo, String userId) {
        runtimeClient.delegateTask(planningInfo, userId);
    }

    private List<TaskInfo> findTasks(Long fromTaskId, Long toTaskId, List<TaskStatus> status, Integer page, Integer pageSize) {
        final List<TaskInfo> taskInfos = runtimeClient.findTasks(fromTaskId, toTaskId, status, page, pageSize);
        taskInfos.forEach(taskInfo -> taskInfo.setPlanningTask(dataService.read(taskInfo.getTaskId())));
        return taskInfos;
    }
}
