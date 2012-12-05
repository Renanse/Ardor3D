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

import com.ardor3d.extension.ui.UITextArea;
import com.ardor3d.input.InputState;
import com.ardor3d.input.Key;

public class DefaultLatinTextAreaKeyHandler implements UIKeyHandler {

    protected final UITextArea _textArea;

    public DefaultLatinTextAreaKeyHandler(final UITextArea textArea) {
        _textArea = textArea;
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
        String text = _textArea.getText();

        // get some meta key states
        final boolean shiftKeyDown = state.getKeyboardState().isAtLeastOneDown(Key.LSHIFT, Key.RSHIFT);
        final boolean ctrlKeyDown = System.getProperty("mrj.version") == null ?
        // non-mac
        state.getKeyboardState().isAtLeastOneDown(Key.LCONTROL, Key.RCONTROL)
                :
                // mac
                state.getKeyboardState().isAtLeastOneDown(Key.LMETA, Key.RMETA);

        // get our current caret location
        int caretPosition = _textArea.getCaretPosition();
        if (caretPosition > text.length()) {
            caretPosition = _textArea.setCaretPosition(text.length());
        }

        // get our text selection object
        final TextSelection selection = _textArea.getSelection();

        // divide the text based on caret position.
        String s1 = text.substring(0, caretPosition);
        String s2 = text.substring(caretPosition, text.length());

        // look to handle special keys
        switch (key) {
            case X: {
                // CUT
                if (!_textArea.isEditable() || !ctrlKeyDown) {
                    break;
                }

                if (_textArea.isCopyable() && selection.getSelectionLength() > 0) {
                    final String selectedText = text.substring(selection.getStartIndex(), _textArea.getSelection()
                            .getStartIndex()
                            + _textArea.getSelectionLength());
                    CopyPasteManager.INSTANCE.setClipBoardContents(selectedText);
                    _textArea.deleteSelectedText();
                    text = _textArea.getText();
                    if (text == null) {
                        text = "";
                    }
                    caretPosition = _textArea.setCaretPosition(selection.getStartIndex());
                    _textArea.clearSelection();
                }

                return true;
            }

            case C: {
                // COPY
                if (!ctrlKeyDown) {
                    break;
                }
                if (_textArea.isCopyable()) {
                    final String selectedText = text.substring(selection.getStartIndex(), _textArea.getSelection()
                            .getStartIndex()
                            + _textArea.getSelectionLength());
                    CopyPasteManager.INSTANCE.setClipBoardContents(selectedText);
                }
                return true;
            }

            case V: {
                // PASTE
                if (!_textArea.isEditable() || !ctrlKeyDown) {
                    break;
                }
                final String clipContents = CopyPasteManager.INSTANCE.getClipBoardContents();
                if (clipContents != null) {
                    if (selection.getStartIndex() < _textArea.getSelection().getEndIndex()) {
                        _textArea.deleteSelectedText();
                        text = _textArea.getText();
                        if (text == null) {
                            text = "";
                        }
                        caretPosition = _textArea.setCaretPosition(selection.getStartIndex());
                        _textArea.clearSelection();
                        s1 = text.substring(0, caretPosition);
                        s2 = text.substring(caretPosition, text.length());
                    }
                    _textArea.setText(s1 + clipContents + s2);
                    caretPosition = _textArea.setCaretPosition(caretPosition + clipContents.length());
                }
                return true;
            }

            case BACK: {
                // Backspace
                if (!_textArea.isEditable()) {
                    return false;
                }
                if (_textArea.getSelection().getSelectionLength() > 0) {
                    _textArea.deleteSelectedText();
                    text = _textArea.getText();
                    if (text == null) {
                        text = "";
                    }
                    caretPosition = _textArea.setCaretPosition(selection.getStartIndex());
                    _textArea.clearSelection();
                } else if (s1.length() > 0) {
                    _textArea.setText(s1.substring(0, s1.length() - 1) + s2);
                    caretPosition = _textArea.setCaretPosition(caretPosition - 1);
                } else {
                    _textArea.setText(s2);
                    caretPosition = _textArea.setCaretPosition(0);
                }
                return true;
            }

            case DELETE: {
                // delete
                if (!_textArea.isEditable()) {
                    break;
                }
                if (_textArea.getSelection().getSelectionLength() > 0) {
                    _textArea.deleteSelectedText();
                    text = _textArea.getText();
                    if (text == null) {
                        text = "";
                    }
                    caretPosition = _textArea.setCaretPosition(selection.getStartIndex());
                    _textArea.clearSelection();
                } else if (s2.length() > 0) {
                    _textArea.setText(s1 + s2.substring(1, s2.length()));
                }
                return true;
            }

            case UP:
                // TODO
                break;

            case DOWN:
                // TODO
                break;

            case RIGHT: {
                // check if a selection should be started
                if (shiftKeyDown) {
                    _textArea.getSelection().checkStart();
                }

                if (caretPosition < text.length()) {
                    caretPosition = _textArea.setCaretPosition(caretPosition + 1);
                }
                if (shiftKeyDown) {
                    _textArea.getSelection().rightKey();
                } else {
                    _textArea.getSelection().reset();
                }
                _textArea.fireComponentDirty();
                return true;
            }

            case LEFT: {
                // check if a selection should be started
                if (shiftKeyDown) {
                    _textArea.getSelection().checkStart();
                }

                if (caretPosition > 0) {
                    caretPosition = _textArea.setCaretPosition(caretPosition - 1);
                }
                if (shiftKeyDown) {
                    _textArea.getSelection().leftKey();
                } else {
                    _textArea.getSelection().reset();
                }
                _textArea.fireComponentDirty();
                return true;
            }

            case HOME: {
                // check if a selection should be started
                if (shiftKeyDown) {
                    _textArea.getSelection().checkStart();
                }

                if (caretPosition > 0) {
                    caretPosition = _textArea.setCaretPosition(0);
                }
                if (shiftKeyDown) {
                    _textArea.getSelection().upKey();
                } else {
                    _textArea.getSelection().reset();
                }
                _textArea.fireComponentDirty();
                return true;
            }

            case END: {
                // check if a selection should be started
                if (shiftKeyDown) {
                    _textArea.getSelection().checkStart();
                }

                if (caretPosition < text.length()) {
                    _textArea.setCaretPosition(text.length());
                }
                if (shiftKeyDown) {
                    _textArea.getSelection().downKey();
                } else {
                    _textArea.getSelection().reset();
                }
                _textArea.fireComponentDirty();
                return true;
            }
        }

        if (_textArea.isEditable()) {
            final char c = state.getKeyboardState().getKeyEvent().getKeyChar();
            if (c >= 32 && c != (char) -1) {
                if (selection.getSelectionLength() > 0) {
                    _textArea.deleteSelectedText();
                    text = _textArea.getText();
                    if (text == null) {
                        text = "";
                    }
                    caretPosition = _textArea.setCaretPosition(selection.getStartIndex());
                    _textArea.clearSelection();
                    s1 = text.substring(0, caretPosition);
                    s2 = text.substring(caretPosition, text.length());
                }

                _textArea.setText(s1 + c + s2);
                caretPosition = _textArea.setCaretPosition(caretPosition + 1);
            }
        }

        return true;
    }
}
