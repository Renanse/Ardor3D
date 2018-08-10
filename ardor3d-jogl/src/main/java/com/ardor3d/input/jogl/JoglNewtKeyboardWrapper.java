/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input.jogl;

import java.util.EnumSet;
import java.util.LinkedList;

import com.ardor3d.annotation.GuardedBy;
import com.ardor3d.framework.jogl.NewtWindowContainer;
import com.ardor3d.input.Key;
import com.ardor3d.input.KeyEvent;
import com.ardor3d.input.KeyState;
import com.ardor3d.input.KeyboardWrapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.PeekingIterator;
import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.NEWTEvent;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;

public class JoglNewtKeyboardWrapper extends KeyAdapter implements KeyboardWrapper {

    @GuardedBy("this")
    protected final LinkedList<KeyEvent> _upcomingEvents = new LinkedList<KeyEvent>();

    @GuardedBy("this")
    protected JoglNewtKeyboardIterator _currentIterator = null;

    protected final GLWindow _newtWindow;

    protected boolean _consumeEvents = false;

    protected boolean _skipAutoRepeatEvents = false;

    protected final EnumSet<Key> _pressedList = EnumSet.noneOf(Key.class);

    public JoglNewtKeyboardWrapper(final NewtWindowContainer newtWindowContainer) {
        super();
        _newtWindow = Preconditions.checkNotNull(newtWindowContainer.getNewtWindow(), "newtWindow");
    }

    @Override
    public void init() {
        _newtWindow.addKeyListener(this);
        _newtWindow.addWindowListener(new WindowAdapter() {
            @Override
            public void windowLostFocus(final WindowEvent e) {}

            @Override
            public void windowGainedFocus(final WindowEvent e) {
                _pressedList.clear();
            }
        });
    }

    @Override
    public synchronized PeekingIterator<KeyEvent> getEvents() {
        if (_currentIterator == null || !_currentIterator.hasNext()) {
            _currentIterator = new JoglNewtKeyboardIterator();
        }

        return _currentIterator;
    }

    @Override
    public synchronized void keyPressed(final com.jogamp.newt.event.KeyEvent e) {
        if (!_skipAutoRepeatEvents || !e.isAutoRepeat()) {
            final Key pressed = fromKeyEventToKey(e);
            if (!_pressedList.contains(pressed)) {
                _upcomingEvents.add(new KeyEvent(pressed, KeyState.DOWN, e.getKeyChar()));
                _pressedList.add(pressed);
            }
            if (_consumeEvents) {
                e.setAttachment(NEWTEvent.consumedTag);
                // ignore this event
            }
        }
    }

    @Override
    public synchronized void keyReleased(final com.jogamp.newt.event.KeyEvent e) {
        if (!_skipAutoRepeatEvents || !e.isAutoRepeat()) {
            final Key released = fromKeyEventToKey(e);
            _upcomingEvents.add(new KeyEvent(released, KeyState.UP, e.getKeyChar()));
            _pressedList.remove(released);
            if (_consumeEvents) {
                e.setAttachment(NEWTEvent.consumedTag);
                // ignore this event
            }
        }
    }

    /**
     * Convert from NEWT key event to Ardor3D Key. Override to provide additional or custom behavior.
     * 
     * @param e
     *            the NEWT KeyEvent received by the input system.
     * @return an Ardor3D Key, to be forwarded to the Predicate/Trigger system.
     */
    public synchronized Key fromKeyEventToKey(final com.jogamp.newt.event.KeyEvent e) {
        return JoglNewtKey.findByCode(e.getKeySymbol());
    }

    private class JoglNewtKeyboardIterator extends AbstractIterator<KeyEvent> implements PeekingIterator<KeyEvent> {
        @Override
        protected KeyEvent computeNext() {
            synchronized (JoglNewtKeyboardWrapper.this) {
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

    public boolean isSkipAutoRepeatEvents() {
        return _skipAutoRepeatEvents;
    }

    public void setSkipAutoRepeatEvents(final boolean skipAutoRepeatEvents) {
        _skipAutoRepeatEvents = skipAutoRepeatEvents;
    }
}
