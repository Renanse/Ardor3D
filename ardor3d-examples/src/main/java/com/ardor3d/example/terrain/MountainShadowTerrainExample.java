/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.example.terrain;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.EnumSet;

import javax.imageio.ImageIO;

import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.extension.model.collada.jdom.ColladaImporter;
import com.ardor3d.extension.model.collada.jdom.data.ColladaStorage;
import com.ardor3d.extension.terrain.client.Terrain;
import com.ardor3d.extension.terrain.client.TerrainBuilder;
import com.ardor3d.extension.terrain.heightmap.ImageHeightMap;
import com.ardor3d.extension.terrain.providers.array.ArrayTerrainDataProvider;
import com.ardor3d.extension.ui.Orientation;
import com.ardor3d.extension.ui.UIButton;
import com.ardor3d.extension.ui.UIFrame;
import com.ardor3d.extension.ui.UIFrame.FrameButtons;
import com.ardor3d.extension.ui.UIHud;
import com.ardor3d.extension.ui.UILabel;
import com.ardor3d.extension.ui.UIPanel;
import com.ardor3d.extension.ui.UISlider;
import com.ardor3d.extension.ui.layout.RowLayout;
import com.ardor3d.extension.ui.text.StyleConstants;
import com.ardor3d.extension.ui.util.Insets;
import com.ardor3d.image.Image;
import com.ardor3d.image.util.awt.AWTImageLoader;
import com.ardor3d.input.keyboard.Key;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyPressedCondition;
import com.ardor3d.light.DirectionalLight;
import com.ardor3d.light.Light;
import com.ardor3d.light.LightProperties;
import com.ardor3d.light.shadow.DirectionalShadowData;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.util.MathUtils;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.material.fog.FogParams;
import com.ardor3d.renderer.material.fog.FogParams.DensityFunction;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.CullState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.scenegraph.hint.SceneHints;
import com.ardor3d.scenegraph.hint.TextureCombineMode;
import com.ardor3d.scenegraph.shape.Quad;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.geom.Debugger;
import com.ardor3d.util.resource.ResourceLocatorTool;

/**
 * Example showing the Geometry Clipmap Terrain system with 'MegaTextures' where the terrain data is
 * provided from a float array populated from a heightmap generated from an Image. Requires GLSL
 * support.
 */
@Purpose(
    htmlDescriptionKey = "com.ardor3d.example.terrain.ImageMapTerrainExample", //
    thumbnailPath = "com/ardor3d/example/media/thumbnails/terrain_ImageMapTerrainExample.jpg", //
    maxHeapMemory = 128)
public class MountainShadowTerrainExample extends ExampleBase {

  private final float farPlane = 8000.0f;

  private Terrain terrain;

  private boolean groundCamera = false;
  private Camera terrainCamera;

  /** Text fields used to present info about the example. */
  private final UILabel[] _exampleInfo = new UILabel[2];

  private DirectionalLight light;

  private final static int SPLITS = 6;
  private final static int QUAD_SIZE = 128;
  private final Quad[] _orthoQuad = new Quad[SPLITS];

  private double lightTime;
  private boolean moveLight = false;

  private UIHud hud;

  public static void main(final String[] args) {
    ExampleBase._minDepthBits = 24;
    ExampleBase.start(MountainShadowTerrainExample.class);
  }

  private double counter = 0;
  private int frames = 0;

  @Override
  protected void updateExample(final ReadOnlyTimer timer) {
    counter += timer.getTimePerFrame();
    frames++;
    if (counter > 1) {
      final double fps = frames / counter;
      counter = 0;
      frames = 0;
      System.out.printf("%7.1f FPS\n", fps);
    }

    final Camera camera = _canvas.getCanvasRenderer().getCamera();

    // Make sure camera is above terrain
    final double height = terrain.getHeightAt(camera.getLocation().getX(), camera.getLocation().getZ());
    if (height > -Float.MAX_VALUE && (groundCamera || camera.getLocation().getY() < height + 3)) {
      camera.setLocation(new Vector3(camera.getLocation().getX(), height + 3, camera.getLocation().getZ()));
      terrainCamera.set(camera);
    } else {
      terrainCamera.set(_canvas.getCanvasRenderer().getCamera());
    }

    // move terrain to view pos
    hud.updateGeometricState(timer.getTimePerFrame());

    if (moveLight) {
      lightTime += timer.getTimePerFrame();
      light.setTranslation(new Vector3(Math.sin(lightTime), .8, Math.cos(lightTime)).normalizeLocal());
      light.lookAt(0, 0, 0);
    }
  }

