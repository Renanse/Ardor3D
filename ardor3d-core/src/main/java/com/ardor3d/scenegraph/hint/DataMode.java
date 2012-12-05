/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scenegraph.hint;

/**
 * Enum used to describe how we prefer data to be sent to the card.
 */
public enum DataMode {
    /**
     * Use our parent's DataMode. If we do not have a parent, Arrays will be used.
     */
    Inherit,

    /**
     * Send each data buffer to the card using vertex arrays.
     */
    Arrays,

    /**
     * Send each data buffer to the card using vertex buffer objects. This is usually faster than Arrays, but may not be
     * supported on older cards. If not supported, Arrays is used by the Renderer.
     */
    VBO,

    /**
     * Send each data buffer to the card using a combined vertex buffer object(s). Usually this is done by combining all
     * FloatBufferData buffers in one buffer sequentially, but if their types are different, multiple buffers might be
     * used instead. This is usually a bit faster than just VBO. If not supported, Arrays is used by the Renderer.
     */
    VBOInterleaved;
}
