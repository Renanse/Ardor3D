/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scenegraph;

import java.io.IOException;
import java.nio.FloatBuffer;

import com.ardor3d.math.MathUtils;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.math.type.ReadOnlyVector2;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.geom.BufferUtils;

public class Line extends Mesh {

    private float _lineWidth;
    private float _miterLimit;

    public Line() {
        this("line");
    }

    /**
     * Constructs a new line with the given name. By default, the line has no geometric data.
     *
     * @param name
     *            The name of the line.
     */
    public Line(final String name) {
        super(name);

        _meshData.setIndexMode(IndexMode.Lines);
        setLineWidth(1.0f);
        setMiterLimit(.75f);
    }

    /**
     * Constructor instantiates a new <code>Line</code> object with a given set of data. Any data can be null except for
     * the vertex list. If vertices are null an exception will be thrown.
     *
     * @param name
     *            the name of the scene element. This is required for identification and comparison purposes.
     * @param vertex
     *            the vertices that make up the lines.
     * @param normal
     *            the normals of the lines.
     * @param color
     *            the color of each point of the lines.
     * @param coords
     *            the texture coordinates of the lines.
     */
    public Line(final String name, final FloatBuffer vertex, final FloatBuffer normal, final FloatBuffer color,
            final FloatBufferData coords) {
        super(name);
        setupData(vertex, normal, color, coords);
        _meshData.setIndexMode(IndexMode.Lines);
    }

    /**
     * Constructor instantiates a new <code>Line</code> object with a given set of data. Any data can be null except for
     * the vertex list. If vertices are null an exception will be thrown.
     *
     * @param name
     *            the name of the scene element. This is required for identification and comparison purposes.
     * @param vertex
     *            the vertices that make up the lines.
     * @param normal
     *            the normals of the lines.
     * @param color
     *            the color of each point of the lines.
     * @param texture
     *            the texture coordinates of the lines.
     */
    public Line(final String name, final ReadOnlyVector3[] vertex, final ReadOnlyVector3[] normal,
            final ReadOnlyColorRGBA[] color, final ReadOnlyVector2[] texture) {
        super(name);
        setupData(BufferUtils.createFloatBuffer(vertex), BufferUtils.createFloatBuffer(normal),
                BufferUtils.createFloatBuffer(color), FloatBufferDataUtil.makeNew(texture));
        _meshData.setIndexMode(IndexMode.Lines);
    }

    /**
     * Initialize the meshdata object with data.
     *
     * @param vertices
     * @param normals
     * @param colors
     * @param coords
     */
    private void setupData(final FloatBuffer vertices, final FloatBuffer normals, final FloatBuffer colors,
            final FloatBufferData coords) {
        _meshData.setVertexBuffer(vertices);
        _meshData.setNormalBuffer(normals);
        _meshData.setColorBuffer(colors);
        _meshData.setTextureCoords(coords, 0);
        _meshData.setIndices(null);
    }

    /**
     * Puts a circle into vertex and normal buffer at the current buffer position. The buffers are enlarged and copied
     * if they are too small.
     *
     * @param radius
     *            radius of the circle
     * @param x
     *            x coordinate of circle center
     * @param y
     *            y coordinate of circle center
     * @param segments
     *            number of line segments the circle is built from
     * @param insideOut
     *            false for normal winding (ccw), true for clockwise winding
     */
    public void appendCircle(final double radius, final double x, final double y, final int segments,
            final boolean insideOut) {
        final int requiredFloats = segments * 2 * 3;
        final FloatBuffer verts = BufferUtils.ensureLargeEnough(_meshData.getVertexBuffer(), requiredFloats);
        _meshData.setVertexBuffer(verts);
        final FloatBuffer normals = BufferUtils.ensureLargeEnough(_meshData.getNormalBuffer(), requiredFloats);
        _meshData.setNormalBuffer(normals);
        double angle = 0;
        final double step = MathUtils.PI * 2 / segments;
        for (int i = 0; i < segments; i++) {
            final double dx = MathUtils.cos(insideOut ? -angle : angle) * radius;
            final double dy = MathUtils.sin(insideOut ? -angle : angle) * radius;
            if (i > 0) {
                verts.put((float) (dx + x)).put((float) (dy + y)).put(0);
                normals.put((float) dx).put((float) dy).put(0);
            }
            verts.put((float) (dx + x)).put((float) (dy + y)).put(0);
            normals.put((float) dx).put((float) dy).put(0);
            angle += step;
        }
        verts.put((float) (radius + x)).put((float) y).put(0);
        normals.put((float) radius).put(0).put(0);

        _meshData.markBufferDirty(MeshData.KEY_VertexCoords);
        _meshData.markBufferDirty(MeshData.KEY_NormalCoords);
    }

    /**
     * @return the width of this line in pixels.
     */
    public float getLineWidth() {
        return _lineWidth;
    }

    /**
     * Sets the desired pixel width of the line when drawn.
     *
     * @param lineWidth
     *            The lineWidth to set.
     */
    public void setLineWidth(final float lineWidth) {
        _lineWidth = lineWidth;
        setProperty("lineWidth", _lineWidth);
    }

    /**
     * @return the dot product limit between line segments where we will draw a miter. The range is [-1, 1]. In effect,
     *         a value of 1 will mean ALWAYS add a miter joint, while -1 will mean NEVER add one. Default is .75.
     */
    public float getMiterLimit() {
        return _miterLimit;
    }

    /**
     * Sets the dot product limit between line segments where we will draw a miter. The range is [-1, 1]. In effect, a
     * value of 1 will mean ALWAYS add a miter joint, while -1 will mean NEVER add one. Default is .75.
     *
     * @param limit
     *            The limit to set. Should be in the range [-1, 1], however this is not enforced.
     */
    public void setMiterLimit(final float limit) {
        _miterLimit = limit;
        setProperty("miterLimit", _miterLimit);
    }

    @Override
    public Line makeCopy(final boolean shareGeometricData) {
        final Line lineCopy = (Line) super.makeCopy(shareGeometricData);
        lineCopy.setLineWidth(_lineWidth);
        lineCopy.setMiterLimit(_miterLimit);
        return lineCopy;
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        super.write(capsule);
        capsule.write(_lineWidth, "lineWidth", 1.0f);
        capsule.write(_miterLimit, "miterLimit", 0.75f);
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        super.read(capsule);
        setLineWidth(capsule.readFloat("lineWidth", 1.0f));
        setMiterLimit(capsule.readFloat("miterLimit", 0.75f));
    }
}
