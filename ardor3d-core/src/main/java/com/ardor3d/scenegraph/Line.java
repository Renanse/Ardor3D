/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
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
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ardor3d.buffer.BufferUtils;
import com.ardor3d.buffer.FloatBufferData;
import com.ardor3d.buffer.FloatBufferDataUtil;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.math.type.ReadOnlyVector2;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;

public class Line extends Mesh {

  private static final Logger logger = Logger.getLogger(Line.class.getName());

  public final static String KEY_Distances = "distance";

  public final static String KEY_TexV = "texV";

  private float _lineWidth;
  private float _miterLimit;
  private boolean _antialiased = false;
  private short _stipplePattern = (short) 0xFFFF;
  private float _stippleFactor = 1f;

  public Line() {
    this("line");
  }

  /**
   * Constructs a new line with the given name. By default, the line has no geometric data.
   *
   * @param name
   *          The name of the line.
   */
  public Line(final String name) {
    super(name);

    _meshData.setIndexMode(IndexMode.Lines);
    setLineWidth(1.0f);
    setMiterLimit(.75f);
  }

  /**
   * Constructor instantiates a new <code>Line</code> object with a given set of data. Any data can be
   * null except for the vertex list. If vertices are null an exception will be thrown.
   *
   * @param name
   *          the name of the scene element. This is required for identification and comparison
   *          purposes.
   * @param vertex
   *          the vertices that make up the lines.
   * @param normal
   *          the normals of the lines.
   * @param color
   *          the color of each point of the lines.
   * @param coords
   *          the texture coordinates of the lines.
   */
  public Line(final String name, final FloatBuffer vertex, final FloatBuffer normal, final FloatBuffer color,
    final FloatBufferData coords) {
    super(name);
    setupData(vertex, normal, color, coords);
    _meshData.setIndexMode(IndexMode.Lines);
  }

  /**
   * Constructor instantiates a new <code>Line</code> object with a given set of data. Any data can be
   * null except for the vertex list. If vertices are null an exception will be thrown.
   *
   * @param name
   *          the name of the scene element. This is required for identification and comparison
   *          purposes.
   * @param vertex
   *          the vertices that make up the lines.
   * @param normal
   *          the normals of the lines.
   * @param color
   *          the color of each point of the lines.
   * @param texture
   *          the texture coordinates of the lines.
   */
  public Line(final String name, final ReadOnlyVector3[] vertex, final ReadOnlyVector3[] normal,
    final ReadOnlyColorRGBA[] color, final ReadOnlyVector2[] texture) {
    super(name);
    setupData(BufferUtils.createFloatBuffer(vertex), BufferUtils.createFloatBuffer(normal),
        BufferUtils.createFloatBuffer(color), FloatBufferDataUtil.makeNew(texture));
    _meshData.setIndexMode(IndexMode.Lines);
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
    _meshData.setIndices(null);

    updateLineDistances();
  }

  /**
   * @return true if lines are to be drawn anti-aliased. This is used only as a hint to MaterialUtil
   *         for choosing an appropriate material.
   */
  public boolean isAntialiased() { return _antialiased; }

  /**
   * Hints whether the line should be anti-aliased. This is used only as a hint to MaterialUtil for
   * choosing an appropriate material. May decrease performance as it requires rendering 3x as many
   * triangles as without. If you want to enabled anti-aliasing, you should also use an alpha state
   * with a source of SourceFunction.SourceAlpha and a destination of DB_ONE_MINUS_SRC_ALPHA or
   * DB_ONE.
   *
   * @param antialiased
   *          true if the line should be anti-aliased.
   */
  public void setAntialiased(final boolean antialiased) { _antialiased = antialiased; }

  /**
   * @return the width of this line in pixels.
   */
  public float getLineWidth() { return _lineWidth; }

  /**
   * Sets the desired pixel width of the line when drawn.
   *
   * @param lineWidth
   *          The lineWidth to set.
   */
  public void setLineWidth(final float lineWidth) {
    _lineWidth = lineWidth;
    setProperty("lineWidth", _lineWidth);
  }

