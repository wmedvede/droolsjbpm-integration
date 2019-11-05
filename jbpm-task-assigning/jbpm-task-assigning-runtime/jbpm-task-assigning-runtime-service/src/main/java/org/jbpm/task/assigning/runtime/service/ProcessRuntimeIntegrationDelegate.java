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

    /*

    What happens if something fails and e.g. the task can't be delegated.


    Possible use cases:
        The task was Ready or Reserved and was moved to InProgress or Suspended or the Completion or Error states.
        just the moment before to applying the plann.
        And also after the double check was performed.


        If (the task is InProgress) {
            the delegation will work. and we'll basically override the assignment realized form the task list.
            this is a very uncommon case and very unlikely to happen.
            When we do the assignment, the solution will be consistent with the jBPM runtime.
        }

        If (the task is Suspended) {
            we get a no 'current status' match ". error since it's not possible to reassign on this state.
            we don't save PlanningData, and the unexpected change will be automatically impacted on the soluiton
            in the next solution refresh.
        }

        if (the task is Completed) {
            we get a no 'current status" match error since it's nt possible to reassign on this state.
            we don't save the PlanningData and the unexpected change will be automatically impacted on the solution
            in the next solution refresh.
        }

        The task can never be in created status since optaplanner don't manage tasks in this status.
        Created

        if (the task is Error, Exited, Failed or Obsolete) {
            we get a no 'current status' match error, since it's not possible to reassign on this state.
        }

        if (we get a generic service error the problem is that e.g. connection failed, etc...)




     */


            /*

            Error detection during task delegation

        1) Una tarea que viene en el plan y que tengo que assignar ya no está Ready o Reserved
        bueno, basicamente no viene en la lista, entonces basicamente no hago nada.
        Pero tendría que hacer algo a nivel del Solver, bueno, en principio no, pues la solution ya se actualizará por los mecanismos q correspnodan
        Haciendo esta query se minimiza un poco el tema de que haya una petada por intentar transicionar una tarea de un estado
        ej. Completed a Reserved....


        2) al pasar una tarea a delegate igualmente puede petar por
            2.1) ya la han transicionado manualmente en el medio y me da un error de estado que no se puede cambiar. (esto se va cuando tenga todo idealmente transaccional)
            2.2) un error de conexion u cualquier otro tipo de error no controlado.

            errores controlados que puedo llegara manejar:
            Si estamos ejecutando con la API REST

            KieServicesHttpException con codigo:
                404 - task not found
                403 - permission denied. Seguramente al tarea ya está en un estado donde no se puede delegar o el usuario no tiene permiso
                500 - Internal Server error. Error no controlado....

            KieServicesException:
                Si hay errores unexpected como por ejemplo sino se puede hacer un marshaling/unmarshalling. (aunque en este caso no deberia suceder)

            NoEndpointFoundException:
                sino es posible encontrar un endpoint al cual conectarse...



         */
}
