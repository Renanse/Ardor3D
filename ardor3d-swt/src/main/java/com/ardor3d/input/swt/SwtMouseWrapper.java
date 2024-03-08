/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.input.swt;

import static com.ardor3d.util.Preconditions.checkNotNull;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedList;

import com.ardor3d.util.AbstractIterator;
import com.ardor3d.util.PeekingIterator;
import com.ardor3d.util.collection.ImmutableMultiset;
import com.ardor3d.util.collection.Multiset;
import com.ardor3d.util.collection.SimpleEnumMultiset;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.widgets.Control;

import com.ardor3d.annotation.GuardedBy;
import com.ardor3d.annotation.ThreadSafe;
import com.ardor3d.framework.Canvas;
import com.ardor3d.input.mouse.ButtonState;
import com.ardor3d.input.mouse.MouseButton;
import com.ardor3d.input.mouse.MouseState;
import com.ardor3d.input.mouse.MouseWrapper;
import com.ardor3d.math.Vector2;

/**
 * A mouse wrapper for use with SWT.
 */
@ThreadSafe
public class SwtMouseWrapper implements MouseWrapper, MouseListener, MouseMoveListener, MouseWheelListener {
  @GuardedBy("this")
  private final LinkedList<MouseState> _upcomingEvents = new LinkedList<>();

  @GuardedBy("this")
  private SwtMouseIterator _currentIterator = null;

  private final Multiset<MouseButton> _clicks = new SimpleEnumMultiset<>(MouseButton.class);
  private final EnumMap<MouseButton, Long> _lastClickTime = new EnumMap<>(MouseButton.class);
  private final EnumSet<MouseButton> _clickArmed = EnumSet.noneOf(MouseButton.class);

  private final EnumMap<MouseButton, ButtonState> _lastButtonState = new EnumMap<>(MouseButton.class);

  private final Vector2 _lastClickLocation = new Vector2();

  private boolean _ignoreInput;

  @GuardedBy("this")
  private MouseState _lastState = null;

  private final Control _control;

  public SwtMouseWrapper(final Control control) {
    _control = checkNotNull(control, "control");
    for (final MouseButton mb : MouseButton.values()) {
      _lastButtonState.put(mb, ButtonState.UNDEFINED);
      _lastClickTime.put(mb, 0L);
    }
  }

  @Override
  public void init() {
    _control.addMouseListener(this);
    _control.addMouseMoveListener(this);
    _control.addMouseWheelListener(this);
  }

  @Override
  public synchronized void mouseDoubleClick(final MouseEvent mouseEvent) {
    // ignoring this. We'll handle (multi)click in a uniform way
  }

  @Override
  public synchronized void mouseDown(final MouseEvent e) {
    handleMouseButton(e, ButtonState.DOWN);
  }

  @Override
  public synchronized void mouseUp(final MouseEvent e) {
    handleMouseButton(e, ButtonState.UP);
  }

  private void handleMouseButton(final MouseEvent e, final ButtonState state) {
    if (_ignoreInput) {
      return;
    }

    final MouseButton mb = getButtonForEvent(e);

    // check for clicks
    processButtonForClick(mb, state);

    // save our state
    _lastButtonState.put(mb, state);

    // Add our new state
    addNextState(e, 0);
  }

  @Override
  public synchronized void mouseMove(final MouseEvent mouseEvent) {
    if (_ignoreInput) {
      return;
    }

    addNextState(mouseEvent, 0);
  }

  @Override
  public synchronized void mouseScrolled(final MouseEvent mouseEvent) {
    if (_ignoreInput) {
      return;
    }

    addNextState(mouseEvent, mouseEvent.count);
  }

  private int getDX(final MouseEvent e) {
    return getArdor3DX(e) - (_lastState != null ? _lastState.getX() : 0);
  }

  private int getDY(final MouseEvent e) {
    return getArdor3DY(e) - (_lastState != null ? _lastState.getY() : 0);
  }

  /**
   * Scale (for HighDPI, if needed) the X coordinate of the event.
   *
   * @param e
   *          our mouseEvent
   * @return the scaled X coordinate of the event.
   */
  private int getArdor3DX(final MouseEvent e) {
    final int x = e.x;
    if (_control instanceof Canvas canvas) {
      return (int) Math.round(canvas.scaleToScreenDpi(x));
    }
    return x;
  }

