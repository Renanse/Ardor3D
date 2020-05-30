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
import com.ardor3d.input.gesture.event.RotateGestureEvent;

public class RotateInterpreter extends AbstractTouchInterpreter {

  private final double _threshold;

  double _lastAngle, _totalAngle;

  public RotateInterpreter(final double threshold) {
    super(2);
    _threshold = threshold;
  }

  @Override
  public AbstractGestureEvent examine(final List<TouchHistory> touchInfo, final int up, final int valid) {
    if (valid == 2) {
      final TouchHistory touchA = touchInfo.get(0);
      final TouchHistory touchB = touchInfo.get(1);
      final double angle = touchA.angleBetween(touchB);
      final TouchStatus finger = InterpreterUtils.determineTwoFingerStatus(touchInfo);
      switch (_state) {
        case Ready:
          if (finger == TouchStatus.Down) {
            _state = ArmState.Armed;
            // save our starting angle for threshold
            _lastAngle = angle;
          } else {
            // invalid
            _state = ArmState.Unknown;
          }
          break;
        case Armed:
          if (finger == TouchStatus.Moved) {
            final double dAngle = angle - _lastAngle;
            if (Math.abs(dAngle) >= _threshold) {
              _state = ArmState.Triggered;
              _totalAngle = dAngle;
              _lastAngle = angle;
              InterpreterUtils.determineBounds(touchInfo, _lastBounds);
              return new RotateGestureEvent(true, _lastBounds, angle, dAngle, _totalAngle);
            }
          } else {
            // invalid
            _state = ArmState.Unknown;
          }
          break;
        case Triggered:
          if (finger == TouchStatus.Moved) {
            final double dAngle = angle - _lastAngle;
            _totalAngle += dAngle;
            _lastAngle = angle;
            InterpreterUtils.determineBounds(touchInfo, _lastBounds);
            return new RotateGestureEvent(false, _lastBounds, angle, dAngle, _totalAngle);
          }

          // invalid
          _state = ArmState.Unknown;
          break;
        case Unknown:
        default:
          // do nothing
          break;
      }
    }

    return null;
  }

  @Override
  public AbstractGestureEvent touchEnd() {
    _state = ArmState.Ready;
    return null;
  }
}
