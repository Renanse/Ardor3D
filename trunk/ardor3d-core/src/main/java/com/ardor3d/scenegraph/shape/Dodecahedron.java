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

public class Dodecahedron extends Mesh {

    private static final int NUM_POINTS = 20;
    private static final int NUM_TRIS = 36;

    private double _sideLength;

    public Dodecahedron() {}

    /**
     * Creates an Dodecahedron (think of 12-sided dice) with center at the origin. The length of the sides will be as
     * specified in sideLength.
     * 
     * @param name
     *            The name of the octahedron.
     * @param sideLength
     *            The length of each side of the octahedron.
     */
    public Dodecahedron(final String name, final double sideLength) {
        super(name);
        _sideLength = sideLength;
        // allocate vertices
        _meshData.setVertexBuffer(BufferUtils.createVector3Buffer(NUM_POINTS));
        _meshData.setNormalBuffer(BufferUtils.createVector3Buffer(NUM_POINTS));
        _meshData.setTextureBuffer(BufferUtils.createVector2Buffer(NUM_POINTS), 0);

        _meshData.setIndices(BufferUtils.createIndexBufferData(3 * NUM_TRIS, NUM_POINTS - 1));

        setVertexData();
        setNormalData();
        setTextureData();
        setIndexData();

    }

    private void setIndexData() {
        final ByteBuffer indices = (ByteBuffer) _meshData.getIndexBuffer();
        indices.rewind();
        indices.put((byte) 0).put((byte) 8).put((byte) 9);
        indices.put((byte) 0).put((byte) 9).put((byte) 4);
        indices.put((byte) 0).put((byte) 4).put((byte) 16);
        indices.put((byte) 0).put((byte) 12).put((byte) 13);
        indices.put((byte) 0).put((byte) 13).put((byte) 1);
        indices.put((byte) 0).put((byte) 1).put((byte) 8);
        indices.put((byte) 0).put((byte) 16).put((byte) 17);
        indices.put((byte) 0).put((byte) 17).put((byte) 2);
        indices.put((byte) 0).put((byte) 2).put((byte) 12);
        indices.put((byte) 8).put((byte) 1).put((byte) 18);
        indices.put((byte) 8).put((byte) 18).put((byte) 5);
        indices.put((byte) 8).put((byte) 5).put((byte) 9);
        indices.put((byte) 12).put((byte) 2).put((byte) 10);
        indices.put((byte) 12).put((byte) 10).put((byte) 3);
        indices.put((byte) 12).put((byte) 3).put((byte) 13);
        indices.put((byte) 16).put((byte) 4).put((byte) 14);
        indices.put((byte) 16).put((byte) 14).put((byte) 6);
        indices.put((byte) 16).put((byte) 6).put((byte) 17);
        indices.put((byte) 9).put((byte) 5).put((byte) 15);
        indices.put((byte) 9).put((byte) 15).put((byte) 14);
        indices.put((byte) 9).put((byte) 14).put((byte) 4);
        indices.put((byte) 6).put((byte) 11).put((byte) 10);
        indices.put((byte) 6).put((byte) 10).put((byte) 2);
        indices.put((byte) 6).put((byte) 2).put((byte) 17);
        indices.put((byte) 3).put((byte) 19).put((byte) 18);
        indices.put((byte) 3).put((byte) 18).put((byte) 1);
        indices.put((byte) 3).put((byte) 1).put((byte) 13);
        indices.put((byte) 7).put((byte) 15).put((byte) 5);
        indices.put((byte) 7).put((byte) 5).put((byte) 18);
        indices.put((byte) 7).put((byte) 18).put((byte) 19);
        indices.put((byte) 7).put((byte) 11).put((byte) 6);
        indices.put((byte) 7).put((byte) 6).put((byte) 14);
        indices.put((byte) 7).put((byte) 14).put((byte) 15);
        indices.put((byte) 7).put((byte) 19).put((byte) 3);
        indices.put((byte) 7).put((byte) 3).put((byte) 10);
        indices.put((byte) 7).put((byte) 10).put((byte) 11);
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
        double fA = 1.0 / Math.sqrt(3.0f);
        double fB = Math.sqrt((3.0 - Math.sqrt(5.0)) / 6.0);
        double fC = Math.sqrt((3.0 + Math.sqrt(5.0)) / 6.0);
        fA *= _sideLength;
        fB *= _sideLength;
        fC *= _sideLength;

        final FloatBuffer vbuf = _meshData.getVertexBuffer();
        vbuf.rewind();
        vbuf.put((float) fA).put((float) fA).put((float) fA);
        vbuf.put((float) fA).put((float) fA).put((float) -fA);
        vbuf.put((float) fA).put((float) -fA).put((float) fA);
        vbuf.put((float) fA).put((float) -fA).put((float) -fA);
        vbuf.put((float) -fA).put((float) fA).put((float) fA);
        vbuf.put((float) -fA).put((float) fA).put((float) -fA);
        vbuf.put((float) -fA).put((float) -fA).put((float) fA);
        vbuf.put((float) -fA).put((float) -fA).put((float) -fA);
        vbuf.put((float) fB).put((float) fC).put(0.0f);
        vbuf.put((float) -fB).put((float) fC).put(0.0f);
        vbuf.put((float) fB).put((float) -fC).put(0.0f);
        vbuf.put((float) -fB).put((float) -fC).put(0.0f);
        vbuf.put((float) fC).put(0.0f).put((float) fB);
        vbuf.put((float) fC).put(0.0f).put((float) -fB);
        vbuf.put((float) -fC).put(0.0f).put((float) fB);
        vbuf.put((float) -fC).put(0.0f).put((float) -fB);
        vbuf.put(0.0f).put((float) fB).put((float) fC);
        vbuf.put(0.0f).put((float) -fB).put((float) fC);
        vbuf.put(0.0f).put((float) fB).put((float) -fC);
        vbuf.put(0.0f).put((float) -fB).put((float) -fC);
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
