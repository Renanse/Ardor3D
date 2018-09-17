/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
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
import com.ardor3d.input.Key;
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
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.BlendState.DestinationFunction;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.texture.TextureRenderer;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Point;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.hint.LightCombineMode;
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
    private Texture2D _blurBufferTexture = null;

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

            isInitialized = true;
        }

        _sceneTextureRenderer.getCamera().set(cam);
        _sceneTextureRenderer.renderSpatial(_root, _blurBufferTexture, Renderer.BUFFER_COLOR_AND_DEPTH);
        renderer.draw((Spatial) _screenQuad);
    }

    @Override
    protected void updateExample(final ReadOnlyTimer timer) {
        if (_rotationEnabled) {
            _rotation.fromAngles(0.0 * timer.getTimeInSeconds(), 0.1 * timer.getTimeInSeconds(),
                    0.0 * timer.getTimeInSeconds());
            _pointCubes.setRotation(_rotation);
        }
        if (_waveEnabled) {
//            _pointCubeShaderState.setUniform("time", (float) timer.getTimeInSeconds());
        }
        if (_scaleEnabled) {
            _boxScale.set((float) (7 + 6 * Math.cos(0.5 * timer.getTimeInSeconds())),
                    (float) (7 + 6 * Math.cos(0.5 * timer.getTimeInSeconds())),
                    (float) (7 + 6 * Math.cos(0.5 * timer.getTimeInSeconds())), 1);
//            _pointCubeShaderState.setUniform("scale", _boxScale);
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
        // _canvas.setVSyncEnabled( true );
        _canvas.setTitle("Box Madness");
        cam.setLocation(new Vector3(0, 600, 800));
        cam.lookAt(new Vector3(0, 0, 0), Vector3.UNIT_Y);

        _controlHandle.setMoveSpeed(200);

//        buildShader();
        buildPointSprites();

        _screenQuad = new Quad("0", cam.getWidth(), cam.getHeight());
        _screenQuad.setTranslation(cam.getWidth() / 2, cam.getHeight() / 2, 0);
        _screenQuad.getSceneHints().setRenderBucketType(RenderBucketType.OrthoOrder);
        _screenQuad.getSceneHints().setLightCombineMode(LightCombineMode.Off);
        _blurBlend = new BlendState();
        _blurBlend.setBlendEnabled(true);
        _blurBlend.setEnabled(true);
        _blurBlend.setConstantColor(_blurFactor);
        _blurBlend.setSourceFunction(BlendState.SourceFunction.ConstantAlpha);
        _blurBlend.setDestinationFunctionRGB(DestinationFunction.OneMinusConstantAlpha);
        _screenQuad.setRenderState(_blurBlend);
        _screenQuad.updateGeometricState(0);
        _screenQuad.updateWorldRenderStates(false);

        for (int i = 0; i < _exampleInfo.length; i++) {
            _exampleInfo[i] = BasicText.createDefaultTextLabel("Text", "", 16);
            _exampleInfo[i].setTranslation(new Vector3(10, (_exampleInfo.length - i - 1) * 20 + 10, 0));
            _root.attachChild(_exampleInfo[i]);
        }
        _exampleInfo[0].setText("[1] Toggle Rotation");
        _exampleInfo[1].setText("[2] Toggle Wave");
        _exampleInfo[2].setText("[3] Toggle Blur");
        _exampleInfo[3].setText("[4] Toggle Scale");
    }

//    private void buildShader() {
//        _pointCubeShaderState = new ShaderState();
//        _pointCubeShaderState.setShader(ShaderType.Vertex, "", s_vert);
//        _pointCubeShaderState.setShader(ShaderType.Geometry, "", s_geom);
//        _pointCubeShaderState.setShader(ShaderType.Fragment, "", s_frag);
//        _pointCubeShaderState.setUniform("texture", 0);
//        _pointCubeShaderState.setUniform("scale", _boxScale);
//    }

    private void buildPointSprites() {
        _pointCubes = new Point();
//        _pointCubes.setRenderState(_pointCubeShaderState);
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

    /**
     * Shader
     */

    private static final String s_vert = "varying mat4 mvp;" + "void main()" + "{"
            + "  mvp = gl_ModelViewProjectionMatrix;" +
            // TODO: frustum cull points so we don't create unnecessary geometry in
            // the geometry shader.
            "  gl_Position = gl_Vertex;" + "}";

    // TODO: Possible Optimization. Since with a cube we always know only 3 sides can be seen at any given time
    // Given a camera angle we should be able to calculate which three sides(6 triangles) to emit.
    // So a kind of back face culling before we even have a face :-)
    private static final String s_geom = "#version 150\n" + "layout(points) in;"
            + "layout(triangle_strip, max_vertices = 17) out;" + "" + "uniform vec4 scale;" + "uniform float time;"
            + "in mat4 mvp[];" + "out vec2 uv;" + "" +
            // Cube vertexes
            "  const vec4 cube[8] = vec4[8](" + "                                vec4(  1, -1, -1, 1),"
            + "                                vec4(  1,  1, -1, 1),"
            + "                                vec4( -1, -1, -1, 1),"
            + "                                vec4( -1,  1, -1, 1),"
            + "                                vec4(  1, -1,  1, 1),"
            + "                                vec4(  1,  1,  1, 1),"
            + "                                vec4( -1,  1,  1, 1),"
            + "                                vec4( -1, -1,  1, 1)" + "                               );" +
            // CubeMap texture coordinates for use with
            // Texture continuous tri strip(degenerate):
            // 326742031 131 65410
            "  const vec2 coord[16] = vec2[16](" + "                                   vec2( 0.25,   0),"
            + "                                   vec2( 0.50,   0),"
            + "                                   vec2( 0.25, 1.0/3),"
            + "                                   vec2( 0.50, 1.0/3),"
            + "                                   vec2( 0.50, 2.0/3),"
            + "                                   vec2( 0.75, 1.0/3),"
            + "                                   vec2( 0.75, 2.0/3),"
            + "                                   vec2( 1.00, 1.0/3),"
            + "                                   vec2( 1.00, 2.0/3),"
            + "                                   vec2( 0.00, 1.0/3),"
            + "                                   vec2( 0.00, 2.0/3),"
            + "                                   vec2( 0.25, 1.0/3),"
            + "                                   vec2( 0.25, 2.0/3),"
            + "                                   vec2( 0.50, 2.0/3),"
            + "                                   vec2( 0.25,   1),"
            + "                                   vec2( 0.50,   1)" + "                               );" +

            "" + "void main()" + "{" +
            // TODO: Support individual scaling of the cubes
            // XXX: Some hardcoded motion pattern use here
            "  float radius = sqrt( gl_in[0].gl_Position.x*gl_in[0].gl_Position.x + gl_in[0].gl_Position.y*gl_in[0].gl_Position.y + gl_in[0].gl_Position.z*gl_in[0].gl_Position.z);"
            + "  vec4 position = vec4( gl_in[0].gl_Position.x + 150*cos(0.8*time + 0.008*radius),"
            + "                        gl_in[0].gl_Position.y + 100*sin(0.8*time + 0.005*radius),"
            + "                        gl_in[0].gl_Position.z + 50*sin(0.5*time + 0.005*radius),"
            + "                        1);" + "  uv = coord[0];"
            + "  gl_Position = mvp[0]*(scale*cube[3] + position); EmitVertex();" + "  uv = coord[1];"
            + "  gl_Position = mvp[0]*(scale*cube[2] + position); EmitVertex();" + "  uv = coord[2];"
            + "  gl_Position = mvp[0]*(scale*cube[6] + position); EmitVertex();" + "  uv = coord[3];"
            + "  gl_Position = mvp[0]*(scale*cube[7] + position); EmitVertex();" + "  uv = coord[4];"
            + "  gl_Position = mvp[0]*(scale*cube[4] + position); EmitVertex();" + "  uv = coord[5];"
            + "  gl_Position = mvp[0]*(scale*cube[2] + position); EmitVertex();" + "  uv = coord[6];"
            + "  gl_Position = mvp[0]*(scale*cube[0] + position); EmitVertex();" + "  uv = coord[7];"
            + "  gl_Position = mvp[0]*(scale*cube[3] + position); EmitVertex();" + "  uv = coord[8];"
            + "  gl_Position = mvp[0]*(scale*cube[1] + position); EmitVertex();" +
            // Texture discontinuity need to use a degenerate strip to be able to use
            // texture mapping. We loose some performance here.
            // XXX: Can it be done in another way?
            // Optimal strip: 32674203165410
            // Texture continuous strip: 326742031 131 65410
            "  gl_Position = mvp[0]*(scale*cube[1] + position); EmitVertex();" + "  uv = coord[9];"
            + "  gl_Position = mvp[0]*(scale*cube[3] + position); EmitVertex();" + "  uv = coord[10];"
            + "  gl_Position = mvp[0]*(scale*cube[1] + position); EmitVertex();" + "  uv = coord[11];"
            + "  gl_Position = mvp[0]*(scale*cube[6] + position); EmitVertex();" + "  uv = coord[12];"
            + "  gl_Position = mvp[0]*(scale*cube[5] + position); EmitVertex();" + "  uv = coord[13];"
            + "  gl_Position = mvp[0]*(scale*cube[4] + position); EmitVertex();" + "  uv = coord[14];"
            + "  gl_Position = mvp[0]*(scale*cube[1] + position); EmitVertex();" + "  uv = coord[15];"
            + "  gl_Position = mvp[0]*(scale*cube[0] + position); EmitVertex();" + "  EndPrimitive();" + "}";

    private static final String s_frag = "uniform sampler2D texture;" + "" + "in vec2 uv;" + "" + "void main()" + "{"
            + "  gl_FragColor = vec4(texture2D(texture,uv));" + "}";

}
