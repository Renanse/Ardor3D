/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.example.effect;

import java.util.EnumSet;
import java.util.concurrent.Callable;

import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.extension.effect.EffectUtils;
import com.ardor3d.extension.effect.particle.ParticleFactory;
import com.ardor3d.extension.effect.particle.ParticleSystem;
import com.ardor3d.extension.effect.particle.emitter.MeshEmitter;
import com.ardor3d.extension.ui.Orientation;
import com.ardor3d.extension.ui.UICheckBox;
import com.ardor3d.extension.ui.UIFrame;
import com.ardor3d.extension.ui.UIFrame.FrameButtons;
import com.ardor3d.extension.ui.UIHud;
import com.ardor3d.extension.ui.UILabel;
import com.ardor3d.extension.ui.UIPanel;
import com.ardor3d.extension.ui.UISlider;
import com.ardor3d.extension.ui.event.ActionEvent;
import com.ardor3d.extension.ui.event.ActionListener;
import com.ardor3d.extension.ui.layout.RowLayout;
import com.ardor3d.extension.ui.util.Insets;
import com.ardor3d.framework.Canvas;
import com.ardor3d.framework.CanvasRenderer;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture.WrapMode;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.input.control.FirstPersonControl;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.MouseMovedCondition;
import com.ardor3d.input.logical.TriggerAction;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.input.mouse.MouseState;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Plane;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Ray3;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.scenegraph.shape.Arrow;
import com.ardor3d.scenegraph.shape.Disk;
import com.ardor3d.util.GameTaskQueue;
import com.ardor3d.util.GameTaskQueueManager;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureManager;

