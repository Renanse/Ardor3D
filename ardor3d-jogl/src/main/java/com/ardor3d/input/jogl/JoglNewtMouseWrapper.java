/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input.jogl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedList;

import com.ardor3d.annotation.GuardedBy;
import com.ardor3d.framework.jogl.NewtWindowContainer;
import com.ardor3d.input.ButtonState;
import com.ardor3d.input.GrabbedState;
import com.ardor3d.input.MouseButton;
import com.ardor3d.input.MouseManager;
import com.ardor3d.input.MouseState;
import com.ardor3d.input.MouseWrapper;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.EnumMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.PeekingIterator;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.newt.event.NEWTEvent;
import com.jogamp.newt.opengl.GLWindow;

public class JoglNewtMouseWrapper implements MouseWrapper, MouseListener {

    @GuardedBy("this")
    protected final LinkedList<MouseState> _upcomingEvents = Lists.newLinkedList();

    @GuardedBy("this")
    protected JoglNewtMouseIterator _currentIterator = null;

    @GuardedBy("this")
    protected MouseState _lastState = null;

    protected final GLWindow _newtWindow;

    protected final MouseManager _manager;

    protected boolean _consumeEvents = false;

    protected boolean _skipAutoRepeatEvents = false;

    protected final Multiset<MouseButton> _clicks = EnumMultiset.create(MouseButton.class);
    protected final EnumMap<MouseButton, Long> _lastClickTime = Maps.newEnumMap(MouseButton.class);
    protected final EnumSet<MouseButton> _clickArmed = EnumSet.noneOf(MouseButton.class);

    protected int _ignoreX = Integer.MAX_VALUE;
    protected int _ignoreY = Integer.MAX_VALUE;

    public JoglNewtMouseWrapper(final NewtWindowContainer newtWindowContainer, final MouseManager manager) {
        _newtWindow = checkNotNull(newtWindowContainer.getNewtWindow(), "newtWindow");
        _manager = manager;
        for (final MouseButton mb : MouseButton.values()) {
            _lastClickTime.put(mb, 0L);
        }
    }

    @Override
    public void init() {
        _newtWindow.addMouseListener(this);
    }

