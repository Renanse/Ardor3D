/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.example.canvas;

import java.util.concurrent.atomic.AtomicBoolean;

import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.framework.BasicScene;
import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.framework.FrameHandler;
import com.ardor3d.framework.lwjgl3.GLFWHeadlessCanvas;
import com.ardor3d.framework.lwjgl3.Lwjgl3CanvasRenderer;
import com.ardor3d.image.util.awt.AWTImageLoader;
import com.ardor3d.image.util.awt.ScreenShotImageExporter;
import com.ardor3d.input.keyboard.Key;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.util.Timer;
import com.ardor3d.util.screen.ScreenExporter;

/**
 * This examples demonstrates how to render OpenGL (via LWJGL) on a SWT canvas via off-screen FBO rendering.
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.canvas.LwjglHeadlessSwtExample", //
        thumbnailPath = "com/ardor3d/example/media/thumbnails/canvas_LwjglHeadlessSwtExample.jpg", //
        maxHeapMemory = 64)
public class LwjglHeadlessExample {
    private static RotatingCubeGame game;
    private static ScreenShotImageExporter _screenShotExp = new ScreenShotImageExporter();
    private static boolean _doShot = true;

    public static void main(final String[] args) {
        ExampleBase.addDefaultResourceLocators();

        final Timer timer = new Timer();
        final FrameHandler frameWork = new FrameHandler(timer);
        final LogicalLayer logicalLayer = new LogicalLayer();

        final AtomicBoolean exit = new AtomicBoolean(false);
        final BasicScene scene = new BasicScene();
        game = new RotatingCubeGame(scene, exit, logicalLayer, Key.T);

        frameWork.addUpdater(game);

        // Setup AWT image loader to load our ardor3d logo and font texture.
        AWTImageLoader.registerLoader();

        // set up the opengl canvas renderer to use
        final Lwjgl3CanvasRenderer renderer = new Lwjgl3CanvasRenderer(scene);

        // Create our headless canvas
        final DisplaySettings settings = new DisplaySettings(800, 600, 24, 0);
        final GLFWHeadlessCanvas canvas = new GLFWHeadlessCanvas(settings, renderer);
        frameWork.addCanvas(canvas);

        frameWork.init();

        // game loop
        while (!exit.get()) {
            frameWork.updateFrame();
            Thread.yield();
            frameWork.updateFrame();
            Thread.yield();
            frameWork.updateFrame();
            Thread.yield();
            frameWork.updateFrame();
            Thread.yield();

            try {
                Thread.sleep(100);
                if (_doShot) {
                    // force any waiting scene elements to be renderer.
                    renderer.getRenderer().renderBuckets();
                    ScreenExporter.exportCurrentScreen(renderer.getRenderer(), _screenShotExp);
                    _doShot = false;
                }
            } catch (final InterruptedException ex) {
                // TODO Auto-generated catch block
                ex.printStackTrace();
            }
        }

        // cleanup and exit
        canvas.close();
        System.exit(0);
    }
}
