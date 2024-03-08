/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.util.concurrent;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A simple class to provide a set of locks for use in a striped locking scheme. This can be useful for cases where
 * multiple threads are likely to be accessing different keys in a map, for example.
 */
public class StripedLocks {
  private final Lock[] lockStripes;

  /**
   * Construct a new StripedLocks object with the given number of stripes.
   *
   * @param stripes
   *          the number of stripes to create.
   */
  public StripedLocks(int stripes) {
    lockStripes = new Lock[stripes];
    for (var i = 0; i < stripes; i++) {
      lockStripes[i] = new ReentrantLock();
    }
  }

  /**
   * Retrieve the lock for the given key.
   *
   * @param key
   *          the key to retrieve a lock for.
   * @return the lock for the given key.
   */
  public Lock getLock(Object key) {
    return lockStripes[Math.abs(key.hashCode()) % lockStripes.length];
  }
}
