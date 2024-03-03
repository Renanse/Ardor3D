/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.util;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;

import com.google.common.collect.MapMaker;

/**
 * <code>GameTaskQueueManager</code> is just a simple Singleton class allowing easy access to task
 * queues.
 */
public final class GameTaskQueueManager {

  private static final Object MAP_LOCK = new Object();
  private static final ConcurrentMap<Object, GameTaskQueueManager> _managers = new MapMaker().weakKeys().makeMap();

  private final ConcurrentMap<String, GameTaskQueue> _managedQueues = new ConcurrentHashMap<>(2);

  public static GameTaskQueueManager getManager(final Object key) {
    synchronized (MAP_LOCK) {
      GameTaskQueueManager manager = _managers.get(key);
      if (manager == null) {
        manager = new GameTaskQueueManager();
        _managers.put(key, manager);
      }
      return manager;
    }
  }

  public static GameTaskQueueManager clearManager(final Object key) {
    return _managers.remove(key);
  }

  private GameTaskQueueManager() {
    addQueue(GameTaskQueue.RENDER, new GameTaskQueue());
    addQueue(GameTaskQueue.UPDATE, new GameTaskQueue());
  }

  public void addQueue(final String name, final GameTaskQueue queue) {
    _managedQueues.put(name, queue);
  }

  public GameTaskQueue getQueue(final String name) {
    return _managedQueues.get(name);
  }

  public void moveTasksTo(final GameTaskQueueManager manager) {
    for (final String key : _managedQueues.keySet()) {
      final GameTaskQueue q = manager.getQueue(key);
      final GameTaskQueue mq = _managedQueues.get(key);
      if (q != null && mq.size() > 0) {
        q.enqueueAll(mq);
      }
    }
  }

  /**
   * Clears all tasks from the queues managed by this manager.
   */
  public void clearTasks() {
    for (final GameTaskQueue q : _managedQueues.values()) {
      q.clear();
    }
  }

  /**
   * This method adds <code>callable</code> to the queue to be invoked in the update() method in the
   * OpenGL thread. The Future returned may be utilized to cancel the task or wait for the return
   * object.
   * 
   * @param callable
   * @return Future<V>
   */

  public <V> Future<V> update(final Callable<V> callable) {
    return getQueue(GameTaskQueue.UPDATE).enqueue(callable);
  }

  /**
   * This method adds <code>callable</code> to the queue to be invoked in the render() method in the
   * OpenGL thread. The Future returned may be utilized to cancel the task or wait for the return
   * object.
   * 
   * @param callable
   * @return Future<V>
   */

  public <V> Future<V> render(final Callable<V> callable) {
    return getQueue(GameTaskQueue.RENDER).enqueue(callable);
  }
}
