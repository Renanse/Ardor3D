/**
 * Copyright (c) 2008-2018 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example.effect;

import java.util.Arrays;
import java.util.List;

import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.scenegraph.Line;
import com.ardor3d.scenegraph.shape.Extrusion;
import com.ardor3d.spline.CatmullRomSpline;
import com.ardor3d.spline.Curve;
import com.google.common.collect.Lists;

/**
 * A demonstration of the Extrusion class - showing how a set of point can be converted into a 3d shape.
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.effect.ExtrusionExample", //
        thumbnailPath = "com/ardor3d/example/media/thumbnails/effect_QuadImposterExample.jpg", //
        maxHeapMemory = 64)
public class ExtrusionExample extends ExampleBase {
    public static void main(final String[] args) {
        start(ExtrusionExample.class);
    }

    @Override
    protected void initExample() {
        _canvas.setTitle("Extrusion - Example");

        _canvas.getCanvasRenderer().getCamera().setLocation(new Vector3(0, 0, 80));
        _canvas.getCanvasRenderer().getCamera().lookAt(new Vector3(), Vector3.UNIT_Y);

        final List<ReadOnlyVector3> path = Lists.newArrayList();
        path.add(new Vector3(0, 0, 0));
        path.add(new Vector3(0, 0, 4));
        path.add(new Vector3(1, 0, 8));
        path.add(new Vector3(2, 1, 12));
        path.add(new Vector3(2, 2, 16));
        path.add(new Vector3(2, 3, 20));
        path.add(new Vector3(3, 4, 24));

        // Linestrip
        final Line lineStrip = createLineStrip(false);
        lineStrip.setTranslation(-10, -20, 0);
        _root.attachChild(lineStrip);
        final Extrusion extrusion = new Extrusion("Extrusion", lineStrip, path, Vector3.UNIT_Y);
        extrusion.setTranslation(10, -20, 0);
        _root.attachChild(extrusion);

        // Lines
        final Line lines = createLines();
        lines.setTranslation(-10, 0, 0);
        _root.attachChild(lines);
        final Extrusion extrusion2 = new Extrusion("Extrusion", lines, path, Vector3.UNIT_Y);
        extrusion2.setTranslation(10, 0, 0);
        _root.attachChild(extrusion2);

        // LineLoop
        final Line lineLoop = createLineStrip(true);
        lineLoop.setTranslation(-10, 20, 0);
        _root.attachChild(lineLoop);
        final Extrusion extrusion3 = new Extrusion("Extrusion", lineLoop, path, Vector3.UNIT_Y);
        extrusion3.setTranslation(10, 20, 0);
        _root.attachChild(extrusion3);

        _root.setRenderMaterial("unlit/untextured/basic.yaml");
    }

    private Line createLines() {
        final ReadOnlyVector3[] vectors = { //
                new Vector3(0, 0, 0), //
                new Vector3(5, 0, 0), //
                new Vector3(5, 0, 0), //
                new Vector3(5, 5, 0), //
                new Vector3(5, 5, 0), //
                new Vector3(-5, 5, 0), //
                new Vector3(-5, 5, 0), //
                new Vector3(-5, -5, 0), //
        };

        final Line line = new Line("curve", vectors, null, null, null);

        line.getMeshData().setIndexMode(IndexMode.Lines);

        return line;
    }

    private Line createLineStrip(final boolean loop) {
        // Create a line with our example "makeLine" method. See method below.
        final ReadOnlyVector3[] vectors = { //
                new Vector3(0, 0, 0), //
                new Vector3(5, 0, 0), //
                new Vector3(5, 5, 0), //
                new Vector3(-5, 5, 0), //
                new Vector3(-5, -5, 0), //
        };

        final List<ReadOnlyVector3> controls = Arrays.asList(vectors);

        // Create our curve from the control points and a spline
        final Curve curve = new Curve(controls, new CatmullRomSpline());

        // Create a line from the curve so its easy to check the box is following it
        final Line line = curve.toRenderableLine(10);

        if (loop) {
            line.getMeshData().setIndexMode(IndexMode.LineLoop);
        }

        return line;
    }
}
