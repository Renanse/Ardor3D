/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
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
import com.ardor3d.math.Quaternion;
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
import com.ardor3d.surface.PbrTexturedSurface;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureManager;

/**
 * Port of a PBR example showing using an hdr skybox to provide image based lighting, originally
 * from https://learnopengl.com/PBR/IBL/Specular-IBL
 */
@Purpose(
    htmlDescriptionKey = "com.ardor3d.example.pbr.FullPbrExample", //
    thumbnailPath = "com/ardor3d/example/media/thumbnails/pbr_FullPbrExample.jpg", //
    maxHeapMemory = 64)
public class FullPbrExample extends ExampleBase {

  int _lightCount = 1;
  PointLight _lights[] = new PointLight[_lightCount];

  TextureCubeMap skyboxTex, irradianceTex, prefilterTex;
  Texture2D brdfTex;

  public static void main(final String[] args) {
    start(FullPbrExample.class);
  }

  @Override
  protected void initExample() {
    _canvas.setTitle("Ardor3d - Pbr Example ported from LearnOpenGL.com");
    _canvas.getCanvasRenderer().getRenderer().setBackgroundColor(new ColorRGBA(0.1f, 0.1f, 0.1f, 1.0f));
    _controlHandle.setMoveSpeed(20.0);

    buildTextures();

    addTeapots();

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

    _root.setRenderMaterial("pbr/pbr_textured_ibl.yaml");
    final PbrTexturedSurface surface = new PbrTexturedSurface();
    surface.setAlbedoMap(3);
    surface.setNormalMap(4);
    surface.setMetallicMap(5);
    surface.setRoughnessMap(6);
    surface.setAoMap(7);
    _root.setProperty("surface", surface);
  }

  private void addTeapots() {
    final Teapot master = new Teapot("teapot");
    master.setRotation(new Quaternion().fromAngleAxis(MathUtils.DEG_TO_RAD * -25, Vector3.UNIT_Y));
    master.setScale(0.66);

    {
      final Mesh teapot = master.makeCopy(true);
      teapot.setTranslation(new Vector3(-8, 0, 0));

      final TextureState ts = new TextureState();
      ts.setTexture(
          TextureManager.load("images/pbr/rustediron1-alt2/albedo.png", Texture.MinificationFilter.Trilinear, true), 3);
      ts.setTexture(
          TextureManager.load("images/pbr/rustediron1-alt2/normal.png", Texture.MinificationFilter.Trilinear, true), 4);
      ts.setTexture(
          TextureManager.load("images/pbr/rustediron1-alt2/metallic.png", Texture.MinificationFilter.Trilinear, true),
          5);
      ts.setTexture(
          TextureManager.load("images/pbr/rustediron1-alt2/roughness.png", Texture.MinificationFilter.Trilinear, true),
          6);
      ts.setTexture(TextureManager.load("images/white.png", Texture.MinificationFilter.Trilinear, true), 7); // AO
      teapot.setRenderState(ts);
      _root.attachChild(teapot);
    }

    {
      final Mesh teapot = master.makeCopy(true);
      teapot.setTranslation(new Vector3(-4, 0, 0));

      final TextureState ts = new TextureState();
      ts.setTexture(TextureManager.load("images/pbr/white-spruce-tree-bark/albedo.png",
          Texture.MinificationFilter.Trilinear, true), 3);
      ts.setTexture(TextureManager.load("images/pbr/white-spruce-tree-bark/normal.png",
          Texture.MinificationFilter.Trilinear, true), 4);
      ts.setTexture(TextureManager.load("images/pbr/white-spruce-tree-bark/metallic.png",
          Texture.MinificationFilter.Trilinear, true), 5);
      ts.setTexture(TextureManager.load("images/pbr/white-spruce-tree-bark/roughness.png",
          Texture.MinificationFilter.Trilinear, true), 6);
      ts.setTexture(
          TextureManager.load("images/pbr/white-spruce-tree-bark/ao.png", Texture.MinificationFilter.Trilinear, true),
          7);
      teapot.setRenderState(ts);
      _root.attachChild(teapot);
    }

    {
      final Mesh teapot = master.makeCopy(true);
      teapot.setTranslation(new Vector3(0, 0, 0));

      final TextureState ts = new TextureState();
      ts.setTexture(TextureManager.load("images/pbr/metalgrid4/albedo.png", Texture.MinificationFilter.Trilinear, true),
          3);
      ts.setTexture(TextureManager.load("images/pbr/metalgrid4/normal.png", Texture.MinificationFilter.Trilinear, true),
          4);
      ts.setTexture(
          TextureManager.load("images/pbr/metalgrid4/metallic.png", Texture.MinificationFilter.Trilinear, true), 5);
      ts.setTexture(
          TextureManager.load("images/pbr/metalgrid4/roughness.png", Texture.MinificationFilter.Trilinear, true), 6);
      ts.setTexture(TextureManager.load("images/pbr/metalgrid4/ao.png", Texture.MinificationFilter.Trilinear, true), 7);
      teapot.setRenderState(ts);
      _root.attachChild(teapot);
    }

    {
      final Mesh teapot = master.makeCopy(true);
      teapot.setTranslation(new Vector3(4, 0, 0));

      final TextureState ts = new TextureState();
      ts.setTexture(TextureManager.load("images/pbr/bamboo-wood-semigloss/albedo.png",
          Texture.MinificationFilter.Trilinear, true), 3);
      ts.setTexture(TextureManager.load("images/pbr/bamboo-wood-semigloss/normal.png",
          Texture.MinificationFilter.Trilinear, true), 4);
      ts.setTexture(TextureManager.load("images/pbr/bamboo-wood-semigloss/metallic.png",
          Texture.MinificationFilter.Trilinear, true), 5);
      ts.setTexture(TextureManager.load("images/pbr/bamboo-wood-semigloss/roughness.png",
          Texture.MinificationFilter.Trilinear, true), 6);
      ts.setTexture(
          TextureManager.load("images/pbr/bamboo-wood-semigloss/ao.png", Texture.MinificationFilter.Trilinear, true),
          7);
      teapot.setRenderState(ts);
      _root.attachChild(teapot);
    }

    {
      final Mesh teapot = master.makeCopy(true);
      teapot.setTranslation(new Vector3(8, 0, 0));

      final TextureState ts = new TextureState();
      ts.setTexture(
          TextureManager.load("images/pbr/pockedconcrete1/albedo.png", Texture.MinificationFilter.Trilinear, true), 3);
      ts.setTexture(
          TextureManager.load("images/pbr/pockedconcrete1/normal.png", Texture.MinificationFilter.Trilinear, true), 4);
      ts.setTexture(
          TextureManager.load("images/pbr/pockedconcrete1/metallic.png", Texture.MinificationFilter.Trilinear, true),
          5);
      ts.setTexture(
          TextureManager.load("images/pbr/pockedconcrete1/roughness.png", Texture.MinificationFilter.Trilinear, true),
          6);
      ts.setTexture(
          TextureManager.load("images/pbr/pockedconcrete1/ao.png", Texture.MinificationFilter.Trilinear, true), 7);
      teapot.setRenderState(ts);
      _root.attachChild(teapot);
    }

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
    skybox.removeProperty("roughness");

    // reuse our skybox for the scene now by updating our material and attaching to root
    skybox.setRenderMaterial("unlit/textured/cubemap_skybox.yaml");
    _root.attachChild(skybox);

    // render our BRDF texture
    final Quad quad = Quad.newFullScreenQuad();
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
