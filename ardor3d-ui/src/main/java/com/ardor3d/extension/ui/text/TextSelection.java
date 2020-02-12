/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.ui.text;

import java.nio.FloatBuffer;
import java.util.ArrayList;
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

    public abstract void setCaretPosition(int position);

    /**
     * @return the data object of the text associated with this selection.
     */
    public abstract RenderedText getRenderedText();

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

    private enum SelectType {
        WhiteSpace, AlphaNumeric, Other
    }

    private enum SelectDirection {
        Left, Right, Expand
    }

    private SelectType determineType(final char c) {
        if (Character.isLetter(c) || Character.isDigit(c)) {
            return SelectType.AlphaNumeric;
        } else if (Character.isWhitespace(c)) {
            return SelectType.WhiteSpace;
        }

        return SelectType.Other;
    }

    public void selectGroupAtPosition(int position) {
        final RenderedText rText = getRenderedText();
        if (rText == null) {
            reset();
            return;
        }

        final String visibleText = rText.getVisibleText();
        if (visibleText.length() == 0) {
            reset();
            return;
        }

        if (position < 0) {
            position = 0;
        } else if (position > visibleText.length()) {
            position = visibleText.length();
        }

        final SelectType type;
        final SelectDirection dir;
        // test what state we are in
        if (position == 0) {
            dir = SelectDirection.Right;
            type = determineType(visibleText.charAt(0));
        } else if (position == visibleText.length()) {
            dir = SelectDirection.Left;
            type = determineType(visibleText.charAt(visibleText.length() - 1));
        } else {
            // are we next to a boundary?
            final SelectType a = determineType(visibleText.charAt(position - 1));
            final SelectType b = determineType(visibleText.charAt(position));

            // easy case - we're in the middle of the same sort of text
            if (a == b) {
                dir = SelectDirection.Expand;
                type = a;
            } else {
                // white space takes last priority
                if (a == SelectType.WhiteSpace) {
                    type = b;
                    dir = SelectDirection.Right;
                } else if (b == SelectType.WhiteSpace) {
                    type = a;
                    dir = SelectDirection.Left;
                }

                // check for Other, lower priority than AlphaNumeric
                else if (a == SelectType.Other) {
                    type = b;
                    dir = SelectDirection.Right;
                } else {
                    // last possible case is Other on Right, since we already check for equality and only 3 types.
                    type = a;
                    dir = SelectDirection.Left;
                }
            }
        }

        selectGroupAtPosition(visibleText, position, dir, type);
    }

    private void selectGroupAtPosition(final String visibleText, final int position, final SelectDirection dir,
            final SelectType type) {
        int left = position;
        int right = position;

        if (dir != SelectDirection.Right) {
            while (left >= 1) {
                final SelectType next = determineType(visibleText.charAt(left - 1));
                if (next != type) {
                    break;
                }
                left--;
            }
        }

        if (dir != SelectDirection.Left) {
            while (right < visibleText.length()) {
                final SelectType next = determineType(visibleText.charAt(right));
                if (next != type) {
                    break;
                }
                right++;
            }
        }

        _startIndex = left;
        _endIndex = right;
        _state = SelectionState.AT_END_OF_SELECTION;
        setCaretPosition(_endIndex);
        updateMesh();
    }

    public void selectLineAtPosition(final int position) {
        final RenderedText rText = getRenderedText();
        if (rText == null) {
            return;
        }

        final List<Integer> ends = rText.getData()._lineEnds;
        if (ends.isEmpty()) {
            reset();
            return;
        }

        final int line = rText.getLineFromCaretPosition(position);
        final int max = ends.size();
        if (line == 0) {
            _startIndex = 0;
            _endIndex = ends.get(0) + 1;
        } else if (line > max - 1) {
            _startIndex = max > 1 ? ends.get(max - 2) + 1 : 0;
            _endIndex = ends.get(max - 1) + 1;
        } else {
            _startIndex = ends.get(line - 1) + 1;
            _endIndex = ends.get(line) + 1;
        }
        _state = SelectionState.AT_END_OF_SELECTION;
        setCaretPosition(_endIndex);
        updateMesh();
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
        // check for a selection
        final int selLength = getSelectionLength();
        if (selLength == 0) {
            return;
        }

        // check we have text
        final RenderedText rText = getRenderedText();
        if (rText == null) {
            return;
        }

        // Make triangle strips for each line.
        final RenderedTextData data = rText.getData();
        float xStart = 0, xEnd = 0, height = 0, yOffset = data.getTotalHeight();
        int end, prevEnd = 0;
        boolean exit = false;
        final List<Float> verts = new ArrayList<>();
        for (int j = 0; !exit && j < data._lineEnds.size(); j++) {
            height = data._lineHeights.get(j);
            yOffset -= height;

            end = data._lineEnds.get(j);
            if (_startIndex > end) {
                continue;
            } else if (_startIndex > prevEnd) {
                xStart = data._xStarts.get(_startIndex);
            } else {
                xStart = 0;
            }
            prevEnd = end;

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
        }
        final MeshData meshData = _standin.getMeshData();
        meshData.setVertexBuffer(BufferUtils.createVector3Buffer(meshData.getVertexBuffer(), verts.size()));
        final FloatBuffer vertBuffer = meshData.getVertexBuffer();
        for (final float f : verts) {
            vertBuffer.put(f);
        }
        meshData.markBufferDirty(MeshData.KEY_VertexCoords);
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
        mesh.setRenderMaterial("ui/untextured/default_color.yaml");

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
