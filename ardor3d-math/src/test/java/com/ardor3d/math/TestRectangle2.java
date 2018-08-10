/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.math;

import static org.junit.Assert.*;

import org.junit.Test;

public class TestRectangle2 {
    @Test
    public void testGetSet() {
        final Rectangle2 rect1 = new Rectangle2();
        assertTrue(0 == rect1.getX());
        assertTrue(0 == rect1.getY());
        assertTrue(0 == rect1.getWidth());
        assertTrue(0 == rect1.getHeight());

        final Rectangle2 rect2 = new Rectangle2(2, 3, 4, 5);
        assertTrue(2 == rect2.getX());
        assertTrue(3 == rect2.getY());
        assertTrue(4 == rect2.getWidth());
        assertTrue(5 == rect2.getHeight());

        rect1.set(rect2);
        assertTrue(2 == rect1.getX());
        assertTrue(3 == rect1.getY());
        assertTrue(4 == rect1.getWidth());
        assertTrue(5 == rect1.getHeight());

        final Rectangle2 rect3 = new Rectangle2(rect1);
        assertTrue(2 == rect3.getX());
        assertTrue(3 == rect3.getY());
        assertTrue(4 == rect3.getWidth());
        assertTrue(5 == rect3.getHeight());

        rect1.set(5, 0, 10, 15);
        assertTrue(5 == rect1.getX());
        assertTrue(0 == rect1.getY());
        assertTrue(10 == rect1.getWidth());
        assertTrue(15 == rect1.getHeight());

        rect1.setX(4);
        assertTrue(4 == rect1.getX());
        rect1.setY(42);
        assertTrue(42 == rect1.getY());
        rect1.setWidth(50);
        assertTrue(50 == rect1.getWidth());
        rect1.setHeight(100);
        assertTrue(100 == rect1.getHeight());
    }

    @Test
    public void testClone() {
        final Rectangle2 rect1 = new Rectangle2();
        final Rectangle2 rect2 = rect1.clone();
        assertEquals(rect1, rect2);
        assertNotSame(rect1, rect2);
    }

    @Test
    public void testSimpleHash() {
        // Just a simple sanity check.
        final Rectangle2 rect1 = new Rectangle2(1, 2, 0, 0);
        final Rectangle2 rect2 = new Rectangle2(1, 2, 0, 0);
        final Rectangle2 rect3 = new Rectangle2(2, 2, 0, 0);

        assertTrue(rect1.hashCode() == rect2.hashCode());
        assertTrue(rect1.hashCode() != rect3.hashCode());
    }

    @Test
    public void testEquals() {
        final Rectangle2 rect1 = new Rectangle2(1, 2, 3, 4);
        final Rectangle2 rect2 = new Rectangle2(1, 0, 0, 0);
        final Rectangle2 rect3 = new Rectangle2(1, 2, 0, 0);
        final Rectangle2 rect4 = new Rectangle2(1, 2, 3, 0);
        final Rectangle2 rect5 = new Rectangle2(1, 2, 3, 4);

        // couple of equals validity tests
        assertEquals(rect1, rect1);
        assertFalse(rect1.equals(null));
        assertFalse(rect1.equals(new Vector2()));

        assertFalse(rect1.equals(rect2));
        assertFalse(rect1.equals(rect3));
        assertFalse(rect1.equals(rect4));
        assertTrue(rect1.equals(rect5));
    }

    @Test
    public void testIntersect() {
        final Rectangle2 rect1 = new Rectangle2(0, 0, 10, 10);
        final Rectangle2 rect2 = new Rectangle2(5, 5, 10, 10);

        Rectangle2 intersection = rect1.intersect(rect2, Rectangle2.fetchTempInstance());
        assertTrue(5 == intersection.getX());
        assertTrue(5 == intersection.getY());
        assertTrue(5 == intersection.getWidth());
        assertTrue(5 == intersection.getHeight());
        Rectangle2.releaseTempInstance(intersection);
        assertNotNull(rect1.intersect(rect2, null));

        intersection = Rectangle2.intersect(rect1, rect2, Rectangle2.fetchTempInstance());
        assertTrue(5 == intersection.getX());
        assertTrue(5 == intersection.getY());
        assertTrue(5 == intersection.getWidth());
        assertTrue(5 == intersection.getHeight());
        Rectangle2.releaseTempInstance(intersection);
        assertNotNull(Rectangle2.intersect(rect1, rect2, null));

    }
}
