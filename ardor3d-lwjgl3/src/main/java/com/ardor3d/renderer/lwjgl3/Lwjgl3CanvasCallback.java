/**
 * Copyright (c) 2008-2018 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.lwjgl3;

public interface Lwjgl3CanvasCallback {

    /**
     * Request this canvas as the current opengl owner.
     */
    void makeCurrent(boolean force);

    /**
     * Release this canvas as the current opengl owner.
     */
    void releaseContext(boolean force);

    void doSwap();

}
