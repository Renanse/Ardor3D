/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.scenegraph;

import com.ardor3d.annotation.MainThread;
import com.ardor3d.renderer.Renderer;

/**
 * Renderable is the interface for things that can be rendered.
 */
public interface Renderable {
    /**
     * Render the object using the supplied renderer instance.
     *
     * @param renderer
     * @return true if a render occurred
     */
    @MainThread
    boolean render(Renderer renderer);
}
