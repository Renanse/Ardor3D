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
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.geom.BufferUtils;

/**
 * <code>Box</code> is an axis-aligned rectangular prism defined by a center point and x, y, and z
 * extents from that center (essentially radii.)
 */
public class Box extends Mesh implements Cloneable {

  private double _xExtent, _yExtent, _zExtent;

  private final Vector3 _center = new Vector3(0, 0, 0);

  /**
   * Constructs a new 1x1x1 <code>Box</code>.
   */
  public Box() {
    this("unnamed Box");
  }

  /**
   * Constructs a new 1x1x1 <code>Box</code> with the given name.
   *
   * @param name
   *          the name to give this new box. This is required for identification and comparison
   *          purposes.
   */
  public Box(final String name) {
    super(name);
    setData(Vector3.ZERO, 0.5, 0.5, 0.5);
  }

  /**
   * Constructs a new <code>Box</code> object using the given two points as opposite corners of the
   * box. These two points may be in any order.
   *
   * @param name
   *          the name to give this new box. This is required for identification and comparison
   *          purposes.
   * @param pntA
   *          the first point
   * @param pntB
   *          the second point.
   */
  public Box(final String name, final ReadOnlyVector3 pntA, final ReadOnlyVector3 pntB) {
    super(name);
    setData(pntA, pntB);
  }

  /**
   * Constructs a new <code>Box</code> object using the given center and extents. Since the extents
   * represent the distance from the center of the box to the edge, the full length of a side is
   * actually 2 * extent.
   *
   * @param name
   *          the name to give this new box. This is required for identification and comparison
   *          purposes.
   * @param center
   *          Center of the box.
   * @param xExtent
   *          x extent of the box
   * @param yExtent
   *          y extent of the box
   * @param zExtent
   *          z extent of the box
   */
  public Box(final String name, final ReadOnlyVector3 center, final double xExtent, final double yExtent,
    final double zExtent) {
    super(name);
    setData(center, xExtent, yExtent, zExtent);
  }

  /**
   * @return the current center of this box.
   */
  public ReadOnlyVector3 getCenter() { return _center; }

  /**
   * @return the current X extent of this box.
   */
  public double getXExtent() { return _xExtent; }

  /**
   * @return the current Y extent of this box.
   */
  public double getYExtent() { return _yExtent; }

  /**
   * @return the current Z extent of this box.
   */
  public double getZExtent() { return _zExtent; }

  /**
   * Updates the center point and extents of this box to match an axis-aligned box defined by the two
   * given opposite corners.
   *
   * @param pntA
   *          the first point
   * @param pntB
   *          the second point.
   */
  public void setData(final ReadOnlyVector3 pntA, final ReadOnlyVector3 pntB) {
    _center.set(pntB).addLocal(pntA).multiplyLocal(0.5);

    final double x = Math.abs(pntB.getX() - _center.getX());
    final double y = Math.abs(pntB.getY() - _center.getY());
    final double z = Math.abs(pntB.getZ() - _center.getZ());
    setData(_center, x, y, z);
  }

  /**
   * Updates the center point and extents of this box using the defined values.
   *
   * @param center
   *          The center of the box.
   * @param xExtent
   *          x extent of the box
   * @param yExtent
   *          y extent of the box
   * @param zExtent
   *          z extent of the box
   */
  public void setData(final ReadOnlyVector3 center, final double xExtent, final double yExtent, final double zExtent) {
    if (center != null) {
      _center.set(center);
    }

    _xExtent = xExtent;
    _yExtent = yExtent;
    _zExtent = zExtent;

    setVertexData();
    setNormalData();
    setTextureData();
    setIndexData();

  }

  /**
   * <code>setVertexData</code> sets the vertex positions that define the box using the center point
   * and defined extents.
   */
  protected void setVertexData() {
    if (_meshData.getVertexBuffer() == null) {
      _meshData.setVertexBuffer(BufferUtils.createVector3Buffer(24));
    }

    final Vector3[] vert = computeVertices(); // returns 8

    // Back
    BufferUtils.setInBuffer(vert[0], _meshData.getVertexBuffer(), 0);
    BufferUtils.setInBuffer(vert[1], _meshData.getVertexBuffer(), 1);
    BufferUtils.setInBuffer(vert[2], _meshData.getVertexBuffer(), 2);
    BufferUtils.setInBuffer(vert[3], _meshData.getVertexBuffer(), 3);

    // Right
    BufferUtils.setInBuffer(vert[1], _meshData.getVertexBuffer(), 4);
    BufferUtils.setInBuffer(vert[4], _meshData.getVertexBuffer(), 5);
    BufferUtils.setInBuffer(vert[6], _meshData.getVertexBuffer(), 6);
    BufferUtils.setInBuffer(vert[2], _meshData.getVertexBuffer(), 7);

    // Front
    BufferUtils.setInBuffer(vert[4], _meshData.getVertexBuffer(), 8);
    BufferUtils.setInBuffer(vert[5], _meshData.getVertexBuffer(), 9);
    BufferUtils.setInBuffer(vert[7], _meshData.getVertexBuffer(), 10);
    BufferUtils.setInBuffer(vert[6], _meshData.getVertexBuffer(), 11);

    // Left
    BufferUtils.setInBuffer(vert[5], _meshData.getVertexBuffer(), 12);
    BufferUtils.setInBuffer(vert[0], _meshData.getVertexBuffer(), 13);
    BufferUtils.setInBuffer(vert[3], _meshData.getVertexBuffer(), 14);
    BufferUtils.setInBuffer(vert[7], _meshData.getVertexBuffer(), 15);

    // Top
    BufferUtils.setInBuffer(vert[2], _meshData.getVertexBuffer(), 16);
    BufferUtils.setInBuffer(vert[6], _meshData.getVertexBuffer(), 17);
    BufferUtils.setInBuffer(vert[7], _meshData.getVertexBuffer(), 18);
    BufferUtils.setInBuffer(vert[3], _meshData.getVertexBuffer(), 19);

    // Bottom
    BufferUtils.setInBuffer(vert[0], _meshData.getVertexBuffer(), 20);
    BufferUtils.setInBuffer(vert[5], _meshData.getVertexBuffer(), 21);
    BufferUtils.setInBuffer(vert[4], _meshData.getVertexBuffer(), 22);
    BufferUtils.setInBuffer(vert[1], _meshData.getVertexBuffer(), 23);
  }

