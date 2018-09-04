/**
 * Copyright (c) 2008-2018 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.material.uniform;

public enum RenderStateProperty {
    /** A Float4 color pulled from Mesh._defaultColor. */
    MeshDefaultColorRGBA,

    /** A Float3 color pulled from Mesh._defaultColor. */
    MeshDefaultColorRGB,

    /** A Float3 vector pulled from current camera's position field. */
    CurrentCameraLocation,

    /**
     * A Float3 vector pulled from rgb color of a light in the current LightState. The light index comes from the extra
     * field of the uniform, which must be of type Integer. Returns 0,0,0 if no light in this position.
     */
    LightColorRGB,

    /**
     * A Float4 vector pulled from rgba color of a light in the current LightState. The light index comes from the extra
     * field of the uniform, which must be of type Integer. Returns 0,0,0,0 if no light in this position.
     */
    LightColorRGBA,

    /**
     * A Float3 vector pulled from position of a light in the current LightState. The light index comes from the extra
     * field of the uniform, which must be of type Integer. Returns 0,0,0 if no light in this position.
     */
    LightPosition,

}
