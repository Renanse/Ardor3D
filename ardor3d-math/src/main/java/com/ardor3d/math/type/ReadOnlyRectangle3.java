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

import com.ardor3d.math.Rectangle3;
import com.ardor3d.math.Vector3;

public interface ReadOnlyRectangle3 {

  ReadOnlyVector3 getA();

  ReadOnlyVector3 getB();

  ReadOnlyVector3 getC();

  Vector3 random(Vector3 result);

  Rectangle3 clone();
}
