/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.renderer;

public enum IndexMode {
    // TRIMESH
    /**
     * Every three vertices referenced by the indexbuffer will be considered a stand-alone triangle.
     */
    Triangles(true),
    /**
     * The first three vertices referenced by the indexbuffer create a triangle, from there, every additional vertex is
     * paired with the two preceding vertices to make a new triangle.
     */
    TriangleStrip(true),
    /**
     * The first three vertices (V0, V1, V2) referenced by the indexbuffer create a triangle, from there, every
     * additional vertex is paired with the preceding vertex and the initial vertex (V0) to make a new triangle.
     */
    TriangleFan(true),

    TrianglesAdjacency(true), TriangleStripAdjacency(true),

    // LINE
    /**
     * Every two vertices referenced by the indexbuffer will be considered a stand-alone line segment.
     */
    Lines(false),
    /**
     * The first two vertices referenced by the indexbuffer create a line, from there, every additional vertex is paired
     * with the preceding vertex to make a new, connected line.
     */
    LineStrip(false),
    /**
     * Identical to <i>LineStrip</i> except the final indexed vertex is then connected back to the initial vertex to
     * form a loop.
     */
    LineLoop(false),
    /**
     * Requires 4 vertices, but the first and last vertex are only used to provide information about adjacent lines -
     * they are not drawn.
     */
    LinesAdjacency(false),

    /**
     * Requires 4 vertices, but the first and last vertex are only used to provide information about adjacent lines
     * strip segments - they are not drawn.
     */
    LineStripAdjacency(false),

    // POINT
    /**
     * Identical to <i>Connected</i> except the final indexed vertex is then connected back to the initial vertex to
     * form a loop.
     */
    Points(false);

    private final boolean _hasPolygons;

    private IndexMode(final boolean hasPolygons) {
        _hasPolygons = hasPolygons;
    }

    public boolean hasPolygons() {
        return _hasPolygons;
    }

    public int getVertexCount() {
        switch (this) {
            case Triangles:
            case TriangleStrip:
            case TriangleFan:
            case TrianglesAdjacency:
            case TriangleStripAdjacency:
                return 3;
            case Lines:
            case LineStrip:
            case LineLoop:
            case LinesAdjacency:
            case LineStripAdjacency:
                return 2;
            case Points:
                return 1;
        }
        throw new IllegalArgumentException("Unhandled type: " + this);
    }

    /**
     * @param indexMode
     * @param size
     * @return the number of primitives you would have if you connected an array of points of the given size using the
     *         given index mode.
     */
    public static int getPrimitiveCount(final IndexMode indexMode, final int size) {
        switch (indexMode) {
            case Triangles:
                return size / 3;
            case TriangleFan:
            case TriangleStrip:
            case LineStripAdjacency:
                return size - 2;
            case Lines:
                return size / 2;
            case LineStrip:
                return size - 1;
            case LineLoop:
                return size;
            case Points:
                return size;
            case LinesAdjacency:
                return size / 4;
            case TrianglesAdjacency:
                return size / 6;
            case TriangleStripAdjacency:
                return (size / 2) - 2;

        }

        throw new IllegalArgumentException("unimplemented index mode: " + indexMode);
    }
}
