/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example.basic;

import java.nio.FloatBuffer;

import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.framework.Canvas;
import com.ardor3d.image.Texture;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.MouseMovedCondition;
import com.ardor3d.input.logical.TriggerAction;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.intersection.BoundingPickResults;
import com.ardor3d.intersection.PickData;
import com.ardor3d.intersection.PickResults;
import com.ardor3d.intersection.PickingUtil;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Ray3;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.MaterialState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.scenegraph.Line;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.controller.SpatialController;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.scenegraph.hint.LightCombineMode;
import com.ardor3d.scenegraph.hint.TextureCombineMode;
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
import com.ardor3d.scenegraph.shape.GeoSphere.TextureMode;
import com.ardor3d.ui.text.BasicText;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.geom.BufferUtils;

/**
 * A display of intrinsic shapes (e.g. Box, Cone, Torus).
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.basic.ShapesExample", //
thumbnailPath = "com/ardor3d/example/media/thumbnails/basic_ShapesExample.jpg", //
maxHeapMemory = 64)
public class ShapesExample extends ExampleBase {
    private int wrapCount;
    private int index;
    private BasicText _text;
    private PickResults _pickResults;
    private Spatial _picked = null;
    private SpatialController<Spatial> _pickedControl;

    public static void main(final String[] args) {
        start(ShapesExample.class);
    }

    @Override
    protected void initExample() {
        _canvas.setTitle("Shapes Example");

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
        addMesh(createLines());

        final TextureState ts = new TextureState();
        ts.setTexture(TextureManager.load("images/ardor3d_white_256.jpg", Texture.MinificationFilter.Trilinear,
                TextureStoreFormat.GuessCompressedFormat, true));
        _root.setRenderState(ts);

        final BlendState bs = new BlendState();
        bs.setBlendEnabled(true);
        _root.setRenderState(bs);

        // Set up a reusable pick results
        _pickResults = new BoundingPickResults();
        _pickResults.setCheckDistance(true);

        // Set up our pick label
        _text = BasicText.createDefaultTextLabel("", "pick");
        _text.setTranslation(10, 10, 0);
        _text.getSceneHints().setCullHint(CullHint.Always);
        _root.attachChild(_text);

        // Set up picked pulse
        _pickedControl = new SpatialController<Spatial>() {
            ColorRGBA curr = new ColorRGBA();
            float val = 0;
            boolean add = true;

            @Override
            public void update(final double time, final Spatial caller) {
                val += time * (add ? 1 : -1);
                if (val < 0) {
                    val = -val;
                    add = true;
                } else if (val > 1) {
                    val = 1 - (val - (int) val);
                    add = false;
                }

                curr.set(val, val, val, 1.0f);

                final MaterialState ms = (MaterialState) caller.getLocalRenderState(StateType.Material);
                ms.setAmbient(curr);
            }
        };
    }

    @Override
    protected void registerInputTriggers() {
        super.registerInputTriggers();

        // Add mouse-over to show labels

        _logicalLayer.registerTrigger(new InputTrigger(new MouseMovedCondition(), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                // Put together a pick ray
                final Vector2 pos = Vector2.fetchTempInstance().set(inputStates.getCurrent().getMouseState().getX(),
                        inputStates.getCurrent().getMouseState().getY());
                final Ray3 pickRay = Ray3.fetchTempInstance();
                _canvas.getCanvasRenderer().getCamera().getPickRay(pos, false, pickRay);
                Vector2.releaseTempInstance(pos);

                // Do the pick
                _pickResults.clear();
                PickingUtil.findPick(_root, pickRay, _pickResults);
                Ray3.releaseTempInstance(pickRay);

                if (_pickResults.getNumber() > 0) {
                    // picked something, show label.
                    _text.getSceneHints().setCullHint(CullHint.Never);

                    // set our text to the name of the ancestor of this object that is right under the _root node.
                    final PickData pick = _pickResults.getPickData(0);
                    if (pick.getTarget() instanceof Spatial) {
                        final Spatial topLevel = getTopLevel((Spatial) pick.getTarget());
                        if (!topLevel.equals(_picked)) {
                            clearPicked();
                            _picked = topLevel;
                            _picked.addController(_pickedControl);
                        }
                        _text.setText(topLevel.getName());
                    }
                } else {
                    // No pick, clear label.
                    _text.getSceneHints().setCullHint(CullHint.Always);
                    _text.setText("");

                    clearPicked();
                }
            }

            private void clearPicked() {
                if (_picked != null) {
                    final MaterialState ms = (MaterialState) _picked.getLocalRenderState(StateType.Material);
                    ms.setAmbient(ColorRGBA.DARK_GRAY);
                    _picked.removeController(_pickedControl);
                }
                _picked = null;
            }

            private Spatial getTopLevel(final Spatial target) {
                if (target.getParent() == null || target.getParent().equals(_root)) {
                    return target;
                } else {
                    return getTopLevel(target.getParent());
                }
            }
        }));

    }

    private Spatial createLines() {
        final FloatBuffer verts = BufferUtils.createVector3Buffer(3);
        verts.put(0).put(0).put(0);
        verts.put(5).put(5).put(0);
        verts.put(0).put(5).put(0);
        final Line line = new Line("Lines", verts, null, null, null);
        // since we do not set texture coords, but we'll have a texture state applied at root, we need to turn off
        // textures on this Line to prevent bleeding of texture coordinates from other shapes.
        line.getSceneHints().setTextureCombineMode(TextureCombineMode.Off);
        line.getMeshData().setIndexMode(IndexMode.LineStrip);
        line.setLineWidth(2);
        line.getSceneHints().setLightCombineMode(LightCombineMode.Off);

        return line;
    }

    private void addMesh(final Spatial spatial) {
        spatial.setTranslation((index % wrapCount) * 8 - wrapCount * 4, (index / wrapCount) * 8 - wrapCount * 4, -50);
        if (spatial instanceof Mesh) {
            ((Mesh) spatial).updateModelBound();
        }
        final MaterialState ms = new MaterialState();
        ms.setAmbient(ColorRGBA.DARK_GRAY);
        spatial.setRenderState(ms);
        _root.attachChild(spatial);
        index++;
    }
}
