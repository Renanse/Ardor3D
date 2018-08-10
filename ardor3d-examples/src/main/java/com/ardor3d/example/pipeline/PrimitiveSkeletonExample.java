/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example.pipeline;

import java.io.IOException;

import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.extension.animation.skeletal.Joint;
import com.ardor3d.extension.animation.skeletal.Skeleton;
import com.ardor3d.extension.animation.skeletal.SkeletonPose;
import com.ardor3d.extension.animation.skeletal.SkinnedMesh;
import com.ardor3d.extension.animation.skeletal.util.SkeletalDebugger;
import com.ardor3d.framework.Canvas;
import com.ardor3d.input.Key;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyPressedCondition;
import com.ardor3d.input.logical.TriggerAction;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Transform;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.ShaderState;
import com.ardor3d.renderer.state.ShaderState.ShaderType;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.scenegraph.hint.LightCombineMode;
import com.ardor3d.scenegraph.shape.Cylinder;
import com.ardor3d.ui.text.BasicText;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.geom.BufferUtils;
import com.ardor3d.util.resource.ResourceLocatorTool;

/**
 * A demonstration of combining the skeletal animation classes with OpenGL Shading Language.
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.pipeline.PrimitiveSkeletonExample", //
thumbnailPath = "com/ardor3d/example/media/thumbnails/pipeline_PrimitiveSkeletonExample.jpg", //
maxHeapMemory = 64)
public class PrimitiveSkeletonExample extends ExampleBase {

    private boolean runAnimation = true;
    private boolean showSkeleton = false;
    private boolean useGPU = false;

    private SkeletonPose pose1, pose2;
    private SkinnedMesh arm, arm1, arm2;
    private BasicText t1, t2, t3;

    public static void main(final String[] args) {
        ExampleBase.start(PrimitiveSkeletonExample.class);
    }

    @Override
    protected void initExample() {
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

        final Skeleton sk = new Skeleton("arm sk", new Joint[] { j1, j2 });

        pose1 = new SkeletonPose(sk);
        pose1.updateTransforms();
        pose2 = new SkeletonPose(sk);
        pose2.updateTransforms();

        arm = new SkinnedMesh("arm");
        final Cylinder cy = new Cylinder("cylinder", 3, 8, 1, 10);
        arm.setBindPoseData(cy.getMeshData());
        arm.getMeshData().setVertexBuffer(BufferUtils.createFloatBuffer(cy.getMeshData().getVertexBuffer().capacity()));
        arm.getMeshData().setNormalBuffer(BufferUtils.createFloatBuffer(cy.getMeshData().getNormalBuffer().capacity()));
        arm.getMeshData().setIndices(BufferUtils.clone(cy.getMeshData().getIndices()));
        arm.getMeshData().setTextureBuffer(BufferUtils.clone(cy.getMeshData().getTextureBuffer(0)), 0);
        arm.setTranslation(0, 0, -10);
        arm.getSceneHints().setCullHint(CullHint.Dynamic);

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

        final ShaderState gpuShader = new ShaderState();
        gpuShader.setEnabled(useGPU);
        try {
            gpuShader.setShader(ShaderType.Vertex, ResourceLocatorTool.getClassPathResourceAsString(
                    PrimitiveSkeletonExample.class, "com/ardor3d/extension/animation/skeletal/skinning_gpu.vert"));
            gpuShader.setShader(ShaderType.Fragment, ResourceLocatorTool.getClassPathResourceAsString(
                    PrimitiveSkeletonExample.class, "com/ardor3d/extension/animation/skeletal/skinning_gpu.frag"));
        } catch (final IOException ioe) {
            ioe.printStackTrace();
        }
        arm.setGPUShader(gpuShader);
        arm.setUseGPU(useGPU);

        arm1 = arm.makeCopy(true);
        arm2 = arm.makeCopy(true);

        arm1.setCurrentPose(pose1);
        arm2.setCurrentPose(pose2);

        arm1.addTranslation(1, 0, 0);
        arm2.addTranslation(-1, 0, 0);

        _root.attachChild(arm1);
        _root.attachChild(arm2);

        t1 = BasicText.createDefaultTextLabel("Text1", "[SPACE] Pause joint animation.");
        t1.getSceneHints().setRenderBucketType(RenderBucketType.Ortho);
        t1.getSceneHints().setLightCombineMode(LightCombineMode.Off);
        t1.setTranslation(new Vector3(5, 2 * (t1.getHeight() + 5) + 10, 0));
        _root.attachChild(t1);

        t2 = BasicText.createDefaultTextLabel("Text2", "[G] GPU Skinning is " + (useGPU ? "ON." : "OFF."));
        t2.getSceneHints().setRenderBucketType(RenderBucketType.Ortho);
        t2.getSceneHints().setLightCombineMode(LightCombineMode.Off);
        t2.setTranslation(new Vector3(5, 1 * (t1.getHeight() + 5) + 10, 0));
        _root.attachChild(t2);

        t3 = BasicText.createDefaultTextLabel("Text3", "[K] Show Skeleton.");
        t3.getSceneHints().setRenderBucketType(RenderBucketType.Ortho);
        t3.getSceneHints().setLightCombineMode(LightCombineMode.Off);
        t3.setTranslation(new Vector3(5, 0 * (t1.getHeight() + 5) + 10, 0));
        _root.attachChild(t3);
        _root.getSceneHints().setCullHint(CullHint.Never);

        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.K), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                showSkeleton = !showSkeleton;
                if (showSkeleton) {
                    t3.setText("[K] Hide Skeleton.");
                } else {
                    t3.setText("[K] Show Skeleon.");
                }
            }
        }));

        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.G), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                useGPU = !useGPU;
                arm1.getGPUShader().setEnabled(useGPU);
                arm1.setUseGPU(useGPU);
                arm2.getGPUShader().setEnabled(useGPU);
                arm2.setUseGPU(useGPU);
                if (useGPU) {
                    t2.setText("[G] GPU Skinning is ON.");
                } else {
                    t2.setText("[G] GPU Skinning is OFF.");
                }
            }
        }));

        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.SPACE), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                runAnimation = !runAnimation;
                if (runAnimation) {
                    t1.setText("[SPACE] Pause joint animation.");
                } else {
                    t1.setText("[SPACE] Start joint animation.");
                }
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
            final Transform t1 = pose1.getLocalJointTransforms()[1];
            t1.setTranslation(t1.getTranslation().getX(), Math.sin(angle * MathUtils.DEG_TO_RAD) * 2, t1
                    .getTranslation().getZ());

            final Transform t2 = pose2.getLocalJointTransforms()[1];
            t2.setTranslation(t2.getTranslation().getX(), Math.cos(angle * MathUtils.DEG_TO_RAD) * 2, t2
                    .getTranslation().getZ());

            pose1.updateTransforms();
            pose2.updateTransforms();

            if (!useGPU) {
                // no point to updating the model bound in gpu mode
                arm1.updateModelBound();
                arm2.updateModelBound();
            }
        }
    }
}
