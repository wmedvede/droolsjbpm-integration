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

package org.kie.server.services.taskassigning.core.model.solver;

import org.kie.server.services.taskassigning.core.model.ModelConstants;
import org.kie.server.services.taskassigning.core.model.Task;
import org.kie.server.services.taskassigning.core.model.TaskAssigningSolution;
import org.kie.server.services.taskassigning.core.model.TaskOrUser;
import org.kie.server.services.taskassigning.core.model.User;
import org.optaplanner.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import org.optaplanner.core.impl.heuristic.selector.move.generic.SwapMove;
import org.optaplanner.core.impl.score.director.ScoreDirector;

public class TaskSwapMoveFilter implements SelectionFilter<TaskAssigningSolution, SwapMove<TaskAssigningSolution>> {

    @Override
    public boolean accept(ScoreDirector<TaskAssigningSolution> scoreDirector, SwapMove<TaskAssigningSolution> move) {
        //Would be nice to apply, here we can probably have the chance of
        //implementing a build-in constraint with groups and skills condition....
        //and doing an early exclusion of certain moves.
        //BUT this can only be configured in the LS, not possible to use in the CH
        TaskOrUser leftTask = (TaskOrUser) move.getLeftEntity();
        TaskOrUser rightTask = (TaskOrUser) move.getRightEntity();

        if (leftTask instanceof User || leftTask.getUser() == null) {
            return true;
        }

        User user = leftTask.getUser();
        return user.isEnabled() && (TaskHelper.isPotentialOwner((Task)rightTask, user) || ModelConstants.IS_PLANNING_USER.test(user.getEntityId()));
    }
}
