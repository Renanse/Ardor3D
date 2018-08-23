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

import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.image.Texture;
import com.ardor3d.image.TextureCubeMap;
import com.ardor3d.image.TextureCubeMap.Face;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.RendererCallable;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.texture.TextureRenderer;
import com.ardor3d.renderer.texture.TextureRendererFactory;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.controller.SpatialController;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.scenegraph.shape.Pyramid;
import com.ardor3d.scenegraph.shape.Sphere;
import com.ardor3d.util.GameTaskQueueManager;
import com.ardor3d.util.TextureManager;

/**
 * Demonstrates rendering to texture, where the texture is a cubemap.
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.renderer.RenderTextureCubeMapExample", //
thumbnailPath = "com/ardor3d/example/media/thumbnails/renderer_RenderTextureCubeMapExample.jpg", //
maxHeapMemory = 64)
public class RenderTextureCubeMapExample extends ExampleBase {

    protected TextureRenderer texRend = null;
    protected TextureCubeMap texture;
    private Sphere sp;

    public static void main(final String[] args) {
        start(RenderTextureCubeMapExample.class);
    }

    @Override
    protected void initExample() {
        _canvas.setTitle("RTT CubeMap Example - Ardor3D");

        GameTaskQueueManager.getManager(_canvas.getCanvasRenderer().getRenderContext()).render(
                new RendererCallable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        final DisplaySettings settings = new DisplaySettings(256, 256, 24, 0, 0, 24, 0, 0, false, false);
                        texRend = TextureRendererFactory.INSTANCE.createTextureRenderer(settings, getRenderer(),
                                ContextManager.getCurrentContext().getCapabilities());

                        texRend.getCamera().setFrustum(.1, 10, -.1, .1, .1, -.1);

                        texture = new TextureCubeMap();
                        // texture.setEnvironmentalMapMode(EnvironmentalMapMode.ObjectLinear);
                        // texture.setApply(ApplyMode.Combine);
                        // texture.setCombineFuncRGB(CombinerFunctionRGB.Interpolate);
                        // // color 1
                        // texture.setCombineSrc0RGB(CombinerSource.CurrentTexture);
                        // texture.setCombineOp0RGB(CombinerOperandRGB.SourceColor);
                        // // color 2
                        // texture.setCombineSrc1RGB(CombinerSource.Previous);
                        // texture.setCombineOp1RGB(CombinerOperandRGB.SourceColor);
                        // // interpolate param will come from alpha of constant color
                        // texture.setCombineSrc2RGB(CombinerSource.Constant);
                        // texture.setCombineOp2RGB(CombinerOperandRGB.SourceAlpha);
                        // texture.setConstantColor(0, 0, 0, .07f);

                        texRend.setupTexture(texture);

                        // add reflection texture to unit 1
                        final TextureState ts = (TextureState) sp.getLocalRenderState(StateType.Texture);
                        ts.setTexture(texture, 1);
                        return null;
                    }
                });

        sp = new Sphere("sphere", 16, 16, 2);
        sp.getMeshData().copyTextureCoordinates(0, 1, 1f);
        _root.attachChild(sp);

        // add base texture to unit 0
        final TextureState ts = new TextureState();
        ts.setTexture(TextureManager.load("images/ardor3d_white_256.jpg", Texture.MinificationFilter.Trilinear, true));
        sp.setRenderState(ts);

        // add some scenery
        final Pyramid b = new Pyramid("box", 2, 3);
        b.setRotation(new Quaternion().fromAngleNormalAxis(MathUtils.PI, Vector3.UNIT_X));
        b.addController(new SpatialController<Spatial>() {
            public void update(final double time, final Spatial caller) {
                b.setTranslation(-3, 6 * MathUtils.sin(_timer.getTimeInSeconds()), 0);
            };
        });
        _root.attachChild(b);

        // add base texture to unit 0
        final TextureState ts2 = new TextureState();
        ts2.setTexture(TextureManager.load("images/ardor3d_white_256.jpg", Texture.MinificationFilter.Trilinear, true));
        b.setRenderState(ts2);
    }

    @Override
    protected void renderExample(final Renderer renderer) {
        sp.getSceneHints().setCullHint(CullHint.Always);
        texRend.getCamera().setLocation(sp.getWorldTranslation());

        // render our scene from the sphere's point of view
        texRend.getCamera().setAxes(Vector3.NEG_UNIT_Z, Vector3.NEG_UNIT_Y, Vector3.NEG_UNIT_X);
        texture.setCurrentRTTFace(Face.NegativeX);
        texRend.render(_root, texture, Renderer.BUFFER_COLOR_AND_DEPTH);

        texRend.getCamera().setAxes(Vector3.UNIT_Z, Vector3.NEG_UNIT_Y, Vector3.UNIT_X);
        texture.setCurrentRTTFace(Face.PositiveX);
        texRend.render(_root, texture, Renderer.BUFFER_COLOR_AND_DEPTH);

        texRend.getCamera().setAxes(Vector3.NEG_UNIT_X, Vector3.NEG_UNIT_Z, Vector3.NEG_UNIT_Y);
        texture.setCurrentRTTFace(Face.NegativeY);
        texRend.render(_root, texture, Renderer.BUFFER_COLOR_AND_DEPTH);

        texRend.getCamera().setAxes(Vector3.NEG_UNIT_X, Vector3.UNIT_Z, Vector3.UNIT_Y);
        texture.setCurrentRTTFace(Face.PositiveY);
        texRend.render(_root, texture, Renderer.BUFFER_COLOR_AND_DEPTH);

        texRend.getCamera().setAxes(Vector3.UNIT_X, Vector3.NEG_UNIT_Y, Vector3.NEG_UNIT_Z);
        texture.setCurrentRTTFace(Face.NegativeZ);
        texRend.render(_root, texture, Renderer.BUFFER_COLOR_AND_DEPTH);

        texRend.getCamera().setAxes(Vector3.NEG_UNIT_X, Vector3.NEG_UNIT_Y, Vector3.UNIT_Z);
        texture.setCurrentRTTFace(Face.PositiveZ);
        texRend.render(_root, texture, Renderer.BUFFER_COLOR_AND_DEPTH);

        sp.getSceneHints().setCullHint(CullHint.Inherit);
        super.renderExample(renderer);
    }
}
