
package com.vecmath.performancetest;

import javax.vecmath.AxisAngle4d;
import javax.vecmath.Matrix3d;
import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import javax.vecmath.Tuple3d;
import javax.vecmath.Vector3d;

import shared.MatrixBenchmark;
import shared.ResultSample;

import com.ardor3d.math.MathUtils;

public class VecmathMatrixBenchmark implements MatrixBenchmark {

    @Override
    public void resetRandom(final long seed) {
        MathUtils.setRandomSeed(seed);
    }

    @Override
    public ResultSample doMultTest(final int count, final int maxCount, final long timeOutMS) {
        final Matrix4d m1 = vecMatrixRandom();
        final Matrix4d m2 = vecMatrixRandom();
        final long start = System.currentTimeMillis();
        int loopCount = 0;
        while (System.currentTimeMillis() - start < timeOutMS && loopCount != maxCount) {
            ++loopCount;
            for (int i = 0; i < count; ++i) {
                m1.mul(m1, m2);
            }
        }
        return populateResult(System.currentTimeMillis() - start, loopCount, toArray(m1));
    }

    private double[] toArray(final Matrix4d m1) {
        return new double[] {//
        m1.m00, m1.m01, m1.m02, m1.m03, //
                m1.m10, m1.m11, m1.m12, m1.m13, //
                m1.m20, m1.m21, m1.m22, m1.m23, //
                m1.m30, m1.m31, m1.m32, m1.m33 };
    }

    private double[] toArray(final Tuple3d v1) {
        return new double[] { v1.x, v1.y, v1.z };
    }

    @Override
    public ResultSample doTranslateTest(final int count, final int maxCount, final long timeOutMS) {
        final Matrix4d m1 = vecMatrixRandom();
        final long start = System.currentTimeMillis();
        int loopCount = 0;
        final Vector3d offset = new Vector3d(1.0, 2.0, 3.0);
        while (System.currentTimeMillis() - start < timeOutMS && loopCount != maxCount) {
            ++loopCount;
            for (int i = 0; i < count; ++i) {
                translate(m1, offset.x, offset.y, offset.z);
            }
        }
        return populateResult(System.currentTimeMillis() - start, loopCount, toArray(m1));
    }

    @Override
    public ResultSample doScaleTest(final int count, final int maxCount, final long timeOutMS) {
        vecMatrixRandom();
        return null;
    }

    @Override
    public ResultSample doRotateTest1(final int count, final int maxCount, final long timeOutMS) {
        final Matrix4d m1 = vecMatrixRandom();
        final Matrix4d m2 = new Matrix4d();
        final Vector3d vec = new Vector3d(1.0, 2.0, 3.0);
        vec.normalize();
        final AxisAngle4d rot = new AxisAngle4d(vec, MathUtils.HALF_PI);

        final long start = System.currentTimeMillis();
        int loopCount = 0;
        while (System.currentTimeMillis() - start < timeOutMS && loopCount != maxCount) {
            ++loopCount;
            for (int i = 0; i < count; ++i) {
                // XXX: This is to approximate having a rotation and applying it to a matrix.
                // Don't optimize this by moving m2.set(...) out of loop or it is just a multiply test, and isn't
                // testing the same thing as everyone else.
                m2.set(rot);
                m1.mul(m2);
            }
        }
        return populateResult(System.currentTimeMillis() - start, loopCount, toArray(m1));
    }

    @Override
    public ResultSample doRotateTest2(final int count, final int maxCount, final long timeOutMS) {
        final Matrix4d m1 = vecMatrixRandom();
        final Matrix4d m2 = new Matrix4d();
        final double a = MathUtils.HALF_PI;

        final long start = System.currentTimeMillis();
        int loopCount = 0;
        while (System.currentTimeMillis() - start < timeOutMS && loopCount != maxCount) {
            ++loopCount;
            for (int i = 0; i < count; ++i) {
                // XXX: This is to approximate having a rotation and applying it to a matrix.
                // Don't optimize this by moving m2.set(...) out of loop or it is just a multiply test, and isn't
                // testing the same thing as everyone else.
                m2.rotX(a);
                m1.mul(m2);
            }
        }
        return populateResult(System.currentTimeMillis() - start, loopCount, toArray(m1));
    }

    @Override
    public ResultSample doTransposeTest(final int count, final int maxCount, final long timeOutMS) {
        final Matrix4d m1 = vecMatrixRandom();

        final long start = System.currentTimeMillis();
        int loopCount = 0;
        while (System.currentTimeMillis() - start < timeOutMS && loopCount != maxCount) {
            ++loopCount;
            for (int i = 0; i < count; ++i) {
                m1.transpose();
            }
        }
        return populateResult(System.currentTimeMillis() - start, loopCount, toArray(m1));
    }

