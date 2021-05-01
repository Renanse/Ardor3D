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
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.scenegraph.controller.interpolation.DefaultColorInterpolationController;

/**
 * A demonstration of the DefaultColorInterpolationController class; which updates the default color
 * each epoch by interpolating between the given colors.
 */
@Purpose(
    htmlDescriptionKey = "com.ardor3d.example.interpolation.DefaultColorInterpolationControllerExample", //
    thumbnailPath = "com/ardor3d/example/media/thumbnails/interpolation_DefaultColorInterpolationControllerExample.jpg", //
    maxHeapMemory = 64)
public class DefaultColorInterpolationControllerExample
    extends InterpolationControllerBase<DefaultColorInterpolationController> {

  public static void main(final String[] args) {
    start(DefaultColorInterpolationControllerExample.class);
  }

  @Override
  protected DefaultColorInterpolationController createController() {
    // Create our control point colors
    final ReadOnlyColorRGBA[] colors = {ColorRGBA.WHITE, ColorRGBA.RED, ColorRGBA.GREEN, ColorRGBA.BLUE};

    // Create our controller
    final DefaultColorInterpolationController controller = new DefaultColorInterpolationController();
    controller.setControls(colors);
    controller.setActive(true);
    controller.setSpeed(0.5);

    return controller;
  }
}
