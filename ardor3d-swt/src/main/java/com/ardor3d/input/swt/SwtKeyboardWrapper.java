/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.input.swt;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.LinkedList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.widgets.Control;

import com.ardor3d.annotation.GuardedBy;
import com.ardor3d.annotation.ThreadSafe;
import com.ardor3d.input.character.CharacterInputEvent;
import com.ardor3d.input.character.CharacterInputWrapper;
import com.ardor3d.input.keyboard.Key;
import com.ardor3d.input.keyboard.KeyEvent;
import com.ardor3d.input.keyboard.KeyState;
import com.ardor3d.input.keyboard.KeyboardWrapper;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.PeekingIterator;

/**
 * Keyboard wrapper for SWT input.
 */
@ThreadSafe
public class SwtKeyboardWrapper implements KeyboardWrapper, KeyListener, CharacterInputWrapper {
    @GuardedBy("this")
    private final LinkedList<KeyEvent> _upcomingKeyEvents = new LinkedList<>();

    @GuardedBy("this")
    protected final LinkedList<CharacterInputEvent> _upcomingCharacterEvents = new LinkedList<>();

    private final Control _control;

    @GuardedBy("this")
    private SwtKeyboardIterator _currentKeyIterator = null;

    @GuardedBy("this")
    protected SwtCharacterIterator _currentCharacterIterator = null;

    @GuardedBy("this")
    private Key _lastKeyPressed = null;

    public SwtKeyboardWrapper(final Control control) {
        _control = checkNotNull(control, "control");
    }

    public void init() {
        _control.addKeyListener(this);
    }

    public synchronized PeekingIterator<KeyEvent> getKeyEvents() {
        if (_currentKeyIterator == null || !_currentKeyIterator.hasNext()) {
            _currentKeyIterator = new SwtKeyboardIterator();
        }

        return _currentKeyIterator;
    }

    @Override
    public synchronized PeekingIterator<CharacterInputEvent> getCharacterEvents() {
        if (_currentCharacterIterator == null || !_currentCharacterIterator.hasNext()) {
            _currentCharacterIterator = new SwtCharacterIterator();
        }

        return _currentCharacterIterator;
    }

    public synchronized void keyPressed(final org.eclipse.swt.events.KeyEvent event) {
        final Key key = fromKeyEventToKey(event);
        if (key == _lastKeyPressed) {
            // ignore if this is a repeat event
            return;
        }

        if ((event.stateMask & SWT.ALT) == 0 && event.character > 32) {
            _upcomingCharacterEvents.add(new CharacterInputEvent(event.character));
        }

        if (_lastKeyPressed != null) {
            // if this is a different key to the last key that was pressed, then
            // add an 'up' even for the previous one - SWT doesn't send an 'up' event for the
            // first key in the below scenario:
            // 1. key 1 down
            // 2. key 2 down
            // 3. key 1 up
            _upcomingKeyEvents.add(new KeyEvent(_lastKeyPressed, KeyState.UP));
        }

        _lastKeyPressed = key;
        _upcomingKeyEvents.add(new KeyEvent(key, KeyState.DOWN));
    }

    public synchronized void keyReleased(final org.eclipse.swt.events.KeyEvent event) {
        _upcomingKeyEvents.add(new KeyEvent(fromKeyEventToKey(event), KeyState.UP));
        _lastKeyPressed = null;
    }

    /**
     * Convert from SWT key event to Ardor3D Key. Override to provide additional or custom behavior.
     *
     * @param e
     *            the SWT KeyEvent received by the input system.
     * @return an Ardor3D Key, to be forwarded to the Predicate/Trigger system.
     */
    public synchronized Key fromKeyEventToKey(final org.eclipse.swt.events.KeyEvent e) {
        return SwtKey.findByCode(e.keyCode);
    }

    private class SwtKeyboardIterator extends AbstractIterator<KeyEvent> implements PeekingIterator<KeyEvent> {

        @Override
        protected KeyEvent computeNext() {
            synchronized (SwtKeyboardWrapper.this) {
                if (_upcomingKeyEvents.isEmpty()) {
                    return endOfData();
                }

                return _upcomingKeyEvents.poll();
            }
        }
    }

    private class SwtCharacterIterator extends AbstractIterator<CharacterInputEvent>
            implements PeekingIterator<CharacterInputEvent> {

        @Override
        protected CharacterInputEvent computeNext() {
            synchronized (SwtKeyboardWrapper.this) {
                if (_upcomingCharacterEvents.isEmpty()) {
                    return endOfData();
                }

                return _upcomingCharacterEvents.poll();
            }
        }
    }
}
