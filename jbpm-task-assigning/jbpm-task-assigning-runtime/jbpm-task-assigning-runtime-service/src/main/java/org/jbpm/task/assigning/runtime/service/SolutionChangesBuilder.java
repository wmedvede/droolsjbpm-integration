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

public class SolutionChangesBuilder {

    private TaskAssigningSolution solution;

    private List<TaskInfo> taskInfos;

    private PublishedTaskCache publishedTasks;

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

    public SolutionChangesBuilder withCache(PublishedTaskCache publishedTasks) {
        this.publishedTasks = publishedTasks;
        return this;
    }

    public List<ProblemFactChange<TaskAssigningSolution>> build() {
        //TODO OJO, siempre puede estar el caso donde nada ha cambiado y no hay q hacer nada. Lo tengo contemplado???
        final List<ProblemFactChange<TaskAssigningSolution>> changes = new ArrayList<>();
        final Map<Long, Task> tasksById = solution.getTaskList()
                .stream()
                .filter(task -> !DUMMY_TASK.getId().equals(task.getId()))
                .collect(Collectors.toMap(Task::getId, Function.identity()));
        final Map<String, User> userById = solution.getUserList()
                .stream()
                .collect(Collectors.toMap(User::getEntityId, Function.identity()));

        Task task;
        for (TaskInfo taskInfo : taskInfos) {
            task = tasksById.remove(taskInfo.getTaskId());

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
                        // (ideally a different user should be prioritized for the next time...)
                        changes.add(new ReleaseTaskProblemFactChange(taskInfo.getTaskId()));
                    }
                    break;
                case Reserved:
                case InProgress:
                    if (task == null) {
                        // if Reserved: it's a new task we add it to the solution.
                        // if InProgress:
                        //       the task was created, reserved and started completely outside of the planner.
                        //       we add it to the solution since this assignment might affect the workload, etc., of the plan.

                        // ensure the published status is true, since the task was already seen by the public audience.
                        markAsPublished(taskInfo.getTaskId());
                        final Task newTask = fromTaskInfo(taskInfo);
                        final User user = userById.get(taskInfo.getActualOwner());
                        // TODO check that the user exists.
                        changes.add(new AssignTaskProblemFactChange(newTask, user));
                    } else if (taskInfo.getActualOwner().equals(task.getUser().getEntityId())) {
                        // that's fine, the task still belongs to the previously assigned user, do nothing.
                        // double check that the task is marked as published.
                        markAsPublished(taskInfo.getTaskId());
                    } else {
                        // if Reserved:
                        //       the task was probably manually assigned from the task list to another user. We must respect
                        //       this assignment.
                        // if InProgress:
                        //       the task was probably reassigned, and started from the task list. We must correct this
                        //       assignment so it's reflected in the plan and also respect it.

                        // ensure the published status is true, since the task was already seen by the public audience.
                        markAsPublished(taskInfo.getTaskId());
                        final User user = userById.get(taskInfo.getActualOwner());
                        // TODO, check that the user exists.
                        changes.add(new AssignTaskProblemFactChange(task, user));
                    }
                    break;
                case Suspended:
                    if (task == null) {
                        // the task was created, eventually assigned and started etc. completely outside of the planner.
                        // if (taskInfo.getActualOwner() == null) {
                        // do nothing, the task was assigned to nobody. So it was necessary in Ready status.
                        // it'll be added to the solution if it comes into Ready or Reserved status in a later moment.
                        // }
                        if (taskInfo.getActualOwner() != null) {
                            // we add it to the solution since this assignment might affect the workload, etc., of the plan.
                            // ensure the published status is true, since the task was already seen by the public audience.
                            markAsPublished(taskInfo.getTaskId());
                            final Task newTask = fromTaskInfo(taskInfo);
                            final User user = userById.get(taskInfo.getActualOwner());
                            // TODO check that the user exists.
                            changes.add(new AssignTaskProblemFactChange(newTask, user));
                        }
                    } else {
                        if (taskInfo.getActualOwner().equals(task.getUser().getEntityId())) {
                            // that's fine, the task still belongs to the previously assigned user, do nothing.
                            // double check that the task is marked as published might not be bad.
                            markAsPublished(taskInfo.getTaskId());
                        } else {
                            // the task was assigned to someone else from the task list prior to the suspension, we must
                            // reflect that change in the plan.
                            // ensure the published status is true, since the task was already seen by the public audience.
                            markAsPublished(taskInfo.getTaskId());
                            final User user = userById.get(taskInfo.getActualOwner());
                            // TODO check that the user exists.
                            changes.add(new AssignTaskProblemFactChange(task, user));
                        }
                    }
            }
        }

        // finally all the tasks that were part of the solution and are no longer in the taskInfos must be removed
        // since they were already Completed, Exited, or any other status were get out from. No users will work on
        // this tasks any more.
        for (Task oldTask : tasksById.values()) {
            changes.add(new RemoveTaskProblemFactChange(oldTask));
        }
        return changes;
    }

    private void markAsPublished(long taskId) {
        if (publishedTasks != null) {
            publishedTasks.put(taskId);
        }
    }
}
