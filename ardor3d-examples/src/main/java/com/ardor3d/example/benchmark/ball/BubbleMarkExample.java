/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example.benchmark.ball;

import java.net.URISyntaxException;
import java.util.concurrent.Callable;

import com.ardor3d.example.Purpose;
import com.ardor3d.framework.CanvasRenderer;
import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.framework.NativeCanvas;
import com.ardor3d.framework.Scene;
import com.ardor3d.framework.jogl.JoglCanvas;
import com.ardor3d.framework.jogl.JoglCanvasRenderer;
import com.ardor3d.framework.lwjgl3.GLFWCanvas;
import com.ardor3d.framework.lwjgl3.Lwjgl3CanvasRenderer;
import com.ardor3d.image.Texture;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.image.util.awt.AWTImageLoader;
import com.ardor3d.intersection.PickResults;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Ray3;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.ui.text.BasicText;
import com.ardor3d.util.ContextGarbageCollector;
import com.ardor3d.util.GameTaskQueue;
import com.ardor3d.util.GameTaskQueueManager;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.Timer;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.SimpleResourceLocator;

/**
 * <p>
 * The famous BubbleMark UI test, recreated using quads.
 * </p>
 * <p>
 * There are several system params you can use to modify the test:
 * <ul>
 * <li>-Djogl=true -- use JoglRenderer and canvas instead of Lwjgl.</li>
 * <li>-Dvsync=true -- ask the canvas to use vertical sync to lock to the monitor refresh rate.</li>
 * <li>-DnoBallCollide=true -- do not do ball-to-ball collision checks.</li>
 * <li>-Dballs=# -- change the number of balls to some integer value. (default is 16)</li>
 * <li>-Dwidth=# -- change the width of the window to some integer value. (default is 500)</li>
 * <li>-Dheight=# -- change the height of the window to some integer value. (default is 300)</li>
 * <li>-Dadaptive=true -- instead of maintaining a set number of balls, try to maintain a steady frame rate (see
 * targetFPS) Not compatible with sync=true.</li>
 * <li>-DtargetFPS=# -- set the target frame rate (fps) for adaptive mode. (default is 200)</li>
 * </ul>
 * </p>
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.benchmark.ball.BubbleMarkExample", //
thumbnailPath = "com/ardor3d/example/media/thumbnails/benchmark_ball_BubbleMarkExample.jpg", //
maxHeapMemory = 64)
public class BubbleMarkExample implements Scene {

    // Our native window, not the gl surface itself.
    private final NativeCanvas canvas;

    // Our timer.
    private final Timer timer = new Timer();

    // A boolean allowing us to "pull the plug" from anywhere.
    private boolean exit = false;

    // The root of our scene
    private final Node root = new Node();

    private BallSprite[] balls;

    private BasicText frameRateLabel;

    private static final int width = (System.getProperty("width") != null ? Integer.parseInt(System
            .getProperty("width")) : 1920);
    private static final int height = (System.getProperty("height") != null ? Integer.parseInt(System
            .getProperty("height")) : 1080);

    private final boolean skipBallCollide = ("true".equalsIgnoreCase(System.getProperty("noBallCollide")));

    private boolean adaptiveStable = !("true".equalsIgnoreCase(System.getProperty("adaptive")));

    private static final int adaptiveTargetFPS = (System.getProperty("targetFPS") != null ? Integer.parseInt(System
            .getProperty("targetFPS")) : 200);

    private int frames = 0;
    private long startTime = System.currentTimeMillis();

    public static void main(final String[] args) {
        final BubbleMarkExample example = new BubbleMarkExample();
        example.start();
    }

    /**
     * Constructs the example class, also creating the native window and GL surface.
     */
    public BubbleMarkExample() {
        canvas = initCanvas();
        canvas.init();

        canvas.setVSyncEnabled("true".equalsIgnoreCase(System.getProperty("vsync")));
    }

    /**
     * Kicks off the example logic, first setting up the scene, then continuously updating and rendering it until exit
     * is flagged. Afterwards, the scene and gl surface are cleaned up.
     */
    private void start() {
        initExample();
        // Run in this same thread.
        while (!exit) {
            updateExample();
            canvas.draw(null);
        }

        // Done, do cleanup
        final CanvasRenderer cr = canvas.getCanvasRenderer();
        cr.makeCurrentContext();
        ContextGarbageCollector.doFinalCleanup(canvas.getCanvasRenderer().getRenderer());
        canvas.close();
        cr.releaseCurrentContext();
    }

    /**
     * Setup a native canvas and canvas renderer.
     *
     * @return the canvas.
     */
    private NativeCanvas initCanvas() {
        if ("true".equalsIgnoreCase(System.getProperty("jogl"))) {
            final JoglCanvasRenderer canvasRenderer = new JoglCanvasRenderer(this);
            final DisplaySettings settings = new DisplaySettings(width, height, 24, 0, 0, 8, 0, 0, false, false);
            return new JoglCanvas(canvasRenderer, settings);
        } else {
            final Lwjgl3CanvasRenderer canvasRenderer = new Lwjgl3CanvasRenderer(this);
            final DisplaySettings settings = new DisplaySettings(width, height, 24, 0, 0, 8, 0, 0, false, false);
            return new GLFWCanvas(settings, canvasRenderer);
        }
    }

    /**
     * Initialize our scene.
     */
    private void initExample() {
        canvas.setTitle("BubbleMarkExample - close window to exit");

        // Add our awt based image loader.
        AWTImageLoader.registerLoader();

        // Set the location of our example resources.
        try {
            final SimpleResourceLocator srl = new SimpleResourceLocator(ResourceLocatorTool.getClassPathResource(
                    BubbleMarkExample.class, "com/ardor3d/example/media/"));
            ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_TEXTURE, srl);
        } catch (final URISyntaxException ex) {
            ex.printStackTrace();
        }

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

        // setup scene
        // Add background
        final CanvasRenderer canvasRenderer = canvas.getCanvasRenderer();
        final RenderContext renderContext = canvasRenderer.getRenderContext();
        final Renderer renderer = canvasRenderer.getRenderer();
        GameTaskQueueManager.getManager(renderContext).getQueue(GameTaskQueue.RENDER).enqueue(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                renderer.setBackgroundColor(ColorRGBA.WHITE);
                return null;
            }
        });

        int ballCount = 16;
        if (System.getProperty("balls") != null) {
            ballCount = Integer.parseInt(System.getProperty("balls"));
        }

        resetBalls(ballCount);

        // Add fps display
        frameRateLabel = BasicText.createDefaultTextLabel("fpsLabel", "");
        frameRateLabel.setTranslation(5, height - 5 - frameRateLabel.getHeight(), 0);
        frameRateLabel.setTextColor(ColorRGBA.BLACK);
        frameRateLabel.getSceneHints().setOrthoOrder(-1);
        root.attachChild(frameRateLabel);
    }

    private void resetBalls(final int ballCount) {
        if (balls != null) {
            for (final BallSprite spr : balls) {
                spr.removeFromParent();
            }
        }
        balls = new BallSprite[ballCount];

        // Add balls
        for (int i = 0; i < balls.length; i++) {
            final BallSprite ballSprite = new BallSprite("ball", width, height);
            root.attachChild(ballSprite);
            balls[i] = ballSprite;
        }
    }

    /**
     * Update our scene... Check if the window is closing. Then update our timer and finally update the geometric state
     * of the root and its children.
     */
    private void updateExample() {
        if (canvas.isClosing()) {
            exit = true;
            return;
        }

        timer.update();

        final long now = System.currentTimeMillis();
        final long dt = now - startTime;
        if (dt > 2000) {
            final int N = balls.length;
            final int fps = (int) (1e3 * frames / dt);
            frameRateLabel.setText(fps + " fps - " + N + " balls" + (adaptiveStable ? "" : "?"));

            startTime = now;
            frames = 0;

            if (!adaptiveStable) {
                double ratio = fps / (double) adaptiveTargetFPS;
                if (ratio < 0.25) {
                    ratio = 0.25;
                } else if (ratio > 4.0) {
                    ratio = 4.0;
                }
                final int newN = Math.max(1, (ratio > 1.1 || ratio < 0.9) ? (int) (N * ratio) : (ratio > 1.025) ? N + 1
                        : (ratio < 0.975) ? N - 1 : N);
                if (newN != N) {
                    resetBalls(newN);
                } else {
                    adaptiveStable = true;
                }
            }
        }

        if (!skipBallCollide) {
            // Check collisions
            for (int i = 0; i < balls.length; i++) {
                for (int j = i + 1; j < balls.length; j++) {
                    balls[i].getBall().doCollide(balls[j].getBall());
                }
            }
        }

        // Update controllers/render states/transforms/bounds for rootNode.
        root.updateGeometricState(timer.getTimePerFrame(), true);
        frames++;
    }

    // ------ Scene methods ------

    public boolean render(final Renderer renderer) {
        if (!canvas.isClosing()) {

            // Draw the root and all its children.
            renderer.draw(root);

            return true;
        }
        return false;
    }

    public PickResults doPick(final Ray3 pickRay) {
        // Ignore
        return null;
    }
}
