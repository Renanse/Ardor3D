/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
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

public class TestVector3 {

    @Test
    public void testAdd() {
        final Vector3 vec1 = new Vector3();
        final Vector3 vec2 = new Vector3(Vector3.ONE);

        vec1.addLocal(1, 2, 3);
        assertEquals(new Vector3(1, 2, 3), vec1);
        vec1.addLocal(-1, -2, -3);
        assertEquals(Vector3.ZERO, vec1);

        vec1.zero();
        vec1.addLocal(vec2);
        assertEquals(Vector3.ONE, vec1);

        vec1.zero();
        final Vector3 vec3 = vec1.add(vec2, new Vector3());
        assertEquals(Vector3.ZERO, vec1);
        assertEquals(Vector3.ONE, vec3);

        final Vector3 vec4 = vec1.add(1, 0, 0, null);
        assertEquals(Vector3.ZERO, vec1);
        assertEquals(Vector3.UNIT_X, vec4);
    }

    @Test
    public void testSubtract() {
        final Vector3 vec1 = new Vector3();
        final Vector3 vec2 = new Vector3(Vector3.ONE);

        vec1.subtractLocal(1, 2, 3);
        assertEquals(new Vector3(-1, -2, -3), vec1);
        vec1.subtractLocal(-1, -2, -3);
        assertEquals(Vector3.ZERO, vec1);

        vec1.zero();
        vec1.subtractLocal(vec2);
        assertEquals(Vector3.NEG_ONE, vec1);

        vec1.zero();
        final Vector3 vec3 = vec1.subtract(vec2, new Vector3());
        assertEquals(Vector3.ZERO, vec1);
        assertEquals(Vector3.NEG_ONE, vec3);

        final Vector3 vec4 = vec1.subtract(1, 0, 0, null);
        assertEquals(Vector3.ZERO, vec1);
        assertEquals(Vector3.NEG_UNIT_X, vec4);
    }

    @Test
    public void testGetSet() {
        final Vector3 vec1 = new Vector3();
        vec1.setX(0);
        assertTrue(vec1.getX() == 0.0);
        vec1.setX(Double.POSITIVE_INFINITY);
        assertTrue(vec1.getX() == Double.POSITIVE_INFINITY);
        vec1.setX(Double.NEGATIVE_INFINITY);
        assertTrue(vec1.getX() == Double.NEGATIVE_INFINITY);
        assertTrue(vec1.getValue(0) == Double.NEGATIVE_INFINITY);

        vec1.setY(0);
        assertTrue(vec1.getY() == 0.0);
        vec1.setY(Double.POSITIVE_INFINITY);
        assertTrue(vec1.getY() == Double.POSITIVE_INFINITY);
        vec1.setY(Double.NEGATIVE_INFINITY);
        assertTrue(vec1.getY() == Double.NEGATIVE_INFINITY);
        assertTrue(vec1.getValue(1) == Double.NEGATIVE_INFINITY);

        vec1.setZ(0);
        assertTrue(vec1.getZ() == 0.0);
        vec1.setZ(Double.POSITIVE_INFINITY);
        assertTrue(vec1.getZ() == Double.POSITIVE_INFINITY);
        vec1.setZ(Double.NEGATIVE_INFINITY);
        assertTrue(vec1.getZ() == Double.NEGATIVE_INFINITY);
        assertTrue(vec1.getValue(2) == Double.NEGATIVE_INFINITY);

        vec1.set(Math.PI, Math.PI, Math.PI);
        assertTrue(vec1.getXf() == (float) Math.PI);
        assertTrue(vec1.getYf() == (float) Math.PI);
        assertTrue(vec1.getZf() == (float) Math.PI);

        final Vector3 vec2 = new Vector3();
        vec2.set(vec1);
        assertEquals(vec1, vec2);

        vec1.setValue(0, 0);
        vec1.setValue(1, 0);
        vec1.setValue(2, 0);
        assertEquals(Vector3.ZERO, vec1);

        // catch a few expected exceptions
        try {
            vec2.getValue(3);
            fail("getValue(3) should have thrown IllegalArgumentException.");
        } catch (final IllegalArgumentException e) {
        }
        try {
            vec2.getValue(-1);
            fail("getValue(-1) should have thrown IllegalArgumentException.");
        } catch (final IllegalArgumentException e) {
        }
        try {
            vec2.setValue(-1, 0);
            fail("setValue(-1, 0) should have thrown IllegalArgumentException.");
        } catch (final IllegalArgumentException e) {
        }
        try {
            vec2.setValue(3, 0);
            fail("setValue(3, 0) should have thrown IllegalArgumentException.");
        } catch (final IllegalArgumentException e) {
        }
        // above exceptions shouldn't have altered vec2
        assertEquals(new Vector3(Math.PI, Math.PI, Math.PI), vec2);
    }

