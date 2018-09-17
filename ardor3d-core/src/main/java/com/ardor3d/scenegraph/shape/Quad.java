/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scenegraph.shape;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.scenegraph.hint.LightCombineMode;
import com.ardor3d.scenegraph.hint.SceneHints;
import com.ardor3d.scenegraph.hint.TextureCombineMode;
import com.ardor3d.util.geom.BufferUtils;

/**
 * <code>Quad</code> defines a four sided, two dimensional shape. The local height of the <code>Quad</code> defines it's
 * size about the y-axis, while the width defines the x-axis. The z-axis will always be 0.
 */
public class Quad extends Mesh {

    protected double _width = 0;
    protected double _height = 0;

    public Quad() {

    }

    /**
     * Constructor creates a new <code>Quad</code> object.
     *
     * @param name
     *            the name of this <code>Quad</code>.
     */
    public Quad(final String name) {
        this(name, 1, 1);
    }

    /**
     * Constructor creates a new <code>Quade</code> object with the provided width and height.
     *
     * @param name
     *            the name of the <code>Quad</code>.
     * @param width
     *            the width of the <code>Quad</code>.
     * @param height
     *            the height of the <code>Quad</code>.
     */
    public Quad(final String name, final double width, final double height) {
        super(name);
        initialize(width, height);
    }

    /**
     * <code>resize</code> changes the width and height of the given quad by altering its vertices.
     *
     * @param width
     *            the new width of the <code>Quad</code>.
     * @param height
     *            the new height of the <code>Quad</code>.
     */
    public void resize(final double width, final double height) {
        _width = width;
        _height = height;

        _meshData.getVertexBuffer().clear();
        _meshData.getVertexBuffer().put((float) (-width / 2)).put((float) (height / 2)).put(0);
        _meshData.getVertexBuffer().put((float) (-width / 2)).put((float) (-height / 2)).put(0);
        _meshData.getVertexBuffer().put((float) (width / 2)).put((float) (-height / 2)).put(0);
        _meshData.getVertexBuffer().put((float) (width / 2)).put((float) (height / 2)).put(0);
    }

    /**
     * <code>initialize</code> builds the data for the <code>Quad</code> object.
     *
     * @param width
     *            the width of the <code>Quad</code>.
     * @param height
     *            the height of the <code>Quad</code>.
     */
    private void initialize(final double width, final double height) {
        final int verts = 4;
        _meshData.setVertexBuffer(BufferUtils.createVector3Buffer(verts));
        _meshData.setNormalBuffer(BufferUtils.createVector3Buffer(verts));
        final FloatBuffer tbuf = BufferUtils.createVector2Buffer(verts);
        _meshData.setTextureBuffer(tbuf, 0);

        _meshData.getNormalBuffer().put(0).put(0).put(1);
        _meshData.getNormalBuffer().put(0).put(0).put(1);
        _meshData.getNormalBuffer().put(0).put(0).put(1);
        _meshData.getNormalBuffer().put(0).put(0).put(1);

        tbuf.put(0).put(1);
        tbuf.put(0).put(0);
        tbuf.put(1).put(0);
        tbuf.put(1).put(1);

        final byte[] indices = { 0, 1, 2, 0, 2, 3 };
        final ByteBuffer buf = BufferUtils.createByteBuffer(indices.length);
        buf.put(indices);
        buf.rewind();
        _meshData.setIndexBuffer(buf);

        resize(width, height);
    }

    public double getWidth() {
        return _width;
    }

    public double getHeight() {
        return _height;
    }

    public static Quad newFullScreenQuad() {
        final Quad quad = new Quad("fsq", 2, 2);
        final SceneHints sceneHints = quad.getSceneHints();
        sceneHints.setCullHint(CullHint.Never);
        sceneHints.setRenderBucketType(RenderBucketType.OrthoOrder);
        sceneHints.setLightCombineMode(LightCombineMode.Off);
        sceneHints.setTextureCombineMode(TextureCombineMode.Replace);
        return quad;
    }
}