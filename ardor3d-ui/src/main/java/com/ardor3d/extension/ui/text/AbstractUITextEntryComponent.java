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

import java.util.Map;

import com.ardor3d.extension.ui.StateBasedUIComponent;
import com.ardor3d.extension.ui.Textable;
import com.ardor3d.extension.ui.UIComponent;
import com.ardor3d.extension.ui.UIState;
import com.ardor3d.extension.ui.util.Alignment;
import com.ardor3d.input.InputState;
import com.ardor3d.input.MouseButton;
import com.google.common.collect.ImmutableSet;

public abstract class AbstractUITextEntryComponent extends StateBasedUIComponent implements Textable {

    protected int _caretPosition = 0;

    protected boolean _editable = false;
    protected boolean _copyable = true;

    protected UIState _disabledState;
    protected UIState _defaultState;
    protected UIState _writingState;

    /** The text object to use for rendering. */
    protected RenderedText _uiText;

    /** If true, our text could be marked up with style information. */
    protected boolean _styled = false;

    /** Alignment value to use to position the icon/text within the overall dimensions of this component. */
    protected Alignment _alignment = Alignment.BOTTOM_LEFT;

    protected TextCaret _caret = new TextCaret();

    protected TextSelection _selection = new TextSelection() {
        @Override
        public int getCaretPosition() {
            return AbstractUITextEntryComponent.this.getCaretPosition();
        }

        @Override
        public void setCaretPosition(final int position) {
            AbstractUITextEntryComponent.this.setCaretPosition(position);
        }

        @Override
        public RenderedText getRenderedText() {
            return _uiText;
        }
    };

    protected abstract UIKeyHandler getKeyHandler();

    @Override
    public UIState getDefaultState() {
        return _defaultState;
    }

    @Override
    public UIState getDisabledState() {
        return _disabledState;
    }

    public UIState getWritingState() {
        return _writingState;
    }

    public String getRawText() {
        return _uiText != null ? _uiText.getRawText() : null;
    }

    public String getText() {
        return _uiText != null && _uiText.getVisibleText() != null ? _uiText.getVisibleText() : "";
    }

    /**
     * Set the text for this component. Also updates the minimum size of the component.
     *
     * @param rawText
     *            the new text
     */
    public void setText(String rawText) {
        rawText = validateInputText(rawText, _uiText != null ? _uiText.getRawText() : null);

        if (rawText != null) {
            int maxWidth = getMaximumLocalComponentWidth() - getTotalLeft() - getTotalRight();
            if (maxWidth <= 0) {
                maxWidth = -1;
            }

            _uiText = TextFactory.INSTANCE.generateText(formatRawText(rawText), isStyledText(), getFontStyles(),
                    _uiText, maxWidth);
            _uiText.setRawText(rawText);
        } else {
            _uiText = null;

            // reset caret position
            setCaretPosition(0);
        }

        updateMinimumSizeFromContents();
    }

    protected String validateInputText(String inputText, final String oldText) {
        if (inputText != null && inputText.length() == 0) {
            inputText = null;
        }
        return inputText;
    }

    /**
     * Applies default formatting to raw text for this component prior to passing it to rendered text generation.
     *
     * @param rawText
     * @return by default, returns the rawText as is.
     */
    protected String formatRawText(final String rawText) {
        return rawText;
    }

    public boolean isStyledText() {
        return _styled;
    }

    public void setStyledText(final boolean value) {
        _styled = value;
    }

    public boolean isCopyable() {
        return _copyable;
    }

    public void setCopyable(final boolean copyable) {
        _copyable = copyable;
    }

    /**
     * Set the position of the text caret as an index to the current set text string. If the specified position is after
     * the last possible index, it is set to the last possible index.
     *
     * @param index
     *            the new position
     */
    public int setCaretPosition(int index) {
        final String text = getText();
        if (text == null) {
            index = 0;
        } else if (index > text.length()) {
            index = text.length();
        }

        if (index < 0) {
            index = 0;
        }
        _caretPosition = index;
        return _caretPosition;
    }

    public int getCaretPosition() {
        return _caretPosition;
    }

    public int getSelectionLength() {
        return _selection.getSelectionLength();
    }

    public void clearSelection() {
        _selection.reset();
    }

    public boolean isEditable() {
        return _editable;
    }

    /**
     * @param editable
     *            true if the text of this component can be changed by keyboard interaction
     */
    public void setEditable(final boolean editable) {
        _editable = editable;
    }

    public TextCaret getCaret() {
        return _caret;
    }

    public TextSelection getSelection() {
        return _selection;
    }

    public Alignment getAlignment() {
        return _alignment;
    }

    public void setAlignment(final Alignment alignment) {
        _alignment = alignment;
    }

    @Override
    public void fireStyleChanged() {
        super.fireStyleChanged();
        setText(getRawText());
    }

    @Override
    public void updateMinimumSizeFromContents() {
        int width = 0;
        int height = 0;

        if (_uiText != null) {
            width += Math.round(_uiText.getWidth());
            height += Math.round(_uiText.getHeight());
        }

        if (height == 0) {
            final Map<String, Object> styles = getFontStyles();
            if (styles.containsKey(StyleConstants.KEY_SIZE)) {
                height = (Integer) styles.get(StyleConstants.KEY_SIZE);
            } else {
                height = UIComponent.getDefaultFontSize();
            }
        }

        setLayoutMinimumContentSize(width, height);
        fireComponentDirty();
    }

    @Override
    public ImmutableSet<UIState> getStates() {
        return ImmutableSet.of(_defaultState, _disabledState, _writingState);
    }

    protected class DefaultTextEntryState extends UIState {
        @Override
        public boolean mousePressed(final MouseButton button, final InputState state) {
            switchState(_writingState);

            final int x = state.getMouseState().getX() - AbstractUITextEntryComponent.this.getHudX()
                    - AbstractUITextEntryComponent.this.getPadding().getLeft();
            final int y = state.getMouseState().getY() - AbstractUITextEntryComponent.this.getHudY()
                    - AbstractUITextEntryComponent.this.getPadding().getBottom();

            setCaretPosition(_uiText != null ? _uiText.findCaretPosition(x, y) : 0);

            return true;
        }

        @Override
        public void mouseEntered(final int mouseX, final int mouseY, final InputState state) {
            // CursorUtil.getCursorFactory().getTextCursor().setActive();
        }

        @Override
        public void mouseDeparted(final int mouseX, final int mouseY, final InputState state) {
            // CursorUtil.getCursorFactory().getDefaultCursor().setActive();
        }
    }
}
