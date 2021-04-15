/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.example.renderer;

import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.framework.CanvasRenderer;
import com.ardor3d.input.keyboard.Key;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyPressedCondition;
import com.ardor3d.light.LightProperties;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.util.MathUtils;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.scenegraph.shape.Box;
import com.ardor3d.scenegraph.shape.Quad;
import com.ardor3d.util.GameTaskQueue;
import com.ardor3d.util.GameTaskQueueManager;
import com.ardor3d.util.ReadOnlyTimer;

/**
 * Illustrates the Camera class, which represents a view into a 3d scene and how that view should
 * map to a 2D rendering surface.
 */
@Purpose(
    htmlDescriptionKey = "com.ardor3d.example.renderer.ViewportExample", //
    thumbnailPath = "com/ardor3d/example/media/thumbnails/renderer_ViewportExample.jpg", //
    maxHeapMemory = 64)
public class ViewportExample extends ExampleBase {

  private Box box2;
  private Quad quad;
  private Box box1;
  private boolean fullViewport = true;

  public static void main(final String[] args) {
    start(ViewportExample.class);
  }

  @Override
  protected void updateExample(final ReadOnlyTimer timer) {
    // Test getScreenCoordinates by centering our Quad on box2.
    if (ContextManager.getCurrentContext() != null && Camera.getCurrentCamera() != null) {
      final Camera camera = Camera.getCurrentCamera();
      final Vector3 vec3 = Vector3.fetchTempInstance();
      camera.getScreenCoordinates(box2.getWorldTranslation(), vec3);
      quad.setTranslation(vec3);
      Vector3.releaseTempInstance(vec3);
    }
  }

  @Override
  protected void initExample() {
    _canvas.setTitle("Viewport Example");
    final CanvasRenderer canvasRenderer = _canvas.getCanvasRenderer();
    final RenderContext renderContext = canvasRenderer.getRenderContext();
    final Renderer renderer = canvasRenderer.getRenderer();
    GameTaskQueueManager.getManager(renderContext).getQueue(GameTaskQueue.RENDER).enqueue(() -> {
      renderer.setBackgroundColor(ColorRGBA.BLUE);
      return null;
    });

    // A box to check aspect ratio of 3D objects
    box1 = new Box("test box 1", new Vector3(-1, -1, -1), new Vector3(1, 1, 1));
    box1.setTranslation(0, 5, 0);
    box1.setRotation(new Quaternion().fromEulerAngles(MathUtils.DEG_TO_RAD * 45, MathUtils.DEG_TO_RAD * 60,
        MathUtils.DEG_TO_RAD * 30));
    box1.setSolidColor(ColorRGBA.GREEN);
    box1.updateModelBound();

    // Another one, not rotated
    box2 = new Box("test box 2", new Vector3(-1, -1, -1), new Vector3(1, 1, 1));
    box2.setTranslation(0, -5, 0);
    box2.setSolidColor(ColorRGBA.RED);
    box2.updateModelBound();

    // A text to check aspect ratio of 2D objects
    quad = new Quad("test quad", 100, 20);
    LightProperties.setLightReceiver(quad, false);
    // if we don't explicitly update model bounds, quad will keep its default infinite bounding sphere,
    // so it will always display (and it will prevent rootnode from culling)

    // Attach children
    _root.attachChild(box1);
    _root.attachChild(box2);
    _orthoRoot.attachChild(quad);

    _root.setRenderMaterial("unlit/untextured/vertex_color.yaml");
  }

  @Override
  protected void registerInputTriggers() {
    super.registerInputTriggers();

    _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.V), (source, inputStates, tpf) -> {

      fullViewport = !fullViewport;
      setFullViewport(fullViewport, source.getCanvasRenderer().getRenderer());
    }));
  }

  private void setFullViewport(final boolean full, final Renderer r) {
    final double vpLeft = full ? 0 : 0.25;
    final double vpRight = full ? 1 : 0.75;

    final double vpBottom = full ? 0 : 0.25;
    final double vpTop = full ? 1 : 0.75;

    final Camera camera = Camera.getCurrentCamera();
    camera.setViewPort(vpLeft, vpRight, vpBottom, vpTop);
    _orthoCam.setViewPort(vpLeft, vpRight, vpBottom, vpTop);

    fullViewport = full;
  }
}
