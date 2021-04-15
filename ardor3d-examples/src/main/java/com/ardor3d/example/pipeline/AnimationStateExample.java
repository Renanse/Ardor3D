/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.example.pipeline;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;

import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.extension.animation.skeletal.AnimationManager;
import com.ardor3d.extension.animation.skeletal.SkeletonPose;
import com.ardor3d.extension.animation.skeletal.SkinnedMesh;
import com.ardor3d.extension.animation.skeletal.SkinnedMeshCombineLogic;
import com.ardor3d.extension.animation.skeletal.blendtree.SimpleAnimationApplier;
import com.ardor3d.extension.animation.skeletal.state.loader.InputStore;
import com.ardor3d.extension.animation.skeletal.state.loader.JSLayerImporter;
import com.ardor3d.extension.animation.skeletal.util.SkeletalDebugger;
import com.ardor3d.extension.model.collada.jdom.ColladaImporter;
import com.ardor3d.extension.model.collada.jdom.data.ColladaStorage;
import com.ardor3d.extension.model.collada.jdom.data.SkinData;
import com.ardor3d.extension.model.util.nvtristrip.NvTriangleStripper;
import com.ardor3d.extension.ui.UIButton;
import com.ardor3d.extension.ui.UICheckBox;
import com.ardor3d.extension.ui.UIComponent;
import com.ardor3d.extension.ui.UIFrame;
import com.ardor3d.extension.ui.UIFrame.FrameButtons;
import com.ardor3d.extension.ui.UIHud;
import com.ardor3d.extension.ui.UILabel;
import com.ardor3d.extension.ui.UIPanel;
import com.ardor3d.extension.ui.event.ActionEvent;
import com.ardor3d.extension.ui.event.ActionListener;
import com.ardor3d.extension.ui.layout.AnchorLayout;
import com.ardor3d.extension.ui.layout.AnchorLayoutData;
import com.ardor3d.extension.ui.util.Alignment;
import com.ardor3d.framework.CanvasRenderer;
import com.ardor3d.framework.NativeCanvas;
import com.ardor3d.light.DirectionalLight;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.material.MaterialManager;
import com.ardor3d.renderer.material.RenderMaterial;
import com.ardor3d.renderer.state.CullState;
import com.ardor3d.renderer.state.CullState.Face;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.util.GameTaskQueue;
import com.ardor3d.util.GameTaskQueueManager;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.geom.MeshCombiner;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.ResourceSource;
import com.ardor3d.util.resource.URLResourceSource;

/**
 * Illustrates loading several animations from Collada and arranging them in an animation state
 * machine.
 */
@Purpose(
    htmlDescriptionKey = "com.ardor3d.example.pipeline.AnimationStateExample", //
    thumbnailPath = "com/ardor3d/example/media/thumbnails/pipeline_AnimationStateExample.jpg", //
    maxHeapMemory = 64)
public class AnimationStateExample extends ExampleBase {

  private Spatial primeModel;
  private boolean showSkeleton = false, showJointLabels = false;

  private UILabel frameRateLabel;
  private UIHud hud;

  private int frames = 0;
  private long startTime = System.currentTimeMillis();

  private AnimationManager manager;
  private SkeletonPose pose;

  private UIButton runWalkButton, punchButton, playPauseButton, stopButton;

  private final Node skNode = new Node("model");
  private RenderMaterial matCPU, matGPU;

  public static void main(final String[] args) {
    ExampleBase.start(AnimationStateExample.class);
  }

