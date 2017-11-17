/**
 * Copyright (c) 2008-2017 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input.swt;

import java.util.LinkedList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.GestureEvent;
import org.eclipse.swt.events.GestureListener;
import org.eclipse.swt.widgets.Control;

import com.ardor3d.annotation.GuardedBy;
import com.ardor3d.annotation.ThreadSafe;
import com.ardor3d.input.gestures.AbstractGestureEvent;
import com.ardor3d.input.gestures.GestureWrapper;
import com.ardor3d.input.gestures.PanGestureEvent;
import com.ardor3d.input.gestures.PinchGestureEvent;
import com.ardor3d.input.gestures.RotateGestureEvent;
import com.ardor3d.input.gestures.SwipeGestureEvent;
import com.ardor3d.math.MathUtils;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.PeekingIterator;

/**
 * A gesture wrapper for use with SWT.
 */
@ThreadSafe
public class SwtGestureWrapper implements GestureWrapper, GestureListener {
    @GuardedBy("this")
    private final LinkedList<AbstractGestureEvent> _upcomingEvents = new LinkedList<AbstractGestureEvent>();

    private final Control _control;

    @GuardedBy("this")
    private SwtGestureIterator _currentIterator = null;

    public SwtGestureWrapper(final Control control) {
        _control = control;
    }

    @Override
    public void init() {
        _control.addGestureListener(this);
    }

    @Override
    public PeekingIterator<AbstractGestureEvent> getEvents() {
        if (_currentIterator == null || !_currentIterator.hasNext()) {
            _currentIterator = new SwtGestureIterator();
        }

        return _currentIterator;
    }

    @Override
    public synchronized void gesture(final GestureEvent e) {
        switch (e.detail) {
            case SWT.GESTURE_MAGNIFY:
                _upcomingEvents.add(new PinchGestureEvent(e.magnification));
                break;
            case SWT.GESTURE_ROTATE:
                _upcomingEvents.add(new RotateGestureEvent(e.rotation * MathUtils.DEG_TO_RAD));
                break;
            case SWT.GESTURE_PAN:
                _upcomingEvents.add(new PanGestureEvent(e.xDirection, e.yDirection));
                break;
            case SWT.GESTURE_SWIPE:
                _upcomingEvents.add(new SwipeGestureEvent(e.xDirection, e.yDirection));
                break;
        }
    }

    private class SwtGestureIterator extends AbstractIterator<AbstractGestureEvent> implements
    PeekingIterator<AbstractGestureEvent> {
        @Override
        protected AbstractGestureEvent computeNext() {
            synchronized (SwtGestureWrapper.this) {
                if (_upcomingEvents.isEmpty()) {
                    return endOfData();
                }

                return _upcomingEvents.poll();
            }
        }
    }
}
