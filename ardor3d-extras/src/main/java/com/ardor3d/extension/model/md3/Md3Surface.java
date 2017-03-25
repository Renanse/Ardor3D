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

    final int _magic;
    /** name */
    final String _name;
    /** flags */
    final int _flags;
    /** Number of animation frames. This should match NUM_FRAMES in the MD3 header. */
    final int _numFrames;
    /**
     * Number of Shader objects defined in this Surface, with a limit of MD3_MAX_SHADERS. Current value of
     * MD3_MAX_SHADERS is 256.
     */
    final int _numShaders;
    /** Number of Vertex objects defined in this Surface, up to MD3_MAX_VERTS. Current value of MD3_MAX_VERTS is 4096. */
    final int _numVerts;
    /**
     * Number of Triangle objects defined in this Surface, maximum of MD3_MAX_TRIANGLES. Current value of
     * MD3_MAX_TRIANGLES is 8192.
     */
    final int _numTriangles;
    /** Relative offset from SURFACE_START where the list of Triangle objects starts. */
    final int _offsetTriangles;
    /** Relative offset from SURFACE_START where the list of Shader objects starts. */
    final int _offsetShaders;
    /** Relative offset from SURFACE_START where the list of ST objects (s-t texture coordinates) starts. */
    final int _offsetTexCoords;
    /** Relative offset from SURFACE_START where the list of Vertex objects (X-Y-Z-N vertices) starts. */
    final int _offsetXyzNormals;
    /** Relative offset from SURFACE_START to where the Surface object ends. */
    final int _offsetEnd;
    /** Indices of triangles' vertices */
    final int[] _triIndexes;
    /** Texture coordinates of triangles' vertices */
    final Vector2[] _texCoords;
    /** Triangles' vertices */
    final Vector3[][] _verts;
    /** Triangles' normals */
    final Vector3[][] _norms;

    Md3Surface(final int magic, final String name, final int flags, final int numFrames, final int numShaders,
            final int numVerts, final int numTriangles, final int offsetTriangles, final int offsetShaders,
            final int offsetTexCoords, final int offsetXyzNormals, final int offsetEnd) {
        super();
        _magic = magic;
        _name = name;
        _flags = flags;
        _numFrames = numFrames;
        _numShaders = numShaders;
        _numVerts = numVerts;
        _numTriangles = numTriangles;
        _offsetTriangles = offsetTriangles;
        _offsetShaders = offsetShaders;
        _offsetTexCoords = offsetTexCoords;
        _offsetXyzNormals = offsetXyzNormals;
        _offsetEnd = offsetEnd;
        _triIndexes = new int[_numTriangles * 3];
        _texCoords = new Vector2[_numVerts];
        _verts = new Vector3[_numFrames][_numVerts];
        _norms = new Vector3[_numFrames][_numVerts];
    }
}