  @Override
  protected void initExample() {
    _canvas.setTitle("Ardor3D - Animation State Example");
    final CanvasRenderer canvasRenderer = _canvas.getCanvasRenderer();
    final RenderContext renderContext = canvasRenderer.getRenderContext();
    final Renderer renderer = canvasRenderer.getRenderer();
    GameTaskQueueManager.getManager(renderContext).getQueue(GameTaskQueue.RENDER).enqueue(() -> {
      renderer.setBackgroundColor(ColorRGBA.GRAY);
      return null;
    });

    // set camera
    final Camera cam = _canvas.getCanvasRenderer().getCamera();
    cam.setLocation(280, 372, -280);
    cam.lookAt(new Vector3(250, 350, -280), Vector3.UNIT_Y);
    cam.setFrustumPerspective(50.0, cam.getWidth() / (double) cam.getHeight(), .25, 900);

    // speed up wasd control a little
    _controlHandle.setMoveSpeed(200);

    // Load collada model
    createCharacter();

    // Create our options frame and fps label
    createHUD();
  }

  @Override
  protected void setupLight() {
    final DirectionalLight light = new DirectionalLight();
    light.setColor(new ColorRGBA(0.75f, 0.75f, 0.75f, 0.75f));
    light.setWorldDirection(new Vector3(-1, -1, -1).normalizeLocal());
    light.setEnabled(true);
    _root.attachChild(light);
  }

  private void createHUD() {
    hud = new UIHud(_canvas);
    hud.setupInput(_physicalLayer, _logicalLayer);
    hud.setMouseManager(_mouseManager);

    // Add fps display
    frameRateLabel = new UILabel("X");
    frameRateLabel.setHudXY(5, hud.getHeight() - 5 - frameRateLabel.getContentHeight());
    frameRateLabel.setForegroundColor(ColorRGBA.WHITE);
    hud.add(frameRateLabel);

    final UIFrame optionsFrame = new UIFrame("Controls", EnumSet.noneOf(FrameButtons.class));

    final UIPanel basePanel = optionsFrame.getContentPanel();
    basePanel.setLayout(new AnchorLayout());

    runWalkButton = new UIButton("Start running...");
    runWalkButton.setLayoutData(new AnchorLayoutData(Alignment.TOP_LEFT, basePanel, Alignment.TOP_LEFT, 5, -5));
    runWalkButton.addActionListener(new ActionListener() {
      boolean walk = true;

      @Override
      public void actionPerformed(final ActionEvent event) {
        if (!walk) {
          if (manager.getBaseAnimationLayer().doTransition("walk")) {
            runWalkButton.setButtonText("Start running...");
            walk = true;
          }
        } else {
          if (manager.getBaseAnimationLayer().doTransition("run")) {
            runWalkButton.setButtonText("Start walking...");
            walk = false;
          }
        }
      }
    });
    basePanel.add(runWalkButton);

    punchButton = new UIButton("PUNCH!");
    punchButton.setLayoutData(new AnchorLayoutData(Alignment.TOP_LEFT, runWalkButton, Alignment.BOTTOM_LEFT, 0, -5));
    punchButton.addActionListener(event -> {
      manager.findAnimationLayer("punch").setCurrentState("punch_right", true);
      punchButton.setEnabled(false);
    });
    basePanel.add(punchButton);

    playPauseButton = new UIButton("Pause");
    playPauseButton.setLayoutData(new AnchorLayoutData(Alignment.TOP_LEFT, punchButton, Alignment.BOTTOM_LEFT, 0, -5));
    playPauseButton.addActionListener(event -> {
      if (playPauseButton.getText().equals("Pause")) {
        manager.pause();
        playPauseButton.setButtonText("Play");
      } else {
        manager.play();
        playPauseButton.setButtonText("Pause");
      }
    });
    basePanel.add(playPauseButton);

    stopButton = new UIButton("Stop");
    stopButton.setLayoutData(new AnchorLayoutData(Alignment.TOP_LEFT, playPauseButton, Alignment.BOTTOM_LEFT, 0, -5));
    stopButton.addActionListener(event -> {
      manager.stop();
      playPauseButton.setButtonText("Play");
    });
    basePanel.add(stopButton);

    final UICheckBox resetAnimCheck = new UICheckBox("Reset Animation On Stop");
    resetAnimCheck.setLayoutData(new AnchorLayoutData(Alignment.TOP_LEFT, stopButton, Alignment.BOTTOM_LEFT, 0, -5));
    resetAnimCheck.setSelected(false);
    resetAnimCheck.addActionListener(event -> manager.setResetClipsOnStop(resetAnimCheck.isSelected()));
    basePanel.add(resetAnimCheck);

    final UICheckBox gpuSkinningCheck = new UICheckBox("Use GPU skinning");
    gpuSkinningCheck
        .setLayoutData(new AnchorLayoutData(Alignment.TOP_LEFT, resetAnimCheck, Alignment.BOTTOM_LEFT, 0, -5));
    gpuSkinningCheck.setSelected(false);
    gpuSkinningCheck.addActionListener(event -> _root.acceptVisitor((final Spatial spatial) -> {
      if (spatial instanceof SkinnedMesh) {
        final SkinnedMesh skinnedSpatial = (SkinnedMesh) spatial;
        if (gpuSkinningCheck.isSelected()) {
          skinnedSpatial.setRenderMaterial(matGPU);
          skinnedSpatial.setUseGPU(true);
        } else {
          skinnedSpatial.setRenderMaterial(matCPU);
          skinnedSpatial.setUseGPU(false);
        }
      }
    }, true));
    basePanel.add(gpuSkinningCheck);

    final UICheckBox skeletonCheck = new UICheckBox("Show skeleton");
    final UICheckBox boneLabelCheck = new UICheckBox("Show joint labels");
    skeletonCheck
        .setLayoutData(new AnchorLayoutData(Alignment.TOP_LEFT, gpuSkinningCheck, Alignment.BOTTOM_LEFT, 0, -5));
    skeletonCheck.setSelected(showSkeleton);
    skeletonCheck.addActionListener(event -> {
      showSkeleton = skeletonCheck.isSelected();
      boneLabelCheck.setEnabled(showSkeleton);
    });
    basePanel.add(skeletonCheck);

    boneLabelCheck.setLayoutData(new AnchorLayoutData(Alignment.TOP_LEFT, skeletonCheck, Alignment.BOTTOM_LEFT, 0, -5));
    boneLabelCheck.setSelected(false);
    boneLabelCheck.setEnabled(showSkeleton);
    boneLabelCheck.addActionListener(event -> showJointLabels = boneLabelCheck.isSelected());
    basePanel.add(boneLabelCheck);

    optionsFrame.pack();

    optionsFrame.setUseStandin(true);
    optionsFrame.setOpacity(0.8f);

    final Camera cam = _canvas.getCanvasRenderer().getCamera();
    optionsFrame.setLocalXY(cam.getWidth() - optionsFrame.getLocalComponentWidth() - 10,
        cam.getHeight() - optionsFrame.getLocalComponentHeight() - 10);
    hud.add(optionsFrame);

    UIComponent.setUseTransparency(true);
  }

