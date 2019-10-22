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

import org.jbpm.task.assigning.model.TaskAssigningSolution;
import org.optaplanner.core.impl.score.director.ScoreDirector;
import org.optaplanner.core.impl.solver.ProblemFactChange;

public class ReleaseTaskProblemFactChange implements ProblemFactChange<TaskAssigningSolution> {

    private long id;

    public ReleaseTaskProblemFactChange(long id) {
        this.id = id;
    }

    @Override
    public void doChange(ScoreDirector<TaskAssigningSolution> scoreDirector) {
        //TODO implement here.
        throw new RuntimeException("fact change not implemented");
    }
}
