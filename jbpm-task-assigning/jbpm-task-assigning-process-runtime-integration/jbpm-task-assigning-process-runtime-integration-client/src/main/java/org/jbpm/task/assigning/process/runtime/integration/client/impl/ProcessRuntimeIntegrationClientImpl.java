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

package org.jbpm.task.assigning.process.runtime.integration.client.impl;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.jbpm.task.assigning.process.runtime.integration.client.PotentialOwner;
import org.jbpm.task.assigning.process.runtime.integration.client.ProcessRuntimeIntegrationClient;
import org.jbpm.task.assigning.process.runtime.integration.client.TaskInfo;
import org.jbpm.task.assigning.process.runtime.integration.client.TaskPlanningInfo;
import org.jbpm.task.assigning.process.runtime.integration.client.TaskStatus;
import org.kie.server.api.marshalling.Marshaller;
import org.kie.server.api.marshalling.MarshallerFactory;
import org.kie.server.api.marshalling.MarshallingFormat;
import org.kie.server.api.model.definition.QueryDefinition;
import org.kie.server.api.model.definition.QueryFilterSpec;
import org.kie.server.api.util.QueryFilterSpecBuilder;
import org.kie.server.client.QueryServicesClient;
import org.kie.server.client.UserTaskServicesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.jbpm.task.assigning.process.runtime.integration.client.impl.ProcessRuntimeIntegrationClientImpl.TASK_QUERY_COLUMN.ACTUAL_OWNER;
import static org.jbpm.task.assigning.process.runtime.integration.client.impl.ProcessRuntimeIntegrationClientImpl.TASK_QUERY_COLUMN.CREATED_ON;
import static org.jbpm.task.assigning.process.runtime.integration.client.impl.ProcessRuntimeIntegrationClientImpl.TASK_QUERY_COLUMN.DEPLOYMENT_ID;
import static org.jbpm.task.assigning.process.runtime.integration.client.impl.ProcessRuntimeIntegrationClientImpl.TASK_QUERY_COLUMN.LAST_MODIFICATION_DATE;
import static org.jbpm.task.assigning.process.runtime.integration.client.impl.ProcessRuntimeIntegrationClientImpl.TASK_QUERY_COLUMN.POTENTIAL_OWNER_ID;
import static org.jbpm.task.assigning.process.runtime.integration.client.impl.ProcessRuntimeIntegrationClientImpl.TASK_QUERY_COLUMN.PRIORITY;
import static org.jbpm.task.assigning.process.runtime.integration.client.impl.ProcessRuntimeIntegrationClientImpl.TASK_QUERY_COLUMN.PROCESS_ID;
import static org.jbpm.task.assigning.process.runtime.integration.client.impl.ProcessRuntimeIntegrationClientImpl.TASK_QUERY_COLUMN.PROCESS_INSTANCE_ID;
import static org.jbpm.task.assigning.process.runtime.integration.client.impl.ProcessRuntimeIntegrationClientImpl.TASK_QUERY_COLUMN.STATUS;
import static org.jbpm.task.assigning.process.runtime.integration.client.impl.ProcessRuntimeIntegrationClientImpl.TASK_QUERY_COLUMN.TASK_ID;
import static org.jbpm.task.assigning.process.runtime.integration.client.impl.ProcessRuntimeIntegrationClientImpl.TASK_QUERY_COLUMN.TASK_NAME;

