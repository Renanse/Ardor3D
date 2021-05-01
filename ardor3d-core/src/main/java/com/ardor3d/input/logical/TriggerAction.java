/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.input.logical;

import com.ardor3d.annotation.MainThread;
import com.ardor3d.framework.Canvas;

/**
 * Defines an action to be performed when a given input condition is true.
 */
public interface TriggerAction {
  /**
   * Implementing classes should implementing this method to take whatever action is desired. This
   * method will always be called on the main GL thread.
   * 
   * @param source
   *          the Canvas that was the source of the current input
   * @param inputState
   *          the current and previous states of the input system when the action was triggered
   * @param tpf
   *          the time per frame in seconds
   */
  @MainThread
  void perform(Canvas source, TwoInputStates inputStates, double tpf);
}
