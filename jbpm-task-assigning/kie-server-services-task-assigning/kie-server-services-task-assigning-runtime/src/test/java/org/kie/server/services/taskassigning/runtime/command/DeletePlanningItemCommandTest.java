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

import org.junit.Test;
import org.kie.server.services.taskassigning.runtime.persistence.PlanningTaskImpl;
import org.mockito.Mock;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DeletePlanningItemCommandTest extends AbstractPlanningCommandTest<DeletePlanningItemCommand> {

    @Mock
    private PlanningTaskImpl planningTask;

    @Override
    protected DeletePlanningItemCommand createCommand() {
        return new DeletePlanningItemCommand(TASK_ID);
    }

    @Test
    public void execute() {
        when(persistenceContext.find(PlanningTaskImpl.class, TASK_ID)).thenReturn(planningTask);
        command.execute(taskContext);
        verify(persistenceContext).remove(planningTask);
    }
}
