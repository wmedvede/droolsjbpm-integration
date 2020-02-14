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

package org.kie.server.services.taskassigning.runtime.command;

import java.util.Date;

import org.kie.api.runtime.Context;
import org.kie.server.api.model.taskassigning.PlanningItem;
import org.kie.server.services.taskassigning.runtime.persistence.PlanningTaskImpl;

/**
 * Helper class intended to be used by the TaskAssigningRuntimeServiceBase and planning execution.
 */
public class SavePlanningItemCommand extends PlanningCommand {

    public SavePlanningItemCommand(PlanningItem planningItem) {
        super(planningItem);
    }

    @Override
    public Object execute(Context context) {
        super.execute(context);
        persistenceContext.merge(new PlanningTaskImpl(planningItem.getTaskId(),
                                                      planningItem.getPlanningTask().getAssignedUser(),
                                                      planningItem.getPlanningTask().getIndex(),
                                                      planningItem.getPlanningTask().isPublished(),
                                                      new Date()));
        return null;
    }
}
