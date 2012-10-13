/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scenegraph.shape;

import java.io.IOException;
import java.nio.FloatBuffer;

import com.ardor3d.math.Vector3;
import com.ardor3d.scenegraph.IndexBufferData;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.geom.BufferUtils;

/**
 * <code>Pyramid</code> provides an extension of <code>Mesh</code>. A pyramid is defined by a width at the base and a
 * height. The pyramid is a four sided pyramid with the center at (0,0). The pyramid will be axis aligned with the peak
 * being on the positive y axis and the base being in the x-z plane.
 */
public class Pyramid extends Mesh {

    private double _height;

    private double _width;

    public Pyramid() {}

    /**
     * Constructor instantiates a new <code>Pyramid</code> object. The base width and the height are provided.
     * 
     * @param name
     *            the name of the scene element. This is required for identification and comparison purposes.
     * @param width
     *            the base width of the pyramid.
     * @param height
     *            the height of the pyramid from the base to the peak.
     */
    public Pyramid(final String name, final double width, final double height) {
        super(name);
        _width = width;
        _height = height;

        setVertexData();
        setNormalData();
        setTextureData();
        setIndexData();
    }

    /**
     * <code>setVertexData</code> sets the vertices that make the pyramid. Where the center of the box is the origin and
     * the base and height are set during construction.
     */
    private void setVertexData() {
        final Vector3 peak = new Vector3(0, _height / 2, 0);
        final Vector3 vert0 = new Vector3(-_width / 2, -_height / 2, -_width / 2);
        final Vector3 vert1 = new Vector3(_width / 2, -_height / 2, -_width / 2);
        final Vector3 vert2 = new Vector3(_width / 2, -_height / 2, _width / 2);
        final Vector3 vert3 = new Vector3(-_width / 2, -_height / 2, _width / 2);

        final FloatBuffer verts = BufferUtils.createVector3Buffer(16);

        // base
        verts.put(vert3.getXf()).put(vert3.getYf()).put(vert3.getZf());
        verts.put(vert2.getXf()).put(vert2.getYf()).put(vert2.getZf());
        verts.put(vert1.getXf()).put(vert1.getYf()).put(vert1.getZf());
        verts.put(vert0.getXf()).put(vert0.getYf()).put(vert0.getZf());

        // side 1
        verts.put(vert0.getXf()).put(vert0.getYf()).put(vert0.getZf());
        verts.put(vert1.getXf()).put(vert1.getYf()).put(vert1.getZf());
        verts.put(peak.getXf()).put(peak.getYf()).put(peak.getZf());

        // side 2
        verts.put(vert1.getXf()).put(vert1.getYf()).put(vert1.getZf());
        verts.put(vert2.getXf()).put(vert2.getYf()).put(vert2.getZf());
        verts.put(peak.getXf()).put(peak.getYf()).put(peak.getZf());

        // side 3
        verts.put(vert2.getXf()).put(vert2.getYf()).put(vert2.getZf());
        verts.put(vert3.getXf()).put(vert3.getYf()).put(vert3.getZf());
        verts.put(peak.getXf()).put(peak.getYf()).put(peak.getZf());

        // side 4
        verts.put(vert3.getXf()).put(vert3.getYf()).put(vert3.getZf());
        verts.put(vert0.getXf()).put(vert0.getYf()).put(vert0.getZf());
        verts.put(peak.getXf()).put(peak.getYf()).put(peak.getZf());

        verts.rewind();
        _meshData.setVertexBuffer(verts);
    }

    /**
     * <code>setNormalData</code> defines the normals of each face of the pyramid.
     */
    private void setNormalData() {
        final FloatBuffer norms = BufferUtils.createVector3Buffer(16);

        // bottom
        norms.put(0).put(-1).put(0);
        norms.put(0).put(-1).put(0);
        norms.put(0).put(-1).put(0);
        norms.put(0).put(-1).put(0);

        // back
        norms.put(0).put(0.70710677f).put(-0.70710677f);
        norms.put(0).put(0.70710677f).put(-0.70710677f);
        norms.put(0).put(0.70710677f).put(-0.70710677f);

        // right
        norms.put(0.70710677f).put(0.70710677f).put(0);
        norms.put(0.70710677f).put(0.70710677f).put(0);
        norms.put(0.70710677f).put(0.70710677f).put(0);

        // front
        norms.put(0).put(0.70710677f).put(0.70710677f);
        norms.put(0).put(0.70710677f).put(0.70710677f);
        norms.put(0).put(0.70710677f).put(0.70710677f);

        // left
        norms.put(-0.70710677f).put(0.70710677f).put(0);
        norms.put(-0.70710677f).put(0.70710677f).put(0);
        norms.put(-0.70710677f).put(0.70710677f).put(0);

        norms.rewind();
        _meshData.setNormalBuffer(norms);
    }

    /**
     * <code>setTextureData</code> sets the texture that defines the look of the pyramid. The top point of the pyramid
     * is the top center of the texture, with the remaining texture wrapping around it.
     */
    private void setTextureData() {
        final FloatBuffer texCoords = BufferUtils.createVector2Buffer(16);

        texCoords.put(1).put(0);
        texCoords.put(0).put(0);
        texCoords.put(0).put(1);
        texCoords.put(1).put(1);

        texCoords.put(1).put(0);
        texCoords.put(0.75f).put(0);
        texCoords.put(0.5f).put(1);

        texCoords.put(0.75f).put(0);
        texCoords.put(0.5f).put(0);
        texCoords.put(0.5f).put(1);

        texCoords.put(0.5f).put(0);
        texCoords.put(0.25f).put(0);
        texCoords.put(0.5f).put(1);

        texCoords.put(0.25f).put(0);
        texCoords.put(0).put(0);
        texCoords.put(0.5f).put(1);

        texCoords.rewind();
        _meshData.setTextureBuffer(texCoords, 0);
    }

    /**
     * <code>setIndexData</code> sets the indices into the list of vertices, defining all triangles that constitute the
     * pyramid.
     */
    private void setIndexData() {
        final IndexBufferData<?> indices = BufferUtils.createIndexBufferData(18, 16 - 1);
        indices.put(3).put(2).put(1);
        indices.put(3).put(1).put(0);
        indices.put(6).put(5).put(4);
        indices.put(9).put(8).put(7);
        indices.put(12).put(11).put(10);
        indices.put(15).put(14).put(13);

        indices.getBuffer().rewind();
        _meshData.setIndices(indices);
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        super.write(capsule);
        capsule.write(_height, "height", 0);
        capsule.write(_width, "width", 0);

    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        super.read(capsule);
        _height = capsule.readDouble("height", 0);
        _width = capsule.readDouble("width", 0);

    }
}