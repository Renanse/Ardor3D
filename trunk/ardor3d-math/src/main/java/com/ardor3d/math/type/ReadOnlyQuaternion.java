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

import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Matrix4;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Vector3;

public interface ReadOnlyQuaternion {

    double getX();

    double getY();

    double getZ();

    double getW();

    float getXf();

    float getYf();

    float getZf();

    float getWf();

    double[] toArray(double[] store);

    double[] toEulerAngles(double[] store);

    Matrix3 toRotationMatrix(Matrix3 store);

    Matrix4 toRotationMatrix(Matrix4 store);

    Vector3 getRotationColumn(int index, Vector3 store);

    double toAngleAxis(Vector3 axisStore);

    Quaternion normalize(Quaternion store);

    Quaternion conjugate(Quaternion store);

    Quaternion add(ReadOnlyQuaternion quat, Quaternion store);

    Quaternion subtract(ReadOnlyQuaternion quat, Quaternion store);

    Quaternion multiply(double scalar, Quaternion store);

    Quaternion multiply(ReadOnlyQuaternion quat, Quaternion store);

    Vector3 apply(ReadOnlyVector3 vec, Vector3 store);

    Vector3[] toAxes(Vector3 axes[]);

    Quaternion slerp(ReadOnlyQuaternion endQuat, double changeAmnt, Quaternion store);

    double magnitudeSquared();

    double magnitude();

    double dot(double x, double y, double z, double w);

    double dot(ReadOnlyQuaternion quat);

    boolean isIdentity();

    Quaternion clone();

    boolean strictEquals(Object o);
}
