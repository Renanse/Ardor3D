/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.example.collision;

import java.util.Random;

import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.image.Texture;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.intersection.PickingUtil;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.state.CullState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.hint.LightCombineMode;
import com.ardor3d.scenegraph.shape.Sphere;
import com.ardor3d.surface.ColorSurface;
import com.ardor3d.ui.text.BasicText;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureManager;

/**
 * A demonstration on how to determine if collisions exist between two nodes.
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.collision.ManyCollisionsExample", //
        thumbnailPath = "com/ardor3d/example/media/thumbnails/collision_ManyCollisionsExample.jpg", //
        maxHeapMemory = 64)
public class ManyCollisionsExample extends ExampleBase {

    private final Matrix3 rotation = new Matrix3();

    private BasicText t;

    private Node n1;
    private Node n2;

    /** Console fps output */
    private double counter = 0;
    private int frames = 0;

    public static void main(final String[] args) {
        start(ManyCollisionsExample.class);
    }

    @Override
    protected void updateExample(final ReadOnlyTimer timer) {
        final boolean hasCollision = PickingUtil.hasCollision(n1, n2, false);
        if (hasCollision) {
            t.setText("Collision!");
        } else {
            t.setText("No Collision!");
        }

        final double time = timer.getTimeInSeconds() * 0.2;

        n1.setRotation(rotation.fromAngles(time, time, time));
        n1.setTranslation(Math.sin(time) * 20.0, 0, -200);

        n2.setRotation(rotation.fromAngles(-time, -time, -time));
        n2.setTranslation(Math.cos(time * 0.7) * 20.0, 0, -200);

        counter += timer.getTimePerFrame();
        frames++;
        if (counter > 1) {
            final double fps = (frames / counter);
            counter = 0;
            frames = 0;
            System.out.printf("%7.1f FPS\n", fps);
        }
    }

    @Override
    protected void initExample() {
        _canvas.setTitle("ManyCollisionsExample");

        t = BasicText.createDefaultTextLabel("Text", "");
        t.getSceneHints().setLightCombineMode(LightCombineMode.Off);
        t.setTranslation(new Vector3(0, 20, 0));
        _orthoRoot.attachChild(t);

        final CullState cs = new CullState();
        cs.setCullFace(CullState.Face.Back);
        cs.setEnabled(true);
        _root.setRenderState(cs);

        final TextureState ts = new TextureState();
        ts.setEnabled(true);
        ts.setTexture(TextureManager.load("images/ardor3d_white_256.jpg", Texture.MinificationFilter.Trilinear,
                TextureStoreFormat.GuessCompressedFormat, true));

        n1 = new Node("n1");
        n1.setTranslation(new Vector3(0, 0, -200));
        _root.attachChild(n1);

        final Random rand = new Random(1337);

        final Sphere sphere = new Sphere("Sphere", 8, 8, 1);
        sphere.updateModelBound();

        for (int i = 0; i < 200; i++) {
            final Mesh sm = sphere.makeCopy(true);

            sm.setTranslation(new Vector3(rand.nextDouble() * 100.0 - 50.0, rand.nextDouble() * 100.0 - 50.0,
                    rand.nextDouble() * 100.0 - 50.0));
            sm.setRenderState(ts);
            n1.attachChild(sm);
        }

        n2 = new Node("n2");
        n2.setTranslation(new Vector3(0, 0, -200));

        final ColorSurface surface = new ColorSurface();
        surface.setDiffuse(ColorRGBA.RED);
        n2.setProperty(ColorSurface.DefaultPropertyKey, surface);

        _root.attachChild(n2);
        _root.setRenderMaterial("lit/textured/basic_phong.yaml");

        for (int i = 0; i < 200; i++) {
            final Mesh sm = sphere.makeCopy(true);

            sm.setTranslation(new Vector3(rand.nextDouble() * 100.0 - 50.0, rand.nextDouble() * 100.0 - 50.0,
                    rand.nextDouble() * 100.0 - 50.0));
            sm.setRenderState(ts);
            n2.attachChild(sm);
        }

    }
}
