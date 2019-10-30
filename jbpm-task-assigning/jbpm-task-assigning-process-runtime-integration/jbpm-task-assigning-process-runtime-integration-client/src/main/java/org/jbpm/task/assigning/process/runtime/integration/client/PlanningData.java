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

package org.jbpm.task.assigning.process.runtime.integration.client;

/**
 * Keeps the information configured/assigned by OptaPlanner when a solution is planned into the jBPM runtime.
 * This information is good enough for restoring a solution and start the solver from the last planned solution.
 */
public interface PlanningData {

    long getTaskId();

    void setTaskId(long taskId);

    int getIndex();

    void setIndex(int index);

    boolean isPublished();

    void setPublished(boolean published);

    void setAssignedUser(String assignedUser);

    String getAssignedUser();
}
