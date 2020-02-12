/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
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

import com.ardor3d.math.type.ReadOnlyVector3;

public class TestQuaternion {

    @Test
    public void testGetSet() {
        final Quaternion quat1 = new Quaternion();
        assertEquals(Quaternion.IDENTITY, quat1);
        assertTrue(quat1.isIdentity());

        quat1.setX(1);
        assertTrue(quat1.getX() == 1.0);
        quat1.setX(Double.POSITIVE_INFINITY);
        assertTrue(quat1.getX() == Double.POSITIVE_INFINITY);
        quat1.setX(Double.NEGATIVE_INFINITY);
        assertTrue(quat1.getX() == Double.NEGATIVE_INFINITY);

        quat1.setY(1);
        assertTrue(quat1.getY() == 1.0);
        quat1.setY(Double.POSITIVE_INFINITY);
        assertTrue(quat1.getY() == Double.POSITIVE_INFINITY);
        quat1.setY(Double.NEGATIVE_INFINITY);
        assertTrue(quat1.getY() == Double.NEGATIVE_INFINITY);

        quat1.setZ(1);
        assertTrue(quat1.getZ() == 1.0);
        quat1.setZ(Double.POSITIVE_INFINITY);
        assertTrue(quat1.getZ() == Double.POSITIVE_INFINITY);
        quat1.setZ(Double.NEGATIVE_INFINITY);
        assertTrue(quat1.getZ() == Double.NEGATIVE_INFINITY);

        quat1.setW(1);
        assertTrue(quat1.getW() == 1.0);
        quat1.setW(Double.POSITIVE_INFINITY);
        assertTrue(quat1.getW() == Double.POSITIVE_INFINITY);
        quat1.setW(Double.NEGATIVE_INFINITY);
        assertTrue(quat1.getW() == Double.NEGATIVE_INFINITY);

        quat1.set(Math.PI, Math.PI, Math.PI, Math.PI);
        assertTrue(quat1.getXf() == (float) Math.PI);
        assertTrue(quat1.getYf() == (float) Math.PI);
        assertTrue(quat1.getZf() == (float) Math.PI);
        assertTrue(quat1.getWf() == (float) Math.PI);

        final Quaternion quat2 = new Quaternion();
        quat2.set(quat1);
        assertEquals(quat1, quat2);
    }

