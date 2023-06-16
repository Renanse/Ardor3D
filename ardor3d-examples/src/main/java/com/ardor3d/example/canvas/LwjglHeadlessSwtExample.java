/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.example.canvas;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.framework.BasicScene;
import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.framework.FrameHandler;
import com.ardor3d.framework.lwjgl3.Lwjgl3CanvasRenderer;
import com.ardor3d.framework.swt.SwtFboCanvas;
import com.ardor3d.image.util.awt.AWTImageLoader;
import com.ardor3d.input.PhysicalLayer;
import com.ardor3d.input.character.CharacterInputWrapper;
import com.ardor3d.input.keyboard.Key;
import com.ardor3d.input.keyboard.KeyboardWrapper;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.input.swt.SwtFocusWrapper;
import com.ardor3d.input.swt.SwtGestureWrapper;
import com.ardor3d.input.swt.SwtKeyboardWrapper;
import com.ardor3d.input.swt.SwtMouseManager;
import com.ardor3d.input.swt.SwtMouseWrapper;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.lwjgl3.Lwjgl3CanvasCallback;
import com.ardor3d.util.Timer;

/**
 * This examples demonstrates how to render OpenGL (via LWJGL) on a SWT canvas via off-screen FBO
 * rendering.
 */
@Purpose(
    htmlDescriptionKey = "com.ardor3d.example.canvas.LwjglHeadlessSwtExample", //
    thumbnailPath = "com/ardor3d/example/media/thumbnails/canvas_LwjglHeadlessSwtExample.jpg", //
    maxHeapMemory = 64)
public class LwjglHeadlessSwtExample {
  private static final Logger logger = Logger.getLogger(LwjglHeadlessSwtExample.class.toString());
  private static RotatingCubeGame game;

  public static void main(final String[] args) {
    ExampleBase.addDefaultResourceLocators();

    final Timer timer = new Timer();
    final FrameHandler frameWork = new FrameHandler(timer);
    final LogicalLayer logicalLayer = new LogicalLayer();

    final AtomicBoolean exit = new AtomicBoolean(false);
    final BasicScene scene = new BasicScene();
    game = new RotatingCubeGame(scene, exit, logicalLayer, Key.T);

    frameWork.addUpdater(game);

    // INIT SWT STUFF
    final Display display = new Display();
    final Shell shell = new Shell(display);
    shell.setLayout(new FillLayout());

    // Setup AWT image loader to load our ardor3d logo and font texture.
    AWTImageLoader.registerLoader();

    // add our canvas to the shell, using the scene we've setup and our logic layer
    addNewFBOCanvas(shell, scene, frameWork, logicalLayer);

    // show the shell
    shell.open();

    // init our game
    game.init();

    // game loop
    while (!shell.isDisposed() && !exit.get()) {
      display.readAndDispatch();
      frameWork.updateFrame();
      Thread.yield();
    }

    // cleanup and exit
    display.dispose();
    System.exit(0);
  }

  private static void addNewFBOCanvas(final Shell shell, final BasicScene scene, final FrameHandler frameWork,
      final LogicalLayer logicalLayer) {
    logger.info("Adding canvas");

    // Display settings to use for canvas - width/height will be updated when canvas is displayed.
    final DisplaySettings settings = new DisplaySettings(32, 32, 32, 16);
    final SwtFboCanvas canvas = new SwtFboCanvas(shell, SWT.NONE, settings);

    // set up the opengl canvas renderer to use
    final Lwjgl3CanvasRenderer renderer = new Lwjgl3CanvasRenderer(scene);
    canvas.setCanvasRenderer(renderer);

    // add our canvas to framework for updates, etc.
    frameWork.addCanvas(canvas);
    canvas.setFocus();

    renderer.getRenderContext().getSceneIndexer().addSceneRoot(scene.getRoot());

    addCanvasCallback(canvas, renderer);
    canvas.addListener((final int w, final int h) -> {
      System.err.println(w);
      if ((w == 0) || (h == 0)) {
        return;
      }

      final float aspect = (float) w / (float) h;
      final Camera camera = renderer.getCamera();
      if (camera != null) {
        final double fovY = camera.getFovY();
        final double near = camera.getFrustumNear();
        final double far = camera.getFrustumFar();
        camera.setFrustumPerspective(fovY, aspect, near, far);
        camera.resize(w, h);
      }
    });

    final SwtKeyboardWrapper keyboardWrapper = new SwtKeyboardWrapper(canvas);
    final SwtMouseWrapper mouseWrapper = new SwtMouseWrapper(canvas);
    final SwtFocusWrapper focusWrapper = new SwtFocusWrapper(canvas);
    final SwtMouseManager mouseManager = new SwtMouseManager(canvas);
    canvas.setMouseManager(mouseManager);
    final SwtGestureWrapper gestureWrapper = new SwtGestureWrapper(canvas, mouseWrapper, true);

    final PhysicalLayer pl = new PhysicalLayer.Builder() //
        .with((KeyboardWrapper) keyboardWrapper) //
        .with((CharacterInputWrapper) keyboardWrapper) //
        .with(mouseWrapper) //
        .with(gestureWrapper) //
        .with(focusWrapper)//
        .build();

    logicalLayer.registerInput(canvas, pl);
  }

  private static void addCanvasCallback(final SwtFboCanvas canvas, final Lwjgl3CanvasRenderer renderer) {
    renderer.setCanvasCallback(new Lwjgl3CanvasCallback() {
      @Override
      public void makeCurrent(final boolean force) {
        canvas.setCurrent();
      }

      @Override
      public void releaseContext(final boolean force) {

      }

      @Override
      public void doSwap() {
        canvas.swapBuffers();
      }
    });
  }
}
