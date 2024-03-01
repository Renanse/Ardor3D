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

import java.nio.FloatBuffer;

import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.extension.effect.water.WaterNode;
import com.ardor3d.extension.terrain.client.Terrain;
import com.ardor3d.extension.terrain.client.TerrainBuilder;
import com.ardor3d.extension.terrain.client.TerrainDataProvider;
import com.ardor3d.extension.terrain.providers.procedural.ProceduralTerrainDataProvider;
import com.ardor3d.image.Texture;
import com.ardor3d.input.keyboard.Key;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyPressedCondition;
import com.ardor3d.intersection.PickingUtil;
import com.ardor3d.intersection.PrimitivePickResults;
import com.ardor3d.light.DirectionalLight;
import com.ardor3d.light.LightProperties;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Plane;
import com.ardor3d.math.Ray3;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.functions.FbmFunction3D;
import com.ardor3d.math.functions.Function3D;
import com.ardor3d.math.functions.Functions;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.CullState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.extension.Skybox;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.scenegraph.shape.Quad;
import com.ardor3d.scenegraph.shape.Sphere;
import com.ardor3d.ui.text.BasicText;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureManager;

/**
 * Example showing how to combine the terrain and water systems. Requires GLSL support.
 */
@Purpose(
    htmlDescriptionKey = "com.ardor3d.example.terrain.TerrainWaterExample", //
    thumbnailPath = "com/ardor3d/example/media/thumbnails/terrain_TerrainWaterExample.jpg", //
    maxHeapMemory = 128)
public class TerrainWaterExample extends ExampleBase {

  private final int SIZE = 2048;

  private Terrain terrain;

  private boolean updateTerrain = true;
  private final float farPlane = 3500.0f;
  private final float heightOffset = 3.0f;

  /** The water instance taking care of the water rendering. */
  private WaterNode waterNode;

  private boolean aboveWater = true;

  /** Node containing debug quads for showing waternode render textures. */
  private Node debugQuadsNode;

  /** The quad used as geometry for the water. */
  private Quad waterQuad;

  private boolean groundCamera = false;

  private Camera terrainCamera;

  private Skybox skybox;

  private final Sphere sphere = new Sphere("sp", 16, 16, 1);

  private final double textureScale = 0.05;

  // private FogState fogState;

  private boolean showUI = true;

  private final Ray3 pickRay = new Ray3();

  /** Text fields used to present info about the example. */
  private final BasicText[] _exampleInfo = new BasicText[8];

  /**
   * The main method.
   *
   * @param args
   *          the arguments
   */
  public static void main(final String[] args) {
    start(TerrainWaterExample.class);
  }

  private double counter = 0;
  private int frames = 0;

