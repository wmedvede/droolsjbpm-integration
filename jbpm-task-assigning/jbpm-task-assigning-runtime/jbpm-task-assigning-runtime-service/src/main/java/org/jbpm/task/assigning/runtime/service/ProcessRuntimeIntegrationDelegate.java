package org.jbpm.task.assigning.runtime.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jbpm.task.assigning.process.runtime.integration.client.ProcessRuntimeIntegrationClient;
import org.jbpm.task.assigning.process.runtime.integration.client.TaskInfo;
import org.jbpm.task.assigning.process.runtime.integration.client.TaskPlanningInfo;
import org.jbpm.task.assigning.process.runtime.integration.client.TaskPlanningResult;
import org.jbpm.task.assigning.process.runtime.integration.client.TaskStatus;

public class ProcessRuntimeIntegrationDelegate implements ProcessRuntimeIntegrationClient {

    private final ProcessRuntimeIntegrationClient runtimeClient;

    private final PlanningDataService dataService;

    public ProcessRuntimeIntegrationDelegate(final ProcessRuntimeIntegrationClient runtimeClient, final PlanningDataService dataService) {
        this.runtimeClient = runtimeClient;
        this.dataService = dataService;
    }

    @Override
    public List<TaskInfo> findTasks(List<TaskStatus> status, Integer page, Integer pageSize) {
        final List<TaskInfo> taskInfos = runtimeClient.findTasks(status, page, pageSize);
        taskInfos.forEach(taskInfo -> taskInfo.setPlanningData(dataService.read(taskInfo.getTaskId())));
        return taskInfos;
    }

    @Override
    public List<TaskInfo> findTasks(Long fromTaskId, Long toTaskId, List<TaskStatus> status, Integer page, Integer pageSize) {
        final List<TaskInfo> taskInfos = runtimeClient.findTasks(fromTaskId, toTaskId, status, page, pageSize);
        taskInfos.forEach(taskInfo -> taskInfo.setPlanningData(dataService.read(taskInfo.getTaskId())));
        return taskInfos;
    }

    @Override
    public void delegateTask(TaskPlanningInfo planningInfo, String userId) {
        runtimeClient.delegateTask(planningInfo, userId);
    }

    @Override
    public List<TaskPlanningResult> applyPlanning(List<TaskPlanningInfo> planningInfos, String userId) {
        long minTaskId = planningInfos.stream().mapToLong(TaskPlanningInfo::getTaskId).min().orElse(0);
        long maxTaskId = planningInfos.stream().mapToLong(TaskPlanningInfo::getTaskId).max().orElse(0);

        final Map<Long, TaskPlanningInfo> taskIdToPlanningInfo = planningInfos.stream()
                .collect(Collectors.toMap(TaskPlanningInfo::getTaskId, Function.identity()));

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
                        dataService.saveOrUpdate(planningInfo.getPlanningData());
                        break;

                    case Reserved:
                        if (taskInfo.getPlanningData() == null) {
                            // It's the first time this task is being managed by the task assigning.
                            delegateTask(planningInfo, userId);
                            dataService.saveOrUpdate(planningInfo.getPlanningData());
                        } else if (!taskInfo.getActualOwner().equals(planningInfo.getPlanningData().getAssignedUser())) {
                            // Task was already managed by the task assigning, and a different user is provided.
                            if (taskInfo.getActualOwner().equals(taskInfo.getPlanningData().getAssignedUser())) {
                                // If the task still keeps the previous assignment done by the task assigning the new
                                // assignment can be applied.
                                // In other cases the task was manually assigned from the task list "in the middle" so
                                // just do nothing since the planning data will be properly updated with the next
                                // solution update.
                                delegateTask(planningInfo, userId);
                                dataService.saveOrUpdate(planningInfo.getPlanningData());
                            }
                        } else {
                            // the assigned user didn't change, there's no need to delegate again. Just update the
                            // planing data to be sure this information is in sync with the solution.
                            dataService.saveOrUpdate(planningInfo.getPlanningData());
                        }
                        break;

                    case InProgress:
                    case Suspended:
                        if (taskInfo.getActualOwner().equals(planningInfo.getPlanningData().getAssignedUser())) {
                            // task might have been created, assigned and started/suspended completely out of the task
                            // assigning or the planning data might have changed. Just update the planning data.
                            // If actualOwner != assignedUser the planning data will be properly updated in the next
                            // solution update.
                            dataService.saveOrUpdate(planningInfo.getPlanningData());
                        }
                        break;
                }
            }
        }
        dataService.detachOldPanningData(planningInfos.stream()
                                                 .map(TaskPlanningInfo::getPlanningData)
                                                 .filter(Objects::nonNull)
                                                 .collect(Collectors.toList()));
        return Collections.emptyList();
    }
}
