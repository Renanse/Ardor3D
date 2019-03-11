/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input.swt;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedList;

import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.internal.DPIUtil;
import org.eclipse.swt.widgets.Control;

import com.ardor3d.annotation.GuardedBy;
import com.ardor3d.annotation.ThreadSafe;
import com.ardor3d.framework.swt.SwtConstants;
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
 * A mouse wrapper for use with SWT.
 */
@ThreadSafe
public class SwtMouseWrapper implements MouseWrapper, MouseListener, MouseMoveListener, MouseWheelListener {
    @GuardedBy("this")
    private final LinkedList<MouseState> _upcomingEvents = new LinkedList<MouseState>();

    private final Control _control;
    private boolean _ignoreInput;

    @GuardedBy("this")
    private SwtMouseIterator _currentIterator = null;

    @GuardedBy("this")
    private MouseState _lastState = null;

    private final Multiset<MouseButton> _clicks = EnumMultiset.create(MouseButton.class);
    private final EnumMap<MouseButton, Long> _lastClickTime = Maps.newEnumMap(MouseButton.class);
    private final EnumSet<MouseButton> _clickArmed = EnumSet.noneOf(MouseButton.class);

    public SwtMouseWrapper(final Control control) {
        _control = checkNotNull(control, "control");
        for (final MouseButton mb : MouseButton.values()) {
            _lastClickTime.put(mb, 0L);
        }
    }

    public void init() {
        _control.addMouseListener(this);
        _control.addMouseMoveListener(this);
        _control.addMouseWheelListener(this);
    }

    public synchronized PeekingIterator<MouseState> getEvents() {
        expireClickEvents();

        if (_currentIterator == null || !_currentIterator.hasNext()) {
            _currentIterator = new SwtMouseIterator();
        }

        return _currentIterator;
    }

    private void expireClickEvents() {
        if (!_clicks.isEmpty()) {
            for (final MouseButton mb : MouseButton.values()) {
                if (System.currentTimeMillis() - _lastClickTime.get(mb) > MouseState.CLICK_TIME_MS) {
                    _clicks.setCount(mb, 0);
                }
            }
        }
    }

    public synchronized void mouseDoubleClick(final MouseEvent mouseEvent) {
        // ignoring this. We'll handle (multi)click in a uniform way
    }

    public synchronized void mouseDown(final MouseEvent e) {
        if (_ignoreInput) {
            return;
        }

        final MouseButton b = getButtonForEvent(e);
        if (_clickArmed.contains(b)) {
            _clicks.setCount(b, 0);
        }
        _clickArmed.add(b);
        _lastClickTime.put(b, System.currentTimeMillis());

        initState(e);

        final EnumMap<MouseButton, ButtonState> buttons = _lastState.getButtonStates();

        setStateForButton(e, buttons, ButtonState.DOWN);

        addNewState(e, 0, buttons, null);
    }

    public synchronized void mouseUp(final MouseEvent e) {
        if (_ignoreInput) {
            return;
        }

        initState(e);

        final EnumMap<MouseButton, ButtonState> buttons = _lastState.getButtonStates();

        setStateForButton(e, buttons, ButtonState.UP);

        final MouseButton b = getButtonForEvent(e);
        if (_clickArmed.contains(b)
                && (System.currentTimeMillis() - _lastClickTime.get(b) <= MouseState.CLICK_TIME_MS)) {
            _clicks.add(b); // increment count of clicks for button b.
            // XXX: Note the double event add... this prevents sticky click counts, but is it the best way?
            addNewState(e, 0, buttons, EnumMultiset.create(_clicks));
        } else {
            _clicks.setCount(b, 0); // clear click count for button b.
        }
        _clickArmed.remove(b);

        addNewState(e, 0, buttons, null);
    }

    private int getDX(final MouseEvent e) {
        return getArdor3DX(e) - _lastState.getX();
    }

    private int getDY(final MouseEvent e) {
        return getArdor3DY(e) - _lastState.getY();
    }

    /**
     * Scale (for HighDPI, if needed) the X coordinate of the event.
     *
     * @param e
     *            our mouseEvent
     * @return the scaled X coordinate of the event.
     */
    private int getArdor3DX(final MouseEvent e) {
        return Math.round(DPIUtil.autoScaleUp(e.x) * SwtConstants.PlatformDPIScale);
    }

    /**
     * Scale (for HighDPI) and flip the Y coordinate of the event.
     *
     * @param e
     *            our mouseEvent
     * @return the scaled Y coordinate of the event, flipped relative to the component since we expect an origin in the
     *         lower left corner.
     */
    private int getArdor3DY(final MouseEvent e) {
        return Math.round(DPIUtil.autoScaleUp(_control.getSize().y - e.y) * SwtConstants.PlatformDPIScale);
    }

    private void setStateForButton(final MouseEvent e, final EnumMap<MouseButton, ButtonState> buttons,
            final ButtonState buttonState) {
        final MouseButton button = getButtonForEvent(e);
        buttons.put(button, buttonState);
    }

    private MouseButton getButtonForEvent(final MouseEvent e) {
        MouseButton button;
        switch (e.button) { // ordering is different than swt
            case 1:
                button = MouseButton.LEFT;
                break;
            case 3:
                button = MouseButton.RIGHT;
                break;
            case 2:
                button = MouseButton.MIDDLE;
                break;
            default:
                throw new RuntimeException("unknown button: " + e.button);
        }
        return button;
    }

    public synchronized void mouseMove(final MouseEvent mouseEvent) {
        if (_ignoreInput) {
            return;
        }

        _clickArmed.clear();
        _clicks.clear();

        // check that we have a valid _lastState
        initState(mouseEvent);

        addNewState(mouseEvent, 0, _lastState.getButtonStates(), null);
    }

    public synchronized void mouseScrolled(final MouseEvent mouseEvent) {
        if (_ignoreInput) {
            return;
        }

        initState(mouseEvent);

        addNewState(mouseEvent, mouseEvent.count, _lastState.getButtonStates(), null);
    }

    private void initState(final MouseEvent mouseEvent) {
        if (_lastState == null) {
            _lastState = new MouseState(getArdor3DX(mouseEvent), getArdor3DY(mouseEvent), 0, 0, 0, null, null);
        }
    }

    private void addNewState(final MouseEvent mouseEvent, final int wheelDX,
            final EnumMap<MouseButton, ButtonState> buttons, final Multiset<MouseButton> clicks) {
        final MouseState newState = new MouseState(getArdor3DX(mouseEvent), getArdor3DY(mouseEvent), getDX(mouseEvent),
                getDY(mouseEvent), wheelDX, buttons, clicks);

        _upcomingEvents.add(newState);
        _lastState = newState;
    }

    private class SwtMouseIterator extends AbstractIterator<MouseState> implements PeekingIterator<MouseState> {
        @Override
        protected MouseState computeNext() {
            synchronized (SwtMouseWrapper.this) {
                if (_upcomingEvents.isEmpty()) {
                    return endOfData();
                }

                return _upcomingEvents.poll();
            }
        }
    }

    @Override
    public void setIgnoreInput(final boolean ignore) {
        _ignoreInput = ignore;
    }

    @Override
    public boolean isIgnoreInput() {
        return _ignoreInput;
    }
}