public class ProcessRuntimeIntegrationClientImpl implements ProcessRuntimeIntegrationClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessRuntimeIntegrationClient.class);

    private UserTaskServicesClient userTaskServicesClient;
    private QueryServicesClient queryServicesClient;

    protected enum POTENTIAL_OWNER_TYPE {
        USER("User"),
        GROUP("Group");

        private String value;

        POTENTIAL_OWNER_TYPE(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    /**
     * Represents the columns returned by the "jbpm-task-assigning-human-task-with-user" query.
     * This enum must be kept in sync with any change made in this query.
     */
    protected enum TASK_QUERY_COLUMN {
        /**
         * A Long value with the taskId. Is never null.
         */
        TASK_ID,

        /**
         * A time stamp without time zone with the task creation date/time. Is never null.
         */
        CREATED_ON,

        /**
         * A time stamp without time zone with the task activation date/time. Can be null.
         */
        ACTIVATION_TIME,

        /**
         * String with the task actual owner id. Can be null.
         */
        ACTUAL_OWNER,

        /**
         * A String with the deploymentId (containerId) to where the task belong. Is never null.
         */
        DEPLOYMENT_ID,

        /**
         * A time stamp without time zone with the task due date. Can be null.
         */
        DUE_DATE,

        /**
         * A String with the task name. Is never null.
         */
        TASK_NAME,

        /**
         * An Integer with the task priority. Is never null.
         */
        PRIORITY,

        /**
         * A String with the process identifier. Is never null.
         */
        PROCESS_ID,

        /**
         * A Long value with the process instance id. Is never null.
         */
        PROCESS_INSTANCE_ID,

        /**
         * A String value with the task status. Is never null.
         */
        STATUS,

        /**
         * A time stamp without time zone with the task last modification date. Is never null.
         */
        LAST_MODIFICATION_DATE,

        /**
         * A String value with the process name. Is never null.
         */
        PROCESS_INSTANCE_DESCRIPTION,

        /**
         * A String with a potential owner identifier for the task. (a task can have many potential owners). Can be null.
         */
        POTENTIAL_OWNER_ID,

        /**
         * A String in the set {User" or "Group"} with the type of the task potential owner.
         * (a task can have many potential owners)
         * If POTENTIAL_OWNER_ID != null => POTENTIAL_OWNER_TYPE != null.
         * If POTENTIAL_OWNER_ID == null => POTENTIAL_OWNER_TYPE == null.
         */
        POTENTIAL_OWNER_TYPE

    }

    public ProcessRuntimeIntegrationClientImpl(UserTaskServicesClient userTaskServicesClient, QueryServicesClient queryServicesClient) {
        this.userTaskServicesClient = userTaskServicesClient;
        this.queryServicesClient = queryServicesClient;
        init();
    }

    @Override
    public List<TaskInfo> findTasks(Long fromTaskId, Date lastModificationDate, List<TaskStatus> status, Integer page, Integer pageSize) {
        return findTasks(new FindTasksQueryFilterSpecBuilder()
                                 .fromTaskId(fromTaskId)
                                 .fromLastModificationDate(lastModificationDate)
                                 .withStatusIn(status)
                                 .build(),
                         page,
                         pageSize);
    }

    @Override
    public void delegateTask(TaskPlanningInfo planningInfo, String userId) {
        userTaskServicesClient.delegateTask(planningInfo.getContainerId(), planningInfo.getTaskId(), userId, planningInfo.getPlanningTask().getAssignedUser());
    }

    private void init() {
        registerQueries();
    }

    public List<TaskInfo> findTasks(Long fromTaskId, Long toTaskId, List<TaskStatus> status, Integer page, Integer pageSize) {
        return findTasks(new FindTasksQueryFilterSpecBuilder()
                                 .withStatusIn(status)
                                 .fromTaskId(fromTaskId)
                                 .toTaskId(toTaskId).build(),
                         page,
                         pageSize);
    }

    private static class FindTasksQueryFilterSpecBuilder {

        private Long fromTaskId;
        private Long toTaskId;
        private Date lastModificationDate;
        private List<TaskStatus> statusIn;
        private static final String TASK_ID_COLUMN = "taskId";
        private static final String STATUS_COLUMN = "status";
        private static final String LAST_MODIFICATION_DATE_COLUMN = "lastModificationDate";

        private FindTasksQueryFilterSpecBuilder() {
        }

        private FindTasksQueryFilterSpecBuilder fromTaskId(Long taskId) {
            this.fromTaskId = taskId;
            return this;
        }

        private FindTasksQueryFilterSpecBuilder toTaskId(Long taskId) {
            this.toTaskId = taskId;
            return this;
        }

        private FindTasksQueryFilterSpecBuilder fromLastModificationDate(Date lastModificationDate) {
            this.lastModificationDate = lastModificationDate;
            return this;
        }

        private FindTasksQueryFilterSpecBuilder withStatusIn(List<TaskStatus> status) {
            this.statusIn = status;
            return this;
        }

        private QueryFilterSpec build() {
            final QueryFilterSpecBuilder builder = new QueryFilterSpecBuilder();
            if (statusIn != null && !statusIn.isEmpty()) {
                builder.equalsTo(STATUS_COLUMN, statusIn.stream().map(Enum::name).toArray(String[]::new));
            }
            if (fromTaskId != null && toTaskId != null) {
                builder.between(TASK_ID_COLUMN, fromTaskId, toTaskId);
            } else if (fromTaskId != null) {
                builder.greaterOrEqualTo(TASK_ID_COLUMN, fromTaskId);
            } else if (toTaskId != null) {
                builder.lowerOrEqualTo(TASK_ID_COLUMN, toTaskId);
            }
            if (lastModificationDate != null) {
                //builder.greaterOrEqualTo(LAST_MODIFICATION_DATE_COLUMN, lastModificationDate);
                builder.greaterOrEqualTo(LAST_MODIFICATION_DATE_COLUMN, new LocalDateTimeParam(LocalDateTime.now()));
                //builder.greaterOrEqualTo(LAST_MODIFICATION_DATE_COLUMN, LocalDateTime.now());
            }

            builder.oderBy(TASK_ID_COLUMN, true);
            return builder.get();
        }
    }

    private List<TaskInfo> findTasks(QueryFilterSpec queryFilter, Integer page, Integer pageSize) {
        final List rawList = queryServicesClient.query("jbpm-task-assigning-human-task-with-user",
                                                       //"RawList",
                                                       "TaskAssigningMapper",
                                                       queryFilter,
                                                       page,
                                                       pageSize,
                                                       List.class);
        final List<TaskInfo> result = new ArrayList<>();
        List<Object> row;
        long previousTaskId = -1;
        long taskId;
        TaskInfo taskInfo = null;
        String potentialOwnerId;
        String potentialOwnerType;
        String actualOwner;

        for (Object o : rawList) {
            row = (List<Object>) o;
            taskId = toLong(row.get(TASK_ID.ordinal()));
            if (previousTaskId != taskId) {
                previousTaskId = taskId;
                taskInfo = new TaskInfo();
                taskInfo.setPotentialOwners(new HashSet<>());

                taskInfo.setTaskId(taskId);
                taskInfo.setCreatedOn(toLocalDateTime(row.get(CREATED_ON.ordinal())));
                taskInfo.setProcessInstanceId(toLong(row.get(PROCESS_INSTANCE_ID.ordinal())));
                taskInfo.setProcessId(toString(row.get(PROCESS_ID.ordinal())));
                taskInfo.setContainerId(toString(row.get(DEPLOYMENT_ID.ordinal())));
                taskInfo.setStatus(TaskStatus.valueOf(toString(row.get(STATUS.ordinal()))));
                taskInfo.setPriority(toInt(row.get(PRIORITY.ordinal())));
                taskInfo.setName(toString(row.get(TASK_NAME.ordinal())));
                taskInfo.setLastModificationDate(toLocalDateTime(row.get(LAST_MODIFICATION_DATE.ordinal())));
                actualOwner = toString(row.get(ACTUAL_OWNER.ordinal()));
                if (isNotEmpty(actualOwner)) {
                    taskInfo.setActualOwner(actualOwner);
                }
                taskInfo.setInputData(userTaskServicesClient.getTaskInputContentByTaskId(taskInfo.getContainerId(), taskId));
                result.add(taskInfo);
            }

            potentialOwnerId = toString(row.get(POTENTIAL_OWNER_ID.ordinal()));
            if (isNotEmpty(potentialOwnerId)) {
                potentialOwnerType = toString(row.get(TASK_QUERY_COLUMN.POTENTIAL_OWNER_TYPE.ordinal()));
                taskInfo.getPotentialOwners().add(new PotentialOwner(POTENTIAL_OWNER_TYPE.USER.value().equals(potentialOwnerType), potentialOwnerId));
            }
        }
        return result;
    }

    private void registerQueries() {
        try (InputStream stream = this.getClass().getResourceAsStream("/jbpm-task-assigning-query-definitions.json")) {
            if (stream == null) {
                LOGGER.info("jBPM task assigning queries file was not found: jbpm-task-assigning-query-definitions.json");
                return;
            }
            final Marshaller marshaller = MarshallerFactory.getMarshaller(MarshallingFormat.JSON,
                                                                          getClass().getClassLoader());
            final String queriesString = IOUtils.toString(stream, StandardCharsets.UTF_8);
            final QueryDefinition[] queries = marshaller.unmarshall(queriesString, QueryDefinition[].class);

            if (queries == null || queries.length == 0) {
                LOGGER.info("No queries were found");
                return;
            }
            registerQueries(queries);
        } catch (Exception e) {
            LOGGER.error("An error was produced during jbpm-task-assigning-query-definitions initialization", e);
        }
    }

    private void registerQueries(QueryDefinition[] queries) {
        Stream.of(queries).forEach(query -> queryServicesClient.replaceQuery(query));
    }

    private static long toLong(Object value) {
        if (value instanceof Long) {
            return (long) value;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.valueOf(value.toString());
    }

    private static int toInt(Object value) {
        if (value instanceof Integer) {
            return (int) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.valueOf(value.toString());
    }

    private static LocalDateTime toLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Date) {
            return LocalDateTime.ofInstant(((Date) value).toInstant(), ZoneId.systemDefault());
        }
        if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        }
        throw new RuntimeException(String.format("Unexpected type %s for toLocalDateTime conversion.", value.getClass()));
    }

    private static String toString(Object value) {
        return value != null ? value.toString() : null;
    }
}
