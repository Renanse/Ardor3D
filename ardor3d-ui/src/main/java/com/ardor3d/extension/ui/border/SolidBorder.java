/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.ui.border;

import com.ardor3d.extension.ui.UIComponent;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Transform;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.ShadingState;
import com.ardor3d.renderer.state.BlendState.DestinationFunction;
import com.ardor3d.renderer.state.BlendState.SourceFunction;
import com.ardor3d.renderer.state.ShadingState.ShadingMode;
import com.ardor3d.scenegraph.FloatBufferData;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.util.geom.BufferUtils;

/**
 * This border draws solid colored edges around a UI component. Each edge may be a different thickness and/or color.
 */
public class SolidBorder extends UIBorder {

    private static final Mesh _mesh = SolidBorder.createMesh();
    private static final float[] _verts = new float[16];
    private static final float[] _colors = new float[32];

    /** Our top color - defaults to GRAY. */
    private final ColorRGBA _topColor = new ColorRGBA(ColorRGBA.GRAY);
    /** Our left color - defaults to GRAY. */
    private final ColorRGBA _leftColor = new ColorRGBA(ColorRGBA.GRAY);
    /** Our bottom color - defaults to LIGHT_GRAY. */
    private final ColorRGBA _bottomColor = new ColorRGBA(ColorRGBA.LIGHT_GRAY);
    /** Our right color - defaults to LIGHT_GRAY. */
    private final ColorRGBA _rightColor = new ColorRGBA(ColorRGBA.LIGHT_GRAY);

    /**
     * Construct a border with the given thicknesses. Uses the default colors.
     * 
     * @param top
     * @param left
     * @param bottom
     * @param right
     */
    public SolidBorder(final int top, final int left, final int bottom, final int right) {
        super(top, left, bottom, right);
    }

    /**
     * Construct a border with the given thicknesses and colors
     * 
     * @param top
     * @param left
     * @param bottom
     * @param right
     * @param topColor
     * @param leftColor
     * @param bottomColor
     * @param rightColor
     */
    public SolidBorder(final int top, final int left, final int bottom, final int right,
            final ReadOnlyColorRGBA topColor, final ReadOnlyColorRGBA leftColor, final ReadOnlyColorRGBA bottomColor,
            final ReadOnlyColorRGBA rightColor) {
        super(top, left, bottom, right);
        setTopColor(topColor);
        setLeftColor(leftColor);
        setBottomColor(bottomColor);
        setRightColor(rightColor);
    }

    public ReadOnlyColorRGBA getBottomColor() {
        return _bottomColor;
    }

    public void setBottomColor(final ReadOnlyColorRGBA color) {
        _bottomColor.set(color);
    }

    public ReadOnlyColorRGBA getLeftColor() {
        return _leftColor;
    }

    public void setLeftColor(final ReadOnlyColorRGBA color) {
        _leftColor.set(color);
    }

    public ReadOnlyColorRGBA getRightColor() {
        return _rightColor;
    }

    public void setRightColor(final ReadOnlyColorRGBA color) {
        _rightColor.set(color);
    }

    public ReadOnlyColorRGBA getTopColor() {
        return _topColor;
    }

    public void setTopColor(final ReadOnlyColorRGBA color) {
        _topColor.set(color);
    }

    /**
     * Sets all of the border colors to the given color.
     * 
     * @param solidColor
     *            new color for all borders
     */
    public void setColor(final ReadOnlyColorRGBA solidColor) {
        setTopColor(solidColor);
        setBottomColor(solidColor);
        setLeftColor(solidColor);
        setRightColor(solidColor);
    }

    @Override
    public void draw(final Renderer renderer, final UIComponent comp) {
        // draw this border using a triangle-strip of 8 triangles.

        final float pAlpha = UIComponent.getCurrentOpacity();

        final Vector3 v = Vector3.fetchTempInstance();
        v.set(comp.getMargin().getLeft(), comp.getMargin().getBottom(), 0);

        final Transform t = Transform.fetchTempInstance();
        t.set(comp.getWorldTransform());
        t.applyForwardVector(v);
        t.translate(v);
        Vector3.releaseTempInstance(v);

        SolidBorder._mesh.setWorldTransform(t);
        Transform.releaseTempInstance(t);

        final int height = UIBorder.getBorderHeight(comp);
        final int width = UIBorder.getBorderWidth(comp);
        final int left = getLeft(), right = getRight(), top = getTop(), bottom = getBottom();

        // set vertices
        SolidBorder._verts[0] = 0;
        SolidBorder._verts[1] = 0;
        SolidBorder._verts[2] = left;
        SolidBorder._verts[3] = bottom;
        SolidBorder._verts[4] = 0;
        SolidBorder._verts[5] = height;
        SolidBorder._verts[6] = left;
        SolidBorder._verts[7] = height - top;
        SolidBorder._verts[8] = width;
        SolidBorder._verts[9] = height;
        SolidBorder._verts[10] = width - right;
        SolidBorder._verts[11] = height - top;
        SolidBorder._verts[12] = width;
        SolidBorder._verts[13] = 0;
        SolidBorder._verts[14] = width - right;
        SolidBorder._verts[15] = bottom;

        setColor(0, _bottomColor, pAlpha);
        setColor(1, _bottomColor, pAlpha);

        setColor(2, _leftColor, pAlpha);
        setColor(3, _leftColor, pAlpha);

        setColor(4, _topColor, pAlpha);
        setColor(5, _topColor, pAlpha);

        setColor(6, _rightColor, pAlpha);
        setColor(7, _rightColor, pAlpha);

        SolidBorder._mesh.getMeshData().getVertexBuffer().rewind();
        SolidBorder._mesh.getMeshData().getVertexBuffer().put(SolidBorder._verts);

        SolidBorder._mesh.getMeshData().getColorBuffer().rewind();
        SolidBorder._mesh.getMeshData().getColorBuffer().put(SolidBorder._colors);

        SolidBorder._mesh.render(renderer);
    }

    private void setColor(int i, final ReadOnlyColorRGBA color, final float pAlpha) {
        i *= 4;
        SolidBorder._colors[i] = color.getRed();
        SolidBorder._colors[i + 1] = color.getGreen();
        SolidBorder._colors[i + 2] = color.getBlue();
        SolidBorder._colors[i + 3] = color.getAlpha() * pAlpha;
    }

    private static Mesh createMesh() {
        // create a triangle strip of 8 triangles.
        final Mesh mesh = new Mesh();
        mesh.getMeshData().setVertexCoords(new FloatBufferData(BufferUtils.createVector2Buffer(8), 2));
        mesh.getMeshData().setColorBuffer(BufferUtils.createColorBuffer(8));
        mesh.getMeshData().setIndexMode(IndexMode.TriangleStrip);
        mesh.getMeshData().setIndexBuffer(BufferUtils.createIntBuffer(new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 0, 1 }));

        // set up alpha blending.
        final BlendState blend = new BlendState();
        blend.setBlendEnabled(true);
        blend.setSourceFunction(SourceFunction.SourceAlpha);
        blend.setDestinationFunction(DestinationFunction.OneMinusSourceAlpha);
        mesh.setRenderState(blend);

        // use flat shade so our borders will have a solid color.
        final ShadingState shading = new ShadingState();
        shading.setShadingMode(ShadingMode.Flat);
        mesh.setRenderState(shading);

        mesh.updateWorldRenderStates(false);

        return mesh;
    }
}
