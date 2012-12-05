
package com.ardor3d.performancetest;

import shared.MatrixBenchmark;
import shared.ResultSample;

import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Matrix4;
import com.ardor3d.math.Transform;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.Vector4;
import com.ardor3d.math.type.ReadOnlyMatrix4;

public class Ardor3DMatrixBenchmark implements MatrixBenchmark {

    @Override
    public void resetRandom(final long seed) {
        MathUtils.setRandomSeed(seed);
    }

    @Override
    public ResultSample doMultTest(final int count, final int maxCount, final long timeOutMS) {
        final Matrix4 m1 = a3dMatrixRandom();
        final Matrix4 m2 = a3dMatrixRandom();
        final long start = System.currentTimeMillis();
        int loopCount = 0;
        while (System.currentTimeMillis() - start < timeOutMS && loopCount != maxCount) {
            ++loopCount;
            for (int i = 0; i < count; ++i) {
                m1.multiply(m2, m1);
            }
        }
        return populateResult(System.currentTimeMillis() - start, loopCount, m1.toArray(null));
    }

    @Override
    public ResultSample doTranslateTest(final int count, final int maxCount, final long timeOutMS) {
        final Matrix4 m1 = a3dMatrixRandom();
        final long start = System.currentTimeMillis();
        int loopCount = 0;
        while (System.currentTimeMillis() - start < timeOutMS && loopCount != maxCount) {
            ++loopCount;
            for (int i = 0; i < count; ++i) {
                m1.applyTranslationPost(1.0, 2.0, 3.0);
            }
        }
        return populateResult(System.currentTimeMillis() - start, loopCount, m1.toArray(null));
    }

    @Override
    public ResultSample doScaleTest(final int count, final int maxCount, final long timeOutMS) {
        final Matrix4 m1 = a3dMatrixRandom();
        final Vector4 vec = new Vector4(1.0, 2.0, 3.0, 1.0);
        final long start = System.currentTimeMillis();
        int loopCount = 0;
        while (System.currentTimeMillis() - start < timeOutMS && loopCount != maxCount) {
            ++loopCount;
            for (int i = 0; i < count; ++i) {
                m1.scaleLocal(vec);
            }
        }
        return populateResult(System.currentTimeMillis() - start, loopCount, m1.toArray(null));
    }

    @Override
    public ResultSample doRotateTest1(final int count, final int maxCount, final long timeOutMS) {
        final Matrix4 m1 = a3dMatrixRandom();
        final Vector3 vec = new Vector3(1.0, 2.0, 3.0).normalizeLocal();
        final double a = MathUtils.HALF_PI;
        final double x = vec.getX(), y = vec.getY(), z = vec.getZ();

        final long start = System.currentTimeMillis();
        int loopCount = 0;
        while (System.currentTimeMillis() - start < timeOutMS && loopCount != maxCount) {
            ++loopCount;
            for (int i = 0; i < count; ++i) {
                m1.applyRotation(a, x, y, z);
            }
        }
        return populateResult(System.currentTimeMillis() - start, loopCount, m1.toArray(null));
    }

    @Override
    public ResultSample doRotateTest2(final int count, final int maxCount, final long timeOutMS) {
        final Matrix4 m1 = a3dMatrixRandom();
        final double a = MathUtils.HALF_PI;

        final long start = System.currentTimeMillis();
        int loopCount = 0;
        while (System.currentTimeMillis() - start < timeOutMS && loopCount != maxCount) {
            ++loopCount;
            for (int i = 0; i < count; ++i) {
                m1.applyRotationX(a);
            }
        }
        return populateResult(System.currentTimeMillis() - start, loopCount, m1.toArray(null));
    }

    @Override
    public ResultSample doTransposeTest(final int count, final int maxCount, final long timeOutMS) {
        final Matrix4 m1 = a3dMatrixRandom();

        final long start = System.currentTimeMillis();
        int loopCount = 0;
        while (System.currentTimeMillis() - start < timeOutMS && loopCount != maxCount) {
            ++loopCount;
            for (int i = 0; i < count; ++i) {
                m1.transposeLocal();
            }
        }
        return populateResult(System.currentTimeMillis() - start, loopCount, m1.toArray(null));
    }

