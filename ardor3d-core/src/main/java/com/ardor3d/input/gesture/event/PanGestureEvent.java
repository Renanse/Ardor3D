/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.input.gesture.event;

import java.text.MessageFormat;

import com.ardor3d.annotation.Immutable;
import com.ardor3d.math.Rectangle2;

@Immutable
public class PanGestureEvent extends AbstractGestureEvent {

  protected final int _touches;
  protected final int _xDirection;
  protected final int _yDirection;

  public PanGestureEvent(final boolean startOfGesture, final Rectangle2 bounds, final int touches, final int xDirection,
    final int yDirection) {
    this(System.nanoTime(), startOfGesture, bounds, touches, xDirection, yDirection);
  }

  public PanGestureEvent(final long nanos, final boolean startOfGesture, final Rectangle2 bounds, final int touches,
    final int xDirection, final int yDirection) {
    super(nanos, startOfGesture, bounds);
    _touches = touches;
    _xDirection = xDirection;
    _yDirection = yDirection;
  }

  public int getTouches() { return _touches; }

  public int getXDirection() { return _xDirection; }

  public int getYDirection() { return _yDirection; }

  @Override
  public String toString() {
    return MessageFormat.format("PanGestureEvent:  touches: {0}  x: {1}  y: {2}", _touches, _xDirection, _yDirection);
  }

}
