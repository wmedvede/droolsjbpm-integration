package org.jbpm.task.assigning.runtime.service;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class RunnableBase implements Runnable {

    protected final AtomicBoolean destroyed = new AtomicBoolean(false);

    public void destroy() {
        destroyed.set(true);
    }

    /**
     * @return true if the destroy() method has been called. False in any other case.
     */
    public boolean isDestroyed() {
        return destroyed.get();
    }

    /**
     * The semantic of RunnableBase class is it that can't continue "executing" as soon the destroy() method was invoked
     * or the backing thread was interrupted.
     * @return true if current RunnableBase can continue executing, false in any other case.
     */
    protected boolean isAlive() {
        return !destroyed.get() && !Thread.currentThread().isInterrupted();
    }
}
