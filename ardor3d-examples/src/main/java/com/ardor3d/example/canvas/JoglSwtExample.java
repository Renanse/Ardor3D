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

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.opengl.GLData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

import com.ardor3d.example.Purpose;
import com.ardor3d.framework.Canvas;
import com.ardor3d.framework.CanvasRenderer;
import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.framework.FrameHandler;
import com.ardor3d.framework.jogl.JoglCanvasRenderer;
import com.ardor3d.framework.jogl.JoglSwtCanvas;
import com.ardor3d.image.util.jogl.JoglImageLoader;
import com.ardor3d.input.ControllerWrapper;
import com.ardor3d.input.GrabbedState;
import com.ardor3d.input.Key;
import com.ardor3d.input.MouseCursor;
import com.ardor3d.input.PhysicalLayer;
import com.ardor3d.input.logical.DummyControllerWrapper;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyPressedCondition;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.input.logical.TriggerAction;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.input.swt.SwtFocusWrapper;
import com.ardor3d.input.swt.SwtKeyboardWrapper;
import com.ardor3d.input.swt.SwtMouseManager;
import com.ardor3d.input.swt.SwtMouseWrapper;
import com.ardor3d.renderer.Camera;
import com.ardor3d.util.Timer;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.SimpleResourceLocator;

