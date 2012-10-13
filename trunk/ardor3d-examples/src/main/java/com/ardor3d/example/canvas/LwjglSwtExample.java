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
import org.lwjgl.LWJGLException;

import com.ardor3d.example.Purpose;
import com.ardor3d.framework.Canvas;
import com.ardor3d.framework.CanvasRenderer;
import com.ardor3d.framework.FrameHandler;
import com.ardor3d.framework.lwjgl.LwjglCanvasCallback;
import com.ardor3d.framework.lwjgl.LwjglCanvasRenderer;
import com.ardor3d.framework.swt.SwtCanvas;
import com.ardor3d.image.util.AWTImageLoader;
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
 * This examples demonstrates how to render OpenGL (via LWJGL) on a SWT canvas.
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.canvas.LwjglSwtExample", //
thumbnailPath = "com/ardor3d/example/media/thumbnails/canvas_LwjglSwtExample.jpg", //
maxHeapMemory = 64)
public class LwjglSwtExample {
    static MouseCursor _cursor1;
    static MouseCursor _cursor2;

    static Map<Canvas, Boolean> _showCursor1 = new HashMap<Canvas, Boolean>();

    private static final Logger logger = Logger.getLogger(LwjglSwtExample.class.toString());
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
            public void handleEvent(final Event e) {
                Display.getDefault().asyncExec(new Runnable() {
                    public void run() {
                        addNewCanvas(tabFolder, scene, frameWork, logicalLayer);
                    }
                });
            }
        });
        item.setText("Add &3d Canvas");
        item.setAccelerator(SWT.MOD1 + '3');

        AWTImageLoader.registerLoader();

        try {
            final SimpleResourceLocator srl = new SimpleResourceLocator(ResourceLocatorTool.getClassPathResource(
                    LwjglSwtExample.class, "com/ardor3d/example/media/"));
            ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_TEXTURE, srl);
        } catch (final URISyntaxException ex) {
            ex.printStackTrace();
        }

        addNewCanvas(tabFolder, scene, frameWork, logicalLayer);

        shell.open();

        game.init();
        // frameWork.init();

        while (!shell.isDisposed() && !exit.isExit()) {
            display.readAndDispatch();
            frameWork.updateFrame();
            Thread.yield();

            // using the below way makes things really jerky. Not sure how to handle that.

            // if (display.readAndDispatch()) {
            // frameWork.updateFrame();
            // }
            // else {
            // display.sleep();
            // }
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

        final SwtCanvas canvas1 = new SwtCanvas(topLeft, SWT.NONE, data);
        final LwjglCanvasRenderer lwjglCanvasRenderer1 = new LwjglCanvasRenderer(scene);
        addCallback(canvas1, lwjglCanvasRenderer1);
        canvas1.setCanvasRenderer(lwjglCanvasRenderer1);
        frameWork.addCanvas(canvas1);
        canvas1.addControlListener(newResizeHandler(canvas1, lwjglCanvasRenderer1));
        canvas1.setFocus();

        final SwtCanvas canvas2 = new SwtCanvas(bottomLeft, SWT.NONE, data);
        final LwjglCanvasRenderer lwjglCanvasRenderer2 = new LwjglCanvasRenderer(scene);
        addCallback(canvas2, lwjglCanvasRenderer2);
        canvas2.setCanvasRenderer(lwjglCanvasRenderer2);
        frameWork.addCanvas(canvas2);
        canvas2.addControlListener(newResizeHandler(canvas2, lwjglCanvasRenderer2));

        final SwtCanvas canvas3 = new SwtCanvas(topRight, SWT.NONE, data);
        final LwjglCanvasRenderer lwjglCanvasRenderer3 = new LwjglCanvasRenderer(scene);
        addCallback(canvas3, lwjglCanvasRenderer3);
        canvas3.setCanvasRenderer(lwjglCanvasRenderer3);
        frameWork.addCanvas(canvas3);
        canvas3.addControlListener(newResizeHandler(canvas3, lwjglCanvasRenderer3));

        final SwtCanvas canvas4 = new SwtCanvas(bottomRight, SWT.NONE, data);
        final LwjglCanvasRenderer lwjglCanvasRenderer4 = new LwjglCanvasRenderer(scene);
        addCallback(canvas4, lwjglCanvasRenderer4);
        canvas4.setCanvasRenderer(lwjglCanvasRenderer4);
        frameWork.addCanvas(canvas4);
        canvas4.addControlListener(newResizeHandler(canvas4, lwjglCanvasRenderer4));

        final SwtKeyboardWrapper keyboardWrapper = new SwtKeyboardWrapper(canvas1);
        final SwtMouseWrapper mouseWrapper = new SwtMouseWrapper(canvas1);
        final SwtFocusWrapper focusWrapper = new SwtFocusWrapper(canvas1);
        final SwtMouseManager mouseManager = new SwtMouseManager(canvas1);
        final ControllerWrapper controllerWrapper = new DummyControllerWrapper();

        final PhysicalLayer pl = new PhysicalLayer(keyboardWrapper, mouseWrapper, controllerWrapper, focusWrapper);

        logicalLayer.registerInput(canvas1, pl);

        logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.H), new TriggerAction() {
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
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                if (source != canvas1) {
                    return;
                }

                mouseManager.setCursor(MouseCursor.SYSTEM_DEFAULT);
            }
        }));
        logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.G), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                if (source != canvas1) {
                    return;
                }

                mouseManager.setGrabbed(mouseManager.getGrabbed() == GrabbedState.NOT_GRABBED ? GrabbedState.GRABBED
                        : GrabbedState.NOT_GRABBED);
            }
        }));

        final AWTImageLoader awtImageLoader = new AWTImageLoader();
        try {
            _cursor1 = createMouseCursor(awtImageLoader, "com/ardor3d/example/media/input/wait_cursor.png");
            _cursor2 = createMouseCursor(awtImageLoader, "com/ardor3d/example/media/input/movedata.gif");
        } catch (final IOException ioe) {
            ioe.printStackTrace();
        }

        _showCursor1.put(canvas1, true);
    }

    private static void addCallback(final SwtCanvas canvas, final LwjglCanvasRenderer renderer) {
        renderer.setCanvasCallback(new LwjglCanvasCallback() {
            @Override
            public void makeCurrent() throws LWJGLException {
                canvas.setCurrent();
            }

            @Override
            public void releaseContext() throws LWJGLException {
                ; // do nothing?
            }
        });
    }

    private static MouseCursor createMouseCursor(final AWTImageLoader awtImageLoader, final String resourceName)
            throws IOException {
        final com.ardor3d.image.Image image = awtImageLoader.load(ResourceLocatorTool.getClassPathResourceAsStream(
                LwjglSwtExample.class, resourceName), false);

        return new MouseCursor("cursor1", image, 0, image.getHeight() - 1);
    }

    static ControlListener newResizeHandler(final SwtCanvas swtCanvas, final CanvasRenderer canvasRenderer) {
        final ControlListener retVal = new ControlListener() {
            public void controlMoved(final ControlEvent e) {}

            public void controlResized(final ControlEvent event) {
                final Rectangle size = swtCanvas.getClientArea();
                if ((size.width == 0) && (size.height == 0)) {
                    return;
                }
                final float aspect = (float) size.width / (float) size.height;
                final Camera camera = canvasRenderer.getCamera();
                if (camera != null) {
                    final float fovY = 45; // XXX no camera.getFov()
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

        public void exit() {
            exit = true;
        }

        public boolean isExit() {
            return exit;
        }
    }
}

// class LwjglSwtModule extends AbstractModule {
// public LwjglSwtModule() {}
//
// @Override
// protected void configure() {
// // enforce a single instance of SwtKeyboardWrapper will handle both the KeyListener and the KeyboardWrapper
// // interfaces
// bind(SwtKeyboardWrapper.class).in(Scopes.SINGLETON);
// bind(KeyboardWrapper.class).to(SwtKeyboardWrapper.class);
// bind(KeyListener.class).to(SwtKeyboardWrapper.class);
//
// bind(MouseWrapper.class).to(SwtMouseWrapper.class);
// }
// }