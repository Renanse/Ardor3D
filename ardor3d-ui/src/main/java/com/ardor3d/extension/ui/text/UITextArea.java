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

import com.ardor3d.extension.ui.UIComponent;
import com.ardor3d.extension.ui.UIState;
import com.ardor3d.extension.ui.text.TextSelection.SelectionState;
import com.ardor3d.extension.ui.util.Alignment;
import com.ardor3d.input.InputState;
import com.ardor3d.input.Key;
import com.ardor3d.input.MouseButton;
import com.ardor3d.math.Rectangle2;
import com.ardor3d.math.Transform;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Renderer;

public class UITextArea extends AbstractUITextEntryComponent {

    /** A store for the clip rectangle. */
    private final Rectangle2 _clipRectangleStore = new Rectangle2();

    protected UIKeyHandler _keyHandler;

    public UITextArea() {
        setAlignment(Alignment.TOP_LEFT);
        _disabledState = new UIState();
        _defaultState = new DefaultTextEntryState();
        _writingState = new TextAreaWritingState();
        setEditable(true);
        applySkin();
        switchState(getDefaultState());
    }

    /**
     * Delete any currently selected text.
     */
    public void deleteSelectedText() {
        if (_selection.getSelectionLength() != 0) {
            final String text = getText();
            final int sIndex = _selection.getStartIndex();
            final int eIndex = _selection.getEndIndex();
            if (sIndex >= 0 && sIndex <= text.length() && eIndex >= 0 && eIndex <= text.length()) {
                setText(text.substring(0, sIndex) + text.substring(eIndex));
            }
        }
    }

    @Override
    protected UIKeyHandler getKeyHandler() {
        if (_keyHandler == null) {
            _keyHandler = new DefaultLatinTextAreaKeyHandler(this);
        }
        return _keyHandler;
    }

    public void setKeyHandler(final UIKeyHandler handler) {
        _keyHandler = handler;
    }

    @Override
    protected void drawComponent(final Renderer r) {
        // figure out our offsets using alignment and edge info
        final double x = _alignment.alignX(getContentWidth(), _uiText != null ? _uiText.getWidth() : 1)
                + getTotalLeft();
        final double y = _alignment.alignY(getContentHeight(), _uiText != null ? _uiText.getHeight() : 1)
                + getTotalBottom();

        // Draw our text, if we have any
        if (_uiText != null) {
            final String text = getText();
            if (text.length() > 0) {
                // set our text location
                final Vector3 v = Vector3.fetchTempInstance();
                // note: we round to get the text pixel aligned... otherwise it can get blurry
                v.set(Math.round(x), Math.round(y), 0);
                final Transform t = Transform.fetchTempInstance();
                t.set(getWorldTransform());
                t.applyForwardVector(v);
                t.translate(v);
                Vector3.releaseTempInstance(v);
                _uiText.setWorldTransform(t);
                Transform.releaseTempInstance(t);

                // draw the selection first
                if (getSelection().getSelectionLength() > 0) {
                    getSelection().draw(r, t);
                }

                // draw text using current foreground color and alpha.
                // TODO: alpha of text...
                final boolean needsPop = getWorldRotation().isIdentity();
                if (needsPop) {
                    _clipRectangleStore.set(getHudX() + getTotalLeft(), getHudY() + getTotalBottom(),
                            getContentWidth(), getContentHeight());
                    r.pushClip(_clipRectangleStore);
                }
                _uiText.render(r);
                if (needsPop) {
                    r.popClip();
                }
            }
        }

        // Draw our caret, if we have one.
        if (isCopyable() && isEditable() && getCurrentState().equals(_writingState) && getCaret().isShowing()) {
            getCaret().draw(r, this,
                    _uiText != null ? _uiText.getLineHeight(getCaretPosition()) : UIComponent.getDefaultFontSize(), x,
                    y);
        }
    }

    public class TextAreaWritingState extends UIState {
        @Override
        public boolean keyReleased(final Key key, final InputState state) {
            return getKeyHandler().keyReleased(key, state);
        }

        @Override
        public boolean keyPressed(final Key key, final InputState state) {
            return getKeyHandler().keyPressed(key, state);
        }

        @Override
        public boolean keyHeld(final Key key, final InputState state) {
            return getKeyHandler().keyHeld(key, state);
        }

        @Override
        public void mouseEntered(final int mouseX, final int mouseY, final InputState state) {
        // TODO: set cursor to text entry
        }

        @Override
        public void mouseDeparted(final int mouseX, final int mouseY, final InputState state) {
        // TODO: set cursor to default
        }

        @Override
        public boolean mousePressed(final MouseButton button, final InputState state) {
            final int x = state.getMouseState().getX() - UITextArea.this.getHudX()
                    - UITextArea.this.getPadding().getLeft();
            final int y = state.getMouseState().getY() - UITextArea.this.getHudY()
                    - UITextArea.this.getPadding().getBottom();

            if (_uiText != null) {
                final int position = _uiText.findCaretPosition(x, y);
                if (isCopyable() && state.getKeyboardState().isAtLeastOneDown(Key.LSHIFT, Key.RSHIFT)) {
                    _selection.checkStart();
                    if (_selection.getState() == SelectionState.AT_START_OF_SELECTION) {
                        if (position <= _selection.getEndIndex()) {
                            _selection.setStartIndex(position);
                        } else {
                            final int oldEnd = _selection.getEndIndex();
                            _selection.setEndIndex(position);
                            _selection.setStartIndex(oldEnd);
                            _selection.setState(SelectionState.AT_END_OF_SELECTION);
                        }
                    } else {
                        if (position >= _selection.getStartIndex()) {
                            _selection.setEndIndex(position);
                        } else {
                            final int oldStart = _selection.getStartIndex();
                            _selection.setStartIndex(position);
                            _selection.setEndIndex(oldStart);
                            _selection.setState(SelectionState.AT_START_OF_SELECTION);
                        }
                    }
                } else {
                    // clear any getSelection()
                    clearSelection();
                }
                setCaretPosition(position);
            } else {
                setCaretPosition(0);
            }

            return true;
        }

        @Override
        public void lostFocus() {
            switchState(_defaultState);
            getSelection().setStartIndex(-1);
        }
    }
}
