/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example.basic;

import java.net.URISyntaxException;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.example.Purpose;
import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.framework.Scene;
import com.ardor3d.framework.lwjgl.LwjglCanvas;
import com.ardor3d.framework.lwjgl.LwjglCanvasRenderer;
import com.ardor3d.image.Texture;
import com.ardor3d.image.util.AWTImageLoader;
import com.ardor3d.intersection.PickResults;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Ray3;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.controller.SpatialController;
import com.ardor3d.scenegraph.shape.Box;
import com.ardor3d.util.ContextGarbageCollector;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.Timer;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.SimpleResourceLocator;

/**
 * <p>
 * This lwjgl-based example is meant to show how to use Ardor3D at the most primitive level, forsaking the use of
 * ExampleBase and much of Ardor3D's framework classes and interfaces.
 * </p>
 * 
 * <p>
 * Also of note, this example does not allow choosing of properties on launch. It also does not handle input or show any
 * special debugging. This is to simplify the example to the basic essentials.
 * </p>
 */

@Purpose(htmlDescriptionKey = "com.ardor3d.example.basic.LwjglBasicExample", //
thumbnailPath = "com/ardor3d/example/media/thumbnails/basic_LwjglBasicExample.jpg", //
maxHeapMemory = 64)
public class LwjglBasicExample implements Scene {

    // Our native window, not the gl surface itself.
    private final LwjglCanvas _canvas;

    // Our timer.
    private final Timer _timer = new Timer();

    // A boolean allowing us to "pull the plug" from anywhere.
    private boolean _exit = false;

    // The root of our scene
    private final Node _root = new Node();

    public static void main(final String[] args) {
        final LwjglBasicExample example = new LwjglBasicExample();
        example.start();
    }

    /**
     * Constructs the example class, also creating the native window and GL surface.
     */
    public LwjglBasicExample() {
        _canvas = initLwjgl();
        _canvas.init();
    }

    /**
     * Kicks off the example logic, first setting up the scene, then continuously updating and rendering it until exit
     * is flagged. Afterwards, the scene and gl surface are cleaned up.
     */
    private void start() {
        initExample();

        // Run in this same thread.
        while (!_exit) {
            updateExample();
            _canvas.draw(null);
            Thread.yield();
        }
        _canvas.getCanvasRenderer().makeCurrentContext();

        // Done, do cleanup
        ContextGarbageCollector.doFinalCleanup(_canvas.getCanvasRenderer().getRenderer());
        _canvas.close();

        _canvas.getCanvasRenderer().releaseCurrentContext();
    }

    /**
     * Setup an lwjgl canvas and canvas renderer.
     * 
     * @return the canvas.
     */
    private LwjglCanvas initLwjgl() {
        final LwjglCanvasRenderer canvasRenderer = new LwjglCanvasRenderer(this);
        final DisplaySettings settings = new DisplaySettings(800, 600, 24, 0, 0, 8, 0, 0, false, false);
        return new LwjglCanvas(settings, canvasRenderer);
    }

    /**
     * Initialize our scene.
     */
    private void initExample() {
        _canvas.setTitle("LwjglBasicExample - close window to exit");

        // Make a box...
        final Box _box = new Box("Box", Vector3.ZERO, 5, 5, 5);

        // Make it a bit more colorful.
        _box.setRandomColors();

        // Setup a bounding box for it.
        _box.setModelBound(new BoundingBox());

        // Set its location in space.
        _box.setTranslation(new Vector3(0, 0, -15));

        // Add to root.
        _root.attachChild(_box);

        // set it to rotate:
        _box.addController(new SpatialController<Spatial>() {
            private final Vector3 _axis = new Vector3(1, 1, 0.5f).normalizeLocal();
            private final Matrix3 _rotate = new Matrix3();
            private double _angle = 0;

            public void update(final double time, final Spatial caller) {
                // update our rotation
                _angle = _angle + (_timer.getTimePerFrame() * 25);
                if (_angle > 180) {
                    _angle = -180;
                }

                _rotate.fromAngleNormalAxis(_angle * MathUtils.DEG_TO_RAD, _axis);
                _box.setRotation(_rotate);
            }
        });

        // Add our awt based image loader.
        AWTImageLoader.registerLoader();

        // Set the location of our example resources.
        try {
            final SimpleResourceLocator srl = new SimpleResourceLocator(ResourceLocatorTool.getClassPathResource(
                    LwjglBasicExample.class, "com/ardor3d/example/media/"));
            ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_TEXTURE, srl);
        } catch (final URISyntaxException ex) {
            ex.printStackTrace();
        }

        // Create a ZBuffer to display pixels closest to the camera above farther ones.
        final ZBufferState buf = new ZBufferState();
        buf.setEnabled(true);
        buf.setFunction(ZBufferState.TestFunction.LessThanOrEqualTo);
        _root.setRenderState(buf);

        // Create a texture from the Ardor3D logo.
        final TextureState ts = new TextureState();
        ts.setEnabled(true);
        ts.setTexture(TextureManager.load("images/ardor3d_white_256.jpg", Texture.MinificationFilter.Trilinear, true));
        _root.setRenderState(ts);
    }

    /**
     * Update our scene... Check if the window is closing. Then update our timer and finally update the geometric state
     * of the root and its children.
     */
    private void updateExample() {
        if (_canvas.isClosing()) {
            _exit = true;
            return;
        }

        _timer.update();

        // Update controllers/render states/transforms/bounds for rootNode.
        _root.updateGeometricState(_timer.getTimePerFrame(), true);
    }

    // ------ Scene methods ------

    public boolean renderUnto(final Renderer renderer) {
        if (!_canvas.isClosing()) {

            // Draw the root and all its children.
            renderer.draw(_root);

            return true;
        }
        return false;
    }

    public PickResults doPick(final Ray3 pickRay) {
        // Ignore
        return null;
    }
}
