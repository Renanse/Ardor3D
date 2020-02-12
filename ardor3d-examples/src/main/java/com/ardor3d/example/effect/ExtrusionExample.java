/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.example.effect;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.scenegraph.Line;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.scenegraph.shape.Extrusion;
import com.ardor3d.spline.CatmullRomSpline;
import com.ardor3d.spline.Curve;
import com.ardor3d.util.MaterialUtil;
import com.ardor3d.util.geom.BufferUtils;

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

        final List<ReadOnlyVector3> path = new ArrayList<>();
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

        MaterialUtil.autoMaterials(_root);
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
        generateLineNormals(line);

        line.setAntialiased(true);
        final BlendState bs = new BlendState();
        bs.setBlendEnabled(true);
        line.setRenderState(bs);

        return line;
    }

    private Line createLineStrip(final boolean loop) {
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

        // Create a line from the curve
        final Line line = curve.toRenderableLine(10);
        line.getMeshData().setIndexMode(loop ? IndexMode.LineLoop : IndexMode.LineStrip);

        // set normals
        generateLineNormals(line);

        // Make it anti-aliased
        line.setAntialiased(true);
        final BlendState bs = new BlendState();
        bs.setBlendEnabled(true);
        line.setRenderState(bs);

        return line;
    }

    private void generateLineNormals(final Line line) {
        final MeshData data = line.getMeshData();
        final FloatBuffer verts = data.getVertexBuffer();
        final FloatBuffer norms = BufferUtils.createVector3Buffer(data.getNormalBuffer(), data.getVertexCount());
        data.setNormalBuffer(norms);
        final Vector3 prev = new Vector3(), curr = new Vector3(), next = new Vector3();
        final Vector3 dirPrev = new Vector3(), dirNext = new Vector3(), norm = new Vector3();
        for (int i = 0, maxI = data.getVertexCount(); i < maxI; i++) {
            BufferUtils.populateFromBuffer(prev, verts, i > 0 ? i - 1 : i);
            BufferUtils.populateFromBuffer(curr, verts, i);
            BufferUtils.populateFromBuffer(next, verts, i < maxI - 1 ? i + 1 : i);

            dirPrev.set(curr).subtractLocal(prev).normalizeLocal();
            dirNext.set(next).subtractLocal(curr).normalizeLocal();

            norm.set((dirNext.getY() + dirPrev.getY()) / 2.0, (dirNext.getX() + dirPrev.getX()) / -2.0, 0);

            BufferUtils.setInBuffer(norm, norms, i);
        }

    }
}
