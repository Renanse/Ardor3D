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
import java.util.List;
import java.util.Map;

import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.extension.animation.skeletal.AnimationManager;
import com.ardor3d.extension.animation.skeletal.SkeletonPose;
import com.ardor3d.extension.animation.skeletal.SkinnedMesh;
import com.ardor3d.extension.animation.skeletal.SkinnedMeshCombineLogic;
import com.ardor3d.extension.animation.skeletal.blendtree.SimpleAnimationApplier;
import com.ardor3d.extension.animation.skeletal.clip.AnimationClip;
import com.ardor3d.extension.animation.skeletal.state.loader.InputStore;
import com.ardor3d.extension.animation.skeletal.state.loader.JSLayerImporter;
import com.ardor3d.extension.animation.skeletal.util.MissingCallback;
import com.ardor3d.extension.model.collada.jdom.ColladaImporter;
import com.ardor3d.extension.model.collada.jdom.data.ColladaStorage;
import com.ardor3d.extension.model.util.nvtristrip.NvTriangleStripper;
import com.ardor3d.framework.NativeCanvas;
import com.ardor3d.light.DirectionalLight;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.state.CullState;
import com.ardor3d.renderer.state.CullState.Face;
import com.ardor3d.renderer.state.GLSLShaderObjectsState;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.hint.DataMode;
import com.ardor3d.scenegraph.visitor.Visitor;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.geom.MeshCombiner;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.ResourceSource;
import com.ardor3d.util.resource.URLResourceSource;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Illustrates loading several animations from Collada and arranging them in an animation state machine.
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.pipeline.AnimationDemoExample", //
thumbnailPath = "com/ardor3d/example/media/thumbnails/pipeline_AnimationDemoExample.jpg", //
maxHeapMemory = 64)
public class AnimationDemoExample extends ExampleBase {

    private static final long MIN_STATE_TIME = 5000;

    static AnimationDemoExample instance;

    private final List<AnimationManager> managers = Lists.newArrayList();
    private final List<AnimationInfo> animInfo = Lists.newArrayList();
    private final Map<SkeletonPose, SkinnedMesh> poseToMesh = Maps.newIdentityHashMap();

    public static void main(final String[] args) {
        ExampleBase.start(AnimationDemoExample.class);
    }

    public AnimationDemoExample() {
        instance = this;
    }

    @Override
    protected void initExample() {
        _canvas.setTitle("Ardor3D - Animation Demo Example - Skeletons Patrol!");
        _canvas.getCanvasRenderer().getRenderer().setBackgroundColor(ColorRGBA.GRAY);

        // set camera
        final Camera cam = _canvas.getCanvasRenderer().getCamera();
        cam.setLocation(197, 113, -126);
        cam.lookAt(new Vector3(157, 91, -174), Vector3.UNIT_Y);
        cam.setFrustumPerspective(45.0, cam.getWidth() / (double) cam.getHeight(), .25, 900);
        cam.update();

        // speed up wasd control a little
        _controlHandle.setMoveSpeed(200);

        _lightState.detachAll();
        final DirectionalLight light = new DirectionalLight();
        light.setDiffuse(new ColorRGBA(0.75f, 0.75f, 0.75f, 0.75f));
        light.setAmbient(new ColorRGBA(0.25f, 0.25f, 0.25f, 1.0f));
        light.setDirection(new Vector3(-1, -1, -1).normalizeLocal());
        light.setEnabled(true);
        _lightState.attach(light);

        final SkinnedMesh skeleton = loadMainSkeleton();

        // Load collada model
        for (int i = 0; i < 10; i++) {
            final SkinnedMesh copy = skeleton.makeCopy(true);
            copy.setCurrentPose(skeleton.getCurrentPose().makeCopy());
            copy.setTranslation(((i % 5) - 2) * 60, 0, ((i / 5) * 40) - 320);
            _root.attachChild(copy);
            final AnimationManager manager = createAnimationManager(copy.getCurrentPose());
            managers.add(manager);
            animInfo.add(new AnimationInfo());
            poseToMesh.put(copy.getCurrentPose(), copy);
        }
    }

