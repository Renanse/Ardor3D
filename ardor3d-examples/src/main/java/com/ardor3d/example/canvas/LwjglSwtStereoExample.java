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

import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.opengl.GLData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.lwjgl.LWJGLException;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.example.Purpose;
import com.ardor3d.example.basic.LwjglBasicExample;
import com.ardor3d.framework.Canvas;
import com.ardor3d.framework.CanvasRenderer;
import com.ardor3d.framework.Scene;
import com.ardor3d.framework.lwjgl.LwjglCanvasCallback;
import com.ardor3d.framework.lwjgl.LwjglCanvasRenderer;
import com.ardor3d.framework.swt.SwtCanvas;
import com.ardor3d.image.Texture;
import com.ardor3d.image.util.awt.AWTImageLoader;
import com.ardor3d.input.Key;
import com.ardor3d.input.PhysicalLayer;
import com.ardor3d.input.control.FirstPersonControl;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyPressedCondition;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.input.logical.TriggerAction;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.input.swt.SwtFocusWrapper;
import com.ardor3d.input.swt.SwtKeyboardWrapper;
import com.ardor3d.input.swt.SwtMouseManager;
import com.ardor3d.input.swt.SwtMouseWrapper;
import com.ardor3d.intersection.PickResults;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Ray3;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.StereoCamera;
import com.ardor3d.renderer.state.ColorMaskState;
import com.ardor3d.renderer.state.MaterialState;
import com.ardor3d.renderer.state.MaterialState.ColorMaterial;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.scene.state.lwjgl.util.SharedLibraryLoader;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.shape.Box;
import com.ardor3d.util.ContextGarbageCollector;
import com.ardor3d.util.GameTaskQueue;
import com.ardor3d.util.GameTaskQueueManager;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.Timer;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.SimpleResourceLocator;

