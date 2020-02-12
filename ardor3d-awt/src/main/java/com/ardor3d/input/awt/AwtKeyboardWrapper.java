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

import java.awt.Component;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyListener;
import java.util.EnumSet;
import java.util.LinkedList;

import com.ardor3d.annotation.GuardedBy;
import com.ardor3d.input.character.CharacterInputEvent;
import com.ardor3d.input.character.CharacterInputWrapper;
import com.ardor3d.input.keyboard.Key;
import com.ardor3d.input.keyboard.KeyEvent;
import com.ardor3d.input.keyboard.KeyState;
import com.ardor3d.input.keyboard.KeyboardWrapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.PeekingIterator;

/**
 * Keyboard wrapper class for use with AWT.
 */
public class AwtKeyboardWrapper implements KeyboardWrapper, KeyListener, CharacterInputWrapper {
    @GuardedBy("this")
    protected final LinkedList<KeyEvent> _upcomingKeyEvents = new LinkedList<>();

    @GuardedBy("this")
    protected final LinkedList<CharacterInputEvent> _upcomingCharacterEvents = new LinkedList<>();

    @GuardedBy("this")
    protected AwtKeyboardIterator _currentKeyIterator = null;

    @GuardedBy("this")
    protected AwtCharacterIterator _currentCharacterIterator = null;

    protected final Component _component;

    protected boolean _consumeEvents = false;

    protected final EnumSet<Key> _pressedKeyList = EnumSet.noneOf(Key.class);

    public AwtKeyboardWrapper(final Component component) {
        _component = Preconditions.checkNotNull(component, "component");
    }

    public void init() {
        _component.addKeyListener(this);
        _component.addFocusListener(new FocusListener() {
            public void focusLost(final FocusEvent e) {
            }

            public void focusGained(final FocusEvent e) {
                _pressedKeyList.clear();
            }
        });
    }

    public synchronized PeekingIterator<KeyEvent> getKeyEvents() {
        if (_currentKeyIterator == null || !_currentKeyIterator.hasNext()) {
            _currentKeyIterator = new AwtKeyboardIterator();
        }

        return _currentKeyIterator;
    }

    @Override
    public synchronized PeekingIterator<CharacterInputEvent> getCharacterEvents() {
        if (_currentCharacterIterator == null || !_currentCharacterIterator.hasNext()) {
            _currentCharacterIterator = new AwtCharacterIterator();
        }

        return _currentCharacterIterator;
    }

    public synchronized void keyTyped(final java.awt.event.KeyEvent e) {
        if (_consumeEvents) {
            e.consume();
            // ignore this event
        }
    }

    public synchronized void keyPressed(final java.awt.event.KeyEvent e) {
        _upcomingCharacterEvents.add(new CharacterInputEvent(e.getKeyChar()));

        final Key pressed = fromKeyEventToKey(e);
        if (!_pressedKeyList.contains(pressed)) {
            _upcomingKeyEvents.add(new KeyEvent(pressed, KeyState.DOWN));
            _pressedKeyList.add(pressed);
        }
        if (_consumeEvents) {
            e.consume();
        }
    }

    public synchronized void keyReleased(final java.awt.event.KeyEvent e) {
        final Key released = fromKeyEventToKey(e);
        _upcomingKeyEvents.add(new KeyEvent(released, KeyState.UP));
        _pressedKeyList.remove(released);
        if (_consumeEvents) {
            e.consume();
        }
    }

    /**
     * Convert from AWT key event to Ardor3D Key. Override to provide additional or custom behavior.
     *
     * @param e
     *            the AWT KeyEvent received by the input system.
     * @return an Ardor3D Key, to be forwarded to the Predicate/Trigger system.
     */
    public synchronized Key fromKeyEventToKey(final java.awt.event.KeyEvent e) {
        return AwtKey.findByCode(e.getKeyCode());
    }

    private class AwtKeyboardIterator extends AbstractIterator<KeyEvent> implements PeekingIterator<KeyEvent> {
        @Override
        protected KeyEvent computeNext() {
            synchronized (AwtKeyboardWrapper.this) {
                if (_upcomingKeyEvents.isEmpty()) {
                    return endOfData();
                }

                return _upcomingKeyEvents.poll();
            }
        }
    }

    private class AwtCharacterIterator extends AbstractIterator<CharacterInputEvent>
            implements PeekingIterator<CharacterInputEvent> {
        @Override
        protected CharacterInputEvent computeNext() {
            synchronized (AwtKeyboardWrapper.this) {
                if (_upcomingCharacterEvents.isEmpty()) {
                    return endOfData();
                }

                return _upcomingCharacterEvents.poll();
            }
        }
    }

    public boolean isConsumeEvents() {
        return _consumeEvents;
    }

    public void setConsumeEvents(final boolean consumeEvents) {
        _consumeEvents = consumeEvents;
    }
}
