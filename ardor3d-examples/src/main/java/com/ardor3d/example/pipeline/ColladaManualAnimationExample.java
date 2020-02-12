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

import java.util.List;
import java.util.concurrent.Callable;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.bounding.BoundingSphere;
import com.ardor3d.bounding.BoundingVolume;
import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.extension.animation.skeletal.Joint;
import com.ardor3d.extension.animation.skeletal.SkeletonPose;
import com.ardor3d.extension.animation.skeletal.SkinnedMesh;
import com.ardor3d.extension.animation.skeletal.util.SkeletalDebugger;
import com.ardor3d.extension.model.collada.jdom.ColladaImporter;
import com.ardor3d.extension.model.collada.jdom.data.ColladaStorage;
import com.ardor3d.extension.model.collada.jdom.data.SkinData;
import com.ardor3d.framework.Canvas;
import com.ardor3d.framework.CanvasRenderer;
import com.ardor3d.input.keyboard.Key;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyPressedCondition;
import com.ardor3d.input.logical.TriggerAction;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.light.DirectionalLight;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Transform;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyTransform;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.scenegraph.hint.LightCombineMode;
import com.ardor3d.scenegraph.shape.Sphere;
import com.ardor3d.ui.text.BasicText;
import com.ardor3d.util.GameTaskQueue;
import com.ardor3d.util.GameTaskQueueManager;
import com.ardor3d.util.ReadOnlyTimer;

