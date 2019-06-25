/**
 * Copyright (c) 2008-2017 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input.logical;

import java.util.List;
import java.util.function.Predicate;

import com.ardor3d.input.InputState;
import com.ardor3d.input.gesture.event.AbstractGestureEvent;

public class GestureEventCondition implements Predicate<TwoInputStates> {

    private final Class<? extends AbstractGestureEvent> _clazz;

    public GestureEventCondition(final Class<? extends AbstractGestureEvent> clazzType) {
        _clazz = clazzType;
    }

    @Override
    public boolean test(final TwoInputStates states) {
        final InputState current = states.getCurrent();
        if (current == null) {
            return false;
        }

        final List<AbstractGestureEvent> events = current.getGestureState().getEvents();
        for (int i = 0, maxI = events.size(); i < maxI; i++) {
            if (_clazz.isInstance(events.get(i))) {
                return true;
            }
        }

        return false;
    }
}
