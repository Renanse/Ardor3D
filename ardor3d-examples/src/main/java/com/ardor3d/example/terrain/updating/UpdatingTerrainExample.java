/**
 * Copyright (c) 2008-2022 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.example.terrain.updating;

import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.extension.terrain.client.Terrain;
import com.ardor3d.extension.terrain.client.TerrainBuilder;
import com.ardor3d.extension.terrain.client.TerrainDataProvider;
import com.ardor3d.extension.terrain.providers.inmemory.InMemoryTerrainDataProvider;
import com.ardor3d.framework.CanvasRenderer;
import com.ardor3d.image.Texture;
import com.ardor3d.input.keyboard.Key;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyPressedCondition;
import com.ardor3d.light.DirectionalLight;
import com.ardor3d.light.LightProperties;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.functions.FbmFunction3D;
import com.ardor3d.math.functions.Functions;
import com.ardor3d.math.util.MathUtils;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.CullState;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.extension.Skybox;
import com.ardor3d.ui.text.BasicText;
import com.ardor3d.util.GameTaskQueue;
import com.ardor3d.util.GameTaskQueueManager;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureManager;

/**
 * Example showing the Geometry Clipmap Terrain system with 'MegaTextures' streaming from an
 * in-memory data source and updating over time.
 */
@Purpose(
    htmlDescriptionKey = "com.ardor3d.example.terrain.updating.UpdatingTerrainExample", //
    thumbnailPath = "com/ardor3d/example/media/thumbnails/terrain_UpdatingTerrainExample.jpg", //
    maxHeapMemory = 128)
public class UpdatingTerrainExample extends ExampleBase {

  private boolean updateTerrain = true;
  private final float farPlane = 8000.0f;

  private Terrain terrain;

  private boolean groundCamera = false;
  private Camera terrainCamera;
  private Skybox skybox;

  private UpdatingTerrainData terrainData;

  /** Text fields used to present info about the example. */
  private final BasicText[] _exampleInfo = new BasicText[6];

  public static void main(final String[] args) {
    ExampleBase.start(UpdatingTerrainExample.class);
  }

  @Override
  protected void updateExample(final ReadOnlyTimer timer) {
    doTerrainDataUpdates(timer);

    final Camera camera = _canvas.getCanvasRenderer().getCamera();

    // Make sure camera is above terrain
    final double height = terrain.getHeightAt(camera.getLocation().getX(), camera.getLocation().getZ());
    if (height > -Float.MAX_VALUE && (groundCamera || camera.getLocation().getY() < height + 3)) {
      camera.setLocation(new Vector3(camera.getLocation().getX(), height + 3, camera.getLocation().getZ()));
    }

    terrainCamera.set(camera);

    skybox.setTranslation(camera.getLocation());
  }

  private double elapsed = 0.0;
  private static final double ThottleTime = 5.0;

  private void doTerrainDataUpdates(final ReadOnlyTimer timer) {
    if (!updateTerrain) {
      return;
    }

    // check throttle
    elapsed += timer.getTimePerFrame();
    if (elapsed < ThottleTime) {
      return;
    }

    // reset throttle timer
    elapsed %= ThottleTime;

    final double maxSize = 800, minSize = 300;
    final double minX = minSize / 2, maxX = 2048 - minX;
    final double minZ = minSize / 2, maxZ = 2048 - minZ;
    // Pick a square region to update
    final var size = MathUtils.nextRandomDouble() * (maxSize - minSize) + minSize;
    final var center = new Vector3(MathUtils.nextRandomDouble() * (maxX - minX) + minX, 0,
        MathUtils.nextRandomDouble() * (maxZ - minZ) + minZ);

    // update a section of the terrain
    System.out.printf("Update a section of the terrain... at %s with side of %,.2f\n", center, size);
    terrainData.updateRegion(center, size);
  }

