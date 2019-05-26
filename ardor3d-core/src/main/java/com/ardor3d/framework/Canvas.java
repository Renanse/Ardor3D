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
import com.ardor3d.math.type.ReadOnlyColorRGBA;

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

    /**
     * Add a listener to this canvas.
     *
     * @param listener
     */
    void addListener(ICanvasListener listener);

    /**
     * Remove a listener from this canvas.
     *
     * @param listener
     * @return true if the listener was removed.
     */
    boolean removeListener(ICanvasListener listener);

    /**
     * @return the width, in pixels, of the content portion of this canvas (i.e. without any chrome.)
     */
    int getContentWidth();

    /**
     * @return the height, in pixels, of the content portion of this canvas (i.e. without any chrome.)
     */
    int getContentHeight();

    /**
     * Set the background color of this canvas, assuming the canvas has a valid CanvasRenderer set.
     *
     * @param color
     *            the new background color to set.
     * @throws NullPointerException
     *             if no CanvasRenderer is set on this Canvas.
     */
    default void setBackgroundColor(final ReadOnlyColorRGBA color) {
        getCanvasRenderer().getRenderer().setBackgroundColor(color);
    }
}
