package org.jbpm.task.assigning.runtime.service;

import java.util.concurrent.atomic.AtomicReference;

import static org.jbpm.task.assigning.runtime.service.RunnableBase.Status.DESTROYED;
import static org.jbpm.task.assigning.runtime.service.RunnableBase.Status.STOPPED;

public abstract class RunnableBase implements Runnable {

    protected enum Status {
        STARTING,
        STARTED,
        STOPPING,
        STOPPED,
        DESTROYED
    }

    protected final AtomicReference<Status> status = new AtomicReference<>(STOPPED);

    public void destroy() {
        status.set(DESTROYED);
    }

    /**
     * @return true if the destroy() method has been called. False in any other case.
     */
    public boolean isDestroyed() {
        return status.get() == DESTROYED;
    }

    /**
     * The semantic of RunnableBase class is it that can't continue "executing" as soon the destroy() method was invoked
     * or the backing thread was interrupted.
     * @return true if current RunnableBase can continue executing, false in any other case.
     */
    protected boolean isAlive() {
        return status.get() != DESTROYED && !Thread.currentThread().isInterrupted();
    }
}
