/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.input.glfw;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedList;

import com.ardor3d.util.AbstractIterator;
import com.ardor3d.util.PeekingIterator;
import com.ardor3d.util.collection.ImmutableMultiset;
import com.ardor3d.util.collection.Multiset;
import com.ardor3d.util.collection.SimpleEnumMultiset;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;

import com.ardor3d.annotation.GuardedBy;
import com.ardor3d.framework.lwjgl3.GLFWCanvas;
import com.ardor3d.input.mouse.ButtonState;
import com.ardor3d.input.mouse.MouseButton;
import com.ardor3d.input.mouse.MouseState;
import com.ardor3d.input.mouse.MouseWrapper;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.util.MathUtils;

public class GLFWMouseWrapper implements MouseWrapper {

  @GuardedBy("this")
  protected final LinkedList<MouseState> _upcomingEvents = new LinkedList<>();

  @SuppressWarnings("unused")
  private GLFWMouseButtonCallback _mouseButtonCallback;

  @SuppressWarnings("unused")
  private GLFWCursorPosCallback _cursorPosCallback;

  @SuppressWarnings("unused")
  private GLFWScrollCallback _scrollCallback;

  private MouseIterator _currentIterator = null;

  private final Multiset<MouseButton> _clicks = new SimpleEnumMultiset<>(MouseButton.class);
  private final EnumMap<MouseButton, Long> _lastClickTime = new EnumMap<>(MouseButton.class);
  private final EnumSet<MouseButton> _clickArmed = EnumSet.noneOf(MouseButton.class);

  private final EnumMap<MouseButton, ButtonState> _lastButtonState = new EnumMap<>(MouseButton.class);

  private final Vector2 _lastClickLocation = new Vector2();

  private boolean _ignoreInput;
  private MouseState _lastState;

  private final GLFWCanvas _canvas;

  public GLFWMouseWrapper(final GLFWCanvas canvas) {
    _canvas = canvas;

    // fill our button state map with undefined
    for (final MouseButton mb : MouseButton.values()) {
      _lastButtonState.put(mb, ButtonState.UNDEFINED);
      _lastClickTime.put(mb, 0L);
    }
  }

  @Override
  public void init() {
    GLFW.glfwSetMouseButtonCallback(_canvas.getWindowId(), _mouseButtonCallback = new GLFWMouseButtonCallback() {
      @Override
      public void invoke(final long window, final int button, final int action, final int mods) {
        if (_ignoreInput) {
          return;
        }

        final MouseButton mb = getButtonByIndex(button);

        final ButtonState state = action == GLFW.GLFW_PRESS ? ButtonState.DOWN : ButtonState.UP;
        final int x = _lastState != null ? _lastState.getX() : 0;
        final int y = _lastState != null ? _lastState.getY() : 0;

        // check for clicks
        processButtonForClick(x, y, mb, state);

        // save our state
        _lastButtonState.put(mb, state);

        // Add our new state
        addNextState(new MouseState(x, y, 0, 0, 0, new EnumMap<>(_lastButtonState),
            !_clicks.isEmpty() ? ImmutableMultiset.of(_clicks) : null));
      }
    });

    GLFW.glfwSetCursorPosCallback(_canvas.getWindowId(), _cursorPosCallback = new GLFWCursorPosCallback() {
      @Override
      public void invoke(final long window, final double xpos, final double ypos) {
        if (_ignoreInput) {
          return;
        }

        final int x = (int) MathUtils.round(xpos);
        final int y = _canvas.getContentHeight() - (int) MathUtils.round(ypos);
        final int dx = _lastState != null ? x - _lastState.getX() : 0;
        final int dy = _lastState != null ? y - _lastState.getY() : 0;

        // Add our new state
        final MouseState event = new MouseState(x, y, dx, dy, 0, new EnumMap<>(_lastButtonState), null);
        addNextState(event);
      }
    });

    GLFW.glfwSetScrollCallback(_canvas.getWindowId(), _scrollCallback = new GLFWScrollCallback() {
      double wheelAccum = 0.0;

      @Override
      public void invoke(final long window, final double xoffset, final double yoffset) {
        wheelAccum += yoffset;
        final int dw = (int) MathUtils.floor(wheelAccum);
        if (dw == 0) {
          return;
        }
        wheelAccum -= dw;

        // Add our new state
        final int x = _lastState != null ? _lastState.getX() : 0;
        final int y = _lastState != null ? _lastState.getY() : 0;
        final MouseState event = new MouseState(x, y, 0, 0, dw, new EnumMap<>(_lastButtonState), null);
        addNextState(event);
      }
    });
  }

  @Override
  public PeekingIterator<MouseState> getMouseEvents() {
    // only create a new iterator if there isn't an existing, valid, one.
    if (_currentIterator == null || !_currentIterator.hasNext()) {
      _currentIterator = new MouseIterator();
    }

    return _currentIterator;
  }

  protected void addNextState(final MouseState nextState) {
    _upcomingEvents.add(nextState);
    _lastState = nextState;
  }

  protected static MouseButton getButtonByIndex(final int glfwButtonIndex) {
    return switch (glfwButtonIndex) {
      case GLFW.GLFW_MOUSE_BUTTON_1 -> MouseButton.LEFT;
      case GLFW.GLFW_MOUSE_BUTTON_2 -> MouseButton.RIGHT;
      case GLFW.GLFW_MOUSE_BUTTON_3 -> MouseButton.MIDDLE;
      case GLFW.GLFW_MOUSE_BUTTON_4 -> MouseButton.FOUR;
      case GLFW.GLFW_MOUSE_BUTTON_5 -> MouseButton.FIVE;
      case GLFW.GLFW_MOUSE_BUTTON_6 -> MouseButton.SIX;
      case GLFW.GLFW_MOUSE_BUTTON_7 -> MouseButton.SEVEN;
      case GLFW.GLFW_MOUSE_BUTTON_8 -> MouseButton.EIGHT;
      default -> MouseButton.UNKNOWN;
    };
  }

  @Override
  public void setIgnoreInput(final boolean ignore) { _ignoreInput = ignore; }

  @Override
  public boolean isIgnoreInput() { return _ignoreInput; }

  private void processButtonForClick(final int x, final int y, final MouseButton b, final ButtonState state) {
    // clean out click states if we've moved the mouse or they've expired
    boolean clear = false;
    final double comp = MouseState.CLICK_MAX_DELTA;
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

  private class MouseIterator extends AbstractIterator<MouseState> implements PeekingIterator<MouseState> {
    @Override
    protected MouseState computeNext() {
      synchronized (GLFWMouseWrapper.this) {
        if (_upcomingEvents.isEmpty()) {
          return endOfData();
        }

        return _upcomingEvents.poll();
      }
    }
  }
}