  /**
   * @return the dot product limit between line segments where we will draw a miter. The range is [-1,
   *         1]. In effect, a value of 1 will mean ALWAYS add a miter joint, while -1 will mean NEVER
   *         add one. Default is .75.
   */
  public float getMiterLimit() { return _miterLimit; }

  /**
   * Sets the dot product limit between line segments where we will draw a miter. The range is [-1,
   * 1]. In effect, a value of 1 will mean ALWAYS add a miter joint, while -1 will mean NEVER add one.
   * Default is .75.
   *
   * @param limit
   *          The limit to set. Should be in the range [-1, 1], however this is not enforced.
   */
  public void setMiterLimit(final float limit) {
    _miterLimit = limit;
    setProperty("miterLimit", _miterLimit);
  }

  /**
   * @return the set stipplePattern. 0xFFFF means no stipple.
   */
  public short getStipplePattern() { return _stipplePattern; }

  /**
   * The stipple or pattern to use when drawing this line. 0xFFFF is a solid line.
   *
   * @param stipplePattern
   *          a 16bit short whose bits describe the pattern to use when drawing this line
   */
  public void setStipplePattern(final short stipplePattern) {
    _stipplePattern = stipplePattern;
    setProperty("stipplePattern", _stipplePattern);
  }

  /**
   * @return the set stippleFactor.
   */
  public float getStippleFactor() { return _stippleFactor; }

  /**
   * @param stippleFactor
   *          magnification factor to apply to the stipple pattern.
   */
  public void setStippleFactor(final float stippleFactor) {
    _stippleFactor = stippleFactor;
    setProperty("stippleFactor", _stippleFactor);
  }

  /**
   * Calculate the distance along the line at each point on the line.
   */
  public void updateLineDistances() {
    final MeshData md = getMeshData();
    final FloatBuffer positions = md.getVertexBuffer();
    if (positions == null) {
      Line.logger.log(Level.WARNING, "Unable to generate line distances - vertices were null.");
      return;
    }

    final int vCount = md.getVertexCount();
    FloatBufferData dists = md.getCoords(Line.KEY_Distances);
    if (dists == null || dists.getBufferCapacity() != vCount) {
      dists = new FloatBufferData(vCount, 1);
      md.setCoords(Line.KEY_Distances, dists);
    }

    float d = 0f;
    final Vector3 prevPos = new Vector3();
    final Vector3 nextPos = new Vector3();
    BufferUtils.populateFromBuffer(nextPos, positions, 0);
    dists.put(0f);
    for (int i = 1; i < vCount; i++) {
      prevPos.set(nextPos);
      BufferUtils.populateFromBuffer(nextPos, positions, i);
      d += nextPos.subtract(prevPos, prevPos).length();
      dists.put(d);
    }
  }

  @Override
  public Line makeCopy(final boolean shareGeometricData) {
    final Line lineCopy = (Line) super.makeCopy(shareGeometricData);
    lineCopy.setLineWidth(_lineWidth);
    lineCopy.setMiterLimit(_miterLimit);
    lineCopy.setAntialiased(_antialiased);
    lineCopy.setStippleFactor(_stippleFactor);
    lineCopy.setStipplePattern(_stipplePattern);
    return lineCopy;
  }

  @Override
  public void write(final OutputCapsule capsule) throws IOException {
    super.write(capsule);
    capsule.write(_lineWidth, "lineWidth", 1.0f);
    capsule.write(_miterLimit, "miterLimit", 0.75f);
    capsule.write(_antialiased, "antialiased", false);
    capsule.write(_stippleFactor, "stippleFactor", 1f);
    capsule.write(_stipplePattern, "stipplePattern", (short) 0xFFFF);
  }

  @Override
  public void read(final InputCapsule capsule) throws IOException {
    super.read(capsule);
    setLineWidth(capsule.readFloat("lineWidth", 1.0f));
    setMiterLimit(capsule.readFloat("miterLimit", 0.75f));
    setAntialiased(capsule.readBoolean("antialiased", false));
    setStippleFactor(capsule.readFloat("stippleFactor", 1f));
    setStipplePattern(capsule.readShort("stipplePattern", (short) 0xFFFF));
  }
}
