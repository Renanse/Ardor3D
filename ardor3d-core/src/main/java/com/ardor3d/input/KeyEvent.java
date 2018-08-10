/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input;

import com.ardor3d.annotation.Immutable;

/**
 * Describes the state of a key - either it has been pressed or it has been released. Also keeps track of which
 * character the event corresponds to - the difference between a key and a character is that a key corresponds to a
 * physical key on the keyboard, whereas the character is the character that a keypress combination represents. Keys are
 * universal and mapped into the Key enum, whereas characters can be any char value. Some examples of the differences:
 * <ul>
 * <li>On almost any keyboard, pressing {@link Key#EIGHT} results in the character '8'.</li>
 * <li>On an English keyboard, pressing {@link Key#EIGHT} when the {@link Key#LSHIFT} is down leads to the character
 * '*'.</li>
 * <li>On a Swedish keyboard, pressing {@link Key#EIGHT} when the {@link Key#LSHIFT} is down leads to the character '('.
 * </li>
 * </ul>
 */
@Immutable
public class KeyEvent {
    public static final KeyEvent NOTHING = new KeyEvent(Key.UNKNOWN, KeyState.UP, (char) 0);

    private final Key _key;
    private final KeyState _state;
    private final char _keyChar;

    public KeyEvent(final Key key, final KeyState state, final char keyChar) {
        _key = key;
        _state = state;
        _keyChar = keyChar;
    }

    public Key getKey() {
        return _key;
    }

    public KeyState getState() {
        return _state;
    }

    public char getKeyChar() {
        return _keyChar;
    }

    @Override
    public String toString() {
        return "KeyEvent{" + "_key=" + _key + ", _state=" + _state + ", _keyChar=" + _keyChar + '}';
    }
}
