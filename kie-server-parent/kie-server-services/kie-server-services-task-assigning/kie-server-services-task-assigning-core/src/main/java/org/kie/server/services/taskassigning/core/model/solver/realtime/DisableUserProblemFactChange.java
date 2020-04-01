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

package org.kie.server.services.taskassigning.core.model.solver.realtime;

import org.kie.server.services.taskassigning.core.model.TaskAssigningSolution;
import org.kie.server.services.taskassigning.core.model.User;
import org.optaplanner.core.impl.score.director.ScoreDirector;
import org.optaplanner.core.impl.solver.ProblemFactChange;

import static org.kie.server.services.taskassigning.core.model.solver.realtime.ProblemFactChangeUtil.releaseNonPinnedTasks;

public class DisableUserProblemFactChange implements ProblemFactChange<TaskAssigningSolution> {

    private User user;

    public DisableUserProblemFactChange(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    @Override
    public void doChange(ScoreDirector<TaskAssigningSolution> scoreDirector) {
        final User workingUser = scoreDirector.lookUpWorkingObjectOrReturnNull(user);
        if (workingUser != null) {
            scoreDirector.beforeProblemPropertyChanged(workingUser);
            workingUser.setEnabled(false);
            scoreDirector.afterProblemPropertyChanged(workingUser);
            releaseNonPinnedTasks(workingUser, scoreDirector);
            scoreDirector.triggerVariableListeners();
        }
    }
}