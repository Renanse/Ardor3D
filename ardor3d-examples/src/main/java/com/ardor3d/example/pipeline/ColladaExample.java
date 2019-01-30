/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example.pipeline;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.Callable;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.bounding.BoundingSphere;
import com.ardor3d.bounding.BoundingVolume;
import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.extension.animation.skeletal.AnimationManager;
import com.ardor3d.extension.animation.skeletal.blendtree.ClipSource;
import com.ardor3d.extension.animation.skeletal.blendtree.SimpleAnimationApplier;
import com.ardor3d.extension.animation.skeletal.clip.AnimationClip;
import com.ardor3d.extension.animation.skeletal.state.SteadyState;
import com.ardor3d.extension.animation.skeletal.util.SkeletalDebugger;
import com.ardor3d.extension.model.collada.jdom.ColladaImporter;
import com.ardor3d.extension.model.collada.jdom.data.ColladaStorage;
import com.ardor3d.extension.model.collada.jdom.data.SkinData;
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
import com.ardor3d.light.DirectionalLight;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.scenegraph.hint.LightCombineMode;
import com.ardor3d.ui.text.BasicText;
import com.ardor3d.util.GameTaskQueue;
import com.ardor3d.util.GameTaskQueueManager;
import com.ardor3d.util.MaterialUtil;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.ResourceSource;
import com.ardor3d.util.resource.URLResourceSource;

