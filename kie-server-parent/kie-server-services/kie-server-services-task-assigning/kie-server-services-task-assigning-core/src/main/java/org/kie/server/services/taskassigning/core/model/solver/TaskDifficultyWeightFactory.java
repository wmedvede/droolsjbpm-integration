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

import org.kie.server.services.taskassigning.core.model.Task;
import org.kie.server.services.taskassigning.core.model.TaskAssigningSolution;
import org.optaplanner.core.impl.heuristic.selector.common.decorator.SelectionSorterWeightFactory;

public class TaskDifficultyWeightFactory implements SelectionSorterWeightFactory<TaskAssigningSolution, Task> {

    @Override
    public Comparable createSorterWeight(TaskAssigningSolution solution, Task selection) {
        //Here we know that a task wants to be evaluated, but the task is assigned to nobody
        //so there's no way to know the weight of the task regarding the already assigned ones,
        //and the makespan/fairness.
        //Again we have to make a local decision based on the task priority, or how many groups, etc.
        //but this decision don't warrante the makespan.

        //In the VRP application there's a fixed calculation regarding the depot but NOT in this scenario.

        /*

        PlanningDepot depot = solution.getDepotList().get(0);
                return new DepotAngleVisitDifficultyWeight(
                visit,
                // angle of the line from visit to depot relative to visit→east
                visit.getLocation().angleTo(depot.getLocation()),
                visit.getLocation().distanceTo(depot.getLocation())
                        + depot.getLocation().distanceTo(visit.getLocation())
        );

         */

        return null;
    }
}