  /**
   * Initialize pssm pass and scene.
   */
  @Override
  protected void initExample() {
    Terrain.addDefaultResourceLocators();

    // Setup main camera.
    _canvas.setTitle("Terrain Example");
    _canvas.getCanvasRenderer().getCamera().setLocation(new Vector3(-256, 900, 1024));
    _canvas.getCanvasRenderer().getCamera().lookAt(new Vector3(1024, 200, 1024), Vector3.UNIT_Y);
    _canvas.getCanvasRenderer().getCamera().setFrustumPerspective(70.0,
        (float) _canvas.getCanvasRenderer().getCamera().getWidth()
            / _canvas.getCanvasRenderer().getCamera().getHeight(),
        1.0f, farPlane);
    final CanvasRenderer canvasRenderer = _canvas.getCanvasRenderer();
    final RenderContext renderContext = canvasRenderer.getRenderContext();
    final Renderer renderer = canvasRenderer.getRenderer();
    GameTaskQueueManager.getManager(renderContext).getQueue(GameTaskQueue.RENDER).enqueue(() -> {
      renderer.setBackgroundColor(ColorRGBA.GRAY);
      return null;
    });
    _controlHandle.setMoveSpeed(1000);

    setupDefaultStates();

    try {
      // Keep a separate camera to be able to freeze terrain update
      final Camera camera = _canvas.getCanvasRenderer().getCamera();
      terrainCamera = new Camera(camera);

      terrainData = new UpdatingTerrainData(2048, 9, 128, new Vector3(3, 100, 3),
          new FbmFunction3D(Functions.simplexNoise(), 7, 0.5, 0.5, 3.14));

      final TerrainDataProvider terrainDataProvider = new InMemoryTerrainDataProvider(terrainData, false);

      terrain = new TerrainBuilder(terrainDataProvider, terrainCamera).withShowDebugPanels(true).build();
      terrain.setRenderMaterial("clipmap/terrain_textured.yaml");
      terrain.addListener((regions, worldTransform) -> {
        for (final var region : regions) {
          System.err.println("detected changed region: " + region);
        }
      });

      _root.attachChild(terrain);
    } catch (final Exception ex1) {
      System.out.println("Problem setting up terrain...");
      ex1.printStackTrace();
    }

    skybox = buildSkyBox();
    skybox.getSceneHints().setAllPickingHints(false);
    _root.attachChild(skybox);

    // Setup labels for presenting example info.
    final Node textNodes = new Node("Text");
    _orthoRoot.attachChild(textNodes);
    textNodes.getSceneHints().setRenderBucketType(RenderBucketType.OrthoOrder);
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

    _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.R), (source, inputStates, tpf) -> {
      terrain.getTextureClipmap().setShowDebug(!terrain.getTextureClipmap().isShowDebug());
      updateText();
    }));
  }

  @Override
  protected void setupLight() {
    final DirectionalLight dLight = new DirectionalLight();
    dLight.setEnabled(true);
    dLight.setColor(new ColorRGBA(0.6f, 0.6f, 0.5f, 1));
    dLight.lookAt(-1, -1, -1);
    _root.attachChild(dLight);
  }

  private void setupDefaultStates() {
    final CullState cs = new CullState();
    cs.setEnabled(true);
    cs.setCullFace(CullState.Face.Back);
    _root.setRenderState(cs);
  }

  /**
   * Builds the sky box.
   */
  private Skybox buildSkyBox() {
    final Skybox skybox = new Skybox("skybox", 10, 10, 10);

    final String dir = "images/skybox/";
    final Texture north = TextureManager.load(dir + "1.jpg", Texture.MinificationFilter.BilinearNearestMipMap, true);
    final Texture south = TextureManager.load(dir + "3.jpg", Texture.MinificationFilter.BilinearNearestMipMap, true);
    final Texture east = TextureManager.load(dir + "2.jpg", Texture.MinificationFilter.BilinearNearestMipMap, true);
    final Texture west = TextureManager.load(dir + "4.jpg", Texture.MinificationFilter.BilinearNearestMipMap, true);
    final Texture up = TextureManager.load(dir + "6.jpg", Texture.MinificationFilter.BilinearNearestMipMap, true);
    final Texture down = TextureManager.load(dir + "5.jpg", Texture.MinificationFilter.BilinearNearestMipMap, true);

    skybox.setTexture(Skybox.Face.North, north);
    skybox.setTexture(Skybox.Face.West, west);
    skybox.setTexture(Skybox.Face.South, south);
    skybox.setTexture(Skybox.Face.East, east);
    skybox.setTexture(Skybox.Face.Up, up);
    skybox.setTexture(Skybox.Face.Down, down);
    skybox.setRenderMaterial("unlit/textured/basic.yaml");

    return skybox;
  }

  /**
   * Update text information.
   */
  private void updateText() {
    _exampleInfo[0].setText("[1/2/3/4] Moving speed: " + _controlHandle.getMoveSpeed() * 3.6 + " km/h");
    _exampleInfo[2].setText("[SPACE] Toggle fly/walk: " + (groundCamera ? "walk" : "fly"));
    _exampleInfo[4].setText("[R] Debug terrain: " + terrain.getTextureClipmap().isShowDebug());
    _exampleInfo[4].setText("[U] Updating terrain: " + !updateTerrain);
  }
}
