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

import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.scenegraph.FloatBufferData;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.geom.BufferUtils;

/**
 * Sphere represents a 3D object with all points equi-distance from a center point.
 */
public class Sphere extends Mesh {

  public enum TextureMode {
    Linear, Projected, Polar;
  }

  protected int _zSamples;

  protected int _radialSamples;

  /** the distance from the center point each point falls on */
  public double _radius;
  /** the center of the sphere */
  public final Vector3 _center = new Vector3();

  protected TextureMode _textureMode = TextureMode.Linear;

  protected boolean _viewInside = false;

  public Sphere() {}

  /**
   * Constructs a sphere. By default the Sphere has not geometry data or center.
   *
   * @param name
   *          The name of the sphere.
   */
  public Sphere(final String name) {
    super(name);
  }

  /**
   * Constructs a sphere with center at the origin. For details, see the other constructor.
   *
   * @param name
   *          Name of sphere.
   * @param zSamples
   *          The samples along the Z.
   * @param radialSamples
   *          The samples along the radial.
   * @param radius
   *          Radius of the sphere.
   * @see #Sphere(java.lang.String, com.ardor3d.math.Vector3, int, int, double)
   */
  public Sphere(final String name, final int zSamples, final int radialSamples, final double radius) {
    this(name, new Vector3(0, 0, 0), zSamples, radialSamples, radius);
  }

  /**
   * Constructs a sphere. All geometry data buffers are updated automatically. Both zSamples and
   * radialSamples increase the quality of the generated sphere.
   *
   * @param name
   *          Name of the sphere.
   * @param center
   *          Center of the sphere.
   * @param zSamples
   *          The number of samples along the Z.
   * @param radialSamples
   *          The number of samples along the radial.
   * @param radius
   *          The radius of the sphere.
   */
  public Sphere(final String name, final ReadOnlyVector3 center, final int zSamples, final int radialSamples,
    final double radius) {
    super(name);
    setData(center, zSamples, radialSamples, radius);
  }

  /**
   * Constructs a sphere. All geometry data buffers are updated automatically. Both zSamples and
   * radialSamples increase the quality of the generated sphere.
   *
   * @param name
   *          Name of the sphere.
   * @param center
   *          Center of the sphere.
   * @param zSamples
   *          The number of samples along the Z.
   * @param radialSamples
   *          The number of samples along the radial.
   * @param radius
   *          The radius of the sphere.
   * @param textureMode
   *          the mode to use when setting uv coordinates for this Sphere.
   */
  public Sphere(final String name, final ReadOnlyVector3 center, final int zSamples, final int radialSamples,
    final double radius, final TextureMode textureMode) {
    super(name);
    _textureMode = textureMode;
    setData(center, zSamples, radialSamples, radius);
  }

  /**
   * Changes the information of the sphere into the given values.
   *
   * @param center
   *          The new center of the sphere.
   * @param zSamples
   *          The new number of zSamples of the sphere.
   * @param radialSamples
   *          The new number of radial samples of the sphere.
   * @param radius
   *          The new radius of the sphere.
   */
  public void setData(final ReadOnlyVector3 center, final int zSamples, final int radialSamples, final double radius) {
    _center.set(center);
    _zSamples = zSamples;
    _radialSamples = radialSamples;
    _radius = radius;

    setGeometryData();
    setIndexData();
  }