  /**
   * Scale (for HighDPI) and flip the Y coordinate of the event.
   *
   * @param e
   *          our mouseEvent
   * @return the scaled Y coordinate of the event, flipped relative to the component since we expect
   *         an origin in the lower left corner.
   */
  private int getArdor3DY(final MouseEvent e) {
    final int y = _control.getSize().y - e.y;
    if (_control instanceof Canvas canvas) {
      return (int) Math.round(canvas.scaleToScreenDpi(y));
    }
    return y;
  }

  @Override
  public synchronized PeekingIterator<MouseState> getMouseEvents() {
    // only create a new iterator if there isn't an existing, valid, one.
    if (_currentIterator == null || !_currentIterator.hasNext()) {
      _currentIterator = new SwtMouseIterator();
    }

    return _currentIterator;
  }

  private void addNextState(final MouseEvent mouseEvent, final int wheelDX) {
    final MouseState newState =
        new MouseState(getArdor3DX(mouseEvent), getArdor3DY(mouseEvent), getDX(mouseEvent), getDY(mouseEvent), wheelDX,
            new EnumMap<>(_lastButtonState), !_clicks.isEmpty() ? ImmutableMultiset.of(_clicks) : null);

    _upcomingEvents.add(newState);
    _lastState = newState;
  }

  private MouseButton getButtonForEvent(final MouseEvent e) {
    return switch (e.button) {
      case 1 -> MouseButton.LEFT;
      case 2 -> MouseButton.MIDDLE;
      case 3 -> MouseButton.RIGHT;
      case 4 -> MouseButton.FOUR;
      case 5 -> MouseButton.FIVE;
      case 6 -> MouseButton.SIX;
      case 7 -> MouseButton.SEVEN;
      case 8 -> MouseButton.EIGHT;
      case 9 -> MouseButton.NINE;
      default -> MouseButton.UNKNOWN;
    };
  }

  @Override
  public void setIgnoreInput(final boolean ignore) { _ignoreInput = ignore; }

  @Override
  public boolean isIgnoreInput() { return _ignoreInput; }

  private void processButtonForClick(final MouseButton b, final ButtonState state) {
    final int x = _lastState != null ? _lastState.getX() : 0;
    final int y = _lastState != null ? _lastState.getY() : 0;

    // clean out click states if we've moved the mouse or they've expired
    boolean clear = false;
    double comp = MouseState.CLICK_MAX_DELTA;
    if (_control instanceof Canvas canvas) {
      comp = canvas.scaleToScreenDpi(comp);
    }
    if (_lastClickLocation.distanceSquared(x, y) > comp * comp) {
      clear = true;
    }
    for (final var button : MouseButton.values()) {
      if (clear || System.currentTimeMillis() - _lastClickTime.get(button) > MouseState.CLICK_TIME_MS) {
        _clicks.setCount(button, 0);
        _clickArmed.remove(button);
      }
    }

    if (_clicks.isEmpty()) {
      _lastClickLocation.set(x, y);
    }

    if (state == ButtonState.DOWN) {
      // MOUSE DOWN
      // check if armed makes sense - if not, clear clicks for this button.
      if (_clickArmed.contains(b)) {
        _clicks.setCount(b, 0);
      }

      // arm click for this button
      _clickArmed.add(b);

      // remember when we clicked this button
      _lastClickTime.put(b, System.currentTimeMillis());
    } else if (state == ButtonState.UP) {
      // MOUSE UP
      // if we are not too late, and are armed...
      // ... and we have not moved too far since our last click (of any kind)
      if (_clickArmed.contains(b)) {
        // increment count of clicks for button b.
        _clicks.add(b);
      } else {
        // clear click count for button b.
        _clicks.setCount(b, 0);
      }
      // disarm click check for this button now that we've handled it
      _clickArmed.remove(b);
    }
  }

  private class SwtMouseIterator extends AbstractIterator<MouseState> implements PeekingIterator<MouseState> {
    @Override
    protected MouseState computeNext() {
      synchronized (SwtMouseWrapper.this) {
        if (_upcomingEvents.isEmpty()) {
          return endOfData();
        }

        return _upcomingEvents.poll();
      }
    }
  }
}
