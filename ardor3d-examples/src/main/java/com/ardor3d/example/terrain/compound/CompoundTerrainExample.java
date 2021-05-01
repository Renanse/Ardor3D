/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.example.terrain.compound;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.extension.terrain.client.Terrain;
import com.ardor3d.extension.terrain.client.TerrainBuilder;
import com.ardor3d.extension.terrain.client.TerrainConfiguration;
import com.ardor3d.extension.terrain.client.TerrainSource;
import com.ardor3d.extension.terrain.providers.compound.CompoundTerrainDataProvider;
import com.ardor3d.extension.terrain.providers.compound.CompoundTerrainSource;
import com.ardor3d.extension.terrain.providers.compound.Entry;
import com.ardor3d.extension.terrain.providers.compound.function.ICombineFunction;
import com.ardor3d.extension.terrain.providers.procedural.ProceduralTerrainSource;
import com.ardor3d.extension.terrain.providers.procedural.ProceduralTextureSource;
import com.ardor3d.framework.CanvasRenderer;
import com.ardor3d.input.keyboard.Key;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyPressedCondition;
import com.ardor3d.intersection.PickingUtil;
import com.ardor3d.intersection.PrimitivePickResults;
import com.ardor3d.light.DirectionalLight;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Ray3;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.material.fog.FogParams;
import com.ardor3d.renderer.material.fog.FogParams.DensityFunction;
import com.ardor3d.renderer.state.CullState;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.scenegraph.shape.Box;
import com.ardor3d.scenegraph.shape.Sphere;
import com.ardor3d.util.GameTaskQueue;
import com.ardor3d.util.GameTaskQueueManager;
import com.ardor3d.util.ReadOnlyTimer;

/**
 * Example showing the Geometry Clipmap Terrain system where terrain data is pulled from multiple
 * sources and combined.
 */
@Purpose(
    htmlDescriptionKey = "com.ardor3d.example.terrain.CompoundTerrainExample", //
    thumbnailPath = "com/ardor3d/example/media/thumbnails/terrain_CompoundTerrainExample.jpg", //
    maxHeapMemory = 128)
public class CompoundTerrainExample extends ExampleBase {

  private boolean updateTerrain = true;
  private final float farPlane = 8000.0f;

  private Terrain terrain;

  private final Sphere sphere = new Sphere("sp", 16, 16, 1);
  private final Mesh arrow = new Box("normal", new Vector3(-0.2, -0.2, 0), new Vector3(0.2, 0.2, 4));
  private final Ray3 pickRay = new Ray3();

  private boolean groundCamera = false;
  private Camera terrainCamera;
  private CompoundTerrainSource _terrainSource;

