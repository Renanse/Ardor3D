/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.ui.text;

import com.ardor3d.extension.ui.UIComponent;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Transform;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.util.geom.BufferUtils;

public class TextCaret {

    private long _blinkInterval = 500; // ms
    private long _lastBlink = 0;
    private boolean _show = true;
    private int _posX, _posY;
    private int _index;

    private final Mesh _strip = new Mesh();

    /**
     * Construct a new TextCaret
     */
    public TextCaret() {
        final MeshData md = _strip.getMeshData();
        md.setVertexBuffer(BufferUtils.createVector3Buffer(4));
        md.setIndices(BufferUtils.createIndexBufferData(new int[] { 0, 1, 3, 2 }, 3));
        md.setIndexMode(IndexMode.TriangleStrip);
        final float[] vals = new float[] { 0, 0, 0, //
                1, 0, 0, //
                1, 1, 0, //
                0, 1, 0 //
        };
        md.getVertexBuffer().put(vals);
        _strip.updateGeometricState(0);

        setCaretColor(ColorRGBA.BLACK);
    }

    public boolean isShowing() {
        final long curr = System.currentTimeMillis();
        if (curr - _lastBlink > _blinkInterval) {
            _lastBlink = curr;
            _show = !_show;
        }

        return _show;
    }

    public void draw(final Renderer renderer, final UIComponent comp, final int height, final double xOffset,
            final double yOffset) {
        final long curr = System.currentTimeMillis();
        if (curr - _lastBlink > _blinkInterval) {
            _lastBlink = curr;
            _show = !_show;
            comp.fireComponentDirty();
        }

        if (!_show) {
            return;
        }

        final Vector3 v = Vector3.fetchTempInstance();
        v.set(getPosX() + xOffset, getPosY() + yOffset, 0);

        final Transform t = Transform.fetchTempInstance();
        t.set(comp.getWorldTransform());
        t.applyForwardVector(v);
        t.translate(v);
        Vector3.releaseTempInstance(v);

        _strip.setWorldTransform(t);
        Transform.releaseTempInstance(t);

        _strip.setWorldScale(1, _strip.getWorldScale().getY() * height, 0);
        _strip.render(renderer);
    }

    public long getBlinkInterval() {
        return _blinkInterval;
    }

    public void setBlinkInterval(final long ms) {
        _blinkInterval = ms;
    }

    public ReadOnlyColorRGBA getCaretColor() {
        return _strip.getDefaultColor();
    }

    public void setCaretColor(final ReadOnlyColorRGBA caretColor) {
        _strip.setDefaultColor(caretColor);
    }

    public int getIndex() {
        return _index;
    }

    public void setIndex(final int index) {
        _index = index;
    }

    public int getPosX() {
        return _posX;
    }

    public void setPosX(final int posX) {
        _posX = posX;
    }

    public int getPosY() {
        return _posY;
    }

    public void setPosY(final int posY) {
        _posY = posY;
    }
}
