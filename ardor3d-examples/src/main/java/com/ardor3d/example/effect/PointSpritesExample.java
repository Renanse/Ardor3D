/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.example.effect;

import java.nio.FloatBuffer;
import java.util.concurrent.Callable;

import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.framework.CanvasRenderer;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture.WrapMode;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.scenegraph.Point;
import com.ardor3d.scenegraph.hint.LightCombineMode;
import com.ardor3d.util.GameTaskQueue;
import com.ardor3d.util.GameTaskQueueManager;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.geom.BufferUtils;

/**
 * A demonstration of texturing Points.
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.effect.PointSpritesExample", //
        thumbnailPath = "com/ardor3d/example/media/thumbnails/effect_PointSpritesExample.jpg", //
        maxHeapMemory = 64)
public class PointSpritesExample extends ExampleBase {
    private final int _spriteCount = 60000;
    private Point _points;

    private final Matrix3 rotation = new Matrix3();

    public static void main(final String[] args) {
        start(PointSpritesExample.class);
    }

    @Override
    protected void updateExample(final ReadOnlyTimer timer) {
        _points.setProperty("time", (float) timer.getTimeInSeconds());

        rotation.fromAngles(0.0 * timer.getTimeInSeconds(), 0.1 * timer.getTimeInSeconds(),
                0.0 * timer.getTimeInSeconds());
        _points.setRotation(rotation);
    }

    @Override
    protected void initExample() {
        final Camera cam = _canvas.getCanvasRenderer().getCamera();
        final CanvasRenderer canvasRenderer = _canvas.getCanvasRenderer();
        final RenderContext renderContext = canvasRenderer.getRenderContext();
        final Renderer renderer = canvasRenderer.getRenderer();
        GameTaskQueueManager.getManager(renderContext).getQueue(GameTaskQueue.RENDER).enqueue(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                renderer.setBackgroundColor(ColorRGBA.BLACK);
                return null;
            }
        });
        _canvas.setVSyncEnabled(true);
        _canvas.setTitle("PointSprites");
        cam.setLocation(new Vector3(0, 30, 40));
        cam.lookAt(new Vector3(0, 0, 0), Vector3.UNIT_Y);

        buildPointSprites();
    }
//
//    private void buildShader() {
//        _pointSpriteShaderState = new ShaderState();
//        _pointSpriteShaderState.setShader(ShaderType.Vertex, s_vert_pointsprite);
//        _pointSpriteShaderState.setShader(ShaderType.Fragment, s_frag_pointsprite);
//        _pointSpriteShaderState.setUniform("texture", 0);
//        _pointSpriteShaderState.setUniform("time", 0f);
//    }

    private void buildPointSprites() {
        _points = new Point();
        _points.getSceneHints().setLightCombineMode(LightCombineMode.Off);
        _points.setPointSize(12);
        final TextureState ts = new TextureState();
        ts.setTexture(TextureManager.load("images/flare.png", Texture.MinificationFilter.NearestNeighborNoMipMaps,
                TextureStoreFormat.GuessCompressedFormat, true));
        ts.getTexture().setWrap(WrapMode.EdgeClamp);
        ts.setEnabled(true);
        _points.setRenderState(ts);

        final ZBufferState zb = new ZBufferState();
        zb.setWritable(false);
        _points.setRenderState(zb);

        final BlendState blend = new BlendState();
        blend.setBlendEnabled(true);
        blend.setEnabled(true);
        blend.setSourceFunction(BlendState.SourceFunction.SourceAlpha);
        blend.setDestinationFunction(BlendState.DestinationFunction.One);
        _points.setRenderState(blend);

        final FloatBuffer vBuf = BufferUtils.createVector3Buffer(_spriteCount);
        final FloatBuffer cBuf = BufferUtils.createVector4Buffer(_spriteCount);
        final Vector3 position = new Vector3();
        double x, y, r;
        for (int i = 0; i < _spriteCount; i++) {
            random(20, position);
            x = position.getX();
            y = position.getY();
            r = Math.sqrt(x * x + y * y);
            vBuf.put((float) x).put((float) (12 - 0.5 * r) * i / _spriteCount).put((float) y);
            final float rnd = (float) Math.random();
            final float rnd2 = rnd * (0.8f - 0.2f * (float) Math.random());
            cBuf.put((float) (20 - 0.9 * r) / 20 * (rnd + (1 - rnd) * i / _spriteCount)) //
                    .put((float) (20 - 0.9 * r) / 20 * (rnd2 + (1 - rnd2) * i / _spriteCount)) //
                    .put((float) (20 - 0.9 * r) / 20 * (0.2f + 0.2f * i / _spriteCount)) //
                    .put((float) r);
        }
        _points.getMeshData().setVertexBuffer(vBuf);
        _points.getMeshData().setColorBuffer(cBuf);
        _root.attachChild(_points);
    }

    public static void random(final float factor, final Vector3 store) {
        double x, y, z, len;

        do {
            x = 2 * Math.random() - 1.0;
            y = 2 * Math.random() - 1.0;
            z = 2 * Math.random() - 1.0;
            len = x * x + y * y + z * z;
        } while (len > 1);

        len = factor / Math.sqrt(len);
        store.set(x, y, z);
        store.multiplyLocal(len);
    }

    private static final String s_vert_pointsprite = //
            "uniform float time;" + //
                    "const float a = 3.1415/20.0;" + //
                    "void main()" + //
                    "{" + //
                    "  float radius = gl_Color.a;" + //
                    "  gl_FrontColor.rgb = gl_Color.rgb;" + //
                    "  gl_FrontColor.a = 1.0 - 0.02*radius;" + //
                    "  gl_Position=gl_ModelViewProjectionMatrix*vec4(gl_Vertex.x, " + //
                    "                                                gl_Vertex.y + (10.0-0.3*radius)*cos(time+a*radius), "
                    + //
                    "                                                gl_Vertex.z, 1.0);" + //
                    "}";

    private static final String s_frag_pointsprite = //
            "#version 120\n" + //
                    "uniform sampler2D texture;" + //
                    "" + //
                    "void main()" + //
                    "{" + //
                    "  gl_FragColor = vec4(texture2D(texture, gl_PointCoord)) * gl_Color;" + //
                    "}";
}
