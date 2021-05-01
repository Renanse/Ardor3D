/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.example.interpolation;

import com.ardor3d.example.Purpose;
import com.ardor3d.math.Quaternion;
import com.ardor3d.scenegraph.controller.interpolation.QuaternionInterpolationController;

/**
 * A demonstration of the QuaternionInterpolationController class; which will rotate a Node each
 * epoch by interpolating between the given quaternions.
 */
@Purpose(
    htmlDescriptionKey = "com.ardor3d.example.interpolation.QuaternionInterpolationControllerExample", //
    thumbnailPath = "com/ardor3d/example/media/thumbnails/interpolation_QuaternionInterpolationControllerExample.jpg", //
    maxHeapMemory = 64)
public class QuaternionInterpolationControllerExample
    extends InterpolationControllerBase<QuaternionInterpolationController> {

  public static void main(final String[] args) {
    start(QuaternionInterpolationControllerExample.class);
  }

  @Override
  protected QuaternionInterpolationController createController() {
    // Create our control point rotations
    final Quaternion[] quats = {new Quaternion(0.0, 0.0, 0.0, 1.0), //
        new Quaternion(1.0, 0.0, 0.0, 1.0), //
        new Quaternion(0.0, 1.0, 0.0, 1.0), //
        new Quaternion(0.0, 0.0, 1.0, 1.0), //
        new Quaternion(1.0, 1.0, 1.0, 1.0)};

    // Create our controller
    final QuaternionInterpolationController controller = new QuaternionInterpolationController();
    controller.setControls(quats);
    controller.setActive(true);

    return controller;
  }
}
