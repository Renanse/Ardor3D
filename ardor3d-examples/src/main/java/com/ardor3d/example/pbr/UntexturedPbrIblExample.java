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
import com.ardor3d.image.Texture.MinificationFilter;
import com.ardor3d.image.Texture.WrapMode;
import com.ardor3d.image.Texture2D;
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
import com.ardor3d.scenegraph.shape.Quad;
import com.ardor3d.scenegraph.shape.Teapot;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureManager;

/**
 * Port of a PBR example showing using an hdr skybox to provide image based lighting, originally from
 * https://learnopengl.com/PBR/IBL/Specular-IBL
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.pbr.UntexturedPbrIblExample", //
        thumbnailPath = "com/ardor3d/example/media/thumbnails/pbr_UntexturedPbrIblExample.jpg", //
        maxHeapMemory = 64)
public class UntexturedPbrIblExample extends ExampleBase {

    int _lightCount = 4;
    PointLight _lights[] = new PointLight[_lightCount];

    TextureCubeMap skyboxTex, irradianceTex, prefilterTex;
    Texture2D brdfTex;

    public static void main(final String[] args) {
        start(UntexturedPbrIblExample.class);
    }

    @Override
    protected void initExample() {
        _canvas.setTitle("Ardor3d - Pbr Example ported from LearnOpenGL.com");
        _canvas.getCanvasRenderer().getRenderer().setBackgroundColor(new ColorRGBA(0.1f, 0.1f, 0.1f, 1.0f));
        _controlHandle.setMoveSpeed(20.0);

        buildTextures();

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
                mesh.setProperty("metallic", metallic);
                mesh.setProperty("roughness", roughness);
                mesh.setProperty("ao", 1.0f);
                mesh.setDefaultColor(new ColorRGBA(1f, 1f, 1f, 1f));

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
        ts.setTexture(irradianceTex, 0);
        ts.setTexture(prefilterTex, 1);
        ts.setTexture(brdfTex, 2);
        _root.setRenderState(ts);

        _root.setRenderMaterial("pbr/pbr_untextured_ibl.yaml");
    }

    private void buildTextures() {
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
        skyboxTex.setMinificationFilter(MinificationFilter.Trilinear);
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
        irradianceTex.setMinificationFilter(MinificationFilter.BilinearNoMipMaps);
        irradianceTex.setTextureStoreFormat(TextureStoreFormat.RGBA16F);
        irradianceTex.setRenderedTexturePixelDataType(PixelDataType.Float);
        irradianceTex.setWrap(WrapMode.EdgeClamp);
        cubeUtil.updateSettings(64, 64, 24, .1, 10);
        cubeUtil.renderToCubeMap((Renderable) skybox, irradianceTex, skybox.getWorldTranslation(),
                Renderer.BUFFER_COLOR_AND_DEPTH);

        // convert skybox cubemap to pre-filter map

        skybox.setRenderMaterial("pbr/cubemap_to_prefilter.yaml");
        skybox.updateGeometricState(0);

        prefilterTex = new TextureCubeMap();
        prefilterTex.setMinificationFilter(MinificationFilter.Trilinear);
        prefilterTex.setTextureStoreFormat(TextureStoreFormat.RGBA16F);
        prefilterTex.setRenderedTexturePixelDataType(PixelDataType.Float);
        prefilterTex.setWrap(WrapMode.EdgeClamp);

        // disable mip generation for this
        cubeUtil.getTextureRenderer().setEnableMipGeneration(false);

        final int maxMips = 5;
        for (int i = 0; i < maxMips; i++) {
            // resize framebuffer according to mip-level size.
            final int mipWidth = (int) (128 * Math.pow(0.5, i));
            final int mipHeight = (int) (128 * Math.pow(0.5, i));

            cubeUtil.updateSettings(mipWidth, mipHeight, 24, .1, 10);

            final float roughness = (float) i / (float) (maxMips - 1);
            skybox.setProperty("roughness", roughness);
            prefilterTex.setTexRenderMipLevel(i);
            cubeUtil.renderToCubeMap((Renderable) skybox, prefilterTex, skybox.getWorldTranslation(),
                    Renderer.BUFFER_COLOR_AND_DEPTH);
        }

        // clear our set property
        skybox.setProperty("roughness", null);

        // reuse our skybox for the scene now by updating our material and attaching to root
        skybox.setRenderMaterial("unlit/textured/cubemap_skybox.yaml");
        _root.attachChild(skybox);

        // render our BRDF texture
        final Quad quad = new Quad("fsq", 2, 2);
        quad.setRenderMaterial("pbr/fsq_brdf.yaml");
        quad.updateGeometricState(0);

        brdfTex = new Texture2D();
        brdfTex.setMinificationFilter(MinificationFilter.BilinearNoMipMaps);
        brdfTex.setTextureStoreFormat(TextureStoreFormat.RG16F);
        brdfTex.setRenderedTexturePixelDataType(PixelDataType.Float);
        brdfTex.setWrap(WrapMode.EdgeClamp);

        cubeUtil.updateSettings(512, 512, 24, .1, 10);
        cubeUtil.getTextureRenderer().setupTexture(brdfTex);
        cubeUtil.getTextureRenderer().render(quad, brdfTex, Renderer.BUFFER_COLOR_AND_DEPTH);

    }

    @Override
    protected void updateExample(final ReadOnlyTimer timer) {
        for (int i = 0; i < _lightCount; i++) {
            _lights[i].setLocation(((i % 2 == 1) ? -30 : 30) + MathUtils.sin(timer.getTimeInSeconds() * 2) * 15,
                    ((i / 2) % 2 == 1) ? -30 : 30, 30f);
        }
    }
}
