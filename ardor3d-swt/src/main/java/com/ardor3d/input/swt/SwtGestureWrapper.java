/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.input.swt;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.TouchEvent;
import org.eclipse.swt.events.TouchListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Touch;

import com.ardor3d.annotation.GuardedBy;
import com.ardor3d.annotation.ThreadSafe;
import com.ardor3d.input.gesture.GestureWrapper;
import com.ardor3d.input.gesture.event.AbstractGestureEvent;
import com.ardor3d.input.gesture.touch.AbstractTouchInterpreter;
import com.ardor3d.input.gesture.touch.InterpreterUtils;
import com.ardor3d.input.gesture.touch.LongPressInterpreter;
import com.ardor3d.input.gesture.touch.PanInterpreter;
import com.ardor3d.input.gesture.touch.PinchInterpreter;
import com.ardor3d.input.gesture.touch.RotateInterpreter;
import com.ardor3d.input.gesture.touch.SwipeInterpreter;
import com.ardor3d.input.gesture.touch.TouchHistory;
import com.ardor3d.input.gesture.touch.TouchStatus;
import com.ardor3d.input.mouse.MouseWrapper;
import com.ardor3d.math.util.MathUtils;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.PeekingIterator;

/**
 * A gesture wrapper for use with SWT.
 */
@ThreadSafe
public class SwtGestureWrapper implements GestureWrapper, TouchListener {
  @GuardedBy("this")
  private final LinkedList<AbstractGestureEvent> _upcomingEvents = new LinkedList<>();

  @GuardedBy("this")
  private SwtGestureIterator _currentIterator = null;

  @GuardedBy("this")
  private final List<AbstractTouchInterpreter> _touchInterpreters = new ArrayList<>();

  @GuardedBy("this")
  private final List<TouchHistory> _touchHistories = new ArrayList<>();

  private final Control _control;
  private final MouseWrapper _mouse;

  private final Predicate<TouchHistory> _cleanupTest =
      t -> t.currState == TouchStatus.Up || t.currState == TouchStatus.Unknown;

  public SwtGestureWrapper(final Control control) {
    this(control, null, true);
  }

  public SwtGestureWrapper(final Control control, final MouseWrapper mouse, final boolean addDefaultInterpreters) {
    _control = control;
    _mouse = mouse;

    if (addDefaultInterpreters) {
      addTouchInterpreter(new RotateInterpreter(10 * MathUtils.DEG_TO_RAD));
      addTouchInterpreter(new PinchInterpreter(40));
      addTouchInterpreter(new PanInterpreter(2));
      addTouchInterpreter(new SwipeInterpreter(2, 1.2, 100L));
      addTouchInterpreter(new LongPressInterpreter());
    }
  }

  @Override
  public void init() {
    _control.setTouchEnabled(true);
    _control.addTouchListener(this);
  }

  public void addTouchInterpreter(final AbstractTouchInterpreter interpreter) {
    _touchInterpreters.add(interpreter);
  }

  public void removeTouchInterpreter(final AbstractTouchInterpreter interpreter) {
    _touchInterpreters.remove(interpreter);
  }

  public void clearInterpreters() {
    _touchInterpreters.clear();
  }

  @Override
  public PeekingIterator<AbstractGestureEvent> getGestureEvents() {
    updateInterpreters();

    if (_currentIterator == null || !_currentIterator.hasNext()) {
      _currentIterator = new SwtGestureIterator();
    }

    return _currentIterator;
  }

  @Override
  public void touch(final TouchEvent e) {
    // update our tracking of touches
    updateTouchHistory(e);

    // now, offer our tracking info to any registered gesture processors
    synchronized (SwtGestureWrapper.this) {
      InterpreterUtils.processTouchHistories(_touchHistories, _touchInterpreters, _upcomingEvents);
    }

    // enable or disable mouse/pointer events based on the events we generated
    updateMouseTracking();

    // clean up old touches - it appears SWT keeps creating new ids
    _touchHistories.removeIf(_cleanupTest);
  }

  private void updateInterpreters() {
    synchronized (SwtGestureWrapper.this) {
      for (int i = 0, maxI = _touchInterpreters.size(); i < maxI; i++) {
        final AbstractGestureEvent event = _touchInterpreters.get(i).update();
        if (event != null) {
          _upcomingEvents.add(event);
        }
      }
    }
  }

  private void updateTouchHistory(final TouchEvent e) {
    synchronized (SwtGestureWrapper.this) {
      for (int i = 0; i < e.touches.length; i++) {
        final Touch t = e.touches[i];
        final String touchId = getId(t);
        final TouchStatus state = getTouchStatus(t);
        final TouchHistory history = getTouchHistory(touchId);

        if (state == TouchStatus.Down && history != null) {
          // something was not cleaned up properly, so invalidate this touch
          resetTouchHistory();
          return;
        }

        if (state != TouchStatus.Down && history == null) {
          // we SHOULD have history, but we don't.
          resetTouchHistory();
          return;
        }

        if (state == TouchStatus.Down) {
          // Add new history
          _touchHistories.add(new TouchHistory(touchId, t.x, t.y, state));
        } else {
          // update existing
          history.update(t.x, t.y, state);
        }
      }
    }
  }

  private void updateMouseTracking() {
    if (_mouse != null) {
      _mouse.setIgnoreInput(!_upcomingEvents.isEmpty());
    }
  }

  private void resetTouchHistory() {
    synchronized (SwtGestureWrapper.this) {
      System.err.println("clear history");
      _touchHistories.clear();
    }
  }

  private TouchHistory getTouchHistory(final String touchId) {
    synchronized (SwtGestureWrapper.this) {
      for (int i = 0, maxI = _touchHistories.size(); i < maxI; i++) {
        final TouchHistory info = _touchHistories.get(i);
        if (info.id.equals(touchId)) {
          return info;
        }
      }
    }
    return null;
  }

  private String getId(final Touch t) {
    return Long.toString(t.id);
  }

  private TouchStatus getTouchStatus(final Touch t) {
    if (t != null) {
      switch (t.state) {
        case SWT.TOUCHSTATE_DOWN:
          return TouchStatus.Down;
        case SWT.TOUCHSTATE_UP:
          return TouchStatus.Up;
        case SWT.TOUCHSTATE_MOVE:
          return TouchStatus.Moved;
      }
    }
    return TouchStatus.Unknown;
  }

  private class SwtGestureIterator extends AbstractIterator<AbstractGestureEvent>
      implements PeekingIterator<AbstractGestureEvent> {
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
