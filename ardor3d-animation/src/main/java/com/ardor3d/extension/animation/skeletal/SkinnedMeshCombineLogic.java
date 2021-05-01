/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.animation.skeletal;

import com.ardor3d.extension.animation.skeletal.util.SkinUtils;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.util.geom.MeshCombiner.MeshCombineLogic;

/**
 * Logic for combining multiple SkinnedMesh objects into one. Should only be used where the pose of
 * each SkinnedMesh is the same.
 */
public class SkinnedMeshCombineLogic extends MeshCombineLogic {
  protected int totalWeightJointEntries = 0, weightsPerVert = 0;
  private final SkinnedMesh _mesh;

  public SkinnedMeshCombineLogic() {
    this("combined");
  }

  public SkinnedMeshCombineLogic(final String name) {
    _mesh = new SkinnedMesh(name);
    _mesh.setBindPoseData(data);
  }

  @Override
  public SkinnedMesh getMesh() {
    // copy and set here so it's the right size.
    _mesh.setMeshData(data.makeCopy());
    _mesh.setWeightsPerVert(weightsPerVert);
    return _mesh;
  }

  @Override
  public void addSource(final Mesh mesh) {
    if (!(mesh instanceof SkinnedMesh)) {
      return;
    }

    final SkinnedMesh skMesh = (SkinnedMesh) mesh;
    if (first) {
      _mesh.setCurrentPose(skMesh.getCurrentPose());
    }

    super.addSource(skMesh);

    weightsPerVert = Math.max(weightsPerVert, skMesh.getWeightsPerVert());
    totalWeightJointEntries += skMesh.getWeights().length / skMesh.getWeightsPerVert();
  }

  @Override
  public void initDataBuffers() {
    super.initDataBuffers();

    // init weight/joint indices
    final int length = totalWeightJointEntries * weightsPerVert;
    _mesh.setWeights(new float[length]);
    _mesh.setJointIndices(new short[length]);
  }

  @Override
  public void combineSources() {
    super.combineSources();

    // combine weights
    int currentIndex = 0;
    for (final Mesh m : sources) {
      final SkinnedMesh skMesh = (SkinnedMesh) m;
      final float[] wData = SkinUtils.pad(skMesh.getWeights(), skMesh.getWeightsPerVert(), weightsPerVert);
      final short[] jData = SkinUtils.pad(skMesh.getJointIndices(), skMesh.getWeightsPerVert(), weightsPerVert);

      System.arraycopy(wData, 0, _mesh.getWeights(), currentIndex, wData.length);
      System.arraycopy(jData, 0, _mesh.getJointIndices(), currentIndex, jData.length);
      currentIndex += wData.length;
    }
  }
}
