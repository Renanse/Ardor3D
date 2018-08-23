/**
 * Copyright (c) 2008-2018 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer;

import com.ardor3d.math.type.ReadOnlyRectangle2;

public interface IScissorUtils {

    /**
     * Add the given rectangle to the clip stack, clipping the rendering area by the given rectangle intersected with
     * any existing scissor entries. Enable clipping if this is the first rectangle in the stack.
     *
     * @param rectangle
     */
    void pushClip(ReadOnlyRectangle2 rectangle);

    /**
     * Push a clip onto the stack indicating that future clips should not intersect with clips prior to this one.
     * Basically this allows you to ignore prior clips for nested drawing. Make sure you pop this using
     * {@link #popClip()}.
     */
    void pushEmptyClip();

    /**
     * Pop the most recent rectangle from the stack and adjust the rendering area accordingly.
     */
    void popClip();

    /**
     * Clear all rectangles from the clip stack and disable clipping.
     */
    void clearClips();

    /**
     * @param enabled
     *            toggle clipping without effecting the current clip stack.
     */
    void setClipTestEnabled(boolean enabled);

    /**
     * @return true if the renderer believes clipping is enabled
     */
    boolean isClipTestEnabled();

}
