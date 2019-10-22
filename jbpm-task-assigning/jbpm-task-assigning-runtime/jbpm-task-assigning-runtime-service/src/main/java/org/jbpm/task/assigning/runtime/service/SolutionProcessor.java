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
import java.util.Iterator;
import java.util.List;
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

import static org.jbpm.task.assigning.runtime.service.SolutionBuilder.DUMMY_TASK;
import static org.kie.soup.commons.validation.PortablePreconditions.checkNotNull;

/**
 * This class manges the processing of new a solution produced by the solver. It must typically apply all the required
 * changes in the jBPM runtime.
 */
public class SolutionProcessor implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(SolutionProcessor.class);

    private final ProcessRuntimeIntegrationClient runtimeClient;
    private final Consumer<Result> resultConsumer;

    private final Semaphore solutionResource = new Semaphore(0);
    private final AtomicBoolean processing = new AtomicBoolean(false);
    private final AtomicBoolean destroyed = new AtomicBoolean(false);

    private TaskAssigningSolution solution;
    private PublishedTaskCache publishedTasks;

    public static class Result {

        private Exception error;

        public Result() {
        }

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

    public SolutionProcessor(final ProcessRuntimeIntegrationClient runtimeClient, final Consumer<Result> resultConsumer) {
        checkNotNull("runtimeClient", runtimeClient);
        checkNotNull("resultConsumer", resultConsumer);
        this.runtimeClient = runtimeClient;
        this.resultConsumer = resultConsumer;
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
    public void process(final TaskAssigningSolution solution, final PublishedTaskCache publishedTasks) {
        checkNotNull("solution", solution);
        checkNotNull("publishedTasks", publishedTasks);
        processing.set(true);
        this.solution = solution;
        this.publishedTasks = publishedTasks;
        solutionResource.release();
    }

    public void destroy() {
        destroyed.set(true);
        solutionResource.release(); //in case it was waiting for a solution to process.
    }

    @Override
    public void run() {
        while (!destroyed.get() && !Thread.currentThread().isInterrupted()) {
            try {
                solutionResource.acquire();
                if (!destroyed.get()) {
                    doProcess(solution, publishedTasks);
                }
            } catch (InterruptedException e) {
                LOGGER.error("Solution Processor was interrupted", e);
            }
        }
    }

    private void doProcess(final TaskAssigningSolution solution, final PublishedTaskCache publishedTasks) {
        LOGGER.debug("Starting processing of solution: " + solution);
        final int publishWindowSize = 4;
        final List<TaskPlanningInfo> taskPlanningInfos = new ArrayList<>(solution.getTaskList().size());
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
                if (DUMMY_TASK.getId().equals(nextTask.getId())) {
                    break;
                }
                taskPlanningInfo = new TaskPlanningInfo(nextTask.getContainerId(), nextTask.getId(), nextTask.getProcessInstanceId());
                taskPlanningInfo.getPlanningParameters().setPublished(publishedTasks.isPublished(nextTask.getId()));
                taskPlanningInfo.getPlanningParameters().setPinned(nextTask.isPinned());
                taskPlanningInfo.getPlanningParameters().setAssignedUser(user.getUser().getEntityId());
                taskPlanningInfo.getPlanningParameters().setIndex(index++);
                userTaskPlanningInfos.add(taskPlanningInfo);
                publishedCount += taskPlanningInfo.getPlanningParameters().isPublished() ? 1 : 0;
                nextTask = nextTask.getNextTask();
            }
            userTaskPlanningInfosIt = userTaskPlanningInfos.iterator();
            while (userTaskPlanningInfosIt.hasNext() && publishedCount <= publishWindowSize) {
                taskPlanningInfo = userTaskPlanningInfosIt.next();
                if (!taskPlanningInfo.getPlanningParameters().isPublished()) {
                    taskPlanningInfo.getPlanningParameters().setPublished(true);
                    //TODO, ojo cuando decido publicar una tareas, inmediatamente tengo que ponerla pinned
                    //osea que tengo que programar ese cambio...
                    //guardo en la BD pero ademas tengo q meter un problem fact change para dejar la tarea pinned ?
                    //NO...¿?
                    publishedCount++;
                }
            }
            taskPlanningInfos.addAll(userTaskPlanningInfos);
        }
        //TODO set the proper user insead of "wbadmin"
        runtimeClient.applyPlanning(taskPlanningInfos, "wbadmin");

        //TODO check the error management.
        processing.set(false);
        resultConsumer.accept(new Result());

        LOGGER.debug("Solution processing finished: " + solution);
    }
}
