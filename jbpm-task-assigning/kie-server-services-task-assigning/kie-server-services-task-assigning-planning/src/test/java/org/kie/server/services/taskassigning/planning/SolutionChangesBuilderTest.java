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

package org.kie.server.services.taskassigning.planning;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.api.task.model.Status;
import org.kie.server.api.model.taskassigning.TaskData;
import org.kie.server.services.taskassigning.core.model.Task;
import org.kie.server.services.taskassigning.core.model.TaskAssigningSolution;
import org.kie.server.services.taskassigning.core.model.User;
import org.kie.server.services.taskassigning.core.model.solver.realtime.AddTaskProblemFactChange;
import org.kie.server.services.taskassigning.core.model.solver.realtime.AssignTaskProblemFactChange;
import org.kie.server.services.taskassigning.core.model.solver.realtime.ReleaseTaskProblemFactChange;
import org.kie.server.services.taskassigning.core.model.solver.realtime.RemoveTaskProblemFactChange;
import org.kie.server.services.taskassigning.planning.util.TaskUtil;
import org.kie.server.services.taskassigning.user.system.api.UserSystemService;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.optaplanner.core.impl.score.director.ScoreDirector;
import org.optaplanner.core.impl.solver.ProblemFactChange;

import static org.junit.Assert.assertEquals;
import static org.kie.api.task.model.Status.InProgress;
import static org.kie.api.task.model.Status.Ready;
import static org.kie.api.task.model.Status.Reserved;
import static org.kie.api.task.model.Status.Suspended;
import static org.kie.server.api.model.taskassigning.util.StatusConverter.convertToString;
import static org.kie.server.services.taskassigning.planning.TestUtil.mockExternalUser;
import static org.kie.server.services.taskassigning.planning.util.TaskUtil.fromTaskData;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SolutionChangesBuilderTest {

    private static final long TASK_ID = 0L;
    private static final long PROCESS_INSTANCE_ID = 1L;
    private static final String PROCESS_ID = "PROCESS_ID";
    private static final String CONTAINER_ID = "CONTAINER_ID";
    private static final String NAME = "NAME";
    private static final int PRIORITY = 2;
    private static final String ACTUAL_OWNER = "ACTUAL_OWNER";
    private static final Map<String, Object> INPUT_DATA = new HashMap<>();

    private static final String USER_ENTITY_ID = "USER_ENTITY_ID";
    private static final long USER_ID = USER_ENTITY_ID.hashCode();

    @Mock
    private UserSystemService userSystemService;

    @Mock
    private ScoreDirector<TaskAssigningSolution> scoreDirector;

    private SolverHandlerContext context;

    @Before
    public void setUp() {
        context = new SolverHandlerContext();
    }

    @Test
    public void addNewReadyTaskChange() {
        TaskData taskData = mockTaskData(TASK_ID, NAME, Ready, null);
        List<TaskData> taskDataList = mockTaskDataList(taskData);
        TaskAssigningSolution solution = mockSolution(Collections.emptyList(), Collections.emptyList());

        List<ProblemFactChange<TaskAssigningSolution>> result = SolutionChangesBuilder.create()
                .withSolution(solution)
                .withTasks(taskDataList)
                .withUserSystem(userSystemService)
                .withContext(context)
                .build();

        assertEquals(3, result.size());

        AddTaskProblemFactChange expected = new AddTaskProblemFactChange(fromTaskData(taskData));
        assertChange(result, 1, expected);
    }

    @Test
    public void addNewReservedTaskChangeWithActualOwnerInSolution() {
        addNewReservedOrInProgressOrSuspendedTaskChangeWithActualOwnerInSolution(Reserved);
    }

    @Test
    public void addNewInProgressTaskChangeWithActualOwnerInSolution() {
        addNewReservedOrInProgressOrSuspendedTaskChangeWithActualOwnerInSolution(InProgress);
    }

    @Test
    public void addNewSuspendedTaskChangeWithActualOwnerInSolution() {
        addNewReservedOrInProgressOrSuspendedTaskChangeWithActualOwnerInSolution(Suspended);
    }

    @Test
    public void addNewReservedTaskChangeWithActualOwnerInExternalSystem() {
        addNewReservedOrInProgressOrSuspendedTaskChangeWithActualOwnerInExternalSystem(Reserved);
    }

    @Test
    public void addNewInProgressTaskChangeWithActualOwnerInExternalSystem() {
        addNewReservedOrInProgressOrSuspendedTaskChangeWithActualOwnerInExternalSystem(InProgress);
    }

    @Test
    public void addNewSuspendedTaskChangeWithActualOwnerInExternalSystem() {
        addNewReservedOrInProgressOrSuspendedTaskChangeWithActualOwnerInExternalSystem(Suspended);
    }

    @Test
    public void addNewReservedTaskChangeWithActualOwnerMissing() {
        addNewReservedOrInProgressOrSuspendedTaskChangeWithActualOwnerMissing(Reserved);
    }

    @Test
    public void addNewInProgressTaskChangeWithActualOwnerMissing() {
        addNewReservedOrInProgressOrSuspendedTaskChangeWithActualOwnerInExternalSystem(InProgress);
    }

    @Test
    public void addNewSuspendedTaskChangeWithActualOwnerMissing() {
        addNewReservedOrInProgressOrSuspendedTaskChangeWithActualOwnerInExternalSystem(Suspended);
    }

    @Test
    public void addReleasedTaskChange() {
        TaskData taskData = mockTaskData(TASK_ID, NAME, Ready, USER_ENTITY_ID);
        Task task = fromTaskData(taskData);
        task.setStatus(convertToString(Reserved));
        TaskAssigningSolution solution = mockSolution(Collections.singletonList(task), Collections.emptyList());

        List<ProblemFactChange<TaskAssigningSolution>> result = SolutionChangesBuilder.create()
                .withSolution(solution)
                .withTasks(mockTaskDataList(taskData))
                .withUserSystem(userSystemService)
                .withContext(context)
                .build();

        assertChange(result, 1, new ReleaseTaskProblemFactChange(task));
    }

    @Test
    public void addRemoveReservedTaskChangeWhenActualOwnerNotPresent() {
        addRemoveReservedOrInProgressOrSuspendedTaskChangeWhenActualOwnerNotPresent(Reserved);
    }

    @Test
    public void addRemoveInProgressTaskChangeWhenActualOwnerNotPresent() {
        addRemoveReservedOrInProgressOrSuspendedTaskChangeWhenActualOwnerNotPresent(InProgress);
    }

    @Test
    public void addRemoveSuspendedTaskChangeWhenActualOwnerNotPresent() {
        addRemoveReservedOrInProgressOrSuspendedTaskChangeWhenActualOwnerNotPresent(Suspended);
    }

    @Test
    public void addReassignReservedTaskChangeWhenActualItWasManuallyReassigned() {
        addReassignReservedOrInProgressOrSuspendedTaskWhenItWasManuallyReassigned(Reserved);
    }

    private void addNewReservedOrInProgressOrSuspendedTaskChangeWithActualOwnerInSolution(Status status) {
        TaskData taskData = mockTaskData(TASK_ID, NAME, status, USER_ENTITY_ID);
        TaskAssigningSolution solution = mockSolution(Collections.emptyList(), Collections.singletonList(mockUser(USER_ID, USER_ENTITY_ID)));
        addNewReservedOrInProgressOrSuspendedTaskChangeWithActualOwner(solution, taskData);
    }

    private void addNewReservedOrInProgressOrSuspendedTaskChangeWithActualOwnerInExternalSystem(Status status) {
        TaskData taskData = mockTaskData(TASK_ID, NAME, status, USER_ENTITY_ID);
        org.kie.server.services.taskassigning.user.system.api.User externalUser = mockExternalUser(USER_ENTITY_ID, true);
        when(userSystemService.findUser(USER_ENTITY_ID)).thenReturn(externalUser);
        TaskAssigningSolution solution = mockSolution(Collections.emptyList(), Collections.emptyList());
        addNewReservedOrInProgressOrSuspendedTaskChangeWithActualOwner(solution, taskData);
        verify(userSystemService).findUser(USER_ENTITY_ID);
    }

    private void addNewReservedOrInProgressOrSuspendedTaskChangeWithActualOwnerMissing(Status status) {
        TaskData taskData = mockTaskData(TASK_ID, NAME, status, USER_ENTITY_ID);
        TaskAssigningSolution solution = mockSolution(Collections.emptyList(), Collections.emptyList());
        addNewReservedOrInProgressOrSuspendedTaskChangeWithActualOwner(solution, taskData);
        verify(userSystemService).findUser(USER_ENTITY_ID);
    }

    private void addNewReservedOrInProgressOrSuspendedTaskChangeWithActualOwner(TaskAssigningSolution solution, TaskData taskData) {
        List<TaskData> taskDataList = mockTaskDataList(taskData);
        List<ProblemFactChange<TaskAssigningSolution>> result = SolutionChangesBuilder.create()
                .withSolution(solution)
                .withTasks(taskDataList)
                .withUserSystem(userSystemService)
                .withContext(context)
                .build();

        assertEquals(3, result.size());

        AssignTaskProblemFactChange expected = new AssignTaskProblemFactChange(fromTaskData(taskData), mockUser(USER_ID, USER_ENTITY_ID), true);
        assertChange(result, 1, expected);
    }

    private void addRemoveReservedOrInProgressOrSuspendedTaskChangeWhenActualOwnerNotPresent(Status status) {
        TaskData taskData = mockTaskData(TASK_ID, NAME, status, null);
        Task task = fromTaskData(taskData);

        TaskAssigningSolution solution = mockSolution(Collections.singletonList(task), Collections.emptyList());

        List<ProblemFactChange<TaskAssigningSolution>> result = SolutionChangesBuilder.create()
                .withSolution(solution)
                .withTasks(mockTaskDataList(taskData))
                .withUserSystem(userSystemService)
                .withContext(context)
                .build();

        assertChange(result, 1, new RemoveTaskProblemFactChange(task));
    }

    private void addReassignReservedOrInProgressOrSuspendedTaskWhenItWasManuallyReassigned(Status status) {

        // TODO, quedé en este metodo
//        TaskData taskData = mockTaskData(TASK_ID, NAME, status, null);
//        Task task = fromTaskData(taskData);
//
//        TaskAssigningSolution solution = mockSolution(Collections.singletonList(task), Collections.emptyList());
//
//        List<ProblemFactChange<TaskAssigningSolution>> result = SolutionChangesBuilder.create()
//                .withSolution(solution)
//                .withTasks(mockTaskDataList(taskData))
//                .withUserSystem(userSystemService)
//                .withContext(context)
//                .build();
//
//        assertChange(result, 1, new RemoveTaskProblemFactChange(task));
    }

    private void assertChange(List<ProblemFactChange<TaskAssigningSolution>> result, int index, AddTaskProblemFactChange expected) {
        AddTaskProblemFactChange change = (AddTaskProblemFactChange) result.get(index);
        assertTaskEquals(expected.getTask(), change.getTask());
    }

    private void assertChange(List<ProblemFactChange<TaskAssigningSolution>> result, int index, AssignTaskProblemFactChange expected) {
        AssignTaskProblemFactChange change = (AssignTaskProblemFactChange) result.get(index);
        assertTaskEquals(expected.getTask(), change.getTask());
        assertUserEquals(expected.getUser(), change.getUser());
    }

    private void assertChange(List<ProblemFactChange<TaskAssigningSolution>> result, int index, ReleaseTaskProblemFactChange expected) {
        ReleaseTaskProblemFactChange change = (ReleaseTaskProblemFactChange) result.get(index);
        assertTaskEquals(expected.getTask(), change.getTask());
    }

    private void assertChange(List<ProblemFactChange<TaskAssigningSolution>> result, int index, RemoveTaskProblemFactChange expected) {
        RemoveTaskProblemFactChange change = (RemoveTaskProblemFactChange) result.get(index);
        assertTaskEquals(expected.getTask(), change.getTask());
    }

    private TaskData mockTaskData(long taskId, String name, Status status, String actualOwner) {
        return TaskData.builder()
                .taskId(taskId)
                .processInstanceId(PROCESS_INSTANCE_ID)
                .processId(PROCESS_ID)
                .containerId(CONTAINER_ID)
                .name(name)
                .priority(PRIORITY)
                .status(convertToString(status))
                .actualOwner(actualOwner)
                .inputData(INPUT_DATA)
                .build();
    }

    private void assertTaskEquals(Task t1, Task t2) {
        assertEquals(t1.getId(), t2.getId(), 0);
    }

    private void assertUserEquals(User user1, User user2) {
        assertEquals(user1.getId(), user2.getId(), 0);
        assertEquals(user1.getEntityId(), user2.getEntityId());
    }

    private User mockUser(long userId, String entityId) {
        return new User(userId, entityId);
    }

    private TaskAssigningSolution mockSolution(List<Task> task, List<User> users) {
        return new TaskAssigningSolution(1L, users, task);
    }

    private List<TaskData> mockTaskDataList(TaskData... tasks) {
        return Arrays.asList(tasks);
    }

    private List<User> mockUserList(User... users) {
        return Arrays.asList(users);
    }
}