  /**
   * Initialize pssm pass and scene.
   */
  @Override
  protected void initExample() {
    Terrain.addDefaultResourceLocators();

    _canvas.setTitle("Mountain Shadow Example");
    _canvas.getCanvasRenderer().getRenderer().setBackgroundColor(ColorRGBA.BLUE);

    final Camera cam = _canvas.getCanvasRenderer().getCamera();
    cam.setLocation(new Vector3(2176, 790, 688));
    cam.lookAt(new Vector3(cam.getLocation()).addLocal(-0.87105768019686, -0.4349655341112313, 0.22817427967541867),
        Vector3.UNIT_Y);
    cam.setFrustumPerspective(45.0, (float) _canvas.getCanvasRenderer().getCamera().getWidth()
        / _canvas.getCanvasRenderer().getCamera().getHeight(), 1.0f, farPlane);
    _controlHandle.setMoveSpeed(400);

    setupDefaultStates();
    addRover();
    addUI();

    final Node terrainNode = new Node("terrain");
    _root.attachChild(terrainNode);
    // TODO: backside lock test
    final Quad floor = new Quad("floor", 2048, 2048);
    floor.updateModelBound();
    floor.setRotation(new Quaternion().fromAngleAxis(MathUtils.HALF_PI, Vector3.UNIT_X));
    floor.setTranslation(1024, 0, 1024);
    terrainNode.attachChild(floor);

    try {
      // Keep a separate camera to be able to freeze terrain update
      final Camera camera = _canvas.getCanvasRenderer().getCamera();
      terrainCamera = new Camera(camera);

      // IMAGE LOADING AND CONVERSION TO HEIGHTMAP DONE HERE
      final BufferedImage heightmap = ImageIO.read(ResourceLocatorTool
          .getClassPathResource(MountainShadowTerrainExample.class, "com/ardor3d/example/media/images/heightmap.jpg"));
      final Image ardorImage = AWTImageLoader.makeArdor3dImage(heightmap, false);
      final float[] heightMap = ImageHeightMap.generateHeightMap(ardorImage, 0.05f, .33f);
      // END OF IMAGE CONVERSION

      final int SIZE = ardorImage.getWidth();

      final ArrayTerrainDataProvider terrainDataProvider =
          new ArrayTerrainDataProvider(heightMap, SIZE, new Vector3(5, 2048, 5), true);
      terrainDataProvider.setHeightMax(0.34f);

      terrain = new TerrainBuilder(terrainDataProvider, terrainCamera).build();
      LightProperties.setShadowCaster(terrain, true);
      terrain.setRenderMaterial("clipmap/terrain_textured_normal_map.yaml");

      terrainNode.attachChild(terrain);

      terrain.setCullingEnabled(false);
    } catch (final Exception ex1) {
      System.out.println("Problem setting up terrain...");
      ex1.printStackTrace();
    }

    final double infoStartY = _canvas.getCanvasRenderer().getCamera().getHeight() / 2;
    for (int i = 0; i < _exampleInfo.length; i++) {
      _exampleInfo[i] = new UILabel("Text");
      _exampleInfo[i].setForegroundColor(ColorRGBA.WHITE, true);
      _exampleInfo[i].addFontStyle(StyleConstants.KEY_SIZE, 16);
      _exampleInfo[i].addFontStyle(StyleConstants.KEY_BOLD, Boolean.TRUE);
      _exampleInfo[i].setTranslation(new Vector3(10, infoStartY - i * 20, 0));
      hud.add(_exampleInfo[i]);
    }

    updateText();

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
  }