/**
 * Another particle demonstration, this one showing a smoking rocket following the cursor around the screen.
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.effect.NewDynamicSmokerExample", //
        thumbnailPath = "com/ardor3d/example/media/thumbnails/effect_NewDynamicSmokerExample.jpg", //
        maxHeapMemory = 64)
public class NewDynamicSmokerExample extends ExampleBase {

    private static double ROCKET_TURN_SPEED = 5;
    private static double ROCKET_PROPEL_SPEED = 100;

    private ParticleSystem smoke;
    private final Node rocketEntityNode = new Node("rocket-node");
    private final Vector2 mouseLoc = new Vector2();
    private final Vector3 worldStore = new Vector3();
    private final Ray3 ray = new Ray3();
    private final Plane rocketPlane = new Plane(Vector3.NEG_UNIT_Z, 0);
    private UIHud hud;
    private boolean stayIn2DPlane = false;
    private boolean particleInWorld = true;
    private int age = 3000;

    public static void main(final String[] args) {
        start(NewDynamicSmokerExample.class);
    }

    @Override
    protected void initExample() {
        EffectUtils.addDefaultResourceLocators();

        _canvas.setTitle("Smoking Rocket");
        final CanvasRenderer canvasRenderer = _canvas.getCanvasRenderer();
        final RenderContext renderContext = canvasRenderer.getRenderContext();
        final Renderer renderer = canvasRenderer.getRenderer();
        GameTaskQueueManager.getManager(renderContext).getQueue(GameTaskQueue.RENDER).enqueue(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                renderer.setBackgroundColor(ColorRGBA.BLUE);
                return null;
            }
        });

        // set our camera at a fixed position
        final Camera cam = _canvas.getCanvasRenderer().getCamera();
        cam.setLocation(0, 0, 300);

        // add our "rocket"
        buildRocket();

        // add smoke to the end of the rocket
        addEngineSmoke();

        // add some ui for changing rocket properties.
        addUI();

        // set initial mouse position to near center
        mouseLoc.set(cam.getWidth() / 2 + 0.1, cam.getHeight() / 2 + 0.1);
    }

    @Override
    protected void registerInputTriggers() {
        super.registerInputTriggers();
        // drop WASD control
        FirstPersonControl.removeTriggers(_logicalLayer, _controlHandle);

        _logicalLayer.registerTrigger(new InputTrigger(new MouseMovedCondition(), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                final MouseState mouse = inputStates.getCurrent().getMouseState();
                mouseLoc.set(mouse.getX(), mouse.getY());
            }
        }));
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
    public void update(final ReadOnlyTimer timer) {
        super.update(timer);
        hud.updateGeometricState(timer.getTimePerFrame());

        // Update rocket
        updateRocket(timer.getTimePerFrame());
    }

    private void updateRocket(double tpf) {
        // keep it sane.
        if (tpf > .1) {
            tpf = .1;
        }
        // find mouse location in world coords
        _canvas.getCanvasRenderer().getCamera().getPickRay(mouseLoc, false, ray);
        ray.intersectsPlane(rocketPlane, worldStore);

        // get rocket's current orientation as quat
        final Quaternion currentOrient = new Quaternion().fromRotationMatrix(rocketEntityNode.getWorldRotation());

        // get orientation that points rocket nose straight at mouse
        final Vector3 dirTowardsMouse = new Vector3(worldStore).subtractLocal(rocketEntityNode.getWorldTranslation())
                .normalizeLocal();
        final Quaternion targetOrient = new Quaternion().fromVectorToVector(Vector3.NEG_UNIT_Z, dirTowardsMouse);

        // get a scale representing choice between direction of old and new quats
        final double scale = currentOrient.dot(targetOrient) * tpf * ROCKET_TURN_SPEED;
        currentOrient.addLocal(targetOrient.multiplyLocal(scale)).normalizeLocal();

        rocketEntityNode.setRotation(currentOrient);

        // propel forward
        rocketEntityNode
                .addTranslation(currentOrient.apply(Vector3.NEG_UNIT_Z, null).multiplyLocal(ROCKET_PROPEL_SPEED * tpf));

        if (stayIn2DPlane) {
            rocketEntityNode.setTranslation(rocketEntityNode.getTranslation().getX(),
                    rocketEntityNode.getTranslation().getY(), 0);
        }
    }

    private void buildRocket() {
        final Arrow rocket = new Arrow("rocket", 5, 2);
        rocket.setRotation(new Quaternion().fromAngleAxis(MathUtils.DEG_TO_RAD * -90, Vector3.UNIT_X));
        rocketEntityNode.attachChild(rocket);
        _root.attachChild(rocketEntityNode);
        rocket.setRenderMaterial("unlit/untextured/basic.yaml");
    }

    private void addEngineSmoke() {
        final Disk emitDisc = new Disk("disc", 6, 6, 1.5f);
        emitDisc.setTranslation(new Vector3(0, 0, 2.5));
        emitDisc.getSceneHints().setCullHint(CullHint.Always);
        rocketEntityNode.attachChild(emitDisc);

        smoke = ParticleFactory.buildParticles("particles", 250);
        smoke.setEmissionDirection(new Vector3(0f, 0f, 1f));
        smoke.setMaximumAngle(0.0f);
        smoke.setSpeed(1.0f);
        smoke.setMinimumLifeTime(age / 2);
        smoke.setMaximumLifeTime(age);
        smoke.setStartSize(1.0f);
        smoke.setEndSize(12.0f);
        smoke.setStartColor(new ColorRGBA(1.0f, 1.0f, 1.0f, 1.0f));
        smoke.setEndColor(new ColorRGBA(.22f, .2f, .18f, 0.0f));
        smoke.setInitialVelocity(0.03f);
        smoke.setParticleEmitter(new MeshEmitter(emitDisc, false));
        smoke.setRotateWithScene(true);

        final BlendState blend = new BlendState();
        blend.setBlendEnabled(true);
        blend.setSourceFunction(BlendState.SourceFunction.SourceAlpha);
        blend.setDestinationFunction(BlendState.DestinationFunction.OneMinusSourceAlpha);
        smoke.setRenderState(blend);

        final TextureState ts = new TextureState();
        ts.setTexture(TextureManager.load("images/flare.png", Texture.MinificationFilter.Trilinear,
                TextureStoreFormat.GuessNoCompressedFormat, true));
        ts.getTexture().setWrap(WrapMode.BorderClamp);
        ts.setEnabled(true);
        smoke.setRenderState(ts);

        final ZBufferState zstate = new ZBufferState();
        zstate.setWritable(false);
        smoke.setRenderState(zstate);
        rocketEntityNode.attachChild(smoke);
    }

    private void addUI() {
        // setup hud
        hud = new UIHud(_canvas);
        hud.setupInput(_physicalLayer, _logicalLayer);
        hud.setMouseManager(_mouseManager);

        final UIFrame frame = new UIFrame("Controls", EnumSet.noneOf(FrameButtons.class));
        frame.setResizeable(false);

        final UILabel turnLabel = new UILabel("Turn Speed: " + ROCKET_TURN_SPEED);
        final UISlider turnSlider = new UISlider(Orientation.Horizontal, 0, 250, (int) (ROCKET_TURN_SPEED * 10));
        turnSlider.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent event) {
                ROCKET_TURN_SPEED = turnSlider.getValue() / 10;
                turnLabel.setText("Turn Speed: " + ROCKET_TURN_SPEED);
            }
        });

        final UILabel propelLabel = new UILabel("Propel Speed: " + ROCKET_PROPEL_SPEED);
        final UISlider propelSlider = new UISlider(Orientation.Horizontal, 0, 250, (int) (ROCKET_PROPEL_SPEED));
        propelSlider.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent event) {
                ROCKET_PROPEL_SPEED = propelSlider.getValue();
                propelLabel.setText("Propel Speed: " + ROCKET_PROPEL_SPEED);
            }
        });

        final UILabel ageLabel = new UILabel("Max Age of Smoke: " + age + " ms");
        final UISlider ageSlider = new UISlider(Orientation.Horizontal, 25, 400, age / 10);
        ageSlider.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent event) {
                age = ageSlider.getValue() * 10;
                ageLabel.setText("Max Age of Smoke: " + age + " ms");
                smoke.setMaximumLifeTime(age);
                smoke.setMinimumLifeTime(age / 2);
            }
        });

        final UICheckBox twoDimCheck = new UICheckBox("Constrain Rocket to 2D Plane.");
        twoDimCheck.setSelected(stayIn2DPlane);
        twoDimCheck.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent event) {
                stayIn2DPlane = twoDimCheck.isSelected();
            }
        });

        final UICheckBox worldCoordsCheck = new UICheckBox("Particles are in world coords");
        worldCoordsCheck.setSelected(particleInWorld);
        worldCoordsCheck.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent event) {
                particleInWorld = worldCoordsCheck.isSelected();
                smoke.setParticlesInWorldCoords(particleInWorld);
            }
        });

        final UIPanel panel = new UIPanel(new RowLayout(false, true, false));
        panel.setPadding(new Insets(10, 20, 10, 20));
        panel.add(turnLabel);
        panel.add(turnSlider);
        panel.add(propelLabel);
        panel.add(propelSlider);
        panel.add(ageLabel);
        panel.add(ageSlider);
        panel.add(twoDimCheck);
        panel.add(worldCoordsCheck);

        frame.setContentPanel(panel);
        frame.pack();
        frame.setLocalXY(hud.getWidth() - frame.getLocalComponentWidth(),
                hud.getHeight() - frame.getLocalComponentHeight());
        hud.add(frame);
    }
}
