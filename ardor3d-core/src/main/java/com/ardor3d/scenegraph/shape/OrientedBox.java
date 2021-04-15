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

import com.ardor3d.buffer.BufferUtils;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.util.export.CapsuleUtils;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;

/**
 * This primitive represents a box that has options to orient it according to its X/Y/Z axis. It is
 * used to create an OrientedBoundingBox mostly.
 */
public class OrientedBox extends Mesh {

  /** Center of the Oriented Box. */
  protected Vector3 _center;

  /** X axis of the Oriented Box. */
  protected Vector3 _xAxis = new Vector3(1, 0, 0);

  /** Y axis of the Oriented Box. */
  protected Vector3 _yAxis = new Vector3(0, 1, 0);

  /** Z axis of the Oriented Box. */
  protected Vector3 _zAxis = new Vector3(0, 0, 1);

  /** Extents of the box along the x,y,z axis. */
  protected Vector3 _extent = new Vector3(0, 0, 0);

  /** Texture coordintae values for the corners of the box. */
  protected Vector2 _texTopRight, _texTopLeft, _texBotRight, _texBotLeft;

  /** Vector array used to store the array of 8 corners the box has. */
  public Vector3[] _vectorStore;

  /**
   * If true, the box's vectorStore array correctly represnts the box's corners.
   */
  public boolean _correctCorners;

  public OrientedBox() {}

  /**
   * Creates a new OrientedBox with the given name.
   *
   * @param name
   *          The name of the new box.
   */
  public OrientedBox(final String name) {
    super(name);
    _vectorStore = new Vector3[8];
    for (int i = 0; i < _vectorStore.length; i++) {
      _vectorStore[i] = new Vector3();
    }
    _texTopRight = new Vector2(1, 1);
    _texTopLeft = new Vector2(1, 0);
    _texBotRight = new Vector2(0, 1);
    _texBotLeft = new Vector2(0, 0);
    _center = new Vector3(0, 0, 0);
    _correctCorners = false;
    computeInformation();
  }

  /**
   * Takes the plane and center information and creates the correct vertex,normal,color,texture,index
   * information to represent the OrientedBox.
   */
  public void computeInformation() {
    setVertexData();
    setNormalData();
    setTextureData();
    setIndexData();
  }

  /**
   * Sets the correct indices array for the box.
   */
  private void setIndexData() {
    if (_meshData.getIndices() == null) {
      _meshData.setIndexBuffer(BufferUtils.createByteBuffer(36));

      for (int i = 0; i < 6; i++) {
        _meshData.getIndices().put(i * 4 + 0);
        _meshData.getIndices().put(i * 4 + 1);
        _meshData.getIndices().put(i * 4 + 3);
        _meshData.getIndices().put(i * 4 + 1);
        _meshData.getIndices().put(i * 4 + 2);
        _meshData.getIndices().put(i * 4 + 3);
      }
    }
  }

  /**
   * Sets the correct texture array for the box.
   */
  private void setTextureData() {
    if (_meshData.getTextureBuffer(0) == null) {
      _meshData.setTextureBuffer(BufferUtils.createVector2Buffer(24), 0);

      for (int x = 0; x < 6; x++) {
        _meshData.getTextureCoords(0).getBuffer().put(_texTopRight.getXf()).put(_texTopRight.getYf());
        _meshData.getTextureCoords(0).getBuffer().put(_texTopLeft.getXf()).put(_texTopLeft.getYf());
        _meshData.getTextureCoords(0).getBuffer().put(_texBotLeft.getXf()).put(_texBotLeft.getYf());
        _meshData.getTextureCoords(0).getBuffer().put(_texBotRight.getXf()).put(_texBotRight.getYf());
      }
    }
  }

