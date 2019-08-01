/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.example.interpolation;

import java.util.Arrays;
import java.util.List;

import com.ardor3d.example.Purpose;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.scenegraph.Line;
import com.ardor3d.scenegraph.Point;
import com.ardor3d.scenegraph.controller.interpolation.CurveInterpolationController;
import com.ardor3d.scenegraph.controller.interpolation.Vector3InterpolationController.UpdateField;
import com.ardor3d.spline.CatmullRomSpline;
import com.ardor3d.spline.Curve;

/**
 * A demonstration of the CurveInterpolationController class; which will move/translate a Node each epoch through a set
 * of 3D coordinates (via spline).
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.interpolation.CurveInterpolationControllerExample", //
thumbnailPath = "com/ardor3d/example/media/thumbnails/interpolation_CurveInterpolationControllerExample.jpg", //
maxHeapMemory = 64)
public class CurveInterpolationControllerExample extends InterpolationControllerBase<CurveInterpolationController> {

    public static void main(final String[] args) {
        start(CurveInterpolationControllerExample.class);
    }

    @Override
    protected CurveInterpolationController createController() {
        // Create our control point vectors
        // final ReadOnlyVector3[] vectors = { new Vector3(-15, 0, 0), //
        // new Vector3(0, 0, 0), //
        // new Vector3(30, 0, 0), //
        // new Vector3(90, 0, 0), //
        // new Vector3(210, 0, 0), //
        // new Vector3(390, 0, 0) };

        final ReadOnlyVector3[] vectors = { new Vector3(10, 10, 10), //
                new Vector3(0, 10, 0), //
                new Vector3(-5, 0, -30), //
                new Vector3(-15, 20, -40), //
                new Vector3(0, 0, 20), //
                new Vector3(10, 0, 0), //
                new Vector3(10, 10, 10), //
                new Vector3(0, 10, 0), //
                new Vector3(-5, 0, -30) };

        final List<ReadOnlyVector3> controls = Arrays.asList(vectors);

        // Create our curve from the control points and a spline
        final Curve curve = new Curve(controls, new CatmullRomSpline());

        // Create a line from the curve so its easy to check the box is following it
        final Line line = curve.toRenderableLine(10);
        line.setRandomColors();

        _root.attachChild(line);

        // Create points from the curve so the actual control points can be easily seen
        final Point point = curve.toRenderablePoint(2);
        point.setPointSize(10f);

        _root.attachChild(point);

        // Create our controller
        final CurveInterpolationController controller = new CurveInterpolationController();
        controller.setCurve(curve);
        controller.setActive(true);
        controller.setUpdateField(UpdateField.LOCAL_TRANSLATION);
        controller.setSpeed(10.0);
        controller.generateArcLengths(10, true); // we must pass true as can switch to cycle repeat type at runtime
        controller.setConstantSpeed(true);

        return controller;
    }
}
