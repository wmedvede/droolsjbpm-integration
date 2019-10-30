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

package org.jbpm.task.assigning.runtime.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.jbpm.task.assigning.model.Task;
import org.jbpm.task.assigning.model.TaskAssigningSolution;
import org.jbpm.task.assigning.model.User;
import org.jbpm.task.assigning.process.runtime.integration.client.ProcessRuntimeIntegrationClient;
import org.jbpm.task.assigning.process.runtime.integration.client.TaskPlanningInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jbpm.task.assigning.model.Task.DUMMY_TASK;
import static org.jbpm.task.assigning.model.Task.DUMMY_TASK_PLANNER_241;
import static org.kie.soup.commons.validation.PortablePreconditions.checkCondition;
import static org.kie.soup.commons.validation.PortablePreconditions.checkNotNull;

/**
 * This class manges the processing of new a solution produced by the solver. It must typically apply all the required
 * changes in the jBPM runtime and eventually produce problem fact changes if the published tasks needs to be adjusted.
 */
public class SolutionProcessor extends RunnableBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(SolutionProcessor.class);

    private final ProcessRuntimeIntegrationClient runtimeClient;
    private final Consumer<Result> resultConsumer;
    private final String targetUserId;
    private final int publishWindowSize;

    private final Semaphore solutionResource = new Semaphore(0);
    private final AtomicBoolean processing = new AtomicBoolean(false);

    private TaskAssigningSolution solution;

    public static class Result {

        private Exception error;

        private Result(Exception error) {
            this.error = error;
        }

        public boolean hasError() {
            return error != null;
        }

        public Exception getError() {
            return error;
        }
    }

    /**
     * @param runtimeClient a ProcessRuntimeClient instance for executing methods into the jBPM runtime.
     * @param resultConsumer a consumer for processing the results.
     * @param targetUserId a user identifier for using as the "on behalf of" user when interacting with the jBPM runtime.
     * @param publishWindowSize Integer value > 0 that indicates the number of tasks to be published.
     */
    public SolutionProcessor(final ProcessRuntimeIntegrationClient runtimeClient,
                             final Consumer<Result> resultConsumer,
                             final String targetUserId,
                             final int publishWindowSize) {
        checkNotNull("runtimeClient", runtimeClient);
        checkNotNull("resultConsumer", resultConsumer);
        checkNotNull("targetUserId", targetUserId);
        checkCondition("publishWindowSize", publishWindowSize > 0);
        this.runtimeClient = runtimeClient;
        this.resultConsumer = resultConsumer;
        this.targetUserId = targetUserId;
        this.publishWindowSize = publishWindowSize;
    }

    /**
     * @return true if a solution is being processed at this time, false in any other case.
     */
    public boolean isProcessing() {
        return processing.get();
    }

    /**
     * This method is invoked form a different thread for doing the processing of a solution. This method is not
     * thread-safe and it's expected that any synchronization required between the isProcessing() and process()
     * methods is performed by the caller. Since only one solution can be processed at time, the caller should typically
     * execute in the following sequence.
     * if (!solutionProcessor.isProcessing()) {
     * solutionProcessor.process(solution);
     * } else {
     * //save/discard the solution and/or invoke at a later time.
     * }
     * A null value will throw an exception.
     * @param solution a solution to process.
     */
    public void process(final TaskAssigningSolution solution) {
        checkNotNull("solution", solution);
        processing.set(true);
        this.solution = solution;
        solutionResource.release();
    }

    @Override
    public void destroy() {
        super.destroy();
        solutionResource.release(); //un-lock in case it was waiting for a solution to process.
    }

    @Override
    public void run() {
        while (isAlive()) {
            try {
                solutionResource.acquire();
                if (isAlive()) {
                    doProcess(solution);
                }
            } catch (InterruptedException e) {
                super.destroy();
                LOGGER.error("Solution Processor was interrupted", e);
            }
        }
    }

    private void doProcess(final TaskAssigningSolution solution) {
        LOGGER.debug("Starting processing of solution: " + solution);
        final List<TaskPlanningInfo> taskPlanningInfos = new ArrayList<>(solution.getTaskList().size());
        final Map<Long, Task> tasksById = new HashMap<>();
        List<TaskPlanningInfo> userTaskPlanningInfos;
        Iterator<TaskPlanningInfo> userTaskPlanningInfosIt;
        TaskPlanningInfo taskPlanningInfo;
        int index;
        int publishedCount;
        for (User user : solution.getUserList()) {
            userTaskPlanningInfos = new ArrayList<>();
            index = 0;
            publishedCount = 0;
            Task nextTask = user.getNextTask();
            while (nextTask != null) {
                if (!DUMMY_TASK.getId().equals(nextTask.getId()) && !DUMMY_TASK_PLANNER_241.getId().equals(nextTask.getId())) {
                    //dummy tasks has nothing to with the jBPM runtime, don't process them
                    tasksById.put(nextTask.getId(), nextTask);
                    taskPlanningInfo = new TaskPlanningInfo(nextTask.getContainerId(),
                                                            nextTask.getId(),
                                                            nextTask.getProcessInstanceId(),
                                                            new PlanningDataImpl(nextTask.getId()));

                    taskPlanningInfo.getPlanningData().setPublished(nextTask.isPinned());
                    taskPlanningInfo.getPlanningData().setAssignedUser(user.getUser().getEntityId());
                    taskPlanningInfo.getPlanningData().setIndex(index++);
                    userTaskPlanningInfos.add(taskPlanningInfo);
                    publishedCount += taskPlanningInfo.getPlanningData().isPublished() ? 1 : 0;
                }
                nextTask = nextTask.getNextTask();
            }
            userTaskPlanningInfosIt = userTaskPlanningInfos.iterator();
            while (userTaskPlanningInfosIt.hasNext() && publishedCount < publishWindowSize) {
                taskPlanningInfo = userTaskPlanningInfosIt.next();
                if (!taskPlanningInfo.getPlanningData().isPublished()) {
                    taskPlanningInfo.getPlanningData().setPublished(true);
                    publishedCount++;
                }
            }
            taskPlanningInfos.addAll(userTaskPlanningInfos);
        }

        //TODO check the error management when this method throws exceptions.
        runtimeClient.applyPlanning(taskPlanningInfos, targetUserId);
        processing.set(false);

        resultConsumer.accept(new Result(null));
        LOGGER.debug("Solution processing finished: " + solution);
    }
}