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
 * Describes a class interested in being notified of SkeletonPose updates.
 */
public interface PoseListener {

    /**
     * Call-back method on skeleton pose updates.
     * 
     * @param pose
     *            the pose that was updated.
     */
    void poseUpdated(SkeletonPose pose);

}
