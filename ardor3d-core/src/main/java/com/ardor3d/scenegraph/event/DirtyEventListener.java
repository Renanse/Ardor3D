/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.scenegraph.event;

import com.ardor3d.scenegraph.Spatial;

/**
 * DirtyEventListener is the interface for objects interested in updates when spatials get marked dirty / clean
 * (updated).
 */
public interface DirtyEventListener {

    /**
     * spatialDirty is called when a spatial is changed in respect to transform, bounding, attach/dettach or renderstate
     * 
     * @return true if the event should be consumed and not continue up the scenegraph.
     */
    boolean spatialDirty(Spatial spatial, DirtyType dirtyType);

    /**
     * spatialClean is called when a spatial is changed in respect to transform, bounding, attach/dettach or renderstate
     * 
     * @return true if the event should be consumed and not continue up the scenegraph.
     */
    boolean spatialClean(Spatial spatial, DirtyType dirtyType);
}
