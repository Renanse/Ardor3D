/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example.renderer;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture2D;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.TextureRenderer;
import com.ardor3d.renderer.TextureRendererFactory;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.hint.LightCombineMode;
import com.ardor3d.scenegraph.shape.Quad;
import com.ardor3d.scenegraph.shape.Sphere;
import com.ardor3d.ui.text.BasicText;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureManager;

/**
 * Illustrates the TextureRenderer class; which renders a scene to a buffer and copying it to a texture.
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.renderer.RenderTextureSideBySideExample", //
thumbnailPath = "com/ardor3d/example/media/thumbnails/renderer_RenderTextureSideBySideExample.jpg", //
maxHeapMemory = 64)
public class RenderTextureSideBySideExample extends ExampleBase {
    private final Quaternion rotQuat = new Quaternion();
    private double angle = 0;
    private final Vector3 axis = new Vector3(1, 1, 0);
    private Sphere sphere;
    private Quad quad;
    private TextureRenderer textureRenderer;
    private Texture2D fakeTex;
    private boolean inited = false;

    public static void main(final String[] args) {
        start(RenderTextureSideBySideExample.class);
    }

    @Override
    protected void quit(final Renderer r) {
        try {
            if (textureRenderer != null) {
                textureRenderer.cleanup();
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
        super.quit(r);
    }

    double counter = 0;
    int frames = 0;

    @Override
    protected void updateExample(final ReadOnlyTimer timer) {
        final double tpf = timer.getTimePerFrame();
        counter += timer.getTimePerFrame();
        frames++;
        if (counter > 1) {
            final double fps = (frames / counter);
            counter = 0;
            frames = 0;
            System.out.printf("%7.1f FPS\n", fps);
        }
        if (tpf < 1) {
            angle = angle + (tpf * 1);
            if (angle > 360) {
                angle = 0;
            }
        }

        rotQuat.fromAngleAxis(angle, axis);
        sphere.setRotation(rotQuat);

    }

    @Override
    protected void renderExample(final Renderer renderer) {

        // lazy init the texture renderer
        if (!inited) {
            initRtt(renderer);
        }

        // render our "imposter"
        if (textureRenderer != null) {
            textureRenderer.render(sphere, fakeTex, Renderer.BUFFER_COLOR_AND_DEPTH);
        }

        // draw the root node...
        super.renderExample(renderer);
    }

    private void initRtt(final Renderer renderer) {
        inited = true;

        textureRenderer = TextureRendererFactory.INSTANCE.createTextureRenderer(_settings, false, renderer,
                ContextManager.getCurrentContext().getCapabilities());

        if (textureRenderer == null) {
            final BasicText t = BasicText.createDefaultTextLabel("Text", "RTT not supported on this computer.");
            t.getSceneHints().setRenderBucketType(RenderBucketType.Ortho);
            t.getSceneHints().setLightCombineMode(LightCombineMode.Off);
            t.setTranslation(new Vector3(0, 20, 0));
            _root.attachChild(t);
        } else {
            textureRenderer.getCamera().setLocation(new Vector3(-10, 0, 15));
            textureRenderer.setBackgroundColor(new ColorRGBA(0f, 0f, 0f, 1));
            fakeTex = new Texture2D();
            textureRenderer.setupTexture(fakeTex);
            final TextureState screen = new TextureState();
            screen.setTexture(fakeTex);
            screen.setEnabled(true);
            quad.setRenderState(screen);
        }
    }

    @Override
    protected void initExample() {
        _canvas.setTitle("RTT Side By Side");

        sphere = new Sphere("Sphere", 25, 25, 5);
        sphere.setTranslation(new Vector3(-10, 0, 0));
        sphere.setModelBound(new BoundingBox());
        _root.attachChild(sphere);

        quad = new Quad("Quad", 15, 13f);
        quad.setTranslation(new Vector3(10, 0, 0));
        quad.setModelBound(new BoundingBox());
        quad.getSceneHints().setLightCombineMode(LightCombineMode.Off);
        _root.attachChild(quad);

        final TextureState ts = new TextureState();
        ts.setEnabled(true);
        ts.setTexture(TextureManager.load("images/ardor3d_white_256.jpg", Texture.MinificationFilter.Trilinear,
                TextureStoreFormat.GuessCompressedFormat, true));

        _root.setRenderState(ts);
    }
}
