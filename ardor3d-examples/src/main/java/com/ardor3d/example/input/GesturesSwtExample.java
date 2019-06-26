/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example.input;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.lwjgl.opengl.swt.GLData;

import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.benchmark.ball.Ball;
import com.ardor3d.example.benchmark.ball.BallSprite;
import com.ardor3d.framework.BasicScene;
import com.ardor3d.framework.Canvas;
import com.ardor3d.framework.CanvasRenderer;
import com.ardor3d.framework.FrameHandler;
import com.ardor3d.framework.Updater;
import com.ardor3d.framework.lwjgl3.Lwjgl3CanvasRenderer;
import com.ardor3d.framework.lwjgl3.swt.Lwjgl3SwtCanvas;
import com.ardor3d.image.Texture;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.image.util.awt.AWTImageLoader;
import com.ardor3d.input.PhysicalLayer;
import com.ardor3d.input.character.CharacterInputWrapper;
import com.ardor3d.input.gesture.event.AbstractGestureEvent;
import com.ardor3d.input.gesture.event.LongPressGestureEvent;
import com.ardor3d.input.gesture.event.PanGestureEvent;
import com.ardor3d.input.gesture.event.PinchGestureEvent;
import com.ardor3d.input.gesture.event.RotateGestureEvent;
import com.ardor3d.input.gesture.event.SwipeGestureEvent;
import com.ardor3d.input.gesture.touch.LongPressInterpreter;
import com.ardor3d.input.gesture.touch.PanInterpreter;
import com.ardor3d.input.gesture.touch.PinchInterpreter;
import com.ardor3d.input.gesture.touch.RotateInterpreter;
import com.ardor3d.input.gesture.touch.SwipeInterpreter;
import com.ardor3d.input.keyboard.KeyboardWrapper;
import com.ardor3d.input.logical.GestureEventCondition;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.input.logical.TriggerAction;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.input.swt.SwtFocusWrapper;
import com.ardor3d.input.swt.SwtGestureWrapper;
import com.ardor3d.input.swt.SwtKeyboardWrapper;
import com.ardor3d.input.swt.SwtMouseWrapper;
import com.ardor3d.math.MathUtils;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.lwjgl3.Lwjgl3CanvasCallback;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.Timer;

public class GesturesSwtExample implements Updater {

    // private static final Logger logger = Logger.getLogger(GesturesSwtExample.class.toString());
    private static GesturesSwtExample game;

    private final BasicScene scene;
    private final LogicalLayer logicalLayer;

    private final List<BallSprite> balls = new ArrayList<>();

    private boolean inited;

    private Lwjgl3SwtCanvas _canvas;

    private double ballScale = 1.0, workScale = 1.0;

    private double timeScale = 1;
    private double currentRot = MathUtils.HALF_PI;
    private final double ROT_SCALE = 10 / MathUtils.PI;

    public GesturesSwtExample(final BasicScene scene, final LogicalLayer logicalLayer) {
        this.scene = scene;
        this.logicalLayer = logicalLayer;
    }

    @Override
    public void init() {
        if (inited) {
            return;
        }

        final Node root = scene.getRoot();

        // Create a texture for our balls to use.
        final TextureState ts = new TextureState();
        ts.setEnabled(true);
        ts.setTexture(TextureManager.load("images/ball.png", Texture.MinificationFilter.NearestNeighborNoMipMaps,
                TextureStoreFormat.GuessCompressedFormat, true));
        root.setRenderState(ts);

        // Add blending.
        final BlendState blend = new BlendState();
        blend.setBlendEnabled(true);
        blend.setSourceFunction(BlendState.SourceFunction.SourceAlpha);
        blend.setDestinationFunction(BlendState.DestinationFunction.OneMinusSourceAlpha);
        blend.setTestEnabled(true);
        blend.setReference(0f);
        blend.setTestFunction(BlendState.TestFunction.GreaterThan);
        root.setRenderState(blend);

        resetBalls(10);
    }

    private void resetBalls(final int ballCount) {
        if (balls != null) {
            for (final BallSprite spr : balls) {
                spr.removeFromParent();
            }
        }

        balls.clear();

        final Camera cam = _canvas.getCanvasRenderer().getCamera();

        // Add balls
        for (int i = 0; i < ballCount; i++) {
            final BallSprite ballSprite = new BallSprite("ball", cam.getWidth(), cam.getHeight());
            scene.getRoot().attachChild(ballSprite);
            balls.add(ballSprite);
        }
    }

