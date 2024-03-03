/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.bounding;

import java.util.Comparator;

import com.ardor3d.intersection.PrimitiveKey;
import com.ardor3d.math.Vector3;
import com.ardor3d.scenegraph.Mesh;

public class TreeComparator implements Comparator<PrimitiveKey> {
  enum Axis {
    X, Y, Z;
  }

  private Axis _axis;

  private Mesh _mesh;

  private Vector3[] _aCompare = null;

  private Vector3[] _bCompare = null;

  public void setAxis(final Axis axis) { _axis = axis; }

  public void setMesh(final Mesh mesh) { _mesh = mesh; }

  @Override
  public int compare(final PrimitiveKey o1, final PrimitiveKey o2) {

    if (o1.equals(o2)) {
      return 0;
    }

    Vector3 centerA = null;
    Vector3 centerB = null;
    _aCompare = _mesh.getMeshData().getPrimitiveVertices(o1.getPrimitiveIndex(), o1.getSection(), _aCompare);
    _bCompare = _mesh.getMeshData().getPrimitiveVertices(o2.getPrimitiveIndex(), o2.getSection(), _bCompare);

    for (int i = 1; i < _aCompare.length; i++) {
      _aCompare[0].addLocal(_aCompare[i]);
    }
    for (int i = 1; i < _bCompare.length; i++) {
      _bCompare[0].addLocal(_bCompare[i]);
    }
    if (_aCompare.length == _bCompare.length) {
      // don't need average since lists are same size. (3X < 3Y ? X < Y)
      centerA = _aCompare[0];
      centerB = _bCompare[0];
    } else {
      // perform average since we have different size lists
      centerA = _aCompare[0].divideLocal(_aCompare.length);
      centerB = _bCompare[0].divideLocal(_bCompare.length);
    }

    return switch (_axis) {
      case X -> Double.compare(centerA.getX(), centerB.getX());
      case Y -> Double.compare(centerA.getY(), centerB.getY());
      case Z -> Double.compare(centerA.getZ(), centerB.getZ());
    };
  }
}
