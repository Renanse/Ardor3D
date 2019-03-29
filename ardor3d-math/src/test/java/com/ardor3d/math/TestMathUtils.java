package com.ardor3d.math;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TestMathUtils {

    @Rule public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testAcos() {
        if (MathConstants.useFastMath) {
            Assert.assertEquals(Float.NaN, MathUtils.acos(-2.0f), 0.01f);
            Assert.assertEquals(Float.NaN, MathUtils.acos(2.0f), 0.0f);
            Assert.assertEquals(1.57f, MathUtils.acos(0.0f), 0.01f);
            Assert.assertEquals(1.047f, MathUtils.acos(0.5f), 0.01f);
            Assert.assertEquals(Float.NaN, MathUtils.acos(Float.POSITIVE_INFINITY), 0.0f);
        } else {
            Assert.assertEquals(Float.NaN, MathUtils.acos(-2.0f), 0.01f);
            Assert.assertEquals(Float.NaN, MathUtils.acos(2.0f), 0.0f);
            Assert.assertEquals(1.57f, MathUtils.acos(0.0f), 0.01f);
            Assert.assertEquals(1.047f, MathUtils.acos(0.5f), 0.001f);
            Assert.assertEquals(Float.NaN, MathUtils.acos(Float.POSITIVE_INFINITY), 0.0f);
        }
    }

    @Test
    public void testAsin() {
        if (MathConstants.useFastMath) {
            Assert.assertEquals(Float.NaN, MathUtils.asin(-2.0f), 0.01f);
            Assert.assertEquals(Float.NaN, MathUtils.asin(2.0f), 0.0f);
            Assert.assertEquals(0.0f, MathUtils.asin(0.0f), 0.01f);
            Assert.assertEquals(0.5247f, MathUtils.asin(0.5f), 0.001f);
            Assert.assertEquals(0.8480f, MathUtils.asin(0.75f), 0.001f);
            Assert.assertEquals(-0.2515f, MathUtils.asin(-0.25f), 0.001f);
        } else {
            Assert.assertEquals(Float.NaN, MathUtils.asin(-2.0f), 0.01f);
            Assert.assertEquals(Float.NaN, MathUtils.asin(2.0f), 0.0f);
            Assert.assertEquals(0.0f, MathUtils.asin(0.0f), 0.01f);
            Assert.assertEquals(0.5235f, MathUtils.asin(0.5f), 0.001f);
            Assert.assertEquals(0.8480f, MathUtils.asin(0.75f), 0.001f);
            Assert.assertEquals(-0.2526f, MathUtils.asin(-0.25f), 0.001f);
        }
    }

    @Test
    public void testAtan() {
        if (MathConstants.useFastMath) {
            Assert.assertEquals(-1.103, MathUtils.atan(-2.0f), 0.001f);
            Assert.assertEquals(1.103, MathUtils.atan(2.0f), 0.001f);
            Assert.assertEquals(0.0f, MathUtils.atan(0.0f), 0.0f);
            Assert.assertEquals(0.4671f, MathUtils.atan(0.5f), 0.001f);
            Assert.assertEquals(0.6476f, MathUtils.atan(0.75f), 0.001f);
            Assert.assertEquals(-0.2449f, MathUtils.atan(-0.25f), 0.001f);
        } else {
            Assert.assertEquals(-1.107, MathUtils.atan(-2.0f), 0.001f);
            Assert.assertEquals(1.107, MathUtils.atan(2.0f), 0.001f);
            Assert.assertEquals(0.0f, MathUtils.atan(0.0f), 0.0f);
            Assert.assertEquals(0.4636f, MathUtils.atan(0.5f), 0.001f);
            Assert.assertEquals(0.6435f, MathUtils.atan(0.75f), 0.001f);
            Assert.assertEquals(-0.2449f, MathUtils.atan(-0.25f), 0.001f);
        }
    }

    @Test
    public void testCartesianToSpherical() {
        final Vector3 cartCoords = new Vector3(-5.48, 1.24, -4.02);
        final Vector3 store = new Vector3(0.0, 0.0, 0.0);

        if (MathConstants.useFastMath) {
            final Vector3 spherical = MathUtils.cartesianToSpherical(cartCoords, store);

            Assert.assertEquals(store, spherical);

            Assert.assertNotNull(spherical);
            Assert.assertEquals(6.902, spherical.getX(), 0.001);
            Assert.assertEquals(3.778, spherical.getY(), 0.001);
            Assert.assertEquals(0.180, spherical.getZ(), 0.001);
        } else {
            final Vector3 spherical = MathUtils.cartesianToSpherical(cartCoords, store);

            Assert.assertEquals(store, spherical);

            Assert.assertNotNull(spherical);
            Assert.assertEquals(6.908, spherical.getX(), 0.001);
            Assert.assertEquals(3.774, spherical.getY(), 0.001);
            Assert.assertEquals(0.180, spherical.getZ(), 0.001);
        }
    }

    @Test
    public void testCartesianToSpherical_noStore() {
        final Vector3 cartCoords = new Vector3(-5.48, 1.24, -4.02);

        if (MathConstants.useFastMath) {
            final Vector3 spherical = MathUtils.cartesianToSpherical(cartCoords, null);

            Assert.assertNotNull(spherical);
            Assert.assertEquals(6.902, spherical.getX(), 0.001);
            Assert.assertEquals(3.778, spherical.getY(), 0.001);
            Assert.assertEquals(0.180, spherical.getZ(), 0.001);
        } else {
            final Vector3 spherical = MathUtils.cartesianToSpherical(cartCoords, null);

            Assert.assertNotNull(spherical);
            Assert.assertEquals(6.908, spherical.getX(), 0.001);
            Assert.assertEquals(3.774, spherical.getY(), 0.001);
            Assert.assertEquals(0.180, spherical.getZ(), 0.001);
        }
    }

    @Test
    public void testCartesianToSpherical_NaN() {
        final Vector3 cartCoords = new Vector3(Double.NaN, 2.5, 1.1);

        if (MathConstants.useFastMath) {
            final Vector3 spherical = MathUtils.cartesianToSpherical(cartCoords, null);

            Assert.assertNotNull(spherical);
            Assert.assertEquals(Double.NaN, spherical.getX(), 0.0);
            Assert.assertEquals(Double.NaN, spherical.getY(), 0.0);
            Assert.assertEquals(Double.NaN, spherical.getZ(), 0.0);
        } else {
            final Vector3 spherical = MathUtils.cartesianToSpherical(cartCoords, null);

            Assert.assertNotNull(spherical);
            Assert.assertEquals(Double.NaN, spherical.getX(), 0.0);
            Assert.assertEquals(Double.NaN, spherical.getY(), 0.0);
            Assert.assertEquals(Double.NaN, spherical.getZ(), 0.0);
        }
    }

    @Test
    public void testCartesianZToSpherical() {
        final Vector3 cartCoords = new Vector3(-5.48, 1.24, -4.02);
        final Vector3 store = new Vector3(0.0, 0.0, 0.0);

        if (MathConstants.useFastMath) {
            final Vector3 spherical = MathUtils.cartesianZToSpherical(cartCoords, store);

            Assert.assertEquals(store, spherical);

            Assert.assertNotNull(spherical);
            Assert.assertEquals(6.902, spherical.getX(), 0.001);
            Assert.assertEquals(0.179, spherical.getY(), 0.001);
            Assert.assertEquals(3.778, spherical.getZ(), 0.001);
        } else {
            final Vector3 spherical = MathUtils.cartesianZToSpherical(cartCoords, store);

            Assert.assertEquals(store, spherical);

            Assert.assertNotNull(spherical);
            Assert.assertEquals(6.908, spherical.getX(), 0.001);
            Assert.assertEquals(0.180, spherical.getY(), 0.001);
            Assert.assertEquals(3.774, spherical.getZ(), 0.001);
        }
    }

    @Test
    public void testCartesianZToSpherical_noStore() {
        final Vector3 cartCoords = new Vector3(-5.48, 1.24, -4.02);

        if (MathConstants.useFastMath) {
            final Vector3 spherical = MathUtils.cartesianZToSpherical(cartCoords, null);

            Assert.assertNotNull(spherical);
            Assert.assertEquals(6.902, spherical.getX(), 0.001);
            Assert.assertEquals(0.179, spherical.getY(), 0.001);
            Assert.assertEquals(3.778, spherical.getZ(), 0.001);
        } else {
            final Vector3 spherical = MathUtils.cartesianZToSpherical(cartCoords, null);

            Assert.assertNotNull(spherical);
            Assert.assertEquals(6.908, spherical.getX(), 0.001);
            Assert.assertEquals(0.180, spherical.getY(), 0.001);
            Assert.assertEquals(3.774, spherical.getZ(), 0.001);
        }
    }

    @Test
    public void testClamp_int() {
        final int min = 3;
        final int max = 10;

        Assert.assertEquals(3, MathUtils.clamp(1, min, max), 0.0);
        Assert.assertEquals(5, MathUtils.clamp(5, min, max), 0.0);
        Assert.assertEquals(10, MathUtils.clamp(10, min, max), 0.0);
        Assert.assertEquals(10, MathUtils.clamp(15, min, max), 0.0);
    }

    @Test
    public void testClamp_float() {
        final float min = 3.0f;
        final float max = 10.0f;

        Assert.assertEquals(3.0f, MathUtils.clamp(1.0f, min, max), 0.0);
        Assert.assertEquals(5.0f, MathUtils.clamp(5.0f, min, max), 0.0);
        Assert.assertEquals(10.0f, MathUtils.clamp(10.0f, min, max), 0.0);
        Assert.assertEquals(10.0f, MathUtils.clamp(15.0f, min, max), 0.0);
    }

    @Test
    public void testClamp_double() {
        final double min = 3.0d;
        final double max = 10.0d;

        Assert.assertEquals(3.0d, MathUtils.clamp(1.0d, min, max), 0.0);
        Assert.assertEquals(5.0d, MathUtils.clamp(5.0d, min, max), 0.0);
        Assert.assertEquals(10.0d, MathUtils.clamp(10.0d, min, max), 0.0);
        Assert.assertEquals(10.0d, MathUtils.clamp(15.0d, min, max), 0.0);
    }

    @Test
    public void testCos() {
        Assert.assertEquals(Double.NaN, MathUtils.cos(Double.NaN), 0.0);
        Assert.assertEquals(-0.455f, MathUtils.cos(-4.24), 0.001);
        Assert.assertEquals(0.283f, MathUtils.cos(5.0f), 0.001);
    }

    @Test
    public void testFloor() {
        Assert.assertEquals(4, MathUtils.floor(4.3));
        Assert.assertEquals(0, MathUtils.floor(-0.0));
        Assert.assertEquals(0, MathUtils.floor(0));
        Assert.assertEquals(-3, MathUtils.floor(-2.3f));
    }

    @Test
    public void testInverseSqrt() {
        if (MathConstants.useFastMath) {
            Assert.assertEquals(0.316, MathUtils.inverseSqrt(10), 0.001);
            Assert.assertEquals(0.198, MathUtils.inverseSqrt(25.3), 0.001);
            Assert.assertEquals(1.4404511704572167e154, MathUtils.inverseSqrt(0), 0.001);
            Assert.assertEquals(Float.POSITIVE_INFINITY, MathUtils.inverseSqrt(-15), 0.0);
        } else {
            Assert.assertEquals(0.316, MathUtils.inverseSqrt(10), 0.001);
            Assert.assertEquals(0.198, MathUtils.inverseSqrt(25.3), 0.001);
            Assert.assertEquals(Float.POSITIVE_INFINITY, MathUtils.inverseSqrt(0), 0.0);
            Assert.assertEquals(Float.NaN, MathUtils.inverseSqrt(-15), 0.0);
        }
    }

    @Test
    public void testIsPowerOfTwo() {
     Assert.assertEquals(false, MathUtils.isPowerOfTwo(3));
     Assert.assertEquals(false, MathUtils.isPowerOfTwo(0));
     Assert.assertEquals(true, MathUtils.isPowerOfTwo(32));
     Assert.assertEquals(true, MathUtils.isPowerOfTwo(512));
    }

    @Test
    public void testLerp() {
        Assert.assertEquals(15, MathUtils.lerp(10, 15, 15), 0.0f);
        Assert.assertEquals(165, MathUtils.lerp(10, 15, 30), 0.0f);
        Assert.assertEquals(1700, MathUtils.lerp(17, 0, 100), 0.0f);
        Assert.assertEquals(343.35, MathUtils.lerp(31.5, 0, 10.9), 0.0f);
        Assert.assertEquals(1450, MathUtils.lerp(25, -50, 10), 0.0f);
        Assert.assertEquals(20d, MathUtils.lerp(25d, 20d, 20d), 0.0);
    }

    @Test
    public void testLog() {
        Assert.assertEquals(Double.NaN, MathUtils.log(0.0, 0.0), 0.0);
        Assert.assertEquals(1.089, MathUtils.log(15, 12), 0.001);
        Assert.assertEquals(0.917, MathUtils.log(12, 15), 0.001);
    }

    @Test
    public void testModuloPositive() {
        Assert.assertEquals(2_146_467_444, MathUtils.moduloPositive(-1_016_204, -2_147_483_648));
        Assert.assertEquals(3.51, MathUtils.moduloPositive(-4.59, 4.05), 0.0f);
        Assert.assertEquals(2.7f, MathUtils.moduloPositive(2.7f, 4.94f), 0.0f);
        Assert.assertEquals(2, MathUtils.moduloPositive(2, 6));
        Assert.assertEquals(0, MathUtils.moduloPositive(0, 1));
    }

    @Test
    public void testNearestPowerOfTwo() {
        Assert.assertEquals(128, MathUtils.nearestPowerOfTwo(110));
        Assert.assertEquals(32768, MathUtils.nearestPowerOfTwo(32767));
        Assert.assertEquals(2147483647, MathUtils.nearestPowerOfTwo(1_145_864_192));
    }

    @Test
    public void testPow2() {
        Assert.assertEquals(1, MathUtils.pow2(-2));
        Assert.assertEquals(262144, MathUtils.pow2(50));
        Assert.assertEquals(8, MathUtils.pow2(3));
        Assert.assertEquals(32, MathUtils.pow2(5));
        Assert.assertEquals(2, MathUtils.pow2(1));
    }

    @Test
    public void testRound() {
        Assert.assertEquals(-6, MathUtils.round(-6.5f));
        Assert.assertEquals(7, MathUtils.round(6.5f));
        Assert.assertEquals(7, MathUtils.round(6.8f));
        Assert.assertEquals(6, MathUtils.round(6.3f));
        Assert.assertEquals(7L, MathUtils.round(6.5));
        Assert.assertEquals(7L, MathUtils.round(6.8));
        Assert.assertEquals(6L, MathUtils.round(6.3));
        Assert.assertEquals(6, MathUtils.round(6));
    }

    @Test
    public void testScurve3() {
        Assert.assertEquals(-242.0f, MathUtils.scurve3(5.5f), 0.0f);
        Assert.assertEquals(-17.496, MathUtils.scurve3(2.7), 0.001);
    }

    @Test
    public void testScurve5() {
        Assert.assertEquals(18134.875f, MathUtils.scurve5(5.5f), 0.0f);
        Assert.assertEquals(260.602, MathUtils.scurve5(2.7), 0.001);
    }

    @Test
    public void testSin() {
        if (MathConstants.useFastMath) {
            Assert.assertEquals(0.997, MathUtils.sin(1.5), 0.001);
            Assert.assertEquals(0.479, MathUtils.sin(0.5), 0.001);
            Assert.assertEquals(0.0, MathUtils.sin(0.0), 0.0);
        } else {
            Assert.assertEquals(0.997, MathUtils.sin(1.5), 0.001);
            Assert.assertEquals(0.479, MathUtils.sin(0.5), 0.001);
            Assert.assertEquals(0.0, MathUtils.sin(0.0), 0.0);
        }
    }

    @Test
    public void testSphericalToCartesian() {
        final Vector3 sphereCoords = new Vector3(1.17, 2.53, -8.34);
        final Vector3 store = new Vector3(0.0, 0.0, 0.0);

        if (MathConstants.useFastMath) {
            final Vector3 cartesian = MathUtils.sphericalToCartesian(sphereCoords, store);

            Assert.assertEquals(store, cartesian);

            Assert.assertNotNull(cartesian);
            Assert.assertEquals(0.447, cartesian.getX(), 0.001);
            Assert.assertEquals(-1.035, cartesian.getY(), 0.001);
            Assert.assertEquals(-0.313, cartesian.getZ(), 0.001);
        } else {
            final Vector3 cartesian = MathUtils.sphericalToCartesian(sphereCoords, store);

            Assert.assertEquals(store, cartesian);

            Assert.assertNotNull(cartesian);
            Assert.assertEquals(0.447, cartesian.getX(), 0.001);
            Assert.assertEquals(-1.035, cartesian.getY(), 0.001);
            Assert.assertEquals(-0.313, cartesian.getZ(), 0.001);
        }
    }

    @Test
    public void testSphericalToCartesian_noStore() {
        final Vector3 sphereCoords = new Vector3(1.17, 2.53, -8.34);

        if (MathConstants.useFastMath) {
            final Vector3 cartesian = MathUtils.sphericalToCartesian(sphereCoords, null);

            Assert.assertNotNull(cartesian);
            Assert.assertEquals(0.447, cartesian.getX(), 0.001);
            Assert.assertEquals(-1.035, cartesian.getY(), 0.001);
            Assert.assertEquals(-0.313, cartesian.getZ(), 0.001);
        } else {
            final Vector3 cartesian = MathUtils.sphericalToCartesian(sphereCoords, null);

            Assert.assertNotNull(cartesian);
            Assert.assertEquals(0.447, cartesian.getX(), 0.001);
            Assert.assertEquals(-1.035, cartesian.getY(), 0.001);
            Assert.assertEquals(-0.313, cartesian.getZ(), 0.001);
        }
    }

    @Test
    public void testSphericalToCartesianZ() {
        final Vector3 sphereCoords = new Vector3(1.17, 2.53, -8.34);
        final Vector3 store = new Vector3(0.0, 0.0, 0.0);

        if (MathConstants.useFastMath) {
            final Vector3 cartesian = MathUtils.sphericalToCartesianZ(sphereCoords, store);

            Assert.assertEquals(store, cartesian);

            Assert.assertNotNull(cartesian);
            Assert.assertEquals(0.447, cartesian.getX(), 0.001);
            Assert.assertEquals(-0.313, cartesian.getY(), 0.001);
            Assert.assertEquals(-1.035, cartesian.getZ(), 0.001);
        } else {
            final Vector3 cartesian = MathUtils.sphericalToCartesianZ(sphereCoords, store);

            Assert.assertEquals(store, cartesian);

            Assert.assertNotNull(cartesian);
            Assert.assertEquals(0.447, cartesian.getX(), 0.001);
            Assert.assertEquals(-0.313, cartesian.getY(), 0.001);
            Assert.assertEquals(-1.034, cartesian.getZ(), 0.001);
        }
    }

    @Test
    public void testSphericalToCartesianz_noStore() {
        final Vector3 sphereCoords = new Vector3(1.17, 2.53, -8.34);

        if (MathConstants.useFastMath) {
            final Vector3 cartesian = MathUtils.sphericalToCartesianZ(sphereCoords, null);

            Assert.assertNotNull(cartesian);
            Assert.assertEquals(0.447, cartesian.getX(), 0.001);
            Assert.assertEquals(-0.313, cartesian.getY(), 0.001);
            Assert.assertEquals(-1.035, cartesian.getZ(), 0.001);
        } else {
            final Vector3 cartesian = MathUtils.sphericalToCartesianZ(sphereCoords, null);

            Assert.assertNotNull(cartesian);
            Assert.assertEquals(0.447, cartesian.getX(), 0.001);
            Assert.assertEquals(-0.313, cartesian.getY(), 0.001);
            Assert.assertEquals(-1.034, cartesian.getZ(), 0.001);
        }
    }

    @Test
    public void testSqrt() {
        if (MathConstants.useFastMath) {
            Assert.assertEquals(1.224, MathUtils.sqrt(1.5), 0.001);
            Assert.assertEquals(3.496, MathUtils.sqrt(12.24), 0.001);
            Assert.assertEquals(0.0, MathUtils.sqrt(0.0), 0.0);
            Assert.assertEquals(Float.NEGATIVE_INFINITY, MathUtils.sqrt(-1.5), 0.0);
        } else {
            Assert.assertEquals(1.224, MathUtils.sqrt(1.5), 0.001);
            Assert.assertEquals(3.498, MathUtils.sqrt(12.24), 0.001);
            Assert.assertEquals(0.0, MathUtils.sqrt(0.0), 0.0);
            Assert.assertEquals(Float.NaN, MathUtils.sqrt(-1.5), 0.0);
        }
    }

    @Test
    public void testTan() {
        if (MathConstants.useFastMath) {
            Assert.assertEquals(1.558, MathUtils.tan(1.0), 0.001);
            Assert.assertEquals(19.522, MathUtils.tan(10.35), 0.001);
            Assert.assertEquals(11.311, MathUtils.tan(-5.2), 0.001);
            Assert.assertEquals(0.0, MathUtils.tan(0), 0.001);
        } else {
            Assert.assertEquals(1.557, MathUtils.tan(1.0), 0.001);
            Assert.assertEquals(1.327, MathUtils.tan(10.35), 0.001);
            Assert.assertEquals(1.885, MathUtils.tan(-5.2), 0.001);
            Assert.assertEquals(0.0, MathUtils.tan(0), 0.001);
        }
    }

    @Test
    public void testMatrixFrustum() {
        Matrix4 store = new Matrix4();
        MathUtils.matrixFrustum(5, 3, 4, 7, 5, 2, store);
        Assert.assertTrue(store.equals(new Matrix4(
                -5.0, 0.0, 0.0, 0.0,
                0.0, 3.3333333333333335, 0.0, 0.0,
                -4.0, 3.6666666666666665, 2.3333333333333335, -1.0,
                0.0, 0.0, 6.666666666666667, 0.0)));
    }

    @Test
    public void testMatrixOrtho() {
        Matrix4 store = new Matrix4();
        MathUtils.matrixOrtho(5, 3, 4, 7, 5, 2, store);
        Assert.assertTrue(store.equals(new Matrix4(
                -1.0, 0.0, 0.0, 0.0,
                0.0, 0.6666666666666666, 0.0, 0.0,
                0.0, 0.0, 0.6666666666666666, 0.0,
                4.0, -3.6666666666666665, 2.3333333333333335, 1.0)));
    }

    @Test
    public void testMatrixPerspective() {
        Matrix4 store = new Matrix4();

        MathUtils.matrixPerspective(5, 3, 4, 7, store);

        if (MathConstants.useFastMath) {
            Assert.assertTrue(store.equals(new Matrix4(
                    7.720758213374329, 0.0, 0.0, 0.0,
                    0.0, 23.162274640122988, 0.0, 0.0,
                    0.0, 0.0, -3.6666666666666665, -1.0,
                    0.0, 0.0, -18.666666666666668, 0.0)));
        } else {
            Assert.assertTrue(store.equals(new Matrix4(
                    7.634588516143733, 0.0, 0.0, 0.0,
                    0.0, 22.9037655484312, 0.0, 0.0,
                    0.0, 0.0, -3.6666666666666665, -1.0,
                    0.0, 0.0, -18.666666666666668, 0.0)));
        }
    }

    @Test
    public void testMatrixLookAt_matrix3() {
        Matrix3 store = new Matrix3();
        Vector3 position = new Vector3();
        Vector3 target = new Vector3(5, 2, 4);
        Vector3 worldUp = new Vector3(1, 2, 3);

        MathUtils.matrixLookAt(position, target, worldUp, store);

        if (MathConstants.useFastMath) {
            Assert.assertTrue(store.equals(new Matrix3(
                    -0.14536047473762695, -0.6491360364674628, -0.7442830632365753,
                    -0.7994826110569481, 0.5193088291739701, -0.2977132252946301,
                    0.5814418989505077, 0.5517656309973433, -0.5954264505892602)));
        } else {
            Assert.assertTrue(store.equals(new Matrix3(
                    -0.14547859349066156, -0.6506000486323555, -0.7453559924999299,
                    -0.8001322641986388, 0.5204800389058843, -0.29814239699997197,
                    0.5819143739626463, 0.5530100413375022, -0.5962847939999439)));
        }
    }

    @Test
    public void testMatrixLookAt_matrix4() {
        Matrix4 store = new Matrix4();
        Vector3 position = new Vector3();
        Vector3 target = new Vector3(5, 2, 4);
        Vector3 worldUp = new Vector3(1, 2, 3);

        MathUtils.matrixLookAt(position, target, worldUp, store);

        if (MathConstants.useFastMath) {
            Assert.assertTrue(store.equals(new Matrix4(
                    -0.14536047473762695, -0.6491360364674628, -0.7442830632365753, 0.0,
                    -0.7994826110569481, 0.5193088291739701, -0.2977132252946301, 0.0,
                    0.5814418989505077, 0.5517656309973433, -0.5954264505892602, 0.0,
                    0.0, 0.0, 0.0, 1.0)));
        } else {
            Assert.assertTrue(store.equals(new Matrix4(
                    -0.14547859349066156, -0.6506000486323555, -0.7453559924999299, 0.0,
                    -0.8001322641986388, 0.5204800389058843, -0.29814239699997197, 0.0,
                    0.5819143739626463, 0.5530100413375022, -0.5962847939999439, 0.0,
                    0.0, 0.0, 0.0, 1.0)));
        }
    }
}
