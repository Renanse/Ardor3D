/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.terrain.providers.awt;

import com.ardor3d.math.type.ReadOnlyVector4;

public interface ElementUpdateListener {
    void elementUpdated(ReadOnlyVector4 oldBounds, ReadOnlyVector4 newBounds);
}
