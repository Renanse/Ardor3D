/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example.renderer;

import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.framework.Canvas;
import com.ardor3d.image.Texture;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.input.Key;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyPressedCondition;
import com.ardor3d.input.logical.TriggerAction;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.MaterialState;
import com.ardor3d.renderer.state.MaterialState.ColorMaterial;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.hint.DataMode;
import com.ardor3d.scenegraph.shape.Arrow;
import com.ardor3d.scenegraph.shape.AxisRods;
import com.ardor3d.scenegraph.shape.Box;
import com.ardor3d.scenegraph.shape.Capsule;
import com.ardor3d.scenegraph.shape.Cone;
import com.ardor3d.scenegraph.shape.Cylinder;
import com.ardor3d.scenegraph.shape.Disk;
import com.ardor3d.scenegraph.shape.Dodecahedron;
import com.ardor3d.scenegraph.shape.Dome;
import com.ardor3d.scenegraph.shape.GeoSphere;
import com.ardor3d.scenegraph.shape.GeoSphere.TextureMode;
import com.ardor3d.scenegraph.shape.Hexagon;
import com.ardor3d.scenegraph.shape.Icosahedron;
import com.ardor3d.scenegraph.shape.MultiFaceBox;
import com.ardor3d.scenegraph.shape.Octahedron;
import com.ardor3d.scenegraph.shape.PQTorus;
import com.ardor3d.scenegraph.shape.Pyramid;
import com.ardor3d.scenegraph.shape.Quad;
import com.ardor3d.scenegraph.shape.RoundedBox;
import com.ardor3d.scenegraph.shape.Sphere;
import com.ardor3d.scenegraph.shape.StripBox;
import com.ardor3d.scenegraph.shape.Teapot;
import com.ardor3d.scenegraph.shape.Torus;
import com.ardor3d.scenegraph.shape.Tube;
import com.ardor3d.ui.text.BasicText;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.scenegraph.CompileOptions;
import com.ardor3d.util.scenegraph.RenderDelegate;
import com.ardor3d.util.scenegraph.SceneCompiler;

/**
 * Illustrates creating a display list of intrinsic shapes (e.g. Box, Cone, Torus).
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.renderer.DisplayListExample", //
thumbnailPath = "com/ardor3d/example/media/thumbnails/renderer_DisplayListExample.jpg", //
maxHeapMemory = 64)
public class DisplayListExample extends ExampleBase {
    private int wrapCount;
    private int index;
    private BasicText _text;
    private final Node _shapeRoot = new Node("shapeRoot");
    private RenderDelegate _delegate;
    private boolean first = true;
    private double counter = 0;
    private int frames = 0;

    public static void main(final String[] args) {
        start(DisplayListExample.class);
    }

    @Override
    protected void renderExample(final Renderer renderer) {
        if (first) {
            final CompileOptions options = new CompileOptions();
            options.setDisplayList(true);
            SceneCompiler.compile(_shapeRoot, _canvas.getCanvasRenderer().getRenderer(), options);
            first = false;
            _delegate = _shapeRoot.getRenderDelegate(ContextManager.getCurrentContext().getGlContextRep());
        }

        super.renderExample(renderer);
    }

    @Override
    protected void updateExample(final ReadOnlyTimer timer) {
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
        _canvas.setTitle("Display List Example");

        _root.attachChild(_shapeRoot);
        _shapeRoot.getSceneHints().setDataMode(DataMode.Arrays);

        wrapCount = 5;
        addMesh(new Arrow("Arrow", 3, 1));
        addMesh(new AxisRods("AxisRods", true, 3, 0.5));
        addMesh(new Box("Box", new Vector3(), 3, 3, 3));
        addMesh(new Capsule("Capsule", 5, 5, 5, 2, 5));
        addMesh(new Cone("Cone", 8, 8, 2, 4));
        addMesh(new Cylinder("Cylinder", 8, 8, 2, 4));
        addMesh(new Disk("Disk", 8, 8, 3));
        addMesh(new Dodecahedron("Dodecahedron", 3));
        addMesh(new Dome("Dome", 8, 8, 3));
        addMesh(new Hexagon("Hexagon", 3));
        addMesh(new Icosahedron("Icosahedron", 3));
        addMesh(new MultiFaceBox("MultiFaceBox", new Vector3(), 3, 3, 3));
        addMesh(new Octahedron("Octahedron", 3));
        addMesh(new PQTorus("PQTorus", 5, 4, 1.5, .5, 128, 8));
        addMesh(new Pyramid("Pyramid", 2, 4));
        addMesh(new Quad("Quad", 3, 3));
        addMesh(new RoundedBox("RoundedBox", new Vector3(3, 3, 3)));
        addMesh(new Sphere("Sphere", 16, 16, 3));
        addMesh(new GeoSphere("GeoSphere", true, 3, 3, TextureMode.Original));
        addMesh(new StripBox("StripBox", new Vector3(), 3, 3, 3));
        addMesh(new Teapot("Teapot"));
        addMesh(new Torus("Torus", 16, 8, 1.0, 2.5));
        addMesh(new Tube("Tube", 2, 3, 4));

        final TextureState ts = new TextureState();
        ts.setTexture(TextureManager.load("images/ardor3d_white_256.jpg", Texture.MinificationFilter.Trilinear,
                TextureStoreFormat.GuessCompressedFormat, true));
        _shapeRoot.setRenderState(ts);

        final BlendState bs = new BlendState();
        bs.setBlendEnabled(true);
        _shapeRoot.setRenderState(bs);

        final MaterialState ms = new MaterialState();
        ms.setColorMaterial(ColorMaterial.Diffuse);
        _shapeRoot.setRenderState(ms);

        // Set up our label
        _text = BasicText.createDefaultTextLabel("label", "[SPACE] display list on");
        _text.setTranslation(10, 10, 0);
        _root.attachChild(_text);
    }

    @Override
    protected void registerInputTriggers() {
        super.registerInputTriggers();
        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.SPACE), new TriggerAction() {
            private boolean useDL = true;

            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                useDL = !useDL;
                if (useDL) {
                    _text.setText("[SPACE] display list on");
                    _shapeRoot.setRenderDelegate(_delegate, ContextManager.getCurrentContext().getGlContextRep());
                } else {
                    _text.setText("[SPACE] display list off");
                    _shapeRoot.setRenderDelegate(null, ContextManager.getCurrentContext().getGlContextRep());
                }
            }
        }));
    }

    private void addMesh(final Spatial spatial) {
        spatial.setTranslation((index % wrapCount) * 8 - wrapCount * 4, (index / wrapCount) * 8 - wrapCount * 4, -50);
        if (spatial instanceof Mesh) {
            ((Mesh) spatial).updateModelBound();
        }
        _shapeRoot.attachChild(spatial);
        index++;
    }
}
