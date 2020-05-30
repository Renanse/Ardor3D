/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.input.gesture.touch;

import java.util.List;

import com.ardor3d.input.gesture.event.AbstractGestureEvent;
import com.ardor3d.input.gesture.event.PanGestureEvent;

public class PanInterpreter extends AbstractTouchInterpreter {

  public PanInterpreter(final int touches) {
    super(touches);
  }

  @Override
  public AbstractGestureEvent examine(final List<TouchHistory> touchInfo, final int up, final int valid) {
    if (valid == _touches && touchInfo.size() == valid && _state == ArmState.Ready) {
      boolean down = false;
      int currX = 0, currY = 0, prevX = 0, prevY = 0;
      for (int i = 0; i < valid; i++) {
        final TouchHistory t = touchInfo.get(i);
        down |= (t.currState == TouchStatus.Down);
        currX += t.currX;
        currY += t.currY;
        prevX += t.prevX;
        prevY += t.prevY;
      }
      currX /= valid;
      currY /= valid;
      prevX /= valid;
      prevY /= valid;

      if (down || prevX != currX || prevY != currY) {
        InterpreterUtils.determineBounds(touchInfo, _lastBounds);
        return new PanGestureEvent(down, _lastBounds, _touches, currX - prevX, -(currY - prevY));
      }
    } else if (up > 0) {
      _state = ArmState.Unknown;
    }

    return null;
  }

  @Override
  public AbstractGestureEvent touchEnd() {
    _state = ArmState.Ready;
    return null;
  }
}
