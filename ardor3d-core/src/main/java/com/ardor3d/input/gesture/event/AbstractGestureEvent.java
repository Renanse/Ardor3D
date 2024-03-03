/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.input.gesture.event;

import com.ardor3d.annotation.Immutable;
import com.ardor3d.math.Rectangle2;
import com.ardor3d.math.type.ReadOnlyRectangle2;

@Immutable
public abstract class AbstractGestureEvent {

  private final long _nanos;
  private final boolean _startOfGesture;
  private final Rectangle2 _bounds;

  /**
   * Constructor
   *
   * @param nanos
   *          a timestamp, used to sort events
   * @param startOfGesture
   *          if true, this is the beginning of the associated event. Not all events will have a
   *          start.
   * @param bounds
   *          the bounds of the touches involved in this event - the x and y will be the center of the
   *          interaction and the width and height are the dimensions of the rectangle centered at x,y
   *          defined by the touches involved in this event.
   */
  public AbstractGestureEvent(final long nanos, final boolean startOfGesture, final Rectangle2 bounds) {
    _nanos = nanos;
    _startOfGesture = startOfGesture;
    _bounds = bounds;
  }

  public long getNanos() { return _nanos; }

  public boolean isStartOfGesture() { return _startOfGesture; }

  public int getX() { return _bounds.getX(); }

  public int getY() { return _bounds.getY(); }

  public int getMinX() { return _bounds.getX() - _bounds.getWidth() / 2; }

  public int getMaxX() { return _bounds.getX() + _bounds.getWidth() / 2; }

  public int getMinY() { return _bounds.getY() - _bounds.getHeight() / 2; }

  public int getMaxY() { return _bounds.getY() + _bounds.getHeight() / 2; }

  public ReadOnlyRectangle2 getBounds() { return _bounds; }
}
