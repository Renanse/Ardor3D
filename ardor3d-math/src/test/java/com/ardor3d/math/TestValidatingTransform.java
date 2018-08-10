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

public class TestValidatingTransform {

    @Test
    public void testConstructor() {
        final ValidatingTransform vt1 = new ValidatingTransform();
        assertEquals(Transform.IDENTITY, vt1);

        vt1.translate(0, 1, 2);
        vt1.setRotation(new Matrix3().fromAngleAxis(Math.PI, Vector3.UNIT_X));

        final ValidatingTransform vt2 = new ValidatingTransform(vt1);
        assertEquals(vt1, vt2);
    }

    @Test(expected = InvalidTransformException.class)
    public void failConstructor() {
        final Transform bad = new Transform();
        bad.translate(Double.NaN, 1, 2);
        new ValidatingTransform(Transform.IDENTITY); // good
        new ValidatingTransform(bad); // bad
    }

    @Test(expected = InvalidTransformException.class)
    public void testSetRotationReadOnlyMatrix3() {
        final ValidatingTransform vt1 = new ValidatingTransform();
        final Matrix3 rotation = new Matrix3();
        vt1.setRotation(rotation); // good
        rotation.setM00(Double.NaN);
        vt1.setRotation(rotation); // bad
    }

    @Test(expected = InvalidTransformException.class)
    public void testSetRotationReadOnlyQuaternion() {
        final ValidatingTransform vt1 = new ValidatingTransform();
        final Quaternion rotation = new Quaternion();
        vt1.setRotation(rotation); // good
        rotation.setX(Double.NaN);
        vt1.setRotation(rotation); // bad
    }

    @Test(expected = InvalidTransformException.class)
    public void testSetTranslationReadOnlyVector3() {
        final ValidatingTransform vt1 = new ValidatingTransform();
        vt1.setTranslation(new Vector3(0, 0, 1)); // good
        vt1.setTranslation(new Vector3(0, 0, Double.NaN)); // bad
    }

    @Test(expected = InvalidTransformException.class)
    public void testSetTranslationDoubleDoubleDouble() {
        final ValidatingTransform vt1 = new ValidatingTransform();
        vt1.setTranslation(0, 0, 1); // good
        vt1.setTranslation(0, 0, Double.NaN); // bad
    }

    @Test(expected = InvalidTransformException.class)
    public void testSetScaleReadOnlyVector3() {
        final ValidatingTransform vt1 = new ValidatingTransform();
        vt1.setScale(new Vector3(1, 1, 1)); // good
        vt1.setScale(new Vector3(1, 1, Double.NaN)); // bad
    }

    @Test(expected = InvalidTransformException.class)
    public void testSetScaleDoubleDoubleDouble() {
        final ValidatingTransform vt1 = new ValidatingTransform();
        vt1.setScale(0, 0, 1); // good
        vt1.setScale(0, 0, Double.NaN); // bad
    }

    @Test(expected = InvalidTransformException.class)
    public void testSetScaleDouble() {
        final ValidatingTransform vt1 = new ValidatingTransform();
        vt1.setScale(1); // good
        vt1.setScale(Double.NaN); // bad
    }

    @Test(expected = InvalidTransformException.class)
    public void testSet() {
        final Transform bad = new Transform();
        bad.translate(Double.NaN, 1, 2);
        final ValidatingTransform vt1 = new ValidatingTransform();
        vt1.set(Transform.IDENTITY); // good
        vt1.set(bad); // bad
    }

    @Test(expected = InvalidTransformException.class)
    public void testTranslateDoubleDoubleDouble() {
        final ValidatingTransform vt1 = new ValidatingTransform();
        vt1.translate(1, 2, 3); // good
        vt1.translate(0, 0, Double.NaN); // bad
    }

    @Test(expected = InvalidTransformException.class)
    public void testTranslateReadOnlyVector3() {
        final ValidatingTransform vt1 = new ValidatingTransform();
        vt1.translate(new Vector3(1, 2, 3)); // good
        vt1.translate(new Vector3(0, 0, Double.NaN)); // bad
    }

    @Test(expected = InvalidTransformException.class)
    public void testMultiply() {
        final ValidatingTransform vt1 = new ValidatingTransform();
        vt1.multiply(Transform.IDENTITY, null); // good
        final Transform bad = new Transform();
        bad.translate(Double.NaN, 1, 2);
        vt1.multiply(bad, null); // bad
    }

    @Test(expected = InvalidTransformException.class)
    public void testInvert() {
        final ValidatingTransform vt1 = new ValidatingTransform();
        vt1.setScale(2);
        vt1.invert(null); // good
        // a little chicanery to get around other checks.
        ((Vector3) vt1.getScale()).setX(0);
        vt1.invert(null); // bad
    }

    @Test(expected = InvalidTransformException.class)
    public void testFromHomogeneousMatrix() {
        final ValidatingTransform vt1 = new ValidatingTransform();
        final Matrix4 matrix = new Matrix4();
        vt1.fromHomogeneousMatrix(matrix); // good
        matrix.setM00(Double.NaN);
        vt1.fromHomogeneousMatrix(matrix); // bad
    }

    @Test
    public void testClone() {
        final ValidatingTransform trans1 = new ValidatingTransform();
        final ValidatingTransform trans2 = trans1.clone();
        assertEquals(trans1, trans2);
        assertNotSame(trans1, trans2);
    }

}
