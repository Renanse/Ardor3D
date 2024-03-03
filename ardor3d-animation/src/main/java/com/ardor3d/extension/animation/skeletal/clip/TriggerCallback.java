/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.animation.skeletal.clip;

import com.ardor3d.extension.animation.skeletal.AnimationManager;
import com.ardor3d.extension.animation.skeletal.SkeletonPose;

/**
 * Callback interface for logic to execute when a Trigger from a TriggerChannel is encountered.
 */
public interface TriggerCallback {

  /**
   * Called once per encounter of a TriggerParam. Not guaranteed to be called if, for example, the
   * window defined in the TriggerParam is very small and/or the frame rate is really bad.
   * 
   * @param applyToPose
   * @param manager
   */
  void doTrigger(SkeletonPose applyToPose, AnimationManager manager);

}
