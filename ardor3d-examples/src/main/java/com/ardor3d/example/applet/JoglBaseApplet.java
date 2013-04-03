/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example.applet;

import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.net.URISyntaxException;
import java.util.concurrent.Callable;

import com.ardor3d.annotation.MainThread;
import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.framework.FrameHandler;
import com.ardor3d.framework.Scene;
import com.ardor3d.framework.Updater;
import com.ardor3d.framework.jogl.JoglCanvasRenderer;
import com.ardor3d.framework.jogl.JoglNewtAwtCanvas;
import com.ardor3d.image.util.awt.AWTImageLoader;
import com.ardor3d.input.GrabbedState;
import com.ardor3d.input.Key;
import com.ardor3d.input.MouseButton;
import com.ardor3d.input.PhysicalLayer;
import com.ardor3d.input.control.FirstPersonControl;
import com.ardor3d.input.jogl.JoglNewtFocusWrapper;
import com.ardor3d.input.jogl.JoglNewtKeyboardWrapper;
import com.ardor3d.input.jogl.JoglNewtMouseManager;
import com.ardor3d.input.jogl.JoglNewtMouseWrapper;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyReleasedCondition;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.input.logical.MouseButtonPressedCondition;
import com.ardor3d.input.logical.MouseButtonReleasedCondition;
import com.ardor3d.input.logical.TriggerAction;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.intersection.PickResults;
import com.ardor3d.math.Ray3;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.TextureRendererFactory;
import com.ardor3d.renderer.jogl.JoglTextureRendererProvider;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.util.Constants;
import com.ardor3d.util.ContextGarbageCollector;
import com.ardor3d.util.GameTaskQueue;
import com.ardor3d.util.GameTaskQueueManager;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.Timer;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.SimpleResourceLocator;
import com.ardor3d.util.stat.StatCollector;
import com.jogamp.newt.ScreenMode;

/**
 * An example base class for ardor3d/jogl applets. This is not meant to be a "best-practices" applet, just a rough demo
 * showing possibilities. As such, there are likely bugs, etc. Please report these. :)
 */
public abstract class JoglBaseApplet extends Applet implements Scene {

    private static final long serialVersionUID = 1L;

    protected DisplaySettings _settings;
    protected JoglNewtAwtCanvas _glCanvas;
    protected LogicalLayer _logicalLayer;
    protected PhysicalLayer _physicalLayer;
    protected JoglNewtMouseManager _mouseManager;

    protected FirstPersonControl _controlHandle;
    protected Vector3 _worldUp = new Vector3(0, 1, 0);

    protected Thread _gameThread;
    protected boolean _running = false;

    protected final Timer _timer = new Timer();
    protected final Node _root = new Node();
    protected final FrameHandler frameHandler = new FrameHandler(_timer);

