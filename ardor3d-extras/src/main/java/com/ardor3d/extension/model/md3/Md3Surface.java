/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.model.md3;

import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;

/**
 * Surface of MD3: http://en.wikipedia.org/wiki/MD3_%28file_format%29#Surface
 */
final class Md3Surface {

    /** name */
    String name;
    /** flags */
    int flags;
    /** Number of animation frames. This should match NUM_FRAMES in the MD3 header. */
    int numFrames;
    /**
     * Number of Shader objects defined in this Surface, with a limit of MD3_MAX_SHADERS. Current value of
     * MD3_MAX_SHADERS is 256.
     */
    int numShaders;
    /** Number of Vertex objects defined in this Surface, up to MD3_MAX_VERTS. Current value of MD3_MAX_VERTS is 4096. */
    int numVerts;
    /**
     * Number of Triangle objects defined in this Surface, maximum of MD3_MAX_TRIANGLES. Current value of
     * MD3_MAX_TRIANGLES is 8192.
     */
    int numTriangles;
    /** Relative offset from SURFACE_START where the list of Triangle objects starts. */
    int offsetTriangles;
    /** Relative offset from SURFACE_START where the list of Shader objects starts. */
    int offsetShaders;
    /** Relative offset from SURFACE_START where the list of ST objects (s-t texture coordinates) starts. */
    int offsetTexCoord;
    /** Relative offset from SURFACE_START where the list of Vertex objects (X-Y-Z-N vertices) starts. */
    int offsetXyzNormal;
    /** Relative offset from SURFACE_START to where the Surface object ends. */
    int offsetEnd;
    /**  */
    int[] triIndexes;
    /**  */
    Vector2[] texCoords;
    /**  */
    Vector3[][] verts;
    /**  */
    Vector3[][] norms;

    Md3Surface() {
        super();
    }
}
