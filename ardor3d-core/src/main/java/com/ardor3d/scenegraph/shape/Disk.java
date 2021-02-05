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

import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.util.MathUtils;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.geom.BufferUtils;

/**
 * An approximations of a flat circle. It is simply defined with a radius. It starts out flat along
 * the Z, with center at the origin.
 */
public class Disk extends Mesh {

  protected int _shellSamples;

  protected int _radialSamples;

  protected double _radius;

  protected double _innerRadius;

  public Disk() {}

  /**
   * Creates a flat disk (circle) at the origin flat along the Z. Usually, a higher sample number
   * creates a better looking cylinder, but at the cost of more vertex information.
   *
   * @param name
   *          The name of the disk.
   * @param shellSamples
   *          The number of shell samples.
   * @param radialSamples
   *          The number of radial samples.
   * @param radius
   *          The radius of the disk.
   */
  public Disk(final String name, final int shellSamples, final int radialSamples, final double radius) {
    this(name, shellSamples, radialSamples, radius, 0);
  }

  /**
   * Creates a flat disk (circle) at the origin flat along the Z. Usually, a higher sample number
   * creates a better looking cylinder, but at the cost of more vertex information.
   *
   * @param name
   *          The name of the disk.
   * @param shellSamples
   *          The number of shell samples.
   * @param radialSamples
   *          The number of radial samples.
   * @param radius
   *          The radius of the disk.
   * @param innerRadius
   *          The inner radius of the disk. If greater than 0, the center of the disk has a hole of
   *          this size.
   */
  public Disk(final String name, final int shellSamples, final int radialSamples, final double radius,
    final double innerRadius) {
    super(name);

    _shellSamples = shellSamples;
    _radialSamples = radialSamples;
    _radius = radius;
    _innerRadius = innerRadius;

    final int shellLess = shellSamples - 1;
    // allocate vertices
    final int verts = _innerRadius <= 0 ? 1 + radialSamples * shellLess : radialSamples * shellSamples;
    _meshData.setVertexBuffer(BufferUtils.createVector3Buffer(verts));
    _meshData.setNormalBuffer(BufferUtils.createVector3Buffer(verts));
    _meshData.setTextureBuffer(BufferUtils.createVector2Buffer(verts), 0);

    final int tris = radialSamples * (2 * shellLess - (_innerRadius <= 0 ? 1 : 0));
    _meshData.setIndices(BufferUtils.createIndexBufferData(3 * tris, verts - 1));

    setGeometryData();
    setIndexData();

  }

  private void setGeometryData() {

    // normals
    for (int x = 0, maxX = _meshData.getVertexCount(); x < maxX; x++) {
      _meshData.getNormalBuffer().put(0).put(0).put(1);
    }

    // generate geometry
    final int shellLess = _shellSamples - 1;
    final double inverseShellLess = 1.0 / shellLess;
    final double inverseRadial = 1.0 / _radialSamples;
    final Vector3 radialFraction = new Vector3();
    final Vector2 texCoord = new Vector2();

    if (_innerRadius <= 0) {
      // no hole!
      // center of disk
      _meshData.getVertexBuffer().put(0).put(0).put(0);
      _meshData.getTextureCoords(0).getBuffer().put(.5f).put(.5f);

      for (int radialCount = 0; radialCount < _radialSamples; radialCount++) {
        final double angle = MathUtils.TWO_PI * inverseRadial * radialCount;
        final double cos = Math.cos(angle);
        final double sin = Math.sin(angle);
        final Vector3 radial = new Vector3(cos, sin, 0);

        for (int shellCount = 1; shellCount < _shellSamples; shellCount++) {
          final double fraction = inverseShellLess * shellCount; // in (0,R]
          radialFraction.set(radial).multiplyLocal(fraction);
          final int i = shellCount + shellLess * radialCount;
          texCoord.setX(0.5 * (1.0 + radialFraction.getX()));
          texCoord.setY(0.5 * (1.0 + radialFraction.getY()));
          BufferUtils.setInBuffer(texCoord, _meshData.getTextureCoords(0).getBuffer(), i);

          radialFraction.multiplyLocal(_radius);
          BufferUtils.setInBuffer(radialFraction, _meshData.getVertexBuffer(), i);
        }
      }

    } else {
      // we have a hole in the middle
      final Vector3 radialInner = new Vector3();
      for (int radialCount = 0; radialCount < _radialSamples; radialCount++) {
        final double angle = MathUtils.TWO_PI * inverseRadial * radialCount;
        final double cos = Math.cos(angle);
        final double sin = Math.sin(angle);
        final Vector3 radial = new Vector3(cos, sin, 0);
        radialInner.set(radial).multiplyLocal(_innerRadius);

        for (int shellCount = 0; shellCount < _shellSamples; shellCount++) {
          final double fraction = inverseShellLess * shellCount;
          radialFraction.set(radial).multiplyLocal(fraction);

          final int i = shellCount + _shellSamples * radialCount;

          radialFraction.multiplyLocal(_radius - _innerRadius).addLocal(radialInner);
          BufferUtils.setInBuffer(radialFraction, _meshData.getVertexBuffer(), i);

          texCoord.setX(0.5 * (1.0 + radialFraction.getX() / _radius));
          texCoord.setY(0.5 * (1.0 + radialFraction.getY() / _radius));
          BufferUtils.setInBuffer(texCoord, _meshData.getTextureCoords(0).getBuffer(), i);
        }
      }
    }

  }