  /**
   * builds the vertices based on the radius, center and radial and zSamples.
   */
  private void setGeometryData() {
    // allocate vertices
    final int verts = (_zSamples - 2) * (_radialSamples + 1) + 2;
    final FloatBufferData vertsData = _meshData.getVertexCoords();
    if (vertsData == null) {
      _meshData.setVertexBuffer(BufferUtils.createVector3Buffer(verts));
    } else {
      vertsData.setBuffer(BufferUtils.createVector3Buffer(vertsData.getBuffer(), verts));
      vertsData.markDirty();
    }

    // allocate normals if requested
    final FloatBufferData normsData = _meshData.getNormalCoords();
    if (normsData == null) {
      _meshData.setNormalBuffer(BufferUtils.createVector3Buffer(verts));
    } else {
      normsData.setBuffer(BufferUtils.createVector3Buffer(normsData.getBuffer(), verts));
      normsData.markDirty();
    }

    // allocate texture coordinates
    final FloatBufferData texData = _meshData.getTextureCoords(0);
    if (texData == null) {
      _meshData.setTextureBuffer(BufferUtils.createVector2Buffer(verts), 0);
    } else {
      texData.setBuffer(BufferUtils.createVector2Buffer(texData.getBuffer(), verts));
      texData.markDirty();
    }

    _meshData.markBuffersDirty();

    // generate geometry
    final double fInvRS = 1.0 / _radialSamples;
    final double fZFactor = 2.0 / (_zSamples - 1);

    // Generate points on the unit circle to be used in computing the mesh
    // points on a sphere slice.
    final double[] afSin = new double[(_radialSamples + 1)];
    final double[] afCos = new double[(_radialSamples + 1)];
    for (int iR = 0; iR < _radialSamples; iR++) {
      final double fAngle = MathUtils.TWO_PI * fInvRS * iR;
      afCos[iR] = MathUtils.cos(fAngle);
      afSin[iR] = MathUtils.sin(fAngle);
    }
    afSin[_radialSamples] = afSin[0];
    afCos[_radialSamples] = afCos[0];

    // generate the sphere itself
    int i = 0;
    final Vector3 tempVa = Vector3.fetchTempInstance();
    final Vector3 tempVb = Vector3.fetchTempInstance();
    final Vector3 tempVc = Vector3.fetchTempInstance();
    for (int iZ = 1; iZ < (_zSamples - 1); iZ++) {
      final double fAFraction = MathUtils.HALF_PI * (-1.0f + fZFactor * iZ); // in (-pi/2, pi/2)
      final double fZFraction = MathUtils.sin(fAFraction); // in (-1,1)
      final double fZ = _radius * fZFraction;

      // compute center of slice
      final Vector3 kSliceCenter = tempVb.set(_center);
      kSliceCenter.setZ(kSliceCenter.getZ() + fZ);

      // compute radius of slice
      final double fSliceRadius = Math.sqrt(Math.abs(_radius * _radius - fZ * fZ));

      // compute slice vertices with duplication at end point
      Vector3 kNormal;
      final int iSave = i;
      for (int iR = 0; iR < _radialSamples; iR++) {
        final double fRadialFraction = iR * fInvRS; // in [0,1)
        final Vector3 kRadial = tempVc.set(afCos[iR], afSin[iR], 0);
        kRadial.multiply(fSliceRadius, tempVa);
        _meshData.getVertexBuffer().put((float) (kSliceCenter.getX() + tempVa.getX()))
            .put((float) (kSliceCenter.getY() + tempVa.getY())).put((float) (kSliceCenter.getZ() + tempVa.getZ()));

        BufferUtils.populateFromBuffer(tempVa, _meshData.getVertexBuffer(), i);
        kNormal = tempVa.subtractLocal(_center);
        kNormal.normalizeLocal();
        if (!_viewInside) {
          _meshData.getNormalBuffer().put(kNormal.getXf()).put(kNormal.getYf()).put(kNormal.getZf());
        } else {
          _meshData.getNormalBuffer().put(-kNormal.getXf()).put(-kNormal.getYf()).put(-kNormal.getZf());
        }

        if (_textureMode == TextureMode.Linear) {
          _meshData.getTextureCoords(0).getBuffer().put((float) fRadialFraction)
              .put((float) (0.5 * (fZFraction + 1.0)));
        } else if (_textureMode == TextureMode.Projected) {
          _meshData.getTextureCoords(0).getBuffer().put((float) fRadialFraction)
              .put((float) (MathUtils.INV_PI * (MathUtils.HALF_PI + Math.asin(fZFraction))));
        } else if (_textureMode == TextureMode.Polar) {
          final double r = (MathUtils.HALF_PI - Math.abs(fAFraction)) / MathUtils.PI;
          final double u = r * afCos[iR] + 0.5;
          final double v = r * afSin[iR] + 0.5;
          _meshData.getTextureCoords(0).getBuffer().put((float) u).put((float) v);
        }

        i++;
      }

      BufferUtils.copyInternalVector3(_meshData.getVertexBuffer(), iSave, i);
      BufferUtils.copyInternalVector3(_meshData.getNormalBuffer(), iSave, i);

      if (_textureMode == TextureMode.Linear) {
        _meshData.getTextureCoords(0).getBuffer().put(1.0f).put((float) (0.5 * (fZFraction + 1.0)));
      } else if (_textureMode == TextureMode.Projected) {
        _meshData.getTextureCoords(0).getBuffer().put(1.0f)
            .put((float) (MathUtils.INV_PI * (MathUtils.HALF_PI + Math.asin(fZFraction))));
      } else if (_textureMode == TextureMode.Polar) {
        final float r = (float) ((MathUtils.HALF_PI - Math.abs(fAFraction)) / MathUtils.PI);
        _meshData.getTextureCoords(0).getBuffer().put(r + 0.5f).put(0.5f);
      }

      i++;
    }

    // south pole
    _meshData.getVertexBuffer().position(i * 3);
    _meshData.getVertexBuffer().put(_center.getXf()).put(_center.getYf()).put((float) (_center.getZ() - _radius));

    _meshData.getNormalBuffer().position(i * 3);
    if (!_viewInside) {
      // TODO: allow for inner texture orientation later.
      _meshData.getNormalBuffer().put(0).put(0).put(-1);
    } else {
      _meshData.getNormalBuffer().put(0).put(0).put(1);
    }

    _meshData.getTextureCoords(0).getBuffer().position(i * 2);
    if (_textureMode == TextureMode.Polar) {
      _meshData.getTextureCoords(0).getBuffer().put(0.5f).put(0.5f);
    } else {
      _meshData.getTextureCoords(0).getBuffer().put(0.5f).put(0.0f);
    }

    i++;

    // north pole
    _meshData.getVertexBuffer().put(_center.getXf()).put(_center.getYf()).put((float) (_center.getZ() + _radius));

    if (!_viewInside) {
      _meshData.getNormalBuffer().put(0).put(0).put(1);
    } else {
      _meshData.getNormalBuffer().put(0).put(0).put(-1);
    }

    if (_textureMode == TextureMode.Polar) {
      _meshData.getTextureCoords(0).getBuffer().put(0.5f).put(0.5f);
    } else {
      _meshData.getTextureCoords(0).getBuffer().put(0.5f).put(1.0f);
    }
    Vector3.releaseTempInstance(tempVa);
    Vector3.releaseTempInstance(tempVb);
    Vector3.releaseTempInstance(tempVc);
  }