  /**
   * Update the PassManager, skybox, camera position, etc.
   *
   * @param timer
   *          the application timer
   */
  @Override
  protected void updateExample(final ReadOnlyTimer timer) {
    final Camera camera = _canvas.getCanvasRenderer().getCamera();

    final double height = terrain.getHeightAt(camera.getLocation().getX(), camera.getLocation().getZ()) + heightOffset;
    if (groundCamera || camera.getLocation().getY() < height) {
      camera.setLocation(new Vector3(camera.getLocation().getX(), height, camera.getLocation().getZ()));
    }

    if (aboveWater && camera.getLocation().getY() < waterNode.getWaterHeight()) {
      // fogState.setStart(-1000f);
      // fogState.setEnd(farPlane / 10f);
      // fogState.setColor(new ColorRGBA(0.0f, 0.0f, 0.1f, 1.0f));
      aboveWater = false;
    } else if (!aboveWater && camera.getLocation().getY() >= waterNode.getWaterHeight()) {
      // fogState.setStart(farPlane / 2.0f);
      // fogState.setEnd(farPlane);
      // fogState.setColor(new ColorRGBA(0.96f, 0.97f, 1.0f, 1.0f));
      aboveWater = true;
    }

    if (updateTerrain) {
      terrainCamera.set(camera);
    }

    skybox.setTranslation(camera.getLocation());

    counter += timer.getTimePerFrame();
    frames++;
    if (counter > 1) {
      final double fps = (frames / counter);
      counter = 0;
      frames = 0;
      System.out.printf("%7.1f FPS\n", fps);
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

    final Vector3 transVec =
        new Vector3(camera.getLocation().getX(), waterNode.getWaterHeight(), camera.getLocation().getZ());

    setTextureCoords(0, transVec.getX(), -transVec.getZ(), textureScale);

    // vertex coords
    setVertexCoords(transVec.getX(), transVec.getY(), transVec.getZ());

    waterNode.update(timer.getTimePerFrame());
  }

  /**
   * Render example.
   *
   * @param renderer
   *          the renderer
   */
  @Override
  protected void renderExample(final Renderer renderer) {
    super.renderExample(renderer);

    if (debugQuadsNode == null) {
      createDebugQuads();
      _orthoRoot.attachChild(debugQuadsNode);
    }
  }

  /**
   * Initialize pssm pass and scene.
   */
  @Override
  protected void initExample() {
    Terrain.addDefaultResourceLocators();

    // Setup main camera.
    _canvas.setTitle("Terrain + Water - Example");
    _canvas.getCanvasRenderer().getCamera().setLocation(new Vector3(0, 100, 0));
    _canvas.getCanvasRenderer().getCamera().lookAt(new Vector3(0, 100, 1), Vector3.UNIT_Y);
    _canvas.getCanvasRenderer().getCamera().setFrustumPerspective(65.0,
        (float) _canvas.getCanvasRenderer().getCamera().getWidth()
            / _canvas.getCanvasRenderer().getCamera().getHeight(),
        1.0f, farPlane);

    _controlHandle.setMoveSpeed(50);

    final CullState cs = new CullState();
    cs.setEnabled(true);
    cs.setCullFace(CullState.Face.Back);
    _root.setRenderState(cs);

    // fogState = new FogState();
    // fogState.setStart(farPlane / 2.0f);
    // fogState.setEnd(farPlane);
    // fogState.setColor(new ColorRGBA(0.96f, 0.97f, 1.0f, 1.0f));
    // fogState.setDensityFunction(DensityFunction.Linear);
    // _root.setRenderState(fogState);

    // add our sphere, but have it off for now.
    sphere.getSceneHints().setCullHint(CullHint.Always);
    sphere.getSceneHints().setAllPickingHints(false);
    _root.attachChild(sphere);

    try {
      // Keep a separate camera to be able to freeze terrain update
      final Camera camera = _canvas.getCanvasRenderer().getCamera();
      terrainCamera = new Camera(camera);

      final double scale = 1.0 / 4000.0;
      Function3D functionTmp = new FbmFunction3D(Functions.simplexNoise(), 9, 0.5, 0.5, 3.14);
      functionTmp = Functions.clamp(functionTmp, -1.2, 1.2);
      final Function3D function = Functions.scaleInput(functionTmp, scale, scale, 1);

      final TerrainDataProvider terrainDataProvider =
          new ProceduralTerrainDataProvider(function, new Vector3(1, 200, 1), -1.2f, 1.2f);

      terrain = new TerrainBuilder(terrainDataProvider, terrainCamera).withShowDebugPanels(true).build();
      terrain.setRenderMaterial("clipmap/terrain_textured.yaml");

    } catch (final Exception ex1) {
      ex1.printStackTrace();
    }

    final Node reflectedNode = new Node("reflectNode");
    reflectedNode.attachChild(terrain);
    skybox = buildSkyBox();
    skybox.getSceneHints().setAllPickingHints(false);
    reflectedNode.attachChild(skybox);

    final Camera cam = _canvas.getCanvasRenderer().getCamera();

    // Create a new WaterNode with refraction enabled.
    waterNode = new WaterNode(cam, 2, false, true);
    // Setup textures to use for the water.
    waterNode.setNormalMapTextureString("images/water/normalmap3.dds");
    waterNode.setDudvMapTextureString("images/water/dudvmap.png");
    waterNode.useFadeToFogColor(true);

    waterNode.setSpeedReflection(0.02);
    waterNode.setSpeedReflection(-0.01);

    // setting to default value just to show
    waterNode.setWaterPlane(new Plane(new Vector3(0.0, 1.0, 0.0), 40.0));

    // Create a quad to use as geometry for the water.
    waterQuad = new Quad("waterQuad", 1, 1);
    // Hack the quad normals to point up in the y-axis. Since we are manipulating the vertices as
    // we move this is more convenient than rotating the quad.
    final FloatBuffer normBuf = waterQuad.getMeshData().getNormalBuffer();
    normBuf.clear();
    normBuf.put(0).put(1).put(0);
    normBuf.put(0).put(1).put(0);
    normBuf.put(0).put(1).put(0);
    normBuf.put(0).put(1).put(0);
    waterNode.attachChild(waterQuad);

    waterNode.addReflectedScene(reflectedNode);
    waterNode.setSkybox(skybox);

    _root.attachChild(reflectedNode);
    _root.attachChild(waterNode);

    // Setup cam above water and terrain
    final Camera camera = _canvas.getCanvasRenderer().getCamera();
    final double height = Math.max(terrain.getHeightAt(camera.getLocation().getX(), camera.getLocation().getZ()),
        waterNode.getWaterHeight()) + heightOffset;
    if (camera.getLocation().getY() < height) {
      camera.setLocation(new Vector3(camera.getLocation().getX(), height, camera.getLocation().getZ()));
    }

    // Setup labels for presenting example info.
    final Node textNodes = new Node("Text");
    _orthoRoot.attachChild(textNodes);
    LightProperties.setLightReceiver(textNodes, false);

    final double infoStartY = _canvas.getCanvasRenderer().getCamera().getHeight() - 20;
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

    _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.SPACE), (source, inputStates, tpf) -> {
      groundCamera = !groundCamera;
      updateText();
    }));

    _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.P), (source, inputStates, tpf) -> {
      sphere.getSceneHints()
          .setCullHint(sphere.getSceneHints().getCullHint() == CullHint.Always ? CullHint.Dynamic : CullHint.Always);
      updateText();
    }));
    _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.V), (source, inputStates, tpf) -> {
      showUI = !showUI;
      if (showUI) {
        textNodes.getSceneHints().setCullHint(CullHint.Never);
        debugQuadsNode.getSceneHints().setCullHint(CullHint.Never);
      } else {
        textNodes.getSceneHints().setCullHint(CullHint.Always);
        debugQuadsNode.getSceneHints().setCullHint(CullHint.Always);
      }
      updateText();
    }));
    _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.B), (source, inputStates, tpf) -> {
      waterNode.setDoBlurReflection(!waterNode.isDoBlurReflection());
      updateText();
    }));

    _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.F), (source, inputStates, tpf) -> {
      final Camera camera1 = _canvas.getCanvasRenderer().getCamera();
      camera1.setLocation(
          new Vector3(camera1.getLocation().getX() + 5000, camera1.getLocation().getY(), camera1.getLocation().getZ()));

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

  /**
   * Sets the vertex coords of the quad.
   *
   * @param x
   *          the x
   * @param y
   *          the y
   * @param z
   *          the z
   */
  private void setVertexCoords(final double x, final double y, final double z) {
    final FloatBuffer vertBuf = waterQuad.getMeshData().getVertexBuffer();
    vertBuf.clear();

    vertBuf.put((float) (x - farPlane)).put((float) y).put((float) (z - farPlane));
    vertBuf.put((float) (x - farPlane)).put((float) y).put((float) (z + farPlane));
    vertBuf.put((float) (x + farPlane)).put((float) y).put((float) (z + farPlane));
    vertBuf.put((float) (x + farPlane)).put((float) y).put((float) (z - farPlane));
  }

  /**
   * Sets the texture coords of the quad.
   *
   * @param buffer
   *          the buffer
   * @param x
   *          the x
   * @param y
   *          the y
   * @param textureScale
   *          the texture scale
   */
  private void setTextureCoords(final int buffer, double x, double y, double textureScale) {
    x *= textureScale * 0.5f;
    y *= textureScale * 0.5f;
    textureScale = farPlane * textureScale;
    FloatBuffer texBuf;
    texBuf = waterQuad.getMeshData().getTextureBuffer(buffer);
    texBuf.clear();
    texBuf.put((float) x).put((float) (textureScale + y));
    texBuf.put((float) x).put((float) y);
    texBuf.put((float) (textureScale + x)).put((float) y);
    texBuf.put((float) (textureScale + x)).put((float) (textureScale + y));
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

    return skybox;
  }

  /**
   * Update text information.
   */
  private void updateText() {
    _exampleInfo[0].setText("Heightmap size: " + SIZE + "x" + SIZE);
    _exampleInfo[1].setText("Spec: One meter per heightmap value");
    _exampleInfo[2].setText("[1/2/3] Moving speed: " + _controlHandle.getMoveSpeed() + " m/s");
    _exampleInfo[3].setText("[U] Update terrain: " + updateTerrain);
    _exampleInfo[4].setText("[SPACE] Toggle fly/walk: " + (groundCamera ? "walk" : "fly"));
    _exampleInfo[5].setText("[P] Toggle showing a sphere that follows the ground using picking: "
        + (sphere.getSceneHints().getCullHint() != CullHint.Always));
    _exampleInfo[6].setText("[B] Blur reflection: " + waterNode.isDoBlurReflection());
    _exampleInfo[7].setText("[V] Show/Hide UI");
  }

  /**
   * Creates the debug quads.
   */
  private void createDebugQuads() {
    debugQuadsNode = new Node("quadNode");
    debugQuadsNode.getSceneHints().setCullHint(CullHint.Never);

    final double quadSize = _canvas.getCanvasRenderer().getCamera().getWidth() / 10;

    Quad debugQuad = new Quad("reflectionQuad", quadSize, quadSize);
    LightProperties.setLightReceiver(debugQuad, false);
    debugQuad.getSceneHints().setRenderBucketType(RenderBucketType.OrthoOrder);
    debugQuad.getSceneHints().setCullHint(CullHint.Never);
    TextureState ts = new TextureState();
    ts.setTexture(waterNode.getTextureReflect());
    debugQuad.setRenderState(ts);
    debugQuad.setTranslation(quadSize * 0.6, quadSize * 1.0, 1.0);
    debugQuadsNode.attachChild(debugQuad);

    if (waterNode.getTextureRefract() != null) {
      debugQuad = new Quad("refractionQuad", quadSize, quadSize);
      LightProperties.setLightReceiver(debugQuad, false);
      debugQuad.getSceneHints().setRenderBucketType(RenderBucketType.OrthoOrder);
      debugQuad.getSceneHints().setCullHint(CullHint.Never);
      ts = new TextureState();
      ts.setTexture(waterNode.getTextureRefract());
      debugQuad.setRenderState(ts);
      debugQuad.setTranslation(quadSize * 0.6, quadSize * 2.1, 1.0);
      debugQuadsNode.attachChild(debugQuad);
    }
  }

}