    @Test
    public void testToArray() {
        final Vector3 vec1 = new Vector3();
        vec1.set(Math.PI, Double.MAX_VALUE, 42);
        final double[] array = vec1.toArray(null);
        final double[] array2 = vec1.toArray(new double[3]);
        assertNotNull(array);
        assertTrue(array.length == 3);
        assertTrue(array[0] == Math.PI);
        assertTrue(array[1] == Double.MAX_VALUE);
        assertTrue(array[2] == 42);
        assertNotNull(array2);
        assertTrue(array2.length == 3);
        assertTrue(array2[0] == Math.PI);
        assertTrue(array2[1] == Double.MAX_VALUE);
        assertTrue(array2[2] == 42);

        try {
            vec1.toArray(new double[1]);
            fail("toArray(d[1]) should have thrown ArrayIndexOutOfBoundsException.");
        } catch (final ArrayIndexOutOfBoundsException e) {
        }

        final float[] farray = vec1.toFloatArray(null);
        final float[] farray2 = vec1.toFloatArray(new float[3]);
        assertNotNull(farray);
        assertTrue(farray.length == 3);
        assertTrue(farray[0] == (float) Math.PI);
        assertTrue(farray[1] == (float) Double.MAX_VALUE);
        assertTrue(farray[2] == 42f);
        assertNotNull(farray2);
        assertTrue(farray2.length == 3);
        assertTrue(farray2[0] == (float) Math.PI);
        assertTrue(farray2[1] == (float) Double.MAX_VALUE);
        assertTrue(farray2[2] == 42f);

        try {
            vec1.toFloatArray(new float[1]);
            fail("toFloatArray(d[1]) should have thrown ArrayIndexOutOfBoundsException.");
        } catch (final ArrayIndexOutOfBoundsException e) {
        }
    }

    @Test
    public void testMultiply() {
        final Vector3 vec1 = new Vector3(1, -1, 2);
        final Vector3 vec2 = vec1.multiply(2.0, null);
        final Vector3 vec2B = vec1.multiply(2.0, new Vector3());
        assertEquals(new Vector3(2.0, -2.0, 4.0), vec2);
        assertEquals(new Vector3(2.0, -2.0, 4.0), vec2B);

        vec2.multiplyLocal(0.5);
        assertEquals(new Vector3(1.0, -1.0, 2.0), vec2);

        final Vector3 vec3 = vec1.multiply(vec2, null);
        final Vector3 vec3B = vec1.multiply(vec2, new Vector3());
        assertEquals(new Vector3(1, 1, 4), vec3);
        assertEquals(new Vector3(1, 1, 4), vec3B);

        final Vector3 vec4 = vec1.multiply(2, 3, 2, null);
        final Vector3 vec4B = vec1.multiply(2, 3, 2, new Vector3());
        assertEquals(new Vector3(2, -3, 4), vec4);
        assertEquals(new Vector3(2, -3, 4), vec4B);

        vec1.multiplyLocal(0.5, 0.5, 0.5);
        assertEquals(new Vector3(0.5, -0.5, 1.0), vec1);

        vec1.multiplyLocal(vec2);
        assertEquals(new Vector3(0.5, 0.5, 2.0), vec1);
    }

