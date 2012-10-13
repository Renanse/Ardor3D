/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scenegraph;

import com.ardor3d.renderer.Renderer;

/**
 * Renderable is the interface for objects that can be rendered.
 */
public interface Renderable {
    /**
     * Render the object using the supplied renderer instance.
     * 
     * @param renderer
     */
    void render(Renderer renderer);
}
