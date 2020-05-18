/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
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

package org.kie.server.services.taskassigning.core.benchmark;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.kie.server.services.taskassigning.core.model.Group;
import org.kie.server.services.taskassigning.core.model.OrganizationalEntity;
import org.kie.server.services.taskassigning.core.model.Task;
import org.kie.server.services.taskassigning.core.model.TaskAssigningSolution;
import org.kie.server.services.taskassigning.core.model.User;
import org.optaplanner.examples.common.app.CommonApp;
import org.optaplanner.examples.common.app.LoggingMain;
import org.optaplanner.persistence.common.api.domain.solution.SolutionFileIO;
import org.optaplanner.persistence.xstream.impl.domain.solution.XStreamSolutionFileIO;

import static org.optaplanner.examples.common.app.CommonApp.DATA_DIR_SYSTEM_PROPERTY;

/**
 * Generates a solution equivalent to the solution produced in the TaskAssigningBatch scenario.
 * Where we basically a set of processCount process instances and userCount users.
 * Each process instance generates 3 tasks.
 * <p>
 * So for processCount = 1000 and userCount = 100
 * <p>
 * The solution generated will have:
 * <p>
 * 3000 tasks (1000 process instances * 3 tasks each)
 * 300  users (100 IT + 100 HR + 100 ENG)
 * <p>
 * <p>
 * 100 users in the IT group.
 * 100 users in the HR group.
 * 100 users in the ENG group.
 */
public class TaskAssigningBatchGenerator extends LoggingMain {

    private static final String HR_USER_PREFIX = "HR-user";
    private static final String IT_USER_PREFIX = "IT-user";
    private static final String ENG_USER_PREFIX = "ENG-user";
    private static final String HR_GROUP = "HR";
    private static final String IT_GROUP = "IT";
    private static final String ENG_GROUP = "ENG";
    private static final String USER_GROUP = "user";

    private static final String PROCESS_ID = "test-jbpm-assignment.testTaskAssignment";
    private static final String CONTAINER_ID = "kieserver-assets";

    private final SolutionFileIO<TaskAssigningSolution> solutionFileIO;
    private final File outputDir;

    private TaskAssigningBatchGenerator() {
        System.setProperty(DATA_DIR_SYSTEM_PROPERTY, "kie-server-services-task-assigning-benchmark");
        solutionFileIO = new XStreamSolutionFileIO<>(TaskAssigningSolution.class);
        outputDir = new File(CommonApp.determineDataDir("data"), "unsolved");
    }

    public static void main(String[] args) {
        TaskAssigningBatchGenerator generator = new TaskAssigningBatchGenerator();

//        generator.generateAndWriteBatchScenario(100, 100, 0);
//        generator.generateAndWriteBatchScenario(500, 100, 0);
//        generator.generateAndWriteBatchScenario(1000, 100, 0);
//        generator.generateAndWriteBatchScenario(2000, 300, 0);
//
//        generator.generateAndWriteBatchScenario(100, 100, 3);
//        generator.generateAndWriteBatchScenario(500, 100, 3);
//        generator.generateAndWriteBatchScenario(1000, 100, 3);
//        generator.generateAndWriteBatchScenario(2000, 300, 3);
    }

    private void generateAndWriteBatchScenario(int processCount, int userCount, int freeTasks) {
        TaskAssigningSolution solution = buildBatchSolution(processCount, userCount, freeTasks);
        String fileName = buildBatchName(solution.getTaskList().size(), solution.getUserList().size());
        writeTaskAssigningSolution(fileName, solution);
    }

    private void writeTaskAssigningSolution(String fileName, TaskAssigningSolution solution) {
        File outputFile = new File(outputDir, fileName + ".xml");
        solutionFileIO.write(solution, outputFile);
        logger.info("Saved: {}", outputFile);
    }

    private String buildBatchName(int tasksSize, int usersSize) {
        return "task-assigning-batch-" + tasksSize + "-tasks-" + usersSize + "users";
    }

