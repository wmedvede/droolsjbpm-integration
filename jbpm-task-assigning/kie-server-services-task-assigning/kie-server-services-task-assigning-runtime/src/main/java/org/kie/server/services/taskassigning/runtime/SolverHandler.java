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

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import org.jbpm.task.assigning.model.TaskAssigningSolution;
import org.jbpm.task.assigning.user.system.integration.UserSystemService;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.api.solver.event.BestSolutionChangedEvent;
import org.optaplanner.core.impl.solver.ProblemFactChange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.kie.server.services.taskassigning.runtime.TaskAssigningConstants.JBPM_TASK_ASSIGNING_PROCESS_RUNTIME_TARGET_USER;
import static org.kie.server.services.taskassigning.runtime.TaskAssigningConstants.JBPM_TASK_ASSIGNING_PUBLISH_WINDOW_SIZE;
import static org.kie.server.services.taskassigning.runtime.TaskAssigningConstants.JBPM_TASK_ASSIGNING_SYNC_PERIOD;
import static org.kie.server.services.taskassigning.runtime.util.PropertyUtil.readSystemProperty;
import static org.kie.soup.commons.validation.PortablePreconditions.checkNotNull;

/**
 * This class handles all the work regarding with: creating/starting the solver, the processing of the produced solutions
 * and the synchronization of the working solution with the changes that might be produced in the jBPM runtime. By
 * coordinating the actions produced by the SolverExecutor, the SolutionProcessor and the SolutionSynchronizer.
 */
public class SolverHandler {

    private static Logger LOGGER = LoggerFactory.getLogger(SolverHandler.class);

    private final SolverDef solverDef;
    private final ProcessRuntimeIntegrationDelegate runtimeClientDelegate;
    private final UserSystemService userSystemService;
    private final ExecutorService executorService;

    /**
     * Synchronizes potential concurrent accesses by the SolverWorker, SolutionProcessor and SolutionSynchronizer.
     */
    private final ReentrantLock lock = new ReentrantLock();
    private TaskAssigningSolution currentSolution = null;
    private TaskAssigningSolution nextSolution = null;

    private Solver<TaskAssigningSolution> solver;
    private SolverExecutor solverExecutor;
    private SolutionSynchronizer solutionSynchronizer;
    private SolutionProcessor solutionProcessor;
    private AtomicBoolean enableUpdate = new AtomicBoolean(false);
    private long lastProcessEndTime;
    private final String targetUserId = readSystemProperty(JBPM_TASK_ASSIGNING_PROCESS_RUNTIME_TARGET_USER, null, value -> value);
    private static final int publishWindowSize = readSystemProperty(JBPM_TASK_ASSIGNING_PUBLISH_WINDOW_SIZE, 2, Integer::parseInt);
    private static final long syncPeriod = readSystemProperty(JBPM_TASK_ASSIGNING_SYNC_PERIOD, 5000L, Long::parseLong);

    public SolverHandler(final SolverDef solverDef,
                         final ProcessRuntimeIntegrationDelegate runtimeClientDelegate,
                         final UserSystemService userSystemService,
                         final ExecutorService executorService) {
        checkNotNull("solverDef", solverDef);
        checkNotNull("runtimeClientDelegate", runtimeClientDelegate);
        checkNotNull("userSystemService", userSystemService);
        checkNotNull("executorService", executorService);
        this.solverDef = solverDef;
        this.runtimeClientDelegate = runtimeClientDelegate;
        this.userSystemService = userSystemService;
        this.executorService = executorService;
    }

    public void init() {
        LOGGER.debug("Initializing SolverHandler.");
        solver = createSolver(solverDef);
        LOGGER.debug("Solver was successfully created.");
    }

    public void start() {
        disableUpdates();
        solverExecutor = new SolverExecutor(solver, this::onBestSolutionChange);
        solutionSynchronizer = new SolutionSynchronizer(solverExecutor, runtimeClientDelegate, userSystemService,
                                                        syncPeriod, this::onUpdateSolution);

        solutionProcessor = new SolutionProcessor(runtimeClientDelegate, this::onSolutionProcessed, targetUserId,
                                                  publishWindowSize);
        executorService.execute(solverExecutor); //is started/stopped by the SolutionSynchronizer.
        executorService.execute(solutionSynchronizer);
        executorService.execute(solutionProcessor); //automatically starts and waits for a solution to process.
        solutionSynchronizer.start();
    }