  public static void main(final String[] args) {
    ExampleBase.start(CompoundTerrainExample.class);
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

        final Vector3 intersectionNormal = pickResults.getPickData(0).getIntersectionRecord().getIntersectionNormal(0);
        final Matrix3 rotation = new Matrix3();
        rotation.lookAt(intersectionNormal, Vector3.UNIT_Z);
        arrow.setRotation(rotation);
        arrow.setTranslation(intersectionPoint);
      }
    }
  }

  /**
   * Initialize pssm pass and scene.
   */
  @Override
  protected void initExample() {
    Terrain.addDefaultResourceLocators();

    // Setup main camera.
    _canvas.setTitle("Terrain Example");
    _canvas.getCanvasRenderer().getCamera().setLocation(new Vector3(0, 300, 0));
    _canvas.getCanvasRenderer().getCamera().lookAt(new Vector3(1, 300, 1), Vector3.UNIT_Y);
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

    sphere.getSceneHints().setAllPickingHints(false);
    sphere.getSceneHints().setCullHint(CullHint.Always);
    _root.attachChild(sphere);

    arrow.getSceneHints().setAllPickingHints(false);
    arrow.getSceneHints().setCullHint(CullHint.Always);
    _root.attachChild(arrow);

    setupTerrain();

    _logicalLayer.registerTrigger(
        new InputTrigger(new KeyPressedCondition(Key.U), (source, inputStates, tpf) -> updateTerrain = !updateTerrain));

    _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.SPACE),
        (source, inputStates, tpf) -> groundCamera = !groundCamera));

    _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.P), (source, inputStates, tpf) -> {
      if (sphere.getSceneHints().getCullHint() == CullHint.Dynamic) {
        sphere.getSceneHints().setCullHint(CullHint.Always);
        arrow.getSceneHints().setCullHint(CullHint.Always);
      } else if (sphere.getSceneHints().getCullHint() == CullHint.Always) {
        sphere.getSceneHints().setCullHint(CullHint.Dynamic);
        arrow.getSceneHints().setCullHint(CullHint.Dynamic);
      }
    }));
    _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.R), (source, inputStates, tpf) -> terrain
        .getTextureClipmap().setShowDebug(!terrain.getTextureClipmap().isShowDebug())));

    setupSwingUI();
  }

  private void setupSwingUI() {
    final JFrame frame = new JFrame("Compound Terrain Layers");
    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    frame.getContentPane().add(new CompoundTerrainSourcePanel(_terrainSource));
    frame.setSize(600, 400);
    frame.setVisible(true);
  }

  private void setupTerrain() {
    try {
      // Keep a separate camera to be able to freeze terrain update
      final Camera camera = _canvas.getCanvasRenderer().getCamera();
      terrainCamera = new Camera(camera);

      final CompoundTerrainDataProvider terrainDataProvider =
          new CompoundTerrainDataProvider(new TerrainConfiguration(7, 128, new Vector3(6, 300, 6), 0, 1.2f, true));
      _terrainSource = terrainDataProvider.getTerrainSource();
      addBaseEntry(terrainDataProvider);
      _terrainSource.addEntry(createPillarEntry(100, 100, 50, 1.2, ICombineFunction.MAX));
      _terrainSource.addEntry(createPillarEntry(65, 50, 65, .8, ICombineFunction.MAX));
      _terrainSource.addEntry(createPillarEntry(120, 80, 40, .2, ICombineFunction.MIN));

      terrain = new TerrainBuilder(terrainDataProvider, terrainCamera).withShowDebugPanels(true).build();
      terrain.setRenderMaterial("clipmap/terrain_textured.yaml");

      _root.attachChild(terrain);
    } catch (final Exception ex1) {
      System.out.println("Problem setting up terrain...");
      ex1.printStackTrace();
    }
  }

  private void addBaseEntry(final CompoundTerrainDataProvider terrainDataProvider) {
    final SimpleTerrainFunction function = new SimpleTerrainFunction(1.0 / 4000.0);
    final TerrainSource source = new ProceduralTerrainSource(function, new Vector3(6, 300, 6), -1.2f, 1.2f);
    final ExampleEntry entry = new ExampleEntry(source, null, "Simple Terrain Layer");
    terrainDataProvider.getTerrainSource().addEntry(entry);

    terrainDataProvider.getTextureSources().add(new ProceduralTextureSource(function));
  }

  private ExampleEntry createPillarEntry(final double offX, final double offY, final double radius, final double height,
      final ICombineFunction combine) {
    final TerrainSource source = new ProceduralTerrainSource( //
        new TerrainPillarFunction(offX, offY, radius, height), //
        new Vector3(1, 1, 1), -1.2f, 1.2f);
    return new ExampleEntry(source, combine, "Pillar Function Layer");
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
    final DirectionalLight dLight = new DirectionalLight();
    dLight.setEnabled(true);
    dLight.setColor(new ColorRGBA(0.6f, 0.6f, 0.5f, 1));
    dLight.lookAt(-1, -1, -1);
    _root.setProperty("lightDir", dLight.getWorldDirection());
    _root.attachChild(dLight);
  }

  public static class ExampleEntry extends Entry {

    private final String _name;

    public ExampleEntry(final TerrainSource source, final ICombineFunction combine, final String displayName) {
      super(source, combine);
      _name = displayName;
    }

    public String getName() { return _name; }
  }
}
