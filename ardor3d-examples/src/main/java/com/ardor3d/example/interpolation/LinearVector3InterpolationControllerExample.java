/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.example.interpolation;

import com.ardor3d.example.Purpose;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.scenegraph.Line;
import com.ardor3d.scenegraph.Point;
import com.ardor3d.scenegraph.controller.interpolation.LinearVector3InterpolationController;

/**
 * A demonstration of the LinearVector3InterpolationController class; which will move a Node through
 * a set of 3D coordinates (via linear interpolation).
 */
@Purpose(
    htmlDescriptionKey = "com.ardor3d.example.interpolation.LinearVector3InterpolationControllerExample", //
    thumbnailPath = "com/ardor3d/example/media/thumbnails/interpolation_LinearVector3InterpolationControllerExample.jpg", //
    maxHeapMemory = 64)
public class LinearVector3InterpolationControllerExample
    extends InterpolationControllerBase<LinearVector3InterpolationController> {

  public static void main(final String[] args) {
    start(LinearVector3InterpolationControllerExample.class);
  }

  @Override
  protected LinearVector3InterpolationController createController() {
    // Create our control point vectors
    // final Vector3[] vectors = { new Vector3(0, 0, 0), //
    // new Vector3(30, 0, 0), //
    // new Vector3(90, 0, 0), //
    // new Vector3(210, 0, 0) };

    final ReadOnlyVector3[] vectors = {new Vector3(10, 10, 10), //
        new Vector3(0, 10, 0), //
        new Vector3(-5, 0, -30), //
        new Vector3(-15, 20, -40), //
        new Vector3(0, 0, 20), //
        new Vector3(10, 0, 0), //
        new Vector3(10, 10, 10)};

    // Create a line from our vectors
    final Line line = new Line("line", vectors, null, null, null);
    line.getMeshData().setIndexMode(IndexMode.LineStrip);
    _root.attachChild(line);

    // Create some points from our vectors
    final Point point = new Point("point", vectors, null, null, null);
    point.setPointSize(10f);
    _root.attachChild(point);

    // Create our controller
    final LinearVector3InterpolationController controller = new LinearVector3InterpolationController();
    controller.setControls(vectors);
    controller.setActive(true);
    controller.setSpeed(1.0);

    return controller;
  }
}
