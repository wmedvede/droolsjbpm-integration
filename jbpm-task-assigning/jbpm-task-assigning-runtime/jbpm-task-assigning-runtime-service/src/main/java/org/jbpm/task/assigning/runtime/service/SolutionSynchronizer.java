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

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;

import org.jbpm.task.assigning.model.TaskAssigningSolution;
import org.jbpm.task.assigning.process.runtime.integration.client.TaskInfo;
import org.jbpm.task.assigning.user.system.integration.UserSystemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jbpm.task.assigning.process.runtime.integration.client.TaskStatus.InProgress;
import static org.jbpm.task.assigning.process.runtime.integration.client.TaskStatus.Ready;
import static org.jbpm.task.assigning.process.runtime.integration.client.TaskStatus.Reserved;
import static org.jbpm.task.assigning.process.runtime.integration.client.TaskStatus.Suspended;
import static org.kie.soup.commons.validation.PortablePreconditions.checkCondition;
import static org.kie.soup.commons.validation.PortablePreconditions.checkNotNull;

/**
 * This class manages the periodical reading (polling strategy) of current tasks from the jBPM runtime and the supply of
 * the results to the "resultConsumer" for updating the current solution with the potential changes. It also determines
 * if the SolverExecutor needs to be started depending on his status, whenever the SolverExecutor is stopped it'll be
 * started by reading the recovered solution from the jBPM runtime.
 */
public class SolutionSynchronizer extends RunnableBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(SolutionSynchronizer.class);

    private final SolverExecutor solverExecutor;
    private final ProcessRuntimeIntegrationDelegate runtimeClientDelegate;
    private final UserSystemService userSystemService;
    private final long period;
    private final Consumer<Result> resultConsumer;
    private int solverExecutorStarts = 0;

    private final Semaphore startPermit = new Semaphore(0);

    public static class Result {

        long readStartTime;
        private List<TaskInfo> taskInfos;

        public Result(long readStartTime, List<TaskInfo> taskInfos) {
            this.readStartTime = readStartTime;
            this.taskInfos = taskInfos;
        }

        public long getReadStartTime() {
            return readStartTime;
        }

        public List<TaskInfo> getTaskInfos() {
            return taskInfos;
        }
    }

    public SolutionSynchronizer(final SolverExecutor solverExecutor,
                                final ProcessRuntimeIntegrationDelegate runtimeClientDelegate,
                                final UserSystemService userSystem,
                                final long period,
                                final Consumer<Result> resultConsumer) {
        checkNotNull("solverExecutor", solverExecutor);
        checkNotNull("runtimeClientDelegate", runtimeClientDelegate);
        checkNotNull("userSystem", userSystem);
        checkCondition("period", period > 0);
        checkNotNull("resultConsumer", resultConsumer);

        this.solverExecutor = solverExecutor;
        this.runtimeClientDelegate = runtimeClientDelegate;
        this.userSystemService = userSystem;
        this.period = period;
        this.resultConsumer = resultConsumer;
    }

    /**
     * Starts the SolutionSynchronizer. Thread-safe method, only the first invocation has effect.
     */
    public void start() {
        startPermit.release();
    }

    /**
     * Starts the synchronizing finalization, that will be produced as soon as possible.
     * It's a non thread-safe method, but only first invocation has effect.
     */
    @Override
    public void destroy() {
        super.destroy();
        startPermit.release(); //in case it's still waiting for start.
    }

    @Override
    public void run() {
        LOGGER.debug("Solution Synchronizer Started");
        try {
            startPermit.acquire();
        } catch (InterruptedException e) {
            super.destroy();
            LOGGER.error("Solution Synchronizer was interrupted while waiting for start.", e);
        }
        while (isAlive()) {
            try {
                Thread.sleep(period);
                if (isAlive()) {
                    if (solverExecutor.isStopped()) {
                        try {
                            LOGGER.debug("Solution Synchronizer will recover the solution from the jBPM runtime for starting the solver.");
                            final TaskAssigningSolution recoveredSolution = recoverSolution();
                            if (isAlive() && !solverExecutor.isDestroyed()) {
                                if (!recoveredSolution.getTaskList().isEmpty()) {
                                    solverExecutor.start(recoveredSolution);
                                    LOGGER.debug("Solution was successfully recovered. Solver was started for #{} time.", ++solverExecutorStarts);
                                    if (solverExecutorStarts > 1) {
                                        LOGGER.debug("It looks like it was necessary to restart the solver. It might" +
                                                             " have been caused due to errors during the solution applying in the jBPM runtime");
                                    }
                                } else {
                                    LOGGER.debug("It looks like there are no tasks for recovering the solution at this moment." +
                                                         " Next attempt will be in " + period + " milliseconds");
                                }
                            }
                        } catch (Exception e) {
                            LOGGER.error("An error was produced during solution recovering." +
                                                 " Next attempt will be in " + period + " milliseconds", e);
                        }
                    } else if (solverExecutor.isStarted()) {
                        try {
                            LOGGER.debug("Refreshing solution status from external repository.");
                            final long readStartTime = System.currentTimeMillis();
                            final List<TaskInfo> updatedTaskInfos = loadTaskInfos();
                            LOGGER.debug("Status was read successful.");
                            if (isAlive()) {
                                resultConsumer.accept(new Result(readStartTime, updatedTaskInfos));
                            }
                        } catch (Exception e) {
                            LOGGER.error("An error was produced during solution status refresh from external repository." +
                                                 " Next attempt will be in " + period + " milliseconds", e);
                        }
                    }
                }
            } catch (InterruptedException e) {
                super.destroy();
                LOGGER.error("Solution Synchronizer was interrupted.", e);
            }
        }
        LOGGER.debug("Solution Synchronizer finished");
    }

    private TaskAssigningSolution recoverSolution() {
        final List<TaskInfo> taskInfos = loadTaskInfos();
        final List<org.jbpm.task.assigning.user.system.integration.User> externalUsers = userSystemService.findAllUsers();
        return new SolutionBuilder()
                .withTasks(taskInfos)
                .withUsers(externalUsers)
                .build();
    }

    private List<TaskInfo> loadTaskInfos() {
        return runtimeClientDelegate.findTasks(Arrays.asList(Ready, Reserved, InProgress, Suspended));
    }
}
