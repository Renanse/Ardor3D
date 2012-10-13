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

import com.ardor3d.math.Rectangle2;

public interface ReadOnlyRectangle2 {

    /**
     * @return the x coordinate of the origin of this rectangle.
     */
    int getX();

    /**
     * @return the y coordinate of the origin of this rectangle.
     */
    int getY();

    /**
     * @return the width of this rectangle.
     */
    int getWidth();

    /**
     * @return the height of this rectangle.
     */
    int getHeight();

    /**
     * @return a new instance of Rectangle2 with the same value as this object.
     */
    Rectangle2 clone();
}
