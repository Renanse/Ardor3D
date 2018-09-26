/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example.renderer;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.framework.Canvas;
import com.ardor3d.image.Texture;
import com.ardor3d.input.Key;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyPressedCondition;
import com.ardor3d.input.logical.TriggerAction;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.scenegraph.hint.LightCombineMode;
import com.ardor3d.scenegraph.shape.StripBox;
import com.ardor3d.ui.text.BasicText;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.geom.MeshCombiner;
import com.ardor3d.util.stat.MultiStatSample;
import com.ardor3d.util.stat.StatCollector;
import com.ardor3d.util.stat.StatType;

/**
 * An example showing use of MeshCombiner to weld together a bunch of boxes into a single mesh.
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.renderer.CombinerExample", //
        thumbnailPath = "com/ardor3d/example/media/thumbnails/renderer_CombinerExample.jpg", //
        maxHeapMemory = 64)
public class CombinerExample extends ExampleBase {

    private final Node origNode = new Node("orig");
    private final BasicText[] text = new BasicText[3];
    private final int edge = 50;

    private boolean showMerged = true;

    public static void main(final String[] args) {
        // turn on stats so we can use them to calc tris/sec
        System.setProperty("ardor3d.stats", "true");
        start(CombinerExample.class);
    }

    @Override
    protected void initExample() {
        // make a scene node to hold our meshes.
        final Node scene = new Node();
        scene.getSceneHints().setCullHint(CullHint.Dynamic);
        _root.attachChild(scene);
        _root.setRenderMaterial("unlit/textured/vertex_color.yaml");

        // Generate many boxes and place them in a 2D grid pattern, under the origNode.
        Mesh mesh;
        for (int i = 0, max = edge * edge; i < max; i++) {
            mesh = new StripBox("stripbox" + i, new Vector3(), .5, .5, .5);
            mesh.setTranslation(new Vector3(i % edge, i / edge, 0));

            mesh.setModelBound(new BoundingBox());
            mesh.setSolidColor(ColorRGBA.randomColor(null));
            origNode.attachChild(mesh);
        }

        // Create a single Mesh from the origNode and its children.
        final Mesh merged = MeshCombiner.combine(origNode);
        // attach to scene.. default will be to show the merged version first
        scene.attachChild(merged);

        // and a texture, this will cover both the uncombined and combined meshes.
        final TextureState ts = new TextureState();
        ts.setTexture(TextureManager.load("images/ardor3d_white_256.jpg", Texture.MinificationFilter.Trilinear, true));
        scene.setRenderState(ts);

        // position our camera to take in all of the mesh
        _canvas.getCanvasRenderer().getCamera().setLocation(edge / 2, edge / 2, 2 * edge);

        // |---------------- UI and control code below... ----------------|
        // Add a trigger on the M key to switch between merged and non-merged
        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.M), new TriggerAction() {
            @Override
            public void perform(final Canvas source, final TwoInputStates inputState, final double tpf) {
                showMerged = !showMerged;
                if (showMerged) {
                    origNode.removeFromParent();
                    scene.attachChild(merged);
                } else {
                    merged.removeFromParent();
                    scene.attachChild(origNode);
                }
                updateMergedLabel();
            }
        }));

        // make some text labels...
        for (int i = 0; i < text.length; i++) {
            text[i] = BasicText.createDefaultTextLabel("Text" + i, "", 16);
            text[i].getSceneHints().setLightCombineMode(LightCombineMode.Off);
            text[i].setTranslation(new Vector3(10, 10 + i * 25, 0));
            _orthoRoot.attachChild(text[i]);
        }

        // Update text on the labels
        updateMergedLabel();

        // Set root to always show, so our text attached to it doesn't disappear when the scene is out of view.
        // We created scene above so the meshes would not inherit this hint.
        _root.getSceneHints().setCullHint(CullHint.Never);
    }

    private void updateMergedLabel() {
        if (showMerged) {
            text[2].setText("Showing a single, merged Mesh.  [M] to un-merge.");
        } else {
            text[2].setText("Showing " + (edge * edge) + " individual Meshes.  [M] to merge.");
        }
    }

    private double counter = 0;
    private int frames = 0;

    @Override
    protected void updateExample(final ReadOnlyTimer timer) {
        counter += timer.getTimePerFrame();
        frames++;
        if (counter > 1) {
            final double fps = (frames / counter);
            text[1].setText("Frames per second: " + Math.round(fps * 10) / 10.0);
            if (StatCollector.hasHistoricalStat(StatType.STAT_TRIANGLE_COUNT)
                    && StatCollector.hasHistoricalStat(StatType.STAT_FRAMES)) {
                final MultiStatSample stats = StatCollector.lastStats();
                final double triangles = stats.getStatValue(StatType.STAT_TRIANGLE_COUNT).getAccumulatedValue();
                final double frames = stats.getStatValue(StatType.STAT_FRAMES).getAccumulatedValue();
                final double tps = (triangles / frames) * fps;

                if (tps > 1000000) {
                    text[0].setText("Triangles per second: " + Math.round(tps / 100000) / 10.0 + " million");
                } else if (tps > 1000) {
                    text[0].setText("Triangles per second: " + Math.round(tps / 100) / 10.0 + " thousand");
                } else {
                    text[0].setText("Triangles per second: " + Math.round(tps));
                }
            }
            counter = 0;
            frames = 0;

        }
    }
}
