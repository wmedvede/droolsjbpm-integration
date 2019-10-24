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

package org.jbpm.task.assigning.model.solver.realtime;

import org.jbpm.task.assigning.model.Task;
import org.jbpm.task.assigning.model.TaskAssigningSolution;
import org.jbpm.task.assigning.model.TaskOrUser;
import org.optaplanner.core.impl.score.director.ScoreDirector;
import org.optaplanner.core.impl.solver.ProblemFactChange;

public class ReleaseTaskProblemFactChange implements ProblemFactChange<TaskAssigningSolution> {

    private Task task;

    public ReleaseTaskProblemFactChange(Task task) {
        this.task = task;
    }

    @Override
    public void doChange(ScoreDirector<TaskAssigningSolution> scoreDirector) {
        Task workingTask = scoreDirector.lookUpWorkingObjectOrReturnNull(task);
        if (workingTask == null || workingTask.getPreviousTaskOrUser() == null) {
            // The task could have been removed in the middle by a previous change
            // or it's simply not yet assigned.
            return;
        }

        //un-link the task from the chain.
        TaskOrUser previousTaskOrUser = workingTask.getPreviousTaskOrUser();
        Task nextTask = workingTask.getNextTask();
        if (nextTask != null) {
            //re-link the chain where the workingTask belonged if any
            scoreDirector.beforeVariableChanged(nextTask, "previousTaskOrUser");
            nextTask.setPreviousTaskOrUser(previousTaskOrUser);
            scoreDirector.afterVariableChanged(nextTask, "previousTaskOrUser");
        }
        scoreDirector.beforeVariableChanged(workingTask, "previousTaskOrUser");
        workingTask.setPreviousTaskOrUser(null);
        scoreDirector.afterVariableChanged(workingTask, "previousTaskOrUser");
        if (workingTask.isPinned()) {
            scoreDirector.beforeProblemPropertyChanged(workingTask);
            workingTask.setPinned(false);
            scoreDirector.afterProblemPropertyChanged(workingTask);
        }
        scoreDirector.triggerVariableListeners();
    }
}