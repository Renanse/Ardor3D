/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.framework.lwjgl3;

import java.util.function.LongSupplier;

import org.lwjgl.glfw.GLFW;

import com.ardor3d.annotation.MainThread;
import com.ardor3d.renderer.lwjgl3.Lwjgl3CanvasCallback;
import com.ardor3d.util.Constants;
import com.ardor3d.util.stat.StatCollector;
import com.ardor3d.util.stat.StatType;

public class GLFWCanvasCallback implements Lwjgl3CanvasCallback {
  private final LongSupplier _windowId;
  private final boolean _doEventPolling;

  /**
   * Construct a canvas callback for the supplied window. Event polling will be set to true.
   *
   * @param windowId
   *          supplies the window id of an underlying glfw based canvas.
   */
  GLFWCanvasCallback(final LongSupplier windowId) {
    this(windowId, true);
  }

  /**
   * Construct a canvas callback for the supplied window.
   *
   * @param windowId
   *          supplies the window id of an underlying glfw based canvas.
   * @param doEventPolling
   *          if true, poll glfw for events after swapping buffers.
   */
  GLFWCanvasCallback(final LongSupplier windowId, final boolean doEventPolling) {
    _windowId = windowId;
    _doEventPolling = doEventPolling;
  }

  @MainThread
  @Override
  public void makeCurrent(final boolean force) {
    if (force || Constants.useMultipleContexts) {
      final long id = _windowId.getAsLong();
      if (id != 0) {
        GLFW.glfwMakeContextCurrent(_windowId.getAsLong());
      }
    }
  }

  @MainThread
  @Override
  public void releaseContext(final boolean force) {
    if (force || Constants.useMultipleContexts) {
      GLFW.glfwMakeContextCurrent(0);
    }
  }

  @MainThread
  @Override
  public void doSwap() {
    final long id = _windowId.getAsLong();
    if (id == 0) {
      return;
    }

    if (Constants.stats) {
      StatCollector.startStat(StatType.STAT_DISPLAYSWAP_TIMER);
    }
    GLFW.glfwSwapBuffers(_windowId.getAsLong());
    if (_doEventPolling) {
      GLFW.glfwPollEvents();
    }
    if (Constants.stats) {
      StatCollector.endStat(StatType.STAT_DISPLAYSWAP_TIMER);
    }
  }
}
