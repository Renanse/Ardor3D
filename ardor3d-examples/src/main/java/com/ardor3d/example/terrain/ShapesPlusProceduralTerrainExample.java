/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.example.terrain;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.extension.terrain.client.Terrain;
import com.ardor3d.extension.terrain.client.TerrainBuilder;
import com.ardor3d.extension.terrain.client.TerrainDataProvider;
import com.ardor3d.extension.terrain.client.TextureClipmap;
import com.ardor3d.extension.terrain.providers.awt.AbstractAwtElement;
import com.ardor3d.extension.terrain.providers.awt.AwtImageElement;
import com.ardor3d.extension.terrain.providers.awt.AwtShapeElement;
import com.ardor3d.extension.terrain.providers.awt.AwtTextureSource;
import com.ardor3d.extension.terrain.providers.procedural.ProceduralTerrainDataProvider;
import com.ardor3d.framework.CanvasRenderer;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.input.keyboard.Key;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyPressedCondition;
import com.ardor3d.light.DirectionalLight;
import com.ardor3d.light.LightProperties;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Transform;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.functions.FbmFunction3D;
import com.ardor3d.math.functions.Function3D;
import com.ardor3d.math.functions.Functions;
import com.ardor3d.math.util.MathUtils;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.CullState;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.shape.Box;
import com.ardor3d.ui.text.BasicText;
import com.ardor3d.util.GameTaskQueue;
import com.ardor3d.util.GameTaskQueueManager;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.resource.ResourceLocatorTool;

/**
 * Example showing the Geometry Clipmap Terrain system with 'MegaTextures'. We merge AWT drawing
 * with the terrain texture in real time. Requires GLSL support.
 */
@Purpose(
    htmlDescriptionKey = "com.ardor3d.example.terrain.ShapesPlusProceduralTerrainExample", //
    thumbnailPath = "com/ardor3d/example/media/thumbnails/terrain_ShapesPlusProceduralTerrainExample.jpg", //
    maxHeapMemory = 128)
public class ShapesPlusProceduralTerrainExample extends ExampleBase {

  private boolean updateTerrain = true;
  private final float farPlane = 8000.0f;

  private final float heightScale = 200;

  private Terrain terrain;

  private boolean groundCamera = false;
  private Camera terrainCamera;
  Transform ovalTrans = new Transform();

  /** Text fields used to present info about the example. */
  private final BasicText _exampleInfo[] = new BasicText[5];

  public static void main(final String[] args) {
    ExampleBase.start(ShapesPlusProceduralTerrainExample.class);
  }

  private double counter = 0;
  private int frames = 0;

  private final double awtUpdate = 1.0 / 15.0; // 15 fps
  private double awtCounter = 0;

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
    awtCounter += timer.getTimePerFrame();
    if (awtCounter > awtUpdate) {
      final double tis = timer.getTimeInSeconds() * 0.75;
      ovalTrans.setTranslation(250 * Math.sin(-tis), 150 * Math.cos(tis), 0);
      oval.setTransform(ovalTrans);
      awtCounter -= awtUpdate;
    }

    // Make sure camera is above terrain
    final double height = terrain.getHeightAt(camera.getLocation().getX(), camera.getLocation().getZ());
    if (height > -Float.MAX_VALUE && (groundCamera || camera.getLocation().getY() < height + 3)) {
      camera.setLocation(new Vector3(camera.getLocation().getX(), height + 3, camera.getLocation().getZ()));
    }