/**
 * Illustrates loading a model from Collada and procedurally animating its joints.
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.pipeline.ColladaManualAnimationExample", //
        thumbnailPath = "com/ardor3d/example/media/thumbnails/pipeline_ColladaManualAnimationExample.jpg", //
        maxHeapMemory = 64)
public class ColladaManualAnimationExample extends ExampleBase {

    private static final boolean UPDATE_BOUNDS = false;
    private static final double UPDATE_RATE = 1.0 / 30.0;
    private Node colladaNode;
    private ColladaStorage colladaStorage;
    private boolean showSkeleton = false, showMesh = true;

    final Transform calcTrans1 = new Transform(), calcTrans2 = new Transform(), calcTrans3 = new Transform();
    final Quaternion calcQuat1 = new Quaternion();
    final Vector3 cameraDirection = new Vector3();
    final Vector3 forwardDirection = new Vector3();

    private Sphere ballSphere;

    private BasicText frameRateLabel;

    private int frames = 0;
    private long startTime = System.currentTimeMillis();

    public static void main(final String[] args) {
        ExampleBase.start(ColladaManualAnimationExample.class);
    }

    double time = 0.0;

    @Override
    protected void updateExample(final ReadOnlyTimer timer) {
        time += timer.getTimePerFrame();
        if (time > ColladaManualAnimationExample.UPDATE_RATE) {
            time -= ColladaManualAnimationExample.UPDATE_RATE;
            final List<SkinData> skinDataList = colladaStorage.getSkins();
            final SkinData skinData = skinDataList.get(0);
            final SkeletonPose pose = skinData.getPose();

            final double time = timer.getTimeInSeconds();
            ballSphere.setTranslation(Math.sin(time) * 5, Math.cos(time) * 5 + 10, 5);
            final ReadOnlyVector3 ballPos = ballSphere.getTranslation();

            // Neck
            targetJoint(pose, 13, Vector3.UNIT_Z, ballPos, 1.0);

            // Right arm
            targetJoint(pose, 10, new Vector3(-1, 0, 0), ballPos, 0.4);
            targetJoint(pose, 11, new Vector3(-1, 0, 0), ballPos, 0.6);
            targetJoint(pose, 12, new Vector3(-1, 0, 0), ballPos, 0.5);

            // Left arm
            targetJoint(pose, 7, new Vector3(1, 0, 0), ballPos, 0.15);
            targetJoint(pose, 8, new Vector3(1, 0, 0), ballPos, 0.15);

            // Waist
            targetJoint(pose, 5, new Vector3(0, 1, 0), ballPos, 0.1);

            pose.updateTransforms();

            if (ColladaManualAnimationExample.UPDATE_BOUNDS) {
                final List<SkinnedMesh> skins = skinData.getSkins();
                for (final SkinnedMesh skinnedMesh : skins) {
                    skinnedMesh.updateModelBound();
                }
            }
        }

        final long now = System.currentTimeMillis();
        final long dt = now - startTime;
        if (dt > 200) {
            final long fps = Math.round(1e3 * frames / dt);
            frameRateLabel.setText(fps + " fps");

            startTime = now;
            frames = 0;
        }
        frames++;
    }

    private void targetJoint(final SkeletonPose pose, final int jointIndex, final ReadOnlyVector3 bindPoseDirection,
            final ReadOnlyVector3 targetPos, final double targetStrength) {
        final Joint[] joints = pose.getSkeleton().getJoints();
        final Transform[] transforms = pose.getLocalJointTransforms();
        final Transform[] globalTransforms = pose.getGlobalJointTransforms();

        final short parentIndex = joints[jointIndex].getParentIndex();

        // neckBindGlobalTransform is the neck bone -> model space transform. essentially, it is the world transform of
        // the neck bone in bind pose.
        final ReadOnlyTransform inverseNeckBindGlobalTransform = joints[jointIndex].getInverseBindPose();
        final ReadOnlyTransform neckBindGlobalTransform = inverseNeckBindGlobalTransform.invert(calcTrans1);

        // Get a vector representing forward direction in neck space, use inverse to take from world -> neck space.
        forwardDirection.set(bindPoseDirection);
        inverseNeckBindGlobalTransform.applyForwardVector(forwardDirection, forwardDirection);

        // Get a vector representing a direction to the camera in neck space.
        targetPos.subtract(globalTransforms[jointIndex].getTranslation(), cameraDirection);
        cameraDirection.normalizeLocal();
        inverseNeckBindGlobalTransform.applyForwardVector(cameraDirection, cameraDirection);

        // Calculate a rotation to go from one direction to the other and set that rotation on a blank transform.
        calcQuat1.fromVectorToVector(forwardDirection, cameraDirection);
        calcQuat1.slerpLocal(Quaternion.IDENTITY, calcQuat1, targetStrength);

        final Transform subTransform = calcTrans2.setIdentity();
        subTransform.setRotation(calcQuat1);

        // Calculate a global version of that transform, as if it were attached to the neck
        final Transform subGlobal = neckBindGlobalTransform.multiply(subTransform, calcTrans3);

        // now remove the global/world transform of the neck's parent bone, leaving us with just the local transform of
        // neck + rotation.
        final Transform local = joints[parentIndex].getInverseBindPose().multiply(subGlobal, calcTrans2);

        // set that as the neck's transform
        transforms[jointIndex].set(local);
    }

    @Override
    protected void initExample() {
        _canvas.setTitle("Ardor3D - Collada Import - Manual Animation");

        _lightState.detachAll();
        final DirectionalLight light = new DirectionalLight();
        light.setDiffuse(new ColorRGBA(0.75f, 0.75f, 0.75f, 0.75f));
        light.setAmbient(new ColorRGBA(0.25f, 0.25f, 0.25f, 1.0f));
        light.setDirection(new Vector3(-1, -1, -1).normalizeLocal());
        light.setEnabled(true);
        _lightState.attach(light);

        // Load collada model
        try {
            final long time = System.currentTimeMillis();
            final ColladaImporter colladaImporter = new ColladaImporter();

            colladaStorage = colladaImporter.load("collada/sony/Seymour.dae");
            colladaNode = colladaStorage.getScene();
            colladaNode.setRenderMaterial("unlit/textured/basic.yaml");

            System.err.println("took " + (System.currentTimeMillis() - time) + " ms");

            // TODO temp camera positioning until reading camera instances...
            positionCamera();

            _root.attachChild(colladaNode);
        } catch (final Exception ex) {
            ex.printStackTrace();
        }

        ballSphere = new Sphere("Ball", Vector3.ZERO, 32, 32, 0.5);
        _root.attachChild(ballSphere);

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

        final BasicText t1 = BasicText.createDefaultTextLabel("Text1", "[K] Show Skeleton.");
        t1.getSceneHints().setRenderBucketType(RenderBucketType.OrthoOrder);
        t1.getSceneHints().setLightCombineMode(LightCombineMode.Off);
        t1.setTranslation(new Vector3(5, 0 * (t1.getHeight() + 5) + 10, 0));
        _orthoRoot.attachChild(t1);
        _orthoRoot.getSceneHints().setCullHint(CullHint.Never);

        final BasicText t2 = BasicText.createDefaultTextLabel("Text2", "[M] Hide Mesh.");
        t2.getSceneHints().setRenderBucketType(RenderBucketType.OrthoOrder);
        t2.getSceneHints().setLightCombineMode(LightCombineMode.Off);
        t2.setTranslation(new Vector3(5, 1 * (t1.getHeight() + 5) + 10, 0));
        _orthoRoot.attachChild(t2);

        _root.getSceneHints().setCullHint(CullHint.Never);

        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.K), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                showSkeleton = !showSkeleton;
                if (showSkeleton) {
                    t1.setText("[K] Hide Skeleton.");
                } else {
                    t1.setText("[K] Show Skeleon.");
                }
            }
        }));

        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.M), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                showMesh = !showMesh;
                colladaNode.getSceneHints().setCullHint(showMesh ? CullHint.Dynamic : CullHint.Always);
                if (showMesh) {
                    t2.setText("[M] Hide Mesh.");
                } else {
                    t2.setText("[M] Show Mesh.");
                }
            }
        }));

        // Add fps display
        frameRateLabel = BasicText.createDefaultTextLabel("fpsLabel", "");
        frameRateLabel.setTranslation(5,
                _canvas.getCanvasRenderer().getCamera().getHeight() - 5 - frameRateLabel.getHeight(), 0);
        frameRateLabel.setTextColor(ColorRGBA.WHITE);
        frameRateLabel.getSceneHints().setOrthoOrder(-1);
        _root.attachChild(frameRateLabel);
    }

    private void positionCamera() {
        colladaNode.updateGeometricState(0.0);
        final BoundingVolume bounding = colladaNode.getWorldBound();
        if (bounding != null) {
            final ReadOnlyVector3 center = bounding.getCenter();
            double radius = 0;
            if (bounding instanceof BoundingSphere) {
                radius = ((BoundingSphere) bounding).getRadius();
            } else if (bounding instanceof BoundingBox) {
                final BoundingBox boundingBox = (BoundingBox) bounding;
                radius = Math.max(Math.max(boundingBox.getXExtent(), boundingBox.getYExtent()),
                        boundingBox.getZExtent());
            }

            final Vector3 vec = new Vector3(center);
            vec.addLocal(radius * 2, radius * 1, radius * 2.5);

            _canvas.getCanvasRenderer().getCamera().setLocation(vec);
            _canvas.getCanvasRenderer().getCamera().lookAt(center, Vector3.UNIT_Y);
            _canvas.getCanvasRenderer().getCamera()
                    .setFrustumPerspective(50.0,
                            (float) _canvas.getCanvasRenderer().getCamera().getWidth()
                                    / _canvas.getCanvasRenderer().getCamera().getHeight(),
                            1.0f, Math.max(radius * 3, 10000.0));

            _controlHandle.setMoveSpeed(radius / 1.0);
        }
    }

    @Override
    protected void renderDebug(final Renderer renderer) {
        super.renderDebug(renderer);

        if (showSkeleton) {
            SkeletalDebugger.drawSkeletons(_root, renderer);
        }
    }
}