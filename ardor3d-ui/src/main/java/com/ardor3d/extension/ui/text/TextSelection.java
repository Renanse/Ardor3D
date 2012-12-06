/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.ui.text;

import java.nio.FloatBuffer;
import java.util.List;

import com.ardor3d.extension.ui.UIComponent;
import com.ardor3d.extension.ui.text.RenderedText.RenderedTextData;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.type.ReadOnlyTransform;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.BlendState.DestinationFunction;
import com.ardor3d.renderer.state.BlendState.SourceFunction;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.util.geom.BufferUtils;
import com.google.common.collect.Lists;

public abstract class TextSelection {

    protected int _startIndex = -1;
    protected int _endIndex = -1;
    protected SelectionState _state = SelectionState.NO_SELECTION;

    /** The mesh used to render this selection. */
    protected Mesh _standin;

    public TextSelection() {
        _standin = createSelectionMesh();
    }

    /**
     * @return the current caret position within the component whose selection we are tracking.
     */
    public abstract int getCaretPosition();

    /**
     * @return the data object of the text associated with this selection.
     */
    public abstract RenderedTextData getTextData();

    public int getSelectionLength() {
        if (_startIndex == -1 || _endIndex == -1) {
            return 0;
        } else {
            return _endIndex - _startIndex;
        }
    }

    public void reset() {
        _startIndex = _endIndex = -1;
        _state = SelectionState.NO_SELECTION;
    }

    public int getEndIndex() {
        return _endIndex;
    }

    public void setEndIndex(final int endIndex) {
        _endIndex = endIndex;
        updateMesh();
    }

    public int getStartIndex() {
        return _startIndex;
    }

    public void setStartIndex(final int startIndex) {
        _startIndex = startIndex;
        updateMesh();
    }

    public SelectionState getState() {
        return _state;
    }

    public void setState(final SelectionState state) {
        _state = state;
    }

    /**
     * Alter the selection as if the "shift" and "up" keys are pressed.
     */
    public void upKey() {
        if (_state != SelectionState.NO_SELECTION && getSelectionLength() == 0) {
            _state = SelectionState.AT_START_OF_SELECTION;
        }

        if (_state == SelectionState.AT_END_OF_SELECTION) {
            if (getCaretPosition() < _startIndex) {
                _endIndex = _startIndex;
                _startIndex = getCaretPosition();
                _state = SelectionState.AT_START_OF_SELECTION;
            } else {
                _endIndex = getCaretPosition();
            }
        } else if (_state == SelectionState.AT_START_OF_SELECTION) {
            _startIndex = getCaretPosition();
        }
        updateMesh();
    }

    /**
     * Alter the selection as if the "shift" and "down" keys are pressed.
     */
    public void downKey() {
        if (_state != SelectionState.NO_SELECTION && getSelectionLength() == 0) {
            _state = SelectionState.AT_END_OF_SELECTION;
        }

        if (_state == SelectionState.AT_END_OF_SELECTION) {
            _endIndex = getCaretPosition();
        } else if (_state == SelectionState.AT_START_OF_SELECTION) {
            if (getCaretPosition() > _endIndex) {
                _startIndex = _endIndex;
                _endIndex = getCaretPosition();
                _state = SelectionState.AT_END_OF_SELECTION;
            } else {
                _startIndex = getCaretPosition();
            }
        }
        updateMesh();
    }

    /**
     * Alter the selection as if the "shift" and "left" keys are pressed.
     */
    public void leftKey() {
        if (_state != SelectionState.NO_SELECTION && getSelectionLength() == 0) {
            _state = SelectionState.AT_START_OF_SELECTION;
        }

        if (_state == SelectionState.AT_END_OF_SELECTION) {
            _endIndex = getCaretPosition();
        } else if (_state == SelectionState.AT_START_OF_SELECTION) {
            _startIndex = getCaretPosition();
        }
        updateMesh();
    }

    /**
     * Alter the selection as if the "shift" and "right" keys are pressed.
     */
    public void rightKey() {
        if (_state != SelectionState.NO_SELECTION && getSelectionLength() == 0) {
            _state = SelectionState.AT_END_OF_SELECTION;
        }

        if (_state == SelectionState.AT_END_OF_SELECTION) {
            _endIndex = getCaretPosition();
        } else if (_state == SelectionState.AT_START_OF_SELECTION) {
            _startIndex = getCaretPosition();
        }
        updateMesh();
    }