    @Test
    public void testToArray() {
        final Quaternion quat1 = new Quaternion();
        quat1.set(Math.PI, Double.MAX_VALUE, 42, -1);
        final double[] array = quat1.toArray(null);
        final double[] array2 = quat1.toArray(new double[4]);
        assertNotNull(array);
        assertTrue(array.length == 4);
        assertTrue(array[0] == Math.PI);
        assertTrue(array[1] == Double.MAX_VALUE);
        assertTrue(array[2] == 42);
        assertTrue(array[3] == -1);
        assertNotNull(array2);
        assertTrue(array2.length == 4);
        assertTrue(array2[0] == Math.PI);
        assertTrue(array2[1] == Double.MAX_VALUE);
        assertTrue(array2[2] == 42);
        assertTrue(array2[3] == -1);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testBadArray() {
        final Quaternion quat = new Quaternion();
        quat.toArray(new double[2]);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testBadAxesArray() {
        final Quaternion quat = new Quaternion();
        quat.toAxes(new Vector3[2]);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testBadEuler1() {
        new Quaternion().fromEulerAngles(new double[2]);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testBadEuler2() {
        final Quaternion quat = new Quaternion();
        quat.toEulerAngles(new double[2]);
    }

    @Test
    public void testEulerAngles() {
        final Quaternion quat = new Quaternion().fromEulerAngles(new double[] { MathUtils.HALF_PI, 0, 0 });
        assertTrue(1.0 == quat.magnitude());
        assertTrue(Math.abs(Vector3.NEG_UNIT_Z.distance(quat.apply(Vector3.UNIT_X, null))) <= MathUtils.EPSILON);

        quat.fromEulerAngles(0, -MathUtils.HALF_PI, 0);
        assertTrue(1.0 == quat.magnitude());
        assertTrue(Math.abs(Vector3.NEG_UNIT_Y.distance(quat.apply(Vector3.UNIT_X, null))) <= MathUtils.EPSILON);

        quat.fromEulerAngles(0, 0, MathUtils.HALF_PI);
        assertTrue(1.0 == quat.magnitude());
        assertTrue(Math.abs(Vector3.UNIT_Z.distance(quat.apply(Vector3.UNIT_Y, null))) <= MathUtils.EPSILON);

        quat.fromEulerAngles(0, MathUtils.HALF_PI, 0);
        double[] angles = quat.toEulerAngles(null);
        final Quaternion quat2 = new Quaternion().fromEulerAngles(angles);
        assertEquals(quat, quat2);
        quat.fromEulerAngles(0, -MathUtils.HALF_PI, 0);
        angles = quat.toEulerAngles(null);
        quat2.fromEulerAngles(angles);
        assertEquals(quat, quat2);
        quat.fromEulerAngles(0, 0, MathUtils.HALF_PI);
        angles = quat.toEulerAngles(null);
        quat2.fromEulerAngles(angles);
        assertEquals(quat, quat2);
    }

    @Test
    public void testMatrix3() {
        double a = MathUtils.HALF_PI;
        final Quaternion quat = new Quaternion();
        quat.fromRotationMatrix( //
                1, 0, 0, //
                0, Math.cos(a), -Math.sin(a), //
                0, Math.sin(a), Math.cos(a));

        assertTrue(Math.abs(Vector3.UNIT_Z.distance(quat.apply(Vector3.UNIT_Y, null))) <= MathUtils.EPSILON);
        final Matrix3 mat = quat.toRotationMatrix((Matrix3) null);
        assertTrue(Math.abs(quat.apply(Vector3.NEG_ONE, null).distance(mat.applyPost(Vector3.NEG_ONE, null))) <= MathUtils.EPSILON);

        a = MathUtils.PI;
        quat.fromRotationMatrix( //
                1, 0, 0, //
                0, Math.cos(a), -Math.sin(a), //
                0, Math.sin(a), Math.cos(a));

        assertTrue(Math.abs(Vector3.NEG_UNIT_Y.distance(quat.apply(Vector3.UNIT_Y, null))) <= MathUtils.EPSILON);
        quat.toRotationMatrix(mat);
        assertTrue(Math.abs(quat.apply(Vector3.ONE, null).distance(mat.applyPost(Vector3.ONE, null))) <= MathUtils.EPSILON);

        quat.set(0, 0, 0, 0);
        assertEquals(Matrix3.IDENTITY, quat.toRotationMatrix((Matrix3) null));

        a = MathUtils.PI;
        quat.fromRotationMatrix( //
                Math.cos(a), 0, Math.sin(a), //
                0, 1, 0, //
                -Math.sin(a), 0, Math.cos(a));

        assertTrue(Math.abs(Vector3.NEG_UNIT_X.distance(quat.apply(Vector3.UNIT_X, null))) <= MathUtils.EPSILON);
        final Matrix4 mat4 = quat.toRotationMatrix((Matrix4) null);
        assertTrue(Math.abs(quat.apply(Vector3.NEG_ONE, null).distance(mat4.applyPostVector(Vector3.NEG_ONE, null))) <= MathUtils.EPSILON);

        a = MathUtils.PI;
        quat.fromRotationMatrix(new Matrix3(//
                Math.cos(a), -Math.sin(a), 0, //
                Math.sin(a), Math.cos(a), 0, //
                0, 0, 1));

        assertTrue(Math.abs(Vector3.NEG_UNIT_X.distance(quat.apply(Vector3.UNIT_X, null))) <= MathUtils.EPSILON);
        quat.toRotationMatrix(mat4);
        assertTrue(Math.abs(quat.apply(Vector3.ONE, null).distance(mat4.applyPostVector(Vector3.ONE, null))) <= MathUtils.EPSILON);

        quat.set(0, 0, 0, 0);
        assertEquals(Matrix4.IDENTITY, quat.toRotationMatrix((Matrix4) null));
    }

    @Test
    public void testRotations() {
        final double a = MathUtils.QUARTER_PI;
        final Quaternion quat = new Quaternion().fromRotationMatrix(new Matrix3(//
                Math.cos(a), -Math.sin(a), 0, //
                Math.sin(a), Math.cos(a), 0, //
                0, 0, 1));
        final Vector3 column = quat.getRotationColumn(0, null);
        assertTrue(Math.abs(new Vector3(Math.cos(a), Math.sin(a), 0).distance(column)) <= MathUtils.EPSILON);
        quat.getRotationColumn(1, column);
        assertTrue(Math.abs(new Vector3(-Math.sin(a), Math.sin(a), 0).distance(column)) <= MathUtils.EPSILON);
        quat.getRotationColumn(2, column);
        assertTrue(Math.abs(new Vector3(0, 0, 1).distance(column)) <= MathUtils.EPSILON);

        quat.set(0, 0, 0, 0);
        assertEquals(Vector3.UNIT_X, quat.getRotationColumn(0, null));

        // Try a new way with new angles...
        quat.fromEulerAngles(MathUtils.QUARTER_PI, MathUtils.PI, MathUtils.HALF_PI);
        final Vector3 rotated = new Vector3(1, 1, 1);
        quat.apply(rotated, rotated);

        // expected
        final Vector3 expected = new Vector3(1, 1, 1);
        final Quaternion worker = new Quaternion();
        // put together matrix, then apply to vector, so YZX
        worker.applyRotationY(MathUtils.QUARTER_PI);
        worker.applyRotationZ(MathUtils.PI);
        worker.applyRotationX(MathUtils.HALF_PI);
        worker.apply(expected, expected);

        // test how close it came out
        assertTrue(rotated.distance(expected) <= Quaternion.ALLOWED_DEVIANCE);

        // test axis rotation methods against general purpose
        // X AXIS
        expected.set(1, 1, 1);
        rotated.set(1, 1, 1);
        worker.setIdentity().applyRotationX(MathUtils.QUARTER_PI).apply(expected, expected);
        worker.setIdentity().applyRotation(MathUtils.QUARTER_PI, 1, 0, 0).apply(rotated, rotated);
        assertTrue(rotated.distance(expected) <= MathUtils.EPSILON);

        // Y AXIS
        expected.set(1, 1, 1);
        rotated.set(1, 1, 1);
        worker.setIdentity().applyRotationY(MathUtils.QUARTER_PI).apply(expected, expected);
        worker.setIdentity().applyRotation(MathUtils.QUARTER_PI, 0, 1, 0).apply(rotated, rotated);
        assertTrue(rotated.distance(expected) <= MathUtils.EPSILON);

        // Z AXIS
        expected.set(1, 1, 1);
        rotated.set(1, 1, 1);
        worker.setIdentity().applyRotationZ(MathUtils.QUARTER_PI).apply(expected, expected);
        worker.setIdentity().applyRotation(MathUtils.QUARTER_PI, 0, 0, 1).apply(rotated, rotated);
        assertTrue(rotated.distance(expected) <= MathUtils.EPSILON);

        quat.set(worker);
        worker.applyRotation(0, 0, 0, 0);
        assertEquals(quat, worker);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadRotationColumn1() {
        new Quaternion().getRotationColumn(-1, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadRotationColumn2() {
        new Quaternion().getRotationColumn(4, null);
    }

    @Test
    public void testAngleAxis() {
        final Quaternion quat = new Quaternion().fromAngleAxis(MathUtils.HALF_PI, new Vector3(2, 0, 0));
        final Quaternion quat2 = new Quaternion().fromAngleNormalAxis(MathUtils.HALF_PI, new Vector3(1, 0, 0));

        assertEquals(quat2, quat);
        assertTrue(1 - quat.magnitude() <= MathUtils.EPSILON);

        assertEquals(quat.apply(Vector3.ONE, null), quat2.apply(Vector3.ONE, null));
        assertTrue(Math.abs(new Vector3(0, -1, 0).distance(quat.apply(new Vector3(0, 0, 1), null))) <= MathUtils.EPSILON);

        assertEquals(Quaternion.IDENTITY,
                new Quaternion(1, 2, 3, 4).fromAngleAxis(MathUtils.HALF_PI, new Vector3(0, 0, 0)));

        final Vector3 axisStore = new Vector3();
        double angle = quat.toAngleAxis(axisStore);
        assertEquals(quat, new Quaternion().fromAngleAxis(angle, axisStore));

        quat.set(0, 0, 0, 0);
        angle = quat.toAngleAxis(axisStore);
        assertTrue(0.0 == angle);
        assertEquals(Vector3.UNIT_X, axisStore);
    }

    @Test
    public void testFromVectorToVector() {
        final Quaternion quat = new Quaternion().fromVectorToVector(Vector3.UNIT_Z, Vector3.UNIT_X);
        assertEquals(new Quaternion().fromAngleAxis(MathUtils.HALF_PI, Vector3.UNIT_Y), quat);

        quat.fromVectorToVector(Vector3.UNIT_Z, Vector3.NEG_UNIT_Z);
        assertTrue(Math.abs(new Vector3(0, 0, -1).distance(quat.apply(new Vector3(0, 0, 1), null))) <= Quaternion.ALLOWED_DEVIANCE);

        quat.fromVectorToVector(Vector3.UNIT_X, Vector3.NEG_UNIT_X);
        assertTrue(Math.abs(new Vector3(-1, 0, 0).distance(quat.apply(new Vector3(1, 0, 0), null))) <= Quaternion.ALLOWED_DEVIANCE);

        quat.fromVectorToVector(Vector3.UNIT_Y, Vector3.NEG_UNIT_Y);
        assertTrue(Math.abs(new Vector3(0, -1, 0).distance(quat.apply(new Vector3(0, 1, 0), null))) <= Quaternion.ALLOWED_DEVIANCE);

        quat.fromVectorToVector(Vector3.ONE, Vector3.NEG_ONE);
        assertTrue(Math.abs(new Vector3(-1, -1, -1).distance(quat.apply(new Vector3(1, 1, 1), null))) <= Quaternion.ALLOWED_DEVIANCE);

        quat.fromVectorToVector(Vector3.ZERO, Vector3.ZERO);
        assertEquals(Quaternion.IDENTITY, quat);
    }

    @Test
    public void testNormalize() {
        final Quaternion quat = new Quaternion(0, 1, 2, 3);
        final Quaternion quat2 = quat.normalize(null);
        assertEquals(quat2, quat.normalizeLocal());
        assertTrue(Math.abs(1 - quat.magnitude()) <= MathUtils.EPSILON);
        assertTrue(Math.abs(1 - quat2.magnitude()) <= MathUtils.EPSILON);
    }

    @Test
    public void testApplyToZero() {
        assertEquals(Vector3.ZERO, new Quaternion().apply(new Vector3(0, 0, 0), null));
    }

    @Test
    public void testInvert() {
        final Quaternion quat1 = new Quaternion(0, 1, 2, 3);
        final Quaternion quat2 = quat1.invert(null);
        assertEquals(Quaternion.IDENTITY, quat1.multiply(quat2, null));
        assertEquals(quat1, quat2.invert(new Quaternion()));
        assertEquals(quat1, quat2.invertLocal());

        // normalized version
        quat1.fromAngleAxis(MathUtils.QUARTER_PI, Vector3.UNIT_Y);
        quat1.invert(quat2);
        assertEquals(Quaternion.IDENTITY, quat1.multiply(quat2, null));
        assertEquals(quat1, quat2.invert(new Quaternion()));
        assertEquals(quat1, quat2.invertLocal());

        // conjugate check
        assertEquals(new Quaternion(-1, -2, -3, 4), new Quaternion(1, 2, 3, 4).conjugate(null));
    }

    @Test
    public void testAddSubtract() {
        final Quaternion quat1 = new Quaternion(0, 1, 2, 3);
        final Quaternion quat2 = new Quaternion(1, 1, 1, 1);
        assertEquals(new Quaternion(1, 2, 3, 4), quat1.add(quat2, null));
        assertEquals(new Quaternion(1, 2, 3, 4), quat1.add(quat2, new Quaternion()));
        assertEquals(new Quaternion(1, 2, 3, 4), quat1.addLocal(quat2));

        quat1.set(0, 1, 2, 3);
        quat2.set(1, 1, 1, 1);
        assertEquals(new Quaternion(-1, 0, 1, 2), quat1.subtract(quat2, null));
        assertEquals(new Quaternion(-1, 0, 1, 2), quat1.subtract(quat2, new Quaternion()));
        assertEquals(new Quaternion(-1, 0, 1, 2), quat1.subtractLocal(quat2));
    }

    @Test
    public void testMultiply() {
        final Quaternion quat1 = new Quaternion(0.5, 1, 2, 3);
        final Quaternion quat2 = new Quaternion();
        assertEquals(new Quaternion(1, 2, 4, 6), quat1.multiply(2, null));
        assertEquals(new Quaternion(2, 4, 8, 12), quat1.multiply(4, quat2));
        assertEquals(new Quaternion(1, 2, 4, 6), quat1.multiplyLocal(2));

        quat1.fromAngleNormalAxis(MathUtils.QUARTER_PI, Vector3.UNIT_Y);
        quat1.multiply(quat1, quat2);

        final ReadOnlyVector3 vec = Vector3.UNIT_Z;
        assertTrue(Math.abs(Vector3.UNIT_X.distance(quat2.apply(vec, null))) <= Quaternion.ALLOWED_DEVIANCE);
        quat1.multiplyLocal(quat1.getX(), quat1.getY(), quat1.getZ(), quat1.getW());
        assertTrue(Math.abs(Vector3.UNIT_X.distance(quat1.apply(vec, null))) <= Quaternion.ALLOWED_DEVIANCE);
        quat2.fromAngleNormalAxis(MathUtils.HALF_PI, Vector3.UNIT_Y);
        quat1.multiplyLocal(quat2);
        assertTrue(Math.abs(Vector3.NEG_UNIT_Z.distance(quat1.apply(vec, null))) <= Quaternion.ALLOWED_DEVIANCE);

        quat1.multiplyLocal(new Matrix3().applyRotationY(MathUtils.HALF_PI));
        assertTrue(Math.abs(Vector3.NEG_UNIT_X.distance(quat1.apply(vec, null))) <= Quaternion.ALLOWED_DEVIANCE);
    }

    @Test
    public void testAxes() {
        final Matrix3 rot = new Matrix3().applyRotationX(MathUtils.QUARTER_PI).applyRotationY(MathUtils.HALF_PI);
        final Quaternion quat1 = new Quaternion().fromAxes(rot.getColumn(0, null), rot.getColumn(1, null),
                rot.getColumn(2, null));
        final Quaternion quat2 = new Quaternion().fromRotationMatrix(rot);
        assertEquals(quat2, quat1);

        final Vector3[] axes = quat1.toAxes(new Vector3[3]);
        quat1.fromAxes(axes[0], axes[1], axes[2]);
        assertEquals(quat2, quat1);
    }

    @Test
    public void testSlerp() {
        final Quaternion quat = new Quaternion();
        final Quaternion quat2 = new Quaternion().applyRotationY(MathUtils.HALF_PI);
        final Quaternion store = quat.slerp(quat2, .5, null);
        assertTrue(Math.abs(new Vector3(Math.sin(MathUtils.QUARTER_PI), 0, Math.sin(MathUtils.QUARTER_PI))
                .distance(store.apply(Vector3.UNIT_Z, null))) <= Quaternion.ALLOWED_DEVIANCE);

        // delta == 100%
        quat2.setIdentity().applyRotationZ(MathUtils.PI);
        quat.slerp(quat2, 1.0, store);
        assertTrue(Math.abs(new Vector3(-1, 0, 0).distance(store.apply(Vector3.UNIT_X, null))) <= Quaternion.ALLOWED_DEVIANCE);

        quat2.setIdentity().applyRotationZ(MathUtils.PI);
        quat.slerp(quat2, .5, store);
        assertTrue(Math.abs(new Vector3(0, 1, 0).distance(store.apply(Vector3.UNIT_X, null))) <= Quaternion.ALLOWED_DEVIANCE);

        // delta == 0%
        quat2.setIdentity().applyRotationZ(MathUtils.PI);
        quat.slerp(quat2, 0, store);
        assertTrue(Math.abs(new Vector3(1, 0, 0).distance(store.apply(Vector3.UNIT_X, null))) <= Quaternion.ALLOWED_DEVIANCE);

        // a==b
        quat2.setIdentity();
        quat.slerp(quat2, 0.25, store);
        assertTrue(Math.abs(new Vector3(1, 0, 0).distance(store.apply(Vector3.UNIT_X, null))) <= Quaternion.ALLOWED_DEVIANCE);

        // negative dot product
        quat.setIdentity().applyRotationX(-2 * MathUtils.HALF_PI);
        quat2.setIdentity().applyRotationX(MathUtils.HALF_PI);
        quat.slerp(quat2, 0.5, store);
        assertTrue(Math.abs(new Vector3(0, -Math.sin(MathUtils.QUARTER_PI), Math.sin(MathUtils.QUARTER_PI))
                .distance(store.apply(Vector3.UNIT_Y, null))) <= Quaternion.ALLOWED_DEVIANCE);

        // LOCAL
        // delta == 100%
        quat2.setIdentity().applyRotationX(MathUtils.PI);
        quat.slerpLocal(quat2, 1.0);
        assertTrue(Math.abs(new Vector3(0, -1, 0).distance(quat.apply(Vector3.UNIT_Y, null))) <= Quaternion.ALLOWED_DEVIANCE);

        quat.setIdentity();
        quat.slerpLocal(quat2, .5);
        assertTrue(Math.abs(new Vector3(0, 0, 1).distance(quat.apply(Vector3.UNIT_Y, null))) <= Quaternion.ALLOWED_DEVIANCE);

        // delta == 0%
        quat.setIdentity();
        quat.slerpLocal(quat2, 0);
        assertTrue(Math.abs(new Vector3(0, 1, 0).distance(quat.apply(Vector3.UNIT_Y, null))) <= Quaternion.ALLOWED_DEVIANCE);

        // a==b
        quat.setIdentity();
        quat2.setIdentity();
        quat.slerpLocal(quat2, 0.25);
        assertTrue(Math.abs(new Vector3(0, 1, 0).distance(quat.apply(Vector3.UNIT_Y, null))) <= Quaternion.ALLOWED_DEVIANCE);

        // negative dot product
        quat.setIdentity().applyRotationX(-2 * MathUtils.HALF_PI);
        quat2.setIdentity().applyRotationX(MathUtils.HALF_PI);
        quat.slerpLocal(quat2, 0.5);
        assertTrue(Math.abs(new Vector3(0, -Math.sin(MathUtils.QUARTER_PI), Math.sin(MathUtils.QUARTER_PI))
                .distance(quat.apply(Vector3.UNIT_Y, null))) <= Quaternion.ALLOWED_DEVIANCE);
    }

    @Test
    public void testLookAt() {
        final Vector3 direction = new Vector3(-1, 0, 0);
        final Quaternion quat = new Quaternion().lookAt(direction, Vector3.UNIT_Y);
        assertTrue(Math.abs(direction.distance(quat.apply(Vector3.UNIT_Z, null))) <= Quaternion.ALLOWED_DEVIANCE);

        direction.set(1, 1, 1).normalizeLocal();
        quat.lookAt(direction, Vector3.UNIT_Y);
        assertTrue(Math.abs(direction.distance(quat.apply(Vector3.UNIT_Z, null))) <= Quaternion.ALLOWED_DEVIANCE);

        direction.set(-1, 2, -1).normalizeLocal();
        quat.lookAt(direction, Vector3.UNIT_Y);
        assertTrue(Math.abs(direction.distance(quat.apply(Vector3.UNIT_Z, null))) <= Quaternion.ALLOWED_DEVIANCE);
    }

    @Test
    public void testDot() {
        final Quaternion quat = new Quaternion(7, 2, 5, -1);
        assertTrue(35.0 == quat.dot(3, 1, 2, -2));

        assertTrue(-11.0 == quat.dot(new Quaternion(-1, 1, -1, 1)));
    }

    @Test
    public void testClone() {
        final Quaternion quat1 = new Quaternion();
        final Quaternion quat2 = quat1.clone();
        assertEquals(quat1, quat2);
        assertNotSame(quat1, quat2);
    }

    @Test
    public void testValid() {
        final Quaternion quat = new Quaternion();
        assertTrue(Quaternion.isValid(quat));

        quat.set(Double.NaN, 0, 0, 0);
        assertFalse(Quaternion.isValid(quat));
        quat.set(0, Double.NaN, 0, 0);
        assertFalse(Quaternion.isValid(quat));
        quat.set(0, 0, Double.NaN, 0);
        assertFalse(Quaternion.isValid(quat));
        quat.set(0, 0, 0, Double.NaN);
        assertFalse(Quaternion.isValid(quat));

        quat.set(Double.NEGATIVE_INFINITY, 0, 0, 0);
        assertFalse(Quaternion.isValid(quat));
        quat.set(0, Double.NEGATIVE_INFINITY, 0, 0);
        assertFalse(Quaternion.isValid(quat));
        quat.set(0, 0, Double.NEGATIVE_INFINITY, 0);
        assertFalse(Quaternion.isValid(quat));
        quat.set(0, 0, 0, Double.NEGATIVE_INFINITY);
        assertFalse(Quaternion.isValid(quat));

        quat.setIdentity();
        assertTrue(Quaternion.isValid(quat));

        assertFalse(Quaternion.isValid(null));

        // couple of equals validity tests
        assertEquals(quat, quat);
        assertTrue(quat.strictEquals(quat));
        assertFalse(quat.equals(null));
        assertFalse(quat.strictEquals(null));
        assertFalse(quat.equals(new Vector2()));
        assertFalse(quat.strictEquals(new Vector2()));

        // throw in a couple pool accesses for coverage
        final Quaternion quatTemp = Quaternion.fetchTempInstance();
        quatTemp.set(quat);
        assertEquals(quat, quatTemp);
        assertNotSame(quat, quatTemp);
        Quaternion.releaseTempInstance(quatTemp);

        // cover more of equals
        quat.set(0, 1, 2, 3);
        final Quaternion comp = new Quaternion(-1, -1, -1, -1);
        assertFalse(quat.equals(comp));
        assertFalse(quat.strictEquals(comp));
        comp.setX(0);
        assertFalse(quat.equals(comp));
        assertFalse(quat.strictEquals(comp));
        comp.setY(1);
        assertFalse(quat.equals(comp));
        assertFalse(quat.strictEquals(comp));
        comp.setZ(2);
        assertFalse(quat.equals(comp));
        assertFalse(quat.strictEquals(comp));
        comp.setW(3);
        assertEquals(quat, comp);
        assertTrue(quat.strictEquals(comp));
    }

    @Test
    public void testSimpleHash() {
        // Just a simple sanity check.
        final Quaternion quat1 = new Quaternion(1, 2, 3, 4);
        final Quaternion quat2 = new Quaternion(1, 2, 3, 4);
        final Quaternion quat3 = new Quaternion(1, 2, 3, 0);

        assertTrue(quat1.hashCode() == quat2.hashCode());
        assertTrue(quat1.hashCode() != quat3.hashCode());
    }

}