  /**
   * sets the indices for rendering the sphere.
   */
  private void setIndexData() {
    // allocate connectivity
    final int verts = (_zSamples - 2) * (_radialSamples + 1) + 2;
    final int tris = 2 * (_zSamples - 2) * _radialSamples;
    if (_meshData.getIndices() == null || _meshData.getIndexBuffer().capacity() != 3 * tris) {
      _meshData.setIndices(BufferUtils.createIndexBufferData(3 * tris, verts - 1));
    } else {
      _meshData.getIndexBuffer().rewind();
    }

    // generate connectivity
    for (int iZ = 0, iZStart = 0; iZ < (_zSamples - 3); iZ++) {
      int i0 = iZStart;
      int i1 = i0 + 1;
      iZStart += (_radialSamples + 1);
      int i2 = iZStart;
      int i3 = i2 + 1;
      for (int i = 0; i < _radialSamples; i++) {
        if (!_viewInside) {
          _meshData.getIndices().put(i0++);
          _meshData.getIndices().put(i1);
          _meshData.getIndices().put(i2);
          _meshData.getIndices().put(i1++);
          _meshData.getIndices().put(i3++);
          _meshData.getIndices().put(i2++);
        } else // inside view
        {
          _meshData.getIndices().put(i0++);
          _meshData.getIndices().put(i2);
          _meshData.getIndices().put(i1);
          _meshData.getIndices().put(i1++);
          _meshData.getIndices().put(i2++);
          _meshData.getIndices().put(i3++);
        }
      }
    }

    // south pole triangles
    for (int i = 0; i < _radialSamples; i++) {
      if (!_viewInside) {
        _meshData.getIndices().put(i);
        _meshData.getIndices().put(_meshData.getVertexCount() - 2);
        _meshData.getIndices().put(i + 1);
      } else // inside view
      {
        _meshData.getIndices().put(i);
        _meshData.getIndices().put(i + 1);
        _meshData.getIndices().put(_meshData.getVertexCount() - 2);
      }
    }

    // north pole triangles
    final int iOffset = (_zSamples - 3) * (_radialSamples + 1);
    for (int i = 0; i < _radialSamples; i++) {
      if (!_viewInside) {
        _meshData.getIndices().put(i + iOffset);
        _meshData.getIndices().put(i + 1 + iOffset);
        _meshData.getIndices().put(_meshData.getVertexCount() - 1);
      } else // inside view
      {
        _meshData.getIndices().put(i + iOffset);
        _meshData.getIndices().put(_meshData.getVertexCount() - 1);
        _meshData.getIndices().put(i + 1 + iOffset);
      }
    }
  }

