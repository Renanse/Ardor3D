/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.terrain.providers.awt;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Map;

import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Transform;
import com.ardor3d.math.Vector4;
import com.ardor3d.math.type.ReadOnlyTransform;
import com.google.common.collect.Maps;

public class AwtShapeElement extends AbstractAwtElement {

    protected Shape _shape;

    protected boolean _filled = true;
    protected Color _fillColor = Color.WHITE;

    protected boolean _outlined = false;
    protected Color _outlineColor = Color.BLACK;

    protected float _strokeWidth = 1.0f;

    protected Vector4 _margin = new Vector4(1, 1, 1, 1);

    protected final Map<Integer, BasicStroke> _strokes = Maps.newHashMap();

    public AwtShapeElement(final Shape shape) {
        this(shape, Transform.IDENTITY, null);
    }

    public AwtShapeElement(final Shape shape, final ReadOnlyTransform transform) {
        this(shape, transform, null);
    }

    public AwtShapeElement(final Shape shape, final ReadOnlyTransform transform, final Composite compositeOverride) {
        super(transform, compositeOverride);
        _shape = shape;
        updateBounds();
    }

    public Shape getShape() {
        return _shape;
    }

    public void setShape(final Shape shape) {
        _shape = shape;
        updateBounds();
    }

    public boolean isFilled() {
        return _filled;
    }

    public void setFilled(final boolean filled) {
        _filled = filled;
    }

    public Color getFillColor() {
        return _fillColor;
    }

    public void setFillColor(final Color color) {
        _fillColor = color;
    }

    public boolean isOutlined() {
        return _outlined;
    }

    public void setOutlined(final boolean outlined) {
        _outlined = outlined;
    }

    public Color getOutlineColor() {
        return _outlineColor;
    }

    public void setOutlineColor(final Color color) {
        _outlineColor = color;
    }

    @Override
    public void updateBoundsFromElement() {
        final Rectangle2D rect = _shape.getBounds2D();
        _awtBounds.set(rect.getMinX() - _margin.getX(), rect.getMinY() - _margin.getY(),
                rect.getWidth() + _margin.getX() + _margin.getZ(), rect.getHeight() + _margin.getY() + _margin.getW());
    }

    @Override
    public void drawTo(final BufferedImage image, final ReadOnlyTransform localTransform, final int clipmapLevel) {
        // apply the two transforms together and then use result to scale/translate and rotate image
        final Transform trans = new Transform();
        localTransform.multiply(getTransform(), trans);

        // grab a copy of the graphics so we don't bleed state to next image
        final Graphics2D g2d = (Graphics2D) image.getGraphics().create();

        // apply hints
        for (final RenderingHints.Key key : hints.keySet()) {
            g2d.setRenderingHint(key, hints.get(key));
        }

        // set transform
        g2d.translate(trans.getTranslation().getX(), trans.getTranslation().getY());
        g2d.rotate(trans.getMatrix().toAngles(null)[2]); // rotation about z
        g2d.scale(trans.getScale().getX(), trans.getScale().getY());

        // set composite
        if (_compositeOverride != null) {
            g2d.setComposite(_compositeOverride);
        }

        // draw outline and/or fill
        if (_filled) {
            g2d.setColor(_fillColor);
            g2d.fill(_shape);
        }

        if (_outlined) {
            // set stroke
            BasicStroke stroke = _strokes.get(clipmapLevel);
            if (stroke == null) {
                stroke = new BasicStroke(_strokeWidth / MathUtils.pow2(clipmapLevel));
                _strokes.put(clipmapLevel, stroke);
            }
            g2d.setStroke(stroke);

            g2d.setColor(_outlineColor);
            g2d.draw(_shape);
        }
    }

    public Vector4 getMargin() {
        return _margin;
    }

    public void setMargin(final Vector4 margin) {
        _margin.set(margin);
    }

    public void setMargin(final double left, final double right, final double top, final double bottom) {
        _margin.set(left, top, right, bottom);
    }

    public void setMargin(final double outline) {
        _margin.set(outline, outline, outline, outline);
    }

    public void setStrokeWidth(final float width) {
        _strokeWidth = width;
        clearStrokes();
    }

    public float getStrokeWidth() {
        return _strokeWidth;
    }

    protected void clearStrokes() {
        _strokes.clear();
    }
}
