/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.input.dummy;

import com.ardor3d.input.controller.ControllerEvent;
import com.ardor3d.input.controller.ControllerInfo;
import com.ardor3d.input.controller.ControllerWrapper;
import com.ardor3d.util.PeekingIterator;

public class DummyControllerWrapper implements ControllerWrapper {
  public static final DummyControllerWrapper INSTANCE = new DummyControllerWrapper();

  PeekingIterator<ControllerEvent> empty = new PeekingIterator<>() {
    @Override
    public boolean hasNext() {
      return false;
    }

    @Override
    public void remove() {}

    @Override
    public ControllerEvent peek() {
      return null;
    }

    @Override
    public ControllerEvent next() {
      return null;
    }
  };

  @Override
  public PeekingIterator<ControllerEvent> getControllerEvents() { return empty; }

  @Override
  public void init() {
    // ignore, does nothing
  }

  @Override
  public int getControllerCount() { return 0; }

  @Override
  public ControllerInfo getControllerInfo(final int controllerIndex) {
    return null;
  }
}
