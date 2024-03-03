/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.util.geom;

import java.nio.FloatBuffer;

import com.ardor3d.buffer.BufferUtils;
import com.ardor3d.buffer.IndexBufferData;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.scenegraph.MeshData;

public class TangentUtil {
  public static FloatBuffer generateTangentBuffer(final MeshData meshData) {
    return generateTangentBuffer(meshData, 0);
  }

  public static FloatBuffer generateTangentBuffer(final MeshData meshData, final int uvUnit) {
    final FloatBuffer vertexBuffer = meshData.getVertexBuffer();
    if (vertexBuffer == null) {
      throw new IllegalArgumentException("Vertex buffer is null!");
    }

    final FloatBuffer normalBuffer = meshData.getNormalBuffer();
    if (normalBuffer == null) {
      throw new IllegalArgumentException("Normal buffer is null!");
    }

    FloatBuffer textureBuffer = meshData.getTextureBuffer(uvUnit);
    if (textureBuffer == null && uvUnit != 0) {
      textureBuffer = meshData.getTextureBuffer(0);
    }
    if (textureBuffer == null) {
      throw new IllegalArgumentException("Texture buffer is null!");
    }

    final IndexBufferData<?> indexBuffer = meshData.getIndices();
    if (indexBuffer == null) {
      throw new IllegalArgumentException("Index buffer is null!");
    }

    final int vertexCount = meshData.getVertexCount();
    final int triangleCount = meshData.getTotalPrimitiveCount();

    final Vector3[] tan1 = new Vector3[vertexCount];
    final Vector3[] tan2 = new Vector3[vertexCount];
    for (int i = 0; i < vertexCount; i++) {
      tan1[i] = new Vector3();
      tan2[i] = new Vector3();
    }

    final Vector3[] vertex = BufferUtils.getVector3Array(vertexBuffer);
    final Vector3[] normal = BufferUtils.getVector3Array(normalBuffer);
    final Vector2[] texcoord = BufferUtils.getVector2Array(textureBuffer);

    for (int a = 0; a < triangleCount; a++) {
      final int i1 = indexBuffer.get(a * 3);
      final int i2 = indexBuffer.get(a * 3 + 1);
      final int i3 = indexBuffer.get(a * 3 + 2);

      final Vector3 v1 = vertex[i1];
      final Vector3 v2 = vertex[i2];
      final Vector3 v3 = vertex[i3];

      final Vector2 w1 = texcoord[i1];
      final Vector2 w2 = texcoord[i2];
      final Vector2 w3 = texcoord[i3];

      final float x1 = v2.getXf() - v1.getXf();
      final float x2 = v3.getXf() - v1.getXf();
      final float y1 = v2.getYf() - v1.getYf();
      final float y2 = v3.getYf() - v1.getYf();
      final float z1 = v2.getZf() - v1.getZf();
      final float z2 = v3.getZf() - v1.getZf();

      final float s1 = w2.getXf() - w1.getXf();
      final float s2 = w3.getXf() - w1.getXf();
      final float t1 = w2.getYf() - w1.getYf();
      final float t2 = w3.getYf() - w1.getYf();

      final float r = 1.0F / (s1 * t2 - s2 * t1);
      if (Float.isNaN(r) || Float.isInfinite(r)) {
        continue;
      }
      final Vector3 sdir = new Vector3((t2 * x1 - t1 * x2) * r, (t2 * y1 - t1 * y2) * r, (t2 * z1 - t1 * z2) * r);
      final Vector3 tdir = new Vector3((s1 * x2 - s2 * x1) * r, (s1 * y2 - s2 * y1) * r, (s1 * z2 - s2 * z1) * r);

      tan1[i1].addLocal(sdir);
      tan1[i2].addLocal(sdir);
      tan1[i3].addLocal(sdir);

      tan2[i1].addLocal(tdir);
      tan2[i2].addLocal(tdir);
      tan2[i3].addLocal(tdir);
    }

    final FloatBuffer tangentBuffer = BufferUtils.createVector4Buffer(vertexCount);

    final Vector3 calc1 = new Vector3();
    final Vector3 calc2 = new Vector3();
    for (int a = 0; a < vertexCount; a++) {
      final Vector3 n = normal[a];
      final Vector3 t = tan1[a];

      // Gram-Schmidt orthogonalize
      double dot = n.dot(t);
      calc1.set(t).subtractLocal(n.multiply(dot, calc2)).normalizeLocal();
      tangentBuffer.put(calc1.getXf()).put(calc1.getYf()).put(calc1.getZf());

      // Calculate handedness
      dot = calc1.set(n).crossLocal(t).dot(tan2[a]);
      final float w = dot < 0.0f ? -1.0f : 1.0f;
      tangentBuffer.put(w);
    }

    return tangentBuffer;
  }
}
