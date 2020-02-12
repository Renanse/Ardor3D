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

import java.awt.image.BufferedImage;
import java.util.concurrent.Callable;

import javax.imageio.ImageIO;

import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.extension.terrain.client.Terrain;
import com.ardor3d.extension.terrain.client.TerrainBuilder;
import com.ardor3d.extension.terrain.client.TerrainDataProvider;
import com.ardor3d.extension.terrain.heightmap.ImageHeightMap;
import com.ardor3d.extension.terrain.providers.array.ArrayTerrainDataProvider;
import com.ardor3d.framework.Canvas;
import com.ardor3d.framework.CanvasRenderer;
import com.ardor3d.image.Image;
import com.ardor3d.image.util.awt.AWTImageLoader;
import com.ardor3d.input.keyboard.Key;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyPressedCondition;
import com.ardor3d.input.logical.TriggerAction;
import com.ardor3d.input.logical.TwoInputStates;
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
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.CullState;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.scenegraph.hint.LightCombineMode;
import com.ardor3d.scenegraph.shape.Box;
import com.ardor3d.scenegraph.shape.Sphere;
import com.ardor3d.ui.text.BasicText;
import com.ardor3d.util.GameTaskQueue;
import com.ardor3d.util.GameTaskQueueManager;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.resource.ResourceLocatorTool;

