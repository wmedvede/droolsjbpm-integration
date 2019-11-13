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

import java.util.List;
import java.util.concurrent.Semaphore;

import org.jbpm.task.assigning.model.TaskAssigningSolution;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.event.SolverEventListener;
import org.optaplanner.core.impl.solver.ProblemFactChange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jbpm.task.assigning.runtime.service.RunnableBase.Status.STARTED;
import static org.jbpm.task.assigning.runtime.service.RunnableBase.Status.STARTING;
import static org.jbpm.task.assigning.runtime.service.RunnableBase.Status.STOPPED;
import static org.jbpm.task.assigning.runtime.service.RunnableBase.Status.STOPPING;
import static org.kie.soup.commons.validation.PortablePreconditions.checkNotNull;

/**
 * This class is intended to manage the solver life-cycle in a multi-threaded environment.
 */
public class SolverExecutor extends RunnableBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(SolverExecutor.class);

    private final Solver<TaskAssigningSolution> solver;
    private TaskAssigningSolution solution;

    private final Semaphore startPermit = new Semaphore(0);

    public SolverExecutor(final Solver<TaskAssigningSolution> solver,
                          final SolverEventListener<TaskAssigningSolution> eventListener) {
        checkNotNull("solver", solver);
        checkNotNull("eventListener", eventListener);
        this.solver = solver;
        this.solver.addEventListener((event) -> {
            if (isAlive() && isStarted()) {
                eventListener.bestSolutionChanged(event);
            }
        });
    }

    /**
     * This method is invoked from a different thread for starting the solver and must be invoked only one time.
     * If the solver was already started an exception is thrown so callers of this method should call the isStarted()
     * method to verify. This method is not thread-safe so it's expected that any synchronization required between the
     * isStarted() and start() methods is performed by the callers. However it's normally not expected that multiple
     * threads might try to start the same solver runner instance.
     * @param solution a valid solution for starting the solver with.
     */
    public void start(final TaskAssigningSolution solution) {
        if (!status.compareAndSet(STOPPED, STARTING)) {
            throw new RuntimeException("SolverExecutor start method can only be invoked when the status is STOPPED");
        }
        this.solution = solution;
        startPermit.release();
    }

    /**
     * @return true if the solver has been started, false in any other case.
     */
    public boolean isStarted() {
        return status.get() == STARTED;
    }

    public boolean isStopped() {
        return status.get() == STOPPED;
    }

    /**
     * Stops the solver.
     * @return
     */
    public void stop() {
        if (!isDestroyed()) {
            Status previousStatus = status.getAndSet(STOPPING);
            if (previousStatus == STARTED) {
                solver.terminateEarly();
            } else {
                status.set(STOPPED);
            }
        }
    }

    private boolean isStopping() {
        return status.get() == STOPPING;
    }

    /**
     * This method programmes the subsequent finalization of the solver (if it was started) that will be produced as
     * soon as possible by invoking the solver.terminateEarly() method. If the solver wasn't started it just finalizes
     * current thread.
     */
    @Override
    public void destroy() {
        super.destroy();
        if (isStarted()) {
            solver.terminateEarly();
        } else {
            startPermit.release();
        }
    }

    /**
     * Thread-safe method that adds a list of ProblemFactChanges to the current solver.
     * If the solver has not been started an exception is thrown, so it's expected that callers of this method should
     * call isStarted() method to verify.
     * @param changes a list of problem fact changes to program in current solver.
     */
    public void addProblemFactChanges(final List<ProblemFactChange<TaskAssigningSolution>> changes) {
        if (isStarted()) {
            solver.addProblemFactChanges(changes);
        } else {
            throw new RuntimeException("SolverExecutor has not been started. Be sure it's started and not stopped or destroyed prior to executing this method");
        }
    }

    @Override
    public void run() {
        while (isAlive()) {
            try {
                LOGGER.debug("SolverExecutor is waiting for a start(solution) method invocation for starting the Solver.");
                startPermit.acquire();
                LOGGER.debug("SolverExecutor, the Solver will be started.");
                if (isAlive() && status.compareAndSet(STARTING, STARTED)) {
                    solver.solve(solution);
                    if (isAlive()) {
                        status.set(STOPPED);
                        LOGGER.debug("Solver has been stopped. It can be restarted with the start(solution) method.");
                    } else {
                        LOGGER.error("SolverExecutor has been destroyed. No more invocations can be done on this instance.");
                    }
                }
            } catch (InterruptedException e) {
                super.destroy();
                LOGGER.error("SolverExecutor was interrupted.", e);
            }
        }
        LOGGER.debug("SolverExecutor finished.");
    }
}