    /**
     * Generates a solution equivalent to the solution produced in the TaskAssigningBatch scenario.
     * @param processCount quantity of processes to emulate. Each process has 3 tasks.
     * @param userCount quantity of users per group to emulate.
     * e.g. a userCount of 100 generates:
     * 100 users in the IT group.
     * 100 users in the HR group.
     * 100 users in the ENG group.
     */
    public static TaskAssigningSolution buildBatchSolution(int processCount, int userCount, int freeTasks) {
        List<User> itUsers = buildUsers(userCount, IT_USER_PREFIX, Arrays.asList(USER_GROUP, IT_GROUP));
        List<User> hrUsers = buildUsers(userCount, HR_USER_PREFIX, Arrays.asList(USER_GROUP, HR_GROUP));
        List<User> engUsers = buildUsers(userCount, ENG_USER_PREFIX, Arrays.asList(USER_GROUP, ENG_GROUP));
        List<User> allUsers = new ArrayList<>();
        allUsers.addAll(itUsers);
        allUsers.addAll(hrUsers);
        allUsers.addAll(engUsers);

        List<Task> allTasks = buildBatchSolutionTasks(0, 0, processCount);

        for (int i = 0; i < freeTasks; i++) {
            long freeTaskId = -100 - i;
            Task freeTask = buildTask(freeTaskId, -100, "FreeTask_" + freeTaskId, Collections.emptyList());
            int index = allTasks.size() / (i  + 2);
            allTasks.add(index, freeTask);
        }

        TaskAssigningSolution solution = new TaskAssigningSolution(-1L, allUsers, allTasks);
        return solution;
    }

    private static List<User> buildUsers(int size, String namePrefix, List<String> groups) {
        List<User> result = new ArrayList<>();
        User user;
        Set<Group> groupSet;
        String entityId;
        for (int i = 1; i <= size; i++) {
            entityId = namePrefix + i;
            user = new User(entityId.hashCode(), entityId, true);
            groupSet = groups.stream()
                    .map(groupName -> new Group(groupName.hashCode(), groupName))
                    .collect(Collectors.toSet());
            user.setGroups(groupSet);
            user.setLabelValues("SKILLS", new HashSet<>());
            user.setLabelValues("AFFINITIES", new HashSet<>());
            user.setAttributes(new HashMap<>());
            result.add(user);
        }
        return result;
    }

    private static List<Task> buildBatchSolutionTasks(long taskStartId, long processInstanceStartId, int size) {
        List<Task> result = new ArrayList<>();
        long taskId = taskStartId;
        long processInstanceId = processInstanceStartId;
        for (int i = 0; i < size; i++) {
            result.add(buildTask(taskId, processInstanceId, "ENG_" + taskId++, Collections.singletonList(ENG_GROUP)));
            result.add(buildTask(taskId, processInstanceId, "HR_" + taskId++, Collections.singletonList(HR_GROUP)));
            result.add(buildTask(taskId, processInstanceId, "IT_" + taskId++, Collections.singletonList(IT_GROUP)));
            processInstanceId++;
        }
        return result;
    }

    private static Task buildTask(long taskId, long processInstanceId, String taskName, List<String> potentialOwners) {
        Task task;
        Set<OrganizationalEntity> potentialOwnersSet;
        task = new Task(taskId, taskName, 0);
        task.setStatus("Ready");
        task.setInputData(new HashMap<>());
        task.getInputData().put("Comment", taskName);
        task.getInputData().put("NodeName", taskName);
        task.getInputData().put("TaskName", taskName);
        task.getInputData().put("Skippable", "false");
        task.getInputData().put("GroupId", "TheGroupName");
        task.setLabelValues("SKILLS", new HashSet<>());
        task.setLabelValues("AFFINITIES", new HashSet<>());
        task.setProcessId(PROCESS_ID);
        task.setContainerId(CONTAINER_ID);
        potentialOwnersSet = potentialOwners.stream()
                .map(potentialOwnerName -> new Group(potentialOwnerName.hashCode(), potentialOwnerName))
                .collect(Collectors.toSet());
        task.setPotentialOwners(potentialOwnersSet);
        task.setProcessInstanceId(processInstanceId);
        return task;
    }
}
