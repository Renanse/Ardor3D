/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.math.type;

import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;

import com.ardor3d.math.Matrix4;
import com.ardor3d.math.Transform;
import com.ardor3d.math.Vector3;

public interface ReadOnlyTransform {
    ReadOnlyMatrix3 getMatrix();

    ReadOnlyVector3 getTranslation();

    ReadOnlyVector3 getScale();

    boolean isIdentity();

    boolean isRotationMatrix();

    boolean isUniformScale();

    Vector3 applyForward(Vector3 point);

    Vector3 applyForward(ReadOnlyVector3 point, Vector3 store);

    Vector3 applyInverse(Vector3 point);

    Vector3 applyInverse(ReadOnlyVector3 point, Vector3 store);

    Vector3 applyForwardVector(Vector3 vector);

    Vector3 applyForwardVector(ReadOnlyVector3 vector, Vector3 store);

    Vector3 applyInverseVector(Vector3 vector);

    Vector3 applyInverseVector(ReadOnlyVector3 vector, Vector3 store);

    Transform multiply(ReadOnlyTransform transformBy, Transform store);

    Transform invert(Transform store);

    Matrix4 getHomogeneousMatrix(Matrix4 store);

    /**
     * Populates an nio double buffer with data from this transform to use as a model view matrix in OpenGL. This is
     * done as efficiently as possible, not touching positions 3, 7, 11 and 15.
     * 
     * @param store
     *            double buffer to store in. Assumes a size of 16 and that position 3, 7 and 11 are already set as 0.0
     *            and 15 is already 1.0.
     */
    void getGLApplyMatrix(DoubleBuffer store);

    /**
     * Populates an nio float buffer with data from this transform to use as a model view matrix in OpenGL. This is done
     * as efficiently as possible, not touching positions 3, 7, 11 and 15.
     * 
     * @param store
     *            float buffer to store in. Assumes a size of 16 and that position 3, 7 and 11 are already set as 0.0f
     *            and 15 is already 1.0f.
     */
    void getGLApplyMatrix(FloatBuffer store);

    Transform clone();

    boolean strictEquals(Object o);
}
