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
import com.ardor3d.input.mouse.ButtonState;
import com.ardor3d.input.mouse.GrabbedState;
import com.ardor3d.input.mouse.MouseButton;
import com.ardor3d.input.mouse.MouseManager;
import com.ardor3d.input.mouse.MouseState;
import com.ardor3d.input.mouse.MouseWrapper;
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

  @GuardedBy("this")
  protected MouseState _lastState = null;

  protected boolean _consumeEvents = false;
  protected boolean _ignoreInput;

  protected final Component _component;
  protected final Frame _frame;
  protected final MouseManager _manager;

  protected final Multiset<MouseButton> _clicks = EnumMultiset.create(MouseButton.class);
  protected final EnumMap<MouseButton, Long> _lastClickTime = new EnumMap<>(MouseButton.class);
  protected final EnumSet<MouseButton> _clickArmed = EnumSet.noneOf(MouseButton.class);

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
  public synchronized PeekingIterator<MouseState> getMouseEvents() {
    expireClickEvents();

    if (_currentIterator == null || !_currentIterator.hasNext()) {
      _currentIterator = new AwtMouseIterator();
    }

    return _currentIterator;
  }

  protected void expireClickEvents() {
    if (!_clicks.isEmpty()) {
      for (final MouseButton mb : MouseButton.values()) {
        if (System.currentTimeMillis() - _lastClickTime.get(mb) > MouseState.CLICK_TIME_MS) {
          _clicks.setCount(mb, 0);
        }
      }
    }
  }

  public boolean isFlipY() { return _flipY; }

  public void setFlipY(final boolean flipY) { _flipY = flipY; }

  @Override
  public synchronized void mousePressed(final MouseEvent e) {
    if (_ignoreInput) {
      return;
    }

    final MouseButton b = getButtonForEvent(e);
    if (_clickArmed.contains(b)) {
      _clicks.setCount(b, 0);
    }
    _clickArmed.add(b);
    _lastClickTime.put(b, System.currentTimeMillis());

    initState(e);
    if (_consumeEvents) {
      e.consume();
    }

    final EnumMap<MouseButton, ButtonState> buttons = _lastState.getButtonStates();

    setStateForButton(e, buttons, ButtonState.DOWN);

    addNewState(e, buttons, null);
  }

  @Override
  public synchronized void mouseReleased(final MouseEvent e) {
    if (_ignoreInput) {
      return;
    }

    initState(e);
    if (_consumeEvents) {
      e.consume();
    }

    final EnumMap<MouseButton, ButtonState> buttons = _lastState.getButtonStates();

    setStateForButton(e, buttons, ButtonState.UP);

    final MouseButton b = getButtonForEvent(e);
    if (_clickArmed.contains(b) && (System.currentTimeMillis() - _lastClickTime.get(b) <= MouseState.CLICK_TIME_MS)) {
      _clicks.add(b); // increment count of clicks for button b.
      // XXX: Note the double event add... this prevents sticky click counts, but is it the best way?
      addNewState(e, buttons, EnumMultiset.create(_clicks));
    } else {
      _clicks.setCount(b, 0); // clear click count for button b.
    }
    _clickArmed.remove(b);

    addNewState(e, buttons, null);
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

    _clickArmed.clear();
    _clicks.clear();

    // check that we have a valid _lastState
    initState(e);
    if (_consumeEvents) {
      e.consume();
    }

    // remember our current ardor3d position
    final int oldX = _lastState.getX(), oldY = _lastState.getY();

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
    addNewState(e, _lastState.getButtonStates(), null);

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

    initState(e);

    addNewState(e, _lastState.getButtonStates(), null);
    if (_consumeEvents) {
      e.consume();
    }
  }

  protected void initState(final MouseEvent mouseEvent) {
    if (_lastState == null) {
      _lastState = new MouseState(mouseEvent.getX(), getArdor3DY(mouseEvent), 0, 0, 0, null, null);
    }
  }

  protected void addNewState(final MouseEvent mouseEvent, final EnumMap<MouseButton, ButtonState> enumMap,
      final Multiset<MouseButton> clicks) {
    final MouseState newState =
        new MouseState(mouseEvent.getX(), getArdor3DY(mouseEvent), getDX(mouseEvent), getDY(mouseEvent),
            (mouseEvent instanceof MouseWheelEvent ? ((MouseWheelEvent) mouseEvent).getWheelRotation() : 0), enumMap,
            clicks);

    synchronized (AwtMouseWrapper.this) {
      _upcomingEvents.add(newState);
    }
    _lastState = newState;
  }

  protected int getDX(final MouseEvent e) {
    return e.getX() - _lastState.getX();
  }

  protected int getDY(final MouseEvent e) {
    return getArdor3DY(e) - _lastState.getY();
  }

  /**
   * @param e
   *          our mouseEvent
   * @return the Y coordinate of the event, flipped relative to the component since we expect an
   *         origin in the lower left corner.
   */
  protected int getArdor3DY(final MouseEvent e) {
    if (_flipY) {
      final int height = (_frame != null && _frame.getComponentCount() > 0) ? _frame.getComponent(0).getHeight()
          : _component.getHeight();
      return height - e.getY();
    } else {
      return e.getY();
    }
  }

  protected void setStateForButton(final MouseEvent e, final EnumMap<MouseButton, ButtonState> buttons,
      final ButtonState buttonState) {
    final MouseButton button = getButtonForEvent(e);
    buttons.put(button, buttonState);
  }

  protected MouseButton getButtonForEvent(final MouseEvent e) {
    MouseButton button;
    switch (e.getButton()) {
      case MouseEvent.BUTTON1:
        button = MouseButton.LEFT;
        break;
      case MouseEvent.BUTTON2:
        button = MouseButton.MIDDLE;
        break;
      case MouseEvent.BUTTON3:
        button = MouseButton.RIGHT;
        break;
      default:
        throw new RuntimeException("unknown button: " + e.getButton());
    }
    return button;
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

  public boolean isConsumeEvents() { return _consumeEvents; }

  public void setConsumeEvents(final boolean consumeEvents) { _consumeEvents = consumeEvents; }

  @Override
  public void setIgnoreInput(final boolean ignore) { _ignoreInput = ignore; }

  @Override
  public boolean isIgnoreInput() { return _ignoreInput; }
}
