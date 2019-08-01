/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.example.renderer;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.image.Texture;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.DrawBufferTarget;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.StereoCamera;
import com.ardor3d.renderer.state.ColorMaskState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.shape.Box;
import com.ardor3d.scenegraph.shape.Sphere;
import com.ardor3d.scenegraph.shape.Teapot;
import com.ardor3d.scenegraph.shape.Torus;
import com.ardor3d.util.TextureManager;

/**
 * Illustrates the StereoCamera class, which allows for your stereo viewing pleasures.
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.renderer.StereoExample", //
        thumbnailPath = "com/ardor3d/example/media/thumbnails/renderer_StereoExample.jpg", //
        maxHeapMemory = 64)
public class StereoExample extends ExampleBase {

    private StereoCamera _camera;
    private ColorMaskState noRed, redOnly;

    /**
     * Change this to true to use side-by-side rendering. false will turn on left/right buffer swapping.
     */
    public static boolean _sideBySide = false;

    /**
     * Change this to true to use anaglyph style (red/cyan) 3d. False will do hardware based 3d.
     */
    public static boolean _useAnaglyph = true;

    public static void main(final String[] args) {
        _stereo = !_sideBySide && !_useAnaglyph;
        start(StereoExample.class);
    }

    @Override
    protected void initExample() {
        _canvas.setTitle("Stereo Example");

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
        _camera.setFocalDistance(1);
        _camera.setEyeSeparation(_camera.getFocalDistance() / 30.0);
        _camera.setAperture(45.0 * MathUtils.DEG_TO_RAD);
        _camera.setSideBySideMode(_sideBySide);
        _camera.setupLeftRightCameras();

        final Mesh tp = new Teapot("Teapot");
        tp.setModelBound(new BoundingBox());
        tp.setTranslation(new Vector3(0, 0, -50));
        tp.setScale(2);
        _root.attachChild(tp);

        final Mesh torus = new Torus("Torus", 16, 16, 1, 4);
        torus.setModelBound(new BoundingBox());
        torus.setTranslation(new Vector3(4, 0, -10));
        _root.attachChild(torus);

        final Mesh sphere = new Sphere("Sphere", 16, 16, 5);
        sphere.setModelBound(new BoundingBox());
        sphere.setTranslation(new Vector3(-8, 0, -30));
        _root.attachChild(sphere);

        final Box box = new Box("Box", new Vector3(), 50, 1, 50);
        box.setModelBound(new BoundingBox());
        box.setTranslation(new Vector3(0, -1, -25));
        _root.attachChild(box);

        final TextureState ts = new TextureState();
        ts.setEnabled(true);
        ts.setTexture(TextureManager.load("images/ardor3d_white_256.jpg", Texture.MinificationFilter.Trilinear, true));
        _root.setRenderState(ts);

        _root.setTranslation(0, -1, 0);
    }

    @Override
    protected void renderExample(final Renderer renderer) {

        // Update left and right camera frames based on current camera.
        _camera.updateLeftRightCameraFrames();

        // Left Eye
        {
            if (!_sideBySide && !_useAnaglyph) {
                // Set left back buffer
                renderer.setDrawBuffer(DrawBufferTarget.BackLeft);
                renderer.clearBuffers(Renderer.BUFFER_COLOR | Renderer.BUFFER_DEPTH);
            } else if (_useAnaglyph) {
                renderer.clearBuffers(Renderer.BUFFER_COLOR | Renderer.BUFFER_DEPTH);
                ContextManager.getCurrentContext().enforceState(redOnly);
            }

            // Set left cam
            _camera.switchToLeftCamera(renderer);

            // Draw scene
            renderer.draw(_root);
            super.renderDebug(renderer);
            renderer.renderBuckets();
        }

        // Right Eye
        {
            if (!_sideBySide && !_useAnaglyph) {
                // Set right back buffer
                renderer.setDrawBuffer(DrawBufferTarget.BackRight);
                renderer.clearBuffers(Renderer.BUFFER_COLOR | Renderer.BUFFER_DEPTH);
            } else if (_useAnaglyph) {
                renderer.clearBuffers(Renderer.BUFFER_DEPTH);
                ContextManager.getCurrentContext().enforceState(noRed);
            }

            // Set right cam
            _camera.switchToRightCamera(renderer);

            // draw scene
            renderer.draw(_root);
            super.renderDebug(renderer);
            renderer.renderBuckets();
        }

        if (_useAnaglyph) {
            ContextManager.getCurrentContext().clearEnforcedState(StateType.ColorMask);
        }
    }

    @Override
    protected void renderDebug(final Renderer renderer) {
        // ignore. We'll call super on the individual left/right renderings.
    }
}