    public void destroy() {
        solverExecutor.destroy();
        solutionSynchronizer.destroy();
        solutionProcessor.destroy();

        executorService.shutdown();
        try {
            executorService.awaitTermination(10, TimeUnit.SECONDS);
            LOGGER.debug("ExecutorService was successfully shutted down.");
        } catch (InterruptedException e) {
            LOGGER.debug("An exception was thrown during executionService graceful termination.", e);
            executorService.shutdownNow();
        }
    }

    private void addProblemFactChanges(List<ProblemFactChange<TaskAssigningSolution>> changes) {
        checkNotNull("changes", changes);
        if (!solverExecutor.isStarted()) {
            LOGGER.info("SolverExecutor has not been started. Changes will be discarded", changes);
            return;
        }
        if (!changes.isEmpty()) {
            solverExecutor.addProblemFactChanges(changes);
        } else {
            LOGGER.info("It looks like an empty change list was provided. Nothing will be done since it has no effect on the solution.");
        }
    }

    /**
     * Invoked when the solver produces a new solution.
     * @param event event produced by the solver.
     */
    private void onBestSolutionChange(BestSolutionChangedEvent<TaskAssigningSolution> event) {
        if (event.isEveryProblemFactChangeProcessed() && event.getNewBestSolution().getScore().isSolutionInitialized()) {
            lock.lock();
            try {
                if (solutionProcessor.isProcessing()) {
                    nextSolution = event.getNewBestSolution();
                } else {
                    currentSolution = event.getNewBestSolution();
                    nextSolution = null;
                    disableUpdates();
                    solutionProcessor.process(currentSolution);
                }
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * Invoked when the last produced solution has been processed by the SolutionProcessor.
     * @param result result produced by the SolutionProcessor.
     */
    private void onSolutionProcessed(SolutionProcessor.Result result) {
        lock.lock();
        try {
            disableUpdates();
            if (result.hasError()) {
                LOGGER.error("An error was produced during the solution processing. The solver will be restarted with"
                                     + " a recovered solution from the jBPM runtime.", result.getError());
                solverExecutor.stop();
                currentSolution = null;
                nextSolution = null;
            } else if (nextSolution != null) {
                currentSolution = nextSolution;
                nextSolution = null;
                solutionProcessor.process(currentSolution);
            } else {
                enableUpdates(System.currentTimeMillis());
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Invoked every time the SolutionSynchronizer gets updated information from the jBPM runtime. This method
     * analyses the updated information and creates and program the necessary changes into the solver.
     * @param result List of tasks information returned from the jBPM runtime.
     */
    private void onUpdateSolution(SolutionSynchronizer.Result result) {
        lock.lock();
        try {
            if (isEnableUpdates() && !isDirty(result)) {
                final List<ProblemFactChange<TaskAssigningSolution>> changes = new SolutionChangesBuilder()
                        .withSolution(currentSolution)
                        .withTasks(result.getTaskInfos())
                        .withUserSystem(userSystemService)
                        .build();
                applyIfNotEmpty(changes);
            }
        } finally {
            lock.unlock();
        }
    }

    private void applyIfNotEmpty(List<ProblemFactChange<TaskAssigningSolution>> changes) {
        if (!changes.isEmpty()) {
            LOGGER.debug("Current solution will be updated with {} changes from last synchronization", changes.size());
            addProblemFactChanges(changes);
        } else {
            LOGGER.debug("There are no changes to apply from last synchronization.");
        }
    }

    private Solver<TaskAssigningSolution> createSolver(SolverDef solverDef) {
        SolverFactory<TaskAssigningSolution> solverFactory = SolverFactory.createFromXmlResource(solverDef.getSolverConfigResource());
        return solverFactory.buildSolver();
    }

    private void disableUpdates() {
        enableUpdate.set(false);
    }

    private void enableUpdates(long lastProcessEndTime) {
        this.lastProcessEndTime = lastProcessEndTime;
        enableUpdate.set(true);
    }

    private boolean isEnableUpdates() {
        return enableUpdate.get();
    }

    private boolean isDirty(SolutionSynchronizer.Result result) {
        return result.getReadStartTime() < lastProcessEndTime;
    }
}
