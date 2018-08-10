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

import java.io.IOException;

import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.math.Vector2;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.ShaderState;
import com.ardor3d.renderer.state.ShaderState.ShaderType;
import com.ardor3d.scenegraph.shape.Quad;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.resource.ResourceLocatorTool;

/**
 * <p>
 * Demonstration of a GLSL effect titled 'To The Road Of Ribbon' by TX95 (2008).
 * </p>
 * <p>
 * Based on a production from the demoscene, the 1k intro by FRequency (http://www.pouet.net/prod.php?which=53939). It
 * made 2nd position in the Main demoparty held in France
 * </p>
 * <p>
 * Adapted from demo at http://iquilezles.org/apps/shadertoy/
 * </p>
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.renderer.GLSLRibbonExample", //
thumbnailPath = "com/ardor3d/example/media/thumbnails/renderer_GLSLRibbonExample.jpg", //
maxHeapMemory = 64)
public class GLSLRibbonExample extends ExampleBase {

    private ShaderState _shader;

    public static void main(final String[] args) {
        start(GLSLRibbonExample.class);
    }

    @Override
    protected void initExample() {
        _canvas.setTitle("'To The Road Of Ribbon' by TX95 - rendered in Ardor3D");
        final Camera cam = _canvas.getCanvasRenderer().getCamera();

        _shader = new ShaderState();
        try {
            _shader.setShader(ShaderType.Fragment, "road_ribbon", ResourceLocatorTool.getClassPathResourceAsString(
                    GLSLRibbonExample.class, "com/ardor3d/example/media/shaders/road_ribbon.frag"));
            _shader.setUniform("time", 0f);
            _shader.setUniform("resolution", new Vector2(cam.getWidth(), cam.getHeight()));
        } catch (final IOException ex) {
            ex.printStackTrace();
        }

        final Quad q = new Quad("fsq", cam.getWidth(), cam.getHeight());
        q.setRenderState(_shader);
        q.setTranslation(cam.getWidth() / 2, cam.getHeight() / 2, 0);
        q.getSceneHints().setRenderBucketType(RenderBucketType.Ortho);
        _root.attachChild(q);
    }

    @Override
    protected void updateExample(final ReadOnlyTimer timer) {
        _shader.setUniform("time", (float) timer.getTimeInSeconds());
    }
}