  /**
   * Sets the correct normal array for the box.
   */
  private void setNormalData() {
    if (_meshData.getNormalBuffer() == null) {
      _meshData.setNormalBuffer(BufferUtils.createVector3Buffer(24));
    } else {
      _meshData.getNormalBuffer().rewind();
    }

    // top
    _meshData.getNormalBuffer().put(_yAxis.getXf()).put(_yAxis.getYf()).put(_yAxis.getZf());
    _meshData.getNormalBuffer().put(_yAxis.getXf()).put(_yAxis.getYf()).put(_yAxis.getZf());
    _meshData.getNormalBuffer().put(_yAxis.getXf()).put(_yAxis.getYf()).put(_yAxis.getZf());
    _meshData.getNormalBuffer().put(_yAxis.getXf()).put(_yAxis.getYf()).put(_yAxis.getZf());

    // right
    _meshData.getNormalBuffer().put(_xAxis.getXf()).put(_xAxis.getYf()).put(_xAxis.getZf());
    _meshData.getNormalBuffer().put(_xAxis.getXf()).put(_xAxis.getYf()).put(_xAxis.getZf());
    _meshData.getNormalBuffer().put(_xAxis.getXf()).put(_xAxis.getYf()).put(_xAxis.getZf());
    _meshData.getNormalBuffer().put(_xAxis.getXf()).put(_xAxis.getYf()).put(_xAxis.getZf());

    // left
    _meshData.getNormalBuffer().put(-_xAxis.getXf()).put(-_xAxis.getYf()).put(-_xAxis.getZf());
    _meshData.getNormalBuffer().put(-_xAxis.getXf()).put(-_xAxis.getYf()).put(-_xAxis.getZf());
    _meshData.getNormalBuffer().put(-_xAxis.getXf()).put(-_xAxis.getYf()).put(-_xAxis.getZf());
    _meshData.getNormalBuffer().put(-_xAxis.getXf()).put(-_xAxis.getYf()).put(-_xAxis.getZf());

    // bottom
    _meshData.getNormalBuffer().put(-_yAxis.getXf()).put(-_yAxis.getYf()).put(-_yAxis.getZf());
    _meshData.getNormalBuffer().put(-_yAxis.getXf()).put(-_yAxis.getYf()).put(-_yAxis.getZf());
    _meshData.getNormalBuffer().put(-_yAxis.getXf()).put(-_yAxis.getYf()).put(-_yAxis.getZf());
    _meshData.getNormalBuffer().put(-_yAxis.getXf()).put(-_yAxis.getYf()).put(-_yAxis.getZf());

    // back
    _meshData.getNormalBuffer().put(-_zAxis.getXf()).put(-_zAxis.getYf()).put(-_zAxis.getZf());
    _meshData.getNormalBuffer().put(-_zAxis.getXf()).put(-_zAxis.getYf()).put(-_zAxis.getZf());
    _meshData.getNormalBuffer().put(-_zAxis.getXf()).put(-_zAxis.getYf()).put(-_zAxis.getZf());
    _meshData.getNormalBuffer().put(-_zAxis.getXf()).put(-_zAxis.getYf()).put(-_zAxis.getZf());

    // front
    _meshData.getNormalBuffer().put(_zAxis.getXf()).put(_zAxis.getYf()).put(_zAxis.getZf());
    _meshData.getNormalBuffer().put(_zAxis.getXf()).put(_zAxis.getYf()).put(_zAxis.getZf());
    _meshData.getNormalBuffer().put(_zAxis.getXf()).put(_zAxis.getYf()).put(_zAxis.getZf());
    _meshData.getNormalBuffer().put(_zAxis.getXf()).put(_zAxis.getYf()).put(_zAxis.getZf());
  }

