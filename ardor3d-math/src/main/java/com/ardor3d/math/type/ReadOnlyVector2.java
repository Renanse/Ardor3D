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

import com.ardor3d.math.Vector2;

public interface ReadOnlyVector2 {

    double getX();

    double getY();

    float getXf();

    float getYf();

    double getValue(int index);

    double[] toArray(double[] store);

    Vector2 add(double x, double y, Vector2 store);

    Vector2 add(ReadOnlyVector2 source, Vector2 store);

    Vector2 subtract(double x, double y, Vector2 store);

    Vector2 subtract(ReadOnlyVector2 source, Vector2 store);

    Vector2 multiply(double scalar, Vector2 store);

    Vector2 multiply(ReadOnlyVector2 scale, Vector2 store);

    Vector2 multiply(double x, double y, Vector2 store);

    Vector2 divide(double scalar, Vector2 store);

    Vector2 divide(ReadOnlyVector2 scale, Vector2 store);

    Vector2 divide(double x, double y, Vector2 store);

    Vector2 scaleAdd(double scale, ReadOnlyVector2 add, Vector2 store);

    Vector2 negate(Vector2 store);

    Vector2 normalize(Vector2 store);

    Vector2 rotateAroundOrigin(double angle, boolean clockwise, Vector2 store);

    Vector2 lerp(ReadOnlyVector2 endVec, double scalar, Vector2 store);

    double length();

    double lengthSquared();

    double distanceSquared(double x, double y);

    double distanceSquared(ReadOnlyVector2 destination);

    double distance(double x, double y);

    double distance(ReadOnlyVector2 destination);

    double dot(double x, double y);

    double dot(ReadOnlyVector2 vec);

    double getPolarAngle();

    double angleBetween(ReadOnlyVector2 otherVector);

    double smallestAngleBetween(ReadOnlyVector2 otherVector);

    Vector2 clone();
}
