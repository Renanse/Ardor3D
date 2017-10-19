/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example.benchmark.ball;

import java.util.concurrent.Callable;

import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.extension.ui.UICheckBox;
import com.ardor3d.extension.ui.UIContainer;
import com.ardor3d.extension.ui.UIFrame;
import com.ardor3d.extension.ui.UIHud;
import com.ardor3d.extension.ui.UILabel;
import com.ardor3d.extension.ui.UIPanel;
import com.ardor3d.extension.ui.UIRadioButton;
import com.ardor3d.extension.ui.backdrop.SolidBackdrop;
import com.ardor3d.extension.ui.event.ActionEvent;
import com.ardor3d.extension.ui.event.ActionListener;
import com.ardor3d.extension.ui.layout.AnchorLayout;
import com.ardor3d.extension.ui.layout.AnchorLayoutData;
import com.ardor3d.extension.ui.util.Alignment;
import com.ardor3d.extension.ui.util.ButtonGroup;
import com.ardor3d.extension.ui.util.SubTex;
import com.ardor3d.image.Texture;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.ui.text.BasicText;
import com.ardor3d.util.GameTaskQueueManager;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureManager;

/**
 * The famous BubbleMark UI test, recreated using Ardor3D UI components.
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.benchmark.ball.BubbleMarkUIExample", //
thumbnailPath = "com/ardor3d/example/media/thumbnails/benchmark_ball_BubbleMarkUIExample.jpg", //
maxHeapMemory = 64)
public class BubbleMarkUIExample extends ExampleBase {

    private BallComponent[] balls;

    private BasicText frameRateLabel;

    private int frames = 0;
    private long startTime = System.currentTimeMillis();

    private UIHud hud;

    private UIFrame _ballFrame;

    private UIFrame _configFrame;

    private boolean skipBallCollide = false;

    public static void main(final String[] args) {
        start(BubbleMarkUIExample.class);
    }

    /**
     * Initialize our scene.
     */
    @Override
    protected void initExample() {
        _canvas.setTitle("BubbleMarkUIExample");
        final int width = _canvas.getCanvasRenderer().getCamera().getWidth();
        final int height = _canvas.getCanvasRenderer().getCamera().getHeight();

        hud = new UIHud(_canvas);

        // Add Frame for balls
        _ballFrame = new UIFrame("Bubbles");
        _ballFrame.getContentPanel().setMinimumContentSize(600, 300);
        _ballFrame.setResizeable(false);
        _ballFrame.setHudXY(5, 5);
        _ballFrame.setUseStandin(false);
        _ballFrame.pack();
        hud.add(_ballFrame);

        // Add background
        _ballFrame.getContentPanel().setBackdrop(new SolidBackdrop(ColorRGBA.WHITE));

        // Add Frame for config
        buildConfigFrame(width, height);
        hud.add(_configFrame);

        resetBalls(16);
        _root.attachChild(hud);

        // Add fps display
        frameRateLabel = BasicText.createDefaultTextLabel("fpsLabel", "");
        frameRateLabel.setTranslation(5, hud.getHeight() - 5 - frameRateLabel.getHeight(), 0);
        frameRateLabel.setTextColor(ColorRGBA.WHITE);
        frameRateLabel.getSceneHints().setOrthoOrder(-1);
        _root.attachChild(frameRateLabel);

        hud.setupInput(_physicalLayer, _logicalLayer);
    }

    private void buildConfigFrame(final int width, final int height) {
        _configFrame = new UIFrame("Config");
        final UIPanel panel = _configFrame.getContentPanel();
        panel.setLayout(new AnchorLayout());
        panel.setMinimumContentSize(320, 240);
        _configFrame.setUseStandin(true);
        _configFrame.setHudXY(width - _configFrame.getLocalComponentWidth() - 5,
                height - _configFrame.getLocalComponentHeight() - 5);

        final UICheckBox vsync = new UICheckBox("Enable vsync");
        vsync.setLayoutData(new AnchorLayoutData(Alignment.TOP_LEFT, panel, Alignment.TOP_LEFT, 5, -5));
        vsync.setSelectable(true);
        vsync.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent event) {
                GameTaskQueueManager.getManager(_canvas.getCanvasRenderer().getRenderContext()).render(
                        new Callable<Void>() {
                            public Void call() throws Exception {
                                _canvas.setVSyncEnabled(vsync.isSelected());
                                return null;
                            }
                        });

            }
        });
        panel.add(vsync);

        final UICheckBox collide = new UICheckBox("Enable ball-ball collision");
        collide.setLayoutData(new AnchorLayoutData(Alignment.TOP_LEFT, vsync, Alignment.BOTTOM_LEFT, 0, -5));
        collide.setSelectable(true);
        collide.setSelected(!skipBallCollide);
        collide.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent event) {
                skipBallCollide = !collide.isSelected();
            }
        });
        panel.add(collide);

        final UILabel ballsLabel = new UILabel("# of balls:");
        ballsLabel.setLayoutData(new AnchorLayoutData(Alignment.TOP_LEFT, collide, Alignment.BOTTOM_LEFT, 0, -15));
        panel.add(ballsLabel);

        final ButtonGroup ballsGroup = new ButtonGroup();

        final UIRadioButton balls16 = new UIRadioButton("16");
        balls16.setLayoutData(new AnchorLayoutData(Alignment.LEFT, ballsLabel, Alignment.RIGHT, 5, 0));
        balls16.setSelectable(true);
        balls16.setSelected(true);
        balls16.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent event) {
                resetBalls(16);
            }
        });
        balls16.setGroup(ballsGroup);
        panel.add(balls16);

        final UIRadioButton balls32 = new UIRadioButton("32");
        balls32.setLayoutData(new AnchorLayoutData(Alignment.TOP_LEFT, balls16, Alignment.BOTTOM_LEFT, 0, -5));
        balls32.setSelectable(true);
        balls32.setSelected(true);
        balls32.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent event) {
                resetBalls(32);
            }
        });
        balls32.setGroup(ballsGroup);
        panel.add(balls32);

        final UIRadioButton balls64 = new UIRadioButton("64");
        balls64.setLayoutData(new AnchorLayoutData(Alignment.TOP_LEFT, balls32, Alignment.BOTTOM_LEFT, 0, -5));
        balls64.setSelectable(true);
        balls64.setSelected(true);
        balls64.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent event) {
                resetBalls(64);
            }
        });
        balls64.setGroup(ballsGroup);
        panel.add(balls64);

        final UIRadioButton balls128 = new UIRadioButton("128");
        balls128.setLayoutData(new AnchorLayoutData(Alignment.TOP_LEFT, balls64, Alignment.BOTTOM_LEFT, 0, -5));
        balls128.setSelectable(true);
        balls128.setSelected(true);
        balls128.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent event) {
                resetBalls(128);
            }
        });
        balls128.setGroup(ballsGroup);
        panel.add(balls128);

        _configFrame.pack();
    }

    @Override
    protected void updateLogicalLayer(final ReadOnlyTimer timer) {
        hud.getLogicalLayer().checkTriggers(timer.getTimePerFrame());
    }

    private void resetBalls(final int ballCount) {
        final UIContainer container = _ballFrame.getContentPanel();
        container.setLayout(null);
        container.detachAllChildren();

        balls = new BallComponent[ballCount];

        // Create a texture for our balls to use.
        final SubTex tex = new SubTex(TextureManager.load("images/ball.png",
                Texture.MinificationFilter.NearestNeighborNoMipMaps, TextureStoreFormat.GuessCompressedFormat, true));

        // Add balls
        for (int i = 0; i < balls.length; i++) {
            final BallComponent ballComp = new BallComponent("ball", tex, Ball.radius * 2, Ball.radius * 2,
                    container.getContentWidth(), container.getContentHeight());
            container.add(ballComp);
            balls[i] = ballComp;
        }

        _ballFrame.setTitle("Bubbles  - " + ballCount + " balls");
    }

    @Override
    protected void updateExample(final ReadOnlyTimer timer) {

        final long now = System.currentTimeMillis();
        final long dt = now - startTime;
        if (dt > 2000) {
            final long fps = Math.round(1e3 * frames / dt);
            frameRateLabel.setText(fps + " fps");

            startTime = now;
            frames = 0;
        }

        if (!skipBallCollide) {
            // Check collisions
            for (int i = 0; i < balls.length; i++) {
                for (int j = i + 1; j < balls.length; j++) {
                    balls[i].getBall().doCollide(balls[j].getBall());
                }
            }
        }

        frames++;
    }
}
