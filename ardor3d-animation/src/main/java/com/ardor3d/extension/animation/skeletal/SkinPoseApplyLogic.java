/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.animation.skeletal;

/**
 * Custom logic for how a skin should react when it is told its pose has updated. This might include throttling skin
 * application, ignoring skin application when the skin is outside of the camera view, etc.
 */
public interface SkinPoseApplyLogic {

    /**
     * Apply, in some way, the given pose to the given mesh.
     * 
     * @param skinnedMesh
     *            the mesh to apply to.
     * @param pose
     *            the pose to apply.
     */
    void doApply(SkinnedMesh skinnedMesh, SkeletonPose pose);

}