    public void checkStart() {
        if (_state == SelectionState.NO_SELECTION) {
            final int caretPosition = getCaretPosition();
            _startIndex = caretPosition;
            _endIndex = caretPosition;
            _state = SelectionState.AT_END_OF_SELECTION;
        }
    }

    private void updateMesh() {
        final int selLength = getSelectionLength();
        if (selLength == 0) {
            return;
        }

        // Make triangle strips for each line.
        final RenderedTextData data = getTextData();
        float xStart = 0, xEnd = 0, height = 0, yOffset = 0;
        boolean exit = false;
        final List<Float> verts = Lists.newArrayList();
        for (int j = 0; !exit && j < data._lineEnds.size(); j++) {
            height = data._lineHeights.get(j);
            final int end = data._lineEnds.get(j);
            if (_startIndex > end) {
                continue;
            } else if (_startIndex <= end) {
                xStart = data._xStarts.get(_startIndex);
            } else {
                xStart = 0;
            }

            if (_endIndex <= end) {
                // last strip
                final CharacterDescriptor charDesc = data._characters.get(_endIndex - 1);
                xEnd = data._xStarts.get(_endIndex - 1)
                        + (int) Math.round(charDesc.getScale() * charDesc.getXAdvance());
                exit = true;
            } else {
                final CharacterDescriptor charDesc = data._characters.get(end);
                xEnd = data._xStarts.get(end) + (int) Math.round(charDesc.getScale() * charDesc.getXAdvance());
            }

            verts.add(xStart);
            verts.add(yOffset);
            verts.add(0f);

            verts.add(xEnd);
            verts.add(yOffset);
            verts.add(0f);

            verts.add(xStart);
            verts.add(yOffset + height);
            verts.add(0f);

            verts.add(xStart);
            verts.add(yOffset + height);
            verts.add(0f);

            verts.add(xEnd);
            verts.add(yOffset);
            verts.add(0f);

            verts.add(xEnd);
            verts.add(yOffset + height);
            verts.add(0f);

            yOffset += height;
        }
        final MeshData mData = _standin.getMeshData();
        mData.setVertexBuffer(BufferUtils.createVector3Buffer(mData.getVertexBuffer(), verts.size()));
        final FloatBuffer vertBuffer = mData.getVertexBuffer();
        for (final float f : verts) {
            vertBuffer.put(f);
        }
    }

    public void draw(final Renderer renderer, final ReadOnlyTransform xform) {
        if (getSelectionLength() == 0) {
            return;
        }

        // set our alpha
        final ColorRGBA color = ColorRGBA.fetchTempInstance();
        color.set(_standin.getDefaultColor()).setAlpha(UIComponent.getCurrentOpacity());
        _standin.setDefaultColor(color);
        ColorRGBA.releaseTempInstance(color);

        // set our location
        _standin.setWorldTransform(xform);

        // draw
        _standin.render(renderer);
    }

    protected Mesh createSelectionMesh() {
        final Mesh mesh = new Mesh("selectionMesh");
        final MeshData mData = mesh.getMeshData();
        mData.setVertexBuffer(BufferUtils.createVector3Buffer(6));
        mData.setIndexMode(IndexMode.Triangles);

        final BlendState blend = new BlendState();
        blend.setBlendEnabled(true);
        blend.setSourceFunction(SourceFunction.SourceAlpha);
        blend.setDestinationFunction(DestinationFunction.OneMinusSourceAlpha);
        mesh.setRenderState(blend);
        mesh.updateWorldRenderStates(false);
        mesh.setDefaultColor(ColorRGBA.LIGHT_GRAY);

        return mesh;
    }

    public enum SelectionState {
        /**
         * No selection is currently made.
         */
        NO_SELECTION,

        /**
         * We have a selection and our caret is currently at the end of that selection.
         */
        AT_END_OF_SELECTION,

        /**
         * We have a selection and our caret is currently at the start of that selection.
         */
        AT_START_OF_SELECTION;
    }
}
