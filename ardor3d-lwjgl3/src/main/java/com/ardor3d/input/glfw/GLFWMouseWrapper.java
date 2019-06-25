/**
 * Copyright (c) 2008-2018 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input.glfw;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedList;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;

import com.ardor3d.annotation.GuardedBy;
import com.ardor3d.framework.lwjgl3.GLFWCanvas;
import com.ardor3d.input.ButtonState;
import com.ardor3d.input.MouseButton;
import com.ardor3d.input.MouseState;
import com.ardor3d.input.MouseWrapper;
import com.ardor3d.math.MathUtils;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.EnumMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.PeekingIterator;

public class GLFWMouseWrapper implements MouseWrapper {

    @GuardedBy("this")
    protected final LinkedList<MouseState> _upcomingEvents = new LinkedList<MouseState>();

    @SuppressWarnings("unused")
    private GLFWMouseButtonCallback _mouseButtonCallback;

    @SuppressWarnings("unused")
    private GLFWCursorPosCallback _cursorPosCallback;

    @SuppressWarnings("unused")
    private GLFWScrollCallback _scrollCallback;

    private MouseIterator _currentIterator = null;

    private static final Multiset<MouseButton> _clicks = EnumMultiset.create(MouseButton.class);
    private static final EnumMap<MouseButton, Long> _lastClickTime = new EnumMap<>(MouseButton.class);
    private static final EnumSet<MouseButton> _clickArmed = EnumSet.noneOf(MouseButton.class);

    private final EnumMap<MouseButton, ButtonState> _lastButtonState = new EnumMap<>(MouseButton.class);

    private static boolean _sendClickState = false;
    private static boolean _ignoreInput;
    private MouseState _lastState;

    private final GLFWCanvas _canvas;

    public GLFWMouseWrapper(final GLFWCanvas canvas) {
        _canvas = canvas;

        // fill our button state map with undefined
        Arrays.asList(MouseButton.values()).forEach((final MouseButton b) -> {
            _lastButtonState.put(b, ButtonState.UNDEFINED);
            _lastClickTime.put(b, 0L);
        });
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

                final boolean down = action == GLFW.GLFW_PRESS;
                final ButtonState state = down ? ButtonState.DOWN : ButtonState.UP;

                // check for clicks
                processButtonForClick(mb, down);

                // save our state
                _lastButtonState.put(mb, state);

                // Add our new state
                final int x = _lastState != null ? _lastState.getX() : 0;
                final int y = _lastState != null ? _lastState.getY() : 0;
                addNextState(new MouseState(x, y, 0, 0, 0, new EnumMap<>(_lastButtonState),
                        _sendClickState && !_clicks.isEmpty() ? EnumMultiset.create(_clicks) : null));
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

                if (dx != 0.0 || dy != 0.0) {
                    _clickArmed.clear();
                    _clicks.clear();
                    _sendClickState = false;
                }

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

    protected void addNextState(final MouseState nextState) {
        _upcomingEvents.add(nextState);
        _lastState = nextState;
    }

    protected static MouseButton getButtonByIndex(final int glfwButtonIndex) {
        switch (glfwButtonIndex) {
            case GLFW.GLFW_MOUSE_BUTTON_1:
                return MouseButton.LEFT;
            case GLFW.GLFW_MOUSE_BUTTON_2:
                return MouseButton.RIGHT;
            case GLFW.GLFW_MOUSE_BUTTON_3:
                return MouseButton.MIDDLE;
            case GLFW.GLFW_MOUSE_BUTTON_4:
                return MouseButton.FOUR;
            case GLFW.GLFW_MOUSE_BUTTON_5:
                return MouseButton.FIVE;
            case GLFW.GLFW_MOUSE_BUTTON_6:
                return MouseButton.SIX;
            case GLFW.GLFW_MOUSE_BUTTON_7:
                return MouseButton.SEVEN;
            case GLFW.GLFW_MOUSE_BUTTON_8:
                return MouseButton.EIGHT;
            default:
                return MouseButton.UNKNOWN;
        }
    }

    @Override
    public PeekingIterator<MouseState> getEvents() {
        // only create a new iterator if there isn't an existing, valid, one.
        if (_currentIterator == null || !_currentIterator.hasNext()) {
            _currentIterator = new MouseIterator();
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

    private class MouseIterator extends AbstractIterator<MouseState> implements PeekingIterator<MouseState> {

        public MouseIterator() {
        }

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
