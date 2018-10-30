/**
 * Copyright (c) 2008-2018 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example.pbr;

import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.image.PixelDataType;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture.WrapMode;
import com.ardor3d.image.TextureCubeMap;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.light.PointLight;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.renderer.texture.CubeMapRenderUtil;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Renderable;
import com.ardor3d.scenegraph.shape.Box;
import com.ardor3d.scenegraph.shape.Teapot;
import com.ardor3d.surface.PbrSurface;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureManager;

/**
 * Port of a PBR example showing using an hdr skybox to provide diffuse irradiance lighting, originally from
 * https://learnopengl.com/PBR/IBL/Diffuse-irradiance
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.pbr.SimplePbrDiffuseIrradianceExample", //
        thumbnailPath = "com/ardor3d/example/media/thumbnails/pbr_SimplePbrDiffuseIrradianceExample.jpg", //
        maxHeapMemory = 64)
public class SimplePbrDiffuseIrradianceExample extends ExampleBase {

    int _lightCount = 4;
    PointLight _lights[] = new PointLight[_lightCount];

    TextureCubeMap irradianceTex, skyboxTex;

    public static void main(final String[] args) {
        start(SimplePbrDiffuseIrradianceExample.class);
    }

    @Override
    protected void initExample() {
        _canvas.setTitle("Ardor3d - Pbr Example ported from LearnOpenGL.com");
        _canvas.getCanvasRenderer().getRenderer().setBackgroundColor(new ColorRGBA(0.1f, 0.1f, 0.1f, 1.0f));
        _controlHandle.setMoveSpeed(20.0);

        buildSkybox();

        final int nrRows = 7, nrColumns = 7;
        final float spacing = 8f;

        final Teapot master = new Teapot("teapot");
        for (int row = 0; row < nrRows; ++row) {
            final float metallic = (float) row / (float) nrRows;
            for (int col = 0; col < nrColumns; ++col) {
                final float roughness = MathUtils.clamp((float) col / (float) nrColumns, 0.05f, 1.0f);

                final Vector3 vec = new Vector3((col - (nrColumns / 2)) * spacing, (row - (nrRows / 2)) * spacing,
                        0.0f);

                final Mesh mesh = master.makeCopy(true);
                mesh.setTranslation(vec);
                mesh.setProperty("surface", new PbrSurface(new ColorRGBA(0.5f, 0f, 0f, 1f), metallic, roughness, 1.0f));

                _root.attachChild(mesh);
            }
        }

        _lightState.detachAll();
        for (int i = 0; i < _lightCount; i++) {
            _lights[i] = new PointLight();
            _lights[i].setDiffuse(new ColorRGBA(900, 900, 900, 1));
            _lightState.attach(_lights[i]);
        }

        final TextureState ts = new TextureState();
        ts.setTexture(irradianceTex);
        _root.setRenderState(ts);

        _root.setRenderMaterial("pbr/pbr_untextured_irradiance.yaml");
    }

    private void buildSkybox() {
        final Box skybox = new Box("skybox");
        skybox.getSceneHints().setRenderBucketType(RenderBucketType.PostBucket);
        final TextureState ts = new TextureState();
        skybox.setRenderState(ts);

        final ZBufferState zs = new ZBufferState();
        zs.setFunction(ZBufferState.TestFunction.LessThanOrEqualTo);
        skybox.setRenderState(zs);

        // convert hdr rect texture to skybox cubemap

        ts.setTexture(TextureManager.load("images/skybox/MonValley_A_LookoutPoint_2k.hdr",
                Texture.MinificationFilter.BilinearNoMipMaps, true), 0);
        skybox.setRenderMaterial("hdr/equirect_to_cubemap.yaml");
        skybox.updateGeometricState(0);

        final CubeMapRenderUtil cubeUtil = new CubeMapRenderUtil(_canvas.getCanvasRenderer().getRenderer());
        cubeUtil.updateSettings(512, 512, 24, .1, 10);

        skyboxTex = new TextureCubeMap();
        skyboxTex.setTextureStoreFormat(TextureStoreFormat.RGBA16F);
        skyboxTex.setRenderedTexturePixelDataType(PixelDataType.Float);
        skyboxTex.setWrap(WrapMode.EdgeClamp);
        cubeUtil.renderToCubeMap((Renderable) skybox, skyboxTex, skybox.getWorldTranslation(),
                Renderer.BUFFER_COLOR_AND_DEPTH);

        // convert skybox cubemap to irradiance map

        ts.setTexture(skyboxTex);
        skybox.setRenderMaterial("pbr/cubemap_to_irradiance.yaml");
        skybox.updateGeometricState(0);

        irradianceTex = new TextureCubeMap();
        irradianceTex.setTextureStoreFormat(TextureStoreFormat.RGBA16F);
        irradianceTex.setRenderedTexturePixelDataType(PixelDataType.Float);
        irradianceTex.setWrap(WrapMode.EdgeClamp);
        cubeUtil.updateSettings(64, 64, 24, .1, 10);
        cubeUtil.renderToCubeMap((Renderable) skybox, irradianceTex, skybox.getWorldTranslation(),
                Renderer.BUFFER_COLOR_AND_DEPTH);

        // reuse our skybox for the scene now by updating our material and attaching to root

        skybox.setRenderMaterial("unlit/textured/cubemap_skybox.yaml");
        _root.attachChild(skybox);
    }

    @Override
    protected void updateExample(final ReadOnlyTimer timer) {
        for (int i = 0; i < _lightCount; i++) {
            _lights[i].setLocation(((i % 2 == 1) ? -30 : 30) + MathUtils.sin(timer.getTimeInSeconds() * 2) * 15,
                    ((i / 2) % 2 == 1) ? -30 : 30, 30f);
        }
    }
}
