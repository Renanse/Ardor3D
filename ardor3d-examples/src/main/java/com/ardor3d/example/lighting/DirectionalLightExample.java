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
import com.ardor3d.light.DirectionalLight;
import com.ardor3d.light.Light;
import com.ardor3d.light.LightProperties;
import com.ardor3d.light.shadow.AbstractShadowData;
import com.ardor3d.light.shadow.DirectionalShadowData;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.util.MathUtils;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.scenegraph.hint.SceneHints;
import com.ardor3d.scenegraph.hint.TextureCombineMode;
import com.ardor3d.scenegraph.shape.Arrow;
import com.ardor3d.scenegraph.shape.Cylinder;
import com.ardor3d.scenegraph.shape.PQTorus;
import com.ardor3d.scenegraph.shape.Quad;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureManager;

/**
 * Shows off a directional light casting shadows in a simple scene.
 */
@Purpose(
    htmlDescriptionKey = "com.ardor3d.example.lighting.DirectionalLightExample", //
    thumbnailPath = "com/ardor3d/example/media/thumbnails/lighting_DirectionalLightExample.jpg", //
    maxHeapMemory = 64)
public class DirectionalLightExample extends ExampleBase {

  private double _rotAngle = 0.0;
  private final Quaternion _rot = new Quaternion();

  /** Quads used for debug showing shadowmaps. */
  private final static int SPLITS = 4;
  private final static int QUAD_SIZE = 128;
  private final Quad _orthoQuad[] = new Quad[SPLITS];

  private Node light;

  public static void main(final String[] args) {
    start(DirectionalLightExample.class);
  }

  @Override
  protected void initExample() {
    _canvas.setTitle("Directional Light Example");
    _canvas.getCanvasRenderer().getRenderer().setBackgroundColor(ColorRGBA.BLACK);
    _root.setRenderMaterial("lit/untextured/basic_phong.yaml");

    final TextureState ts = new TextureState();
    ts.setTexture(TextureManager.load("images/ardor3d_white_256.jpg", Texture.MinificationFilter.Trilinear, true));

    final TextureState ts2 = new TextureState();
    ts2.setTexture(
        TextureManager.load("images/pbr/bamboo-wood-semigloss/albedo.png", Texture.MinificationFilter.Trilinear, true));

    final var rotNode = new Node("rotate");
    rotNode.addController((time, caller) -> {
      _rotAngle += time * 0.25;
      _rot.fromAngleAxis(_rotAngle, Vector3.NEG_UNIT_Y);
      caller.setRotation(_rot);
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

  @Override
  protected void updateExample(final ReadOnlyTimer timer) {
    super.updateExample(timer);
    final double offset = 10;
    final double height = 30;
    final double speed = .5;
    final var xOffset = Math.sin(speed * timer.getTimeInSeconds() * 2);
    light.setTranslation(offset, 0.5 * height * (1.0 + xOffset), offset);
    light.lookAt(0, 1, 0);
  }

  @Override
  protected void setupLight() {
    light = new Node("light1");
    _root.attachChild(light);

    final Arrow arrow = new Arrow("arrow", 1, .25);
    LightProperties.setLightReceiver(arrow, false);
    LightProperties.setShadowCaster(arrow, false);
    arrow.setRotation(new Matrix3().fromAngleNormalAxis(MathUtils.HALF_PI, Vector3.UNIT_X));
    arrow.setRenderMaterial("unlit/untextured/basic.yaml");
    light.attachChild(arrow);

    final DirectionalLight dl = new DirectionalLight();
    dl.setIntensity(1.0f);
    dl.setColor(ColorRGBA.WHITE);
    dl.setShadowCaster(true);
    dl.getShadowData().setFilterMode(AbstractShadowData.FILTER_MODE_PCF);
    dl.getShadowData().setMaxDistance(200);
    dl.getShadowData().setMinimumCameraDistance(50);
    dl.getShadowData().setCascades(SPLITS);
    arrow.setDefaultColor(dl.getColor());
    light.attachChild(dl);

    for (int i = 0; i < SPLITS; i++) {
      _orthoQuad[i] = makeDebugQuad(dl, i);
      _orthoQuad[i].setTranslation(i * 10 + (i * 2 + 1) * QUAD_SIZE / 2, 10 + QUAD_SIZE / 2, 0);
    }
  }

  private Quad makeDebugQuad(final Light light, final int i) {
    final Quad quad = new Quad("fsq", QUAD_SIZE, QUAD_SIZE);
    quad.setProperty(DirectionalShadowData.KEY_DebugSplit, i);
    LightProperties.setLightReceiver(quad, false);
    LightProperties.setShadowCaster(quad, false);

    quad.setRenderMaterial("occluder/debug_quad_directional.yaml");
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
