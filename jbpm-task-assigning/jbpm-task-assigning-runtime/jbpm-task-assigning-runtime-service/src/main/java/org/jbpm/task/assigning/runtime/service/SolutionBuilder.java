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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jbpm.task.assigning.model.Group;
import org.jbpm.task.assigning.model.Task;
import org.jbpm.task.assigning.model.TaskAssigningSolution;
import org.jbpm.task.assigning.model.TaskOrUser;
import org.jbpm.task.assigning.model.User;
import org.jbpm.task.assigning.process.runtime.integration.client.PlanningTask;
import org.jbpm.task.assigning.process.runtime.integration.client.TaskInfo;
import org.jbpm.task.assigning.runtime.service.util.UserUtil;

import static org.apache.commons.lang3.StringUtils.isNoneEmpty;
import static org.jbpm.task.assigning.model.Task.DUMMY_TASK;
import static org.jbpm.task.assigning.model.User.IS_PLANNING_USER;
import static org.jbpm.task.assigning.model.User.PLANNING_USER;
import static org.jbpm.task.assigning.process.runtime.integration.client.TaskStatus.InProgress;
import static org.jbpm.task.assigning.process.runtime.integration.client.TaskStatus.Reserved;
import static org.jbpm.task.assigning.process.runtime.integration.client.TaskStatus.Suspended;

/**
 * This class is intended for the restoring of a TaskAssigningSolution given a set TaskInfo, a set of User and the
 * corresponding PlanningTask for each task. I'ts typically used when the solver needs to be started during the
 * application startup procedure.
 */
public class SolutionBuilder {

    static class AssignedTask {

        private Task task;
        private int index;
        private boolean pinned;

        AssignedTask(Task task, int index, boolean pinned) {
            this.task = task;
            this.index = index;
            this.pinned = pinned;
        }

        Task getTask() {
            return task;
        }

        int getIndex() {
            return index;
        }

        boolean isPinned() {
            return pinned;
        }
    }

    private List<TaskInfo> taskInfos;
    private List<org.jbpm.task.assigning.user.system.integration.User> externalUsers;

    public SolutionBuilder() {
    }

    public SolutionBuilder withTasks(List<TaskInfo> taskInfos) {
        this.taskInfos = taskInfos;
        return this;
    }

    public SolutionBuilder withUsers(List<org.jbpm.task.assigning.user.system.integration.User> externalUsers) {
        this.externalUsers = externalUsers;
        return this;
    }

    public TaskAssigningSolution build() {
        final List<Task> tasks = new ArrayList<>();
        final Map<String, List<SolutionBuilder.AssignedTask>> assignedTasksByUserId = new HashMap<>();
        final Map<String, User> usersById = externalUsers.stream()
                .map(UserUtil::fromExternalUser)
                .collect(Collectors.toMap(User::getEntityId, Function.identity()));
        usersById.put(PLANNING_USER.getEntityId(), PLANNING_USER);

        taskInfos.forEach(taskInfo -> {
            final Task task = fromTaskInfo(taskInfo);
            switch (taskInfo.getStatus()) {
                case Ready:
                    tasks.add(task);
                    break;
                case Reserved:
                case InProgress:
                case Suspended:
                    if (isNoneEmpty(taskInfo.getActualOwner())) {
                        // If actualOwner is empty the only chance is that the task was in Ready status and changed to
                        // Suspended, since Reserved and InProgress tasks has always an owner in jBPM.
                        // Finally tasks with no actualOwner but (Suspended) are skipped, since they'll be properly added to
                        // the solution when they change to Ready status and the proper jBPM event is raised.
                        tasks.add(task);
                        final PlanningTask planningTask = taskInfo.getPlanningTask();
                        if (planningTask != null && taskInfo.getActualOwner().equals(planningTask.getAssignedUser())) {
                            boolean pinned = InProgress == taskInfo.getStatus() || Suspended == taskInfo.getStatus() ||
                                    planningTask.isPublished() || !usersById.containsKey(taskInfo.getActualOwner());
                            addTaskToUser(assignedTasksByUserId, task, planningTask.getAssignedUser(), planningTask.getIndex(), pinned);
                        } else {
                            boolean pinned = (Reserved == taskInfo.getStatus() && !IS_PLANNING_USER.test(taskInfo.getActualOwner())) ||
                                    InProgress == taskInfo.getStatus() || Suspended == taskInfo.getStatus();
                            addTaskToUser(assignedTasksByUserId, task, taskInfo.getActualOwner(), -1, pinned);
                        }
                    }
                    break;
            }
        });

        assignedTasksByUserId.forEach((key, assignedTasks) -> {
            User user = usersById.get(key);
            if (user == null) {
                //create the user by convention.
                user = new User(key.hashCode(), key);
                usersById.put(key, user);
            }
            final List<Task> userTasks = assignedTasks.stream().map(AssignedTask::getTask).collect(Collectors.toList());
            addTasksToUser(user, userTasks);
        });

        //Add the DUMMY_TASK to avoid running into scenarios where the solution remains with no tasks.
        tasks.add(DUMMY_TASK);
        final List<User> users = new ArrayList<>(usersById.values());
        return new TaskAssigningSolution(-1, users, tasks);
    }

