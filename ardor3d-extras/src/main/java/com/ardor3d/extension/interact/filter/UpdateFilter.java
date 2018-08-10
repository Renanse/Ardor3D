/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.interact.filter;

import com.ardor3d.extension.interact.InteractManager;
import com.ardor3d.extension.interact.data.SpatialState;

/**
 * Filter used to modify {@link SpatialState} information prior to it being applied to a Spatial by the
 * {@link InteractManager}.
 */
public interface UpdateFilter {

    /**
     * Called after a successful application of mouse/key input.
     * 
     * @param manager
     */
    void applyFilter(InteractManager manager);

    /**
     * Callback for when a control begins a drag operation.
     * 
     * @param manager
     */
    void beginDrag(InteractManager manager);

    /**
     * Callback for when a control ends a drag operation.
     * 
     * @param manager
     */
    void endDrag(InteractManager manager);

}
