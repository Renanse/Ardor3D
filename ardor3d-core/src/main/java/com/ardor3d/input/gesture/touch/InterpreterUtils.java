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
import com.ardor3d.math.Rectangle2;

public final class InterpreterUtils {

  public static TouchStatus determineTwoFingerStatus(final List<TouchHistory> touchInfo) {
    final TouchStatus statusA = touchInfo.get(0).currState;
    final TouchStatus statusB = touchInfo.get(1).currState;
    if (statusA == TouchStatus.Unknown || statusB == TouchStatus.Unknown) {
      return TouchStatus.Unknown;
    }
    if (statusA == TouchStatus.Moved) {
      return statusB;
    }
    if (statusB == TouchStatus.Moved) {
      return statusB;
    }

    return statusA;
  }

  public static void processTouchHistories(final List<TouchHistory> touchHistories,
      final List<AbstractTouchInterpreter> touchInterpreters, final List<AbstractGestureEvent> upcomingEvents) {
    final int historyCount = touchHistories.size();
    int up = 0, valid = 0;
    for (int i = 0, maxI = historyCount; i < maxI; i++) {
      final TouchHistory t = touchHistories.get(i);
      if (t.currState == TouchStatus.Up || t.currState == TouchStatus.Unknown) {
        up++;
      } else {
        valid++;
      }
    }

    for (int i = 0, maxI = touchInterpreters.size(); i < maxI; i++) {
      final AbstractTouchInterpreter abstractTouchInterpreter = touchInterpreters.get(i);
      final AbstractGestureEvent event = abstractTouchInterpreter.examine(touchHistories, up, valid);
      if (event != null) {
        upcomingEvents.add(event);
      }
      if (up == historyCount) {
        final AbstractGestureEvent endEvent = abstractTouchInterpreter.touchEnd();
        if (endEvent != null) {
          upcomingEvents.add(endEvent);
        }
      }
    }
  }

  public static void determineBounds(final List<TouchHistory> touchHistories, final Rectangle2 store) {
    int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
    int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
    boolean found = false;
    for (int i = 0, maxI = touchHistories.size(); i < maxI; i++) {
      final TouchHistory t = touchHistories.get(i);
      if (t.currState == TouchStatus.Moved || t.currState == TouchStatus.Down) {
        found = true;
        minX = Math.min(minX, t.currX);
        maxX = Math.max(maxX, t.currX);
        minY = Math.min(minY, t.currY);
        maxY = Math.max(maxY, t.currY);
      }
    }

    if (found) {
      store.set((minX + maxX) / 2, (minY + maxY) / 2, maxX - minX, maxY - minY);
    } else {
      store.set(-1, -1, -1, -1);
    }
  }

}
