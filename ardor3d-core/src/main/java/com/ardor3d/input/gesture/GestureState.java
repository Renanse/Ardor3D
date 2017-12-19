/**
 * Copyright (c) 2008-2017 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input.gesture;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.ardor3d.annotation.Immutable;
import com.ardor3d.input.gesture.event.AbstractGestureEvent;
import com.google.common.collect.Lists;

@Immutable
public class GestureState {

    public static final GestureState NOTHING = new GestureState(0);

    protected final List<AbstractGestureEvent> _eventsSinceLastState = Lists.newArrayList();

    protected GestureState(final int ignore) {}

    public GestureState() {}

    public void addEvent(final AbstractGestureEvent event) {
        _eventsSinceLastState.add(event);
    }

    public List<AbstractGestureEvent> getEvents() {
        Collections.sort(_eventsSinceLastState, new Comparator<AbstractGestureEvent>() {
            public int compare(final AbstractGestureEvent o1, final AbstractGestureEvent o2) {
                return (int) (o2.getNanos() - o1.getNanos());
            }
        });

        return Collections.unmodifiableList(_eventsSinceLastState);
    }

    public void clearEvents() {
        _eventsSinceLastState.clear();
    }

    @SuppressWarnings("unchecked")
    public <E extends AbstractGestureEvent> E first(final Class<E> eventType) {
        for (int i = 0, maxI = _eventsSinceLastState.size(); i < maxI; i++) {
            final AbstractGestureEvent event = _eventsSinceLastState.get(i);
            if (eventType.isInstance(event)) {
                return (E) event;
            }
        }
        return null;
    }
}
