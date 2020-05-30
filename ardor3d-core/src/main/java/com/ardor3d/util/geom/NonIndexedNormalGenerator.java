/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.util.geom;

import java.util.Arrays;

import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Vector3;
import com.ardor3d.scenegraph.Mesh;

/**
 * A utility class to generate normals for a set of vertices. The triangles must be defined by just
 * the vertices, so that every 3 consecutive vertices define one triangle. However, an index array
 * must be specified to identify identical vertices properly (see method
 * {@link #generateNormals(double[], int[], double)}. If the index aray is not specified, the vertex
 * normals are currently simply taken from the faces they belong to (this might be changed in the
 * future, so that vertices are compared by their values).
 */
public class NonIndexedNormalGenerator {

  private final Vector3 _temp1 = new Vector3();

  private final Vector3 _temp2 = new Vector3();

  private final Vector3 _temp3 = new Vector3();

  private int[] _indices;

  private double _creaseAngle;

  private double[] _faceNormals;

  private int[] _normalsToSet;

  /**
   * Calculates the normals for a set of faces determined by the specified vertices. Every 3
   * consecutive vertices define one triangle.<br />
   * <strong>Please note:</strong> This method uses class fields and is not synchronized! Therefore it
   * should only be called from a single thread, unless synchronization is taken care of externally.
   * 
   * @param vertices
   *          The vertex coordinates. Every three values define one vertex
   * @param indices
   *          An array containing int values. Each value belongs to one vertex in the
   *          <code>vertices</code> array, the values are stored in the same order as the vertices.
   *          For equal vertices in the <code>vertices</code> array, the indices are also equal.
   * @param creaseAngle
   *          The maximum angle in radians between faces to which normals between the faces are
   *          interpolated to create a smooth transition
   * @return An array containing the generated normals for the geometry
   */
  public double[] generateNormals(final double[] vertices, final int[] indices, final double creaseAngle) {

    _indices = indices;
    _creaseAngle = creaseAngle;
    _normalsToSet = new int[10];
    Arrays.fill(_normalsToSet, -1);

    initFaceNormals(vertices);

    if (creaseAngle < MathUtils.ZERO_TOLERANCE || indices == null) {
      return getFacetedVertexNormals();
    }
    return getVertexNormals();
  }

  /**
   * Initializes the array <code>faceNormals</code> with the normals of all faces (triangles) of the
   * mesh.
   * 
   * @param vertices
   *          The array containing all vertex coordinates
   */
  private void initFaceNormals(final double[] vertices) {
    _faceNormals = new double[vertices.length / 3];

    for (int i = 0; i * 9 < vertices.length; i++) {
      _temp1.set(vertices[i * 9 + 0], vertices[i * 9 + 1], vertices[i * 9 + 2]);
      _temp2.set(vertices[i * 9 + 3], vertices[i * 9 + 4], vertices[i * 9 + 5]);
      _temp3.set(vertices[i * 9 + 6], vertices[i * 9 + 7], vertices[i * 9 + 8]);

      _temp2.subtractLocal(_temp1); // A -> B
      _temp3.subtractLocal(_temp1); // A -> C

      _temp2.cross(_temp3, _temp1);
      _temp1.normalizeLocal(); // Normal

      _faceNormals[i * 3 + 0] = _temp1.getX();
      _faceNormals[i * 3 + 1] = _temp1.getY();
      _faceNormals[i * 3 + 2] = _temp1.getZ();
    }
  }

  /**
   * Creates an array containing the interpolated normals for all vertices
   * 
   * @return The array with the vertex normals
   */
  private double[] getVertexNormals() {

    final double[] normals = new double[_faceNormals.length * 3];
    final boolean[] setNormals = new boolean[_faceNormals.length];

    for (int i = 0; i * 3 < _faceNormals.length; i++) {
      for (int j = 0; j < 3; j++) {
        if (!setNormals[i * 3 + j]) {
          setInterpolatedNormal(normals, setNormals, i, j);
        }
      }
    }

    return normals;
  }

  /**
   * Computes the interpolated normal for the specified vertex of the specified face and applies it to
   * all identical vertices for which the normal is interpolated.
   * 
   * @param normals
   *          The array to store the vertex normals
   * @param setNormals
   *          An array indicating which vertex normals have already been set
   * @param face
   *          The index of the face containing the current vertex
   * @param vertex
   *          The index of the vertex inside the face (0 - 2)
   */
  private void setInterpolatedNormal(final double[] normals, final boolean[] setNormals, final int face,
      final int vertex) {

    // temp1: Normal of the face the specified vertex belongs to
    _temp1.set(_faceNormals[face * 3 + 0], _faceNormals[face * 3 + 1], _faceNormals[face * 3 + 2]);

    // temp2: Sum of all face normals to be interpolated
    _temp2.set(_temp1);

    final int vertIndex = _indices[face * 3 + vertex];
    _normalsToSet[0] = face * 3 + vertex;
    int count = 1;

    /*
     * Get the normals of all faces containing the specified vertex whose angle to the specified one is
     * less than the crease angle
     */
    for (int i = face * 3 + vertex + 1; i < _indices.length; i++) {
      if (_indices[i] == vertIndex && !setNormals[face * 3 + vertex]) {
        // temp3: Normal of the face the current vertex belongs to
        _temp3.set(_faceNormals[(i / 3) * 3 + 0], _faceNormals[(i / 3) * 3 + 1], _faceNormals[(i / 3) * 3 + 2]);
        if (_temp1.smallestAngleBetween(_temp3) < _creaseAngle) {
          _normalsToSet = setValue(_normalsToSet, count, i);
          count++;
          _temp2.addLocal(_temp3);
        }
      }
    }

    _temp2.normalizeLocal();

    // Set the normals for all vertices marked for interpolation
    for (int i = 0; i < _normalsToSet.length && _normalsToSet[i] != -1; i++) {
      normals[_normalsToSet[i] * 3 + 0] = _temp2.getX();
      normals[_normalsToSet[i] * 3 + 1] = _temp2.getY();
      normals[_normalsToSet[i] * 3 + 2] = _temp2.getZ();
      setNormals[_normalsToSet[i]] = true;
      _normalsToSet[i] = -1;
    }
  }

  /**
   * Puts the value into the array at the specified index. If the index is out of bounds, an new array
   * with a length of 3 fields more than the specified one is created first and the values copied to
   * it.
   * 
   * @param array
   *          The array
   * @param index
   *          The index to insert the value
   * @param value
   *          The value to insert
   * @return The array with the values, either the specified one or the new one
   */
  private int[] setValue(int[] array, final int index, final int value) {
    if (index >= array.length) {
      final int[] temp = new int[array.length + 3];
      Arrays.fill(temp, -1);
      System.arraycopy(array, 0, temp, 0, array.length);
      array = temp;
    }

    array[index] = value;
    return array;
  }

  /**
   * Simply copies the face normals to the vertices contained in each face, creating a faceted
   * appearance.
   * 
   * @return The vertex normals
   */
  private double[] getFacetedVertexNormals() {
    final double[] normals = new double[_faceNormals.length * 3];
    for (int i = 0; i * 3 < _faceNormals.length; i++) {
      for (int j = 0; j < 3; j++) {
        normals[i * 9 + j + 0] = _faceNormals[i * 3 + j];
        normals[i * 9 + j + 3] = _faceNormals[i * 3 + j];
        normals[i * 9 + j + 6] = _faceNormals[i * 3 + j];
      }
    }
    return normals;
  }

  public void generateNormals(final Mesh mesh) {

  }
}
