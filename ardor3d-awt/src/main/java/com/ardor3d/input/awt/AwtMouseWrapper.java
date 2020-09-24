/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.input.awt;

import static com.google.common.base.Preconditions.checkNotNull;

import java.awt.Component;
import java.awt.Frame;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedList;

import com.ardor3d.annotation.GuardedBy;
import com.ardor3d.framework.Canvas;
import com.ardor3d.input.mouse.ButtonState;
import com.ardor3d.input.mouse.GrabbedState;
import com.ardor3d.input.mouse.MouseButton;
import com.ardor3d.input.mouse.MouseManager;
import com.ardor3d.input.mouse.MouseState;
import com.ardor3d.input.mouse.MouseWrapper;
import com.ardor3d.math.Vector2;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.EnumMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.PeekingIterator;

/**
 * Mouse wrapper class for use with AWT.
 */
public class AwtMouseWrapper implements MouseWrapper, MouseListener, MouseWheelListener, MouseMotionListener {
  @GuardedBy("this")
  protected final LinkedList<MouseState> _upcomingEvents = new LinkedList<>();

  @GuardedBy("this")
  protected AwtMouseIterator _currentIterator = null;

  protected final Multiset<MouseButton> _clicks = EnumMultiset.create(MouseButton.class);
  protected final EnumMap<MouseButton, Long> _lastClickTime = new EnumMap<>(MouseButton.class);
  protected final EnumSet<MouseButton> _clickArmed = EnumSet.noneOf(MouseButton.class);

  private final EnumMap<MouseButton, ButtonState> _lastButtonState = new EnumMap<>(MouseButton.class);

  private final Vector2 _lastClickLocation = new Vector2();

  protected boolean _consumeEvents = false;
  protected boolean _ignoreInput;

  @GuardedBy("this")
  protected MouseState _lastState = null;

  protected final Component _component;
  protected final Frame _frame;
  protected final MouseManager _manager;

  protected int _ignoreX = Integer.MAX_VALUE;
  protected int _ignoreY = Integer.MAX_VALUE;

  protected boolean _flipY = true;

  public AwtMouseWrapper(final Component component, final MouseManager manager) {
    _manager = manager;
    if (component instanceof Frame) {
      _frame = (Frame) (_component = component);
    } else {
      _component = checkNotNull(component, "component");
      _frame = null;
    }
    for (final MouseButton mb : MouseButton.values()) {
      _lastButtonState.put(mb, ButtonState.UNDEFINED);
      _lastClickTime.put(mb, 0L);
    }
  }

  @Override
  public void init() {
    _component.addMouseListener(this);
    _component.addMouseMotionListener(this);
    _component.addMouseWheelListener(this);
  }

  @Override
  public synchronized void mousePressed(final MouseEvent e) {
    handleMouseButton(e, ButtonState.DOWN);
  }

  @Override
  public synchronized void mouseReleased(final MouseEvent e) {
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

    if (_consumeEvents) {
      e.consume();
    }
  }

  @Override
  public synchronized void mouseDragged(final MouseEvent e) {
    // forward to mouseMoved.
    mouseMoved(e);
  }

  @Override
  public synchronized void mouseMoved(final MouseEvent e) {
    if (_ignoreInput) {
      return;
    }

    if (_consumeEvents) {
      e.consume();
    }

    // remember our current ardor3d position
    final int oldX = _lastState != null ? _lastState.getX() : 0;
    final int oldY = _lastState != null ? _lastState.getY() : 0;

    // check the state against the "ignore next" values
    if (_ignoreX != Integer.MAX_VALUE // shortcut to prevent dx/dy calculations
        && (_ignoreX == getDX(e) && _ignoreY == getDY(e))) {

      // we matched, so we'll consider this a "mouse pointer reset move"
      // so reset ignore to let the next move event through.
      _ignoreX = Integer.MAX_VALUE;
      _ignoreY = Integer.MAX_VALUE;

      // exit without adding an event to our queue
      return;
    }

    // save our old "last state."
    final MouseState _savedState = _lastState;

    // Add our latest state info to the queue
    addNextState(e, 0);

    // If we have a valid move... should always be the case, but occasionally something slips through.
    if (_lastState.getDx() != 0 || _lastState.getDy() != 0) {

      // Ask our manager if we're currently "captured"
      if (_manager.getGrabbed() == GrabbedState.GRABBED) {

        // if so, set "ignore next" to the inverse of this move
        _ignoreX = -_lastState.getDx();
        _ignoreY = -_lastState.getDy();

        // Move us back to our last position.
        _manager.setPosition(oldX, oldY);

        // And finally, revert our _lastState.
        _lastState = _savedState;
      } else {
        // otherwise, set us to not ignore anything. This may be unnecessary, but prevents any possible
        // "ignore" bleeding.
        _ignoreX = Integer.MAX_VALUE;
        _ignoreY = Integer.MAX_VALUE;
      }
    }
  }

