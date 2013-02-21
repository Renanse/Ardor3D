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

public class TestVector2 {

    @Test
    public void testAdd() {
        final Vector2 vec1 = new Vector2();
        final Vector2 vec2 = new Vector2(Vector2.ONE);

        vec1.addLocal(1, 2);
        assertEquals(new Vector2(1, 2), vec1);
        vec1.addLocal(-1, -2);
        assertEquals(Vector2.ZERO, vec1);

        vec1.zero();
        vec1.addLocal(vec2);
        assertEquals(Vector2.ONE, vec1);

        vec1.zero();
        final Vector2 vec3 = vec1.add(vec2, new Vector2());
        assertEquals(Vector2.ZERO, vec1);
        assertEquals(Vector2.ONE, vec3);

        final Vector2 vec4 = vec1.add(1, 0, null);
        assertEquals(Vector2.ZERO, vec1);
        assertEquals(Vector2.UNIT_X, vec4);
    }

    @Test
    public void testSubtract() {
        final Vector2 vec1 = new Vector2();
        final Vector2 vec2 = new Vector2(Vector2.ONE);

        vec1.subtractLocal(1, 2);
        assertEquals(new Vector2(-1, -2), vec1);
        vec1.subtractLocal(-1, -2);
        assertEquals(Vector2.ZERO, vec1);

        vec1.zero();
        vec1.subtractLocal(vec2);
        assertEquals(Vector2.NEG_ONE, vec1);

        vec1.zero();
        final Vector2 vec3 = vec1.subtract(vec2, new Vector2());
        assertEquals(Vector2.ZERO, vec1);
        assertEquals(Vector2.NEG_ONE, vec3);

        final Vector2 vec4 = vec1.subtract(1, 0, null);
        assertEquals(Vector2.ZERO, vec1);
        assertEquals(Vector2.NEG_UNIT_X, vec4);
    }

    @Test
    public void testGetSet() {
        final Vector2 vec1 = new Vector2();
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

        vec1.set(Math.PI, Math.PI);
        assertTrue(vec1.getXf() == (float) Math.PI);
        assertTrue(vec1.getYf() == (float) Math.PI);

        final Vector2 vec2 = new Vector2();
        vec2.set(vec1);
        assertEquals(vec1, vec2);

        vec1.setValue(0, 0);
        vec1.setValue(1, 0);
        assertEquals(Vector2.ZERO, vec1);

        // catch a few expected exceptions
        try {
            vec2.getValue(2);
            fail("getValue(2) should have thrown IllegalArgumentException.");
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
            vec2.setValue(2, 0);
            fail("setValue(2, 0) should have thrown IllegalArgumentException.");
        } catch (final IllegalArgumentException e) {
        }
        // above exceptions shouldn't have altered vec2
        assertEquals(new Vector2(Math.PI, Math.PI), vec2);
    }

    @Test
    public void testPolarAngle() {
        final Vector2 vec1 = new Vector2();
        assertTrue(0.0 == vec1.getPolarAngle());

        vec1.set(1.0, 0.0); // 0
        assertTrue(Math.abs(0 - vec1.getPolarAngle()) <= MathUtils.EPSILON);

        vec1.set(0.0, 1.0); // -HALF_PI
        assertTrue(Math.abs(-MathUtils.HALF_PI - vec1.getPolarAngle()) <= MathUtils.EPSILON);

        vec1.set(-1.0, 0.0); // -PI
        assertTrue(Math.abs(-MathUtils.PI - vec1.getPolarAngle()) <= MathUtils.EPSILON);

        vec1.set(0, -1.0); // HALF_PI
        assertTrue(Math.abs(MathUtils.HALF_PI - vec1.getPolarAngle()) <= MathUtils.EPSILON);
    }

    @Test
    public void testToArray() {
        final Vector2 vec1 = new Vector2();
        vec1.set(Math.PI, Double.MAX_VALUE);
        final double[] array = vec1.toArray(null);
        final double[] array2 = vec1.toArray(new double[2]);
        assertNotNull(array);
        assertTrue(array.length == 2);
        assertTrue(array[0] == Math.PI);
        assertTrue(array[1] == Double.MAX_VALUE);
        assertNotNull(array2);
        assertTrue(array2.length == 2);
        assertTrue(array2[0] == Math.PI);
        assertTrue(array2[1] == Double.MAX_VALUE);

        try {
            vec1.toArray(new double[1]);
            fail("toArray(d[1]) should have thrown ArrayIndexOutOfBoundsException.");
        } catch (final ArrayIndexOutOfBoundsException e) {
        }
    }

