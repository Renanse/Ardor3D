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

import java.util.LinkedList;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCharCallback;

import com.ardor3d.annotation.GuardedBy;
import com.ardor3d.framework.lwjgl3.GLFWCanvas;
import com.ardor3d.input.character.CharacterInputEvent;
import com.ardor3d.input.character.CharacterInputWrapper;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.PeekingIterator;

public class GLFWCharacterInputWrapper implements CharacterInputWrapper {

  @GuardedBy("this")
  protected final LinkedList<CharacterInputEvent> _upcomingCharacterEvents = new LinkedList<>();

  @GuardedBy("this")
  protected CharacterIterator _currentCharacterIterator = null;

  @SuppressWarnings("unused")
  private GLFWCharCallback _charCallback;

  private final GLFWCanvas _canvas;

  public GLFWCharacterInputWrapper(final GLFWCanvas canvas) {
    _canvas = canvas;
  }

  @Override
  public void init() {
    GLFW.glfwSetCharCallback(_canvas.getWindowId(), (_charCallback = new GLFWCharCallback() {

      @Override
      public void invoke(final long window, final int codepoint) {
        _upcomingCharacterEvents.add(new CharacterInputEvent((char) codepoint));
      }
    }));
  }

  @Override
  public synchronized PeekingIterator<CharacterInputEvent> getCharacterEvents() {
    if (_currentCharacterIterator == null || !_currentCharacterIterator.hasNext()) {
      _currentCharacterIterator = new CharacterIterator();
    }

    return _currentCharacterIterator;
  }

  private class CharacterIterator extends AbstractIterator<CharacterInputEvent>
      implements PeekingIterator<CharacterInputEvent> {
    @Override
    protected CharacterInputEvent computeNext() {
      synchronized (GLFWCharacterInputWrapper.this) {
        if (_upcomingCharacterEvents.isEmpty()) {
          return endOfData();
        }

        return _upcomingCharacterEvents.poll();
      }
    }
  }

}
