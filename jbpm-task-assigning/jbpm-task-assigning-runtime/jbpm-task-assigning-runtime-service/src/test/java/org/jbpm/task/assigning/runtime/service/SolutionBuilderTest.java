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
import java.util.Arrays;
import java.util.List;

import org.jbpm.task.assigning.model.Task;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SolutionBuilderTest {

    @Test
    public void addInOrderTest() {

        SolutionBuilder.AssignedTask task1 = new SolutionBuilder.AssignedTask(new Task(-1, "Task1", -1), 4, true);
        SolutionBuilder.AssignedTask task2 = new SolutionBuilder.AssignedTask(new Task(-1, "Task2", -1), 7, true);
        SolutionBuilder.AssignedTask task3 = new SolutionBuilder.AssignedTask(new Task(-1, "Task3", -1), 1, false);
        SolutionBuilder.AssignedTask task4 = new SolutionBuilder.AssignedTask(new Task(-1, "Task4", -1), 3, false);
        SolutionBuilder.AssignedTask task5 = new SolutionBuilder.AssignedTask(new Task(-1, "Task5", -1), 8, false);
        List<SolutionBuilder.AssignedTask> tasks = new ArrayList<>(Arrays.asList(task1, task2, task3, task4, task5));

        List<SolutionBuilder.AssignedTask> tasksByAdding = new ArrayList<>();
        tasks.forEach(assignedTask -> SolutionBuilder.addInOrder(tasksByAdding, assignedTask.getTask(), assignedTask.getIndex(), assignedTask.isPinned()));
        for (int i = 0; i < tasks.size(); i++) {
            assertEquals(tasks.get(i).getTask().getName(), tasksByAdding.get(i).getTask().getName());
        }

        Task newTask6 = new Task(-1, "newTask6", -1);
        Task newTask7 = new Task(-1, "newTask7", -1);
        Task newTask8 = new Task(-1, "newTask8", -1);
        Task newTask9 = new Task(-1, "newTask9", -1);
        Task newTask10 = new Task(-1, "newTask10", -1);
        Task newTask11 = new Task(-1, "newTask11", -1);
        Task newTask12 = new Task(-1, "newTask12", -1);
        Task newTask13 = new Task(-1, "newTask13", -1);
        Task newTask14 = new Task(-1, "newTask14", -1);

        SolutionBuilder.addInOrder(tasks, newTask6, -1, true);
        assertTaskInPosition(tasks, newTask6, 2);

        SolutionBuilder.addInOrder(tasks, newTask7, -1, true);
        assertTaskInPosition(tasks, newTask7, 3);

        SolutionBuilder.addInOrder(tasks, newTask8, 5, true);
        assertTaskInPosition(tasks, newTask8, 1);

        SolutionBuilder.addInOrder(tasks, newTask9, 0, false);
        assertTaskInPosition(tasks, newTask9, 5);

        SolutionBuilder.addInOrder(tasks, newTask10, -1, false);
        assertTaskInPosition(tasks, newTask10, 9);

        SolutionBuilder.addInOrder(tasks, newTask11, 2, false);
        assertTaskInPosition(tasks, newTask11, 7);

        SolutionBuilder.addInOrder(tasks, newTask12, 7, true);
        assertTaskInPosition(tasks, newTask12, 3);

        SolutionBuilder.addInOrder(tasks, newTask13, 2, false);
        assertTaskInPosition(tasks, newTask13, 9);

        SolutionBuilder.addInOrder(tasks, newTask14, 3, true);
        assertTaskInPosition(tasks, newTask14, 0);
    }

    private void assertTaskInPosition(List<SolutionBuilder.AssignedTask> tasks, Task expectedTask, int index) {
        assertEquals(expectedTask, tasks.get(index).getTask());
    }
}
