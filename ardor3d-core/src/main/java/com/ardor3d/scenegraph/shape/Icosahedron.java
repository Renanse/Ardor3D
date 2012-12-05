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
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.geom.BufferUtils;

public class Icosahedron extends Mesh {

    private static final int NUM_POINTS = 12;

    private double _sideLength;

    public Icosahedron() {}

    /**
     * Creates an Icosahedron (think of 20-sided dice) with center at the origin. The length of the sides will be as
     * specified in sideLength.
     * 
     * @param name
     *            The name of the Icosahedron.
     * @param sideLength
     *            The length of each side of the Icosahedron.
     */
    public Icosahedron(final String name, final double sideLength) {
        super(name);
        _sideLength = sideLength;

        // allocate vertices
        _meshData.setVertexBuffer(BufferUtils.createVector3Buffer(NUM_POINTS));
        _meshData.setNormalBuffer(BufferUtils.createVector3Buffer(NUM_POINTS));
        _meshData.setTextureBuffer(BufferUtils.createVector2Buffer(NUM_POINTS), 0);

        setVertexData();
        setNormalData();
        setTextureData();
        setIndexData();

    }

    private void setIndexData() {
        final byte[] indices = { 0, 8, 4, 0, 5, 10, 2, 4, 9, 2, 11, 5, 1, 6, 8, 1, 10, 7, 3, 9, 6, 3, 7, 11, 0, 10, 8,
                1, 8, 10, 2, 9, 11, 3, 11, 9, 4, 2, 0, 5, 0, 2, 6, 1, 3, 7, 3, 1, 8, 6, 4, 9, 4, 6, 10, 5, 7, 11, 7, 5 };
        final ByteBuffer buf = BufferUtils.createByteBuffer(indices.length);
        buf.put(indices);
        buf.rewind();
        _meshData.setIndexBuffer(buf);
    }

    private void setTextureData() {
        final Vector2 tex = new Vector2();
        final Vector3 vert = new Vector3();
        for (int i = 0; i < NUM_POINTS; i++) {
            BufferUtils.populateFromBuffer(vert, _meshData.getVertexBuffer(), i);
            if (Math.abs(vert.getZ()) < _sideLength) {
                tex.setX(0.5 * (1.0 + Math.atan2(vert.getY(), vert.getX()) * MathUtils.INV_PI));
            } else {
                tex.setX(0.5);
            }
            tex.setY(Math.acos(vert.getZ() / _sideLength) * MathUtils.INV_PI);

            _meshData.getTextureCoords(0).getBuffer().put((float) tex.getX()).put((float) tex.getY());
        }
    }

    private void setNormalData() {
        final Vector3 norm = new Vector3();
        for (int i = 0; i < NUM_POINTS; i++) {
            BufferUtils.populateFromBuffer(norm, _meshData.getVertexBuffer(), i);
            norm.normalizeLocal();
            BufferUtils.setInBuffer(norm, _meshData.getNormalBuffer(), i);
        }
    }

    private void setVertexData() {
        final double dGoldenRatio = 0.5 * (1.0 + Math.sqrt(5.0));
        final double dInvRoot = 1.0 / Math.sqrt(1.0 + dGoldenRatio * dGoldenRatio);
        final float dU = (float) (dGoldenRatio * dInvRoot * _sideLength);
        final float dV = (float) (dInvRoot * _sideLength);

        final FloatBuffer vbuf = _meshData.getVertexBuffer();
        vbuf.rewind();
        vbuf.put(dU).put(dV).put(0.0f);
        vbuf.put(-dU).put(dV).put(0.0f);
        vbuf.put(dU).put(-dV).put(0.0f);
        vbuf.put(-dU).put(-dV).put(0.0f);
        vbuf.put(dV).put(0.0f).put(dU);
        vbuf.put(dV).put(0.0f).put(-dU);
        vbuf.put(-dV).put(0.0f).put(dU);
        vbuf.put(-dV).put(0.0f).put(-dU);
        vbuf.put(0.0f).put(dU).put(dV);
        vbuf.put(0.0f).put(-dU).put(dV);
        vbuf.put(0.0f).put(dU).put(-dV);
        vbuf.put(0.0f).put(-dU).put(-dV);
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        super.write(capsule);
        capsule.write(_sideLength, "sideLength", 0);

    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        super.read(capsule);
        _sideLength = capsule.readInt("sideLength", 0);

    }
}
