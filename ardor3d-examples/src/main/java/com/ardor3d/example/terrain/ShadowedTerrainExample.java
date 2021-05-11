/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.example.terrain;

import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.extension.terrain.client.Terrain;
import com.ardor3d.extension.terrain.client.TerrainBuilder;
import com.ardor3d.extension.terrain.client.TerrainDataProvider;
import com.ardor3d.extension.terrain.heightmap.MidPointHeightMapGenerator;
import com.ardor3d.extension.terrain.providers.array.ArrayTerrainDataProvider;
import com.ardor3d.input.keyboard.Key;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyPressedCondition;
import com.ardor3d.intersection.PickingUtil;
import com.ardor3d.intersection.PrimitivePickResults;
import com.ardor3d.light.DirectionalLight;
import com.ardor3d.light.Light;
import com.ardor3d.light.LightProperties;
import com.ardor3d.light.shadow.DirectionalShadowData;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Ray3;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.material.fog.FogParams;
import com.ardor3d.renderer.material.fog.FogParams.DensityFunction;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.CullState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.scenegraph.hint.SceneHints;
import com.ardor3d.scenegraph.hint.TextureCombineMode;
import com.ardor3d.scenegraph.shape.Box;
import com.ardor3d.scenegraph.shape.Quad;
import com.ardor3d.scenegraph.shape.Sphere;
import com.ardor3d.ui.text.BasicText;
import com.ardor3d.util.ReadOnlyTimer;

/**
 * Example showing off Ardor3D's terrain system with directional light shadows.
 */
@Purpose(
    htmlDescriptionKey = "com.ardor3d.example.terrain.ShadowedTerrainExample", //
    thumbnailPath = "com/ardor3d/example/media/thumbnails/terrain_ShadowedTerrainExample.jpg", //
    maxHeapMemory = 128)
public class ShadowedTerrainExample extends ExampleBase {
  /** The Constant logger. */
  private static final Logger logger = Logger.getLogger(ShadowedTerrainExample.class.getName());

  private boolean updateTerrain = true;
  private final float farPlane = 2500.0f;

  private Terrain terrain;

  private final Sphere sphere = new Sphere("sp", 16, 16, 1);
  private final Ray3 pickRay = new Ray3();

  private boolean groundCamera = false;
  private Camera terrainCamera;

  /** Text fields used to present info about the example. */
  private final BasicText _exampleInfo[] = new BasicText[5];

  private DirectionalLight dl;

  private final static int SPLITS = 6;
  private final static int QUAD_SIZE = 128;
  private final Quad _orthoQuad[] = new Quad[SPLITS];

  public static void main(final String[] args) {
    ExampleBase._minDepthBits = 24;
    ExampleBase.start(ShadowedTerrainExample.class);
  }

  @Override
  protected void updateExample(final ReadOnlyTimer timer) {
    final Camera camera = _canvas.getCanvasRenderer().getCamera();

    // Make sure camera is above terrain
    final double height = terrain.getHeightAt(camera.getLocation().getX(), camera.getLocation().getZ());
    if (height > -Float.MAX_VALUE && (groundCamera || camera.getLocation().getY() < height + 3)) {
      camera.setLocation(new Vector3(camera.getLocation().getX(), height + 3, camera.getLocation().getZ()));
    }

    if (updateTerrain) {
      terrainCamera.set(camera);
    }

    // if we're picking...
    if (sphere.getSceneHints().getCullHint() == CullHint.Dynamic) {
      // Set up our pick ray
      pickRay.setOrigin(camera.getLocation());
      pickRay.setDirection(camera.getDirection());

      // do pick and move the sphere
      final PrimitivePickResults pickResults = new PrimitivePickResults();
      pickResults.setCheckDistance(true);
      PickingUtil.findPick(_root, pickRay, pickResults);
      if (pickResults.getNumber() != 0) {
        final Vector3 intersectionPoint = pickResults.getPickData(0).getIntersectionRecord().getIntersectionPoint(0);
        sphere.setTranslation(intersectionPoint);
        // XXX: maybe change the color of the ball for valid vs. invalid?
      }
    }
  }