  private void addRover() {
    try {
      final ColladaStorage storage = new ColladaImporter().load("collada/sketchup/NASA Mars Rover.dae");
      final Node rover = storage.getScene();
      rover.setTranslation(440, 102, 160.1);
      rover.setScale(3);
      rover.setRotation(new Quaternion().fromAngleAxis(-MathUtils.HALF_PI, Vector3.UNIT_X));
      _root.attachChild(rover);
    } catch (final IOException ex) {
      ex.printStackTrace();
    }

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
    light = new DirectionalLight();
    light.setEnabled(true);
    light.setColor(ColorRGBA.WHITE);
    light.setIntensity(1.0f);
    light.setTranslation(1, 1, 1);
    light.lookAt(0, 0, 0);
    _root.attachChild(light);

    // shadow params - have to play around a bit to get it looking nice for your scene
    light.setShadowCaster(true);
    light.getShadowData().setBias(0.01f);
    light.getShadowData().setCascades(SPLITS);
    light.getShadowData().setTextureSize(2048);
    light.getShadowData().setTextureDepthBits(32);
    light.getShadowData().setMinimumCameraDistance(200);
    light.getShadowData().setMaxDistance(farPlane);

    for (int i = 0; i < SPLITS; i++) {
      _orthoQuad[i] = makeDebugQuad(light, i);
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
    _exampleInfo[0].setText("[1/2/3/4] Moving speed: " + _controlHandle.getMoveSpeed() * 3.6 + "km/h");
    _exampleInfo[1].setText("[SPACE] Toggle fly/walk: " + (groundCamera ? "walk" : "fly"));
  }

  @Override
  protected void updateLogicalLayer(final ReadOnlyTimer timer) {
    hud.getLogicalLayer().checkTriggers(timer.getTimePerFrame());
  }

  @Override
  protected void renderDebug(final Renderer renderer) {
    super.renderDebug(renderer);
    if (_showBounds) {
      Debugger.drawBounds(terrain, renderer, true);
    }
  }

  private void addUI() {
    // setup hud
    hud = new UIHud(_canvas);
    hud.setupInput(_physicalLayer, _logicalLayer);
    hud.setMouseManager(_mouseManager);

    final UIFrame frame = new UIFrame("Controls", EnumSet.noneOf(FrameButtons.class));
    frame.setResizeable(false);

    final int max = ((int) farPlane);
    final UILabel distLabel = new UILabel("Max Shadow Distance: " + max);
    final UISlider distSlider = new UISlider(Orientation.Horizontal, 0, max, max);
    distSlider.addActionListener(event -> {
      light.getShadowData().setMaxDistance(distSlider.getValue());
      distLabel.setText("Max Shadow Distance: " + distSlider.getValue());
    });

    final UILabel mindistLabel = new UILabel("Min Shadow Distance: " + 200);
    final UISlider mindistSlider = new UISlider(Orientation.Horizontal, 1, 2000, 200);
    mindistSlider.addActionListener(event -> {
      light.getShadowData().setMinimumCameraDistance(mindistSlider.getValue());
      mindistLabel.setText("Min Shadow Distance: " + mindistSlider.getValue());
    });

    final UIButton rotateLight = new UIButton("Rotate Light");
    rotateLight.setSelectable(true);
    rotateLight.setSelected(false);
    rotateLight.addActionListener(event -> {
      moveLight = rotateLight.isSelected();
      updateText();
    });

    final UIPanel panel = new UIPanel(new RowLayout(false, true, false));
    panel.setPadding(new Insets(10, 20, 10, 20));
    panel.add(distLabel);
    panel.add(distSlider);
    panel.add(mindistLabel);
    panel.add(mindistSlider);
    panel.add(rotateLight);

    frame.setContentPanel(panel);
    frame.pack();
    frame.setLocalXY(hud.getWidth() - frame.getLocalComponentWidth(),
        hud.getHeight() - frame.getLocalComponentHeight());
    hud.add(frame);

    _orthoRoot.attachChild(hud);
  }
}