  @Override
  public void mouseWheelMoved(final MouseWheelEvent e) {
    if (_ignoreInput) {
      return;
    }

    addNextState(e, e.getWheelRotation());

    if (_consumeEvents) {
      e.consume();
    }
  }

  protected int getDX(final MouseEvent e) {
    return getArdor3DX(e) - (_lastState != null ? _lastState.getX() : 0);
  }

  protected int getDY(final MouseEvent e) {
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
    final int x = e.getX();
    if (_component instanceof Canvas) {
      final Canvas canvas = (Canvas) _component;
      return (int) Math.round(canvas.scaleToScreenDpi(x));
    }
    return x;
  }

  /**
   * Scale (for HighDPI) and flip (if requested) the Y coordinate of the event.
   *
   * @param e
   *          our mouseEvent
   * @return the scaled Y coordinate of the event, flipped (if requested) relative to the component
   *         since we expect an origin in the lower left corner.
   */
  protected int getArdor3DY(final MouseEvent e) {
    int y = e.getY();
    if (_flipY) {
      final int height = (_frame != null && _frame.getComponentCount() > 0) ? _frame.getComponent(0).getHeight()
          : _component.getHeight();
      y = height - y;
    }
    if (_component instanceof Canvas) {
      final Canvas canvas = (Canvas) _component;
      y = (int) Math.round(canvas.scaleToScreenDpi(y));
    }
    return y;
  }

  protected void setStateForButton(final MouseEvent e, final EnumMap<MouseButton, ButtonState> buttons,
      final ButtonState buttonState) {
    final MouseButton button = getButtonForEvent(e);
    buttons.put(button, buttonState);
  }

  @Override
  public synchronized PeekingIterator<MouseState> getMouseEvents() {
    // only create a new iterator if there isn't an existing, valid, one.
    if (_currentIterator == null || !_currentIterator.hasNext()) {
      _currentIterator = new AwtMouseIterator();
    }

    return _currentIterator;
  }

  private void addNextState(final MouseEvent mouseEvent, final int wheelDX) {
    final MouseState newState =
        new MouseState(getArdor3DX(mouseEvent), getArdor3DY(mouseEvent), getDX(mouseEvent), getDY(mouseEvent), wheelDX,
            new EnumMap<>(_lastButtonState), !_clicks.isEmpty() ? EnumMultiset.create(_clicks) : null);

    synchronized (AwtMouseWrapper.this) {
      _upcomingEvents.add(newState);
      _lastState = newState;
    }
  }

  protected MouseButton getButtonForEvent(final MouseEvent e) {
    switch (e.getButton()) {
      case MouseEvent.BUTTON1:
        return MouseButton.LEFT;
      case MouseEvent.BUTTON2:
        return MouseButton.MIDDLE;
      case MouseEvent.BUTTON3:
        return MouseButton.RIGHT;
      case 4:
        return MouseButton.FOUR;
      case 5:
        return MouseButton.FIVE;
      case 6:
        return MouseButton.SIX;
      case 7:
        return MouseButton.SEVEN;
      case 8:
        return MouseButton.EIGHT;
      case 9:
        return MouseButton.NINE;
      default:
        return MouseButton.UNKNOWN;
    }
  }

  public boolean isFlipY() { return _flipY; }

  public void setFlipY(final boolean flipY) { _flipY = flipY; }

  public boolean isConsumeEvents() { return _consumeEvents; }

  public void setConsumeEvents(final boolean consumeEvents) { _consumeEvents = consumeEvents; }

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
    if (_component instanceof Canvas) {
      final Canvas canvas = (Canvas) _component;
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

  protected class AwtMouseIterator extends AbstractIterator<MouseState> implements PeekingIterator<MouseState> {
    @Override
    protected MouseState computeNext() {
      synchronized (AwtMouseWrapper.this) {
        if (_upcomingEvents.isEmpty()) {
          return endOfData();
        }

        return _upcomingEvents.poll();
      }
    }
  }

  // -- The following interface methods are not used. --

  @Override
  public synchronized void mouseClicked(final MouseEvent e) {
    // Yes, we could use the click count here, but in the interests of this working the same way as SWT
    // and Native,
    // we will do it the same way they do it.
    if (_consumeEvents) {
      e.consume();
    }
  }

  @Override
  public synchronized void mouseEntered(final MouseEvent e) {
    // ignore this
    if (_consumeEvents) {
      e.consume();
    }
  }

  @Override
  public synchronized void mouseExited(final MouseEvent e) {
    // ignore this
    if (_consumeEvents) {
      e.consume();
    }
  }

}