  private void createCharacter() {
    try {
      SkinnedMesh.addDefaultResourceLocators();
      matGPU = MaterialManager.INSTANCE.findMaterial("lit/textured/basic_skinmesh_phong.yaml");
      matCPU = MaterialManager.INSTANCE.findMaterial("lit/textured/basic_phong.yaml");

      skNode.detachAllChildren();
      _root.attachChild(skNode);

      final long time = System.currentTimeMillis();
      final ColladaImporter colladaImporter = new ColladaImporter();

      // OPTIMIZATION: run GeometryTool on collada meshes to reduce redundant vertices...
      colladaImporter.setOptimizeMeshes(true);

      // Load the collada scene
      final String mainFile = "collada/skeleton/skeleton.walk.dae";
      final ColladaStorage storage = colladaImporter.load(mainFile);
      final Node colladaNode = storage.getScene();
      final List<SkinData> skinDatas = storage.getSkins();
      pose = skinDatas.get(0).getPose();

      createAnimation();

      System.out.println("Importing: " + mainFile);
      System.out.println("Took " + (System.currentTimeMillis() - time) + " ms");

      // OPTIMIZATION: SkinnedMesh combining... Useful in our case because the skeleton model is composed
      // of 2
      // separate meshes.
      primeModel = MeshCombiner.combine(colladaNode, new SkinnedMeshCombineLogic());
      // Non-combined:
      // primeModel = colladaNode;

      // OPTIMIZATION: turn on the buffers in our skeleton so they can be shared. (reuse ids)
      primeModel.acceptVisitor((final Spatial spatial) -> {
        if (spatial instanceof SkinnedMesh) {
          final SkinnedMesh skinnedSpatial = (SkinnedMesh) spatial;
          skinnedSpatial.recreateWeightAttributeBuffer();
          skinnedSpatial.recreateJointAttributeBuffer();
        }
      }, true);

      // OPTIMIZATION: run nv strippifyier on model...
      final NvTriangleStripper stripper = new NvTriangleStripper();
      stripper.setReorderVertices(true);
      primeModel.acceptVisitor(stripper, true);

      // OPTIMIZATION: don't draw surfaces that face away from the camera...
      final CullState cullState = new CullState();
      cullState.setCullFace(Face.Back);
      primeModel.setRenderState(cullState);
      primeModel.setRenderMaterial(matCPU);

      for (int i = 0; i < 10; i++) {
        for (int j = 0; j < 10; j++) {
          // Add copy of model
          final Spatial copy = primeModel.makeCopy(true);
          copy.setTranslation(-i * 50, 0, -50 - (j * 50));
          skNode.attachChild(copy);
        }
      }
    } catch (final Exception ex) {
      ex.printStackTrace();
    }
  }