  /**
   * Sets the correct vertex information for the box.
   */
  private void setVertexData() {
    computeCorners();
    if (_meshData.getVertexBuffer() == null) {
      _meshData.setVertexBuffer(BufferUtils.createVector3Buffer(24));
    } else {
      _meshData.getVertexBuffer().rewind();
    }

    // Top
    _meshData.getVertexBuffer().put(_vectorStore[0].getXf()).put(_vectorStore[0].getYf()).put(_vectorStore[0].getZf());
    _meshData.getVertexBuffer().put(_vectorStore[1].getXf()).put(_vectorStore[1].getYf()).put(_vectorStore[1].getZf());
    _meshData.getVertexBuffer().put(_vectorStore[5].getXf()).put(_vectorStore[5].getYf()).put(_vectorStore[5].getZf());
    _meshData.getVertexBuffer().put(_vectorStore[3].getXf()).put(_vectorStore[3].getYf()).put(_vectorStore[3].getZf());

    // Right
    _meshData.getVertexBuffer().put(_vectorStore[0].getXf()).put(_vectorStore[0].getYf()).put(_vectorStore[0].getZf());
    _meshData.getVertexBuffer().put(_vectorStore[3].getXf()).put(_vectorStore[3].getYf()).put(_vectorStore[3].getZf());
    _meshData.getVertexBuffer().put(_vectorStore[6].getXf()).put(_vectorStore[6].getYf()).put(_vectorStore[6].getZf());
    _meshData.getVertexBuffer().put(_vectorStore[2].getXf()).put(_vectorStore[2].getYf()).put(_vectorStore[2].getZf());

    // Left
    _meshData.getVertexBuffer().put(_vectorStore[5].getXf()).put(_vectorStore[5].getYf()).put(_vectorStore[5].getZf());
    _meshData.getVertexBuffer().put(_vectorStore[1].getXf()).put(_vectorStore[1].getYf()).put(_vectorStore[1].getZf());
    _meshData.getVertexBuffer().put(_vectorStore[4].getXf()).put(_vectorStore[4].getYf()).put(_vectorStore[4].getZf());
    _meshData.getVertexBuffer().put(_vectorStore[7].getXf()).put(_vectorStore[7].getYf()).put(_vectorStore[7].getZf());

    // Bottom
    _meshData.getVertexBuffer().put(_vectorStore[6].getXf()).put(_vectorStore[6].getYf()).put(_vectorStore[6].getZf());
    _meshData.getVertexBuffer().put(_vectorStore[7].getXf()).put(_vectorStore[7].getYf()).put(_vectorStore[7].getZf());
    _meshData.getVertexBuffer().put(_vectorStore[4].getXf()).put(_vectorStore[4].getYf()).put(_vectorStore[4].getZf());
    _meshData.getVertexBuffer().put(_vectorStore[2].getXf()).put(_vectorStore[2].getYf()).put(_vectorStore[2].getZf());

    // Back
    _meshData.getVertexBuffer().put(_vectorStore[3].getXf()).put(_vectorStore[3].getYf()).put(_vectorStore[3].getZf());
    _meshData.getVertexBuffer().put(_vectorStore[5].getXf()).put(_vectorStore[5].getYf()).put(_vectorStore[5].getZf());
    _meshData.getVertexBuffer().put(_vectorStore[7].getXf()).put(_vectorStore[7].getYf()).put(_vectorStore[7].getZf());
    _meshData.getVertexBuffer().put(_vectorStore[6].getXf()).put(_vectorStore[6].getYf()).put(_vectorStore[6].getZf());

    // Front
    _meshData.getVertexBuffer().put(_vectorStore[1].getXf()).put(_vectorStore[1].getYf()).put(_vectorStore[1].getZf());
    _meshData.getVertexBuffer().put(_vectorStore[4].getXf()).put(_vectorStore[4].getYf()).put(_vectorStore[4].getZf());
    _meshData.getVertexBuffer().put(_vectorStore[2].getXf()).put(_vectorStore[2].getYf()).put(_vectorStore[2].getZf());
    _meshData.getVertexBuffer().put(_vectorStore[0].getXf()).put(_vectorStore[0].getYf()).put(_vectorStore[0].getZf());
  }

  /**
   * Sets the vectorStore information to the 8 corners of the box.
   */
  public void computeCorners() {
    _correctCorners = true;

    final Vector3 tempVa = Vector3.fetchTempInstance();
    final Vector3 tempVb = Vector3.fetchTempInstance();
    final Vector3 tempVc = Vector3.fetchTempInstance();
    tempVa.set(_xAxis).multiplyLocal(_extent.getX());
    tempVb.set(_yAxis).multiplyLocal(_extent.getY());
    tempVc.set(_zAxis).multiplyLocal(_extent.getZ());

    _vectorStore[0].set(_center).addLocal(tempVa).addLocal(tempVb).addLocal(tempVc);
    _vectorStore[1].set(_center).addLocal(tempVa).subtractLocal(tempVb).addLocal(tempVc);
    _vectorStore[2].set(_center).addLocal(tempVa).addLocal(tempVb).subtractLocal(tempVc);
    _vectorStore[3].set(_center).subtractLocal(tempVa).addLocal(tempVb).addLocal(tempVc);
    _vectorStore[4].set(_center).addLocal(tempVa).subtractLocal(tempVb).subtractLocal(tempVc);
    _vectorStore[5].set(_center).subtractLocal(tempVa).subtractLocal(tempVb).addLocal(tempVc);
    _vectorStore[6].set(_center).subtractLocal(tempVa).addLocal(tempVb).subtractLocal(tempVc);
    _vectorStore[7].set(_center).subtractLocal(tempVa).subtractLocal(tempVb).subtractLocal(tempVc);

    Vector3.releaseTempInstance(tempVa);
    Vector3.releaseTempInstance(tempVb);
    Vector3.releaseTempInstance(tempVc);
  }

