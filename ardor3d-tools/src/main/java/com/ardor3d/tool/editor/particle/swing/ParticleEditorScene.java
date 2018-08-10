/**
 * Copyright (c) 2008-2018 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.tool.editor.particle.swing;

import java.util.prefs.Preferences;

import com.ardor3d.extension.effect.particle.ParticleSystem;
import com.ardor3d.framework.Scene;
import com.ardor3d.framework.Updater;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.intersection.PickResults;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Ray3;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.scenegraph.Line;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.scenegraph.shape.Quad;
import com.ardor3d.util.Constants;
import com.ardor3d.util.ContextGarbageCollector;
import com.ardor3d.util.GameTaskQueue;
import com.ardor3d.util.GameTaskQueueManager;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.stat.StatCollector;
import com.ardor3d.util.stat.StatType;
import com.ardor3d.util.stat.graph.GraphFactory;
import com.ardor3d.util.stat.graph.LineGrapher;
import com.ardor3d.util.stat.graph.TabledLabelGrapher;

class ParticleEditorScene implements Scene, Updater {
    Node particleNode;
    ParticleSystem particleGeom;
    Line grid;

    private final Preferences _prefs;

    /**
     * The root node of our stat graphs.
     */
    final Node statNode = new Node("stats");

    /**
     * The root node of our main scenegraph.
     */
    final Node rootNode = new Node("root");

    private TabledLabelGrapher tgrapher;

    private Quad labGraph;

    private final LogicalLayer _logicalLayer;

    public ParticleEditorScene(final Preferences prefs, final LogicalLayer ll) {
        _prefs = prefs;
        _logicalLayer = ll;
    }

    public void update(final ReadOnlyTimer timer) {
        final double tpf = timer.getTimePerFrame();

        // Execute update queue items
        GameTaskQueueManager.getManager(ParticleEditorFrame.GLOBAL_CONTEXT).getQueue(GameTaskQueue.UPDATE).execute();

        _logicalLayer.checkTriggers(tpf);

        rootNode.updateGeometricState(tpf);

        if (Constants.stats) {
            final Camera cam = Camera.getCurrentCamera();
            if (cam != null) {
                labGraph.setTranslation((cam.getWidth() - .5f * labGraph.getWidth()), (.5f * labGraph.getHeight()), 0);
            }

            statNode.updateGeometricState(tpf);
        }
    }

    @Override
    public boolean renderUnto(final Renderer renderer) {

        if (Constants.stats && labGraph == null) {
            setupStatGraphs(renderer);
            setupStats();
        }

        // Execute renderQueue items
        GameTaskQueueManager.getManager(ParticleEditorFrame.GLOBAL_CONTEXT).getQueue(GameTaskQueue.RENDER)
                .execute(renderer);

        // Clean up card garbage such as textures, vbos, etc.
        ContextGarbageCollector.doRuntimeCleanup(renderer);

        rootNode.draw(renderer);
        if (Constants.stats) {
            StatCollector.update();
            statNode.draw(renderer);
        }

        return true;
    }

    public void init() {
        if (Constants.stats) {
            Constants.updateGraphs = true;
        }
        rootNode.getSceneHints().setCullHint(CullHint.Dynamic);
        statNode.getSceneHints().setCullHint(CullHint.Never);

        grid = createGrid();

        rootNode.attachChild(grid);

        particleNode = new Node("particles");
        rootNode.attachChild(particleNode);

        final ZBufferState zbuf = new ZBufferState();
        zbuf.setWritable(false);
        zbuf.setEnabled(true);
        zbuf.setFunction(ZBufferState.TestFunction.LessThanOrEqualTo);

        particleNode.setRenderState(zbuf);

        statNode.updateGeometricState(0, true);
    };

    /**
     * Set up which stats to graph
     *
     */
    protected void setupStats() {
        tgrapher.addConfig(StatType.STAT_FRAMES, LineGrapher.ConfigKeys.Color.name(), ColorRGBA.GREEN);
        tgrapher.addConfig(StatType.STAT_TRIANGLE_COUNT, LineGrapher.ConfigKeys.Color.name(), ColorRGBA.CYAN);
        tgrapher.addConfig(StatType.STAT_LINE_COUNT, LineGrapher.ConfigKeys.Color.name(), ColorRGBA.RED);
        tgrapher.addConfig(StatType.STAT_POINT_COUNT, LineGrapher.ConfigKeys.Color.name(), ColorRGBA.YELLOW);
        tgrapher.addConfig(StatType.STAT_MESH_COUNT, LineGrapher.ConfigKeys.Color.name(), ColorRGBA.GRAY);
        tgrapher.addConfig(StatType.STAT_TEXTURE_BINDS, LineGrapher.ConfigKeys.Color.name(), ColorRGBA.ORANGE);

        tgrapher.addConfig(StatType.STAT_FRAMES, TabledLabelGrapher.ConfigKeys.Decimals.name(), 0);
        tgrapher.addConfig(StatType.STAT_FRAMES, TabledLabelGrapher.ConfigKeys.Name.name(), "Frames/s:");
        tgrapher.addConfig(StatType.STAT_TRIANGLE_COUNT, TabledLabelGrapher.ConfigKeys.Decimals.name(), 0);
        tgrapher.addConfig(StatType.STAT_TRIANGLE_COUNT, TabledLabelGrapher.ConfigKeys.Name.name(), "Avg.Tris:");
        tgrapher.addConfig(StatType.STAT_TRIANGLE_COUNT, TabledLabelGrapher.ConfigKeys.FrameAverage.name(), true);
        tgrapher.addConfig(StatType.STAT_LINE_COUNT, TabledLabelGrapher.ConfigKeys.Decimals.name(), 0);
        tgrapher.addConfig(StatType.STAT_LINE_COUNT, TabledLabelGrapher.ConfigKeys.Name.name(), "Avg.Lines:");
        tgrapher.addConfig(StatType.STAT_LINE_COUNT, TabledLabelGrapher.ConfigKeys.FrameAverage.name(), true);
        tgrapher.addConfig(StatType.STAT_POINT_COUNT, TabledLabelGrapher.ConfigKeys.Decimals.name(), 0);
        tgrapher.addConfig(StatType.STAT_POINT_COUNT, TabledLabelGrapher.ConfigKeys.Name.name(), "Avg.Points:");
        tgrapher.addConfig(StatType.STAT_POINT_COUNT, TabledLabelGrapher.ConfigKeys.FrameAverage.name(), true);
        tgrapher.addConfig(StatType.STAT_MESH_COUNT, TabledLabelGrapher.ConfigKeys.Decimals.name(), 0);
        tgrapher.addConfig(StatType.STAT_MESH_COUNT, TabledLabelGrapher.ConfigKeys.Name.name(), "Avg.Objs:");
        tgrapher.addConfig(StatType.STAT_MESH_COUNT, TabledLabelGrapher.ConfigKeys.FrameAverage.name(), true);
        tgrapher.addConfig(StatType.STAT_TEXTURE_BINDS, TabledLabelGrapher.ConfigKeys.Decimals.name(), 0);
        tgrapher.addConfig(StatType.STAT_TEXTURE_BINDS, TabledLabelGrapher.ConfigKeys.Name.name(), "Avg.Tex binds:");
        tgrapher.addConfig(StatType.STAT_TEXTURE_BINDS, TabledLabelGrapher.ConfigKeys.FrameAverage.name(), true);
    }

    /**
     * Set up the graphers we will use and the quads we'll show the stats on.
     *
     * @param caps
     *
     */
    protected void setupStatGraphs(final Renderer renderer) {
        StatCollector.setSampleRate(1000L);
        StatCollector.setMaxSamples(30);
        final int width = 800, height = 600;
        labGraph = new Quad("labelGraph", width / 3, height / 3) {
            @Override
            public void draw(final Renderer r) {
                StatCollector.pause();
                super.draw(r);
                StatCollector.resume();
            }
        };
        tgrapher = GraphFactory.makeTabledLabelGraph((width / 3), (height / 3), labGraph, renderer, ContextManager
                .getCurrentContext().getCapabilities());
        tgrapher.setMinimalBackground(true);
        tgrapher.setColumns(1);
        labGraph.setTranslation((width * 5 / 6), (height * 1 / 6), 0);
        statNode.attachChild(labGraph);

    }

    public PickResults doPick(final Ray3 pickRay) {
        // TODO Auto-generated method stub
        return null;
    }

    private static final int GRID_LINES = 51;
    private static final float GRID_SPACING = 100f;

    private Line createGrid() {
        final Vector3[] vertices = new Vector3[GRID_LINES * 2 * 2];
        final float edge = GRID_LINES / 2 * GRID_SPACING;
        for (int ii = 0, idx = 0; ii < GRID_LINES; ii++) {
            final float coord = (ii - GRID_LINES / 2) * GRID_SPACING;
            vertices[idx++] = new Vector3(-edge, 0f, coord);
            vertices[idx++] = new Vector3(+edge, 0f, coord);
            vertices[idx++] = new Vector3(coord, 0f, -edge);
            vertices[idx++] = new Vector3(coord, 0f, +edge);
        }
        final Line grid = new Line("grid", vertices, null, null, null) {
            @Override
            public void draw(final Renderer r) {
                StatCollector.pause();
                super.draw(r);
                StatCollector.resume();
            }
        };
        grid.setDefaultColor(ColorRGBA.DARK_GRAY);
        grid.getSceneHints().setCullHint(_prefs.getBoolean("showgrid", true) ? CullHint.Dynamic : CullHint.Always);
        return grid;
    }

}