    /**
     * Link the list of tasks to the given user. The tasks comes in the expected order.
     * @param user the user that will "own" the tasks in the chained graph.
     * @param tasks the tasks to link.
     */
    static void addTasksToUser(User user, List<Task> tasks) {
        TaskOrUser previousTask = user;
        // startTime, endTime, nextTask and user are shadow variables that should be calculated by the solver at
        // start time. However this is not yet implemented see: https://issues.jboss.org/browse/PLANNER-1316 so by now
        // they are initialized as part of the solution restoring.
        for (Task nextTask : tasks) {
            previousTask.setNextTask(nextTask);

            nextTask.setStartTime(previousTask.getEndTime());
            nextTask.setEndTime(nextTask.getStartTime() + nextTask.getDuration());
            nextTask.setPreviousTaskOrUser(previousTask);
            nextTask.setUser(user);

            previousTask = nextTask;
        }
    }

    static void addTaskToUser(Map<String, List<SolutionBuilder.AssignedTask>> tasksByUser,
                              Task task,
                              String actualOwner,
                              int index,
                              boolean pinned) {
        task.setPinned(pinned);
        final List<SolutionBuilder.AssignedTask> userAssignedTasks = tasksByUser.computeIfAbsent(actualOwner, key -> new ArrayList<>());
        addInOrder(userAssignedTasks, task, index);
    }

    static void addInOrder(List<SolutionBuilder.AssignedTask> assignedTasks,
                           Task task,
                           int index) {
        boolean pinned = task.isPinned();
        int insertIndex = 0;
        SolutionBuilder.AssignedTask currentTask;
        final Iterator<SolutionBuilder.AssignedTask> it = assignedTasks.iterator();
        boolean found = false;
        while (!found && it.hasNext()) {
            currentTask = it.next();
            if (pinned && currentTask.isPinned()) {
                found = (index >= 0) && (currentTask.getIndex() < 0 || index < currentTask.getIndex());
            } else if (pinned && !currentTask.isPinned()) {
                found = true;
            } else if (!pinned && !currentTask.isPinned()) {
                found = (index >= 0) && (currentTask.getIndex() < 0 || index < currentTask.getIndex());
            }
            insertIndex = !found ? insertIndex + 1 : insertIndex;
        }
        assignedTasks.add(insertIndex, new SolutionBuilder.AssignedTask(task, index, pinned));
    }

    static Task fromTaskInfo(TaskInfo taskInfo) {
        final Task task = new Task(taskInfo.getTaskId(),
                                   taskInfo.getProcessInstanceId(),
                                   taskInfo.getProcessId(),
                                   taskInfo.getContainerId(),
                                   taskInfo.getName(),
                                   taskInfo.getPriority(),
                                   taskInfo.getInputData());
        if (taskInfo.getPotentialOwners() != null) {
            taskInfo.getPotentialOwners().forEach(potentialOwner -> {
                if (potentialOwner.isUser()) {
                    task.getPotentialOwners().add(new User(potentialOwner.getEntityId().hashCode(), potentialOwner.getEntityId()));
                } else {
                    task.getPotentialOwners().add(new Group(potentialOwner.getEntityId().hashCode(), potentialOwner.getEntityId()));
                }
            });
        }
        return task;
    }
}

