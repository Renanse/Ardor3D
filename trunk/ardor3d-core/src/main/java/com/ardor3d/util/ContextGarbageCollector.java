/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util;

import com.ardor3d.renderer.Renderer;
import com.ardor3d.scenegraph.AbstractBufferData;
import com.ardor3d.util.scenegraph.DisplayListDelegate;

public class ContextGarbageCollector {

    private ContextGarbageCollector() {}

    /**
     * Handle detecting and scheduling cleanup of OpenGL assets. This method will place delete calls on the task queue
     * of appropriate RenderContexts when an asset such as a Texture is determined to no longer be reachable by Java.
     * 
     * @param immediateDelete
     *            an optional Renderer to use for immediate cleanup when the asset is owned by the current context. In
     *            general this is best used in single context applications, and null is a perfectly acceptable value.
     */
    public static void doRuntimeCleanup(final Renderer immediateDelete) {
        TextureManager.cleanExpiredTextures(immediateDelete, null);
        AbstractBufferData.cleanExpiredVBOs(immediateDelete);
        DisplayListDelegate.cleanExpiredDisplayLists(immediateDelete);
    }

    /**
     * Handle cleanup of all open OpenGL assets. This method is meant to be used on application shutdown.
     * 
     * @param immediateDelete
     *            an optional Renderer to use for immediate cleanup when the asset is owned by the current context. In
     *            general this is best used in single context applications, and null is a perfectly acceptable value.
     *            However, if there is more than one context or null was passed, you must have all of the contexts
     *            process at least one more (empty) frame to allow for the final gl calls to be processed.
     */
    public static void doFinalCleanup(final Renderer immediateDelete) {
        TextureManager.cleanAllTextures(immediateDelete, null);
        AbstractBufferData.cleanAllVBOs(immediateDelete);
        DisplayListDelegate.cleanAllDisplayLists(immediateDelete);
    }
}
