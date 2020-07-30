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

import com.ardor3d.math.Vector3;
import com.ardor3d.scenegraph.IndexBufferData;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.geom.BufferUtils;

public class RoundedBox extends Mesh implements Cloneable {

  private final Vector3 _extent = new Vector3(0.5, 0.5, 0.5);
  private final Vector3 _border = new Vector3(0.05, 0.05, 0.05);
  private final Vector3 _slope = new Vector3(0.02, 0.02, 0.02);

  /** Creates a new instance of RoundedBox */
  public RoundedBox(final String name) {
    super(name);
    setData();
  }

  public RoundedBox(final String name, final Vector3 extent) {
    super(name);
    extent.subtract(_slope, _extent);
    setData();
  }

  public RoundedBox(final String name, final Vector3 extent, final Vector3 border, final Vector3 slope) {
    super(name);
    _border.set(border);
    _slope.set(slope);
    extent.subtract(_slope, _extent);
    setData();
  }

  private void setData() {
    setVertexAndNormalData();
    setTextureData();
    setIndexData();
  }

  private void put(final FloatBuffer fb, final FloatBuffer nb, final Vector3 vec) {
    fb.put((float) vec.getX()).put((float) vec.getY()).put((float) vec.getZ());
    final Vector3 v = vec.normalize(Vector3.fetchTempInstance());
    nb.put((float) v.getX()).put((float) v.getY()).put((float) v.getZ());
    Vector3.releaseTempInstance(v);
  }

  private void setVertexAndNormalData() {
    _meshData.setVertexBuffer(BufferUtils.createVector3Buffer(_meshData.getVertexBuffer(), 48));
    _meshData.setNormalBuffer(BufferUtils.createVector3Buffer(48));
    final Vector3[] vert = computeVertices(); // returns 32
    final FloatBuffer vb = _meshData.getVertexBuffer();
    final FloatBuffer nb = _meshData.getNormalBuffer();

    // bottom
    put(vb, nb, vert[0]);
    put(vb, nb, vert[1]);
    put(vb, nb, vert[2]);
    put(vb, nb, vert[3]);
    put(vb, nb, vert[8]);
    put(vb, nb, vert[9]);
    put(vb, nb, vert[10]);
    put(vb, nb, vert[11]);

    // front
    put(vb, nb, vert[1]);
    put(vb, nb, vert[0]);
    put(vb, nb, vert[5]);
    put(vb, nb, vert[4]);
    put(vb, nb, vert[13]);
    put(vb, nb, vert[12]);
    put(vb, nb, vert[15]);
    put(vb, nb, vert[14]);

    // right
    put(vb, nb, vert[3]);
    put(vb, nb, vert[1]);
    put(vb, nb, vert[7]);
    put(vb, nb, vert[5]);
    put(vb, nb, vert[17]);
    put(vb, nb, vert[16]);
    put(vb, nb, vert[19]);
    put(vb, nb, vert[18]);

    // back
    put(vb, nb, vert[2]);
    put(vb, nb, vert[3]);
    put(vb, nb, vert[6]);
    put(vb, nb, vert[7]);
    put(vb, nb, vert[20]);
    put(vb, nb, vert[21]);
    put(vb, nb, vert[22]);
    put(vb, nb, vert[23]);

    // left
    put(vb, nb, vert[0]);
    put(vb, nb, vert[2]);
    put(vb, nb, vert[4]);
    put(vb, nb, vert[6]);
    put(vb, nb, vert[24]);
    put(vb, nb, vert[25]);
    put(vb, nb, vert[26]);
    put(vb, nb, vert[27]);

    // top
    put(vb, nb, vert[5]);
    put(vb, nb, vert[4]);
    put(vb, nb, vert[7]);
    put(vb, nb, vert[6]);
    put(vb, nb, vert[29]);
    put(vb, nb, vert[28]);
    put(vb, nb, vert[31]);
    put(vb, nb, vert[30]);
  }