/**
 * Illustrates loading a model from Collada. If the model also contains an animation, the animation is played as well.
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.pipeline.ColladaExample", //
        thumbnailPath = "com/ardor3d/example/media/thumbnails/pipeline_ColladaExample.jpg", //
        maxHeapMemory = 128)
public class ColladaExample extends ExampleBase {

    private Node colladaNode;
    private boolean showSkeleton = false, showJointLabels = false;

    private UILabel frameRateLabel;
    private UIHud hud;

    private int frames = 0;
    private long startTime = System.currentTimeMillis();

    private List<File> daeFiles;
    private int fileIndex = 0;

    private AnimationManager manager;
    private List<SkinData> skinDatas;

    public static void main(final String[] args) {
        ExampleBase.start(ColladaExample.class);
    }

    @Override
    protected void initExample() {
        _canvas.setTitle("Ardor3D - Collada Example");

        _lightState.detachAll();
        final DirectionalLight light = new DirectionalLight();
        light.setDiffuse(new ColorRGBA(1, 1, 1, 1));
        light.setAmbient(new ColorRGBA(0.9f, 0.9f, 0.9f, 1));
        light.setDirection(new Vector3(-1, -1, -1).normalizeLocal());
        light.setEnabled(true);
        _lightState.attach(light);

        // Load collada model
        loadColladaModel(
                ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_MODEL, "collada/sony/Seymour.dae"));

        final File rootDir = new File(".");
        daeFiles = findFiles(rootDir, ".dae", null);

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

        // Create our options frame and fps label
        createHUD();
    }

    private void createHUD() {
        final BasicText t1 = BasicText.createDefaultTextLabel("Text1", "Seymour.dae");
        t1.getSceneHints().setRenderBucketType(RenderBucketType.OrthoOrder);
        t1.getSceneHints().setLightCombineMode(LightCombineMode.Off);
        t1.setTranslation(new Vector3(5, 0 * (t1.getHeight() + 5) + 10, 0));
        _orthoRoot.attachChild(t1);
        _orthoRoot.getSceneHints().setCullHint(CullHint.Never);
        _root.getSceneHints().setCullHint(CullHint.Never);

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

        final UIButton loadSceneButton = new UIButton("Load next scene");
        loadSceneButton.setLayoutData(new AnchorLayoutData(Alignment.TOP_LEFT, basePanel, Alignment.TOP_LEFT, 5, -5));
        loadSceneButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent event) {
                final File file = daeFiles.get(fileIndex);
                try {
                    loadColladaModel(new URLResourceSource(file.toURI().toURL()));
                    t1.setText(file.getName());
                } catch (final MalformedURLException e) {
                    e.printStackTrace();
                }
                fileIndex = (fileIndex + 1) % daeFiles.size();
            }
        });
        basePanel.add(loadSceneButton);

        final UICheckBox skinCheck = new UICheckBox("Show skin mesh");
        skinCheck
                .setLayoutData(new AnchorLayoutData(Alignment.TOP_LEFT, loadSceneButton, Alignment.BOTTOM_LEFT, 0, -5));
        skinCheck.setSelected(true);
        skinCheck.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent event) {
                colladaNode.getSceneHints().setCullHint(skinCheck.isSelected() ? CullHint.Dynamic : CullHint.Always);
            }
        });
        basePanel.add(skinCheck);

        final UICheckBox skeletonCheck = new UICheckBox("Show skeleton");
        final UICheckBox boneLabelCheck = new UICheckBox("Show joint labels");
        skeletonCheck.setLayoutData(new AnchorLayoutData(Alignment.TOP_LEFT, skinCheck, Alignment.BOTTOM_LEFT, 0, -5));
        skeletonCheck.setSelected(showSkeleton);
        skeletonCheck.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent event) {
                showSkeleton = skeletonCheck.isSelected();
                boneLabelCheck.setEnabled(showSkeleton);
            }
        });
        basePanel.add(skeletonCheck);

        boneLabelCheck
                .setLayoutData(new AnchorLayoutData(Alignment.TOP_LEFT, skeletonCheck, Alignment.BOTTOM_LEFT, 0, -5));
        boneLabelCheck.setSelected(false);
        boneLabelCheck.setEnabled(showSkeleton);
        boneLabelCheck.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent event) {
                showJointLabels = boneLabelCheck.isSelected();
            }
        });
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

    private void loadColladaModel(final ResourceSource source) {
        try {
            // detach the old colladaNode, if present.
            _root.detachChild(colladaNode);

            final long time = System.currentTimeMillis();
            final ColladaImporter colladaImporter = new ColladaImporter();

            // Load the collada scene
            final ColladaStorage storage = colladaImporter.load(source);
            colladaNode = storage.getScene();

            setupSkins(storage);
            setupAnimations(storage);

            System.out.println("Importing: " + source);
            System.out.println("Took " + (System.currentTimeMillis() - time) + " ms");

            // Add colladaNode to root
            _root.attachChild(colladaNode);
            MaterialUtil.autoMaterials(colladaNode);

            // Setup camera
            ReadOnlyVector3 upAxis = Vector3.UNIT_Y;
            if (storage.getAssetData().getUpAxis() != null) {
                upAxis = storage.getAssetData().getUpAxis();
            }

            positionCamera(upAxis);
        } catch (final Exception ex) {
            ex.printStackTrace();
        }
    }

    private void setupSkins(final ColladaStorage storage) {
        skinDatas = storage.getSkins();
    }

    private void setupAnimations(final ColladaStorage storage) {
        // Check if there is any animationdata in the file
        if (storage.getAnimationChannels().isEmpty() || storage.getSkins().isEmpty()) {
            return;
        }

        // Make our manager
        manager = new AnimationManager(_timer, skinDatas.get(0).getPose());

        final AnimationClip clipA = storage.extractChannelsAsClip("clipA");

        // Set some clip instance specific data - repeat, time scaling
        manager.getClipInstance(clipA).setLoopCount(Integer.MAX_VALUE);

        // Add our "applier logic".
        manager.setApplier(new SimpleAnimationApplier());

        // Add our clip as a state in the default animation layer
        final SteadyState animState = new SteadyState("anim_state");
        animState.setSourceTree(new ClipSource(clipA, manager));
        manager.getBaseAnimationLayer().addSteadyState(animState);

        // Set the current animation state on default layer
        manager.getBaseAnimationLayer().setCurrentState("anim_state", true);
    }

    private void positionCamera(final ReadOnlyVector3 upAxis) {
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
            // XXX: a bit of a hack
            if (upAxis.equals(Vector3.UNIT_Z)) {
                vec.addLocal(0.0, -radius * 2, 0.0);
            } else {
                vec.addLocal(0.0, 0.0, radius * 2);
            }

            _controlHandle.setUpAxis(upAxis);

            final Camera cam = _canvas.getCanvasRenderer().getCamera();
            cam.setLocation(vec);
            cam.lookAt(center, upAxis);
            final double near = Math.max(radius / 50.0, 0.25);
            final double far = Math.min(radius * 5, 10000.0);
            cam.setFrustumPerspective(50.0, cam.getWidth() / (double) cam.getHeight(), near, far);

            _controlHandle.setMoveSpeed(radius / 1.0);
        }
    }

    @Override
    protected void updateLogicalLayer(final ReadOnlyTimer timer) {
        hud.getLogicalLayer().checkTriggers(timer.getTimePerFrame());
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

        if (manager != null) {
            manager.update();
        }
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

    private List<File> findFiles(final File rootDir, final String name, List<File> fileList) {
        if (fileList == null) {
            fileList = new ArrayList<File>();
        }
        final File[] files = rootDir.listFiles();
        for (int i = 0; i < files.length; i++) {
            final File file = files[i];
            if (file.isDirectory()) {
                findFiles(file, name, fileList);
            } else if (file.getName().toLowerCase().endsWith(name)) {
                fileList.add(file);
            }
        }
        return fileList;
    }
}