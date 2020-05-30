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
import com.ardor3d.math.Vector2;
import com.ardor3d.renderer.Camera;
import com.ardor3d.scenegraph.shape.Quad;
import com.ardor3d.util.ReadOnlyTimer;

/**
 * <p>
 * Demonstration of a GLSL effect titled 'To The Road Of Ribbon' by TX95 (2008).
 * </p>
 * <p>
 * Based on a production from the demoscene, the 1k intro by FRequency
 * (http://www.pouet.net/prod.php?which=53939). It made 2nd position in the Main demoparty held in
 * France
 * </p>
 * <p>
 * Adapted from demo at http://iquilezles.org/apps/shadertoy/
 * </p>
 */
@Purpose(
    htmlDescriptionKey = "com.ardor3d.example.renderer.GLSLRibbonExample", //
    thumbnailPath = "com/ardor3d/example/media/thumbnails/renderer_GLSLRibbonExample.jpg", //
    maxHeapMemory = 64)
public class GLSLRibbonExample extends ExampleBase {

  public static void main(final String[] args) {
    start(GLSLRibbonExample.class);
  }

  @Override
  protected void initExample() {
    _canvas.setTitle("'To The Road Of Ribbon' by TX95 - rendered in Ardor3D");
    final Camera cam = _canvas.getCanvasRenderer().getCamera();

    final Quad q = Quad.newFullScreenQuad();
    q.setRenderMaterial("road_ribbon_example.yaml");
    q.setProperty("resolution", new Vector2(cam.getWidth(), cam.getHeight()));
    _orthoRoot.attachChild(q);
  }

  @Override
  protected void updateExample(final ReadOnlyTimer timer) {
    _orthoRoot.setProperty("time", (float) timer.getTimeInSeconds());
  }
}
