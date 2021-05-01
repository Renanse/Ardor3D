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

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.image.Texture;
import com.ardor3d.input.control.FirstPersonControl;
import com.ardor3d.input.control.OrbitCamControl;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.shape.Teapot;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureManager;

/**
 * A demonstration of the OrbitCamControl, a controller that can position a camera in an orbit
 * around a given target - in this case, a teapot.
 */
@Purpose(
    htmlDescriptionKey = "com.ardor3d.example.basic.OrbitCamExample", //
    thumbnailPath = "com/ardor3d/example/media/thumbnails/basic_OrbitCamExample.jpg", //
    maxHeapMemory = 64)
public class OrbitCamExample extends ExampleBase {

  /** Our teapot target, initialized here to make it available immediately. */
  private final Mesh targetMesh = new Teapot("target");

  /** Our orbiter control. */
  private OrbitCamControl control;

  public static void main(final String[] args) {
    start(OrbitCamExample.class);
  }

  @Override
  protected void updateExample(final ReadOnlyTimer timer) {
    // update orbiter
    control.update(timer.getTimePerFrame());
  }

  @Override
  protected void initExample() {
    _canvas.setTitle("OrbitCam Example");

    // setup some basics on the teapot.
    targetMesh.setModelBound(new BoundingBox());
    targetMesh.setTranslation(new Vector3(0, 0, -15));
    _root.attachChild(targetMesh);

    // Add a texture to the mesh.
    final TextureState ts = new TextureState();
    ts.setTexture(TextureManager.load("images/ardor3d_white_256.jpg", Texture.MinificationFilter.Trilinear, true));
    targetMesh.setRenderState(ts);
  }

  @Override
  protected void registerInputTriggers() {
    super.registerInputTriggers();

    // clean out the first person handler
    FirstPersonControl.removeTriggers(_logicalLayer, _controlHandle);

    // add Orbit handler - set it up to control the main camera
    control = new OrbitCamControl(_canvas.getCanvasRenderer().getCamera(), targetMesh);
    control.setupMouseTriggers(_logicalLayer, true);
    control.setSphereCoords(15, 0, 0);
  }
}
