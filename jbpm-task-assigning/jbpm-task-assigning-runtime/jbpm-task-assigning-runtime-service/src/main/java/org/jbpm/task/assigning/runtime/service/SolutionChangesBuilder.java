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
import org.jbpm.task.assigning.user.system.integration.UserSystemService;
import org.optaplanner.core.impl.solver.ProblemFactChange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jbpm.task.assigning.model.Task.DUMMY_TASK;
import static org.jbpm.task.assigning.model.Task.DUMMY_TASK_PLANNER_241;
import static org.jbpm.task.assigning.model.User.PLANNING_USER;
import static org.jbpm.task.assigning.runtime.service.SolutionBuilder.fromTaskInfo;
import static org.jbpm.task.assigning.runtime.service.util.UserUtil.fromExternalUser;

/**
 * This class performs the calculation of the impact (i.e. the set of changes to be applied) on a solution given the
 * updated information about the tasks in the jBPM runtime.
 */
public class SolutionChangesBuilder {

    private static Logger LOGGER = LoggerFactory.getLogger(SolutionChangesBuilder.class);

    private TaskAssigningSolution solution;

    private List<TaskInfo> taskInfos;

    private UserSystemService systemService;

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

    public SolutionChangesBuilder withUserSystem(UserSystemService userSystemService) {
        this.systemService = userSystemService;
        return this;
    }

    public List<ProblemFactChange<TaskAssigningSolution>> build() {
        final List<ProblemFactChange<TaskAssigningSolution>> changes = new ArrayList<>();
        final Map<Long, Task> taskById = solution.getTaskList()
                .stream()
                .filter(task -> !DUMMY_TASK.getId().equals(task.getId()))
                .filter(task -> !DUMMY_TASK_PLANNER_241.getId().equals(task.getId()))
                .collect(Collectors.toMap(Task::getId, Function.identity()));
        final Map<String, User> usersById = solution.getUserList()
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
                        final User user = getUser(usersById, taskInfo.getActualOwner());
                        // assign and ensure the task is published since the task was already seen by the public audience.
                        changes.add(new AssignTaskProblemFactChange(newTask, user, true));
                    } else if (!taskInfo.getActualOwner().equals(task.getUser().getEntityId()) ||
                            (taskInfo.getPlanningData().isPublished() && !task.isPinned())) {
                        // if Reserved:
                        //       the task was probably manually re-assigned from the task list to another user. We must respect
                        //       this assignment.
                        // if InProgress:
                        //       the task was probably re-assigned to another user from the task list prior to start.
                        //       We must correct this assignment so it's reflected in the plan and also respect it.

                        //Or the task was published and not yet pinned

                        final User user = getUser(usersById, taskInfo.getActualOwner());
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
                            final User user = getUser(usersById, taskInfo.getActualOwner());
                            // assign and ensure the task is published since the task was already seen by the public audience.
                            changes.add(new AssignTaskProblemFactChange(newTask, user, true));
                        }
                    } else if (!taskInfo.getActualOwner().equals(task.getUser().getEntityId()) ||
                            (taskInfo.getPlanningData().isPublished() && !task.isPinned())) {
                        // the task was assigned to someone else from the task list prior to the suspension, we must
                        // reflect that change in the plan.
                        // Or the task was published and not yet pinned.
                        final User user = getUser(usersById, taskInfo.getActualOwner());
                        // assign and ensure the task is published since the task was already seen by the public audience.
                        changes.add(new AssignTaskProblemFactChange(task, user, true));
                    }
            }
        }

        for (Task oldTask : taskById.values()) {
            changes.add(new RemoveTaskProblemFactChange(oldTask));
        }

        applyWorkaroundForPLANNER_241(solution, changes);
        return changes;
    }

    private User getUser(Map<String, User> usersById, String userId) {
        User user = usersById.get(userId);
        if (user == null) {
            LOGGER.debug("User {} was not found in current solution, it'll we looked up in the external user system .", userId);
            org.jbpm.task.assigning.user.system.integration.User externalUser = systemService.findUser(userId);
            if (externalUser != null) {
                user = fromExternalUser(externalUser);
            } else {
                // We add it by convention, since the task list administration supports the delegation to non-existent users.
                LOGGER.debug("User {} was not found in the external user system, it looks like it's a manual" +
                                     " assignment from the tasks administration. It'll be added to the solution" +
                                     " to respect the assignment.", userId);
                user = new User(userId.hashCode(), userId);
            }
        }
        return user;
    }

    /**
     * This method adds a second dummy task for avoiding the issue produced by https://issues.jboss.org/browse/PLANNER-241
     * and will be removed as soon it's fixed. Note that workaround doesn't have a huge impact on the solution since
     * the dummy task is added only once and to the planning user.
     */
    private void applyWorkaroundForPLANNER_241(TaskAssigningSolution solution, List<ProblemFactChange<TaskAssigningSolution>> changes) {
        boolean hasDummyTask2 = solution.getTaskList().stream().anyMatch(task -> DUMMY_TASK_PLANNER_241.getId().equals(task.getId()));
        if (!hasDummyTask2) {
            changes.add(new AssignTaskProblemFactChange(DUMMY_TASK_PLANNER_241, PLANNING_USER));
        }
    }
}