    @Test
    public void testMultiply() {
        final Vector2 vec1 = new Vector2(1, -1);
        final Vector2 vec2 = vec1.multiply(2.0, null);
        final Vector2 vec2B = vec1.multiply(2.0, new Vector2());
        assertEquals(new Vector2(2.0, -2.0), vec2);
        assertEquals(new Vector2(2.0, -2.0), vec2B);

        vec2.multiplyLocal(0.5);
        assertEquals(new Vector2(1.0, -1.0), vec2);

        final Vector2 vec3 = vec1.multiply(vec2, null);
        final Vector2 vec3B = vec1.multiply(vec2, new Vector2());
        assertEquals(Vector2.ONE, vec3);
        assertEquals(Vector2.ONE, vec3B);

        final Vector2 vec4 = vec1.multiply(2, 3, null);
        final Vector2 vec4B = vec1.multiply(2, 3, new Vector2());
        assertEquals(new Vector2(2, -3), vec4);
        assertEquals(new Vector2(2, -3), vec4B);

        vec1.multiplyLocal(0.5, 0.5);
        assertEquals(new Vector2(0.5, -0.5), vec1);

        vec1.multiplyLocal(vec2);
        assertEquals(new Vector2(0.5, 0.5), vec1);
    }

    @Test
    public void testDivide() {
        final Vector2 vec1 = new Vector2(1, -1);
        final Vector2 vec2 = vec1.divide(2.0, null);
        final Vector2 vec2B = vec1.divide(2.0, new Vector2());
        assertEquals(new Vector2(0.5, -0.5), vec2);
        assertEquals(new Vector2(0.5, -0.5), vec2B);

        vec2.divideLocal(0.5);
        assertEquals(new Vector2(1.0, -1.0), vec2);

        final Vector2 vec3 = vec1.divide(vec2, null);
        final Vector2 vec3B = vec1.divide(vec2, new Vector2());
        assertEquals(Vector2.ONE, vec3);
        assertEquals(Vector2.ONE, vec3B);

        final Vector2 vec4 = vec1.divide(2, 3, null);
        final Vector2 vec4B = vec1.divide(2, 3, new Vector2());
        assertEquals(new Vector2(1 / 2., -1 / 3.), vec4);
        assertEquals(new Vector2(1 / 2., -1 / 3.), vec4B);

        vec1.divideLocal(0.5, 0.5);
        assertEquals(new Vector2(2, -2), vec1);

        vec1.divideLocal(vec2);
        assertEquals(new Vector2(2, 2), vec1);
    }

    @Test
    public void testScaleAdd() {
        final Vector2 vec1 = new Vector2(1, 1);
        final Vector2 vec2 = vec1.scaleAdd(2.0, new Vector2(1, 2), null);
        final Vector2 vec2B = vec1.scaleAdd(2.0, new Vector2(1, 2), new Vector2());
        assertEquals(new Vector2(3.0, 4.0), vec2);
        assertEquals(new Vector2(3.0, 4.0), vec2B);

        vec1.scaleAddLocal(2.0, new Vector2(1, 2));
        assertEquals(vec2, vec1);
    }

    @Test
    public void testNegate() {
        final Vector2 vec1 = new Vector2(2, 1);
        final Vector2 vec2 = vec1.negate(null);
        assertEquals(new Vector2(-2, -1), vec2);

        vec1.negateLocal();
        assertEquals(vec2, vec1);
    }

    @Test
    public void testNormalize() {
        final Vector2 vec1 = new Vector2(2, 1);
        assertTrue(vec1.length() == Math.sqrt(5));

        final Vector2 vec2 = vec1.normalize(null);
        final double invLength = MathUtils.inverseSqrt(2 * 2 + 1 * 1);
        assertEquals(new Vector2(2 * invLength, 1 * invLength), vec2);

        vec1.normalizeLocal();
        assertEquals(new Vector2(2 * invLength, 1 * invLength), vec1);

        vec1.zero();
        vec1.normalize(vec2);
        assertEquals(vec1, vec2);

        // ensure no exception thrown
        vec1.normalizeLocal();
        vec1.normalize(null);
    }

    @Test
    public void testDistance() {
        final Vector2 vec1 = new Vector2(0, 0);
        assertTrue(3.0 == vec1.distance(0, 3));
        assertTrue(4.0 == vec1.distance(4, 0));

        final Vector2 vec2 = new Vector2(1, 1);
        assertTrue(Math.sqrt(2) == vec1.distance(vec2));
    }

