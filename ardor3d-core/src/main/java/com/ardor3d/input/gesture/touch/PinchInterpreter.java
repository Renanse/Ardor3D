/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
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
import com.ardor3d.input.gesture.event.PinchGestureEvent;

public class PinchInterpreter extends AbstractTouchInterpreter {

  private final double _threshold;
  private double _initialDistance;

  public PinchInterpreter(final double threshold) {
    super(2);
    _threshold = threshold;
    _state = ArmState.Ready;
  }

  @Override
  public AbstractGestureEvent examine(final List<TouchHistory> touchInfo, final int up, final int valid) {
    if (valid == 2) {
      final double distance = touchInfo.get(0).distanceTo(touchInfo.get(1));
      if (distance > 0.0) {
        final TouchStatus finger = InterpreterUtils.determineTwoFingerStatus(touchInfo);
        switch (_state) {
          case Ready:
            if (finger == TouchStatus.Down) {
              _state = ArmState.Armed;
            } else {
              // invalid
              _state = ArmState.Unknown;
            }
            break;
          case Armed:
            if (finger == TouchStatus.Moved) {
              final double movement =
                  Math.max(touchInfo.get(0).distanceFromInitialSq(), touchInfo.get(1).distanceFromInitialSq());
              if (movement >= _threshold * _threshold) {
                // save our starting distance
                _initialDistance = distance;
                _state = ArmState.Triggered;

                InterpreterUtils.determineBounds(touchInfo, _lastBounds);
                return new PinchGestureEvent(true, _lastBounds, distance / _initialDistance);
              }
            } else {
              // invalid
              _state = ArmState.Unknown;
            }
            break;
          case Triggered:
            if (finger == TouchStatus.Moved) {
              InterpreterUtils.determineBounds(touchInfo, _lastBounds);
              return new PinchGestureEvent(false, _lastBounds, distance / _initialDistance);
            } else {
              // invalid
              _state = ArmState.Unknown;
            }
            break;
          case Unknown:
          default:
            // do nothing
            break;
        }
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