/**
 * Example showing the Geometry Clipmap Terrain system with 'MegaTextures' where the terrain data is provided from a
 * float array populated from a heightmap generated from an Image. Requires GLSL support.
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.terrain.ImageMapTerrainExample", //
        thumbnailPath = "com/ardor3d/example/media/thumbnails/terrain_ImageMapTerrainExample.jpg", //
        maxHeapMemory = 128)
public class ImageMapTerrainExample extends ExampleBase {

    private boolean updateTerrain = true;
    private final float farPlane = 8000.0f;

    private Terrain terrain;

    private final Sphere sphere = new Sphere("sp", 16, 16, 1);
    private final Mesh arrow = new Box("normal", new Vector3(-0.2, -0.2, 0), new Vector3(0.2, 0.2, 4));
    private final Ray3 pickRay = new Ray3();

    private boolean groundCamera = false;
    private Camera terrainCamera;

    /** Text fields used to present info about the example. */
    private final BasicText _exampleInfo[] = new BasicText[5];

    private double counter = 0;
    private int frames = 0;

    public static void main(final String[] args) {
        ExampleBase.start(ImageMapTerrainExample.class);
    }

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
                final Vector3 intersectionPoint = pickResults.getPickData(0).getIntersectionRecord()
                        .getIntersectionPoint(0);
                sphere.setTranslation(intersectionPoint);

                final Vector3 intersectionNormal = pickResults.getPickData(0).getIntersectionRecord()
                        .getIntersectionNormal(0);
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
        GameTaskQueueManager.getManager(renderContext).getQueue(GameTaskQueue.RENDER).enqueue(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                renderer.setBackgroundColor(ColorRGBA.GRAY);
                return null;
            }
        });
        _controlHandle.setMoveSpeed(200);

        setupDefaultStates();

        sphere.getSceneHints().setAllPickingHints(false);
        sphere.getSceneHints().setCullHint(CullHint.Always);
        _root.attachChild(sphere);

        arrow.getSceneHints().setAllPickingHints(false);
        arrow.getSceneHints().setCullHint(CullHint.Always);
        _root.attachChild(arrow);

        try {
            // Keep a separate camera to be able to freeze terrain update
            final Camera camera = _canvas.getCanvasRenderer().getCamera();
            terrainCamera = new Camera(camera);

            // IMAGE LOADING AND CONVERSION TO HEIGHTMAP DONE HERE
            final BufferedImage dudvImage = ImageIO.read(ResourceLocatorTool.getClassPathResource(
                    ImageMapTerrainExample.class, "com/ardor3d/example/media/images/water/dudvmap.png"));
            final Image ardorImage = AWTImageLoader.makeArdor3dImage(dudvImage, false);
            final float[] heightMap = ImageHeightMap.generateHeightMap(ardorImage, 0.1f, .4f);
            // END OF IMAGE CONVERSION

            final int SIZE = ardorImage.getWidth();

            final TerrainDataProvider terrainDataProvider = new ArrayTerrainDataProvider(heightMap, SIZE,
                    new Vector3(3, 50, 3));

            terrain = new TerrainBuilder(terrainDataProvider, terrainCamera).withShowDebugPanels(true).build();
            terrain.setRenderMaterial("clipmap/terrain_textured.yaml");

            _root.attachChild(terrain);
        } catch (final Exception ex1) {
            System.out.println("Problem setting up terrain...");
            ex1.printStackTrace();
        }

        // Setup labels for presenting example info.
        final Node textNodes = new Node("Text");
        _orthoRoot.attachChild(textNodes);
        textNodes.getSceneHints().setRenderBucketType(RenderBucketType.OrthoOrder);
        textNodes.getSceneHints().setLightCombineMode(LightCombineMode.Off);

        final double infoStartY = _canvas.getCanvasRenderer().getCamera().getHeight() / 2;
        for (int i = 0; i < _exampleInfo.length; i++) {
            _exampleInfo[i] = BasicText.createDefaultTextLabel("Text", "", 16);
            _exampleInfo[i].setTranslation(new Vector3(10, infoStartY - i * 20, 0));
            textNodes.attachChild(_exampleInfo[i]);
        }

        textNodes.updateGeometricState(0.0);
        updateText();

        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.U), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                updateTerrain = !updateTerrain;
                updateText();
            }
        }));
        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.ONE), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                _controlHandle.setMoveSpeed(5);
                updateText();
            }
        }));
        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.TWO), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                _controlHandle.setMoveSpeed(50);
                updateText();
            }
        }));
        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.THREE), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                _controlHandle.setMoveSpeed(400);
                updateText();
            }
        }));
        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.FOUR), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                _controlHandle.setMoveSpeed(1000);
                updateText();
            }
        }));

        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.SPACE), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                groundCamera = !groundCamera;
                updateText();
            }
        }));

        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.P), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                if (sphere.getSceneHints().getCullHint() == CullHint.Dynamic) {
                    sphere.getSceneHints().setCullHint(CullHint.Always);
                    arrow.getSceneHints().setCullHint(CullHint.Always);
                } else if (sphere.getSceneHints().getCullHint() == CullHint.Always) {
                    sphere.getSceneHints().setCullHint(CullHint.Dynamic);
                    arrow.getSceneHints().setCullHint(CullHint.Dynamic);
                }
                updateText();
            }
        }));
        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.R), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                terrain.getTextureClipmap().setShowDebug(!terrain.getTextureClipmap().isShowDebug());
                updateText();
            }
        }));
        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.FIVE), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                terrain.getTextureClipmap().setPixelDensity(terrain.getTextureClipmap().getPixelDensity() / 2);
                updateText();
            }
        }));
        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.SIX), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                terrain.getTextureClipmap().setPixelDensity(terrain.getTextureClipmap().getPixelDensity() * 2);
                updateText();
            }
        }));

        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.SEVEN), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                final Camera camera = _canvas.getCanvasRenderer().getCamera();
                camera.setLocation(camera.getLocation().getX() + 500.0, camera.getLocation().getY(),
                        camera.getLocation().getZ());
            }
        }));

        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.EIGHT), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                final Camera camera = _canvas.getCanvasRenderer().getCamera();
                camera.setLocation(camera.getLocation().getX() - 500.0, camera.getLocation().getY(),
                        camera.getLocation().getZ());
            }
        }));
        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.NINE), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                final Camera camera = _canvas.getCanvasRenderer().getCamera();
                camera.setLocation(camera.getLocation().getX(), camera.getLocation().getY(),
                        camera.getLocation().getZ() + 1500.0);
            }
        }));

        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.ZERO), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                final Camera camera = _canvas.getCanvasRenderer().getCamera();
                camera.setLocation(camera.getLocation().getX(), camera.getLocation().getY(),
                        camera.getLocation().getZ() - 1500.0);
            }
        }));
    }

    private void setupDefaultStates() {
        _lightState.detachAll();
        final DirectionalLight dLight = new DirectionalLight();
        dLight.setEnabled(true);
        dLight.setAmbient(new ColorRGBA(0.4f, 0.4f, 0.5f, 1));
        dLight.setDiffuse(new ColorRGBA(0.6f, 0.6f, 0.5f, 1));
        dLight.setSpecular(new ColorRGBA(0.3f, 0.3f, 0.2f, 1));
        dLight.setDirection(new Vector3(-1, -1, -1).normalizeLocal());
        _lightState.attach(dLight);
        _lightState.setEnabled(true);

        final CullState cs = new CullState();
        cs.setEnabled(true);
        cs.setCullFace(CullState.Face.Back);
        _root.setRenderState(cs);

//        final FogState fs = new FogState();
//        fs.setStart(farPlane / 2.0f);
//        fs.setEnd(farPlane);
//        fs.setColor(new ColorRGBA(1.0f, 1.0f, 1.0f, 1.0f));
//        fs.setDensityFunction(DensityFunction.Linear);
//        _root.setRenderState(fs);
    }

    /**
     * Update text information.
     */
    private void updateText() {
        _exampleInfo[0].setText("[1/2/3] Moving speed: " + _controlHandle.getMoveSpeed() * 3.6 + " km/h");
        _exampleInfo[1].setText("[P] Do picking: " + (sphere.getSceneHints().getCullHint() == CullHint.Dynamic));
        _exampleInfo[2].setText("[SPACE] Toggle fly/walk: " + (groundCamera ? "walk" : "fly"));
        _exampleInfo[3].setText("[J] Regenerate heightmap/texture");
        _exampleInfo[4].setText("[U] Freeze terrain(debug): " + !updateTerrain);
    }
}