    @Override
    public void init() {
        _settings = getSettings();
        setLayout(new BorderLayout(0, 0));
        try {
            TextureRendererFactory.INSTANCE.setProvider(new JoglTextureRendererProvider());
            final JoglCanvasRenderer canvasRenderer = new JoglCanvasRenderer(this);
            _glCanvas = new JoglNewtAwtCanvas(_settings, canvasRenderer) {
                private static final long serialVersionUID = 1L;

                @Override
                public final void removeNotify() {
                    stopJOGL();
                    super.removeNotify();
                }
            };
            ;
            _glCanvas.setSize(getWidth(), getHeight());
            _glCanvas.setFocusable(true);
            _glCanvas.requestFocus();
            _glCanvas.setIgnoreRepaint(true);
            _glCanvas.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(final ComponentEvent e) {
                    GameTaskQueueManager.getManager(_glCanvas.getCanvasRenderer().getRenderContext()).update(
                            new Callable<Void>() {
                                public Void call() throws Exception {
                                    final Camera cam = _glCanvas.getCanvasRenderer().getCamera();
                                    cam.resize(getWidth(), getHeight());
                                    cam.setFrustumPerspective(cam.getFovY(), getWidth() / (double) getHeight(),
                                            cam.getFrustumNear(), cam.getFrustumFar());
                                    appletResized(getWidth(), getHeight());
                                    return null;
                                }
                            });
                }
            });
            frameHandler.addCanvas(_glCanvas);
            frameHandler.addUpdater(new Updater() {

                @Override
                @MainThread
                public void update(final ReadOnlyTimer timer) {
                    JoglBaseApplet.this.update();
                }

                @Override
                @MainThread
                public void init() {
                    initInput();
                    initBaseScene();
                    initAppletScene();
                }
            });
            add(_glCanvas, BorderLayout.CENTER);
            setVisible(true);
            startJOGL();
        } catch (final Exception e) {
            System.err.println(e);
            throw new RuntimeException("Unable to create display");
        }
    }

    protected DisplaySettings getSettings() {
        return new DisplaySettings(getWidth(), getHeight(), 8, 0);
    }

    @Override
    public void destroy() {
        remove(_glCanvas);
    }

    protected void startJOGL() {
        frameHandler.init();
        _gameThread = new Thread() {
            @Override
            public void run() {
                _running = true;
                gameLoop();
            }
        };
        _gameThread.start();
    }

    protected void stopJOGL() {
        _running = false;
        try {
            _gameThread.join();
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected void gameLoop() {
        while (_running) {
            frameHandler.updateFrame();
            Thread.yield();
        }
    }

    public void update() {
        _timer.update();

        /** update stats, if enabled. */
        if (Constants.stats) {
            StatCollector.update();
        }
        updateLogicalLayer(_timer);

        // Execute updateQueue item
        GameTaskQueueManager.getManager(_glCanvas.getCanvasRenderer().getRenderContext())
                .getQueue(GameTaskQueue.UPDATE).execute();
        updateAppletScene(_timer);

        // Update controllers/render states/transforms/bounds for rootNode.
        _root.updateGeometricState(_timer.getTimePerFrame(), true);
    }

    protected void updateLogicalLayer(final ReadOnlyTimer timer) {
        // check and execute any input triggers, if we are concerned with input
        if (_logicalLayer != null) {
            _logicalLayer.checkTriggers(timer.getTimePerFrame());
        }
    }

    protected void initInput() {
        _mouseManager = new JoglNewtMouseManager(_glCanvas);
        _logicalLayer = new LogicalLayer();
        _physicalLayer = new PhysicalLayer(new JoglNewtKeyboardWrapper(_glCanvas), new JoglNewtMouseWrapper(_glCanvas,
                _mouseManager), new JoglNewtFocusWrapper(_glCanvas));
        _logicalLayer.registerInput(_glCanvas, _physicalLayer);
        _controlHandle = FirstPersonControl.setupTriggers(_logicalLayer, _worldUp, true);

        _logicalLayer.registerTrigger(new InputTrigger(new KeyReleasedCondition(Key.F), new TriggerAction() {

            public void perform(final com.ardor3d.framework.Canvas source, final TwoInputStates inputState,
                    final double tpf) {

                _glCanvas.getNewtWindow().setFullscreen(!_glCanvas.getNewtWindow().isFullscreen());
                final Camera cam = _glCanvas.getCanvasRenderer().getCamera();
                if (_glCanvas.getNewtWindow().isFullscreen()) {
                    final ScreenMode mode = _glCanvas.getNewtWindow().getScreen().getCurrentScreenMode();
                    cam.resize(mode.getMonitorMode().getScreenSizeMM().getWidth(), mode.getMonitorMode()
                            .getScreenSizeMM().getHeight());
                    cam.setFrustumPerspective(cam.getFovY(), mode.getMonitorMode().getScreenSizeMM().getWidth()
                            / (float) mode.getMonitorMode().getScreenSizeMM().getHeight(), cam.getFrustumNear(),
                            cam.getFrustumFar());
                    appletResized(mode.getMonitorMode().getScreenSizeMM().getWidth(), mode.getMonitorMode()
                            .getScreenSizeMM().getHeight());
                } else {
                    cam.resize(getWidth(), getHeight());
                    cam.setFrustumPerspective(cam.getFovY(), getWidth() / (float) getHeight(), cam.getFrustumNear(),
                            cam.getFrustumFar());
                    appletResized(getWidth(), getHeight());
                }
            }
        }));

        _logicalLayer.registerTrigger(new InputTrigger(new KeyReleasedCondition(Key.V), new TriggerAction() {
            public void perform(final com.ardor3d.framework.Canvas source, final TwoInputStates inputState,
                    final double tpf) {
                _glCanvas.setVSyncEnabled(true);
            }
        }));

        _logicalLayer.registerTrigger(new InputTrigger(new KeyReleasedCondition(Key.B), new TriggerAction() {
            public void perform(final com.ardor3d.framework.Canvas source, final TwoInputStates inputState,
                    final double tpf) {
                _glCanvas.setVSyncEnabled(false);
            }
        }));

        _logicalLayer.registerTrigger(new InputTrigger(new MouseButtonPressedCondition(MouseButton.LEFT),
                new TriggerAction() {
                    public void perform(final com.ardor3d.framework.Canvas source, final TwoInputStates inputState,
                            final double tpf) {
                        if (_mouseManager.isSetGrabbedSupported()) {
                            _mouseManager.setGrabbed(GrabbedState.GRABBED);
                        }
                    }
                }));

        _logicalLayer.registerTrigger(new InputTrigger(new MouseButtonReleasedCondition(MouseButton.LEFT),
                new TriggerAction() {
                    public void perform(final com.ardor3d.framework.Canvas source, final TwoInputStates inputState,
                            final double tpf) {
                        if (_mouseManager.isSetGrabbedSupported()) {
                            _mouseManager.setGrabbed(GrabbedState.NOT_GRABBED);
                        }
                    }
                }));
    }

    protected void initBaseScene() {
        // Add our awt based image loader.
        AWTImageLoader.registerLoader();

        // Set the location of our example resources.
        try {
            final SimpleResourceLocator srl = new SimpleResourceLocator(ResourceLocatorTool.getClassPathResource(
                    LwjglBaseApplet.class, "com/ardor3d/example/media/"));
            ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_TEXTURE, srl);
        } catch (final URISyntaxException ex) {
            ex.printStackTrace();
        }

        // Create a ZBuffer to display pixels closest to the camera above farther ones.
        final ZBufferState buf = new ZBufferState();
        buf.setEnabled(true);
        buf.setFunction(ZBufferState.TestFunction.LessThanOrEqualTo);
        _root.setRenderState(buf);
    }

    public PickResults doPick(final Ray3 pickRay) {
        // ignore
        return null;
    }

    public boolean renderUnto(final Renderer renderer) {
        // Execute renderQueue item
        GameTaskQueueManager.getManager(_glCanvas.getCanvasRenderer().getRenderContext())
                .getQueue(GameTaskQueue.RENDER).execute(renderer);

        // Clean up card garbage such as textures, vbos, etc.
        ContextGarbageCollector.doRuntimeCleanup(renderer);

        renderScene(renderer);

        return true;
    }

    protected abstract void initAppletScene();

    protected void updateAppletScene(final ReadOnlyTimer timer) {};

    protected void renderScene(final Renderer renderer) {
        // Draw the root and all its children.
        renderer.draw(_root);
    }

    protected void appletResized(final int width, final int height) {}
}