  private void setTextureData() {
    if (_meshData.getTextureCoords(0) == null) {
      _meshData.setTextureBuffer(BufferUtils.createVector2Buffer(48), 0);
      final FloatBuffer tex = _meshData.getTextureCoords(0).getBuffer();

      final double[][] ratio = new double[][] {
          {0.5 * _border.getX() / (_extent.getX() + _slope.getX()),
              0.5 * _border.getZ() / (_extent.getZ() + _slope.getZ())},
          {0.5 * _border.getX() / (_extent.getX() + _slope.getX()),
              0.5 * _border.getY() / (_extent.getY() + _slope.getY())},
          {0.5 * _border.getZ() / (_extent.getZ() + _slope.getZ()),
              0.5 * _border.getY() / (_extent.getY() + _slope.getY())},
          {0.5 * _border.getX() / (_extent.getX() + _slope.getX()),
              0.5 * _border.getY() / (_extent.getY() + _slope.getY())},
          {0.5 * _border.getZ() / (_extent.getZ() + _slope.getZ()),
              0.5 * _border.getY() / (_extent.getY() + _slope.getY())},
          {0.5 * _border.getX() / (_extent.getX() + _slope.getX()),
              0.5 * _border.getZ() / (_extent.getZ() + _slope.getZ())},};

      for (int i = 0; i < 6; i++) {
        tex.put(1).put(0);
        tex.put(0).put(0);
        tex.put(1).put(1);
        tex.put(0).put(1);
        tex.put((float) (1 - ratio[i][0])).put((float) (0 + ratio[i][1]));
        tex.put((float) (0 + ratio[i][0])).put((float) (0 + ratio[i][1]));
        tex.put((float) (1 - ratio[i][0])).put((float) (1 - ratio[i][1]));
        tex.put((float) (0 + ratio[i][0])).put((float) (1 - ratio[i][1]));
      }
    }
  }

  private void setIndexData() {
    if (_meshData.getIndices() == null) {
      final IndexBufferData<?> buff = BufferUtils.createIndexBufferData(180, 48 - 1);
      final int[] data =
          new int[] {0, 4, 1, 1, 4, 5, 1, 5, 3, 3, 5, 7, 3, 7, 2, 2, 7, 6, 2, 6, 0, 0, 6, 4, 4, 6, 5, 5, 6, 7};
      for (int i = 0; i < 6; i++) {
        for (int n = 0; n < 30; n++) {
          buff.put(30 * i + n, 8 * i + data[n]);
        }
      }
      _meshData.setIndices(buff);
    }
  }

