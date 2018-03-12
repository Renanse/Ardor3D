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

import java.util.EnumMap;
import java.util.EnumSet;

import org.lwjgl.input.Mouse;

import com.ardor3d.input.ButtonState;
import com.ardor3d.input.MouseButton;
import com.ardor3d.input.MouseState;
import com.ardor3d.input.MouseWrapper;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.EnumMultiset;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.PeekingIterator;

/**
 * Wrapper over the {@link org.lwjgl.input.Mouse} mouse interface class.
 */
public class LwjglMouseWrapper implements MouseWrapper {
    private LwjglMouseIterator _currentIterator = null;

    private static final Multiset<MouseButton> _clicks = EnumMultiset.create(MouseButton.class);
    private static final EnumMap<MouseButton, Long> _lastClickTime = Maps.newEnumMap(MouseButton.class);
    private static final EnumSet<MouseButton> _clickArmed = EnumSet.noneOf(MouseButton.class);

    private static boolean _sendClickState = false;
    private static boolean _ignoreInput;
    private static MouseState _nextState;

    public LwjglMouseWrapper() {
        for (final MouseButton mb : MouseButton.values()) {
            _lastClickTime.put(mb, 0L);
        }
    }

    public void init() {
        if (!Mouse.isCreated()) {
            try {
                Mouse.create();
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public PeekingIterator<MouseState> getEvents() {
        // only create a new iterator if there isn't an existing, valid, one.
        if (_currentIterator == null || !_currentIterator.hasNext()) {
            _currentIterator = new LwjglMouseIterator();
        }

        return _currentIterator;
    }

    @Override
    public void setIgnoreInput(final boolean ignore) {
        _ignoreInput = ignore;
    }

    @Override
    public boolean isIgnoreInput() {
        return _ignoreInput;
    }

    private static class LwjglMouseIterator extends AbstractIterator<MouseState>
            implements PeekingIterator<MouseState> {

        public LwjglMouseIterator() {}

        @Override
        protected MouseState computeNext() {
            if (_ignoreInput) {
                while (Mouse.next()) {
                    ;
                }
                return endOfData();
            }

            if (_nextState != null) {
                final MouseState rVal = _nextState;
                _nextState = null;
                return rVal;
            }

            if (!Mouse.next()) {
                return endOfData();
            }

            final EnumMap<MouseButton, ButtonState> buttons = Maps.newEnumMap(MouseButton.class);

            if (Mouse.getButtonCount() > 0) {
                final boolean down = Mouse.isButtonDown(0);
                processButtonForClick(MouseButton.LEFT, down);
                buttons.put(MouseButton.LEFT, down ? ButtonState.DOWN : ButtonState.UP);
            }
            if (Mouse.getButtonCount() > 1) {
                final boolean down = Mouse.isButtonDown(1);
                processButtonForClick(MouseButton.RIGHT, down);
                buttons.put(MouseButton.RIGHT, down ? ButtonState.DOWN : ButtonState.UP);
            }
            if (Mouse.getButtonCount() > 2) {
                final boolean down = Mouse.isButtonDown(2);
                processButtonForClick(MouseButton.MIDDLE, down);
                buttons.put(MouseButton.MIDDLE, down ? ButtonState.DOWN : ButtonState.UP);
            }

            final MouseState nextState = new MouseState(Mouse.getEventX(), Mouse.getEventY(), Mouse.getEventDX(),
                    Mouse.getEventDY(), Mouse.getEventDWheel(), buttons, null);

            if (nextState.getDx() != 0.0 || nextState.getDy() != 0.0) {
                _clickArmed.clear();
                _clicks.clear();
                _sendClickState = false;
            }

            if (_sendClickState) {
                _nextState = nextState;
                _sendClickState = false;
                return new MouseState(nextState.getX(), nextState.getY(), nextState.getDx(), nextState.getDy(),
                        nextState.getDwheel(), buttons, EnumMultiset.create(_clicks));
            } else {
                return nextState;
            }
        }

        private void processButtonForClick(final MouseButton b, final boolean down) {
            boolean expired = false;
            if (System.currentTimeMillis() - _lastClickTime.get(b) > MouseState.CLICK_TIME_MS) {
                _clicks.setCount(b, 0);
                expired = true;
            }
            if (down) {
                if (_clickArmed.contains(b)) {
                    _clicks.setCount(b, 0);
                }
                _clickArmed.add(b);
                _lastClickTime.put(b, System.currentTimeMillis());
            } else {
                if (!expired && _clickArmed.contains(b)) {
                    _clicks.add(b); // increment count of clicks for button b.
                    // XXX: Note the double event add... this prevents sticky click counts, but is it the best way?
                    _sendClickState = true;
                } else {
                    _clicks.setCount(b, 0); // clear click count for button b.
                }
                _clickArmed.remove(b);
            }
        }

    }
}