    @Test
    public void testLerp() {
        final Vector2 vec1 = new Vector2(8, 3);
        final Vector2 vec2 = new Vector2(2, 1);
        assertEquals(new Vector2(5, 2), vec1.lerp(vec2, 0.5, null));
        assertEquals(new Vector2(5, 2), vec1.lerp(vec2, 0.5, new Vector2()));
        assertEquals(new Vector2(5, 2), Vector2.lerp(vec1, vec2, 0.5, null));
        assertEquals(new Vector2(5, 2), Vector2.lerp(vec1, vec2, 0.5, new Vector2()));

        vec1.set(14, 5);
        vec1.lerpLocal(vec2, 0.25);
        assertEquals(new Vector2(11, 4), vec1);

        vec1.set(15, 7);
        final Vector2 vec3 = new Vector2(-1, -1);
        vec3.lerpLocal(vec1, vec2, 0.5);
        assertEquals(new Vector2(8.5, 4.0), vec3);
    }

    @Test
    public void testRotate() {
        final Vector2 vec1 = new Vector2(1, 0);
        final Vector2 vec2 = vec1.rotateAroundOrigin(MathUtils.HALF_PI, true, null);
        final Vector2 vec2B = vec1.rotateAroundOrigin(MathUtils.HALF_PI, false, new Vector2());
        assertEquals(new Vector2(0, -1), vec2);
        assertEquals(new Vector2(0, 1), vec2B);
        vec2.rotateAroundOriginLocal(MathUtils.HALF_PI, false);
        assertEquals(new Vector2(1, 0), vec2);
        vec2.rotateAroundOriginLocal(MathUtils.PI, true);
        assertTrue(Math.abs(vec2.getX() - -1) <= MathUtils.EPSILON);
        assertTrue(Math.abs(vec2.getY() - 0) <= MathUtils.EPSILON);
    }

    @Test
    public void testAngle() {
        final Vector2 vec1 = new Vector2(1, 0);
        assertTrue(MathUtils.HALF_PI == vec1.angleBetween(new Vector2(0, 1)));
        assertTrue(-MathUtils.HALF_PI == vec1.angleBetween(new Vector2(0, -1)));

        assertTrue(MathUtils.HALF_PI == vec1.smallestAngleBetween(new Vector2(0, -1)));
    }

    @Test
    public void testDot() {
        final Vector2 vec1 = new Vector2(7, 2);
        assertTrue(23.0 == vec1.dot(3, 1));

        assertTrue(-5.0 == vec1.dot(new Vector2(-1, 1)));
    }

    @Test
    public void testClone() {
        final Vector2 vec1 = new Vector2(0, 0);
        final Vector2 vec2 = vec1.clone();
        assertEquals(vec1, vec2);
        assertNotSame(vec1, vec2);
    }

    @Test
    public void testValid() {
        final Vector2 vec1 = new Vector2(0, 0);
        final Vector2 vec2 = new Vector2(Double.POSITIVE_INFINITY, 0);
        final Vector2 vec3 = new Vector2(0, Double.NEGATIVE_INFINITY);
        final Vector2 vec4 = new Vector2(Double.NaN, 0);
        final Vector2 vec5 = new Vector2(0, Double.NaN);

        assertTrue(Vector2.isValid(vec1));
        assertFalse(Vector2.isValid(vec2));
        assertFalse(Vector2.isValid(vec3));
        assertFalse(Vector2.isValid(vec4));
        assertFalse(Vector2.isValid(vec5));

        vec5.zero();
        assertTrue(Vector2.isValid(vec5));

        assertFalse(Vector2.isValid(null));

        // couple of equals validity tests
        assertEquals(vec1, vec1);
        assertFalse(vec1.equals(null));
        assertFalse(vec1.equals(new Vector3()));

        // throw in a couple pool accesses for coverage
        final Vector2 vec6 = Vector2.fetchTempInstance();
        vec6.set(vec1);
        assertEquals(vec1, vec6);
        assertNotSame(vec1, vec6);
        Vector2.releaseTempInstance(vec6);

        // cover more of equals
        vec1.set(0, 1);
        assertFalse(vec1.equals(new Vector2(0, 2)));
    }

    @Test
    public void testSimpleHash() {
        // Just a simple sanity check.
        final Vector2 vec1 = new Vector2(1, 2);
        final Vector2 vec2 = new Vector2(1, 2);
        final Vector2 vec3 = new Vector2(2, 2);

        assertTrue(vec1.hashCode() == vec2.hashCode());
        assertTrue(vec1.hashCode() != vec3.hashCode());
    }
}