  /**
   * Returns the center of this sphere.
   *
   * @return The sphere's center.
   */
  public Vector3 getCenter() { return _center; }

  /**
   *
   * @return true if the normals are inverted to point into the sphere so that the face is oriented
   *         for a viewer inside the sphere. false (the default) for exterior viewing.
   */
  public boolean isViewFromInside() { return _viewInside; }

  /**
   *
   * @param viewInside
   *          if true, the normals are inverted to point into the sphere so that the face is oriented
   *          for a viewer inside the sphere. Default is false (for outside viewing)
   */
  public void setViewFromInside(final boolean viewInside) {
    if (viewInside != _viewInside) {
      _viewInside = viewInside;
      setGeometryData();
      setIndexData();
    }
  }

  /**
   * @return Returns the textureMode.
   */
  public TextureMode getTextureMode() { return _textureMode; }

  /**
   * @param textureMode
   *          The textureMode to set.
   */
  public void setTextureMode(final TextureMode textureMode) {
    _textureMode = textureMode;
    setGeometryData();
    setIndexData();
  }

  public double getRadius() { return _radius; }

  @Override
  public void write(final OutputCapsule capsule) throws IOException {
    super.write(capsule);
    capsule.write(_zSamples, "zSamples", 0);
    capsule.write(_radialSamples, "radialSamples", 0);
    capsule.write(_radius, "radius", 0);
    capsule.write(_center, "center", (Vector3) Vector3.ZERO);
    capsule.write(_textureMode, "textureMode", TextureMode.Linear);
    capsule.write(_viewInside, "viewInside", false);
  }

  @Override
  public void read(final InputCapsule capsule) throws IOException {
    super.read(capsule);
    _zSamples = capsule.readInt("zSamples", 0);
    _radialSamples = capsule.readInt("radialSamples", 0);
    _radius = capsule.readDouble("radius", 0);
    _center.set(capsule.readSavable("center", (Vector3) Vector3.ZERO));
    _textureMode = capsule.readEnum("textureMode", TextureMode.class, TextureMode.Linear);
    _viewInside = capsule.readBoolean("viewInside", false);
  }
}
