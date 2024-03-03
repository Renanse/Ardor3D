/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
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
import com.ardor3d.light.SpotLight;
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
import com.ardor3d.scenegraph.shape.Cone;
import com.ardor3d.scenegraph.shape.Cylinder;
import com.ardor3d.scenegraph.shape.PQTorus;
import com.ardor3d.scenegraph.shape.Quad;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureManager;

/**
 * Shows off two spot lights casting shadows on a simple, rotating scene.
 */
@Purpose(
    htmlDescriptionKey = "com.ardor3d.example.lighting.SpotLightExample", //
    thumbnailPath = "com/ardor3d/example/media/thumbnails/lighting_SpotLightExample.jpg", //
    maxHeapMemory = 64)
public class SpotLightExample extends ExampleBase {

  protected double rotAngle = 0.0;
  protected Quaternion rot = new Quaternion();

  public static void main(final String[] args) {
    start(SpotLightExample.class);
  }

  @Override
  protected void initExample() {
    _canvas.setTitle("Spot Light Example");
    _canvas.getCanvasRenderer().getRenderer().setBackgroundColor(ColorRGBA.BLACK);
    _root.setRenderMaterial("lit/untextured/basic_phong.yaml");

    final TextureState ts = new TextureState();
    ts.setTexture(TextureManager.load("images/ardor3d_white_256.jpg", Texture.MinificationFilter.Trilinear, true));

    final TextureState ts2 = new TextureState();
    ts2.setTexture(
        TextureManager.load("images/pbr/bamboo-wood-semigloss/albedo.png", Texture.MinificationFilter.Trilinear, true));

    final var rotNode = new Node("rotate");
    rotNode.addController((time, caller) -> {
      rotAngle += time * 0.25;
      rot.fromAngleAxis(rotAngle, Vector3.NEG_UNIT_Y);
      caller.setRotation(rot);
    });
    _root.attachChild(rotNode);

    final var plane = new Quad("floor", 75, 75);
    plane.setTranslation(0, 0, 0);
    plane.setRotation(new Quaternion().fromAngleNormalAxis(MathUtils.HALF_PI, Vector3.NEG_UNIT_X));
    plane.setRenderState(ts);
    plane.setRenderMaterial("lit/textured/basic_phong.yaml");
    LightProperties.setShadowCaster(plane, false);
    _root.attachChild(plane);

    final var pq = new PQTorus("PQTorus", 3, 2, 1.5, .5, 128, 8);
    pq.setTranslation(new Vector3(0, 4.5, 0));
    rotNode.attachChild(pq);

    final var platform = new Cylinder("platform", 4, 64, 3, 1, true);
    platform.setTranslation(new Vector3(0, .5, 0));
    platform.setRotation(new Quaternion().fromAngleNormalAxis(MathUtils.HALF_PI, Vector3.NEG_UNIT_X));
    platform.setRenderState(ts2);
    platform.setRenderMaterial("lit/textured/basic_phong.yaml");
    rotNode.attachChild(platform);

    final var cam = _canvas.getCanvasRenderer().getCamera();
    cam.setLocation(-16, 29, 19);
    cam.lookAt(0, 2, 0, Vector3.UNIT_Y);
  }

  /** Quads used for debug showing shadowmaps. */
  private final Quad[] _orthoQuad = new Quad[2];

  Node light1, light2;

  @Override
  protected void updateExample(final ReadOnlyTimer timer) {
    super.updateExample(timer);
    final double radius = 15;
    final double height = 12;
    final double speed = .5;
    final var xOffset = Math.sin(speed * timer.getTimeInSeconds() * 2);
    light1.setTranslation(radius + xOffset * radius / 2, height, radius);
    light1.lookAt(0, 1, 0);
    light2.setTranslation(-(radius + xOffset * radius / 2), height, radius);
    light2.lookAt(0, 1, 0);
  }

  @Override
  protected void setupLight() {
    light1 = new Node("light1");
    light1.setTranslation(18, 10, 0);
    _root.attachChild(light1);

    final Cone c1 = new Cone("c1", 4, 16, .5, 1);
    LightProperties.setLightReceiver(c1, false);
    c1.setRenderMaterial("unlit/untextured/basic.yaml");
    light1.attachChild(c1);

    final SpotLight spot1 = new SpotLight();
    spot1.setIntensity(1.0f);
    spot1.setAngle((float) (30f * MathUtils.DEG_TO_RAD));
    spot1.setInnerAngle((float) (27f * MathUtils.DEG_TO_RAD));
    spot1.setColor(ColorRGBA.WHITE);
    spot1.setShadowCaster(true);
    c1.setDefaultColor(spot1.getColor());
    light1.attachChild(spot1);

    light2 = new Node("light2");
    light2.setTranslation(-18, 10, 0);
    _root.attachChild(light2);

    final Cone c2 = new Cone("c2", 4, 16, .5, 1);
    LightProperties.setLightReceiver(c2, false);
    c2.setRenderMaterial("unlit/untextured/basic.yaml");
    light2.attachChild(c2);

    final SpotLight spot2 = new SpotLight();
    spot2.setIntensity(1.0f);
    spot2.setAngle((float) (30f * MathUtils.DEG_TO_RAD));
    spot2.setInnerAngle((float) (27f * MathUtils.DEG_TO_RAD));
    spot2.setColor(ColorRGBA.WHITE);
    spot2.setShadowCaster(true);
    c2.setDefaultColor(spot2.getColor());
    light2.attachChild(spot2);

    _orthoQuad[0] = makeDebugQuad(spot1);
    _orthoQuad[0].setTranslation(10 + QUAD_SIZE / 2, 10 + QUAD_SIZE / 2, 0);
    _orthoQuad[1] = makeDebugQuad(spot2);
    _orthoQuad[1].setTranslation(20 + (3 * QUAD_SIZE / 2), 10 + QUAD_SIZE / 2, 0);
  }

  final int QUAD_SIZE = 128;

  private Quad makeDebugQuad(final Light light) {
    final Quad quad = new Quad("fsq", QUAD_SIZE, QUAD_SIZE);
    LightProperties.setLightReceiver(quad, false);

    quad.setRenderMaterial("occluder/debug_quad_spot.yaml");
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
