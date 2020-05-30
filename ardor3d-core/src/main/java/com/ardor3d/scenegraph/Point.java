/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.scenegraph;

import java.io.IOException;
import java.nio.FloatBuffer;

import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.math.type.ReadOnlyVector2;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.geom.BufferUtils;

/**
 * <code>Point</code> defines a collection of vertices that are rendered as single points or
 * textured sprites depending on PointType.
 */
public class Point extends Mesh {

  private float _pointSize = 1.0f;
  private boolean _sizeFromVertexShader = false;

  public Point() {
    this("point", null, null, null, (FloatBufferData) null);
  }

  /**
   * Constructor instantiates a new <code>Point</code> object with a given set of data. Any data may
   * be null, except the vertex array. If this is null an exception is thrown.
   *
   * @param name
   *          the name of the scene element. This is required for identification and comparison
   *          purposes.
   * @param vertex
   *          the vertices or points.
   * @param normal
   *          the normals of the points.
   * @param color
   *          the color of the points.
   * @param texture
   *          the texture coordinates of the points.
   */
  public Point(final String name, final ReadOnlyVector3[] vertex, final ReadOnlyVector3[] normal,
    final ReadOnlyColorRGBA[] color, final ReadOnlyVector2[] texture) {
    super(name);
    setupData(BufferUtils.createFloatBuffer(vertex), BufferUtils.createFloatBuffer(normal),
        BufferUtils.createFloatBuffer(color), FloatBufferDataUtil.makeNew(texture));
    _meshData.setIndexMode(IndexMode.Points);
  }

  /**
   * Constructor instantiates a new <code>Point</code> object with a given set of data. Any data may
   * be null, except the vertex array. If this is null an exception is thrown.
   *
   * @param name
   *          the name of the scene element. This is required for identification and comparison
   *          purposes.
   * @param vertex
   *          the vertices or points.
   * @param normal
   *          the normals of the points.
   * @param color
   *          the color of the points.
   * @param coords
   *          the texture coordinates of the points.
   */
  public Point(final String name, final FloatBuffer vertex, final FloatBuffer normal, final FloatBuffer color,
    final FloatBufferData coords) {
    super(name);
    setupData(vertex, normal, color, coords);
    _meshData.setIndexMode(IndexMode.Points);
  }

  /**
   * Initialize the meshdata object with data.
   *
   * @param vertices
   * @param normals
   * @param colors
   * @param coords
   */
  private void setupData(final FloatBuffer vertices, final FloatBuffer normals, final FloatBuffer colors,
      final FloatBufferData coords) {
    _meshData.setVertexBuffer(vertices);
    _meshData.setNormalBuffer(normals);
    _meshData.setColorBuffer(colors);
    _meshData.setTextureCoords(coords, 0);
  }

  /**
   * @return the pixel size of each point.
   */
  public float getPointSize() { return _pointSize; }

  /**
   * Sets the pixel width of the point when drawn. Non anti-aliased point sizes are rounded to the
   * nearest whole number by opengl.
   *
   * @param size
   *          The size to set.
   */
  public void setPointSize(final float size) { _pointSize = size; }

  public boolean isSizeFromVertexShader() { return _sizeFromVertexShader; }

  public void setSizeFromVertexShader(final boolean sizeFromVertexShader) {
    _sizeFromVertexShader = sizeFromVertexShader;
  }

  /**
   * Used with Serialization. Do not call this directly.
   *
   * @param s
   * @throws IOException
   * @see java.io.Serializable
   */
  private void writeObject(final java.io.ObjectOutputStream s) throws IOException {
    s.defaultWriteObject();
  }

  /**
   * Used with Serialization. Do not call this directly.
   *
   * @param s
   * @throws IOException
   * @throws ClassNotFoundException
   * @see java.io.Serializable
   */
  private void readObject(final java.io.ObjectInputStream s) throws IOException, ClassNotFoundException {
    s.defaultReadObject();
  }

  @Override
  public Point makeCopy(final boolean shareGeometricData) {
    final Point pointCopy = (Point) super.makeCopy(shareGeometricData);
    pointCopy.setSizeFromVertexShader(_sizeFromVertexShader);
    pointCopy.setPointSize(_pointSize);
    return pointCopy;
  }

  @Override
  public void write(final OutputCapsule capsule) throws IOException {
    super.write(capsule);
    capsule.write(_pointSize, "pointSize", 1);
    capsule.write(_sizeFromVertexShader, "sizeFromVertexShader", false);

  }

  @Override
  public void read(final InputCapsule capsule) throws IOException {
    super.read(capsule);
    _pointSize = capsule.readFloat("pointSize", 1);
    _sizeFromVertexShader = capsule.readBoolean("sizeFromVertexShader", false);
  }

  @Override
  public boolean render(final Renderer renderer) {
    renderer.setPointSize(isSizeFromVertexShader(), getPointSize());

    return super.render(renderer);
  }

}
