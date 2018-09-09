/**
 * Copyright (c) 2008-2017 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.ui.backdrop;

import com.ardor3d.extension.ui.UIComponent;
import com.ardor3d.extension.ui.UIPieMenu;
import com.ardor3d.extension.ui.UIPieMenuItem;
import com.ardor3d.extension.ui.util.Insets;
import com.ardor3d.extension.ui.util.SubTex;
import com.ardor3d.extension.ui.util.UIArc;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Transform;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.BlendState.DestinationFunction;
import com.ardor3d.renderer.state.BlendState.SourceFunction;

/**
 * This backdrop paints a solid arc of color behind a UI component.
 */
public class SolidArcBackdrop extends UIBackdrop {

    /** The color to draw */
    protected final ColorRGBA _color = new ColorRGBA(ColorRGBA.GRAY);
    /** The disk used across all disk backdrops to render with. */
    private static UIArc _standin = SolidArcBackdrop.createStandinArc();
    static {
        SolidArcBackdrop._standin.setRenderMaterial("ui/untextured/default_color.yaml");
    }

    /**
     * Construct this backdrop, using the given color.
     *
     * @param color
     *            the color of the backdrop
     */
    public SolidArcBackdrop(final ReadOnlyColorRGBA color) {
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
        drawBackdrop(SolidArcBackdrop._standin, renderer, comp, null);
    }

    protected void drawBackdrop(final UIArc arc, final Renderer renderer, final UIComponent comp, final SubTex sub) {
        final float oldA = _color.getAlpha();
        if (oldA == 0) {
            // no need to draw.
            return;
        }

        _color.setAlpha(oldA * UIComponent.getCurrentOpacity());
        arc.setDefaultColor(_color);

        final Vector3 v = Vector3.fetchTempInstance();
        final Insets margin = comp.getMargin() != null ? comp.getMargin() : Insets.EMPTY;
        final Insets border = comp.getBorder() != null ? comp.getBorder() : Insets.EMPTY;
        v.set(margin.getLeft() + border.getLeft(), margin.getBottom() + border.getBottom(), 0);

        final Transform t = Transform.fetchTempInstance();
        if (comp instanceof UIPieMenuItem && comp.getParent() instanceof UIPieMenu) {
            final UIPieMenu pie = (UIPieMenu) comp.getParent();
            t.set(pie.getWorldTransform());
        } else {
            t.set(comp.getWorldTransform());
        }
        t.applyForwardVector(v);
        t.translate(v);
        Vector3.releaseTempInstance(v);

        arc.setWorldTransform(t);
        Transform.releaseTempInstance(t);

        double size = 0, inner = 0, angle = 0, length = MathUtils.TWO_PI;
        boolean ignoreArcEdges = true;
        if (comp instanceof UIPieMenu) {
            final UIPieMenu pie = (UIPieMenu) comp;
            size = pie.getOuterRadius() - 2;
            length = pie.getTotalArcLength();
            inner = pie.getInnerRadius();
            angle = pie.getStartAngle();
        } else if (comp instanceof UIPieMenuItem && comp.getParent() instanceof UIPieMenu) {
            final UIPieMenuItem item = (UIPieMenuItem) comp;
            final UIPieMenu pie = (UIPieMenu) comp.getParent();
            if (pie.getCenterItem() == item) {
                size = pie.getInnerRadius();
            } else {
                size = pie.getOuterRadius();
                inner = pie.getInnerRadius();
                length = pie.getSliceRadians();
                angle = pie.getSliceIndex(item) * length + pie.getStartAngle();
                ignoreArcEdges = false;
            }
        } else {
            size = Math.max(UIBackdrop.getBackdropWidth(comp), UIBackdrop.getBackdropHeight(comp)) / 2;
        }

        arc.resetGeometry(angle, length, size, inner, sub, ignoreArcEdges);
        arc.render(renderer);

        _color.setAlpha(oldA);
    }

    public static UIArc createStandinArc() {
        final UIArc arc = new UIArc("standin", MathUtils.TWO_PI / 60, 1, 0.5);

        final BlendState blend = new BlendState();
        blend.setBlendEnabled(true);
        blend.setSourceFunction(SourceFunction.SourceAlpha);
        blend.setDestinationFunction(DestinationFunction.OneMinusSourceAlpha);
        arc.setRenderState(blend);
        arc.updateWorldRenderStates(false);

        return arc;
    }
}