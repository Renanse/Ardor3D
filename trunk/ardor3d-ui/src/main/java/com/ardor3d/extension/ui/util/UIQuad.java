/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.ui.util;

import java.nio.FloatBuffer;

import com.ardor3d.renderer.IndexMode;
import com.ardor3d.scenegraph.FloatBufferData;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.util.geom.BufferUtils;

/**
 * Defines a four sided quad with its origin at the lower left corner. By default, this mesh only contains vertices (in
 * the two dimensional space) and texture coordinates.
 */
public class UIQuad extends Mesh {

    /** The width of this ui quad */
    protected double _width = 0;
    /** The height of this ui quad */
    protected double _height = 0;

    /** Construct a new 1x1 UI quad. */
    public UIQuad() {
        this(null);
    }

    /**
     * Construct a new 1x1 UI quad with the given name.
     * 
     * @param name
     */
    public UIQuad(final String name) {
        this(name, 1, 1);
    }

    /**
     * Construct a new UI quad with the given name and dimensions.
     * 
     * @param name
     * @param width
     * @param height
     */
    public UIQuad(final String name, final double width, final double height) {
        super(name);
        initialize();
        resize(width, height);
    }

    /**
     * Alter the vertices of this Ui quad so that it of the given size.
     * 
     * @param width
     * @param height
     */
    public void resize(final double width, final double height) {
        _width = width;
        _height = height;

        _meshData.getVertexBuffer().clear();
        _meshData.getVertexBuffer().put(0).put((float) _height);
        _meshData.getVertexBuffer().put(0).put(0);
        _meshData.getVertexBuffer().put((float) _width).put(0);
        _meshData.getVertexBuffer().put((float) _width).put((float) _height);
    }

    /**
     * Set the basic data for this mesh such as texture coordinates, index mode and our vertex buffer.
     */
    private void initialize() {
        _meshData.setVertexCoords(new FloatBufferData(BufferUtils.createVector2Buffer(4), 2));
        final FloatBuffer tbuf = BufferUtils.createVector2Buffer(4);
        _meshData.setTextureBuffer(tbuf, 0);

        tbuf.put(0).put(1);
        tbuf.put(0).put(0);
        tbuf.put(1).put(0);
        tbuf.put(1).put(1);

        _meshData.setIndexMode(IndexMode.TriangleFan);
    }

    public double getWidth() {
        return _width;
    }

    public double getHeight() {
        return _height;
    }
}