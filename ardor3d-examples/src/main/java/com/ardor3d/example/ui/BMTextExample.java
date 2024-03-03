/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.example.ui;

import java.util.List;
import java.util.Random;

import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.framework.CanvasRenderer;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.CullState;
import com.ardor3d.renderer.state.CullState.Face;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.controller.SpatialController;
import com.ardor3d.scenegraph.shape.Box;
import com.ardor3d.ui.text.BMFont;
import com.ardor3d.ui.text.BMText;
import com.ardor3d.ui.text.BMText.AutoFade;
import com.ardor3d.ui.text.BMText.AutoScale;
import com.ardor3d.util.GameTaskQueue;
import com.ardor3d.util.GameTaskQueueManager;

/**
 * Illustrates how to modify text properties (e.g. font, color, alignment) and display on a canvas.
 */
@Purpose(
    htmlDescriptionKey = "com.ardor3d.example.ui.BMTextExample", //
    thumbnailPath = "com/ardor3d/example/media/thumbnails/ui_BMTextExample.jpg", //
    maxHeapMemory = 64)
public class BMTextExample extends ExampleBase {

  private final Matrix3 rotate = new Matrix3();

  public static void main(final String[] args) {
    start(BMTextExample.class);
  }

  @Override
  protected void initExample() {
    _canvas.setTitle("BMFont Text Example");
    final ColorRGBA backgroundColor = new ColorRGBA(0.3f, 0.3f, 0.5f, 1);
    final CanvasRenderer canvasRenderer = _canvas.getCanvasRenderer();
    final RenderContext renderContext = canvasRenderer.getRenderContext();
    final Renderer renderer = canvasRenderer.getRenderer();
    GameTaskQueueManager.getManager(renderContext).getQueue(GameTaskQueue.RENDER).enqueue(() -> {
      renderer.setBackgroundColor(backgroundColor);
      return null;
    });

    addTextTestNodes(_root);
  }

  /**
   *
   * @param topNode
   */
  // -----------------------------------------------------
  void addTextTestNodes(final Node topNode) {
    final Node zup = new Node("zup");
    rotate.fromAngleNormalAxis(-Math.PI / 2, Vector3.UNIT_X);
    zup.setRotation(rotate);

    final Node textMoveNode = new Node("textModeNode");

    final CullState cs = new CullState();
    cs.setCullFace(Face.Back);
    textMoveNode.setRenderState(cs);

    final Node textExampleNode = new Node("textExampleNode");
    textExampleNode.setTranslation(0, 0, 0);
    final double a = 9.0;
    final double b = 4.5;
    newBox(textExampleNode, 0, 1, 0);
    newBox(textExampleNode, -a, 1, -b);
    newBox(textExampleNode, -a, 1, b);
    newBox(textExampleNode, a, 1, b);
    newBox(textExampleNode, a, 1, -b);

    topNode.attachChild(zup);
    zup.attachChild(textMoveNode);
    textMoveNode.attachChild(textExampleNode);

    final SpatialController<BMText> fontChanger = createFontChanger();
    final SpatialController<Node> nodeMover = createNodeMover();
    textMoveNode.addController(nodeMover);

    final BMFont font = BMFontLoader.defaultFont();
    final String initialString = font.getStyleName() + "\nThe Quick Brown Fox...";
    final double fontScale = 1.0;
    if (true) {
      final BMText text = new BMText("textSpatial1", initialString, font, BMText.Align.SouthWest, BMText.Justify.Right);
      text.setFontScale(fontScale);
      text.setAutoFade(AutoFade.CapScreenSize);
      text.setAutoFadeFalloff(1.0f);
      text.setAutoScale(AutoScale.CapScreenSize);
      text.setAutoRotate(true);
      textExampleNode.attachChild(text);
      text.addController(fontChanger);
    }

    if (true) {
      final BMText text = new BMText("textSpatial2", initialString, font, BMText.Align.SouthEast, BMText.Justify.Left);
      text.setFontScale(fontScale);
      text.setAutoFade(AutoFade.FixedPixelSize);
      text.setAutoFadeFalloff(0.5f);
      text.setAutoFadeFixedPixelSize(15);
      text.setAutoScale(AutoScale.CapScreenSize);
      text.setAutoRotate(true);
      textExampleNode.attachChild(text);
      text.addController(fontChanger);
    }

    if (true) {
      final BMText text = new BMText("textSpatial3", initialString, font, BMText.Align.NorthEast, BMText.Justify.Left);
      text.setFontScale(fontScale);
      text.setAutoFade(AutoFade.Off);
      text.setAutoScale(AutoScale.FixedScreenSize);
      text.setAutoRotate(true);
      textExampleNode.attachChild(text);
      text.addController(fontChanger);
    }

    if (true) {
      final BMText text =
          new BMText("textSpatial4", initialString, font, BMText.Align.NorthWest, BMText.Justify.Center);
      text.setFontScale(fontScale);
      text.setAutoFade(AutoFade.DistanceRange);
      text.setAutoFadeDistanceRange(10, 100); // start fading distance to camera > 10
      text.setAutoScale(AutoScale.Off);
      text.setAutoRotate(true);
      textExampleNode.attachChild(text);
      text.addController(fontChanger);
    }
  }

