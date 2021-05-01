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

import com.ardor3d.example.Purpose;

/**
 * Example showing the Geometry Clipmap Terrain system combined with PSSM. (a bit experimental)
 * Requires GLSL support.
 */
@Purpose(
    htmlDescriptionKey = "com.ardor3d.example.terrain.ShadowedTerrainExample", //
    thumbnailPath = "com/ardor3d/example/media/thumbnails/terrain_ShadowedTerrainExample.jpg", //
    maxHeapMemory = 128)
public class ShadowedTerrainExample {
  // extends ExampleBase {
  // /** The Constant logger. */
  // private static final Logger logger = Logger.getLogger(ShadowedTerrainExample.class.getName());
  //
  // private boolean updateTerrain = true;
  // private final float farPlane = 2500.0f;
  //
  // private Terrain terrain;
  //
  // private final Sphere sphere = new Sphere("sp", 16, 16, 1);
  // private final Ray3 pickRay = new Ray3();
  //
  // private boolean groundCamera = false;
  // private Camera terrainCamera;
  //
  // /** Pssm shadow map pass. */
  // private ParallelSplitShadowMapPass _pssmPass;
  //
  // private DirectionalLight light;
  //
  // /** Temp vec for updating light pos. */
  // private final Vector3 lightPosition = new Vector3(10000, 10000, 10000);
  //
  // /** Text fields used to present info about the example. */
  // private final BasicText _exampleInfo[] = new BasicText[5];
  //
  // public static void main(final String[] args) {
  // ExampleBase.start(ShadowedTerrainExample.class);
  // }
  //
  // @Override
  // protected void updateExample(final ReadOnlyTimer timer) {
  // final Camera camera = _canvas.getCanvasRenderer().getCamera();
  //
  // // Make sure camera is above terrain
  // final double height = terrain.getHeightAt(camera.getLocation().getX(),
  // camera.getLocation().getZ());
  // if (height > -Float.MAX_VALUE && (groundCamera || camera.getLocation().getY() < height + 3)) {
  // camera.setLocation(new Vector3(camera.getLocation().getX(), height + 3,
  // camera.getLocation().getZ()));
  // }
  //
  // if (updateTerrain) {
  // terrainCamera.set(camera);
  // }
  //
  // // if we're picking...
  // if (sphere.getSceneHints().getCullHint() == CullHint.Dynamic) {
  // // Set up our pick ray
  // pickRay.setOrigin(camera.getLocation());
  // pickRay.setDirection(camera.getDirection());
  //
  // // do pick and move the sphere
  // final PrimitivePickResults pickResults = new PrimitivePickResults();
  // pickResults.setCheckDistance(true);
  // PickingUtil.findPick(_root, pickRay, pickResults);
  // if (pickResults.getNumber() != 0) {
  // final Vector3 intersectionPoint =
  // pickResults.getPickData(0).getIntersectionRecord().getIntersectionPoint(0);
  // sphere.setTranslation(intersectionPoint);
  // // XXX: maybe change the color of the ball for valid vs. invalid?
  // }
  // }
  // }
  //
  // @Override
  // protected void renderExample(final Renderer renderer) {
  // // Lazy init since it needs the renderer...
  // if (!_pssmPass.isInitialised()) {
  // _pssmPass.init(renderer);
  // // _pssmPass.setPssmShader(terrain.getGeometryClipmapShader());
  // for (int i = 0; i < _pssmPass.getNumOfSplits(); i++) {
  // terrain.getClipTextureState().setTexture(_pssmPass.getShadowMapTexture(i), i + 1);
  // }
  // for (int i = 0; i < ParallelSplitShadowMapPass._MAX_SPLITS; i++) {
  // // terrain.getGeometryClipmapShader().setUniform("shadowMap" + i, i + 1);
  // }
  // }
  //
  // // Update shadowmaps
  // _pssmPass.updateShadowMaps(renderer);
  //
  // // Render scene and terrain with shadows
  // super.renderExample(renderer);
  // renderer.renderBuckets();
  //
  // // Render overlay shadows for all objects except the terrain
  // _pssmPass.renderShadowedScene(renderer);
  //
  // // TODO: this results in text etc also being shadowed, since they are drawn in the main render...
  // }
  //
  // /**
  // * Initialize pssm pass and scene.
  // */
  // @Override
  // protected void initExample() {
  // _canvas.setTitle("Terrain Example");
  // final Camera cam = _canvas.getCanvasRenderer().getCamera();
  // cam.setLocation(new Vector3(440, 215, 275));
  // cam.lookAt(new Vector3(450, 140, 360), Vector3.UNIT_Y);
  // cam.setFrustumPerspective(70.0, (float) cam.getWidth() / cam.getHeight(), 1.0f, farPlane);
  // final CanvasRenderer canvasRenderer = _canvas.getCanvasRenderer();
  // final RenderContext renderContext = canvasRenderer.getRenderContext();
  // final Renderer renderer = canvasRenderer.getRenderer();
  // GameTaskQueueManager.getManager(renderContext).getQueue(GameTaskQueue.RENDER).enqueue(() -> {
  // renderer.setBackgroundColor(ColorRGBA.GRAY);
  // return null;
  // });
  // _controlHandle.setMoveSpeed(200);
  //
  // setupDefaultStates();
  //
  // sphere.getSceneHints().setAllPickingHints(false);
  // sphere.getSceneHints().setCullHint(CullHint.Always);
  // _root.attachChild(sphere);
  //
  // try {
  // // Keep a separate camera to be able to freeze terrain update
  // terrainCamera = new Camera(cam);
  //
  // final int SIZE = 2048;
  //
  // final MidPointHeightMapGenerator raw = new MidPointHeightMapGenerator(SIZE, 0.6f);
  // raw.setHeightRange(0.2f);
  // final float[] heightMap = raw.getHeightData();
  //
  // final TerrainDataProvider terrainDataProvider =
  // new ArrayTerrainDataProvider(heightMap, SIZE, new Vector3(1, 300, 1));
  //
  // terrain = new TerrainBuilder(terrainDataProvider,
  // terrainCamera).withShowDebugPanels(true).build();
  //
  // // terrain.setPixelShader(
  // // new UrlInputSupplier(ResourceLocatorTool.getClassPathResource(ShadowedTerrainExample.class,
  // // "com/ardor3d/extension/terrain/shadowedGeometryClipmapShaderPCF.frag")));
  //
  // _root.attachChild(terrain);
  // } catch (final Exception e) {
  // logger.log(Level.SEVERE, "Problem setting up terrain...", e);
  // System.exit(1);
  // }
  //
  // // Initialize PSSM shadows
  // _pssmPass = new ParallelSplitShadowMapPass(light, 1024, 4);
  // _pssmPass.setFiltering(Filter.Pcf);
  // _pssmPass.setRenderShadowedScene(false);
  // _pssmPass.setKeepMainShader(true);
  // _pssmPass.setMaxShadowDistance(750); // XXX: Tune this
  // // _pssmPass.setMinimumLightDistance(500); // XXX: Tune this
  // _pssmPass.setUseSceneTexturing(false);
  // _pssmPass.setUseObjectCullFace(false);
  // // _pssmPass.setDrawDebug(true);
  //
  // final Node occluders = setupOccluders();
  // _root.attachChild(occluders);
  //
  // // TODO: could we use the shadow variable in scenehints here??
  // // Add objects that will get shadowed through overlay render
  // _pssmPass.add(occluders);
  //
  // // Add terrain in as bounds receiver as well, since it's not in the overlay list
  // _pssmPass.addBoundsReceiver(terrain);
  //
  // // Set our occluders to produce shadows
  // occluders.setProperty(ShadowCastMode.SpatialKey, ShadowCastMode.OneSidedUntextured);
  //
  // // Setup labels for presenting example info.
  // final Node textNodes = new Node("Text");
  // _orthoRoot.attachChild(textNodes);
  // LightProperties.setLightReceiver(textNodes, false);
  //
  // final double infoStartY = _canvas.getCanvasRenderer().getCamera().getHeight() / 2;
  // for (int i = 0; i < _exampleInfo.length; i++) {
  // _exampleInfo[i] = BasicText.createDefaultTextLabel("Text", "", 16);
  // _exampleInfo[i].setTranslation(new Vector3(10, infoStartY - i * 20, 0));
  // textNodes.attachChild(_exampleInfo[i]);
  // }
  //
  // textNodes.updateGeometricState(0.0);
  // updateText();
  //
  // _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.U), (source,
  // inputStates, tpf) -> {
  // updateTerrain = !updateTerrain;
  // updateText();
  // }));
  // _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.ONE), (source,
  // inputStates, tpf) -> {
  // _controlHandle.setMoveSpeed(5);
  // updateText();
  // }));
  // _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.TWO), (source,
  // inputStates, tpf) -> {
  // _controlHandle.setMoveSpeed(50);
  // updateText();
  // }));
  // _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.THREE), (source,
  // inputStates, tpf) -> {
  // _controlHandle.setMoveSpeed(400);
  // updateText();
  // }));
  // _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.FOUR), (source,
  // inputStates, tpf) -> {
  // _controlHandle.setMoveSpeed(1000);
  // updateText();
  // }));
  //
  // _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.SPACE), (source,
  // inputStates, tpf) -> {
  // groundCamera = !groundCamera;
  // updateText();
  // }));
  //
  // _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.P), (source,
  // inputStates, tpf) -> {
  // if (sphere.getSceneHints().getCullHint() == CullHint.Dynamic) {
  // sphere.getSceneHints().setCullHint(CullHint.Always);
  // } else if (sphere.getSceneHints().getCullHint() == CullHint.Always) {
  // sphere.getSceneHints().setCullHint(CullHint.Dynamic);
  // }
  // updateText();
  // }));
  // _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.R), (source,
  // inputStates, tpf) -> {
  // terrain.getTextureClipmap().setShowDebug(!terrain.getTextureClipmap().isShowDebug());
  // updateText();
  // }));
  // _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.FIVE), (source,
  // inputStates, tpf) -> {
  // terrain.getTextureClipmap().setPixelDensity(terrain.getTextureClipmap().getPixelDensity() / 2);
  // updateText();
  // }));
  // _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.SIX), (source,
  // inputStates, tpf) -> {
  // terrain.getTextureClipmap().setPixelDensity(terrain.getTextureClipmap().getPixelDensity() * 2);
  // updateText();
  // }));
  // _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.C), (source,
  // inputStates, tpf) -> {
  // _pssmPass.setUpdateMainCamera(!_pssmPass.isUpdateMainCamera());
  // updateText();
  // }));
  // _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.ZERO), (source,
  // inputStates, tpf) -> {
  // final Camera cam1 = _canvas.getCanvasRenderer().getCamera();
  // System.out.println("camera location: " + cam1.getLocation());
  // System.out.println("camera direction: " + cam1.getDirection());
  // }));
  //
  // }
  //
  // private Node setupOccluders() {
  // final Node occluders = new Node("Occluders");
  //
  // final Box box = new Box("Box", new Vector3(), 1, 40, 1);
  // box.setModelBound(new BoundingBox());
  // box.setRandomColors();
  //
  // final Random rand = new Random(1337);
  // for (int x = 0; x < 8; x++) {
  // for (int y = 0; y < 8; y++) {
  // final Mesh sm = box.makeCopy(true);
  //
  // sm.setTranslation(500 + rand.nextDouble() * 300 - 150, 20 + rand.nextDouble() * 5.0,
  // 500 + rand.nextDouble() * 300 - 150);
  // occluders.attachChild(sm);
  // }
  // }
  //
  // return occluders;
  // }
  //
  // private void setupDefaultStates() {
  // final CullState cs = new CullState();
  // cs.setEnabled(true);
  // cs.setCullFace(CullState.Face.Back);
  // _root.setRenderState(cs);
  //
  // // final FogState fs = new FogState();
  // // fs.setStart(farPlane / 2.0f);
  // // fs.setEnd(farPlane);
  // // fs.setColor(new ColorRGBA(1.0f, 1.0f, 1.0f, 1.0f));
  // // fs.setDensityFunction(DensityFunction.Linear);
  // // _root.setRenderState(fs);
  // }
  //
  // @Override
  // protected void setupLight() {
  // light = new DirectionalLight();
  // light.setEnabled(true);
  // light.setColor(new ColorRGBA(0.6f, 0.6f, 0.5f, 1));
  // light.setWorldDirection(lightPosition.normalize(null).negateLocal());
  // _root.attachChild(light);
  // }
  //
  // /**
  // * Update text information.
  // */
  // private void updateText() {
  // _exampleInfo[0].setText("[1/2/3] Moving speed: " + _controlHandle.getMoveSpeed() * 3.6 + "
  // km/h");
  // _exampleInfo[1].setText("[P] Do picking: " + (sphere.getSceneHints().getCullHint() ==
  // CullHint.Dynamic));
  // _exampleInfo[2].setText("[SPACE] Toggle fly/walk: " + (groundCamera ? "walk" : "fly"));
  // _exampleInfo[3].setText("[U] Freeze terrain(debug): " + !updateTerrain);
  // }
}
