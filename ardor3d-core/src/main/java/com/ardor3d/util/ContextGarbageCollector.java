/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.util;

import com.ardor3d.renderer.Renderer;
import com.ardor3d.scenegraph.AbstractBufferData;
import com.ardor3d.scenegraph.MeshData;

public class ContextGarbageCollector {

    private ContextGarbageCollector() {}

    /**
     * Handle detecting and scheduling cleanup of OpenGL assets. This method will place delete calls on the task queue
     * of appropriate RenderContexts when an asset such as a Texture is determined to no longer be reachable by Java.
     *
     * @param renderer
     *            an optional Renderer to use for immediate cleanup when the asset is owned by the current context. In
     *            general this is best used in single context applications, and null is a perfectly acceptable value.
     */
    public static void doRuntimeCleanup(final Renderer renderer) {
        TextureManager.cleanExpiredTextures(renderer.getTextureUtils(), null);
        AbstractBufferData.cleanExpiredVBOs(renderer.getShaderUtils());
    }

    /**
     * Handle cleanup of all open OpenGL assets. This method is meant to be used on application shutdown.
     *
     * @param renderer
     *            an optional Renderer to use for immediate cleanup when the asset is owned by the current context. In
     *            general this is best used in single context applications, and null is a perfectly acceptable value.
     *            However, if there is more than one context or null was passed, you must have all of the contexts
     *            process at least one more (empty) frame to allow for the final gl calls to be processed.
     */
    public static void doFinalCleanup(final Renderer renderer) {
        TextureManager.cleanAllTextures(renderer.getTextureUtils(), null);
        AbstractBufferData.cleanAllBuffers(renderer.getShaderUtils());
        MeshData.cleanAllVertexArrays(renderer.getShaderUtils());
    }
}