/**
 * This examples demonstrates how to render OpenGL (via JOGL) in a SWT canvas.
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.canvas.JoglSwtExample", //
thumbnailPath = "com/ardor3d/example/media/thumbnails/canvas_JoglSwtExample.jpg", //
maxHeapMemory = 64)
public class JoglSwtExample {
    static MouseCursor _cursor1;
    static MouseCursor _cursor2;

    static Map<Canvas, Boolean> _showCursor1 = new HashMap<Canvas, Boolean>();

    private static final Logger logger = Logger.getLogger(JoglSwtExample.class.toString());
    private static int i = 0;

    public static void main(final String[] args) {
        System.setProperty("ardor3d.useMultipleContexts", "true");

        final Timer timer = new Timer();
        final FrameHandler frameWork = new FrameHandler(timer);
        final LogicalLayer logicalLayer = new LogicalLayer();

        final MyExit exit = new MyExit();
        final ExampleScene scene = new ExampleScene();
        final RotatingCubeGame game = new RotatingCubeGame(scene, exit, logicalLayer, Key.T);

        frameWork.addUpdater(game);

        // INIT SWT STUFF
        final Display display = new Display();
        final Shell shell = new Shell(display);
        shell.setLayout(new FillLayout());

        // This is our tab folder, it will be accepting our 3d canvases
        final TabFolder tabFolder = new TabFolder(shell, SWT.BORDER);

        // Add a menu item that will create and add a new canvas.
        final Menu bar = new Menu(shell, SWT.BAR);
        shell.setMenuBar(bar);

        final MenuItem fileItem = new MenuItem(bar, SWT.CASCADE);
        fileItem.setText("&Tasks");

        final Menu submenu = new Menu(shell, SWT.DROP_DOWN);
        fileItem.setMenu(submenu);
        final MenuItem item = new MenuItem(submenu, SWT.PUSH);
        item.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(final Event e) {
                Display.getDefault().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        addNewCanvas(tabFolder, scene, frameWork, logicalLayer);
                    }
                });
            }
        });
        item.setText("Add &3d Canvas");
        item.setAccelerator(SWT.MOD1 + '3');

        JoglImageLoader.registerLoader();

        try {
            final SimpleResourceLocator srl = new SimpleResourceLocator(ResourceLocatorTool.getClassPathResource(
                    JoglSwtExample.class, "com/ardor3d/example/media/"));
            ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_TEXTURE, srl);
        } catch (final URISyntaxException ex) {
            ex.printStackTrace();
        }

        addNewCanvas(tabFolder, scene, frameWork, logicalLayer);

        shell.open();

        game.init();

        while (!shell.isDisposed() && !exit.isExit()) {
            display.readAndDispatch();
            frameWork.updateFrame();
            Thread.yield();
        }

        display.dispose();
        System.exit(0);
    }

    private static void addNewCanvas(final TabFolder tabFolder, final ExampleScene scene, final FrameHandler frameWork,
            final LogicalLayer logicalLayer) {
        i++;
        logger.info("Adding canvas");

        // Add a new tab to hold our canvas
        final TabItem item = new TabItem(tabFolder, SWT.NONE);
        item.setText("Canvas #" + i);
        tabFolder.setSelection(item);
        final Composite canvasParent = new Composite(tabFolder, SWT.NONE);
        canvasParent.setLayout(new FillLayout());
        item.setControl(canvasParent);

        final GLData data = new GLData();
        data.depthSize = 8;
        data.doubleBuffer = true;

        final SashForm splitter = new SashForm(canvasParent, SWT.HORIZONTAL);

        final SashForm splitterLeft = new SashForm(splitter, SWT.VERTICAL);
        final Composite topLeft = new Composite(splitterLeft, SWT.NONE);
        topLeft.setLayout(new FillLayout());
        final Composite bottomLeft = new Composite(splitterLeft, SWT.NONE);
        bottomLeft.setLayout(new FillLayout());

        final SashForm splitterRight = new SashForm(splitter, SWT.VERTICAL);
        final Composite topRight = new Composite(splitterRight, SWT.NONE);
        topRight.setLayout(new FillLayout());
        final Composite bottomRight = new Composite(splitterRight, SWT.NONE);
        bottomRight.setLayout(new FillLayout());

        canvasParent.layout();

        final DisplaySettings settings = new DisplaySettings(400, 300, 24, 0, 0, 16, 0, 0, false, false);

        final JoglCanvasRenderer canvasRenderer1 = new JoglCanvasRenderer(scene);
        final JoglSwtCanvas canvas1 = new JoglSwtCanvas(settings, canvasRenderer1, topLeft, SWT.NONE);
        frameWork.addCanvas(canvas1);
        canvas1.addControlListener(newResizeHandler(canvas1, canvasRenderer1));

        final JoglCanvasRenderer canvasRenderer2 = new JoglCanvasRenderer(scene);
        final JoglSwtCanvas canvas2 = new JoglSwtCanvas(settings, canvasRenderer2, bottomLeft, SWT.NONE);
        frameWork.addCanvas(canvas2);
        canvas2.addControlListener(newResizeHandler(canvas2, canvasRenderer2));

        final JoglCanvasRenderer canvasRenderer3 = new JoglCanvasRenderer(scene);
        final JoglSwtCanvas canvas3 = new JoglSwtCanvas(settings, canvasRenderer3, topRight, SWT.NONE);
        frameWork.addCanvas(canvas3);
        canvas3.addControlListener(newResizeHandler(canvas3, canvasRenderer3));

        final JoglCanvasRenderer canvasRenderer4 = new JoglCanvasRenderer(scene);
        final JoglSwtCanvas canvas4 = new JoglSwtCanvas(settings, canvasRenderer4, bottomRight, SWT.NONE);
        frameWork.addCanvas(canvas4);
        canvas4.addControlListener(newResizeHandler(canvas4, canvasRenderer4));

        canvas1.setFocus();

        final SwtKeyboardWrapper keyboardWrapper = new SwtKeyboardWrapper(canvas1);
        final SwtMouseWrapper mouseWrapper = new SwtMouseWrapper(canvas1);
        final SwtFocusWrapper focusWrapper = new SwtFocusWrapper(canvas1);
        final SwtMouseManager mouseManager = new SwtMouseManager(canvas1);
        canvas1.setMouseManager(mouseManager);
        final ControllerWrapper controllerWrapper = new DummyControllerWrapper();

        final PhysicalLayer pl = new PhysicalLayer(keyboardWrapper, mouseWrapper, controllerWrapper, focusWrapper);

        logicalLayer.registerInput(canvas1, pl);

        logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.H), new TriggerAction() {
            @Override
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                if (source != canvas1) {
                    return;
                }

                if (_showCursor1.get(canvas1)) {
                    mouseManager.setCursor(_cursor1);
                } else {
                    mouseManager.setCursor(_cursor2);
                }

                _showCursor1.put(canvas1, !_showCursor1.get(canvas1));
            }
        }));
        logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.J), new TriggerAction() {
            @Override
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                if (source != canvas1) {
                    return;
                }

                mouseManager.setCursor(MouseCursor.SYSTEM_DEFAULT);
            }
        }));
        logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.G), new TriggerAction() {
            @Override
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                if (source != canvas1) {
                    return;
                }

                mouseManager.setGrabbed(mouseManager.getGrabbed() == GrabbedState.NOT_GRABBED ? GrabbedState.GRABBED
                        : GrabbedState.NOT_GRABBED);
            }
        }));

        final JoglImageLoader joglImageLoader = new JoglImageLoader();
        try {
            _cursor1 = createMouseCursor(joglImageLoader, "com/ardor3d/example/media/input/wait_cursor.png");
            _cursor2 = createMouseCursor(joglImageLoader, "com/ardor3d/example/media/input/movedata.gif");
        } catch (final IOException ioe) {
            ioe.printStackTrace();
        }

        _showCursor1.put(canvas1, true);
    }

    private static MouseCursor createMouseCursor(final JoglImageLoader joglImageLoader, final String resourceName)
            throws IOException {
        final com.ardor3d.image.Image image = joglImageLoader.load(
                ResourceLocatorTool.getClassPathResourceAsStream(JoglSwtExample.class, resourceName), false);

        return new MouseCursor("cursor1", image, 0, image.getHeight() - 1);
    }

    static ControlListener newResizeHandler(final JoglSwtCanvas swtCanvas, final CanvasRenderer canvasRenderer) {
        final ControlListener retVal = new ControlListener() {
            @Override
            public void controlMoved(final ControlEvent e) {}

            @Override
            public void controlResized(final ControlEvent event) {
                final Rectangle size = swtCanvas.getClientArea();
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
        };
        return retVal;
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
