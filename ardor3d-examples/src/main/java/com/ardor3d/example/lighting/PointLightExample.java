/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.example.lighting;

import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.image.Texture;
import com.ardor3d.light.Light;
import com.ardor3d.light.LightProperties;
import com.ardor3d.light.PointLight;
import com.ardor3d.light.shadow.PointShadowData;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.util.MathUtils;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.scenegraph.hint.SceneHints;
import com.ardor3d.scenegraph.hint.TextureCombineMode;
import com.ardor3d.scenegraph.shape.Box;
import com.ardor3d.scenegraph.shape.Quad;
import com.ardor3d.scenegraph.shape.Sphere;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureManager;

/**
 * Shows off a point light casting shadows while moving through a simple scene.
 */
@Purpose(
    htmlDescriptionKey = "com.ardor3d.example.lighting.PointLightExample", //
    thumbnailPath = "com/ardor3d/example/media/thumbnails/lighting_PointLightExample.jpg", //
    maxHeapMemory = 64)
public class PointLightExample extends ExampleBase {

  private Node lightNode;
  private final double xRadius = 22;
  private final double zRadius = 12;
  private final double height = 6;
  private final double speed = .5;
  private final static int QUAD_SIZE = 128;
  private final Quad[] _orthoQuad = new Quad[6];

  public static void main(final String[] args) {
    start(PointLightExample.class);
  }

  @Override
  protected void initExample() {
    _canvas.setTitle("Point Light Example");
    _canvas.getCanvasRenderer().getRenderer().setBackgroundColor(ColorRGBA.BLACK);

    final TextureState ts = new TextureState();
    ts.setTexture(TextureManager.load("images/ardor3d_white_256.jpg", Texture.MinificationFilter.Trilinear, true));

    final var plane = new Quad("floor", 75, 75);
    plane.setTranslation(0, 0, 0);
    plane.setRotation(new Quaternion().fromAngleNormalAxis(MathUtils.HALF_PI, Vector3.NEG_UNIT_X));
    plane.setRenderState(ts);
    plane.setRenderMaterial("lit/textured/basic_phong.yaml");
    LightProperties.setShadowCaster(plane, false);
    _root.attachChild(plane);

    for (int i = -30; i <= 30; i += 15) {
      for (int j = -30; j <= 30; j += 10) {
        if (i == 0 && j == 0) {
          continue;
        }
        newBox(2, 10, new Vector3(i, 0, j));
      }
    }

    final var cam = _canvas.getCanvasRenderer().getCamera();
    cam.setLocation(8, 85, 38);
    cam.lookAt(0, 0, 0, Vector3.UNIT_Y);
  }

  @Override
  protected void setupLight() {
    lightNode = new Node("light");
    _root.attachChild(lightNode);

    light = new PointLight();
    light.setColor(ColorRGBA.WHITE);
    light.setIntensity(1f);
    light.setLinear(.01f);
    light.setConstant(1);
    light.setEnabled(true);
    light.setShadowCaster(true);
    lightNode.attachChild(light);

    final Sphere lightSphere = new Sphere("light", 16, 16, .25);
    lightSphere.setRenderMaterial("unlit/untextured/basic.yaml");
    LightProperties.setLightReceiver(lightSphere, false);
    LightProperties.setShadowCaster(lightSphere, false);
    lightNode.attachChild(lightSphere);
    lightSphere.setDefaultColor(light.getColor());

    for (int i = 0; i < 6; i++) {
      _orthoQuad[i] = makeDebugQuad(light, i);
      _orthoQuad[i].setTranslation(i * 10 + (i * 2 + 1) * QUAD_SIZE / 2, 10 + QUAD_SIZE / 2, 0);
    }
  }

  @Override
  protected void updateExample(final ReadOnlyTimer timer) {
    super.updateExample(timer);
    final var val = speed * timer.getTimeInSeconds();
    final var sinOffset = Math.sin(val);
    final var cosOffset = Math.cos(val);
    lightNode.setTranslation(xRadius * cosOffset, height, zRadius * sinOffset * cosOffset);
  }

  private Box newBox(final double width, final double height, final Vector3 loc) {
    final Box b = new Box("box", new Vector3(0, height / 2, 0), width / 2, height / 2, width / 2);
    b.setTranslation(loc);
    b.setRenderMaterial("lit/untextured/basic_phong.yaml");
    _root.attachChild(b);
    return b;
  }

  private Quad makeDebugQuad(final Light light, final int i) {
    final Quad quad = new Quad("fsq", QUAD_SIZE, QUAD_SIZE);
    quad.setProperty(PointShadowData.KEY_DebugFace, i);
    LightProperties.setLightReceiver(quad, false);
    LightProperties.setShadowCaster(quad, false);

    quad.setRenderMaterial("occluder/debug_quad_cube.yaml");
    final SceneHints sceneHints = quad.getSceneHints();
    sceneHints.setCullHint(CullHint.Never);
    sceneHints.setRenderBucketType(RenderBucketType.OrthoOrder);
    sceneHints.setTextureCombineMode(TextureCombineMode.Replace);
    _orthoRoot.attachChild(quad);

    final TextureState ts = new TextureState();
    ts.setTexture(light.getShadowData().getTexture());
    quad.setRenderState(ts);

    return quad;
  }

}
