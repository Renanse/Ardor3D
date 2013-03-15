/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.ui.backdrop;

import com.ardor3d.extension.ui.UIComponent;
import com.ardor3d.extension.ui.util.UIQuad;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Transform;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.BlendState.DestinationFunction;
import com.ardor3d.renderer.state.BlendState.SourceFunction;

/**
 * This backdrop paints a solid rectangle of color behind a UI component.
 */
public class SolidBackdrop extends UIBackdrop {

    /** The color to draw */
    private final ColorRGBA _color = new ColorRGBA(ColorRGBA.GRAY);
    /** The quad used across all solid backdrops to render with. */
    private static UIQuad _standin = SolidBackdrop.createStandinQuad();

    /**
     * Construct this backdrop, using the given color.
     * 
     * @param color
     *            the color of the backdrop
     */
    public SolidBackdrop(final ReadOnlyColorRGBA color) {
        setColor(color);
    }

    /**
     * @return the color of this back drop.
     */
    public ReadOnlyColorRGBA getColor() {
        return _color;
    }

    /**
     * Set the color of this back drop.
     * 
     * @param color
     *            the color to use
     */
    public void setColor(final ReadOnlyColorRGBA color) {
        if (color != null) {
            _color.set(color);
        }
    }

    @Override
    public void draw(final Renderer renderer, final UIComponent comp) {

        final float oldA = _color.getAlpha();
        if (oldA == 0) {
            // no need to draw.
            return;
        }

        _color.setAlpha(oldA * UIComponent.getCurrentOpacity());
        SolidBackdrop._standin.setDefaultColor(_color);

        final Vector3 v = Vector3.fetchTempInstance();
        v.set(comp.getMargin().getLeft() + comp.getBorder().getLeft(), comp.getMargin().getBottom()
                + comp.getBorder().getBottom(), 0);

        final Transform t = Transform.fetchTempInstance();
        t.set(comp.getWorldTransform());
        t.applyForwardVector(v);
        t.translate(v);
        Vector3.releaseTempInstance(v);

        SolidBackdrop._standin.setWorldTransform(t);
        Transform.releaseTempInstance(t);

        final float width = UIBackdrop.getBackdropWidth(comp);
        final float height = UIBackdrop.getBackdropHeight(comp);
        SolidBackdrop._standin.resize(width, height);
        SolidBackdrop._standin.render(renderer);

        _color.setAlpha(oldA);
    }

    private static UIQuad createStandinQuad() {
        final UIQuad quad = new UIQuad("standin", 1, 1);

        final BlendState blend = new BlendState();
        blend.setBlendEnabled(true);
        blend.setSourceFunction(SourceFunction.SourceAlpha);
        blend.setDestinationFunction(DestinationFunction.OneMinusSourceAlpha);
        quad.setRenderState(blend);
        quad.updateWorldRenderStates(false);

        return quad;
    }
}
