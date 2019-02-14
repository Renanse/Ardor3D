/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input.swt;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Event;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ardor3d.framework.swt.SwtCanvas;
import com.ardor3d.input.ButtonState;
import com.ardor3d.input.MouseButton;
import com.ardor3d.input.MouseState;

public class TestSwtMouseWrapper {
    SwtMouseWrapper wrapper;

    SwtCanvas control;

    MouseEvent e1, e2;

    Object[] mocks;

    @Before
    public void setup() throws Exception {
        control = createMock("control", SwtCanvas.class);

        wrapper = new SwtMouseWrapper(control);
        final Event event = new Event();
        event.widget = control;

        e1 = new MouseEvent(event);
        e2 = new MouseEvent(event);

        mocks = new Object[] { control };
    }

    @After
    public void verifyMocks() throws Exception {
        verify(mocks);
    }

    @Test
    public void testMove1() throws Exception {
        control.addMouseListener(wrapper);
        control.addMouseMoveListener(wrapper);
        control.addMouseWheelListener(wrapper);

        expect(control.getSize()).andReturn(new Point(100, 100)).atLeastOnce();

        replay(mocks);

        wrapper.init();

        setXY(e1, 2, 0);

        wrapper.mouseMove(e1);

        MouseState state = wrapper.getEvents().next();

        assertFalse("no more events", wrapper.getEvents().hasNext());

        assertEquals("x", 2, state.getX());
        assertEquals("dx", 0, state.getDx());
        assertEquals("y", 100, state.getY());
        assertEquals("dy", 0, state.getDy());

        for (final ButtonState bs : state.getButtonStates().values()) {
            assertEquals("up", ButtonState.UP, bs);
        }

        setXY(e1, 4, 0);

        wrapper.mouseMove(e1);

        state = wrapper.getEvents().next();

        assertFalse("no more events", wrapper.getEvents().hasNext());

        assertEquals("x", 4, state.getX());
        assertEquals("dx", 2, state.getDx());
        assertEquals("y", 100, state.getY());
        assertEquals("dy", 0, state.getDy());

        for (final ButtonState bs : state.getButtonStates().values()) {
            assertEquals("up", ButtonState.UP, bs);
        }

        setXY(e1, 2, 4);

        wrapper.mouseMove(e1);

        state = wrapper.getEvents().next();

        assertFalse("no more events", wrapper.getEvents().hasNext());

        assertEquals("x", 2, state.getX());
        assertEquals("dx", -2, state.getDx());
        assertEquals("y", 96, state.getY());
        assertEquals("dy", -4, state.getDy());

        for (final ButtonState bs : state.getButtonStates().values()) {
            assertEquals("up", ButtonState.UP, bs);
        }
    }

    @Test
    public void testButtons() throws Exception {
        control.addMouseListener(wrapper);
        control.addMouseMoveListener(wrapper);
        control.addMouseWheelListener(wrapper);

        expect(control.getSize()).andReturn(new Point(100, 100)).atLeastOnce();

        replay(mocks);

        wrapper.init();

        // mouse down, then drag
        setWithButtons(e1, 0, 0, 1);
        wrapper.mouseDown(e1);

        setXY(e2, 2, 4);
        wrapper.mouseMove(e2);

        final Iterator<MouseState> events = wrapper.getEvents();

        MouseState s1 = events.next();
        final MouseState s2 = events.next();

        assertFalse("more events", events.hasNext());
        assertFalse("more events", wrapper.getEvents().hasNext());

        assertEquals("x", 0, s1.getX());
        assertEquals("dx", 0, s1.getDx());
        assertEquals("y", 100, s1.getY());
        assertEquals("dy", 0, s1.getDy());
        assertTrue("left down", s1.getButtonState(MouseButton.LEFT) == ButtonState.DOWN);
        assertFalse("right down", s1.getButtonState(MouseButton.RIGHT) == ButtonState.DOWN);

        assertEquals("x", 2, s2.getX());
        assertEquals("dx", 2, s2.getDx());
        assertEquals("y", 96, s2.getY());
        assertEquals("dy", -4, s2.getDy());
        assertTrue("left down", s2.getButtonState(MouseButton.LEFT) == ButtonState.DOWN);
        assertFalse("right down", s2.getButtonState(MouseButton.RIGHT) == ButtonState.DOWN);

        // press right button
        setWithButtons(e1, 2, 4, 3);
        wrapper.mouseDown(e1);

        s1 = wrapper.getEvents().next();

        assertFalse("more events", wrapper.getEvents().hasNext());

        assertEquals("x", 2, s1.getX());
        assertEquals("dx", 0, s1.getDx());
        assertEquals("y", 96, s1.getY());
        assertEquals("dy", 0, s1.getDy());
        assertTrue("left down", s1.getButtonState(MouseButton.LEFT) == ButtonState.DOWN);
        assertTrue("right down", s1.getButtonState(MouseButton.RIGHT) == ButtonState.DOWN);

        // release left button
        setWithButtons(e1, 3, 5, 1);
        wrapper.mouseUp(e1);

        s1 = wrapper.getEvents().next();

        // Because we moved the mouse, there should not be a click count for the left button release...
        assertTrue("clicked", s1.getClickCount(MouseButton.LEFT) == 0);

        assertFalse("more events", wrapper.getEvents().hasNext());

        assertEquals("x", 3, s1.getX());
        assertEquals("dx", 1, s1.getDx());
        assertEquals("y", 95, s1.getY());
        assertEquals("dy", -1, s1.getDy());
        assertFalse("left down", s1.getButtonState(MouseButton.LEFT) == ButtonState.DOWN);
        assertTrue("right down", s1.getButtonState(MouseButton.RIGHT) == ButtonState.DOWN);

        // release right button
        setWithButtons(e1, 3, 5, 3);
        wrapper.mouseUp(e1);

        // We expect two state, one with a clickcount of 1,
        s1 = wrapper.getEvents().next();

        assertTrue("clicked", s1.getClickCount(MouseButton.RIGHT) == 1);

        // and then the same state, but with clickcount of 0
        s1 = wrapper.getEvents().next();

        assertFalse("more events", wrapper.getEvents().hasNext());

        assertEquals("x", 3, s1.getX());
        assertEquals("dx", 0, s1.getDx());
        assertEquals("y", 95, s1.getY());
        assertEquals("dy", 0, s1.getDy());
        assertFalse("left down", s1.getButtonState(MouseButton.LEFT) == ButtonState.DOWN);
        assertFalse("right down", s1.getButtonState(MouseButton.RIGHT) == ButtonState.DOWN);

    }

    private void setWithButtons(final MouseEvent event, final int x, final int y, final int button) {
        event.x = x;
        event.y = y;
        event.button = button;
    }

    private void setXY(final MouseEvent event, final int x, final int y) {
        event.x = x;
        event.y = y;
    }

}
