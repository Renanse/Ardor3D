/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input.lwjgl;

import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;

import com.ardor3d.input.Key;
import com.ardor3d.input.KeyEvent;
import com.ardor3d.input.KeyState;
import com.ardor3d.input.KeyboardWrapper;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.PeekingIterator;

/**
 * Wraps the {@link org.lwjgl.input.Keyboard} class.
 */
public class LwjglKeyboardWrapper implements KeyboardWrapper {
    private LwjglKeyEventIterator _currentIterator = null;

    public void init() {
        if (!Keyboard.isCreated()) {
            try {
                Keyboard.create();
            } catch (final LWJGLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public PeekingIterator<KeyEvent> getEvents() {
        if (_currentIterator == null || !_currentIterator.hasNext()) {
            _currentIterator = new LwjglKeyEventIterator();
        }

        return _currentIterator;
    }

    private static class LwjglKeyEventIterator extends AbstractIterator<KeyEvent> implements PeekingIterator<KeyEvent> {

        @Override
        protected KeyEvent computeNext() {
            if (!Keyboard.next()) {
                return endOfData();
            }

            final int keyCode = Keyboard.getEventKey();
            final boolean pressed = Keyboard.getEventKeyState();
            final char keyChar = Keyboard.getEventCharacter();

            final Key k = LwjglKey.findByCode(keyCode);

            return new KeyEvent(k, pressed ? KeyState.DOWN : KeyState.UP, keyChar);
        }
    }
}