    @Override
    public synchronized PeekingIterator<MouseState> getEvents() {
        expireClickEvents();

        if (_currentIterator == null || !_currentIterator.hasNext()) {
            _currentIterator = new JoglNewtMouseIterator();
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

    @Override
    public synchronized void mousePressed(final MouseEvent me) {
        if (!_skipAutoRepeatEvents || !me.isAutoRepeat()) {
            final MouseButton b = getButtonForEvent(me);
            if (_clickArmed.contains(b)) {
                _clicks.setCount(b, 0);
            }
            _clickArmed.add(b);
            _lastClickTime.put(b, System.currentTimeMillis());

            initState(me);
            if (_consumeEvents) {
                me.setAttachment(NEWTEvent.consumedTag);
            }

            final EnumMap<MouseButton, ButtonState> buttons = _lastState.getButtonStates();

            setStateForButton(me, buttons, ButtonState.DOWN);

            addNewState(me, buttons, null);
        }
    }

    @Override
    public synchronized void mouseReleased(final MouseEvent me) {
        if (!_skipAutoRepeatEvents || !me.isAutoRepeat()) {
            initState(me);
            if (_consumeEvents) {
                me.setAttachment(NEWTEvent.consumedTag);
            }

            final EnumMap<MouseButton, ButtonState> buttons = _lastState.getButtonStates();

            setStateForButton(me, buttons, ButtonState.UP);

            final MouseButton b = getButtonForEvent(me);
            if (_clickArmed.contains(b)
                    && (System.currentTimeMillis() - _lastClickTime.get(b) <= MouseState.CLICK_TIME_MS)) {
                _clicks.add(b); // increment count of clicks for button b.
                // XXX: Note the double event add... this prevents sticky click counts, but is it the best way?
                addNewState(me, buttons, EnumMultiset.create(_clicks));
            } else {
                _clicks.setCount(b, 0); // clear click count for button b.
            }
            _clickArmed.remove(b);

            addNewState(me, buttons, null);
        }
    }

    @Override
    public synchronized void mouseDragged(final MouseEvent me) {
        mouseMoved(me);
    }

    @Override
    public synchronized void mouseMoved(final MouseEvent me) {
        _clickArmed.clear();
        _clicks.clear();

        // check that we have a valid _lastState
        initState(me);
        if (_consumeEvents) {
            me.setAttachment(NEWTEvent.consumedTag);
        }

        // remember our current ardor3d position
        final int oldX = _lastState.getX(), oldY = _lastState.getY();

        // check the state against the "ignore next" values
        if (_ignoreX != Integer.MAX_VALUE // shortcut to prevent dx/dy calculations
                && (_ignoreX == getDX(me) && _ignoreY == getDY(me))) {

            // we matched, so we'll consider this a "mouse pointer reset move"
            // so reset ignore to let the next move event through.
            _ignoreX = Integer.MAX_VALUE;
            _ignoreY = Integer.MAX_VALUE;

            // exit without adding an event to our queue
            return;
        }

        // save our old "last state."
        final MouseState _savedState = _lastState;

        // Add our latest state info to the queue
        addNewState(me, _lastState.getButtonStates(), null);

        // If we have a valid move... should always be the case, but occasionally something slips through.
        if (_lastState.getDx() != 0 || _lastState.getDy() != 0) {

            // Ask our manager if we're currently "captured"
            if (_manager.getGrabbed() == GrabbedState.GRABBED) {

                // if so, set "ignore next" to the inverse of this move
                _ignoreX = -_lastState.getDx();
                _ignoreY = -_lastState.getDy();

                // Move us back to our last position.
                _manager.setPosition(oldX, oldY);

                // And finally, revert our _lastState.
                _lastState = _savedState;
            } else {
                // otherwise, set us to not ignore anything. This may be unnecessary, but prevents any possible
                // "ignore" bleeding.
                _ignoreX = Integer.MAX_VALUE;
                _ignoreY = Integer.MAX_VALUE;
            }
        }
    }

    @Override
    public void mouseWheelMoved(final MouseEvent me) {
        initState(me);

        addNewState(me, _lastState.getButtonStates(), null);
        if (_consumeEvents) {
            me.setAttachment(NEWTEvent.consumedTag);
        }
    }

    private void initState(final MouseEvent mouseEvent) {
        if (_lastState == null) {
            _lastState = new MouseState(mouseEvent.getX(), getArdor3DY(mouseEvent), 0, 0, 0, null, null);
        }
    }

    private void addNewState(final MouseEvent mouseEvent, final EnumMap<MouseButton, ButtonState> enumMap,
            final Multiset<MouseButton> clicks) {
        final MouseState newState = new MouseState(mouseEvent.getX(), getArdor3DY(mouseEvent), getDX(mouseEvent),
                getDY(mouseEvent), (int) (mouseEvent.isShiftDown() ? mouseEvent.getRotation()[0]
                        : mouseEvent.getRotation()[1]), enumMap, clicks);

        synchronized (JoglNewtMouseWrapper.this) {
            _upcomingEvents.add(newState);
        }
        _lastState = newState;
    }

    private int getDX(final MouseEvent me) {
        return me.getX() - _lastState.getX();
    }

    private int getDY(final MouseEvent me) {
        return getArdor3DY(me) - _lastState.getY();
    }

    /**
     * @param e
     *            our mouseEvent
     * @return the Y coordinate of the event, flipped relative to the component since we expect an origin in the lower
     *         left corner.
     */
    private int getArdor3DY(final MouseEvent me) {
        return _newtWindow.getHeight() - me.getY();
    }

    private void setStateForButton(final MouseEvent e, final EnumMap<MouseButton, ButtonState> buttons,
            final ButtonState buttonState) {
        final MouseButton button = getButtonForEvent(e);
        buttons.put(button, buttonState);
    }

    private MouseButton getButtonForEvent(final MouseEvent me) {
        MouseButton button;
        switch (me.getButton()) {
            case MouseEvent.BUTTON1:
                button = MouseButton.LEFT;
                break;
            case MouseEvent.BUTTON2:
                button = MouseButton.MIDDLE;
                break;
            case MouseEvent.BUTTON3:
                button = MouseButton.RIGHT;
                break;
            case MouseEvent.BUTTON4:
                button = MouseButton.FOUR;
                break;
            case MouseEvent.BUTTON5:
                button = MouseButton.FIVE;
                break;
            case MouseEvent.BUTTON6:
                button = MouseButton.SIX;
                break;
            case MouseEvent.BUTTON7:
                button = MouseButton.SEVEN;
                break;
            case MouseEvent.BUTTON8:
                button = MouseButton.EIGHT;
                break;
            case MouseEvent.BUTTON9:
                button = MouseButton.NINE;
                break;
            default:
                throw new RuntimeException("unknown button: " + me.getButton());
        }
        return button;
    }

    private class JoglNewtMouseIterator extends AbstractIterator<MouseState> implements PeekingIterator<MouseState> {

        @Override
        protected MouseState computeNext() {
            synchronized (JoglNewtMouseWrapper.this) {
                if (_upcomingEvents.isEmpty()) {
                    return endOfData();
                }
                return _upcomingEvents.poll();
            }
        }
    }

    @Override
    public synchronized void mouseClicked(final MouseEvent me) {
        // Yes, we could use the click count here, but in the interests of this working the same way as SWT and Native,
        // we
        // will do it the same way they do it.

    }

    @Override
    public synchronized void mouseEntered(final MouseEvent me) {
        // ignore this
    }

    @Override
    public synchronized void mouseExited(final MouseEvent me) {
        // ignore this
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
