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

public class TestTriangle {

    @Test
    public void testGetSet() {
        final Triangle tri1 = new Triangle();
        assertEquals(Vector3.ZERO, tri1.getA());
        assertEquals(Vector3.ZERO, tri1.getB());
        assertEquals(Vector3.ZERO, tri1.getC());
        assertTrue(tri1.getIndex() == 0);

        tri1.setA(Vector3.NEG_ONE);
        tri1.setB(Vector3.UNIT_X);
        tri1.setC(Vector3.UNIT_Z);
        tri1.setIndex(1);
        assertEquals(Vector3.NEG_ONE, tri1.getA());
        assertEquals(Vector3.UNIT_X, tri1.getB());
        assertEquals(Vector3.UNIT_Z, tri1.getC());
        assertTrue(tri1.getIndex() == 1);

        final Triangle tri2 = new Triangle(tri1);
        assertEquals(Vector3.NEG_ONE, tri2.getA());
        assertEquals(Vector3.UNIT_X, tri2.getB());
        assertEquals(Vector3.UNIT_Z, tri2.getC());
        assertTrue(tri2.getIndex() == 1);

        final Triangle tri3 = new Triangle(Vector3.ONE, Vector3.UNIT_Y, Vector3.NEG_ONE);
        assertEquals(Vector3.ONE, tri3.getA());
        assertEquals(Vector3.UNIT_Y, tri3.getB());
        assertEquals(Vector3.NEG_ONE, tri3.getC());
        assertTrue(tri3.getIndex() == 0);

        final Triangle tri4 = new Triangle(Vector3.ONE, Vector3.UNIT_Y, Vector3.NEG_ONE, 42);
        assertEquals(Vector3.ONE, tri4.getA());
        assertEquals(Vector3.UNIT_Y, tri4.getB());
        assertEquals(Vector3.NEG_ONE, tri4.getC());
        assertTrue(tri4.getIndex() == 42);

        tri2.set(0, Vector3.UNIT_X);
        tri2.set(1, Vector3.UNIT_Y);
        tri2.set(2, Vector3.UNIT_Z);

        // catch a few expected exceptions
        try {
            tri2.get(3);
            fail("get(3) should have thrown IllegalArgumentException.");
        } catch (final IllegalArgumentException e) {
        }
        try {
            tri2.get(-1);
            fail("get(-1) should have thrown IllegalArgumentException.");
        } catch (final IllegalArgumentException e) {
        }
        try {
            tri2.set(-1, Vector3.ZERO);
            fail("set(-1, 0) should have thrown IllegalArgumentException.");
        } catch (final IllegalArgumentException e) {
        }
        try {
            tri2.set(3, Vector3.ZERO);
            fail("set(3, 0) should have thrown IllegalArgumentException.");
        } catch (final IllegalArgumentException e) {
        }

        // shouldn't have changed
        assertEquals(Vector3.UNIT_X, tri2.get(0));
        assertEquals(Vector3.UNIT_Y, tri2.get(1));
        assertEquals(Vector3.UNIT_Z, tri2.get(2));
    }

    @Test
    public void testEquals() {
        // couple of equals validity tests
        final Triangle tri1 = new Triangle();
        assertEquals(tri1, tri1);
        assertFalse(tri1.equals(null));
        assertFalse(tri1.equals(new Vector2()));

        // throw in a couple pool accesses for coverage
        final Triangle tri2 = Triangle.fetchTempInstance();
        tri2.set(tri1);
        assertEquals(tri1, tri2);
        assertNotSame(tri1, tri2);
        Triangle.releaseTempInstance(tri2);

        // cover more of equals
        assertTrue(tri1.equals(new Triangle(Vector3.ZERO, Vector3.ZERO, Vector3.ZERO)));
        assertFalse(tri1.equals(new Triangle(Vector3.ZERO, Vector3.ZERO, Vector3.UNIT_X)));
        assertFalse(tri1.equals(new Triangle(Vector3.ZERO, Vector3.UNIT_X, Vector3.UNIT_X)));
        assertFalse(tri1.equals(new Triangle(Vector3.ZERO, Vector3.UNIT_X, Vector3.ZERO)));
        assertFalse(tri1.equals(new Triangle(Vector3.UNIT_X, Vector3.ZERO, Vector3.ZERO)));
        assertFalse(tri1.equals(new Triangle(Vector3.UNIT_X, Vector3.ZERO, Vector3.UNIT_X)));
        assertFalse(tri1.equals(new Triangle(Vector3.UNIT_X, Vector3.UNIT_X, Vector3.ZERO)));
        assertFalse(tri1.equals(new Triangle(Vector3.UNIT_X, Vector3.UNIT_X, Vector3.UNIT_X)));
    }

    @Test
    public void testValid() {
        final Triangle vec1 = new Triangle();
        final Triangle vec2 = new Triangle(new Vector3(0, 0, Double.NaN), Vector3.ZERO, Vector3.ZERO);
        final Triangle vec3 = new Triangle(Vector3.ZERO, new Vector3(0, 0, Double.NaN), Vector3.ZERO);
        final Triangle vec4 = new Triangle(Vector3.ZERO, Vector3.ZERO, new Vector3(0, 0, Double.NaN));

        assertTrue(Triangle.isValid(vec1));
        assertFalse(Triangle.isValid(vec2));
        assertFalse(Triangle.isValid(vec3));
        assertFalse(Triangle.isValid(vec4));

        vec4.setC(Vector3.ZERO);
        assertTrue(Triangle.isValid(vec4));

        assertFalse(Triangle.isValid(null));
    }

    @Test
    public void testSimpleHash() {
        // Just a simple sanity check.
        final Triangle tri1 = new Triangle(Vector3.ZERO, Vector3.UNIT_Y, Vector3.NEG_ONE, 1);
        final Triangle tri2 = new Triangle(Vector3.ZERO, Vector3.UNIT_Y, Vector3.NEG_ONE, 1);
        final Triangle tri3 = new Triangle(Vector3.ZERO, Vector3.UNIT_Z, Vector3.NEG_ONE, 2);

        assertTrue(tri1.hashCode() == tri2.hashCode());
        assertTrue(tri1.hashCode() != tri3.hashCode());
    }

    @Test
    public void testClone() {
        final Triangle tri1 = new Triangle();
        final Triangle tri2 = tri1.clone();
        assertEquals(tri1, tri2);
        assertNotSame(tri1, tri2);
    }

    @Test
    public void testCenter() {
        final Triangle tri1 = new Triangle(Vector3.ZERO, Vector3.UNIT_Y, Vector3.UNIT_X, 0);
        assertEquals(new Vector3(1 / 3., 1 / 3., 0), tri1.getCenter()); // dirty
        assertEquals(new Vector3(1 / 3., 1 / 3., 0), tri1.getCenter()); // clean
        tri1.setA(Vector3.ONE);
        assertEquals(new Vector3(2 / 3., 2 / 3., 1 / 3.), tri1.getCenter()); // dirty, but with existing center
    }

    @Test
    public void testNormal() {
        final Triangle tri1 = new Triangle(Vector3.ZERO, Vector3.UNIT_Y, Vector3.UNIT_X, 0);
        assertEquals(new Vector3(0, 0, -1), tri1.getNormal()); // dirty
        assertEquals(new Vector3(0, 0, -1), tri1.getNormal()); // clean
        tri1.setB(Vector3.UNIT_Z);
        assertEquals(new Vector3(0, 1, 0), tri1.getNormal()); // dirty, but with existing normal
    }
}
