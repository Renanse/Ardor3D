/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.input.controller;

import com.google.common.collect.PeekingIterator;

public interface ControllerWrapper {
  /**
   * Allows the controller wrapper implementation to initialize itself.
   */
  void init();

  /**
   * Returns a peeking iterator that allows the client to loop through all controller events that have
   * not yet been handled.
   * 
   * @return an iterator that allows the client to check which events have still not been handled
   */
  PeekingIterator<ControllerEvent> getControllerEvents();

  int getControllerCount();

  ControllerInfo getControllerInfo(int controllerIndex);
}
