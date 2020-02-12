/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.example.renderer;

import java.nio.FloatBuffer;

import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.framework.Canvas;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture.MagnificationFilter;
import com.ardor3d.image.Texture.MinificationFilter;
import com.ardor3d.image.Texture.WrapMode;
import com.ardor3d.image.Texture2D;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.input.keyboard.Key;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyPressedCondition;
import com.ardor3d.input.logical.TriggerAction;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.Vector4;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.BlendState.DestinationFunction;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.texture.TextureRenderer;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Point;
import com.ardor3d.scenegraph.shape.Quad;
import com.ardor3d.ui.text.BasicText;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.geom.BufferUtils;

/**
 * A more complex example of using geometry shaders. Requires support for geometry shaders (obviously).
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.renderer.PointCubeExample", //
        thumbnailPath = "com/ardor3d/example/media/thumbnails/renderer_PointCubeExample.jpg", //
        maxHeapMemory = 64)
public class PointCubeExample extends ExampleBase {
    protected Node _textNode = new Node();
    protected BasicText _exampleInfo[] = new BasicText[4];

    private Point _pointCubes;

    private TextureRenderer _sceneTextureRenderer;
    private Texture2D _blurBufferTexture;

    private final Matrix3 _rotation = new Matrix3();
    private Quad _screenQuad;
    private final ColorRGBA _blurFactor = new ColorRGBA(1, 1, 1, 1f);
    private BlendState _blurBlend;
    private final Vector4 _boxScale = new Vector4(2, 2, 2, 1);

    private boolean isInitialized;
    private boolean _rotationEnabled = true;
    private boolean _blurEnabled = true;
    private boolean _scaleEnabled = true;
    private boolean _waveEnabled = true;

    public static void main(final String[] args) {
        start(PointCubeExample.class);
    }

    @Override
    protected void renderExample(final Renderer renderer) {
        final Camera cam = _canvas.getCanvasRenderer().getCamera();

        if (!isInitialized) {
            _sceneTextureRenderer = renderer.createTextureRenderer(cam.getWidth(), cam.getHeight(), 24, 0);
            _sceneTextureRenderer.setBackgroundColor(new ColorRGBA(0.0f, 0.0f, 0.1f, 0f));

            _blurBufferTexture = new Texture2D();
            _blurBufferTexture.setWrap(Texture.WrapMode.EdgeClamp);
            _blurBufferTexture.setMinificationFilter(MinificationFilter.NearestNeighborNoMipMaps);
            _blurBufferTexture.setMagnificationFilter(Texture.MagnificationFilter.NearestNeighbor);
            _sceneTextureRenderer.setupTexture(_blurBufferTexture);

            final TextureState ts = new TextureState();
            ts.setEnabled(true);
            ts.setTexture(_blurBufferTexture);
            _screenQuad.setRenderState(ts);
            _screenQuad.updateWorldRenderStates(false);
            _screenQuad.setRenderMaterial("unlit/textured/fsq.yaml");

            isInitialized = true;
        }

        _sceneTextureRenderer.getCamera().set(cam);
        _sceneTextureRenderer.renderSpatial(_root, _blurBufferTexture, Renderer.BUFFER_COLOR_AND_DEPTH);

        super.renderExample(renderer);
    }

    @Override
    protected void updateExample(final ReadOnlyTimer timer) {
        if (_rotationEnabled) {
            _rotation.fromAngles(0.0 * timer.getTimeInSeconds(), 0.1 * timer.getTimeInSeconds(),
                    0.0 * timer.getTimeInSeconds());
            _pointCubes.setRotation(_rotation);
        }
        if (_waveEnabled) {
            _pointCubes.setProperty("time", (float) timer.getTimeInSeconds());
        }
        if (_scaleEnabled) {
            _boxScale.set((float) (7 + 6 * Math.cos(0.5 * timer.getTimeInSeconds())),
                    (float) (7 + 6 * Math.cos(0.5 * timer.getTimeInSeconds())),
                    (float) (7 + 6 * Math.cos(0.5 * timer.getTimeInSeconds())), 1);
            _pointCubes.setProperty("scale", _boxScale);
        }
        if (_blurEnabled) {
            _blurFactor.setAlpha((float) (0.05 + 0.2 * Math.abs(Math.cos(0.5 * timer.getTimeInSeconds()))));
        } else {
            _blurFactor.setAlpha(1);
        }
        _blurBlend.setConstantColor(_blurFactor);
    }

    @Override
    protected void initExample() {
        final Camera cam = _canvas.getCanvasRenderer().getCamera();
        cam.setFrustumFar(3000);
        _canvas.getCanvasRenderer().setFrameClear(Renderer.BUFFER_NONE); // needed for motion blur
        _canvas.setTitle("Box Madness");
        cam.setLocation(new Vector3(0, 600, 800));
        cam.lookAt(new Vector3(0, 0, 0), Vector3.UNIT_Y);

        _controlHandle.setMoveSpeed(200);

        buildPointSprites();

        _screenQuad = Quad.newFullScreenQuad();

        _blurBlend = new BlendState();
        _blurBlend.setBlendEnabled(true);
        _blurBlend.setEnabled(true);
        _blurBlend.setConstantColor(_blurFactor);
        _blurBlend.setSourceFunction(BlendState.SourceFunction.ConstantAlpha);
        _blurBlend.setDestinationFunctionRGB(DestinationFunction.OneMinusConstantAlpha);

        _screenQuad.setRenderState(_blurBlend);

        for (int i = 0; i < _exampleInfo.length; i++) {
            _exampleInfo[i] = BasicText.createDefaultTextLabel("Text", "", 16);
            _exampleInfo[i].setTranslation(new Vector3(10, (_exampleInfo.length - i - 1) * 20 + 10, 0));
            _orthoRoot.attachChild(_exampleInfo[i]);
        }
        _exampleInfo[0].setText("[1] Toggle Rotation");
        _exampleInfo[1].setText("[2] Toggle Wave");
        _exampleInfo[2].setText("[3] Toggle Blur");
        _exampleInfo[3].setText("[4] Toggle Scale");

        _orthoRoot.attachChild(_screenQuad);
    }

    private void buildPointSprites() {
        _pointCubes = new Point();

        _pointCubes.setRenderMaterial("point_cube_example.yaml");

        final Texture tex = TextureManager.load("images/cube_map.png", Texture.MinificationFilter.BilinearNoMipMaps,
                TextureStoreFormat.GuessCompressedFormat, true);
        tex.setMagnificationFilter(MagnificationFilter.Bilinear);
        final TextureState ts = new TextureState();
        ts.setTexture(tex);
        ts.getTexture().setWrap(WrapMode.EdgeClamp);
        ts.setEnabled(true);
        _pointCubes.setRenderState(ts);

        int xDim, yDim, zDim;
        xDim = yDim = zDim = 100;
        final int cubeCount = xDim * yDim * zDim;
        final FloatBuffer vBuf = BufferUtils.createVector3Buffer(cubeCount);
        float x, y, z;
        for (int k = 0; k < zDim; k++) {
            for (int j = 0; j < yDim; j++) {
                for (int i = 0; i < xDim; i++) {
                    x = 1000 * (0.5f - (float) i / xDim);
                    y = 1000 * (0.5f - (float) j / yDim);
                    z = 1000 * (0.5f - (float) k / zDim);
                    vBuf.put(x).put(y).put(z);
                }
            }
        }
        _pointCubes.getMeshData().setVertexBuffer(vBuf);
        _root.attachChild(_pointCubes);
        _pointCubes.setProperty("scale", _boxScale);

    }

    @Override
    public void registerInputTriggers() {
        super.registerInputTriggers();

        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.ONE), new TriggerAction() {
            public void perform(final Canvas canvas, final TwoInputStates inputState, final double tpf) {
                _rotationEnabled = !_rotationEnabled;
            }
        }));
        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.TWO), new TriggerAction() {
            public void perform(final Canvas canvas, final TwoInputStates inputState, final double tpf) {
                _waveEnabled = !_waveEnabled;
            }
        }));
        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.THREE), new TriggerAction() {
            public void perform(final Canvas canvas, final TwoInputStates inputState, final double tpf) {
                _blurEnabled = !_blurEnabled;
            }
        }));
        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.FOUR), new TriggerAction() {
            public void perform(final Canvas canvas, final TwoInputStates inputState, final double tpf) {
                _scaleEnabled = !_scaleEnabled;
            }
        }));
    }

}
