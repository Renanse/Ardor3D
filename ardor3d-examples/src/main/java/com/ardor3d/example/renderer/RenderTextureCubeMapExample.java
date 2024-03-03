/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.example.renderer;

import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.image.Texture;
import com.ardor3d.image.TextureCubeMap;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.util.MathUtils;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.texture.CubeMapRenderUtil;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.scenegraph.shape.Pyramid;
import com.ardor3d.scenegraph.shape.Sphere;
import com.ardor3d.util.TextureManager;

/**
 * Demonstrates rendering to texture, where the texture is a cubemap.
 */
@Purpose(
    htmlDescriptionKey = "com.ardor3d.example.renderer.RenderTextureCubeMapExample", //
    thumbnailPath = "com/ardor3d/example/media/thumbnails/renderer_RenderTextureCubeMapExample.jpg", //
    maxHeapMemory = 64)
public class RenderTextureCubeMapExample extends ExampleBase {

  protected TextureCubeMap texture;
  protected CubeMapRenderUtil cubeUtil;
  private Sphere sp;

  public static void main(final String[] args) {
    start(RenderTextureCubeMapExample.class);
  }

  @Override
  protected void initExample() {
    _canvas.setTitle("RTT CubeMap Example - Ardor3D");

    sp = new Sphere("sphere", 64, 64, 2);
    sp.getMeshData().copyTextureCoordinates(0, 1, 1f);
    _root.attachChild(sp);

    cubeUtil = new CubeMapRenderUtil(_canvas.getCanvasRenderer().getRenderer());
    cubeUtil.updateSettings(512, 512, 24, .1, 10);

    texture = new TextureCubeMap();

    final TextureState ts = new TextureState();
    // add base texture to unit 0
    ts.setTexture(TextureManager.load("images/ardor3d_white_256.jpg", Texture.MinificationFilter.Trilinear, true));
    // add reflection texture to unit 1
    ts.setTexture(texture, 1);
    sp.setRenderState(ts);
    sp.setRenderMaterial("rtt_cubemap_example.yaml");

    // add some moving "scenery"
    final Pyramid b = new Pyramid("box", 2, 3);
    b.setRotation(new Quaternion().fromAngleNormalAxis(MathUtils.PI, Vector3.UNIT_X));
    b.addController((time, caller) -> b.setTranslation(-3, 6 * Math.sin(_timer.getTimeInSeconds()), 0));
    _root.attachChild(b);

    // texture our scenery
    final TextureState ts2 = new TextureState();
    ts2.setTexture(TextureManager.load("images/ardor3d_white_256.jpg", Texture.MinificationFilter.Trilinear, true));
    b.setRenderState(ts2);

    _root.setRenderMaterial("unlit/textured/basic.yaml");
  }

  @Override
  protected void renderExample(final Renderer renderer) {
    // hide the sphere
    sp.getSceneHints().setCullHint(CullHint.Always);

    // render our scene to the cubemap
    cubeUtil.renderToCubeMap(_root, texture, sp.getWorldTranslation(), Renderer.BUFFER_COLOR_AND_DEPTH);

    // bring back the sphere
    sp.getSceneHints().setCullHint(CullHint.Inherit);

    // render our scene as normal
    super.renderExample(renderer);
  }
}
