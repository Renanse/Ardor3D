/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.terrain.providers.awt;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Map;

import com.ardor3d.math.Transform;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.Vector4;
import com.ardor3d.math.type.ReadOnlyTransform;
import com.ardor3d.math.type.ReadOnlyVector4;
import com.google.common.collect.Maps;

public abstract class AbstractAwtElement {

    protected final Transform _transform = new Transform();
    protected Composite _compositeOverride;
    protected ElementUpdateListener _listener;
    protected Map<RenderingHints.Key, Object> hints = Maps.newHashMap();

    protected Vector4 _awtBounds = new Vector4();

    public AbstractAwtElement(final ReadOnlyTransform transform, final Composite compositeOverride) {
        _transform.set(transform);
        _compositeOverride = compositeOverride;

        hints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        hints.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        hints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    public ReadOnlyTransform getTransform() {
        return _transform;
    }

    public void setTransform(final ReadOnlyTransform transform) {
        _transform.set(transform);
        updateBounds();
    }

    public Composite getCompositeOverride() {
        return _compositeOverride;
    }

    public void setCompositeOverride(final Composite override) {
        _compositeOverride = override;
        updateBounds();
    }

    public abstract void drawTo(BufferedImage image, ReadOnlyTransform localTransform, int clipmapLevel);

    public abstract void updateBoundsFromElement();

    public void updateBounds() {
        final Vector4 oldBounds = new Vector4(_awtBounds);
        // update using size of element
        updateBoundsFromElement();

        // So apply transform
        final double x = _awtBounds.getX(), y = _awtBounds.getY(), width = _awtBounds.getZ(), height = _awtBounds
                .getW();
        final Vector3[] vects = new Vector3[] { //
        //
                new Vector3(x, y, 0), //
                new Vector3(x + width, y, 0), //
                new Vector3(x + width, y + height, 0), //
                new Vector3(x, y + height, 0) //
        };

        // update final bounds info.
        double minX, minY, maxX, maxY;
        minX = minY = Double.POSITIVE_INFINITY;
        maxX = maxY = Double.NEGATIVE_INFINITY;

        for (final Vector3 vect : vects) {
            _transform.applyForward(vect);
            if (vect.getX() < minX) {
                minX = vect.getX();
            }
            if (vect.getX() > maxX) {
                maxX = vect.getX();
            }
            if (vect.getY() < minY) {
                minY = vect.getY();
            }
            if (vect.getY() > maxY) {
                maxY = vect.getY();
            }
        }

        _awtBounds.set(minX, minY, maxX - minX, maxY - minY);

        if (_listener != null) {
            _listener.elementUpdated(oldBounds, _awtBounds);
        }
    }

    public ReadOnlyVector4 getBounds() {
        return _awtBounds;
    }

    public void setUpdateListener(final ElementUpdateListener listener) {
        _listener = listener;
    }

    public static AlphaComposite makeAlphaComposite(final float alpha) {
        final int type = AlphaComposite.SRC_OVER;
        return AlphaComposite.getInstance(type, alpha);
    }

    public Map<RenderingHints.Key, Object> getHints() {
        return hints;
    }
}
