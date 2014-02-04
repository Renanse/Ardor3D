/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
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

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import com.ardor3d.example.Purpose;
import com.ardor3d.framework.Canvas;
import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.framework.FrameHandler;
import com.ardor3d.framework.jogl.JoglCanvasRenderer;
import com.ardor3d.framework.jogl.JoglNewtAwtCanvas;
import com.ardor3d.image.util.jogl.JoglImageLoader;
import com.ardor3d.input.ControllerWrapper;
import com.ardor3d.input.Key;
import com.ardor3d.input.MouseCursor;
import com.ardor3d.input.PhysicalLayer;
import com.ardor3d.input.jogl.JoglNewtFocusWrapper;
import com.ardor3d.input.jogl.JoglNewtKeyboardWrapper;
import com.ardor3d.input.jogl.JoglNewtMouseManager;
import com.ardor3d.input.jogl.JoglNewtMouseWrapper;
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
 * This examples demonstrates how to render OpenGL (via JOGL) on a NEWT AWT canvas. FIXME update the thumbnail and the
 * description
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.canvas.JoglAwtExample", //
thumbnailPath = "com/ardor3d/example/media/thumbnails/canvas_JoglAwtExample.jpg", //
maxHeapMemory = 64)
public class JoglNewtAwtExample {
    static MouseCursor _cursor1;
    static MouseCursor _cursor2;

    static Map<Canvas, Boolean> _showCursor1 = new HashMap<Canvas, Boolean>();

    public static void main(final String[] args) throws Exception {
        System.setProperty("ardor3d.useMultipleContexts", "true");

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

        final JFrame frame = new JFrame("NEWT AWT Example");
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent e) {
                exit.exit();
            }
        });

        frame.setLayout(new GridLayout(2, 3));

        JoglImageLoader.registerLoader();

        try {
            final SimpleResourceLocator srl = new SimpleResourceLocator(ResourceLocatorTool.getClassPathResource(
                    JoglNewtAwtExample.class, "com/ardor3d/example/media/"));
            ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_TEXTURE, srl);
        } catch (final URISyntaxException ex) {
            ex.printStackTrace();
        }

        final JoglImageLoader joglImageLoader = new JoglImageLoader();
        _cursor1 = createMouseCursor(joglImageLoader, "com/ardor3d/example/media/input/wait_cursor.png");
        _cursor2 = createMouseCursor(joglImageLoader, "com/ardor3d/example/media/input/movedata.gif");

        addCanvas(frame, scene1, logicalLayer, frameWork);
        frame.add(new JLabel(
                "<html>"
                        + "<table>"
                        + "<tr><th align=\"left\" style=\"font-size: 16\">Action</th><th align=\"left\" style=\"font-size: 16\">Command</th></tr>"
                        + "<tr><td>WS</td><td>Move camera position forward/back</td></tr>"
                        + "<tr><td>AD</td><td>Turn camera left/right</td></tr>"
                        + "<tr><td>QE</td><td>Strafe camera left/right</td></tr>"
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

        while (!exit.isExit()) {
            frameWork.updateFrame();
            Thread.yield();
        }

        frame.dispose();
        System.exit(0);
    }

    private static MouseCursor createMouseCursor(final JoglImageLoader joglImageLoader, final String resourceName)
            throws IOException {
        final com.ardor3d.image.Image image = joglImageLoader.load(
                ResourceLocatorTool.getClassPathResourceAsStream(JoglNewtAwtExample.class, resourceName), false);

        return new MouseCursor("cursor1", image, 0, image.getHeight() - 1);
    }

    private static void addCanvas(final JFrame frame, final ExampleScene scene, final LogicalLayer logicalLayer,
            final FrameHandler frameWork) throws Exception {
        final JoglCanvasRenderer canvasRenderer = new JoglCanvasRenderer(scene);

        final DisplaySettings settings = new DisplaySettings(400, 300, 24, 0, 0, 16, 0, 0, false, false);
        final JoglNewtAwtCanvas theCanvas = new JoglNewtAwtCanvas(settings, canvasRenderer);

        frame.add(theCanvas);

        _showCursor1.put(theCanvas, true);

        theCanvas.setSize(new Dimension(400, 300));
        theCanvas.setVisible(true);

        final JoglNewtKeyboardWrapper keyboardWrapper = new JoglNewtKeyboardWrapper(theCanvas);
        final JoglNewtFocusWrapper focusWrapper = new JoglNewtFocusWrapper(theCanvas);
        final JoglNewtMouseManager mouseManager = new JoglNewtMouseManager(theCanvas);
        final JoglNewtMouseWrapper mouseWrapper = new JoglNewtMouseWrapper(theCanvas, mouseManager);
        final ControllerWrapper controllerWrapper = new DummyControllerWrapper();

        final PhysicalLayer pl = new PhysicalLayer(keyboardWrapper, mouseWrapper, controllerWrapper, focusWrapper);

        logicalLayer.registerInput(theCanvas, pl);

        logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.H), new TriggerAction() {
            @Override
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
            @Override
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                if (source != theCanvas) {
                    return;
                }

                mouseManager.setCursor(MouseCursor.SYSTEM_DEFAULT);
            }
        }));

        frameWork.addCanvas(theCanvas);

    }

    private static class MyExit implements Exit {
        private volatile boolean exit = false;

        @Override
        public void exit() {
            exit = true;
        }

        public boolean isExit() {
            return exit;
        }
    }
}
