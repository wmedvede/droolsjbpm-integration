package org.jbpm.task.assigning.runtime.service;

import java.util.List;

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
    public List<TaskPlanningResult> applyPlanning(List<TaskPlanningInfo> planningInfos, String userId) {
        final List<TaskPlanningResult> result = runtimeClient.applyPlanning(planningInfos, userId);
        planningInfos.forEach(taskPlanningInfo -> dataService.addOrUpdate(taskPlanningInfo.getPlanningData()));
        return result;
    }
}
