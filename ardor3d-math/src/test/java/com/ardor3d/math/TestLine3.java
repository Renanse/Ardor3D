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

public class TestLine3 {

    @Test
    public void testGetSet() {
        final Line3 line1 = new Line3();
        assertEquals(Vector3.ZERO, line1.getOrigin());
        assertEquals(Vector3.UNIT_Z, line1.getDirection());

        line1.setOrigin(Vector3.NEG_ONE);
        line1.setDirection(Vector3.UNIT_X);
        assertEquals(Vector3.NEG_ONE, line1.getOrigin());
        assertEquals(Vector3.UNIT_X, line1.getDirection());

        final Line3 line2 = new Line3(line1);
        assertEquals(Vector3.NEG_ONE, line2.getOrigin());
        assertEquals(Vector3.UNIT_X, line2.getDirection());

        final Line3 line3 = new Line3(Vector3.ONE, Vector3.UNIT_Y);
        assertEquals(Vector3.ONE, line3.getOrigin());
        assertEquals(Vector3.UNIT_Y, line3.getDirection());
    }

    @Test
    public void testEquals() {
        // couple of equals validity tests
        final Line3 line1 = new Line3();
        assertEquals(line1, line1);
        assertFalse(line1.equals(null));
        assertFalse(line1.equals(new Vector2()));

        // throw in a couple pool accesses for coverage
        final Line3 line2 = Line3.fetchTempInstance();
        line2.set(line1);
        assertEquals(line1, line2);
        assertNotSame(line1, line2);
        Line3.releaseTempInstance(line2);

        // cover more of equals
        assertFalse(line1.equals(new Line3(Vector3.ZERO, Vector3.UNIT_X)));
    }

    @Test
    public void testSimpleHash() {
        // Just a simple sanity check.
        final Line3 line1 = new Line3(Vector3.ZERO, Vector3.UNIT_Y);
        final Line3 line2 = new Line3(Vector3.ZERO, Vector3.UNIT_Y);
        final Line3 line3 = new Line3(Vector3.ZERO, Vector3.UNIT_Z);

        assertTrue(line1.hashCode() == line2.hashCode());
        assertTrue(line1.hashCode() != line3.hashCode());
    }

    @Test
    public void testClone() {
        final Line3 line1 = new Line3();
        final Line3 line2 = line1.clone();
        assertEquals(line1, line2);
        assertNotSame(line1, line2);
    }

    @Test
    public void testValid() {
        final Line3 line1 = new Line3();
        final Line3 line2 = new Line3(new Vector3(Double.NaN, 0, 0), Vector3.UNIT_Z);
        final Line3 line3 = new Line3(Vector3.ZERO, new Vector3(Double.NaN, 0, 0));

        assertTrue(Line3.isValid(line1));
        assertFalse(Line3.isValid(line2));
        assertFalse(Line3.isValid(line3));

        line2.setOrigin(Vector3.ZERO);
        assertTrue(Line3.isValid(line2));

        assertFalse(Line3.isValid(null));
    }

    @Test
    public void testDistance() {
        final Line3 line1 = new Line3(Vector3.ZERO, Vector3.UNIT_Z);
        final Vector3 store = new Vector3();
        assertTrue(0.0 == line1.distanceSquared(new Vector3(0, 0, 5), store));
        assertTrue(16.0 == line1.distanceSquared(new Vector3(0, 4, 1), store));
        assertEquals(Vector3.UNIT_Z, store);
        assertTrue(9.0 == line1.distanceSquared(new Vector3(0, -3, -1), store));
        assertEquals(Vector3.NEG_UNIT_Z, store);
        assertTrue(1.0 == line1.distanceSquared(Vector3.NEG_UNIT_X, null));
    }

}
