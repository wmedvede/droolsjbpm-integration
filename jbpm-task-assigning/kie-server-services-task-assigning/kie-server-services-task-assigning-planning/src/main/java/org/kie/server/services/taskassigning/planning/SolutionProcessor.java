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

package org.kie.server.services.taskassigning.planning;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.kie.server.services.taskassigning.core.model.Task;
import org.kie.server.services.taskassigning.core.model.TaskAssigningSolution;
import org.kie.server.services.taskassigning.core.model.User;
import org.kie.server.api.model.taskassigning.ExecutePlanningResult;
import org.kie.server.api.model.taskassigning.PlanningItem;
import org.kie.server.api.model.taskassigning.PlanningTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.kie.server.services.taskassigning.core.model.Task.IS_NOT_DUMMY;
import static org.kie.server.services.taskassigning.core.model.User.IS_PLANNING_USER;
import static org.kie.soup.commons.validation.PortablePreconditions.checkCondition;
import static org.kie.soup.commons.validation.PortablePreconditions.checkNotNull;

/**
 * This class manges the processing of new a solution produced by the solver.
 */
public class SolutionProcessor extends RunnableBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(SolutionProcessor.class);

    private final TaskAssigningRuntimeDelegate delegate;
    private final Consumer<Result> resultConsumer;
    private final String targetUserId;
    private final int publishWindowSize;

    private final Semaphore solutionResource = new Semaphore(0);
    private final AtomicBoolean processing = new AtomicBoolean(false);

    private TaskAssigningSolution solution;

    public static class Result {

        private Exception exception;

        private ExecutePlanningResult executeResult;

        private Result() {

        }

        private Result(Exception exception) {
            this.exception = exception;
        }

        public Result(ExecutePlanningResult executeResult) {
            this.executeResult = executeResult;
        }

        public boolean hasException() {
            return exception != null;
        }

        public Exception getException() {
            return exception;
        }

        public ExecutePlanningResult getExecuteResult() {
            return executeResult;
        }
    }

    /**
     * @param delegate a TaskAssigningRuntimeDelegate instance for executing methods into the jBPM runtime.
     * @param resultConsumer a consumer for processing the results.
     * @param targetUserId a user identifier for using as the "on behalf of" user when interacting with the jBPM runtime.
     * @param publishWindowSize Integer value > 0 that indicates the number of tasks to be published.
     */
    public SolutionProcessor(final TaskAssigningRuntimeDelegate delegate,
                             final Consumer<Result> resultConsumer,
                             final String targetUserId,
                             final int publishWindowSize) {
        checkNotNull("delegate", delegate);
        checkNotNull("resultConsumer", resultConsumer);
        checkNotNull("targetUserId", targetUserId);
        checkCondition("publishWindowSize", publishWindowSize > 0);
        this.delegate = delegate;
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
     * //invoke at a later time.
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
        LOGGER.debug("Solution Processor finished");
    }

    private void doProcess(final TaskAssigningSolution solution) {
        LOGGER.debug("Starting processing of solution: " + solution);
        final List<PlanningItem> planningItems = new ArrayList<>(solution.getTaskList().size());
        List<PlanningItem> userPlanningItems;
        Iterator<PlanningItem> userPlanningItemsIt;
        PlanningItem planningItem;
        int index;
        int publishedCount;
        for (User user : solution.getUserList()) {
            userPlanningItems = new ArrayList<>();
            index = 0;
            publishedCount = 0;
            Task nextTask = user.getNextTask();

            while (nextTask != null) {
                if (IS_NOT_DUMMY.test(nextTask)) {
                    //dummy tasks has nothing to with the jBPM runtime, don't process them
                    planningItem = PlanningItem.builder()
                            .containerId(nextTask.getContainerId())
                            .taskId(nextTask.getId())
                            .processInstanceId(nextTask.getProcessInstanceId())
                            .planningTask(PlanningTask.builder()
                                                  .taskId(nextTask.getId())
                                                  .published(nextTask.isPinned())
                                                  .assignedUser(user.getUser().getEntityId())
                                                  .index(index++)
                                                  .build())
                            .build();

                    userPlanningItems.add(planningItem);
                    publishedCount += planningItem.getPlanningTask().isPublished() ? 1 : 0;
                }
                nextTask = nextTask.getNextTask();
            }
            if (!IS_PLANNING_USER.test(user.getEntityId())) {
                userPlanningItemsIt = userPlanningItems.iterator();
                while (userPlanningItemsIt.hasNext() && publishedCount < publishWindowSize) {
                    planningItem = userPlanningItemsIt.next();
                    if (!planningItem.getPlanningTask().isPublished()) {
                        planningItem.getPlanningTask().setPublished(true);
                        publishedCount++;
                    }
                }
            }
            planningItems.addAll(userPlanningItems);
        }

        final List<PlanningItem> publishedTasks = planningItems.stream().filter(item -> item.getPlanningTask().isPublished()).collect(Collectors.toList());

        if (LOGGER.isTraceEnabled()) {
            traceSolution(solution);
            tracePublishedTasks(publishedTasks);
        }

        Result result;
        try {
            ExecutePlanningResult executeResult = delegate.executePlanning(publishedTasks, targetUserId);
            result = new Result(executeResult);
        } catch (Exception e) {
            LOGGER.error("An error was produced during solution processing, planning execution failed.", e);
            result = new Result(e);
        }

        LOGGER.debug("Solution processing finished: " + solution);
        processing.set(false);
        resultConsumer.accept(result);
    }

    private void traceSolution(TaskAssigningSolution solution) {
        LOGGER.trace("\n");
        LOGGER.trace("*** Start of solution trace, with users = {} and tasks = {} ***", solution.getUserList().size(), solution.getTaskList().size());
        for (User user : solution.getUserList()) {
            Task nextTask = user.getNextTask();
            while (nextTask != null) {
                LOGGER.trace(user.getEntityId() + " -> " + nextTask.getId() + ", pinned: " + nextTask.isPinned() + " priority: " + nextTask.getPriority() + ", status: " + nextTask.getStatus());
                nextTask = nextTask.getNextTask();
            }
        }
        LOGGER.trace("*** End of solution trace ***");
        LOGGER.trace("\n");
    }

    private void tracePublishedTasks(List<PlanningItem> publishedTasks) {
        LOGGER.trace("\n");
        LOGGER.trace("*** Start of published tasks trace with {} published tasks ***", publishedTasks.size());
        publishedTasks.forEach(item -> {
            LOGGER.trace(item.getPlanningTask().getAssignedUser() + " -> " + item.getTaskId() + ", index: " + item.getPlanningTask().getIndex() + ", published: " + item.getPlanningTask().isPublished());
        });
        LOGGER.trace("*** End of published trace ***");
        LOGGER.trace("\n");
    }
}