/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.scenegraph.shape;

import java.io.IOException;
import java.nio.FloatBuffer;

import com.ardor3d.buffer.BufferUtils;
import com.ardor3d.buffer.IndexBufferData;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.util.MathUtils;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;

public class Dodecahedron extends Mesh {

  private static final int NUM_POINTS = 20;
  private static final int NUM_TRIS = 36;

  private double _sideLength;

  public Dodecahedron() {}

  /**
   * Creates an Dodecahedron (think of 12-sided dice) with center at the origin. The length of the
   * sides will be as specified in sideLength.
   * 
   * @param name
   *          The name of the octahedron.
   * @param sideLength
   *          The length of each side of the octahedron.
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
    final IndexBufferData<?> indices = _meshData.getIndices();
    indices.rewind();
    indices.put(0).put(8).put(9);
    indices.put(0).put(9).put(4);
    indices.put(0).put(4).put(16);
    indices.put(0).put(12).put(13);
    indices.put(0).put(13).put(1);
    indices.put(0).put(1).put(8);
    indices.put(0).put(16).put(17);
    indices.put(0).put(17).put(2);
    indices.put(0).put(2).put(12);
    indices.put(8).put(1).put(18);
    indices.put(8).put(18).put(5);
    indices.put(8).put(5).put(9);
    indices.put(12).put(2).put(10);
    indices.put(12).put(10).put(3);
    indices.put(12).put(3).put(13);
    indices.put(16).put(4).put(14);
    indices.put(16).put(14).put(6);
    indices.put(16).put(6).put(17);
    indices.put(9).put(5).put(15);
    indices.put(9).put(15).put(14);
    indices.put(9).put(14).put(4);
    indices.put(6).put(11).put(10);
    indices.put(6).put(10).put(2);
    indices.put(6).put(2).put(17);
    indices.put(3).put(19).put(18);
    indices.put(3).put(18).put(1);
    indices.put(3).put(1).put(13);
    indices.put(7).put(15).put(5);
    indices.put(7).put(5).put(18);
    indices.put(7).put(18).put(19);
    indices.put(7).put(11).put(6);
    indices.put(7).put(6).put(14);
    indices.put(7).put(14).put(15);
    indices.put(7).put(19).put(3);
    indices.put(7).put(3).put(10);
    indices.put(7).put(10).put(11);
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
