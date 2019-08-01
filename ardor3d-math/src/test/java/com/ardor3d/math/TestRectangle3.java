/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.math;

import static org.junit.Assert.*;

import org.junit.Test;

public class TestRectangle3 {

    @Test
    public void testGetSet() {
        final Rectangle3 rect = new Rectangle3();
        assertEquals(Vector3.ZERO, rect.getA());
        assertEquals(Vector3.ZERO, rect.getB());
        assertEquals(Vector3.ZERO, rect.getC());

        rect.setA(Vector3.ONE);
        rect.setB(Vector3.UNIT_X);
        rect.setC(Vector3.UNIT_Z);
        assertEquals(Vector3.ONE, rect.getA());
        assertEquals(Vector3.UNIT_X, rect.getB());
        assertEquals(Vector3.UNIT_Z, rect.getC());

        final Rectangle3 rect2 = new Rectangle3(rect);
        assertEquals(Vector3.ONE, rect2.getA());
        assertEquals(Vector3.UNIT_X, rect2.getB());
        assertEquals(Vector3.UNIT_Z, rect2.getC());

        final Rectangle3 rect3 = new Rectangle3(Vector3.NEG_ONE, Vector3.UNIT_Z, Vector3.NEG_UNIT_Y);
        assertEquals(Vector3.NEG_ONE, rect3.getA());
        assertEquals(Vector3.UNIT_Z, rect3.getB());
        assertEquals(Vector3.NEG_UNIT_Y, rect3.getC());
    }

    @Test
    public void testEquals() {
        // couple of equals validity tests
        final Rectangle3 rect1 = new Rectangle3();
        assertEquals(rect1, rect1);
        assertFalse(rect1.equals(null));
        assertFalse(rect1.equals(new Vector2()));

        // throw in a couple pool accesses for coverage
        final Rectangle3 rect2 = Rectangle3.fetchTempInstance();
        rect2.set(rect1);
        assertEquals(rect1, rect2);
        assertNotSame(rect1, rect2);
        Rectangle3.releaseTempInstance(rect2);

        // cover more of equals
        assertTrue(rect1.equals(new Rectangle3(Vector3.ZERO, Vector3.ZERO, Vector3.ZERO)));
        assertFalse(rect1.equals(new Rectangle3(Vector3.ZERO, Vector3.ZERO, Vector3.UNIT_X)));
        assertFalse(rect1.equals(new Rectangle3(Vector3.ZERO, Vector3.UNIT_X, Vector3.UNIT_X)));
        assertFalse(rect1.equals(new Rectangle3(Vector3.ZERO, Vector3.UNIT_X, Vector3.ZERO)));
        assertFalse(rect1.equals(new Rectangle3(Vector3.UNIT_X, Vector3.ZERO, Vector3.ZERO)));
        assertFalse(rect1.equals(new Rectangle3(Vector3.UNIT_X, Vector3.ZERO, Vector3.UNIT_X)));
        assertFalse(rect1.equals(new Rectangle3(Vector3.UNIT_X, Vector3.UNIT_X, Vector3.ZERO)));
        assertFalse(rect1.equals(new Rectangle3(Vector3.UNIT_X, Vector3.UNIT_X, Vector3.UNIT_X)));
    }

    @Test
    public void testSimpleHash() {
        // Just a simple sanity check.
        final Rectangle3 rect1 = new Rectangle3(Vector3.ZERO, Vector3.UNIT_Y, Vector3.UNIT_X);
        final Rectangle3 rect2 = new Rectangle3(Vector3.ZERO, Vector3.UNIT_Y, Vector3.UNIT_X);
        final Rectangle3 rect3 = new Rectangle3(Vector3.ZERO, Vector3.UNIT_Y, Vector3.UNIT_Z);

        assertTrue(rect1.hashCode() == rect2.hashCode());
        assertTrue(rect1.hashCode() != rect3.hashCode());
    }

    @Test
    public void testClone() {
        final Rectangle3 rect1 = new Rectangle3();
        final Rectangle3 rect2 = rect1.clone();
        assertEquals(rect1, rect2);
        assertNotSame(rect1, rect2);
    }

    @Test
    public void testRandom() {
        MathUtils.setRandomSeed(0);
        final Rectangle3 rect1 = new Rectangle3();
        final Vector3 store = rect1.random(null);
        assertEquals(new Vector3(0.0, 0.0, 0.0), store);

        rect1.setA(new Vector3(1, 0, 0));
        rect1.setB(new Vector3(1, 1, 0));
        rect1.setC(new Vector3(0, 1, 0));
        rect1.random(store);
        assertEquals(new Vector3(0.39365482330322266, 0.8468815684318542, 0.0), store);
    }
}