    @Test
    public void testDivide() {
        final Vector3 vec1 = new Vector3(1, -1, 2);
        final Vector3 vec2 = vec1.divide(2.0, null);
        final Vector3 vec2B = vec1.divide(2.0, new Vector3());
        assertEquals(new Vector3(0.5, -0.5, 1.0), vec2);
        assertEquals(new Vector3(0.5, -0.5, 1.0), vec2B);

        vec2.divideLocal(0.5);
        assertEquals(new Vector3(1.0, -1.0, 2.0), vec2);

        final Vector3 vec3 = vec1.divide(vec2, null);
        final Vector3 vec3B = vec1.divide(vec2, new Vector3());
        assertEquals(Vector3.ONE, vec3);
        assertEquals(Vector3.ONE, vec3B);

        final Vector3 vec4 = vec1.divide(2, 3, 4, null);
        final Vector3 vec4B = vec1.divide(2, 3, 4, new Vector3());
        assertEquals(new Vector3(0.5, -1 / 3., 0.5), vec4);
        assertEquals(new Vector3(0.5, -1 / 3., 0.5), vec4B);

        vec1.divideLocal(0.5, 0.5, 0.5);
        assertEquals(new Vector3(2, -2, 4), vec1);

        vec1.divideLocal(vec2);
        assertEquals(new Vector3(2, 2, 2), vec1);
    }

    @Test
    public void testScaleAdd() {
        final Vector3 vec1 = new Vector3(1, 1, 1);
        final Vector3 vec2 = vec1.scaleAdd(2.0, new Vector3(1, 2, 3), null);
        final Vector3 vec2B = vec1.scaleAdd(2.0, new Vector3(1, 2, 3), new Vector3());
        assertEquals(new Vector3(3.0, 4.0, 5.0), vec2);
        assertEquals(new Vector3(3.0, 4.0, 5.0), vec2B);

        vec1.scaleAddLocal(2.0, new Vector3(1, 2, 3));
        assertEquals(vec2, vec1);
    }

    @Test
    public void testNegate() {
        final Vector3 vec1 = new Vector3(3, 2, -1);
        final Vector3 vec2 = vec1.negate(null);
        assertEquals(new Vector3(-3, -2, 1), vec2);

        vec1.negateLocal();
        assertEquals(vec2, vec1);
    }

    @Test
    public void testNormalize() {
        final Vector3 vec1 = new Vector3(2, 1, 3);
        assertTrue(vec1.length() == Math.sqrt(14));

        final Vector3 vec2 = vec1.normalize(null);
        final double invLength = MathUtils.inverseSqrt(2 * 2 + 1 * 1 + 3 * 3);
        assertEquals(new Vector3(2 * invLength, 1 * invLength, 3 * invLength), vec2);

        vec1.normalizeLocal();
        assertEquals(new Vector3(2 * invLength, 1 * invLength, 3 * invLength), vec1);

        vec1.zero();
        vec1.normalize(vec2);
        assertEquals(vec1, vec2);

        // ensure no exception thrown
        vec1.normalizeLocal();
        vec1.normalize(null);
    }

    @Test
    public void testDistance() {
        final Vector3 vec1 = new Vector3(0, 0, 0);
        assertTrue(4.0 == vec1.distance(4, 0, 0));
        assertTrue(3.0 == vec1.distance(0, 3, 0));
        assertTrue(2.0 == vec1.distance(0, 0, 2));

        final Vector3 vec2 = new Vector3(1, 1, 1);
        assertTrue(Math.sqrt(3) == vec1.distance(vec2));
    }

    @Test
    public void testLerp() {
        final Vector3 vec1 = new Vector3(8, 3, -2);
        final Vector3 vec2 = new Vector3(2, 1, 0);
        assertEquals(new Vector3(5, 2, -1), vec1.lerp(vec2, 0.5, null));
        assertEquals(new Vector3(5, 2, -1), vec1.lerp(vec2, 0.5, new Vector3()));
        assertEquals(new Vector3(5, 2, -1), Vector3.lerp(vec1, vec2, 0.5, null));
        assertEquals(new Vector3(5, 2, -1), Vector3.lerp(vec1, vec2, 0.5, new Vector3()));

        vec1.set(14, 5, 4);
        vec1.lerpLocal(vec2, 0.25);
        assertEquals(new Vector3(11, 4, 3), vec1);

        vec1.set(15, 7, 6);
        final Vector3 vec3 = new Vector3(-1, -1, -1);
        vec3.lerpLocal(vec1, vec2, 0.5);
        assertEquals(new Vector3(8.5, 4.0, 3.0), vec3);

        // coverage
        assertEquals(vec1.lerp(vec1, .25, null), vec1);
        assertEquals(vec2.lerpLocal(vec2, .25), vec2);
        assertEquals(vec2.lerpLocal(vec2, vec2, .25), vec2);
        assertEquals(Vector3.lerp(vec1, vec1, .25, null), vec1);
    }

