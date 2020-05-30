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

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import com.ardor3d.math.Matrix4;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.util.geom.BufferUtils;

public class InstancingManager {

  private int _maxBatchSize = 30;
  private final List<Mesh> _visibleMeshes = new ArrayList<>(_maxBatchSize);
  private FloatBuffer _transformBuffer;
  private int _primCount;
  private int _meshesToDraw = 0;

  /**
   * Register a mesh for instancing for this current frame (internal use only)
   *
   * @param mesh
   */
  public void registerMesh(final Mesh mesh) {
    _visibleMeshes.add(mesh);
    _meshesToDraw++;
  }

  /**
   * Fill the buffer with the transforms and return it
   */
  protected FloatBuffer fillTransformBuffer() {

    _primCount = Math.min(_visibleMeshes.size(), _maxBatchSize);

    final int nrOfFloats = _primCount * 16; /* 16 floats per matrix */

    // re-init buffer when it is too small of more than twice the required size
    if (_transformBuffer == null || nrOfFloats > _transformBuffer.capacity()) {
      _transformBuffer = BufferUtils.createFloatBuffer(nrOfFloats);
    }

    _transformBuffer.rewind();
    _transformBuffer.limit(nrOfFloats);

    final Matrix4 mat = Matrix4.fetchTempInstance();

    for (int i = 0; i < _maxBatchSize && _meshesToDraw > 0; i++) {
      final Mesh mesh = _visibleMeshes.get(--_meshesToDraw);
      final Matrix4 transform = mesh.getWorldTransform().getHomogeneousMatrix(mat);
      transform.toFloatBuffer(_transformBuffer, false);
    }

    Matrix4.releaseTempInstance(mat);
    _transformBuffer.rewind();

    return _transformBuffer;
  }

  /**
   * Returns the number of meshes to be drawn this batch. This function is only valid after the apply
   * call (internal use only)
   */
  public int getPrimitiveCount() { return _primCount; }

  /**
   * Split the batch in multiple batches if number of visible meshes exceeds this amount. Using larger
   * batches will lead to better performance, although you might overflow the uniform space of the
   * shader/videocard (crashes)
   *
   * @return maximum batch size
   */
  public int getMaxBatchSize() { return _maxBatchSize; }

  /**
   * Split the batch in multiple batches if number of visible meshes exceeds this amount. Using larger
   * batches will lead to better performance, although you might overflow the uniform space of the
   * shader/videocard (crashes)
   *
   * @param maxBatchSize
   *          maximum batch size
   */
  public void setMaxBatchSize(final int maxBatchSize) { _maxBatchSize = maxBatchSize; }

  public boolean isAddedToRenderQueue() { return _meshesToDraw > 0; }

  /**
   * Applies all instancing info to the mesh and returns if the current render call is allowed to
   * continue
   *
   * @param mesh
   * @param renderer
   * @return continue rendering or skip rendering all together
   */
  public boolean apply(final Mesh mesh, final Renderer renderer) {
    // TODO: Reimplement using new Material system
    return false;
    // if (_meshesToDraw <= 0) {
    // // reset for next draw call
    // _primCount = -1;
    // shader.setUniform("nrOfInstances", -1);
    // _visibleMeshes.clear();
    // return false;
    // }
    //
    // shader.setUniform("transforms", fillTransformBuffer(), 4);
    // shader.setUniform("nrOfInstances", getPrimitiveCount());
    // return true;
  }

}