/**
 * This examples demonstrates how to render to an SWT canvas in anaglyph stereo.
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.canvas.LwjglSwtStereoExample", //
        thumbnailPath = "com/ardor3d/example/media/thumbnails/canvas_LwjglSwtStereoExample.jpg", //
        maxHeapMemory = 64)
public class LwjglSwtStereoExample implements Scene {

    private static SwtCanvas _canvas;
    private static StereoCamera _camera;
    private static ColorMaskState noRed, redOnly;
    private static final AtomicBoolean _exit = new AtomicBoolean(false);
    private final Timer _timer = new Timer();

    private final Node _root = new Node("root");
    private final Mesh _box;
    private final Matrix3 _rotate = new Matrix3();
    private double _angle = 0;
    private final Vector3 _axis = new Vector3(1, 1, 0.5f).normalizeLocal();
    private final LogicalLayer _logicalLayer = new LogicalLayer();

    public LwjglSwtStereoExample() {
        _box = new Box("Box", new Vector3(0, 0, 0), 5, 5, 5);
        _box.setModelBound(new BoundingBox());
        _box.setTranslation(new Vector3(0, 0, -15));
        _box.setRandomColors();
        _root.attachChild(_box);

        // Add our awt based image loader.
        AWTImageLoader.registerLoader();

        // Set the location of our example resources.
        try {
            final SimpleResourceLocator srl = new SimpleResourceLocator(
                    ResourceLocatorTool.getClassPathResource(LwjglBasicExample.class, "com/ardor3d/example/media/"));
            ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_TEXTURE, srl);
        } catch (final URISyntaxException ex) {
            ex.printStackTrace();
        }

        // Add a texture to the box.
        final TextureState ts = new TextureState();
        ts.setTexture(TextureManager.load("images/ardor3d_white_256.jpg", Texture.MinificationFilter.Trilinear, true));
        _box.setRenderState(ts);

        // Add a material to the box, to show both vertex color and lighting/shading.
        final MaterialState ms = new MaterialState();
        ms.setColorMaterial(ColorMaterial.Diffuse);
        _box.setRenderState(ms);

        // Create a ZBuffer to display pixels closest to the camera above farther ones.
        final ZBufferState buf = new ZBufferState();
        buf.setEnabled(true);
        buf.setFunction(ZBufferState.TestFunction.LessThanOrEqualTo);
        _root.setRenderState(buf);

        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.ESCAPE), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputState, final double tpf) {
                _exit.set(true);
            }
        }));

        FirstPersonControl.setupTriggers(_logicalLayer, Vector3.UNIT_Y, true);
    }

    protected void doUpdate() {
        _timer.update();
        final double tpf = _timer.getTimePerFrame();
        _logicalLayer.checkTriggers(tpf);
        _angle += tpf * 50;
        _angle %= 360;

        _rotate.fromAngleNormalAxis(_angle * MathUtils.DEG_TO_RAD, _axis);
        _box.setRotation(_rotate);

        _root.updateGeometricState(tpf);
    }

    public static void main(final String[] args) {
        try {
            SharedLibraryLoader.load(true);
        } catch (final Exception e) {
            e.printStackTrace();
        }

        final LwjglSwtStereoExample scene = new LwjglSwtStereoExample();

        // INIT SWT STUFF
        final Display display = new Display();
        final Shell shell = new Shell(display);
        shell.setText("SWT Stereo Example");
        shell.setLayout(new FillLayout());

        AWTImageLoader.registerLoader();

        try {
            final SimpleResourceLocator srl = new SimpleResourceLocator(
                    ResourceLocatorTool.getClassPathResource(LwjglSwtExample.class, "com/ardor3d/example/media/"));
            ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_TEXTURE, srl);
        } catch (final URISyntaxException ex) {
            ex.printStackTrace();
        }

        setupCanvas(shell, scene);
        _canvas.init();
        setupStereo();

        shell.open();
        final CountDownLatch latch = new CountDownLatch(1);

        while (!shell.isDisposed() && !_exit.get()) {
            display.readAndDispatch();
            scene.doUpdate();
            _canvas.draw(latch);
            Thread.yield();
        }

        display.dispose();
        System.exit(0);
    }

    private static void setupStereo() {
        redOnly = new ColorMaskState();
        redOnly.setAll(true);
        redOnly.setBlue(false);
        redOnly.setGreen(false);

        noRed = new ColorMaskState();
        noRed.setAll(true);
        noRed.setRed(false);

        // setup our stereo camera as the new canvas camera
        _camera = new StereoCamera(_canvas.getCanvasRenderer().getCamera());
        _canvas.getCanvasRenderer().setCamera(_camera);

        // Setup our left and right camera using the parameters on the stereo camera itself
        // The Focal Distance and Eye Separation values are application and user dependent - you may need to play with
        // them to find a comfortable setup.
        _camera.setFocalDistance(1);
        _camera.setEyeSeparation(_camera.getFocalDistance() / 30.0);
        _camera.setAperture(45.0 * MathUtils.DEG_TO_RAD);
        _camera.setSideBySideMode(false);
        _camera.setupLeftRightCameras();
    }

    private static void setupCanvas(final Shell shell, final LwjglSwtStereoExample scene) {
        final GLData data = new GLData();
        data.depthSize = 8;
        data.doubleBuffer = true;
        data.stereo = true;

        _canvas = new SwtCanvas(shell, SWT.NONE, data);
        final LwjglCanvasRenderer renderer = new LwjglCanvasRenderer(scene);
        addCallback(_canvas, renderer);
        _canvas.setCanvasRenderer(renderer);
        _canvas.addControlListener(newResizeHandler(_canvas, renderer));
        _canvas.setFocus();

        final SwtKeyboardWrapper keyboardWrapper = new SwtKeyboardWrapper(_canvas);
        final SwtMouseWrapper mouseWrapper = new SwtMouseWrapper(_canvas);
        final SwtFocusWrapper focusWrapper = new SwtFocusWrapper(_canvas);
        _canvas.setMouseManager(new SwtMouseManager(_canvas));

        scene._logicalLayer.registerInput(_canvas, new PhysicalLayer(keyboardWrapper, mouseWrapper, focusWrapper));
    }

    private static void addCallback(final SwtCanvas canvas, final LwjglCanvasRenderer renderer) {
        renderer.setCanvasCallback(new LwjglCanvasCallback() {
            @Override
            public void makeCurrent() throws LWJGLException {
                canvas.setCurrent();
            }

            @Override
            public void releaseContext() throws LWJGLException {
            }
        });
    }

    static ControlListener newResizeHandler(final SwtCanvas swtCanvas, final CanvasRenderer canvasRenderer) {
        final ControlListener retVal = new ControlListener() {
            public void controlMoved(final ControlEvent e) {
            }

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

    @Override
    public boolean renderUnto(final Renderer renderer) {
        // Execute renderQueue item
        GameTaskQueueManager.getManager(ContextManager.getCurrentContext()).getQueue(GameTaskQueue.RENDER)
                .execute(renderer);

        // Clean up card garbage such as textures, vbos, etc.
        ContextGarbageCollector.doRuntimeCleanup(renderer);

        // Update left and right camera frames based on current camera.
        _camera.updateLeftRightCameraFrames();

        // Left Eye
        {
            // apply default color mask before we clear color.
            renderer.applyState(StateType.ColorMask, null);
            renderer.clearBuffers(Renderer.BUFFER_COLOR | Renderer.BUFFER_DEPTH);
            ContextManager.getCurrentContext().enforceState(redOnly);

            // Set left cam
            _camera.switchToLeftCamera(renderer);

            // Draw scene
            renderer.draw(_root);
            renderer.renderBuckets();
        }

        // Right Eye
        {
            renderer.clearBuffers(Renderer.BUFFER_DEPTH);
            ContextManager.getCurrentContext().enforceState(noRed);

            // Set right cam
            _camera.switchToRightCamera(renderer);

            // draw scene
            renderer.draw(_root);
            renderer.renderBuckets();
        }

        ContextManager.getCurrentContext().clearEnforcedState(StateType.ColorMask);

        return true;
    }

    @Override
    public PickResults doPick(final Ray3 pickRay) {
        // Ignore
        return null;
    }
}
