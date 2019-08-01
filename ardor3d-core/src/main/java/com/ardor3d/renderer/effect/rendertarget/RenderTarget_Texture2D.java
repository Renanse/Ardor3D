/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.renderer.effect.rendertarget;

import java.util.EnumMap;
import java.util.List;

import com.ardor3d.image.Texture2D;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.effect.EffectManager;
import com.ardor3d.renderer.effect.TextureRendererPool;
import com.ardor3d.renderer.material.RenderMaterial;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.texture.TextureRenderer;
import com.ardor3d.scenegraph.Spatial;

public class RenderTarget_Texture2D implements RenderTarget {

    private final Texture2D _texture = new Texture2D();
    private final int _width, _height;
    private boolean _texSetup = false;
    private final ColorRGBA _backgroundColor = new ColorRGBA(ColorRGBA.BLACK_NO_ALPHA);

    public RenderTarget_Texture2D(final int width, final int height) {
        this(width, height, TextureStoreFormat.RGB8);
    }

    public RenderTarget_Texture2D(final int width, final int height, final TextureStoreFormat format) {
        _width = width;
        _height = height;
        _texture.setTextureStoreFormat(format);
    }

    @Override
    public void render(final EffectManager effectManager, final Camera camera, final List<Spatial> spatials,
            final RenderMaterial enforcedMaterial, final EnumMap<StateType, RenderState> enforcedStates) {
        render(effectManager.getCurrentRenderer(), camera, spatials, null, enforcedMaterial, enforcedStates);
    }

    @Override
    public void render(final EffectManager effectManager, final Camera camera, final Spatial spatial,
            final RenderMaterial enforcedMaterial, final EnumMap<StateType, RenderState> enforcedStates) {
        render(effectManager.getCurrentRenderer(), camera, null, spatial, enforcedMaterial, enforcedStates);
    }

    protected void render(final Renderer renderer, final Camera camera, final List<Spatial> spatials,
            final Spatial spatial, final RenderMaterial enforcedMaterial,
            final EnumMap<StateType, RenderState> enforcedStates) {
        final TextureRenderer texRend = TextureRendererPool.fetch(_width, _height, renderer);
        if (!_texSetup) {
            texRend.setupTexture(_texture);
            _texSetup = true;
        }

        // set desired bg color
        texRend.setBackgroundColor(_backgroundColor);

        // setup camera
        if (camera != null) {
            texRend.getCamera().setFrame(camera);
            texRend.getCamera().setFrustum(camera);
            texRend.getCamera().setProjectionMode(camera.getProjectionMode());
        }

        texRend.enforceMaterial(enforcedMaterial);
        texRend.enforceStates(enforcedStates);

        // draw to texture
        if (spatial != null) {
            texRend.renderSpatial(spatial, _texture, Renderer.BUFFER_COLOR_AND_DEPTH);
        } else {
            texRend.renderSpatials(spatials, _texture, Renderer.BUFFER_COLOR_AND_DEPTH);
        }

        texRend.clearEnforcedStates();
        texRend.enforceMaterial(null);
        TextureRendererPool.release(texRend);
    }

    @Override
    public Texture2D getTexture() {
        return _texture;
    }

    public int getWidth() {
        return _width;
    }

    public int getHeight() {
        return _height;
    }

    public ReadOnlyColorRGBA getBackgroundColor() {
        return _backgroundColor;
    }

    public void setBackgroundColor(final ReadOnlyColorRGBA color) {
        _backgroundColor.set(color);
    }
}
