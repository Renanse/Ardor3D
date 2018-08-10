/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.framework;

import java.util.concurrent.CountDownLatch;

import com.ardor3d.annotation.MainThread;
import com.ardor3d.input.MouseManager;

/**
 * This interface defines the View, and should maybe be called the ViewUpdater. It owns the rendering phase, and
 * controls all interactions with the Renderer.
 */
public interface Canvas {

    /**
     * Do work to initialize this canvas, generally setting up the associated CanvasRenderer, etc.
     */
    @MainThread
    void init();

    /**
     * Ask the canvas to render itself. Note that this may occur in another thread and therefore a latch is given so the
     * caller may know when the draw has completed.
     *
     * @param latch
     *            a counter that should be decremented once drawing has completed.
     */
    @MainThread
    void draw(CountDownLatch latch);

    /**
     * @return the CanvasRenderer associated with this Canvas.
     */
    CanvasRenderer getCanvasRenderer();

    /**
     * @return the MouseManager associated with this Canvas, if any
     */
    MouseManager getMouseManager();

    /**
     * Sets a MouseManager to be associated with this Canvas.
     * 
     * @param manager
     *            the manager to associate
     */
    void setMouseManager(MouseManager manager);
}
