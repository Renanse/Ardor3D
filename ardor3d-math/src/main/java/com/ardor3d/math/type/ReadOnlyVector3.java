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

import com.ardor3d.math.Vector3;

public interface ReadOnlyVector3 {

  double getX();

  double getY();

  double getZ();

  float getXf();

  float getYf();

  float getZf();

  double getValue(int index);

  Vector3 add(double x, double y, double z, Vector3 store);

  Vector3 add(ReadOnlyVector3 source, Vector3 store);

  Vector3 subtract(double x, double y, double z, Vector3 store);

  Vector3 subtract(ReadOnlyVector3 source, Vector3 store);

  Vector3 multiply(double scalar, Vector3 store);

  Vector3 multiply(ReadOnlyVector3 scale, Vector3 store);

  Vector3 multiply(double x, double y, double z, Vector3 store);

  Vector3 divide(double scalar, Vector3 store);

  Vector3 divide(ReadOnlyVector3 scale, Vector3 store);

  Vector3 divide(double x, double y, double z, Vector3 store);

  Vector3 scaleAdd(double scale, ReadOnlyVector3 add, Vector3 store);

  Vector3 negate(Vector3 store);

  Vector3 normalize(Vector3 store);

  Vector3 lerp(ReadOnlyVector3 endVec, double scalar, Vector3 store);

  double length();

  double lengthSquared();

  double distanceSquared(double x, double y, double z);

  double distanceSquared(ReadOnlyVector3 destination);

  double distance(double x, double y, double z);

  double distance(ReadOnlyVector3 destination);

  double dot(double x, double y, double z);

  double dot(ReadOnlyVector3 vec);

  Vector3 cross(double x, double y, double z, Vector3 store);

  Vector3 cross(ReadOnlyVector3 vec, Vector3 store);

  double smallestAngleBetween(ReadOnlyVector3 otherVector);

  double[] toArray(double[] store);

  Vector3 clone();

  boolean equals(ReadOnlyVector3 v, double epsilon);
}
