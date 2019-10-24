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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.jbpm.task.assigning.model.Group;
import org.jbpm.task.assigning.model.Task;
import org.jbpm.task.assigning.model.TaskAssigningSolution;
import org.jbpm.task.assigning.model.TaskOrUser;
import org.jbpm.task.assigning.model.User;
import org.jbpm.task.assigning.process.runtime.integration.client.PlanningData;
import org.jbpm.task.assigning.process.runtime.integration.client.TaskInfo;

import static org.jbpm.task.assigning.process.runtime.integration.client.TaskStatus.InProgress;
import static org.jbpm.task.assigning.process.runtime.integration.client.TaskStatus.Ready;
import static org.jbpm.task.assigning.process.runtime.integration.client.TaskStatus.Reserved;
import static org.jbpm.task.assigning.process.runtime.integration.client.TaskStatus.Suspended;

/**
 * This class is intended for the construction of a TaskAssigningSolution given a set TaskInfo and a set of User.
 * The solution is constructed considering the PlanningData for each task.
 */
public class SolutionBuilder {

    public static final Task DUMMY_TASK;

    static {
        DUMMY_TASK = new Task(-1,
                              -1,
                              "dummy-process",
                              "dummy-container",
                              "dummy-task",
                              10, new HashMap<>());
        DUMMY_TASK.getPotentialOwners().add(User.PLANNING_USER);
    }

    static class AssignedTask {

        private Task task;
        private int index;
        private boolean published;
        private boolean pinned;

        AssignedTask(Task task, int index, boolean published, boolean pinned) {
            this.task = task;
            this.index = index;
            this.published = published;
            this.pinned = pinned;
        }

        Task getTask() {
            return task;
        }

        int getIndex() {
            return index;
        }

        boolean isPublished() {
            return published;
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
        final List<Task> unAssignedTasks = new ArrayList<>();
        final Map<String, List<SolutionBuilder.AssignedTask>> assignedTasksByUserId = new HashMap<>();

        taskInfos.forEach(taskInfo -> {
            final Task task = fromTaskInfo(taskInfo);
            if (Ready == taskInfo.getStatus()) {
                //ready tasks are assigned to nobody.
                unAssignedTasks.add(task);
            } else if (Reserved == taskInfo.getStatus() || InProgress == taskInfo.getStatus() || Suspended == taskInfo.getStatus()) {
                if (StringUtils.isNoneEmpty(taskInfo.getActualOwner())) {
                    // If actualOwner is empty the only chance is that the task was in Ready status and changed to
                    // Suspended, since Reserved and InProgress tasks has always an owner in jBPM.
                    // Finally tasks with no actualOwner (Suspended) are skipped, since they'll be properly added to the
                    // solution when they change to Ready status and the proper jBPM event is raised.

                    final PlanningData planningData = taskInfo.getPlanningData();
                    boolean published;
                    boolean pinned;
                    if (planningData != null) {
                        //the task was already planned.
                        published = InProgress == taskInfo.getStatus() || planningData.isPublished();
                        pinned = published || planningData.isPinned();
                        task.setPinned(pinned);
                        task.setPublished(published);
                        if (Objects.equals(planningData.getAssignedUser(), taskInfo.getActualOwner())) {
                            //preserve currentParameters.
                            addTaskToUser(assignedTasksByUserId, task, planningData.getAssignedUser(), planningData.getIndex(), published, pinned);
                        } else {
                            addTaskToUser(assignedTasksByUserId, task, taskInfo.getActualOwner(), -1, published, pinned);
                        }
                    } else {
                        published = InProgress == taskInfo.getStatus();
                        pinned = published;
                        task.setPinned(pinned);
                        task.setPublished(published);
                        addTaskToUser(assignedTasksByUserId, task, taskInfo.getActualOwner(), -1, published, pinned);
                    }
                }
            }
        });

        final List<Task> allTasks = new ArrayList<>();
        final List<User> allUsers = new ArrayList<>();
        final Map<String, User> usersById = externalUsers.stream()
                .map(SolutionBuilder::fromExternalUser)
                .collect(Collectors.toMap(User::getEntityId, Function.identity()));
        usersById.put(User.PLANNING_USER.getEntityId(), User.PLANNING_USER);
        //TODO, check if this dummy task is ok, by now if we don't add it there are problems when e.g. all tasks has
        //been completed in the jBPM and thus all the tasks are removed from the solution. In this case as soon the
        //solution gets with 0 tasks an exception is thrown.
        allTasks.add(DUMMY_TASK);

        usersById.values().forEach(user -> {
            List<SolutionBuilder.AssignedTask> assignedTasks = assignedTasksByUserId.get(user.getEntityId());
            if (assignedTasks != null) {
                //add the tasks for this user.
                final List<Task> userTasks = assignedTasks.stream().map(AssignedTask::getTask).collect(Collectors.toList());
                addTasksToUser(user, userTasks);
                allTasks.addAll(userTasks);
            }
            allUsers.add(user);
        });

        assignedTasksByUserId.forEach((key, assignedTasks) -> {
            if (!usersById.containsKey(key)) {
                //Find the tasks that are assigned to users that are no longer available and let the Solver assign them again.
                unAssignedTasks.addAll(assignedTasks.stream().map(AssignedTask::getTask).collect(Collectors.toList()));
            }
        });
        allTasks.addAll(unAssignedTasks);
        return new TaskAssigningSolution(-1, allUsers, allTasks);
    }

    /**
     * Link the list of tasks to the given user. The tasks comes in the expected order.
     * @param user the user that will "own" the tasks in the chained graph.
     * @param tasks the tasks to link.
     */
    static void addTasksToUser(User user, List<Task> tasks) {
        TaskOrUser previousTask = user;
        for (Task nextTask : tasks) {
            previousTask.setNextTask(nextTask);
            nextTask.setPreviousTaskOrUser(previousTask);
            previousTask = nextTask;
        }
    }

    static void addTaskToUser(Map<String, List<SolutionBuilder.AssignedTask>> tasksByUser,
                              Task task,
                              String actualOwner,
                              int index,
                              boolean published,
                              boolean pinned) {
        final List<SolutionBuilder.AssignedTask> userAssignedTasks = tasksByUser.computeIfAbsent(actualOwner, key -> new ArrayList<>());
        addInOrder(userAssignedTasks, task, index, published, pinned);
    }

    static void addInOrder(List<SolutionBuilder.AssignedTask> assignedTasks,
                           Task task,
                           int index,
                           boolean published,
                           boolean pinned) {
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
        assignedTasks.add(insertIndex, new SolutionBuilder.AssignedTask(task, index, published, pinned));
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

    static User fromExternalUser(org.jbpm.task.assigning.user.system.integration.User externalUser) {
        final User user = new User(externalUser.getId().hashCode(), externalUser.getId());
        final Set<Group> groups = new HashSet<>();
        user.setGroups(groups);
        if (externalUser.getGroups() != null) {
            externalUser.getGroups().forEach(externalGroup -> groups.add(new Group(externalGroup.getId().hashCode(), externalGroup.getId())));
        }
        return user;
    }
}