  private void setIndexData() {
    final int shellLess = _shellSamples - 1;

    // generate connectivity
    if (_innerRadius <= 0) {
      // no hole!
      for (int radialCount0 = _radialSamples - 1, radialCount1 = 0; radialCount1 < _radialSamples; radialCount0 =
          radialCount1++) {
        _meshData.getIndices().put(0);
        _meshData.getIndices().put(1 + shellLess * radialCount0);
        _meshData.getIndices().put(1 + shellLess * radialCount1);
        for (int iS = 1; iS < shellLess; iS++) {
          final int i00 = iS + shellLess * radialCount0;
          final int i01 = iS + shellLess * radialCount1;
          final int i10 = i00 + 1;
          final int i11 = i01 + 1;
          _meshData.getIndices().put(i00);
          _meshData.getIndices().put(i10);
          _meshData.getIndices().put(i11);
          _meshData.getIndices().put(i00);
          _meshData.getIndices().put(i11);
          _meshData.getIndices().put(i01);
        }
      }
    } else {
      // generate a hole!
      for (int radialCount0 = _radialSamples - 1, radialCount1 = 0; radialCount1 < _radialSamples; radialCount0 =
          radialCount1++) {
        for (int iS = 0; iS < shellLess; iS++) {
          final int i00 = iS + _shellSamples * radialCount0;
          final int i01 = iS + _shellSamples * radialCount1;
          final int i10 = i00 + 1;
          final int i11 = i01 + 1;
          _meshData.getIndices().put(i00);
          _meshData.getIndices().put(i10);
          _meshData.getIndices().put(i11);
          _meshData.getIndices().put(i00);
          _meshData.getIndices().put(i11);
          _meshData.getIndices().put(i01);
        }
      }
    }
  }

  public int getRadialSamples() { return _radialSamples; }

  public int getShellSamples() { return _shellSamples; }

  public double getRadius() { return _radius; }

  public double getInnerRadius() { return _innerRadius; }

  @Override
  public void write(final OutputCapsule capsule) throws IOException {
    super.write(capsule);
    capsule.write(_shellSamples, "shellSamples", 0);
    capsule.write(_radialSamples, "radialSamples", 0);
    capsule.write(_radius, "radius", 0);
    capsule.write(_innerRadius, "innerRadius", 0);
  }

  @Override
  public void read(final InputCapsule capsule) throws IOException {
    super.read(capsule);
    _shellSamples = capsule.readInt("shellSamples", 0);
    _radialSamples = capsule.readInt("radialSamples", 0);
    _radius = capsule.readDouble("radius", 0);
    _innerRadius = capsule.readDouble("innerRadius", 0);
  }
}