  /**
   * <code>setNormalData</code> sets the normals of each of the box's planes.
   */
  private void setNormalData() {
    if (_meshData.getNormalBuffer() == null) {
      _meshData.setNormalBuffer(BufferUtils.createVector3Buffer(24));

      // back
      for (int i = 0; i < 4; i++) {
        _meshData.getNormalBuffer().put(0).put(0).put(-1);
      }

      // right
      for (int i = 0; i < 4; i++) {
        _meshData.getNormalBuffer().put(1).put(0).put(0);
      }

      // front
      for (int i = 0; i < 4; i++) {
        _meshData.getNormalBuffer().put(0).put(0).put(1);
      }

      // left
      for (int i = 0; i < 4; i++) {
        _meshData.getNormalBuffer().put(-1).put(0).put(0);
      }

      // top
      for (int i = 0; i < 4; i++) {
        _meshData.getNormalBuffer().put(0).put(1).put(0);
      }

      // bottom
      for (int i = 0; i < 4; i++) {
        _meshData.getNormalBuffer().put(0).put(-1).put(0);
      }
    }
  }

  /**
   * <code>setTextureData</code> sets the points that define the texture of the box. It's a one-to-one
   * ratio, where each plane of the box has it's own copy of the texture. That is, the texture is
   * repeated one time for each six faces.
   */
  private void setTextureData() {
    if (_meshData.getTextureCoords(0) == null) {
      _meshData.setTextureBuffer(BufferUtils.createVector2Buffer(24), 0);
      final FloatBuffer tex = _meshData.getTextureBuffer(0);

      for (int i = 0; i < 6; i++) {
        tex.put(1).put(0);
        tex.put(0).put(0);
        tex.put(0).put(1);
        tex.put(1).put(1);
      }
    }
  }

  /**
   * <code>setIndexData</code> sets the indices into the list of vertices, defining all triangles that
   * constitute the box.
   */
  private void setIndexData() {
    if (_meshData.getIndexBuffer() == null) {
      final byte[] indices = {2, 1, 0, 3, 2, 0, 6, 5, 4, 7, 6, 4, 10, 9, 8, 11, 10, 8, 14, 13, 12, 15, 14, 12, 18, 17,
          16, 19, 18, 16, 22, 21, 20, 23, 22, 20};
      final ByteBuffer buf = BufferUtils.createByteBuffer(indices.length);
      buf.put(indices);
      buf.rewind();
      _meshData.setIndexBuffer(buf);
    }
  }

  /**
   * <code>clone</code> creates a new Box object containing the same data as this one.
   *
   * @return the new Box
   */
  @Override
  public Box clone() {
    return new Box(getName() + "_clone", _center.clone(), _xExtent, _yExtent, _zExtent);
  }

  /**
   * @return a size 8 array of Vectors representing the 8 points of the box.
   */
  public Vector3[] computeVertices() {

    final Vector3 rVal[] = new Vector3[8];
    rVal[0] = _center.add(-_xExtent, -_yExtent, -_zExtent, null);
    rVal[1] = _center.add(_xExtent, -_yExtent, -_zExtent, null);
    rVal[2] = _center.add(_xExtent, _yExtent, -_zExtent, null);
    rVal[3] = _center.add(-_xExtent, _yExtent, -_zExtent, null);
    rVal[4] = _center.add(_xExtent, -_yExtent, _zExtent, null);
    rVal[5] = _center.add(-_xExtent, -_yExtent, _zExtent, null);
    rVal[6] = _center.add(_xExtent, _yExtent, _zExtent, null);
    rVal[7] = _center.add(-_xExtent, _yExtent, _zExtent, null);
    return rVal;
  }

  @Override
  public void write(final OutputCapsule capsule) throws IOException {
    super.write(capsule);
    capsule.write(_xExtent, "xExtent", 0);
    capsule.write(_yExtent, "yExtent", 0);
    capsule.write(_zExtent, "zExtent", 0);
    capsule.write(_center, "center", (Vector3) Vector3.ZERO);

  }

  @Override
  public void read(final InputCapsule capsule) throws IOException {
    super.read(capsule);
    _xExtent = capsule.readDouble("xExtent", 0);
    _yExtent = capsule.readDouble("yExtent", 0);
    _zExtent = capsule.readDouble("zExtent", 0);
    _center.set(capsule.readSavable("center", (Vector3) Vector3.ZERO));
  }
}
