/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example.canvas;

import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JPopupMenu;

import com.ardor3d.example.Purpose;
import com.ardor3d.framework.Canvas;
import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.framework.FrameHandler;
import com.ardor3d.framework.jogl.JoglAwtCanvas;
import com.ardor3d.framework.jogl.JoglCanvasRenderer;
import com.ardor3d.image.util.AWTImageLoader;
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
import com.ardor3d.renderer.Camera;
import com.ardor3d.util.Timer;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.SimpleResourceLocator;

/**
 * This examples demonstrates how to render OpenGL (via JOGL) inside JDesktop internal frames.
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.canvas.JoglAwtDesktopExample", //
thumbnailPath = "com/ardor3d/example/media/thumbnails/canvas_JoglAwtDesktopExample.jpg", //
maxHeapMemory = 64)
public class JoglAwtDesktopExample {
    static MouseCursor _cursor1;
    static MouseCursor _cursor2;

    static Map<Canvas, Boolean> _showCursor1 = new HashMap<Canvas, Boolean>();

    public static void main(final String[] args) throws Exception {
        System.setProperty("ardor3d.useMultipleContexts", "true");
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);

        final Timer timer = new Timer();
        final FrameHandler frameWork = new FrameHandler(timer);

        final MyExit exit = new MyExit();
        final LogicalLayer logicalLayer = new LogicalLayer();

        final ExampleScene scene1 = new ExampleScene();
        final RotatingCubeGame game1 = new RotatingCubeGame(scene1, exit, logicalLayer, Key.T);

        final ExampleScene scene2 = new ExampleScene();
        final RotatingCubeGame game2 = new RotatingCubeGame(scene2, exit, logicalLayer, Key.G);

        frameWork.addUpdater(game1);
        frameWork.addUpdater(game2);

        final JFrame frame = new JFrame("AWT Example");
        final JDesktopPane desktop = new JDesktopPane();
        frame.setContentPane(desktop);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent e) {
                exit.exit();
            }
        });

        AWTImageLoader.registerLoader();

        try {
            final SimpleResourceLocator srl = new SimpleResourceLocator(ResourceLocatorTool.getClassPathResource(
                    JoglAwtDesktopExample.class, "com/ardor3d/example/media/"));
            ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_TEXTURE, srl);
        } catch (final URISyntaxException ex) {
            ex.printStackTrace();
        }

        final AWTImageLoader awtImageLoader = new AWTImageLoader();
        _cursor1 = createMouseCursor(awtImageLoader, "com/ardor3d/example/media/input/wait_cursor.png");
        _cursor2 = createMouseCursor(awtImageLoader, "com/ardor3d/example/media/input/movedata.gif");

        addCanvas(desktop, scene1, logicalLayer, frameWork, 1);
        addCanvas(desktop, scene1, logicalLayer, frameWork, 2);
        addCanvas(desktop, scene2, logicalLayer, frameWork, 3);

        frame.setPreferredSize(new Dimension(1024, 768));
        frame.pack();
        frame.setVisible(true);

        game1.init();
        game2.init();

        while (!exit.isExit()) {
            frameWork.updateFrame();
            Thread.yield();
        }

        frame.dispose();
        System.exit(0);
    }

    private static MouseCursor createMouseCursor(final AWTImageLoader awtImageLoader, final String resourceName)
            throws IOException {
        final com.ardor3d.image.Image image = awtImageLoader.load(ResourceLocatorTool.getClassPathResourceAsStream(
                JoglAwtDesktopExample.class, resourceName), false);

        return new MouseCursor("cursor1", image, 0, image.getHeight() - 1);
    }

    private static void addCanvas(final JDesktopPane desktop, final ExampleScene scene,
            final LogicalLayer logicalLayer, final FrameHandler frameWork, final int index) throws Exception {
        final JInternalFrame frame = new JInternalFrame(String.valueOf(index), true, true, true, true);
        final JoglCanvasRenderer canvasRenderer = new JoglCanvasRenderer(scene);

        final DisplaySettings settings = new DisplaySettings(400, 300, 24, 0, 0, 16, 0, 0, false, false);
        final JoglAwtCanvas theCanvas = new JoglAwtCanvas(settings, canvasRenderer);

        _showCursor1.put(theCanvas, true);

        theCanvas.setSize(new Dimension(400, 300));
        theCanvas.setVisible(true);

        final AwtKeyboardWrapper keyboardWrapper = new AwtKeyboardWrapper(theCanvas);
        final AwtFocusWrapper focusWrapper = new AwtFocusWrapper(theCanvas);
        final AwtMouseManager mouseManager = new AwtMouseManager(theCanvas);
        final AwtMouseWrapper mouseWrapper = new AwtMouseWrapper(theCanvas, mouseManager);
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
        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(final ComponentEvent e) {
                final Camera cam = theCanvas.getCanvasRenderer().getCamera();
                if (cam != null) {
                    cam.resize(theCanvas.getWidth(), theCanvas.getHeight());
                    cam.setFrustumPerspective(cam.getFovY(), theCanvas.getWidth() / (float) theCanvas.getHeight(), cam
                            .getFrustumNear(), cam.getFrustumFar());
                }
            }
        });
        frame.setLocation(20 * index, 20 * index);
        frame.add(theCanvas);
        frame.pack();
        frame.setVisible(true);
        desktop.add(frame);
    }

    private static class MyExit implements Exit {
        private volatile boolean exit = false;

        public void exit() {
            exit = true;
        }

        public boolean isExit() {
            return exit;
        }
    }
}