  /**
   *
   * @param text
   *          the text object
   * @return the proper contents to use for our text object.
   */
  // -----------------------------------------------------
  String testDisplayString(final BMText text) {
    return text.getFont().getStyleName() + "\n" + // -------------------
        "justify[" + text.getJustify().toString() + "]\n" + // -----
        "scale  [" + text.getAutoScale().toString() + "]\n" + // ---
        "fade   [" + text.getAutoFade().toString() + "]\n" + // ----
        "align  [" + text.getAlign().toString() + "]\n" + // -------
        "The Quick Brown Fox \nJumps over the Lazy Dog.";
  }

  /**
   *
   */
  // -----------------------------------------------------
  SpatialController<BMText> createFontChanger() {
    final SpatialController<BMText> fontChanger = new SpatialController<>() {
      final Random rand = new Random();
      final int changeInterval = 20000;
      final List<BMFont> fonts = BMFontLoader.allFonts();

      @Override
      public void update(final double time, final BMText text) {
        final int t = (int) (System.currentTimeMillis() / changeInterval);
        final int index = t % fonts.size();
        if (!fonts.get(index).equals(text.getFont())) {
          if (rand.nextDouble() < 0.15) {
            text.setAutoRotate(false);
          } else {
            text.setAutoRotate(true);
          }

          if (rand.nextDouble() < 0.5) {
            final ColorRGBA clr = new ColorRGBA(rand.nextFloat(), rand.nextFloat(), rand.nextFloat(), 1);
            text.setTextColor(clr);
          } else {
            text.setTextColor(ColorRGBA.WHITE);
          }
          text.setFont(fonts.get(index));
          text.setText(testDisplayString(text));
        }
      }
    };
    return fontChanger;
  }

  /**
   *
   */
  // -----------------------------------------------------
  private SpatialController<Node> createNodeMover() {
    final SpatialController<Node> nodeMover = new SpatialController<>() {
      Matrix3 rot = new Matrix3();

      @Override
      public void update(final double time, final Node caller) {
        final long t = System.currentTimeMillis();
        final double s = Math.cos(t * Math.PI / 10000.0);
        final double y = 60 + s * 65;
        caller.setTranslation(0.5, y, 0);
        rot.fromAngleNormalAxis(-(t / 1000.0) % (2 * Math.PI), Vector3.UNIT_Z);
        caller.setRotation(rot);
      }
    };
    return nodeMover;
  }

  /**
   *
   */
  // -----------------------------------------------------
  void newBox(final Node parent, final double x, final double y, final double z) {
    final float sz = 3;
    final Box box = new Box("box", new Vector3(Vector3.ZERO), new Vector3(sz, sz, sz));
    box.setTranslation(x, y, z);
    // box.setRandomColors();
    parent.attachChild(box);
  }

}
