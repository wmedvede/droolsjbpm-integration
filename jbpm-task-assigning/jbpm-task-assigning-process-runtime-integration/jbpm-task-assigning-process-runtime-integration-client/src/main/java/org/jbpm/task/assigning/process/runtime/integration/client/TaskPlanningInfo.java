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
 * Keeps the information for executing a planning into the jBPM engine.
 */
public class TaskPlanningInfo {

    private String containerId;
    private long taskId;
    private long processInstanceId;
    private PlanningParameters planningParameters = new PlanningParameters();

    public TaskPlanningInfo() {
    }

    public TaskPlanningInfo(String containerId, long taskId, long processInstanceId) {
        this.containerId = containerId;
        this.taskId = taskId;
        this.processInstanceId = processInstanceId;
    }

    public String getContainerId() {
        return containerId;
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }

    public long getTaskId() {
        return taskId;
    }

    public void setTaskId(long taskId) {
        this.taskId = taskId;
    }

    public long getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(long processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public PlanningParameters getPlanningParameters() {
        return planningParameters;
    }
}