  /**
   * Returns the center of the box.
   *
   * @return The box's center.
   */
  public Vector3 getCenter() { return _center; }

  /**
   * Sets the box's center to the given value. Shallow copy only.
   *
   * @param center
   *          The box's new center.
   */
  public void setCenter(final Vector3 center) { _center = center; }

  /**
   * Returns the box's extent vector along the x,y,z.
   *
   * @return The box's extent vector.
   */
  public Vector3 getExtent() { return _extent; }

  /**
   * Sets the box's extent vector to the given value. Shallow copy only.
   *
   * @param extent
   *          The box's new extent.
   */
  public void setExtent(final Vector3 extent) { _extent = extent; }

  /**
   * Returns the x axis of this box.
   *
   * @return This OB's x axis.
   */
  public Vector3 getxAxis() {
    return _xAxis;
  }

  /**
   * Sets the x axis of this OB. Shallow copy.
   *
   * @param xAxis
   *          The new x axis.
   */
  public void setXAxis(final Vector3 xAxis) { _xAxis = xAxis; }

  /**
   * Gets the Y axis of this OB.
   *
   * @return This OB's Y axis.
   */
  public Vector3 getYAxis() { return _yAxis; }

  /**
   * Sets the Y axis of this OB. Shallow copy.
   *
   * @param yAxis
   *          The new Y axis.
   */
  public void setYAxis(final Vector3 yAxis) { _yAxis = yAxis; }

  /**
   * Returns the Z axis of this OB.
   *
   * @return The Z axis.
   */
  public Vector3 getZAxis() { return _zAxis; }

  /**
   * Sets the Z axis of this OB. Shallow copy.
   *
   * @param zAxis
   *          The new Z axis.
   */
  public void setZAxis(final Vector3 zAxis) { _zAxis = zAxis; }

  /**
   * Returns if the corners are set corectly.
   *
   * @return True if the vectorStore is correct.
   */
  public boolean isCorrectCorners() { return _correctCorners; }

  @Override
  public void write(final OutputCapsule capsule) throws IOException {
    super.write(capsule);
    capsule.write(_center, "center", (Vector3) Vector3.ZERO);
    capsule.write(_xAxis, "_xAxis", (Vector3) Vector3.UNIT_X);
    capsule.write(_yAxis, "yAxis", (Vector3) Vector3.UNIT_Y);
    capsule.write(_zAxis, "zAxis", (Vector3) Vector3.UNIT_Z);
    capsule.write(_extent, "extent", (Vector3) Vector3.ZERO);
    capsule.write(_texTopRight, "texTopRight", new Vector2(1, 1));
    capsule.write(_texTopLeft, "texTopLeft", new Vector2(1, 0));
    capsule.write(_texBotRight, "texBotRight", new Vector2(0, 1));
    capsule.write(_texBotLeft, "texBotLeft", new Vector2(0, 0));
    capsule.write(_vectorStore, "vectorStore", new Vector3[8]);
    capsule.write(_correctCorners, "correctCorners", false);
  }

  @Override
  public void read(final InputCapsule capsule) throws IOException {
    super.read(capsule);
    _center = capsule.readSavable("center", (Vector3) Vector3.ZERO);
    _xAxis = capsule.readSavable("_xAxis", (Vector3) Vector3.UNIT_X);
    _yAxis = capsule.readSavable("yAxis", (Vector3) Vector3.UNIT_Y);
    _zAxis = capsule.readSavable("zAxis", (Vector3) Vector3.UNIT_Z);
    _extent = capsule.readSavable("extent", (Vector3) Vector3.ZERO);
    _texTopRight = capsule.readSavable("texTopRight", new Vector2(1, 1));
    _texTopLeft = capsule.readSavable("texTopLeft", new Vector2(1, 0));
    _texBotRight = capsule.readSavable("texBotRight", new Vector2(0, 1));
    _texBotLeft = capsule.readSavable("texBotLeft", new Vector2(0, 0));
    _vectorStore = CapsuleUtils.asArray(capsule.readSavableArray("vectorStore", new Vector3[8]), Vector3.class);
    _correctCorners = capsule.readBoolean("correctCorners", false);
  }
}