    @Test
    public void testCross() {
        final Vector3 vec1 = new Vector3(1, 0, 0);
        final Vector3 vec2 = new Vector3(0, 1, 0);
        assertEquals(Vector3.UNIT_Z, vec1.cross(vec2, null));
        assertEquals(Vector3.UNIT_Z, vec1.cross(vec2, new Vector3()));

        assertEquals(Vector3.UNIT_Z, vec1.cross(0, 1, 0, null));
        assertEquals(Vector3.UNIT_Z, vec1.cross(0, 1, 0, new Vector3()));

        vec1.crossLocal(vec2);
        assertEquals(Vector3.UNIT_Z, vec1);
        vec2.crossLocal(1, 0, 0);
        assertEquals(Vector3.NEG_UNIT_Z, vec2);
    }

    @Test
    public void testAngle() {
        final Vector3 vec1 = new Vector3(1, 0, 0);

        assertTrue(MathUtils.HALF_PI == vec1.smallestAngleBetween(new Vector3(0, -1, 0)));
    }

    @Test
    public void testDot() {
        final Vector3 vec1 = new Vector3(7, 2, 5);
        assertTrue(33.0 == vec1.dot(3, 1, 2));

        assertTrue(-10.0 == vec1.dot(new Vector3(-1, 1, -1)));
    }

    @Test
    public void testClone() {
        final Vector3 vec1 = new Vector3(0, 0, 0);
        final Vector3 vec2 = vec1.clone();
        assertEquals(vec1, vec2);
        assertNotSame(vec1, vec2);
    }

    @Test
    public void testValid() {
        final Vector3 vec1 = new Vector3(0, 0, 0);
        final Vector3 vec2A = new Vector3(Double.POSITIVE_INFINITY, 0, 0);
        final Vector3 vec2B = new Vector3(0, Double.NEGATIVE_INFINITY, 0);
        final Vector3 vec2C = new Vector3(0, 0, Double.POSITIVE_INFINITY);
        final Vector3 vec3A = new Vector3(Double.NaN, 0, 0);
        final Vector3 vec3B = new Vector3(0, Double.NaN, 0);
        final Vector3 vec3C = new Vector3(0, 0, Double.NaN);

        assertTrue(Vector3.isValid(vec1));
        assertFalse(Vector3.isValid(vec2A));
        assertFalse(Vector3.isValid(vec2B));
        assertFalse(Vector3.isValid(vec2C));
        assertFalse(Vector3.isValid(vec3A));
        assertFalse(Vector3.isValid(vec3B));
        assertFalse(Vector3.isValid(vec3C));

        assertFalse(Vector3.isInfinite(vec1));
        assertTrue(Vector3.isInfinite(vec2A));

        vec3C.zero();
        assertTrue(Vector3.isValid(vec3C));

        assertFalse(Vector3.isValid(null));
        assertFalse(Vector3.isInfinite(null));

        // couple of equals validity tests
        assertEquals(vec1, vec1);
        assertFalse(vec1.equals(null));
        assertFalse(vec1.equals(new Vector4()));

        // throw in a couple pool accesses for coverage
        final Vector3 vec6 = Vector3.fetchTempInstance();
        vec6.set(vec1);
        assertEquals(vec1, vec6);
        assertNotSame(vec1, vec6);
        Vector3.releaseTempInstance(vec6);

        // cover more of equals
        vec1.set(0, 1, 2);
        assertFalse(vec1.equals(new Vector3(0, 2, 3)));
        assertFalse(vec1.equals(new Vector3(0, 1, 3)));
    }

    @Test
    public void testSimpleHash() {
        // Just a simple sanity check.
        final Vector3 vec1 = new Vector3(1, 2, 3);
        final Vector3 vec2 = new Vector3(1, 2, 3);
        final Vector3 vec3 = new Vector3(2, 2, 2);

        assertTrue(vec1.hashCode() == vec2.hashCode());
        assertTrue(vec1.hashCode() != vec3.hashCode());
    }
}