    @Override
    public ResultSample doInverseTest(final int count, final int maxCount, final long timeOutMS) {
        final Matrix4 m1 = new Matrix4();
        final double near = 1.0, far = 1000.0, fovy = 90.0, aspect = 0.5;
        MathUtils.matrixPerspective(fovy, aspect, near, far, m1);

        final long start = System.currentTimeMillis();
        int loopCount = 0;
        while (System.currentTimeMillis() - start < timeOutMS && loopCount != maxCount) {
            ++loopCount;
            for (int i = 0; i < count; ++i) {
                m1.invertLocal();
            }
        }
        return populateResult(System.currentTimeMillis() - start, loopCount, m1.toArray(null));
    }

    @Override
    public ResultSample doInverse3Test(final int count, final int maxCount, final long timeOutMS) {
        final Matrix4 mat = a3dMatrixRandom();
        final Matrix3 m1 = new Matrix3( //
                mat.getM00(), mat.getM01(), mat.getM02(), //
                mat.getM10(), mat.getM11(), mat.getM12(), //
                mat.getM20(), mat.getM21(), mat.getM22());
        mat.setIdentity();

        final long start = System.currentTimeMillis();
        int loopCount = 0;
        while (System.currentTimeMillis() - start < timeOutMS && loopCount != maxCount) {
            ++loopCount;
            for (int i = 0; i < count; ++i) {
                m1.invertLocal();
            }
        }
        return populateResult(System.currentTimeMillis() - start, loopCount, mat.set(m1).toArray(null));
    }

    @Override
    public ResultSample doTransformPointTest(final int count, final int maxCount, final long timeOutMS) {
        final ReadOnlyMatrix4 m1 = a3dMatrixRandom();
        final Vector3 v1 = new Vector3(1, 2, 3);

        final long start = System.currentTimeMillis();
        int loopCount = 0;
        while (System.currentTimeMillis() - start < timeOutMS && loopCount != maxCount) {
            ++loopCount;
            for (int i = 0; i < count; ++i) {
                m1.applyPostPoint(v1, v1);
            }
        }
        return populateResult(System.currentTimeMillis() - start, loopCount, v1.toArray(null));
    }

    @Override
    public ResultSample doTransformMultTest(final int count, final int maxCount, final long timeOutMS) {
        final ReadOnlyMatrix4 m1 = new Matrix4().fromAngleAxis(MathUtils.nextRandomDouble(),
                new Vector3(MathUtils.nextRandomDouble(), MathUtils.nextRandomDouble(), MathUtils.nextRandomDouble()));
        final ReadOnlyMatrix4 m2 = new Matrix4().fromAngleAxis(MathUtils.nextRandomDouble(),
                new Vector3(MathUtils.nextRandomDouble(), MathUtils.nextRandomDouble(), MathUtils.nextRandomDouble()));
        final Transform a = new Transform().fromHomogeneousMatrix(m1);
        final Transform b = new Transform();
        final Transform by = new Transform().fromHomogeneousMatrix(m2);

        final long start = System.currentTimeMillis();
        int loopCount = 0;
        while (System.currentTimeMillis() - start < timeOutMS && loopCount != maxCount) {
            ++loopCount;
            for (int i = 0; i < count; ++i) {
                if (i % 2 == 0) {
                    a.multiply(by, b);
                } else {
                    b.multiply(by, a);
                }
            }
        }
        return populateResult(System.currentTimeMillis() - start, loopCount, a.getHomogeneousMatrix(null).toArray(null));
    }

    @Override
    public ResultSample doTransformVectorTest(final int count, final int maxCount, final long timeOutMS) {
        final ReadOnlyMatrix4 m1 = a3dMatrixRandom();
        final Vector3 v1 = new Vector3(1, 2, 3);

        final long start = System.currentTimeMillis();
        int loopCount = 0;
        while (System.currentTimeMillis() - start < timeOutMS && loopCount != maxCount) {
            ++loopCount;
            for (int i = 0; i < count; ++i) {
                m1.applyPostVector(v1, v1);
            }
        }
        return populateResult(System.currentTimeMillis() - start, loopCount, v1.toArray(null));
    }

    private Matrix4 a3dMatrixRandom() {
        final Matrix4 mat = new Matrix4();
        final Vector3 axis = new Vector3(MathUtils.nextRandomDouble(), MathUtils.nextRandomDouble(),
                MathUtils.nextRandomDouble());
        axis.normalizeLocal();
        mat.applyRotation(MathUtils.nextRandomDouble(), axis.getX(), axis.getY(), axis.getZ());
        mat.applyTranslationPost(MathUtils.nextRandomDouble(), MathUtils.nextRandomDouble(),
                MathUtils.nextRandomDouble());
        return mat;
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
        return "Ardor3D";
    };
}
