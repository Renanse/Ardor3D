/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.pass;

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.CullState;
import com.ardor3d.renderer.state.LightState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.WireframeState;
import com.ardor3d.renderer.state.CullState.Face;

/**
 * This Pass can be used for drawing an outline around geometry objects. It does this by first drawing the geometry as
 * normal, and then drawing an outline using the geometry's wireframe.
 */
public class OutlinePass extends RenderPass {

    private static final long serialVersionUID = 1L;

    public static final float DEFAULT_LINE_WIDTH = 3f;
    public static final ReadOnlyColorRGBA DEFAULT_OUTLINE_COLOR = new ColorRGBA(ColorRGBA.BLACK);

    // render states needed to draw the outline
    private final CullState _frontCull;
    private final CullState _backCull;
    private final WireframeState _wireframeState;
    private final LightState _noLights;
    private final TextureState _noTexture;
    private BlendState _blendState;

    public OutlinePass(final boolean antialiased) {
        _wireframeState = new WireframeState();
        _wireframeState.setFace(WireframeState.Face.FrontAndBack);
        _wireframeState.setLineWidth(DEFAULT_LINE_WIDTH);
        _wireframeState.setEnabled(true);

        _frontCull = new CullState();
        _frontCull.setCullFace(Face.Front);

        _backCull = new CullState();
        _backCull.setCullFace(Face.Back);

        _wireframeState.setAntialiased(antialiased);

        _noLights = new LightState();
        _noLights.setGlobalAmbient(DEFAULT_OUTLINE_COLOR);
        _noLights.setEnabled(true);

        _noTexture = new TextureState();
        _noTexture.setEnabled(true);

        _blendState = new BlendState();
        _blendState.setSourceFunction(BlendState.SourceFunction.SourceAlpha);
        _blendState.setDestinationFunction(BlendState.DestinationFunction.OneMinusSourceAlpha);
        _blendState.setBlendEnabled(true);
        _blendState.setEnabled(true);

    }

    @Override
    public void doRender(final Renderer renderer) {
        // if there's nothing to do
        if (_spatials.size() == 0) {
            return;
        }

        // normal render
        _context.enforceState(_frontCull);
        super.doRender(renderer);

        // set up the render states
        // CullState.setFlippedCulling(true);
        _context.enforceState(_backCull);
        _context.enforceState(_wireframeState);
        _context.enforceState(_noLights);
        _context.enforceState(_noTexture);
        _context.enforceState(_blendState);

        // this will draw the wireframe
        super.doRender(renderer);

        // revert state changes
        // CullState.setFlippedCulling(false);
        _context.clearEnforcedStates();
    }

    public void setOutlineWidth(final float width) {
        _wireframeState.setLineWidth(width);
    }

    public float getOutlineWidth() {
        return _wireframeState.getLineWidth();
    }

    public void setOutlineColor(final ReadOnlyColorRGBA outlineColor) {
        _noLights.setGlobalAmbient(outlineColor);
    }

    public ReadOnlyColorRGBA getOutlineColor() {
        return _noLights.getGlobalAmbient();
    }

    public BlendState getBlendState() {
        return _blendState;
    }

    public void setBlendState(final BlendState alphaState) {
        _blendState = alphaState;
    }
}
