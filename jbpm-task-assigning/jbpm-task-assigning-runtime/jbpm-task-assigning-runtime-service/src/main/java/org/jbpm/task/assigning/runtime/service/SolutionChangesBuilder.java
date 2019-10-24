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
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jbpm.task.assigning.model.Task;
import org.jbpm.task.assigning.model.TaskAssigningSolution;
import org.jbpm.task.assigning.model.User;
import org.jbpm.task.assigning.model.solver.realtime.AddTaskProblemFactChange;
import org.jbpm.task.assigning.model.solver.realtime.AssignTaskProblemFactChange;
import org.jbpm.task.assigning.model.solver.realtime.ReleaseTaskProblemFactChange;
import org.jbpm.task.assigning.model.solver.realtime.RemoveTaskProblemFactChange;
import org.jbpm.task.assigning.process.runtime.integration.client.TaskInfo;
import org.optaplanner.core.impl.solver.ProblemFactChange;

import static org.jbpm.task.assigning.runtime.service.SolutionBuilder.DUMMY_TASK;
import static org.jbpm.task.assigning.runtime.service.SolutionBuilder.fromTaskInfo;

/**
 * This class manages the calculation of the impact (i.e. the set of changes to be applied) on a solution given the
 * refreshed information about the tasks in the jBPM runtime.
 */
public class SolutionChangesBuilder {

    private TaskAssigningSolution solution;

    private List<TaskInfo> taskInfos;

    public SolutionChangesBuilder() {
    }

    public SolutionChangesBuilder withSolution(TaskAssigningSolution solution) {
        this.solution = solution;
        return this;
    }

    public SolutionChangesBuilder withTasks(List<TaskInfo> taskInfos) {
        this.taskInfos = taskInfos;
        return this;
    }

    public List<ProblemFactChange<TaskAssigningSolution>> build() {
        final List<ProblemFactChange<TaskAssigningSolution>> changes = new ArrayList<>();
        final Map<Long, Task> taskById = solution.getTaskList()
                .stream()
                .filter(task -> !DUMMY_TASK.getId().equals(task.getId()))
                .collect(Collectors.toMap(Task::getId, Function.identity()));
        final Map<String, User> userById = solution.getUserList()
                .stream()
                .collect(Collectors.toMap(User::getEntityId, Function.identity()));

        Task task;
        for (TaskInfo taskInfo : taskInfos) {
            task = taskById.remove(taskInfo.getTaskId());

            switch (taskInfo.getStatus()) {
                case Ready:
                    if (task == null) {
                        // it's a new task
                        final Task newTask = fromTaskInfo(taskInfo);
                        changes.add(new AddTaskProblemFactChange(newTask));
                    } else {
                        // task was probably assigned to someone else in the past and released from the task list administration
                        // since the planner never leave tasks in Released status.
                        // release the task in the plan and let it be assigned again.


                        //TODO MAÑANa seguir aca
                        //OJO, si se hizo el release en la lista de tareas y en la solution dice que hay asignaciones....
                        //Qué hacemos...... se puede 1) respetar el release desde la lista de tareas o imponer
                        //lo que han en la memoria, pero es una opcion.... por ahora esta bien hacer el release...
                        changes.add(new ReleaseTaskProblemFactChange(task));
                    }
                    break;
                case Reserved:
                case InProgress:
                    if (task == null) {
                        // if Reserved:
                        //        the task was created and reserved completely outside of the planner. We add it to the
                        //        solution.
                        // if InProgress:
                        //        the task was created, reserved and started completely outside of the planner.
                        //        We add it to the solution since this assignment might affect the workload, etc., of the plan.

                        final Task newTask = fromTaskInfo(taskInfo);
                        final User user = userById.get(taskInfo.getActualOwner());
                        // TODO check that the user exists. (future iteration when we manage a more fine grained interaction
                        // with the user system.)
                        // assign and ensure the task is published since the task was already seen by the public audience.
                        changes.add(new AssignTaskProblemFactChange(newTask, user, true));
                    } else if (!taskInfo.getActualOwner().equals(task.getUser().getEntityId())) {
                        // if Reserved:
                        //       the task was probably manually re-assigned from the task list to another user. We must respect
                        //       this assignment.
                        // if InProgress:
                        //       the task was probably re-assigned to another user from the task list prior to start.
                        //       We must correct this assignment so it's reflected in the plan and also respect it.

                        final User user = userById.get(taskInfo.getActualOwner());
                        // TODO, check that the user exists. (future iteration when we manage a more fine grained interaction
                        // with the user system.)
                        // assign and ensure the task is published since the task was already seen by the public audience.
                        changes.add(new AssignTaskProblemFactChange(task, user, true));
                    }
                    break;
                case Suspended:
                    if (task == null) {
                        // the task was created, eventually assigned and started, suspended etc. completely outside of
                        // the planner.
                        // if (taskInfo.getActualOwner() == null) {
                        // do nothing, the task was assigned to nobody. So it was necessary in Ready status.
                        // it'll be added to the solution if it comes into Ready or Reserved status in a later moment.
                        // }
                        if (taskInfo.getActualOwner() != null) {
                            // we add it to the solution since this assignment might affect the workload, etc., of the plan.
                            final Task newTask = fromTaskInfo(taskInfo);
                            final User user = userById.get(taskInfo.getActualOwner());
                            // TODO check that the user exists. (future iteration when we manage a more fine grained interaction
                            // with the user system.)
                            // assign and ensure the task is published since the task was already seen by the public audience.
                            changes.add(new AssignTaskProblemFactChange(newTask, user, true));
                        }
                    } else if (!taskInfo.getActualOwner().equals(task.getUser().getEntityId())) {
                        // the task was assigned to someone else from the task list prior to the suspension, we must
                        // reflect that change in the plan.
                        final User user = userById.get(taskInfo.getActualOwner());
                        // TODO check that the user exists. (future iteration when we manage a more fine grained interaction
                        // with the user system.)
                        // assign and ensure the task is published since the task was already seen by the public audience.
                        changes.add(new AssignTaskProblemFactChange(task, user, true));
                    }
            }
        }

        // finally all the tasks that were part of the solution and are no longer in the taskInfos must be removed
        // since they were already Completed, Exited, or any other status were they will never get out from.
        // No users will work on this tasks any more.
        for (Task oldTask : taskById.values()) {
            changes.add(new RemoveTaskProblemFactChange(oldTask));
        }
        return changes;
    }
}
