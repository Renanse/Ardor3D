/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <code>GameTask</code> is used in <code>GameTaskQueue</code> to manage tasks that have yet to be accomplished.
 */
public class GameTask<V> implements Future<V> {
    protected static final Logger logger = Logger.getLogger(GameTask.class.getName());

    protected final Callable<V> callable;

    protected V _result;
    protected ExecutionException _exception;
    protected boolean _cancelled, _finished;
    protected final ReentrantLock _stateLock = new ReentrantLock();
    protected final Condition _finishedCondition = _stateLock.newCondition();

    public GameTask(final Callable<V> callable) {
        this.callable = callable;
    }

    /**
     * @param mayInterruptIfRunning
     *            ignored by this implementation.
     */
    public boolean cancel(final boolean mayInterruptIfRunning) {
        _stateLock.lock();
        try {
            if (isDone()) {
                return false;
            }
            _cancelled = true;

            _finishedCondition.signalAll();

            return true;
        } finally {
            _stateLock.unlock();
        }
    }

    public V get() throws InterruptedException, ExecutionException {
        _stateLock.lock();
        try {
            while (!isDone()) {
                _finishedCondition.await();
            }
            if (_exception != null) {
                throw _exception;
            }
            return _result;
        } finally {
            _stateLock.unlock();
        }
    }

    public V get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException,
            TimeoutException {
        _stateLock.lock();
        try {
            if (!isDone()) {
                _finishedCondition.await(timeout, unit);
            }
            if (_exception != null) {
                throw _exception;
            }
            if (_result == null) {
                throw new TimeoutException("Object not returned in time allocated.");
            }
            return _result;
        } finally {
            _stateLock.unlock();
        }
    }

    public boolean isCancelled() {
        _stateLock.lock();
        try {
            return _cancelled;
        } finally {
            _stateLock.unlock();
        }
    }

    public boolean isDone() {
        _stateLock.lock();
        try {
            return _finished || _cancelled || (_exception != null);
        } finally {
            _stateLock.unlock();
        }
    }

    public Callable<V> getCallable() {
        return callable;
    }

    public void invoke() {
        try {
            final V tmpResult = callable.call();

            _stateLock.lock();
            try {
                _result = tmpResult;
                _finished = true;

                _finishedCondition.signalAll();
            } finally {
                _stateLock.unlock();
            }
        } catch (final Exception e) {
            logger.logp(Level.SEVERE, this.getClass().toString(), "invoke()", "Exception", e);

            _stateLock.lock();
            try {
                _exception = new ExecutionException(e);

                _finishedCondition.signalAll();
            } finally {
                _stateLock.unlock();
            }
        }
    }

}