    private SkinnedMesh loadMainSkeleton() {
        SkinnedMesh skeleton = null;
        try {
            final long time = System.currentTimeMillis();
            final ColladaImporter colladaImporter = new ColladaImporter();

            // OPTIMIZATION: run GeometryTool on collada meshes to reduce redundant vertices...
            colladaImporter.setOptimizeMeshes(true);

            // Load the collada scene
            final String mainFile = "collada/skeleton/skeleton.walk.dae";
            final ColladaStorage storage = colladaImporter.load(mainFile);
            final Node colladaNode = storage.getScene();

            System.out.println("Importing: " + mainFile);
            System.out.println("Took " + (System.currentTimeMillis() - time) + " ms");

            final GLSLShaderObjectsState gpuShader = new GLSLShaderObjectsState();
            gpuShader.setEnabled(true);
            try {
                gpuShader.setVertexShader(ResourceLocatorTool.getClassPathResourceAsStream(AnimationDemoExample.class,
                        "com/ardor3d/extension/animation/skeletal/skinning_gpu_texture.vert"));
                gpuShader.setFragmentShader(ResourceLocatorTool.getClassPathResourceAsStream(
                        AnimationDemoExample.class,
                        "com/ardor3d/extension/animation/skeletal/skinning_gpu_texture.frag"));

                gpuShader.setUniform("texture", 0);
                gpuShader.setUniform("lightDirection", new Vector3(1, 1, 1).normalizeLocal());
            } catch (final IOException ioe) {
                ioe.printStackTrace();
            }

            // OPTIMIZATION: SkinnedMesh combining... Useful in our case because the skeleton model is composed of 2
            // separate meshes.
            skeleton = (SkinnedMesh) MeshCombiner.combine(colladaNode, new SkinnedMeshCombineLogic());
            // Non-combined:
            // primeModel = colladaNode;

            // OPTIMIZATION: turn on the buffers in our skeleton so they can be shared. (reuse ids)
            skeleton.acceptVisitor(new Visitor() {
                @Override
                public void visit(final Spatial spatial) {
                    if (spatial instanceof SkinnedMesh) {
                        final SkinnedMesh skinnedSpatial = (SkinnedMesh) spatial;
                        skinnedSpatial.recreateWeightAttributeBuffer();
                        skinnedSpatial.recreateJointAttributeBuffer();
                    }
                }
            }, true);

            // OPTIMIZATION: run nv strippifyier on model...
            final NvTriangleStripper stripper = new NvTriangleStripper();
            stripper.setReorderVertices(true);
            skeleton.acceptVisitor(stripper, true);

            // OPTIMIZATION: don't draw surfaces that face away from the camera...
            final CullState cullState = new CullState();
            cullState.setCullFace(Face.Back);
            skeleton.setRenderState(cullState);

            skeleton.getSceneHints().setDataMode(DataMode.VBO);
            gpuShader.setUseAttributeVBO(true);
            skeleton.setGPUShader(gpuShader);
            skeleton.setUseGPU(true);

        } catch (final Exception ex) {
            ex.printStackTrace();
        }
        return skeleton;
    }

    private final Map<String, AnimationClip> animationStore = Maps.newHashMap();

    private AnimationManager createAnimationManager(final SkeletonPose pose) {
        // Make our manager
        final AnimationManager manager = new AnimationManager(_timer, pose);

        // Add our "applier logic".
        final SimpleAnimationApplier applier = new SimpleAnimationApplier();
        manager.setApplier(applier);

        // Add a call back to load clips.
        final InputStore input = new InputStore();
        input.getClips().setMissCallback(new MissingCallback<String, AnimationClip>() {
            public AnimationClip getValue(final String key) {
                if (!animationStore.containsKey(key)) {
                    try {
                        final ColladaStorage storage1 = new ColladaImporter().load("collada/skeleton/" + key + ".dae");
                        animationStore.put(key, storage1.extractChannelsAsClip(key));
                    } catch (final IOException e) {
                        e.printStackTrace();
                        animationStore.put(key, null);
                    }
                }
                return animationStore.get(key);
            }
        });

        // Load our layer and states from script
        try {
            final ResourceSource layersFile = new URLResourceSource(ResourceLocatorTool.getClassPathResource(
                    AnimationDemoExample.class, "com/ardor3d/example/pipeline/AnimationDemoExample.js"));
            JSLayerImporter.addLayers(layersFile, manager, input);
        } catch (final Exception e) {
            e.printStackTrace();
        }

        // kick things off by setting our starting state
        manager.getBaseAnimationLayer().setCurrentState("walk_anim", true);

        return manager;
    }

    @Override
    protected void updateExample(final ReadOnlyTimer timer) {
        for (int i = 0, max = managers.size(); i < max; i++) {
            final AnimationManager manager = managers.get(i);
            final AnimationInfo info = animInfo.get(i);
            if (System.currentTimeMillis() - info.lastStateChange > MIN_STATE_TIME) {
                if (Math.random() < 0.001) {
                    manager.getBaseAnimationLayer().doTransition(info.running ? "walk" : "run");
                    info.running = !info.running;
                    info.lastStateChange = System.currentTimeMillis();
                    manager.findAnimationLayer("punch").setCurrentState("punch_right", true);
                }
            }
            manager.update();
        }
    }

    public NativeCanvas getCanvas() {
        return _canvas;
    }

    public Node getRoot() {
        return _root;
    }

    class AnimationInfo {
        boolean running = false;
        long lastStateChange = 0;
    }

    public SkinnedMesh getMeshForPose(final SkeletonPose applyToPose) {
        return poseToMesh.get(applyToPose);
    }
}