    @Override
    public ResultSample doInverseTest(final int count, final int maxCount, final long timeOutMS) {
        final Matrix4d m1 = new Matrix4d();
        final double near = 1.0, far = 1000.0, fovy = 90.0, aspect = 0.5;
        final double height = near * MathUtils.tan(fovy * 0.5 * MathUtils.DEG_TO_RAD);
        final double width = height * aspect;
        final double x = near / width;
        final double y = near / height;
        final double a = 0;
        final double b = 0;
        final double c = -(far + near) / (far - near);
        final double d = -(2.0 * far * near) / (far - near);
        m1.set(new double[] { x, 0.0, 0.0, 0.0, 0.0, y, 0.0, 0.0, a, b, c, -1.0, 0.0, 0.0, d, 0.0 });

        final long start = System.currentTimeMillis();
        int loopCount = 0;
        while (System.currentTimeMillis() - start < timeOutMS && loopCount != maxCount) {
            ++loopCount;
            for (int i = 0; i < count; ++i) {
                m1.invert();
            }
        }
        return populateResult(System.currentTimeMillis() - start, loopCount, toArray(m1));
    }

    @Override
    public ResultSample doInverse3Test(final int count, final int maxCount, final long timeOutMS) {
        final Matrix4d mat = vecMatrixRandom();
        final Matrix3d m1 = new Matrix3d( //
                mat.getElement(0, 0), mat.getElement(0, 1), mat.getElement(0, 2), //
                mat.getElement(1, 0), mat.getElement(1, 1), mat.getElement(1, 2), //
                mat.getElement(2, 0), mat.getElement(2, 1), mat.getElement(2, 2));
        mat.setIdentity();

        final long start = System.currentTimeMillis();
        int loopCount = 0;
        while (System.currentTimeMillis() - start < timeOutMS && loopCount != maxCount) {
            ++loopCount;
            for (int i = 0; i < count; ++i) {
                m1.invert();
            }
        }
        mat.set(m1);
        return populateResult(System.currentTimeMillis() - start, loopCount, toArray(mat));
    }

    @Override
    public ResultSample doTransformPointTest(final int count, final int maxCount, final long timeOutMS) {
        final Matrix4d m1 = vecMatrixRandom();
        final Point3d p1 = new Point3d(1, 2, 3);

        final long start = System.currentTimeMillis();
        int loopCount = 0;
        while (System.currentTimeMillis() - start < timeOutMS && loopCount != maxCount) {
            ++loopCount;
            for (int i = 0; i < count; ++i) {
                m1.transform(p1);
            }
        }
        return populateResult(System.currentTimeMillis() - start, loopCount, toArray(p1));
    }

    @Override
    public ResultSample doTransformVectorTest(final int count, final int maxCount, final long timeOutMS) {
        final Matrix4d m1 = vecMatrixRandom();
        final Vector3d v1 = new Vector3d(1, 2, 3);

        final long start = System.currentTimeMillis();
        int loopCount = 0;
        while (System.currentTimeMillis() - start < timeOutMS && loopCount != maxCount) {
            ++loopCount;
            for (int i = 0; i < count; ++i) {
                m1.transform(v1);
            }
        }
        return populateResult(System.currentTimeMillis() - start, loopCount, toArray(v1));
    }

    @Override
    public ResultSample doTransformMultTest(final int count, final int maxCount, final long timeOutMS) {
        MathUtils.nextRandomDouble();
        MathUtils.nextRandomDouble();
        MathUtils.nextRandomDouble();
        MathUtils.nextRandomDouble();
        MathUtils.nextRandomDouble();
        MathUtils.nextRandomDouble();
        MathUtils.nextRandomDouble();
        MathUtils.nextRandomDouble();
        return null;
    }

    private Matrix4d vecMatrixRandom() {
        final Matrix4d mat = new Matrix4d();
        final Vector3d axis = new Vector3d(MathUtils.nextRandomDouble(), MathUtils.nextRandomDouble(),
                MathUtils.nextRandomDouble());
        axis.normalize();
        mat.set(new AxisAngle4d(axis, MathUtils.nextRandomDouble()));
        final double x = MathUtils.nextRandomDouble();
        final double y = MathUtils.nextRandomDouble();
        final double z = MathUtils.nextRandomDouble();
        translate(mat, x, y, z);
        return mat;
    }

    private void translate(final Matrix4d mat, final double x, final double y, final double z) {
        mat.m03 = mat.m00 * x + mat.m01 * y + mat.m02 * z + mat.m03;
        mat.m13 = mat.m10 * x + mat.m11 * y + mat.m12 * z + mat.m13;
        mat.m23 = mat.m20 * x + mat.m21 * y + mat.m22 * z + mat.m23;
        mat.m33 = mat.m30 * x + mat.m31 * y + mat.m32 * z + mat.m33;
    }

    private ResultSample populateResult(final long time, final int loopCount, final double[] val) {
        final ResultSample rVal = new ResultSample();
        rVal.time = time;
        rVal.loopCount = loopCount;
        rVal.result = val;
        return rVal;
    }

    @Override
    public String getPlatformName() {
        return "Vecmath";
    };
}
