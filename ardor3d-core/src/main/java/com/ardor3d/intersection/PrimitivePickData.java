/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.intersection;

import com.ardor3d.math.Ray3;

/**
 * Pick data for primitive accurate picking including sort by distance to intersection point.
 */
public class PrimitivePickData extends PickData {
  public PrimitivePickData(final Ray3 ray, final Pickable target) {
    super(ray, target, false); // hard coded to false

    _intersectionRecord = target.intersectsPrimitivesWhere(ray);
  }
}
