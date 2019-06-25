/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input.awt;

import java.awt.Component;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyListener;
import java.util.EnumSet;
import java.util.LinkedList;

import com.ardor3d.annotation.GuardedBy;
import com.ardor3d.input.Key;
import com.ardor3d.input.KeyEvent;
import com.ardor3d.input.KeyState;
import com.ardor3d.input.KeyboardWrapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.PeekingIterator;

/**
 * Keyboard wrapper class for use with AWT.
 */
public class AwtKeyboardWrapper implements KeyboardWrapper, KeyListener {
    @GuardedBy("this")
    protected final LinkedList<KeyEvent> _upcomingEvents = new LinkedList<KeyEvent>();

    @GuardedBy("this")
    protected AwtKeyboardIterator _currentIterator = null;

    protected final Component _component;

    protected boolean _consumeEvents = false;

    protected final EnumSet<Key> _pressedList = EnumSet.noneOf(Key.class);

    public AwtKeyboardWrapper(final Component component) {
        _component = Preconditions.checkNotNull(component, "component");
    }

    public void init() {
        _component.addKeyListener(this);
        _component.addFocusListener(new FocusListener() {
            public void focusLost(final FocusEvent e) {
            }

            public void focusGained(final FocusEvent e) {
                _pressedList.clear();
            }
        });
    }

    public synchronized PeekingIterator<KeyEvent> getEvents() {
        if (_currentIterator == null || !_currentIterator.hasNext()) {
            _currentIterator = new AwtKeyboardIterator();
        }

        return _currentIterator;
    }

    public synchronized void keyTyped(final java.awt.event.KeyEvent e) {
        if (_consumeEvents) {
            e.consume();
            // ignore this event
        }
    }

    public synchronized void keyPressed(final java.awt.event.KeyEvent e) {
        // TODO: use key character
        final char keyChar = e.getKeyChar();

        final Key pressed = fromKeyEventToKey(e);
        if (!_pressedList.contains(pressed)) {
            _upcomingEvents.add(new KeyEvent(pressed, KeyState.DOWN));
            _pressedList.add(pressed);
        }
        if (_consumeEvents) {
            e.consume();
        }
    }

    public synchronized void keyReleased(final java.awt.event.KeyEvent e) {
        final Key released = fromKeyEventToKey(e);
        _upcomingEvents.add(new KeyEvent(released, KeyState.UP));
        _pressedList.remove(released);
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
                if (_upcomingEvents.isEmpty()) {
                    return endOfData();
                }

                return _upcomingEvents.poll();
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
