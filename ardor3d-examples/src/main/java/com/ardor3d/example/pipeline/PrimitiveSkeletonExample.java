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

import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.extension.animation.skeletal.Joint;
import com.ardor3d.extension.animation.skeletal.Skeleton;
import com.ardor3d.extension.animation.skeletal.SkeletonPose;
import com.ardor3d.extension.animation.skeletal.SkinnedMesh;
import com.ardor3d.extension.animation.skeletal.util.SkeletalDebugger;
import com.ardor3d.input.keyboard.Key;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyPressedCondition;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Transform;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.material.MaterialManager;
import com.ardor3d.renderer.material.RenderMaterial;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.scenegraph.hint.LightCombineMode;
import com.ardor3d.scenegraph.shape.Cylinder;
import com.ardor3d.ui.text.BasicText;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.geom.BufferUtils;

/**
 * A demonstration of combining the skeletal animation classes with OpenGL Shading Language.
 */
@Purpose(
    htmlDescriptionKey = "com.ardor3d.example.pipeline.PrimitiveSkeletonExample", //
    thumbnailPath = "com/ardor3d/example/media/thumbnails/pipeline_PrimitiveSkeletonExample.jpg", //
    maxHeapMemory = 64)
public class PrimitiveSkeletonExample extends ExampleBase {

  private boolean runAnimation = true;
  private boolean showSkeleton = false;
  private boolean useGPU = true;

  private SkeletonPose pose;
  private SkinnedMesh arm;
  private BasicText t1, t2, t3;

  private RenderMaterial matCPU, matGPU;

  public static void main(final String[] args) {
    ExampleBase.start(PrimitiveSkeletonExample.class);
  }

