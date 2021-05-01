/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.example.basic;

import java.nio.FloatBuffer;

import com.ardor3d.buffer.BufferUtils;
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
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.Line;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.controller.SpatialController;
import com.ardor3d.scenegraph.hint.CullHint;
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
import com.ardor3d.ui.text.BMTextBackground;
import com.ardor3d.ui.text.BasicText;
import com.ardor3d.util.MaterialUtil;
import com.ardor3d.util.TextureManager;

/**
 * A display of intrinsic shapes (e.g. Box, Cone, Torus).
 */
@Purpose(
    htmlDescriptionKey = "com.ardor3d.example.basic.ShapesExample", //
    thumbnailPath = "com/ardor3d/example/media/thumbnails/basic_ShapesExample.jpg", //
    maxHeapMemory = 64)
public class ShapesExample extends ExampleBase {
  private int wrapCount;
  private int index;
  private BasicText _text;
  private Node _textNode;
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
    final AxisRods rods = new AxisRods("AxisRods", true, 3, 0.5);
    rods.setRenderMaterial("unlit/untextured/basic.yaml");
    addMesh(rods);
    addMesh(new Box("Box", new Vector3(), 3, 3, 3));
    addMesh(new Capsule("Capsule", 5, 5, 5, 2, 5));
    addMesh(new Cone("Cone", 8, 8, 2, 4));
    addMesh(new Cylinder("Cylinder", 8, 8, 2, 4));
    addMesh(new Disk("Disk", 2, 36, 3, 0.5));
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

    // Our shapes material
    _root.setRenderMaterial("lit/textured/basic_phong.yaml");

    // Set up a reusable pick results
    _pickResults = new BoundingPickResults();
    _pickResults.setCheckDistance(true);

    setupText();

    // Set up picked pulse
    _pickedControl = new SpatialController<>() {
      ColorRGBA curr = new ColorRGBA();
      double t = 0;

      @Override
      public void update(final double time, final Spatial caller) {
        t += time;

        final float val = (float) Math.sin(t * 2) * .25f + .75f;

        curr.set(val, val, val, 1.0f);

        if (caller instanceof Mesh) {
          ((Mesh) caller).setDefaultColor(curr);
        }
      }
    };
  }

  private void setupText() {
    // Set up our pick label
    _textNode = new Node("textNode");
    _textNode.setTranslation(20, 20, 0);
    _textNode.getSceneHints().setCullHint(CullHint.Always);
    _orthoRoot.attachChild(_textNode);

    _text = BasicText.createDefaultTextLabel("", "pick");
    _text.getSceneHints().setOrthoOrder(0);
    _textNode.attachChild(_text);

    final Texture border = TextureManager.load("images/border.png", Texture.MinificationFilter.Trilinear, true);

    final BMTextBackground outerBorder = new BMTextBackground("bg1", _text, border);
    outerBorder.setTexBorderOffsets(0.2f);
    outerBorder.setContentPadding(10);
    outerBorder.getSceneHints().setRenderBucketType(RenderBucketType.OrthoOrder);
    outerBorder.getSceneHints().setOrthoOrder(2);
    outerBorder.setBackgroundColor(ColorRGBA.LIGHT_GRAY);
    _textNode.attachChild(outerBorder);

    final BMTextBackground innerBG = new BMTextBackground("bg2", _text, border);
    innerBG.setTexBorderOffsets(0.2f);
    innerBG.getSceneHints().setRenderBucketType(RenderBucketType.OrthoOrder);
    innerBG.getSceneHints().setOrthoOrder(1);
    innerBG.setBackgroundColor(ColorRGBA.BLUE);
    _textNode.attachChild(innerBG);
  }

  @Override
  protected void registerInputTriggers() {
    super.registerInputTriggers();

    // Add mouse-over to show labels

    _logicalLayer.registerTrigger(new InputTrigger(new MouseMovedCondition(), new TriggerAction() {
      @Override
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
          _textNode.getSceneHints().setCullHint(CullHint.Never);

          // set our text to the name of the ancestor of this object that is right under the _root node.
          final PickData pick = _pickResults.getPickData(0);
          if (pick.getTarget() instanceof Spatial) {
            final Spatial topLevel = getTopLevel((Spatial) pick.getTarget());
            if (!topLevel.equals(_picked)) {
              clearPicked();
              _picked = topLevel;
              _picked.addController(_pickedControl);
            }
            if (!_text.getText().equals(topLevel.getName())) {
              _text.setText(topLevel.getName());
            }
          }
        } else {
          // No pick, clear label.
          _textNode.getSceneHints().setCullHint(CullHint.Always);
          _text.setText("");

          clearPicked();
        }
      }

      private void clearPicked() {
        if (_picked != null) {
          if (_picked instanceof Mesh) {
            ((Mesh) _picked).setDefaultColor(ColorRGBA.WHITE);
          }
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
    line.getMeshData().setIndexMode(IndexMode.LineStrip);
    line.setAntialiased(true);
    line.setLineWidth(1);
    MaterialUtil.autoMaterials(line);

    return line;
  }

  private void addMesh(final Spatial spatial) {
    spatial.setTranslation((index % wrapCount) * 8 - wrapCount * 4, (index / wrapCount) * 8 - wrapCount * 4, -50);
    if (spatial instanceof Mesh) {
      ((Mesh) spatial).updateModelBound();
    }
    _root.attachChild(spatial);
    index++;
  }
}
