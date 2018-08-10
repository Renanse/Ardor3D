/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.state.record;

import java.util.Stack;

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.type.ReadOnlyRectangle2;
import com.ardor3d.renderer.DrawBufferTarget;

public class RendererRecord extends StateRecord {
    private int _matrixMode = -1;
    private int _currentVaoId = -1;
    private boolean _matrixValid;
    private boolean _vaoValid;
    private boolean _clippingTestValid;
    private boolean _clippingTestEnabled;
    private transient final ColorRGBA _tempColor = new ColorRGBA();
    private DrawBufferTarget _drawBufferTarget = null;
    private final Stack<ReadOnlyRectangle2> _clips = new Stack<ReadOnlyRectangle2>();
    private int _enabledTextures = 0;
    private boolean _texturesValid = false;
    private int _currentTextureArraysUnit = 0;

    @Override
    public void invalidate() {
        invalidateMatrix();
        invalidateVAO();
        _drawBufferTarget = null;
        _clippingTestValid = false;
        _texturesValid = false;
        _currentTextureArraysUnit = -1;
    }

    @Override
    public void validate() {
        // ignore - validate per item or locally
    }

    public void invalidateMatrix() {
        _matrixValid = false;
        _matrixMode = -1;
    }

    public void invalidateVAO() {
        _vaoValid = false;
        _currentVaoId = -1;
    }

    public int getMatrixMode() {
        return _matrixMode;
    }

    public void setMatrixMode(final int matrixMode) {
        _matrixMode = matrixMode;
    }

    public int getCurrentVaoId() {
        return _currentVaoId;
    }

    public void setCurrentVaoId(final int id) {
        _currentVaoId = id;
    }

    public boolean isMatrixValid() {
        return _matrixValid;
    }

    public void setMatrixValid(final boolean valid) {
        _matrixValid = valid;
    }

    public boolean isVaoValid() {
        return _vaoValid;
    }

    public void setVaoValid(final boolean valid) {
        _vaoValid = valid;
    }

    public ColorRGBA getTempColor() {
        return _tempColor;
    }

    public DrawBufferTarget getDrawBufferTarget() {
        return _drawBufferTarget;
    }

    public void setDrawBufferTarget(final DrawBufferTarget target) {
        _drawBufferTarget = target;
    }

    public Stack<ReadOnlyRectangle2> getScissorClips() {
        return _clips;
    }

    public boolean isClippingTestEnabled() {
        return _clippingTestEnabled;
    }

    public void setClippingTestEnabled(final boolean enabled) {
        _clippingTestEnabled = enabled;
    }

    public boolean isClippingTestValid() {
        return _clippingTestValid;
    }

    public void setClippingTestValid(final boolean valid) {
        _clippingTestValid = valid;
    }

    public int getEnabledTextures() {
        return _enabledTextures;
    }

    public void setEnabledTextures(final int enabledTextures) {
        _enabledTextures = enabledTextures;
    }

    public boolean isTexturesValid() {
        return _texturesValid;
    }

    public void setTexturesValid(final boolean valid) {
        _texturesValid = valid;
    }

    public int getCurrentTextureArraysUnit() {
        return _currentTextureArraysUnit;
    }

    public void setCurrentTextureArraysUnit(final int currentTextureArraysUnit) {
        _currentTextureArraysUnit = currentTextureArraysUnit;
    }
}
