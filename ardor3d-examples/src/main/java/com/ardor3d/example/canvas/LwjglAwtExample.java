/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.example.canvas;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import org.lwjgl.opengl.awt.GLData;

import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.framework.BasicScene;
import com.ardor3d.framework.Canvas;
import com.ardor3d.framework.FrameHandler;
import com.ardor3d.framework.lwjgl3.Lwjgl3CanvasRenderer;
import com.ardor3d.framework.lwjgl3.awt.Lwjgl3AwtCanvas;
import com.ardor3d.image.util.awt.AWTImageLoader;
import com.ardor3d.input.PhysicalLayer;
import com.ardor3d.input.awt.AwtFocusWrapper;
import com.ardor3d.input.awt.AwtKeyboardWrapper;
import com.ardor3d.input.awt.AwtMouseManager;
import com.ardor3d.input.awt.AwtMouseWrapper;
import com.ardor3d.input.character.CharacterInputWrapper;
import com.ardor3d.input.keyboard.Key;
import com.ardor3d.input.keyboard.KeyboardWrapper;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.input.logical.MouseButtonClickedCondition;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.input.mouse.MouseButton;
import com.ardor3d.renderer.Camera;
import com.ardor3d.util.Timer;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.SimpleResourceLocator;

/**
 * This examples demonstrates how to render OpenGL (via LWJGL) on a AWT canvas.
 */
@Purpose(
    htmlDescriptionKey = "com.ardor3d.example.canvas.LwjglAwtExample", //
    thumbnailPath = "com/ardor3d/example/media/thumbnails/canvas_LwjglAwtExample.jpg", //
    maxHeapMemory = 64)
public class LwjglAwtExample {

  static Map<Canvas, Boolean> _showCursor1 = new HashMap<>();

  public static void main(final String[] args) throws Exception {
    System.setProperty("ardor3d.useMultipleContexts", "true");
    AWTImageLoader.registerLoader();
    ExampleBase.addDefaultResourceLocators();

    final Timer timer = new Timer();
    final FrameHandler frameWork = new FrameHandler(timer);

    final AtomicBoolean exit = new AtomicBoolean(false);
    final LogicalLayer logicalLayer = new LogicalLayer();

    final BasicScene scene1 = new BasicScene();
    final RotatingCubeGame game1 = new RotatingCubeGame(scene1, exit, logicalLayer, Key.T);

    final BasicScene scene2 = new BasicScene();
    final RotatingCubeGame game2 = new RotatingCubeGame(scene2, exit, logicalLayer, Key.G);

    final Predicate<TwoInputStates> clickLeftOrRight =
        new MouseButtonClickedCondition(MouseButton.LEFT).or(new MouseButtonClickedCondition(MouseButton.RIGHT));

    logicalLayer.registerTrigger(new InputTrigger(clickLeftOrRight, (source, inputStates, tpf) -> System.err
        .println("clicked: " + inputStates.getCurrent().getMouseState().getClickCounts())));

    frameWork.addUpdater(game1);
    frameWork.addUpdater(game2);

    final JFrame frame = new JFrame("AWT Example");
    frame.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(final WindowEvent e) {
        exit.set(true);
      }
    });

    frame.setLayout(new GridLayout(2, 3));

    try {
      final SimpleResourceLocator srl = new SimpleResourceLocator(
          ResourceLocatorTool.getClassPathResource(LwjglAwtExample.class, "com/ardor3d/example/media/"));
      ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_TEXTURE, srl);
    } catch (final URISyntaxException ex) {
      ex.printStackTrace();
    }

    addCanvas(frame, scene1, logicalLayer, frameWork);
    frame.add(new JLabel("<html>" + "<table>"
        + "<tr><th align=\"left\" style=\"font-size: 16\">Action</th><th align=\"left\" style=\"font-size: 16\">Command</th></tr>"
        + "<tr><td>T</td><td>Toggle cube rotation for scene 1 on press</td></tr>"
        + "<tr><td>G</td><td>Toggle cube rotation for scene 2 on press</td></tr>"
        + "<tr><td>U</td><td>Toggle both cube rotations on release</td></tr>"
        + "<tr><td>0 (zero)</td><td>Reset camera position</td></tr>"
        + "<tr><td>9</td><td>Face camera towards cube without changing position</td></tr>"
        + "<tr><td>ESC</td><td>Quit</td></tr>"
        + "<tr><td>Mouse</td><td>Press left button and drag to rotate cube.</td></tr>" + "</table>" + "</html>",
        SwingConstants.CENTER));
    addCanvas(frame, scene1, logicalLayer, frameWork);
    frame.add(new JLabel("", SwingConstants.CENTER));
    addCanvas(frame, scene2, logicalLayer, frameWork);
    frame.add(new JLabel("", SwingConstants.CENTER));

    frame.pack();
    frame.setVisible(true);

    game1.init();
    game2.init();

    while (!exit.get()) {
      frameWork.updateFrame();
      Thread.yield();
    }

    frame.dispose();
    System.exit(0);
  }

  private static void addCanvas(final JFrame frame, final BasicScene scene, final LogicalLayer logicalLayer,
      final FrameHandler frameWork) throws Exception {
    final GLData data = new GLData();
    data.depthSize = 16;
    data.doubleBuffer = true;
    data.profile = GLData.Profile.CORE;
    data.majorVersion = 3;
    data.minorVersion = 3;

    final Lwjgl3AwtCanvas theCanvas = new Lwjgl3AwtCanvas(data);
    final Lwjgl3CanvasRenderer renderer = new Lwjgl3CanvasRenderer(scene);
    theCanvas.setCanvasRenderer(renderer);

    frame.add(theCanvas);

    _showCursor1.put(theCanvas, true);

    theCanvas.setSize(new Dimension(400, 300));
    theCanvas.setVisible(true);

    final AwtKeyboardWrapper keyboardWrapper = new AwtKeyboardWrapper(theCanvas);
    final AwtFocusWrapper focusWrapper = new AwtFocusWrapper(theCanvas);
    final AwtMouseManager mouseManager = new AwtMouseManager(theCanvas);
    final AwtMouseWrapper mouseWrapper = new AwtMouseWrapper(theCanvas, mouseManager);
    theCanvas.setMouseManager(mouseManager);

    final PhysicalLayer pl = new PhysicalLayer.Builder() //
        .with((KeyboardWrapper) keyboardWrapper) //
        .with((CharacterInputWrapper) keyboardWrapper)//
        .with(mouseWrapper)//
        .with(focusWrapper)//
        .build();
    logicalLayer.registerInput(theCanvas, pl);

    frameWork.addCanvas(theCanvas);

    theCanvas.addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(final ComponentEvent e) {
        final int w = theCanvas.getSize().width;
        final int h = theCanvas.getSize().height;

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
      }
    });
  }
}