    if (updateTerrain) {
      terrainCamera.set(camera);
    }
  }

  private AwtTextureSource awtTextureSource;
  private AwtShapeElement rectangle;
  private AwtShapeElement oval;
  private AwtImageElement bubble;

  /**
   * Initialize pssm pass and scene.
   */
  @Override
  protected void initExample() {
    Terrain.addDefaultResourceLocators();

    for (int i = 0; i < 9; i++) {
      final double x = (i % 3 - 1) * 128.0;
      final double z = (i / 3 - 1) * 128.0;
      final Box b = new Box("" + i, new Vector3(x, 0, z), 5, 150, 5);
      _root.attachChild(b);
    }
    _root.setRenderMaterial("unlit/untextured/basic.yaml");

    // Setup main camera.
    _canvas.setTitle("Shapes + Procedural Terrain Example");
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
    _controlHandle.setMoveSpeed(50);

    setupDefaultStates();

    try {
      // Keep a separate camera to be able to freeze terrain update
      final Camera camera = _canvas.getCanvasRenderer().getCamera();
      terrainCamera = new Camera(camera);

      awtTextureSource = new AwtTextureSource(8, TextureStoreFormat.RGBA8); // Same as procedural one

      final double scale = 1.0 / 4000.0;
      Function3D functionTmp = new FbmFunction3D(Functions.simplexNoise(), 9, 0.5, 0.5, 3.14);
      functionTmp = Functions.clamp(functionTmp, -1.2, 1.2);
      final Function3D function = Functions.scaleInput(functionTmp, scale, scale, 1);

      final TerrainDataProvider baseTerrainDataProvider =
          new ProceduralTerrainDataProvider(function, new Vector3(1, heightScale, 1), -1.2f, 1.2f);

      final TerrainBuilder terrainBuilder = new TerrainBuilder(baseTerrainDataProvider, terrainCamera);
      terrainBuilder.addTextureConnection(awtTextureSource);
      terrain = terrainBuilder.withShowDebugPanels(true).build();
      terrain.setRenderMaterial("clipmap/terrain_textured.yaml");

      _root.attachChild(terrain);
    } catch (final Exception ex1) {
      System.out.println("Problem setting up terrain...");
      ex1.printStackTrace();
    }

    // add some shapes
    rectangle = new AwtShapeElement(new Rectangle(400, 50));
    Transform t = new Transform();
    t.setRotation(new Matrix3().fromAngles(0, 0, 45 * MathUtils.DEG_TO_RAD));
    rectangle.setTransform(t);
    awtTextureSource.getProvider().addElement(rectangle);

    oval = new AwtShapeElement(new Ellipse2D.Float(0, 0, 250, 150));
    oval.setFillColor(Color.red);
    // set transparency
    oval.setCompositeOverride(AbstractAwtElement.makeAlphaComposite(.75f));
    awtTextureSource.getProvider().addElement(oval);

    // add an image element to test.
    t = new Transform();
    t.setScale(2.0);
    t.translate(-250, 150, 0);
    addBubble(t);

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
      _controlHandle.setMoveSpeed(200);
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

    _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.H), (source, inputStates, tpf) -> {
      final TextureClipmap cm = terrain.findTextureClipmap(awtTextureSource);
      cm.setEnabled(!cm.isEnabled());
    }));

    _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.J), (source, inputStates, tpf) -> {
      final TextureClipmap cm = terrain.getTextureClipmap();
      cm.setEnabled(!cm.isEnabled());
    }));

    _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.R), (source, inputStates, tpf) -> {
      terrain.getTextureClipmap().setShowDebug(!terrain.getTextureClipmap().isShowDebug());
      updateText();
    }));
    _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.FIVE), (source, inputStates, tpf) -> {
      for (final TextureClipmap clipmap : terrain.getTextureClipmaps()) {
        clipmap.setPixelDensity(terrain.getTextureClipmap().getPixelDensity() / 2);
      }
      updateText();
    }));
    _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.SIX), (source, inputStates, tpf) -> {
      for (final TextureClipmap clipmap : terrain.getTextureClipmaps()) {
        clipmap.setPixelDensity(terrain.getTextureClipmap().getPixelDensity() * 2);
      }
      updateText();
    }));

    _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.SEVEN), (source, inputStates, tpf) -> {
      final Camera camera = _canvas.getCanvasRenderer().getCamera();
      camera.setLocation(-5000, 500, -5000);
    }));

    _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.EIGHT), (source, inputStates, tpf) -> {
      final Camera camera = _canvas.getCanvasRenderer().getCamera();
      if (inputStates.getCurrent().getKeyboardState().isDown(Key.LEFT_SHIFT)) {
        camera.setLocation(0, 500, 0);
      } else {
        camera.setLocation(5000, 500, 5000);
      }
    }));
    _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.NINE), (source, inputStates, tpf) -> {
      final Camera camera = _canvas.getCanvasRenderer().getCamera();
      camera.setLocation(camera.getLocation().getX(), camera.getLocation().getY(),
          camera.getLocation().getZ() + 1500.0);
    }));

    _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.V), (source, inputStates, tpf) -> {
      final Transform t1 = new Transform();
      t1.setScale(MathUtils.nextRandomDouble() * 4.9 + 0.1);
      t1.translate(MathUtils.nextRandomDouble() * 500 - 250, MathUtils.nextRandomDouble() * 500 - 250, 0);
      addBubble(t1);
    }));

    _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.ZERO), (source, inputStates, tpf) -> {
      final Camera camera = _canvas.getCanvasRenderer().getCamera();
      camera.setLocation(camera.getLocation().getX(), camera.getLocation().getY(),
          camera.getLocation().getZ() - 1500.0);
    }));
  }

  protected void addBubble(final Transform t) {
    java.awt.Image bubbleImg = null;
    try {
      bubbleImg =
          ImageIO.read(ResourceLocatorTool.getClassPathResourceAsStream(ShapesPlusProceduralTerrainExample.class,
              "com/ardor3d/example/media/images/ball.png"));
    } catch (final IOException e) {
      e.printStackTrace();
      System.exit(-1);
    }
    bubble = new AwtImageElement(bubbleImg);
    bubble.setTransform(t);
    awtTextureSource.getProvider().addElement(bubble);
  }

  private void setupDefaultStates() {
    final CullState cs = new CullState();
    cs.setEnabled(true);
    cs.setCullFace(CullState.Face.Back);
    _root.setRenderState(cs);

    // final FogState fs = new FogState();
    // fs.setStart(farPlane / 2.0f);
    // fs.setEnd(farPlane);
    // fs.setColor(new ColorRGBA(1.0f, 1.0f, 1.0f, 1.0f));
    // fs.setDensityFunction(DensityFunction.Linear);
    // _root.setRenderState(fs);
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
   * Update text information.
   */
  private void updateText() {
    _exampleInfo[0].setText("[1/2/3] Moving speed: " + _controlHandle.getMoveSpeed() * 3.6 + " km/h");
    _exampleInfo[1].setText("[R] Draw texture debug: " + terrain.getTextureClipmap().isShowDebug());
    _exampleInfo[2].setText("[SPACE] Toggle fly/walk: " + (groundCamera ? "walk" : "fly"));
    _exampleInfo[3].setText("[J] Regenerate heightmap/texture");
    _exampleInfo[4].setText("[U] Freeze terrain(debug): " + !updateTerrain);
  }
}
