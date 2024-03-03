/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.scenegraph.shape;

import java.io.IOException;

import com.ardor3d.buffer.BufferUtils;
import com.ardor3d.math.Vector3;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;

/**
 * A regular hexagon with each triangle having side length that is given in the constructor.
 */
public class Hexagon extends Mesh {

  private static final int NUM_POINTS = 7;

  private static final int NUM_TRIS = 6;

  private float _sideLength;

  public Hexagon() {}

  /**
   * Hexagon Constructor instantiates a new Hexagon. This element is center on 0,0,0 with all normals
   * pointing up. The user must move and rotate for positioning.
   * 
   * @param name
   *          the name of the scene element. This is required for identification and comparison
   *          purposes.
   * @param sideLength
   *          The length of all the sides of the triangles
   */
  public Hexagon(final String name, final float sideLength) {
    super(name);
    _sideLength = sideLength;
    // allocate vertices
    _meshData.setVertexBuffer(BufferUtils.createVector3Buffer(NUM_POINTS));
    _meshData.setNormalBuffer(BufferUtils.createVector3Buffer(NUM_POINTS));
    _meshData.setTextureBuffer(BufferUtils.createVector2Buffer(NUM_POINTS), 0);

    _meshData.setIndices(BufferUtils.createIndexBufferData(3 * NUM_TRIS, NUM_POINTS - 1));

    setVertexData();
    setIndexData();
    setTextureData();
    setNormalData();

  }

  /**
   * Vertices are set up like this: 0__1 / \ / \ 5/__\6/__\2 \ / \ / \ /___\ / 4 3 All lines on this
   * diagram are sideLength long. Therefore, the width of the hexagon is sideLength * 2, and the
   * height is 2 * the height of one equilateral triangle with all side = sideLength which is .866
   */
  private void setVertexData() {
    _meshData.getVertexBuffer().put(-(_sideLength / 2)).put(_sideLength * 0.866f).put(0.0f);
    _meshData.getVertexBuffer().put(_sideLength / 2).put(_sideLength * 0.866f).put(0.0f);
    _meshData.getVertexBuffer().put(_sideLength).put(0.0f).put(0.0f);
    _meshData.getVertexBuffer().put(_sideLength / 2).put(-_sideLength * 0.866f).put(0.0f);
    _meshData.getVertexBuffer().put(-(_sideLength / 2)).put(-_sideLength * 0.866f).put(0.0f);
    _meshData.getVertexBuffer().put(-_sideLength).put(0.0f).put(0.0f);
    _meshData.getVertexBuffer().put(0.0f).put(0.0f).put(0.0f);
  }

  /**
   * Sets up the indexes of the mesh. These go in a clockwise fashion and thus only the 'up' side of
   * the hex is lit properly. If you wish to have to either set two sided lighting or create two hexes
   * back-to-back
   */

  private void setIndexData() {
    _meshData.getIndices().rewind();
    // tri 1
    _meshData.getIndices().put(0);
    _meshData.getIndices().put(6);
    _meshData.getIndices().put(1);
    // tri 2
    _meshData.getIndices().put(1);
    _meshData.getIndices().put(6);
    _meshData.getIndices().put(2);
    // tri 3
    _meshData.getIndices().put(2);
    _meshData.getIndices().put(6);
    _meshData.getIndices().put(3);
    // tri 4
    _meshData.getIndices().put(3);
    _meshData.getIndices().put(6);
    _meshData.getIndices().put(4);
    // tri 5
    _meshData.getIndices().put(4);
    _meshData.getIndices().put(6);
    _meshData.getIndices().put(5);
    // tri 6
    _meshData.getIndices().put(5);
    _meshData.getIndices().put(6);
    _meshData.getIndices().put(0);
  }

  private void setTextureData() {
    _meshData.getTextureCoords(0).getBuffer().put(0.25f).put(0);
    _meshData.getTextureCoords(0).getBuffer().put(0.75f).put(0);
    _meshData.getTextureCoords(0).getBuffer().put(1.0f).put(0.5f);
    _meshData.getTextureCoords(0).getBuffer().put(0.75f).put(1.0f);
    _meshData.getTextureCoords(0).getBuffer().put(0.25f).put(1.0f);
    _meshData.getTextureCoords(0).getBuffer().put(0.0f).put(0.5f);
    _meshData.getTextureCoords(0).getBuffer().put(0.5f).put(0.5f);
  }

  /**
   * Sets all the default vertex normals to 'up', +1 in the Z direction.
   */
  private void setNormalData() {
    final Vector3 zAxis = new Vector3(0, 0, 1);
    for (int i = 0; i < NUM_POINTS; i++) {
      BufferUtils.setInBuffer(zAxis, _meshData.getNormalBuffer(), i);
    }
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
