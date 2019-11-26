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

package org.kie.server.services.taskassigning.runtime;

/**
 * A container class for having the centralized definition of different constants that are used by the task assigning
 * kie-server modules.
 */
public class TaskAssigningConstants {

    /**
     * Property name for establishing if the TaskAssigningKieServerExtension must be disabled.
     */
    public static final String JBPM_TASK_ASSIGNING_EXT_DISABLED = "org.jbpm.server.taskAssigning.ext.disabled";

    /**
     * Property for configuring the rest endpoint url of the kie-server with the jBPM runtime.
     */
    public static final String JBPM_TASK_ASSIGNING_PROCESS_RUNTIME_URL = "org.jbpm.server.taskAssigning.processRuntime.url";

    /**
     * Property for configuring the user for connecting with the with the jBPM runtime.
     */
    public static final String JBPM_TASK_ASSIGNING_PROCESS_RUNTIME_USER = "org.jbpm.server.taskAssigning.processRuntime.user";

    /**
     * Property for configuring the user password for connecting with the jBPM runtime.
     */
    public static final String JBPM_TASK_ASSIGNING_PROCESS_RUNTIME_PWD = "org.jbpm.server.taskAssigning.processRuntime.pwd";

    /**
     * Property for configuring a user identifier for using as the "on behalf of" user when interacting with the jBPM runtime.
     */
    public static final String JBPM_TASK_ASSIGNING_PROCESS_RUNTIME_TARGET_USER = "org.jbpm.server.taskAssigning.processRuntime.targetUser";

    /**
     * Property for configuring the jndi datasource to be used by the PlanningDataService.
     */
    public static final String JBPM_TASK_ASSIGNING_CFG_PERSISTANCE_DS = "org.kie.server.persistence.taskAssigning.ds";

    /**
     * Property for configuring the size of the tasks publish window.
     */
    public static final String JBPM_TASK_ASSIGNING_PUBLISH_WINDOW_SIZE = "org.jbpm.server.taskAssigning.publishWindowSize";

    /**
     * Property for configuring the solution synchronization period in milliseconds.
     */
    public static final String JBPM_TASK_ASSIGNING_SYNC_PERIOD = "org.jbpm.server.taskAssigning.solutionSyncPeriod";

    /**
     * Property for configuring the resource with the solver configuration.
     */
    public static final String TASK_ASSIGNING_SOLVER_CONFIG_RESOURCE = "org.jbpm.services.task.assigning.solverConfigResource";

}
