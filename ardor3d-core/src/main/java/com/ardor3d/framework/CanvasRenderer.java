/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.framework;

import com.ardor3d.annotation.MainThread;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.util.Ardor3dException;

/**
 * Represents a class that knows how to render a scene using a specific Open GL implementation.
 */
public interface CanvasRenderer {
    void init(DisplaySettings settings, boolean doSwap);

    /**
     * Draw the current state of the scene.
     */
    @MainThread
    boolean draw();

    /**
     * Returns the camera being used by this canvas renderer. Modifying the returned {@link Camera} instance effects the
     * view being rendered, so this method can be used to move the camera, etc.
     *
     * @return the camera used by this canvas renderer
     */
    Camera getCamera();

    /**
     * Replaces the camera being used by this canvas renderer.
     *
     * @param camera
     *            the camera to use
     */
    void setCamera(Camera camera);

    /**
     * Returns the scene being used by this canvas renderer.
     *
     * @return the camera used by this canvas renderer
     */
    Scene getScene();

    /**
     * Replaces the scene being used by this canvas renderer.
     *
     * @param scene
     *            the scene to use
     */
    void setScene(Scene scene);

    /**
     * Creates a new renderer for this canvas renderer
     *
     * @return a new renderer
     */
    Renderer createRenderer();

    /**
     * Returns the renderer being used by this canvas renderer.
     *
     * @return the renderer used by this canvas renderer
     */
    Renderer getRenderer();

    /**
     * Have the CanvasRenderer claim the graphics context.
     *
     * @throws Ardor3dException
     *             if we can not claim the context.
     */
    void makeCurrentContext() throws Ardor3dException;

    /**
     * Have the CanvasRenderer release the graphics context.
     */
    void releaseCurrentContext();

    /**
     * @return the Ardor3D RenderContext associated with this CanvasRenderer.
     */
    RenderContext getRenderContext();

    /**
     * @return an int representing the buffers to clear at the start of each frame. Default is
     *         Renderer.BUFFER_COLOR_AND_DEPTH
     */
    int getFrameClear();

    /**
     * @param buffers
     *            an int representing the buffers to clear at the start of each frame. Default is
     *            Renderer.BUFFER_COLOR_AND_DEPTH
     */
    void setFrameClear(final int buffers);

    /**
     * Convenience method for retrieving the CanvasRenderer set on the current RenderContext. Similar to
     * ContextManager.getCurrentContext().getCurrentCanvasRenderer() but with null checks for current context.
     *
     * @return the CanvasRenderer on the current RenderContext.
     */
    public static CanvasRenderer getCurrent() {
        if (ContextManager.getCurrentContext() == null) {
            return null;
        }
        return ContextManager.getCurrentContext().getCurrentCanvasRenderer();
    }

}