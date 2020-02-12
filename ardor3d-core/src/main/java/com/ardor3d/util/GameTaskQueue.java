/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.util;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.RendererCallable;

/**
 * <code>GameTaskQueue</code> is a simple queuing system to enqueue tasks that need to be accomplished in a specific
 * thread or phase of the application execution (for example, the OpenGL rendering thread.) Upon sending in a task, the
 * caller gets back a Future object useful for retrieving a return from the Callable that was passed in.
 * 
 * @see Future
 * @see Callable
 */
public class GameTaskQueue {

    public interface ExecutionExceptionListener {
        void executionException(ExecutionException e);
    }

    public static final String RENDER = "render";
    public static final String UPDATE = "update";

    private final ConcurrentLinkedQueue<GameTask<?>> _queue = new ConcurrentLinkedQueue<GameTask<?>>();
    private final AtomicBoolean _executeMultiple = new AtomicBoolean();

    // Default execution time is 0, which means only 1 task will be executed at a time.
    private long _executionTime = 0;

    private final List<ExecutionExceptionListener> _executionExceptionListeners = new LinkedList<ExecutionExceptionListener>();

    public void addExecutionExceptionListener(final ExecutionExceptionListener l) {
        _executionExceptionListeners.add(l);
    }

    public boolean removeExecutionExceptionListener(final ExecutionExceptionListener l) {
        return _executionExceptionListeners.remove(l);
    }

    /**
     * The state of this <code>GameTaskQueue</code> if it will execute all enqueued Callables on an execute invokation.
     * 
     * @return boolean
     */
    public boolean isExecuteAll() {
        return _executeMultiple.get();
    }

    /**
     * @param executeMultiple
     *            if false, only one task at most is executed per call to execute. If true, we will execute as many
     *            tasks as are available, bounded by the max execution time.
     * @see #setExecutionTime(int)
     */
    public void setExecuteMultiple(final boolean executeMultiple) {
        _executeMultiple.set(executeMultiple);
        if (executeMultiple) {
            _executionTime = Integer.MAX_VALUE;
        }
    }

    /**
     * Sets the minimum amount of time the queue will execute tasks per frame. If this is set, executeMultiple is
     * automatically set to true and the execute() loop will execute as many tasks as it can before the execution window
     * threshold is passed. Any remaining tasks will be executed in the following frame.
     * 
     * @param msecs
     *            the maximum number of milliseconds to start tasks. Note that this does not guarantee the tasks will
     *            finish under this time, only start.
     */
    public void setExecutionTime(final int msecs) {
        _executionTime = msecs;
        _executeMultiple.set(true);
    }

    /**
     * min time queue is permitted to execute tasks per frame
     * 
     * @return -1 if executeAll is false, else min time allocated for task execution per frame
     */
    public long getExecutionTime() {
        if (!_executeMultiple.get()) {
            return -1;
        }
        return _executionTime;
    }

    /**
     * Adds the Callable to the internal queue to invoked and returns a Future that wraps the return. This is useful for
     * checking the status of the task as well as being able to retrieve the return object from Callable asynchronously.
     * 
     * @param <V>
     * @param callable
     * @return
     */
    public <V> Future<V> enqueue(final Callable<V> callable) {
        final GameTask<V> task = new GameTask<V>(callable);
        _queue.add(task);
        return task;
    }

    /**
     * Adds the given task to the internal queue to be invoked later.
     * 
     * @param <V>
     * @param task
     */
    public <V> void enqueue(final GameTask<V> task) {
        _queue.add(task);
    }

    /**
     * Execute the tasks from this queue. Note that depending on the queue type, tasks may expect to be run in a certain
     * context (for example, the Render queue expects to be run from the Thread owning a GL context.)
     */
    public void execute() {
        execute(null);
    }

    /**
     * Execute the tasks from this queue. Note that depending on the queue type, tasks may expect to be run in a certain
     * context (for example, the Render queue expects to be run from the Thread owning a GL context.)
     */
    public void execute(final Renderer renderer) {
        final long beginTime = System.currentTimeMillis();
        long elapsedTime;
        GameTask<?> task = _queue.poll();
        do {
            if (task == null) {
                return;
            }

            // Inject the Renderer if correct type of Callable.
            if (renderer != null && task.getCallable() instanceof RendererCallable<?>) {
                ((RendererCallable<?>) task.getCallable()).setRenderer(renderer);
            }

            while (task.isCancelled()) {
                task = _queue.poll();
                if (task == null) {
                    return;
                }
            }
            task.invoke();

            final ExecutionException e = task.getExecutionException();
            if (e != null) {
                for (final ExecutionExceptionListener l : _executionExceptionListeners) {
                    l.executionException(e);
                }
            }

            elapsedTime = System.currentTimeMillis() - beginTime;
        } while ((_executeMultiple.get()) && (elapsedTime < _executionTime) && ((task = _queue.poll()) != null));
    }

    /**
     * Remove all tasks from this queue without executing them.
     */
    public void clear() {
        _queue.clear();
    }

    /**
     * Move the tasks from the given queue to this one.
     * 
     * @param gameTaskQueue
     */
    public void enqueueAll(final GameTaskQueue queue) {
        _queue.addAll(queue._queue);
        queue._queue.clear();
    }

    /**
     * @return count of tasks in queue.
     */
    public int size() {
        return _queue.size();
    }
}
