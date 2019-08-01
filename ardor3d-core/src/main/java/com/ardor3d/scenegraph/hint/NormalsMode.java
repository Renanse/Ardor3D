/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.scenegraph.hint;

public enum NormalsMode {

    /**
     * Do whatever our parent does. If no parent, we'll default to {@link #NormalizeIfScaled}
     */
    Inherit,

    /**
     * Send through the normals currently set as-is.
     */
    UseProvided,

    /**
     * Tell the card to normalize any normals data we might give it.
     */
    AlwaysNormalize,

    /**
     * If a scale other than 1,1,1 is being used then tell the card to normalize any normals data we might give it.
     */
    NormalizeIfScaled,

    /**
     * Do not send normal data to the card, even if we have some.
     */
    Off;
}