  @Override
  protected void initExample() {
    _canvas.setTitle("Simple example of skinned mesh");
    _canvas.getCanvasRenderer().getRenderer().setBackgroundColor(new ColorRGBA(0.1f, 0.1f, 0.1f, 1.0f));

    _lightState.get(0).setDiffuse(new ColorRGBA(300, 300, 300, 1));

    SkinnedMesh.addDefaultResourceLocators();

    final Transform j1Transform = new Transform();
    j1Transform.setTranslation(0, 0, -5);
    j1Transform.invert(j1Transform);
    final Joint j1 = new Joint("j1");
    j1.setInverseBindPose(j1Transform);
    j1.setParentIndex(Joint.NO_PARENT);

    final Transform j2Transform = new Transform();
    j2Transform.setTranslation(0, 0, 5);
    j2Transform.invert(j2Transform);
    final Joint j2 = new Joint("j2");
    j2.setInverseBindPose(j2Transform);
    j2.setParentIndex((short) 0);

    final Skeleton sk = new Skeleton("arm sk", new Joint[] {j1, j2});

    matGPU = MaterialManager.INSTANCE.findMaterial("unlit/untextured/basic_skinmesh.yaml");
    matCPU = MaterialManager.INSTANCE.findMaterial("unlit/untextured/basic.yaml");

    pose = new SkeletonPose(sk);
    pose.updateTransforms();

    arm = new SkinnedMesh("arm");
    arm.setRenderMaterial(useGPU ? matGPU : matCPU);
    final Cylinder cy = new Cylinder("cylinder", 3, 8, 1, 10);
    arm.setBindPoseData(cy.getMeshData());
    arm.getMeshData().setVertexBuffer(BufferUtils.clone(cy.getMeshData().getVertexBuffer()));
    arm.getMeshData().setNormalBuffer(BufferUtils.createFloatBuffer(cy.getMeshData().getNormalBuffer().capacity()));
    arm.getMeshData().setIndices(BufferUtils.clone(cy.getMeshData().getIndices()));
    arm.getMeshData().setTextureBuffer(BufferUtils.clone(cy.getMeshData().getTextureBuffer(0)), 0);
    arm.setTranslation(0, 0, -10);
    arm.updateModelBound();
    arm.getSceneHints().setCullHint(CullHint.Dynamic);

    arm.setDefaultColor(ColorRGBA.LIGHT_GRAY);
    arm.setWeightsPerVert(4);

    final float[] weights = { //
        1, 0, 0, 0, //
        1, 0, 0, 0, //
        1, 0, 0, 0, //
        1, 0, 0, 0, //
        1, 0, 0, 0, //
        1, 0, 0, 0, //
        1, 0, 0, 0, //
        1, 0, 0, 0, //
        1, 0, 0, 0, //

        0.5f, 0.5f, 0, 0, //
        0.5f, 0.5f, 0, 0, //
        0.5f, 0.5f, 0, 0, //
        0.5f, 0.5f, 0, 0, //
        0.5f, 0.5f, 0, 0, //
        0.5f, 0.5f, 0, 0, //
        0.5f, 0.5f, 0, 0, //
        0.5f, 0.5f, 0, 0, //
        0.5f, 0.5f, 0, 0, //

        0, 1, 0, 0, //
        0, 1, 0, 0, //
        0, 1, 0, 0, //
        0, 1, 0, 0, //
        0, 1, 0, 0, //
        0, 1, 0, 0, //
        0, 1, 0, 0, //
        0, 1, 0, 0, //
        0, 1, 0, 0 //
    };
    arm.setWeights(weights);

    final short[] indices = new short[4 * 27];
    for (int i = 0; i < 27; i++) {
      indices[i * 4 + 0] = 0;
      indices[i * 4 + 1] = 1;
      indices[i * 4 + 2] = 0;
      indices[i * 4 + 3] = 0;
    }
    arm.setJointIndices(indices);

    arm.setUseGPU(useGPU);

    arm.setCurrentPose(pose);

    _root.attachChild(arm);

    t1 = BasicText.createDefaultTextLabel("Text1", "[SPACE] Pause joint animation.");
    t1.getSceneHints().setRenderBucketType(RenderBucketType.OrthoOrder);
    t1.getSceneHints().setLightCombineMode(LightCombineMode.Off);
    t1.setTranslation(new Vector3(5, 2 * (t1.getHeight() + 5) + 10, 0));
    _orthoRoot.attachChild(t1);

    t2 = BasicText.createDefaultTextLabel("Text2", "[G] GPU Skinning is " + (useGPU ? "ON." : "OFF."));
    t2.getSceneHints().setRenderBucketType(RenderBucketType.OrthoOrder);
    t2.getSceneHints().setLightCombineMode(LightCombineMode.Off);
    t2.setTranslation(new Vector3(5, 1 * (t1.getHeight() + 5) + 10, 0));
    _orthoRoot.attachChild(t2);

    t3 = BasicText.createDefaultTextLabel("Text3", "[K] Show Skeleton.");
    t3.getSceneHints().setRenderBucketType(RenderBucketType.OrthoOrder);
    t3.getSceneHints().setLightCombineMode(LightCombineMode.Off);
    t3.setTranslation(new Vector3(5, 0 * (t1.getHeight() + 5) + 10, 0));
    _orthoRoot.attachChild(t3);
    _orthoRoot.getSceneHints().setCullHint(CullHint.Never);

    _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.K), (source, inputStates, tpf) -> {
      showSkeleton = !showSkeleton;
      if (showSkeleton) {
        t3.setText("[K] Hide Skeleton.");
      } else {
        t3.setText("[K] Show Skeleon.");
      }
    }));

    _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.G), (source, inputStates, tpf) -> {
      useGPU = !useGPU;
      arm.setUseGPU(useGPU);
      arm.setRenderMaterial(useGPU ? matGPU : matCPU);
      if (useGPU) {
        t2.setText("[G] GPU Skinning is ON.");
      } else {
        t2.setText("[G] GPU Skinning is OFF.");
      }
    }));

    _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.SPACE), (source, inputStates, tpf) -> {
      runAnimation = !runAnimation;
      if (runAnimation) {
        t1.setText("[SPACE] Pause joint animation.");
      } else {
        t1.setText("[SPACE] Start joint animation.");
      }
    }));
  }

  @Override
  protected void renderDebug(final Renderer renderer) {
    super.renderDebug(renderer);

    if (showSkeleton) {
      SkeletalDebugger.drawSkeletons(_root, renderer, true, false);
    }
  }

  double angle = 0;

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

    if (runAnimation) {
      angle += timer.getTimePerFrame() * 50;
      angle %= 360;

      // move the end of the arm up and down.
      final Transform t1 = pose.getLocalJointTransforms()[1];
      t1.setTranslation(t1.getTranslation().getX(), Math.sin(angle * MathUtils.DEG_TO_RAD) * 2,
          t1.getTranslation().getZ());

      pose.updateTransforms();

      if (!useGPU) {
        // no point to updating the model bound in gpu mode
        arm.updateModelBound();
      }
    }
  }
}
