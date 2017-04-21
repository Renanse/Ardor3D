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

import com.ardor3d.extension.ui.UITextField;
import com.ardor3d.input.InputState;
import com.ardor3d.input.Key;

public class DefaultLatinTextFieldKeyHandler implements UIKeyHandler {

    private final UITextField _textField;

    public DefaultLatinTextFieldKeyHandler(final UITextField textField) {
        _textField = textField;
    }

    @Override
    public boolean keyReleased(final Key key, final InputState state) {
        return true;
    }

    @Override
    public boolean keyHeld(final Key key, final InputState state) {
        return true;
    }

    @Override
    public boolean keyPressed(final Key key, final InputState state) {
        // grab our text
        String text = _textField.getText();

        // get some meta key states
        final boolean shiftKeyDown = state.getKeyboardState().isAtLeastOneDown(Key.LSHIFT, Key.RSHIFT);
        final boolean ctrlKeyDown = System.getProperty("mrj.version") == null ?
        // non-mac
        state.getKeyboardState().isAtLeastOneDown(Key.LCONTROL, Key.RCONTROL)
                :
                // mac
                state.getKeyboardState().isAtLeastOneDown(Key.LMETA, Key.RMETA);

        // get our current caret location
        int caretPosition = _textField.getCaretPosition();
        if (caretPosition > text.length()) {
            caretPosition = _textField.setCaretPosition(text.length());
        }

        // get our text selection object
        final TextSelection selection = _textField.getSelection();

        // divide the text based on caret position.
        String s1 = text.substring(0, caretPosition);
        String s2 = text.substring(caretPosition, text.length());

        // look to handle special keys
        switch (key) {
            case RETURN: {
                // ACTION
                _textField.fireActionEvent();
                return true;
            }
            case X: {
                // CUT
                if (!_textField.isEditable() || !ctrlKeyDown) {
                    break;
                }

                if (_textField.isCopyable() && selection.getSelectionLength() > 0) {
                    final String selectedText = text.substring(selection.getStartIndex(), _textField.getSelection()
                            .getStartIndex()
                            + _textField.getSelectionLength());
                    CopyPasteManager.INSTANCE.setClipBoardContents(selectedText);
                    _textField.deleteSelectedText();
                    text = _textField.getText();
                    if (text == null) {
                        text = "";
                    }
                    caretPosition = _textField.setCaretPosition(selection.getStartIndex());
                    _textField.clearSelection();
                }

                return true;
            }

            case C: {
                // COPY
                if (!ctrlKeyDown) {
                    break;
                }
                if (_textField.isCopyable()) {
                    final String selectedText = text.substring(selection.getStartIndex(), _textField.getSelection()
                            .getStartIndex()
                            + _textField.getSelectionLength());
                    CopyPasteManager.INSTANCE.setClipBoardContents(selectedText);
                }
                return true;
            }

            case V: {
                // PASTE
                if (!_textField.isEditable() || !ctrlKeyDown) {
                    break;
                }
                final String clipContents = CopyPasteManager.INSTANCE.getClipBoardContents();
                if (clipContents != null) {
                    if (selection.getStartIndex() < _textField.getSelection().getEndIndex()) {
                        _textField.deleteSelectedText();
                        text = _textField.getText();
                        if (text == null) {
                            text = "";
                        }
                        caretPosition = _textField.setCaretPosition(selection.getStartIndex());
                        _textField.clearSelection();
                        s1 = text.substring(0, caretPosition);
                        s2 = text.substring(caretPosition, text.length());
                    }
                    _textField.setText(s1 + clipContents + s2);
                    caretPosition = _textField.setCaretPosition(caretPosition + clipContents.length());
                }
                return true;
            }

            case BACK: {
                // Backspace
                if (!_textField.isEditable()) {
                    return false;
                }
                if (_textField.getSelection().getSelectionLength() > 0) {
                    _textField.deleteSelectedText();
                    text = _textField.getText();
                    if (text == null) {
                        text = "";
                    }
                    caretPosition = _textField.setCaretPosition(selection.getStartIndex());
                    _textField.clearSelection();
                } else if (s1.length() > 0) {
                    _textField.setText(s1.substring(0, s1.length() - 1) + s2);
                    caretPosition = _textField.setCaretPosition(caretPosition - 1);
                } else {
                    _textField.setText(s2);
                    caretPosition = _textField.setCaretPosition(0);
                }
                return true;
            }

            case DELETE: {
                // delete
                if (!_textField.isEditable()) {
                    break;
                }
                if (_textField.getSelection().getSelectionLength() > 0) {
                    _textField.deleteSelectedText();
                    text = _textField.getText();
                    if (text == null) {
                        text = "";
                    }
                    caretPosition = _textField.setCaretPosition(selection.getStartIndex());
                    _textField.clearSelection();
                } else if (s2.length() > 0) {
                    _textField.setText(s1 + s2.substring(1, s2.length()));
                }
                return true;
            }

            case RIGHT: {
                // check if a selection should be started
                if (shiftKeyDown) {
                    _textField.getSelection().checkStart();
                }

                if (caretPosition < text.length()) {
                    caretPosition = _textField.setCaretPosition(caretPosition + 1);
                }
                if (shiftKeyDown) {
                    _textField.getSelection().rightKey();
                } else {
                    _textField.getSelection().reset();
                }
                _textField.fireComponentDirty();
                return true;
            }

            case LEFT: {
                // check if a selection should be started
                if (shiftKeyDown) {
                    _textField.getSelection().checkStart();
                }

                if (caretPosition > 0) {
                    caretPosition = _textField.setCaretPosition(caretPosition - 1);
                }
                if (shiftKeyDown) {
                    _textField.getSelection().leftKey();
                } else {
                    _textField.getSelection().reset();
                }
                _textField.fireComponentDirty();
                return true;
            }

            case UP:
            case HOME: {
                // check if a selection should be started
                if (shiftKeyDown) {
                    _textField.getSelection().checkStart();
                }

                if (caretPosition > 0) {
                    caretPosition = _textField.setCaretPosition(0);
                }
                if (shiftKeyDown) {
                    _textField.getSelection().upKey();
                } else {
                    _textField.getSelection().reset();
                }
                _textField.fireComponentDirty();
                return true;
            }

            case DOWN:
            case END: {
                // check if a selection should be started
                if (shiftKeyDown) {
                    _textField.getSelection().checkStart();
                }

                if (caretPosition < text.length()) {
                    _textField.setCaretPosition(text.length());
                }
                if (shiftKeyDown) {
                    _textField.getSelection().downKey();
                } else {
                    _textField.getSelection().reset();
                }
                _textField.fireComponentDirty();
                return true;
            }
                    default:
                        // not special, ignore
                        break;
        }

        if (_textField.isEditable()) {
            final char c = state.getKeyboardState().getKeyEvent().getKeyChar();
            if (c >= 32 && c != (char) -1) {
                if (selection.getSelectionLength() > 0) {
                    _textField.deleteSelectedText();
                    text = _textField.getText();
                    if (text == null) {
                        text = "";
                    }
                    caretPosition = _textField.setCaretPosition(selection.getStartIndex());
                    _textField.clearSelection();
                    s1 = text.substring(0, caretPosition);
                    s2 = text.substring(caretPosition, text.length());
                }

                _textField.setText(s1 + c + s2);
                caretPosition = _textField.setCaretPosition(caretPosition + 1);
            }
        }

        return true;
    }
}