  public Vector3[] computeVertices() {
    return new Vector3[] {
        // Cube
        new Vector3(-_extent.getX(), -_extent.getY(), _extent.getZ()), // 0
        new Vector3(_extent.getX(), -_extent.getY(), _extent.getZ()), // 1
        new Vector3(-_extent.getX(), -_extent.getY(), -_extent.getZ()), // 2
        new Vector3(_extent.getX(), -_extent.getY(), -_extent.getZ()), // 3
        new Vector3(-_extent.getX(), _extent.getY(), _extent.getZ()), // 4
        new Vector3(_extent.getX(), _extent.getY(), _extent.getZ()), // 5
        new Vector3(-_extent.getX(), _extent.getY(), -_extent.getZ()), // 6
        new Vector3(_extent.getX(), _extent.getY(), -_extent.getZ()), // 7
        // bottom
        new Vector3(-_extent.getX() + _border.getX(), -_extent.getY() - _slope.getY(), _extent.getZ() - _border.getZ()), // 8
                                                                                                                         // (0)
        new Vector3(_extent.getX() - _border.getX(), -_extent.getY() - _slope.getY(), _extent.getZ() - _border.getZ()), // 9
        // (
        // 1
        // )
        new Vector3(-_extent.getX() + _border.getX(), -_extent.getY() - _slope.getY(),
            -_extent.getZ() + _border.getZ()), // 10 (2)
        new Vector3(_extent.getX() - _border.getX(), -_extent.getY() - _slope.getY(), -_extent.getZ() + _border.getZ()), // 11
                                                                                                                         // (3)
        // front
        new Vector3(-_extent.getX() + _border.getX(), -_extent.getY() + _border.getY(), _extent.getZ() + _slope.getZ()), // 12
                                                                                                                         // (0)
        new Vector3(_extent.getX() - _border.getX(), -_extent.getY() + _border.getY(), _extent.getZ() + _slope.getZ()), // 13
        // (
        // 1
        // )
        new Vector3(-_extent.getX() + _border.getX(), _extent.getY() - _border.getY(), _extent.getZ() + _slope.getZ()), // 14
        // (
        // 4
        // )
        new Vector3(_extent.getX() - _border.getX(), _extent.getY() - _border.getY(), _extent.getZ() + _slope.getZ()), // 15
        // (
        // 5
        // )
        // right
        new Vector3(_extent.getX() + _slope.getX(), -_extent.getY() + _border.getY(), _extent.getZ() - _border.getZ()), // 16
        // (
        // 1
        // )
        new Vector3(_extent.getX() + _slope.getX(), -_extent.getY() + _border.getY(), -_extent.getZ() + _border.getZ()), // 17
                                                                                                                         // (3)
        new Vector3(_extent.getX() + _slope.getX(), _extent.getY() - _border.getY(), _extent.getZ() - _border.getZ()), // 18
        // (
        // 5
        // )
        new Vector3(_extent.getX() + _slope.getX(), _extent.getY() - _border.getY(), -_extent.getZ() + _border.getZ()), // 19
        // (
        // 7
        // )
        // back
        new Vector3(-_extent.getX() + _border.getX(), -_extent.getY() + _border.getY(),
            -_extent.getZ() - _slope.getZ()), // 20 (2)
        new Vector3(_extent.getX() - _border.getX(), -_extent.getY() + _border.getY(), -_extent.getZ() - _slope.getZ()), // 21
                                                                                                                         // (3)
        new Vector3(-_extent.getX() + _border.getX(), _extent.getY() - _border.getY(), -_extent.getZ() - _slope.getZ()), // 22
                                                                                                                         // (6)
        new Vector3(_extent.getX() - _border.getX(), _extent.getY() - _border.getY(), -_extent.getZ() - _slope.getZ()), // 23
        // (
        // 7
        // )
        // left
        new Vector3(-_extent.getX() - _slope.getX(), -_extent.getY() + _border.getY(), _extent.getZ() - _border.getZ()), // 24
                                                                                                                         // (0)
        new Vector3(-_extent.getX() - _slope.getX(), -_extent.getY() + _border.getY(),
            -_extent.getZ() + _border.getZ()), // 25 (2)
        new Vector3(-_extent.getX() - _slope.getX(), _extent.getY() - _border.getY(), _extent.getZ() - _border.getZ()), // 26
        // (
        // 4
        // )
        new Vector3(-_extent.getX() - _slope.getX(), _extent.getY() - _border.getY(), -_extent.getZ() + _border.getZ()), // 27
                                                                                                                         // (6)
        // top
        new Vector3(-_extent.getX() + _border.getX(), _extent.getY() + _slope.getY(), _extent.getZ() - _border.getZ()), // 28
        // (
        // 4
        // )
        new Vector3(_extent.getX() - _border.getX(), _extent.getY() + _slope.getY(), _extent.getZ() - _border.getZ()), // 29
        // (
        // 5
        // )
        new Vector3(-_extent.getX() + _border.getX(), _extent.getY() + _slope.getY(), -_extent.getZ() + _border.getZ()), // 30
                                                                                                                         // (6)
        new Vector3(_extent.getX() - _border.getX(), _extent.getY() + _slope.getY(), -_extent.getZ() + _border.getZ()), // 31
        // (
        // 7
        // )
    };
  }

  /**
   * <code>clone</code> creates a new RoundedBox object containing the same data as this one.
   *
   * @return the new Box
   */
  @Override
  public RoundedBox clone() {
    return new RoundedBox(getName() + "_clone", _extent.clone(), _border.clone(), _slope.clone());
  }

  @Override
  public void write(final OutputCapsule capsule) throws IOException {
    super.write(capsule);
    capsule.write(_extent, "extent", (Vector3) Vector3.ZERO);
    capsule.write(_border, "border", (Vector3) Vector3.ZERO);
    capsule.write(_slope, "slope", (Vector3) Vector3.ZERO);
  }

  @Override
  public void read(final InputCapsule capsule) throws IOException {
    super.read(capsule);
    _extent.set(capsule.readSavable("extent", (Vector3) Vector3.ZERO));
    _border.set(capsule.readSavable("border", (Vector3) Vector3.ZERO));
    _slope.set(capsule.readSavable("slope", (Vector3) Vector3.ZERO));
  }
}