  private void createAnimation() {
    final ColladaImporter colladaImporter = new ColladaImporter();

    // Make our manager
    manager = new AnimationManager(_timer, pose);

    // Add our "applier logic".
    final SimpleAnimationApplier applier = new SimpleAnimationApplier();
    manager.setApplier(applier);

    // Add a call back to load clips.
    final InputStore input = new InputStore();
    input.getClips().setMissCallback(key -> {
      try {
        final ColladaStorage storage1 = colladaImporter.load("collada/skeleton/" + key + ".dae");
        return storage1.extractChannelsAsClip(key);
      } catch (final IOException e) {
        e.printStackTrace();
      }
      return null;
    });

    // Load our layer and states from script
    try {
      final ResourceSource layersFile = new URLResourceSource(ResourceLocatorTool
          .getClassPathResource(AnimationStateExample.class, "com/ardor3d/example/pipeline/AnimationCopyExample.js"));
      JSLayerImporter.addLayers(layersFile, manager, input);
    } catch (final Exception e) {
      e.printStackTrace();
    }

    // kick things off by setting our starting states
    manager.getBaseAnimationLayer().setCurrentState("walk_anim", true);

    // add callback for our UI
    manager.findClipInstance("skeleton.punch").addAnimationListener(source -> punchButton.setEnabled(true));
  }

  @Override
  protected void updateExample(final ReadOnlyTimer timer) {
    hud.updateGeometricState(timer.getTimePerFrame());

    final long now = System.currentTimeMillis();
    final long dt = now - startTime;
    if (dt > 200) {
      final long fps = Math.round(1e3 * frames / dt);
      frameRateLabel.setText(fps + " fps");

      startTime = now;
      frames = 0;
    }
    frames++;

    manager.update();
  }

  @Override
  protected void updateLogicalLayer(final ReadOnlyTimer timer) {
    hud.getLogicalLayer().checkTriggers(timer.getTimePerFrame());
  }

  @Override
  protected void renderExample(final Renderer renderer) {
    super.renderExample(renderer);
    renderer.renderBuckets();
    renderer.draw(hud);
  }

  @Override
  protected void renderDebug(final Renderer renderer) {
    super.renderDebug(renderer);

    if (showSkeleton) {
      SkeletalDebugger.drawSkeletons(_root, renderer, false, showJointLabels);
    }
  }

  public NativeCanvas getCanvas() { return _canvas; }

  public Node getRoot() { return _root; }
}