  @Override
  protected void initExample() {
    Terrain.addDefaultResourceLocators();

    _canvas.setTitle("Shadowed Terrain Example");
    _canvas.getCanvasRenderer().getRenderer().setBackgroundColor(ColorRGBA.GRAY);

    final Camera cam = _canvas.getCanvasRenderer().getCamera();
    cam.setLocation(new Vector3(440, 215, 275));
    cam.lookAt(new Vector3(450, 140, 360), Vector3.UNIT_Y);
    cam.setFrustumPerspective(70.0, (float) cam.getWidth() / cam.getHeight(), 1.0f, farPlane);
    _controlHandle.setMoveSpeed(200);

    setupDefaultStates();

    sphere.getSceneHints().setAllPickingHints(false);
    sphere.getSceneHints().setCullHint(CullHint.Always);
    _root.attachChild(sphere);

    try {
      // Keep a separate camera to be able to freeze terrain update
      terrainCamera = new Camera(cam);

      final int SIZE = 2048;

      final MidPointHeightMapGenerator raw = new MidPointHeightMapGenerator(SIZE, 0.95f);
      raw.setHeightRange(0.2f);
      final float[] heightMap = raw.getHeightData();

      final TerrainDataProvider terrainDataProvider =
          new ArrayTerrainDataProvider(heightMap, SIZE, new Vector3(1, 300, 1), true);

      terrain = new TerrainBuilder(terrainDataProvider, terrainCamera).build();
      terrain.setRenderMaterial("clipmap/terrain_textured_normal_map.yaml");

      _root.attachChild(terrain);
    } catch (final Exception e) {
      logger.log(Level.SEVERE, "Problem setting up terrain...", e);
      System.exit(1);
    }

    final Node occluders = setupOccluders();
    _root.attachChild(occluders);

    // Setup labels for presenting example info.
    final Node textNodes = new Node("Text");
    _orthoRoot.attachChild(textNodes);
    LightProperties.setLightReceiver(textNodes, false);

    final double infoStartY = _canvas.getCanvasRenderer().getCamera().getHeight() / 2;
    for (int i = 0; i < _exampleInfo.length; i++) {
      _exampleInfo[i] = BasicText.createDefaultTextLabel("Text", "", 16);
      _exampleInfo[i].setTranslation(new Vector3(10, infoStartY - i * 20, 0));
      textNodes.attachChild(_exampleInfo[i]);
    }

    textNodes.updateGeometricState(0.0);
    updateText();

    _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.U), (source, inputStates, tpf) -> {
      updateTerrain = !updateTerrain;
      updateText();
    }));
    _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.ONE), (source, inputStates, tpf) -> {
      _controlHandle.setMoveSpeed(5);
      updateText();
    }));
    _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.TWO), (source, inputStates, tpf) -> {
      _controlHandle.setMoveSpeed(50);
      updateText();
    }));
    _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.THREE), (source, inputStates, tpf) -> {
      _controlHandle.setMoveSpeed(400);
      updateText();
    }));
    _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.FOUR), (source, inputStates, tpf) -> {
      _controlHandle.setMoveSpeed(1000);
      updateText();
    }));

    _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.SPACE), (source, inputStates, tpf) -> {
      groundCamera = !groundCamera;
      updateText();
    }));

    _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.P), (source, inputStates, tpf) -> {
      if (sphere.getSceneHints().getCullHint() == CullHint.Dynamic) {
        sphere.getSceneHints().setCullHint(CullHint.Always);
      } else if (sphere.getSceneHints().getCullHint() == CullHint.Always) {
        sphere.getSceneHints().setCullHint(CullHint.Dynamic);
      }
      updateText();
    }));
    _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.R), (source, inputStates, tpf) -> {
      terrain.getTextureClipmap().setShowDebug(!terrain.getTextureClipmap().isShowDebug());
      updateText();
    }));
    _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.FIVE), (source, inputStates, tpf) -> {
      terrain.getTextureClipmap().setPixelDensity(terrain.getTextureClipmap().getPixelDensity() / 2);
      updateText();
    }));
    _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.SIX), (source, inputStates, tpf) -> {
      terrain.getTextureClipmap().setPixelDensity(terrain.getTextureClipmap().getPixelDensity() * 2);
      updateText();
    }));
    _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.ZERO), (source, inputStates, tpf) -> {
      final Camera cam1 = _canvas.getCanvasRenderer().getCamera();
      System.out.println("camera location: " + cam1.getLocation());
      System.out.println("camera direction: " + cam1.getDirection());
    }));

  }

  private Node setupOccluders() {
    final Node occluders = new Node("Occluders");

    final Random rand = new Random(1337);
    for (int x = 0; x < 8; x++) {
      for (int y = 0; y < 8; y++) {

        final Box box = new Box("Box", new Vector3(), 5, 40, 5);
        box.setModelBound(new BoundingBox());

        box.setTranslation(500 + rand.nextDouble() * 300 - 150, 20 + rand.nextDouble() * 5.0,
            500 + rand.nextDouble() * 300 - 150);
        occluders.attachChild(box);
        LightProperties.setShadowCaster(box, true);
        LightProperties.setLightReceiver(box, true);
        box.setRenderMaterial("lit/untextured/basic_phong.yaml");
      }
    }

    return occluders;
  }

  private void setupDefaultStates() {
    final CullState cs = new CullState();
    cs.setEnabled(true);
    cs.setCullFace(CullState.Face.Back);
    _root.setRenderState(cs);

    final FogParams fog = new FogParams();
    fog.setStart(farPlane / 2.0f);
    fog.setEnd(farPlane);
    fog.setColor(new ColorRGBA(1.0f, 1.0f, 1.0f, 1.0f));
    fog.setFunction(DensityFunction.Linear);
    _root.setProperty(FogParams.DefaultPropertyKey, fog);
  }

  @Override
  protected void setupLight() {
    dl = new DirectionalLight();
    dl.setEnabled(true);
    dl.setColor(ColorRGBA.WHITE);
    dl.setIntensity(1.0f);
    dl.setTranslation(280, 118, 544);
    dl.lookAt(305, 100, 545);
    _root.attachChild(dl);

    // shadow params - have to play around a bit to get it looking nice for your scene
    dl.setShadowCaster(true);
    dl.getShadowData().setBias(0.01f);
    dl.getShadowData().setCascades(SPLITS);
    dl.getShadowData().setTextureSize(1024);
    dl.getShadowData().setTextureDepthBits(32);
    dl.getShadowData().setMinimumCameraDistance(500);
    dl.getShadowData().setMaxDistance(farPlane);

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

  /**
   * Update text information.
   */
  private void updateText() {
    _exampleInfo[0].setText("[1/2/3] Moving speed: " + _controlHandle.getMoveSpeed() * 3.6 + "km/h");
    _exampleInfo[1].setText("[P] Do picking: " + (sphere.getSceneHints().getCullHint() == CullHint.Dynamic));
    _exampleInfo[2].setText("[SPACE] Toggle fly/walk: " + (groundCamera ? "walk" : "fly"));
    _exampleInfo[3].setText("[U] Freeze terrain(debug): " + !updateTerrain);
  }
}
