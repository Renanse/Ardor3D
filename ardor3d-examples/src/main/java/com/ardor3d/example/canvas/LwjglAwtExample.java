/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example.canvas;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import com.ardor3d.example.Purpose;
import com.ardor3d.framework.BasicScene;
import com.ardor3d.framework.Canvas;
import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.framework.FrameHandler;
import com.ardor3d.framework.lwjgl3.Lwjgl3AwtCanvas;
import com.ardor3d.framework.lwjgl3.Lwjgl3CanvasRenderer;
import com.ardor3d.image.util.awt.AWTImageLoader;
import com.ardor3d.input.ControllerWrapper;
import com.ardor3d.input.Key;
import com.ardor3d.input.MouseCursor;
import com.ardor3d.input.PhysicalLayer;
import com.ardor3d.input.awt.AwtFocusWrapper;
import com.ardor3d.input.awt.AwtKeyboardWrapper;
import com.ardor3d.input.awt.AwtMouseManager;
import com.ardor3d.input.awt.AwtMouseWrapper;
import com.ardor3d.input.logical.DummyControllerWrapper;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyPressedCondition;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.input.logical.TriggerAction;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.util.Timer;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.SimpleResourceLocator;

/**
 * This examples demonstrates how to render OpenGL (via LWJGL) on a AWT canvas.
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.canvas.LwjglAwtExample", //
thumbnailPath = "com/ardor3d/example/media/thumbnails/canvas_LwjglAwtExample.jpg", //
maxHeapMemory = 64)
public class LwjglAwtExample {
    static MouseCursor _cursor1;
    static MouseCursor _cursor2;

    static Map<Canvas, Boolean> _showCursor1 = new HashMap<Canvas, Boolean>();

    public static void main(final String[] args) throws Exception {
        System.setProperty("ardor3d.useMultipleContexts", "true");

        final Timer timer = new Timer();
        final FrameHandler frameWork = new FrameHandler(timer);

        final AtomicBoolean exit = new AtomicBoolean(false);
        final LogicalLayer logicalLayer = new LogicalLayer();

        final BasicScene scene1 = new BasicScene();
        final RotatingCubeGame game1 = new RotatingCubeGame(scene1, exit, logicalLayer, Key.T);

        final BasicScene scene2 = new BasicScene();
        final RotatingCubeGame game2 = new RotatingCubeGame(scene2, exit, logicalLayer, Key.G);

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

        AWTImageLoader.registerLoader();

        try {
            final SimpleResourceLocator srl = new SimpleResourceLocator(ResourceLocatorTool.getClassPathResource(
                    LwjglAwtExample.class, "com/ardor3d/example/media/"));
            ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_TEXTURE, srl);
        } catch (final URISyntaxException ex) {
            ex.printStackTrace();
        }

        final AWTImageLoader awtImageLoader = new AWTImageLoader();
        _cursor1 = createMouseCursor(awtImageLoader, "com/ardor3d/example/media/input/wait_cursor.png");
        _cursor2 = createMouseCursor(awtImageLoader, "com/ardor3d/example/media/input/movedata.gif");

        addCanvas(frame, scene1, logicalLayer, frameWork);
        frame.add(new JLabel(
                "<html>"
                        + "<table>"
                        + "<tr><th align=\"left\" style=\"font-size: 16\">Action</th><th align=\"left\" style=\"font-size: 16\">Command</th></tr>"
                        + "<tr><td>WS</td><td>Move camera position forward/back</td></tr>"
                        + "<tr><td>AD</td><td>Turn camera left/right</td></tr>"
                        + "<tr><td>T</td><td>Toggle cube rotation for scene 1 on press</td></tr>"
                        + "<tr><td>G</td><td>Toggle cube rotation for scene 2 on press</td></tr>"
                        + "<tr><td>U</td><td>Toggle both cube rotations on release</td></tr>"
                        + "<tr><td>0 (zero)</td><td>Reset camera position</td></tr>"
                        + "<tr><td>9</td><td>Face camera towards cube without changing position</td></tr>"
                        + "<tr><td>ESC</td><td>Quit</td></tr>"
                        + "<tr><td>Mouse</td><td>Press left button to rotate camera.</td></tr>" + "</table>"
                        + "</html>", SwingConstants.CENTER));
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

    private static MouseCursor createMouseCursor(final AWTImageLoader awtImageLoader, final String resourceName)
            throws IOException {
        final com.ardor3d.image.Image image = awtImageLoader.load(
                ResourceLocatorTool.getClassPathResourceAsStream(LwjglAwtExample.class, resourceName), false);

        return new MouseCursor("cursor1", image, 0, image.getHeight() - 1);
    }

    private static void addCanvas(final JFrame frame, final BasicScene scene, final LogicalLayer logicalLayer,
            final FrameHandler frameWork) throws Exception {
        final Lwjgl3CanvasRenderer canvasRenderer = new Lwjgl3CanvasRenderer(scene);

        final DisplaySettings settings = new DisplaySettings(400, 300, 24, 0, 0, 16, 0, 0, false, false);
        final Lwjgl3AwtCanvas theCanvas = new Lwjgl3AwtCanvas(settings, canvasRenderer);

        frame.add(theCanvas);

        _showCursor1.put(theCanvas, true);

        theCanvas.setSize(new Dimension(400, 300));
        theCanvas.setVisible(true);

        final AwtKeyboardWrapper keyboardWrapper = new AwtKeyboardWrapper(theCanvas);
        final AwtFocusWrapper focusWrapper = new AwtFocusWrapper(theCanvas);
        final AwtMouseManager mouseManager = new AwtMouseManager(theCanvas);
        final AwtMouseWrapper mouseWrapper = new AwtMouseWrapper(theCanvas, mouseManager);
        theCanvas.setMouseManager(mouseManager);
        final ControllerWrapper controllerWrapper = new DummyControllerWrapper();

        final PhysicalLayer pl = new PhysicalLayer(keyboardWrapper, mouseWrapper, controllerWrapper, focusWrapper);

        logicalLayer.registerInput(theCanvas, pl);

        logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.H), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                if (source != theCanvas) {
                    return;
                }

                if (_showCursor1.get(theCanvas)) {
                    mouseManager.setCursor(_cursor1);
                } else {
                    mouseManager.setCursor(_cursor2);
                }

                _showCursor1.put(theCanvas, !_showCursor1.get(theCanvas));
            }
        }));
        logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.J), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                if (source != theCanvas) {
                    return;
                }

                mouseManager.setCursor(MouseCursor.SYSTEM_DEFAULT);
            }
        }));

        frameWork.addCanvas(theCanvas);

    }
}