    @Override
    public void update(final ReadOnlyTimer timer) {
        final double tpf = timer.getTimePerFrame() * timeScale;

        logicalLayer.checkTriggers(tpf);

        if (balls == null) {
            return;
        }
        // Check collisions
        final int ballCount = balls.size();
        for (int i = 0; i < ballCount - 1; i++) {
            final Ball ballA = balls.get(i).getBall();
            for (int j = i + 1; j < ballCount; j++) {
                ballA.doCollide(balls.get(j).getBall());
            }
        }

        scene.getRoot().updateGeometricState(tpf, true);
    }

    public static void main(final String[] args) {
        AWTImageLoader.registerLoader();
        ExampleBase.addDefaultResourceLocators();

        final Timer timer = new Timer();
        final FrameHandler frameWork = new FrameHandler(timer);
        final LogicalLayer logicalLayer = new LogicalLayer();

        final AtomicBoolean exit = new AtomicBoolean(false);
        final BasicScene scene = new BasicScene();
        game = new GesturesSwtExample(scene, logicalLayer);

        frameWork.addUpdater(game);

        // INIT SWT STUFF
        final Display display = new Display();
        final Shell shell = new Shell(display);
        shell.setText("Gestures SWT Example");
        shell.setLayout(new FillLayout());
        final SashForm splitter = new SashForm(shell, SWT.HORIZONTAL);
        final Composite left = new Composite(splitter, SWT.NONE);
        left.setLayout(new FillLayout());
        final Composite right = new Composite(splitter, SWT.NONE);
        addInstructions(right);
        splitter.setWeights(new int[] { 75, 25 });

        scene.getRoot().setRenderMaterial("unlit/textured/basic.yaml");

        game.setupCanvas(shell, left, scene, frameWork, logicalLayer);

        shell.open();

        game.init();

        while (!shell.isDisposed() && !exit.get()) {
            display.readAndDispatch();
            frameWork.updateFrame();
            Thread.yield();
        }

        display.dispose();
        System.exit(0);
    }

    private static void addInstructions(final Composite parent) {
        final RowLayout rowLayout = new RowLayout();
        // rowLayout.fill = true;
        // rowLayout.justify = true;
        // rowLayout.pack = false;
        rowLayout.type = SWT.VERTICAL;
        // rowLayout.wrap = false;
        parent.setLayout(rowLayout);
        new Label(parent, SWT.NONE).setText("Ball Scale: Pinch (2 finger)");
        new Label(parent, SWT.NONE).setText("Time Scale: Rotate (2 finger)");
        new Label(parent, SWT.NONE).setText("Clear: Swipe (2 finger)");
        new Label(parent, SWT.NONE).setText("Spawn Ball: Pan (3 fingers)");
        new Label(parent, SWT.NONE).setText("Spawn Big Ball: Long Press (2 finger)");
    }

    private void setupCanvas(final Shell shell, final Composite parent, final BasicScene scene,
            final FrameHandler frameWork, final LogicalLayer logicalLayer) {
        final GLData data = new GLData();
        data.depthSize = 8;
        data.doubleBuffer = true;
        data.profile = GLData.Profile.CORE;
        data.majorVersion = 3;
        data.minorVersion = 3;

        _canvas = new Lwjgl3SwtCanvas(parent, SWT.NONE, data);
        final Lwjgl3CanvasRenderer canvasRenderer = new Lwjgl3CanvasRenderer(scene);
        canvasRenderer.setCanvasCallback(new Lwjgl3CanvasCallback() {
            @Override
            public void makeCurrent(final boolean force) {
                _canvas.setCurrent();
            }

            @Override
            public void releaseContext(final boolean force) {
            }

            @Override
            public void doSwap() {
                _canvas.swapBuffers();
            }
        });

        _canvas.setCanvasRenderer(canvasRenderer);
        frameWork.addCanvas(_canvas);
        addResizeHandler(_canvas, canvasRenderer);
        _canvas.setFocus();

        final Camera cam = Camera.newOrthoCamera(_canvas);
        canvasRenderer.setCamera(cam);

        final SwtKeyboardWrapper keyboardWrapper = new SwtKeyboardWrapper(_canvas);
        final SwtMouseWrapper mouseWrapper = new SwtMouseWrapper(_canvas);
        final SwtFocusWrapper focusWrapper = new SwtFocusWrapper(_canvas);
        final SwtGestureWrapper gestureWrapper = new SwtGestureWrapper(_canvas, mouseWrapper, false);
        gestureWrapper.addTouchInterpreter(new RotateInterpreter(10 * MathUtils.DEG_TO_RAD));
        gestureWrapper.addTouchInterpreter(new PinchInterpreter(40));
        gestureWrapper.addTouchInterpreter(new PanInterpreter(3));
        gestureWrapper.addTouchInterpreter(new SwipeInterpreter(2, 1.2, 100L));
        gestureWrapper.addTouchInterpreter(new LongPressInterpreter());

        final PhysicalLayer pl = new PhysicalLayer.Builder() //
                .with((KeyboardWrapper) keyboardWrapper) //
                .with((CharacterInputWrapper) keyboardWrapper) //
                .with(mouseWrapper) //
                .with(gestureWrapper) //
                .with(focusWrapper)//
                .build();

        logicalLayer.registerInput(_canvas, pl);

        logicalLayer.registerTrigger(
                new InputTrigger(new GestureEventCondition(PinchGestureEvent.class), new TriggerAction() {
                    public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                        final PinchGestureEvent event = inputStates.getCurrent().getGestureState()
                                .first(PinchGestureEvent.class);
                        // scale the balls
                        if (event.isStartOfGesture()) {
                            workScale = ballScale;
                        }

                        ballScale = MathUtils.clamp(workScale * event.getScale(), 0.1, 10);
                        if (balls != null) {
                            for (final BallSprite b : balls) {
                                b.getBall().scale(ballScale);
                                b.setScale(b.getBall().getTotalScale());
                            }
                        }
                    }
                }));

