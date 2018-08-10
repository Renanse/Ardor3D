/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scenegraph.extension;

import java.util.BitSet;

import com.ardor3d.renderer.Renderer;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;

public class SwitchNode extends Node {

    protected BitSet _childMask = new BitSet();

    public SwitchNode() {
        this("SwitchNode");
    }

    public SwitchNode(final String name) {
        super(name);

        _childMask.set(0);
    }

    @Override
    public void draw(final Renderer r) {
        if (_children == null) {
            return;
        }
        for (int i = 0, max = Math.min(_childMask.length(), _children.size()); i < max; i++) {
            if (_childMask.get(i)) {
                final Spatial child = _children.get(i);
                if (child != null) {
                    child.onDraw(r);
                }
            }
        }
    }

    @Override
    protected void updateChildren(final double time) {
        if (_children == null) {
            return;
        }
        for (int i = 0, max = Math.min(_childMask.length(), _children.size()); i < max; i++) {
            if (_childMask.get(i)) {
                final Spatial child = _children.get(i);
                if (child != null) {
                    child.updateGeometricState(time, false);
                }
            }
        }
    }

    public void setAllNonVisible() {
        _childMask.clear();
    }

    public void setAllVisible() {
        _childMask.set(0, getNumberOfChildren());
    }

    public void flipAllVisible() {
        _childMask.flip(0, getNumberOfChildren());
    }

    public boolean getVisible(final int bitIndex) {
        return _childMask.get(bitIndex);
    }

    public BitSet getVisible() {
        return _childMask;
    }

    public void setVisible(final BitSet set) {
        _childMask = set;
    }

    public void setVisible(final int bitIndex, final boolean value) {
        _childMask.set(bitIndex, value);
    }

    public void setVisible(final int fromIndex, final int toIndex, final boolean value) {
        _childMask.set(fromIndex, toIndex, value);
    }

    public void setSingleVisible(final int bitIndex) {
        _childMask.clear();
        _childMask.set(bitIndex);
    }

    public int getNextNonVisible(final int fromIndex) {
        return _childMask.nextClearBit(fromIndex);
    }

    public int getNextVisible(final int fromIndex) {
        return _childMask.nextSetBit(fromIndex);
    }

    public void shiftVisibleRight() {
        final int nrChildren = getNumberOfChildren();
        if (nrChildren == 0) {
            return;
        }

        final boolean lastVal = _childMask.get(nrChildren - 1);
        for (int i = nrChildren - 1; i > 0; i--) {
            _childMask.set(i, _childMask.get(i - 1));
        }
        _childMask.set(0, lastVal);
    }

    public void shiftVisibleLeft() {
        final int nrChildren = getNumberOfChildren();
        if (nrChildren == 0) {
            return;
        }

        final boolean firstVal = _childMask.get(0);
        for (int i = 0; i < nrChildren - 1; i++) {
            _childMask.set(i, _childMask.get(i + 1));
        }
        _childMask.set(getNumberOfChildren() - 1, firstVal);
    }

    public void flipVisible(final int fromIndex, final int toIndex) {
        _childMask.flip(fromIndex, toIndex);
    }

    public void flipVisible(final int bitIndex) {
        _childMask.flip(bitIndex);
    }

}
