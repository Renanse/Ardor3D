/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.math.type;

import com.ardor3d.math.Vector4;

public interface ReadOnlyVector4 {

  double getX();

  double getY();

  double getZ();

  double getW();

  float getXf();

  float getYf();

  float getZf();

  float getWf();

  double getValue(int index);

  Vector4 add(double x, double y, double z, double w, Vector4 store);

  Vector4 add(ReadOnlyVector4 source, Vector4 store);

  Vector4 subtract(double x, double y, double z, double w, Vector4 store);

  Vector4 subtract(ReadOnlyVector4 source, Vector4 store);

  Vector4 multiply(double scalar, Vector4 store);

  Vector4 multiply(ReadOnlyVector4 scale, Vector4 store);

  Vector4 multiply(double x, double y, double z, double w, Vector4 store);

  Vector4 divide(double scalar, Vector4 store);

  Vector4 divide(ReadOnlyVector4 scale, Vector4 store);

  Vector4 divide(double x, double y, double z, double w, Vector4 store);

  Vector4 scaleAdd(double scale, ReadOnlyVector4 add, Vector4 store);

  Vector4 negate(Vector4 store);

  Vector4 normalize(Vector4 store);

  Vector4 lerp(ReadOnlyVector4 endVec, double scalar, Vector4 store);

  double length();

  double lengthSquared();

  double distanceSquared(double x, double y, double z, double w);

  double distanceSquared(ReadOnlyVector4 destination);

  double distance(double x, double y, double z, double w);

  double distance(ReadOnlyVector4 destination);

  double dot(double x, double y, double z, double w);

  double dot(ReadOnlyVector4 vec);

  double[] toArray(double[] store);

  Vector4 clone();

  boolean equals(ReadOnlyVector4 v, double epsilon);
}
