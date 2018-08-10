/**
 * Copyright (c) 2008-2018 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.ui.text;

import com.ardor3d.extension.ui.UIState;
import com.ardor3d.extension.ui.text.TextSelection.SelectionState;
import com.ardor3d.input.ButtonState;
import com.ardor3d.input.InputState;
import com.ardor3d.input.Key;
import com.ardor3d.input.MouseButton;
import com.ardor3d.input.MouseState;

public class TextEntryWritingState extends UIState {

    protected final AbstractUITextEntryComponent _component;
    protected long _lastClick;
    protected int _clickCount;

    public static long MultiClickThresholdMS = 500;

    public TextEntryWritingState(final AbstractUITextEntryComponent component) {
        _component = component;
    }

    @Override
    public boolean keyReleased(final Key key, final InputState state) {
        return _component.getKeyHandler().keyReleased(key, state);
    }

    @Override
    public boolean keyPressed(final Key key, final InputState state) {
        return _component.getKeyHandler().keyPressed(key, state);
    }

    @Override
    public boolean keyHeld(final Key key, final InputState state) {
        return _component.getKeyHandler().keyHeld(key, state);
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
    public boolean mouseClicked(final MouseButton button, final InputState state) {
        if (_component._uiText == null || !_component.isCopyable()) {
            return super.mouseClicked(button, state);
        }

        // track our click count
        final long now = System.currentTimeMillis();
        final long elapsed = now - _lastClick;
        _lastClick = now;

        if (elapsed <= TextEntryWritingState.MultiClickThresholdMS) {
            _clickCount++;
        } else {
            // first click, ignore
            _clickCount = 1;
            return super.mouseClicked(button, state);
        }

        // figure out where we are clicking in our text
        final MouseState mouseState = state.getMouseState();
        final int x = mouseState.getX() - _component.getHudX() - _component.getPadding().getLeft();
        final int y = mouseState.getY() - _component.getHudY() - _component.getPadding().getBottom();
        final int position = _component._uiText.findCaretPosition(x, y);

        // modulo used to repeat pattern of 2 and 3 clicks
        if (_clickCount % 2 == 0) {
            // double click - select single group
            _component.getSelection().selectGroupAtPosition(position);
        } else {
            // triple click - select line
            _component.getSelection().selectLineAtPosition(position);
        }

        return true;
    }

    @Override
    public boolean mouseMoved(final int mouseX, final int mouseY, final InputState state) {
        // test if we are holding our button
        if (state.getMouseState().getButtonState(MouseButton.LEFT) != ButtonState.DOWN) {
            return super.mouseMoved(mouseX, mouseY, state);
        }

        updateSelection(state.getMouseState(), true);

        return true;
    }

    @Override
    public boolean mousePressed(final MouseButton button, final InputState state) {
        if (button != MouseButton.LEFT) {
            return super.mousePressed(button, state);
        }

        final boolean copyToggled = state.getKeyboardState().isAtLeastOneDown(Key.LEFT_SHIFT, Key.RIGHT_SHIFT);
        updateSelection(state.getMouseState(), copyToggled);

        return true;
    }

    private void updateSelection(final MouseState mouseState, final boolean copyToggled) {
        final int x = mouseState.getX() - _component.getHudX() - _component.getPadding().getLeft();
        final int y = mouseState.getY() - _component.getHudY() - _component.getPadding().getBottom();

        if (_component._uiText != null) {
            final int position = _component._uiText.findCaretPosition(x, y);
            if (_component.isCopyable() && copyToggled) {
                final TextSelection selection = _component.getSelection();
                selection.checkStart();
                if (selection.getState() == SelectionState.AT_START_OF_SELECTION) {
                    if (position <= selection.getEndIndex()) {
                        selection.setStartIndex(position);
                    } else {
                        final int oldEnd = selection.getEndIndex();
                        selection.setEndIndex(position);
                        selection.setStartIndex(oldEnd);
                        selection.setState(SelectionState.AT_END_OF_SELECTION);
                    }
                } else {
                    if (position >= selection.getStartIndex()) {
                        selection.setEndIndex(position);
                    } else {
                        final int oldStart = selection.getStartIndex();
                        selection.setStartIndex(position);
                        selection.setEndIndex(oldStart);
                        selection.setState(SelectionState.AT_START_OF_SELECTION);
                    }
                }
            } else {
                // clear any getSelection()
                _component.clearSelection();
            }
            _component.setCaretPosition(position);
        } else {
            _component.setCaretPosition(0);
        }
    }

    @Override
    public void lostFocus() {
        _component.switchState(_component.getDefaultState());
    }
}
