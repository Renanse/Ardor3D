/**
 * Copyright (c) 2008-2017 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input.gesture.touch;

import java.util.List;

import com.ardor3d.input.gesture.event.AbstractGestureEvent;

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

}