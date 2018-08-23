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

public enum UniformSource {
    /** Uniform Value is an nio.Buffer of correct type for the associated UniformType. */
    Value,

    /**
     * Uniform Value is a String key to be used to pull an nio.Buffer from the local properties of the Spatial being
     * drawn.
     */
    SpatialProperty,

    /** Uniform Value is an RenderMatrixType enum value. */
    RendererMatrix,

    /** Uniform Value is of type Function<Mesh, Buffer>. */
    Function,

    /**
     * Uniform Value is an RenderStateProperty enum value, indicating a value to pull from the current Spatial being
     * drawn.
     */
    RenderState,
}
