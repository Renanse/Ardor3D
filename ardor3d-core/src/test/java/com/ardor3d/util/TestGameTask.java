/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.util;

import static org.junit.Assert.assertNull;

import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

public class TestGameTask {

  /**
   * A task that completes with a null result (e.g. a {@code Callable<Void>}) must return null, not
   * report a spurious timeout. The buggy implementation treated {@code _result == null} as a
   * timeout even when the task had finished.
   */
  @Test
  public void testGetReturnsNullResultWithoutTimingOut() throws Exception {
    final GameTask<Void> task = new GameTask<>(() -> null);
    task.invoke();

    assertNull(task.get(2, TimeUnit.SECONDS));
  }

  /** A task that is never invoked must still genuinely time out. */
  @Test(expected = TimeoutException.class)
  public void testGetTimesOutWhenNeverInvoked() throws Exception {
    final GameTask<String> task = new GameTask<>(() -> "done");
    task.get(50, TimeUnit.MILLISECONDS);
  }

  /** A cancelled task must report cancellation, not a timeout. */
  @Test(expected = CancellationException.class)
  public void testGetReportsCancellation() throws Exception {
    final GameTask<String> task = new GameTask<>(() -> "done");
    task.cancel(false);
    task.get(2, TimeUnit.SECONDS);
  }
}
