/**
 * Copyright (c) 2008-2018 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example.canvas;

import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.lwjgl.LWJGLException;

import com.ardor3d.example.Purpose;
import com.ardor3d.framework.BasicScene;
import com.ardor3d.framework.CanvasRenderer;
import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.framework.FrameHandler;
import com.ardor3d.framework.lwjgl.LwjglCanvasCallback;
import com.ardor3d.framework.lwjgl.LwjglCanvasRenderer;
import com.ardor3d.image.util.awt.AWTImageLoader;
import com.ardor3d.input.ControllerWrapper;
import com.ardor3d.input.Key;
import com.ardor3d.input.PhysicalLayer;
import com.ardor3d.input.logical.DummyControllerWrapper;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.input.swt.SwtFocusWrapper;
import com.ardor3d.input.swt.SwtGestureWrapper;
import com.ardor3d.input.swt.SwtKeyboardWrapper;
import com.ardor3d.input.swt.SwtMouseManager;
import com.ardor3d.input.swt.SwtMouseWrapper;
import com.ardor3d.renderer.Camera;
import com.ardor3d.scene.state.lwjgl.util.SharedLibraryLoader;
import com.ardor3d.util.Timer;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.SimpleResourceLocator;

/**
 * This examples demonstrates how to render OpenGL (via LWJGL) on a SWT canvas via off-screen FBO rendering.
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.canvas.LwjglHeadlessSwtExample", //
thumbnailPath = "com/ardor3d/example/media/thumbnails/canvas_LwjglHeadlessSwtExample.jpg", //
maxHeapMemory = 64)
public class LwjglHeadlessSwtExample {
    private static final Logger logger = Logger.getLogger(LwjglHeadlessSwtExample.class.toString());
    private static RotatingCubeGame game;

    public static void main(final String[] args) {
        try {
            SharedLibraryLoader.load(true);
        } catch (final Exception e) {
            e.printStackTrace();
        }

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

        AWTImageLoader.registerLoader();

        try {
            final SimpleResourceLocator srl = new SimpleResourceLocator(ResourceLocatorTool.getClassPathResource(
                    LwjglSwtExample.class, "com/ardor3d/example/media/"));
            ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_TEXTURE, srl);
        } catch (final URISyntaxException ex) {
            ex.printStackTrace();
        }

        addNewFBOCanvas(shell, scene, frameWork, logicalLayer);

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

    private static void addNewFBOCanvas(final Shell shell, final BasicScene scene, final FrameHandler frameWork,
            final LogicalLayer logicalLayer) {
        logger.info("Adding canvas");

        final DisplaySettings settings = new DisplaySettings(32, 32, 32, 16);

        final SwtFboCanvas canvas = new SwtFboCanvas(shell, SWT.NONE, settings);
        final LwjglCanvasRenderer renderer = new LwjglCanvasRenderer(scene);
        canvas.setCanvasRenderer(renderer);
        frameWork.addCanvas(canvas);
        canvas.setFocus();

        addCanvasCallback(canvas, renderer);
        addControlListener(canvas, renderer);

        final SwtKeyboardWrapper keyboardWrapper = new SwtKeyboardWrapper(canvas);
        final SwtMouseWrapper mouseWrapper = new SwtMouseWrapper(canvas);
        final SwtFocusWrapper focusWrapper = new SwtFocusWrapper(canvas);
        final SwtMouseManager mouseManager = new SwtMouseManager(canvas);
        canvas.setMouseManager(mouseManager);
        final SwtGestureWrapper gestureWrapper = new SwtGestureWrapper(canvas, mouseWrapper, true);
        final ControllerWrapper controllerWrapper = new DummyControllerWrapper();

        final PhysicalLayer pl = new PhysicalLayer(keyboardWrapper, mouseWrapper, controllerWrapper, gestureWrapper,
                focusWrapper);

        logicalLayer.registerInput(canvas, pl);
    }

    private static void addCanvasCallback(final SwtFboCanvas canvas, final LwjglCanvasRenderer renderer) {
        renderer.setCanvasCallback(new LwjglCanvasCallback() {
            @Override
            public void makeCurrent() throws LWJGLException {
                canvas.setCurrent();
            }

            @Override
            public void releaseContext() throws LWJGLException {
                ; // do nothing
            }
        });
    }

    static void addControlListener(final Composite comp, final CanvasRenderer canvasRenderer) {
        comp.addControlListener(new ControlListener() {
            public void controlMoved(final ControlEvent e) {}

            public void controlResized(final ControlEvent event) {
                final Rectangle size = comp.getClientArea();
                if ((size.width == 0) && (size.height == 0)) {
                    return;
                }

                final float aspect = (float) size.width / (float) size.height;
                final Camera camera = canvasRenderer.getCamera();
                if (camera != null) {
                    final double fovY = camera.getFovY();
                    final double near = camera.getFrustumNear();
                    final double far = camera.getFrustumFar();
                    camera.setFrustumPerspective(fovY, aspect, near, far);
                    camera.resize(size.width, size.height);
                }
            }
        });
    }
}
