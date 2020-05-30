/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.input.glfw;

import java.util.LinkedList;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWKeyCallback;

import com.ardor3d.annotation.GuardedBy;
import com.ardor3d.framework.lwjgl3.GLFWCanvas;
import com.ardor3d.input.keyboard.Key;
import com.ardor3d.input.keyboard.KeyEvent;
import com.ardor3d.input.keyboard.KeyState;
import com.ardor3d.input.keyboard.KeyboardWrapper;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.PeekingIterator;

public class GLFWKeyboardWrapper implements KeyboardWrapper {

  @GuardedBy("this")
  protected final LinkedList<KeyEvent> _upcomingEvents = new LinkedList<>();

  @SuppressWarnings("unused")
  private GLFWKeyCallback _keyCallback;

  private KeyboardIterator _currentIterator;

  private final GLFWCanvas _canvas;

  public GLFWKeyboardWrapper(final GLFWCanvas canvas) {
    _canvas = canvas;
  }

  @Override
  public void init() {
    GLFW.glfwSetKeyCallback(_canvas.getWindowId(), (_keyCallback = new GLFWKeyCallback() {

      @Override
      public void invoke(final long window, final int keyCode, final int scancode, final int action, final int mods) {

        final Key key = GLFWKey.findByCode(keyCode);
        final KeyState state;
        switch (action) {
          case GLFW.GLFW_PRESS:
            state = KeyState.DOWN;
            break;
          case GLFW.GLFW_RELEASE:
            state = KeyState.UP;
            break;
          case GLFW.GLFW_REPEAT:
          default:
            // do nothing on REPEAT?
            return;
        }

        _upcomingEvents.add(new KeyEvent(key, state));
      }
    }));
  }

  @Override
  public synchronized PeekingIterator<KeyEvent> getKeyEvents() {
    if (_currentIterator == null || !_currentIterator.hasNext()) {
      _currentIterator = new KeyboardIterator();
    }

    return _currentIterator;
  }

  private class KeyboardIterator extends AbstractIterator<KeyEvent> implements PeekingIterator<KeyEvent> {
    @Override
    protected KeyEvent computeNext() {
      synchronized (GLFWKeyboardWrapper.this) {
        if (_upcomingEvents.isEmpty()) {
          return endOfData();
        }

        return _upcomingEvents.poll();
      }
    }
  }

}