        logicalLayer.registerTrigger(
                new InputTrigger(new GestureEventCondition(RotateGestureEvent.class), new TriggerAction() {
                    public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                        final RotateGestureEvent event = inputStates.getCurrent().getGestureState()
                                .first(RotateGestureEvent.class);
                        // Alter time scale
                        currentRot += event.getDeltaRadians();
                        currentRot = MathUtils.clamp(currentRot, 0, MathUtils.PI);
                        timeScale = currentRot * ROT_SCALE + 0.01;
                    }
                }));

        logicalLayer.registerTrigger(
                new InputTrigger(new GestureEventCondition(PanGestureEvent.class), new TriggerAction() {
                    int oldX = -1, oldY = -1;
                    int minDeltaSq = 10 * 10;

                    public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                        final PanGestureEvent event = inputStates.getCurrent().getGestureState()
                                .first(PanGestureEvent.class);

                        if (event.isStartOfGesture()) {
                            oldX = event.getX();
                            oldY = event.getY();
                            return;
                        }

                        final int dx = event.getX() - oldX;
                        final int dy = event.getY() - oldY;

                        final int dSq = dx * dx + dy * dy;

                        if (dSq > minDeltaSq) {
                            spawnExtraBall(shell.getLocation(), scene, event, 1.0);
                            oldX = event.getX();
                            oldY = event.getY();
                        }
                    }
                }));

        logicalLayer.registerTrigger(
                new InputTrigger(new GestureEventCondition(SwipeGestureEvent.class), new TriggerAction() {
                    public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                        @SuppressWarnings("unused")
                        final SwipeGestureEvent event = inputStates.getCurrent().getGestureState()
                                .first(SwipeGestureEvent.class);
                        resetBalls(0);
                    }
                }));

        logicalLayer.registerTrigger(
                new InputTrigger(new GestureEventCondition(LongPressGestureEvent.class), new TriggerAction() {
                    @Override
                    public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                        final LongPressGestureEvent event = inputStates.getCurrent().getGestureState()
                                .first(LongPressGestureEvent.class);

                        // spawn a larger bubble
                        spawnExtraBall(shell.getLocation(), scene, event, 3.0);
                    }
                }));

        _canvas.init();
    }

    private void spawnExtraBall(final Point canvasOffset, final BasicScene scene, final AbstractGestureEvent event,
            final double scale) {
        final int locX = event.getX() - canvasOffset.x;
        final int locY = _canvas.getBounds().height - event.getY() + canvasOffset.y;
        final Camera cam = _canvas.getCanvasRenderer().getCamera();
        final BallSprite ballSprite = new BallSprite("ball", cam.getWidth(), cam.getHeight(), scale);
        final Ball ball = ballSprite.getBall();
        ball.setPosition(locX, locY);
        ball.scale(ballScale);
        ballSprite.setScale(ball.getTotalScale());
        balls.add(ballSprite);
        scene.getRoot().attachChild(ballSprite);
        ballSprite.updateGeometricState(0);
    }

    void addResizeHandler(final Lwjgl3SwtCanvas swtCanvas, final CanvasRenderer canvasRenderer) {
        swtCanvas.addListener((final int w, final int h) -> {
            if ((w == 0) || (h == 0)) {
                return;
            }

            final Camera camera = canvasRenderer.getCamera();
            if (camera != null) {
                final double near = camera.getFrustumNear();
                final double far = camera.getFrustumFar();
                camera.setFrustum(near, far, 0, w, h, 0);
                camera.resize(w, h);
            }

            if (balls != null) {
                for (final BallSprite b : balls) {
                    b.updateAreaDimensions(w, h);
                }
            }
        });
    }
}
