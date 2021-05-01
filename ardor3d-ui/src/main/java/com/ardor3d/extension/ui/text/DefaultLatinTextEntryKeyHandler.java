/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.ui.text;

import com.ardor3d.input.InputState;
import com.ardor3d.input.keyboard.Key;
import com.ardor3d.math.Vector2;
import com.ardor3d.util.trigger.TimedTrigger;

public class DefaultLatinTextEntryKeyHandler implements UIKeyHandler {

  public static int TAB_SIZE = 4;

  private final AbstractUITextEntryComponent _textEntry;
  private final TimedTrigger _repeatTimer = new TimedTrigger();

  public DefaultLatinTextEntryKeyHandler(final AbstractUITextEntryComponent textEntry) {
    _textEntry = textEntry;
  }

  @Override
  public boolean keyReleased(final Key key, final InputState state) {
    _repeatTimer.disarm();
    return true;
  }

  @Override
  public boolean keyHeld(final Key key, final InputState state) {
    // start a timer if we haven't already.
    if (_repeatTimer.isArmed()) {
      _repeatTimer.checkTrigger();
    } else if (_repeatTimer.isTriggered()) {
      keyPressed(key, state);
      _repeatTimer.arm(UIKeyHandler.KeyRepeatIntervalTime);
    } else {
      _repeatTimer.arm(UIKeyHandler.KeyRepeatStartTime);
    }
    // if timer has passed, call keyPressed instead.
    return true;
  }

  @Override
  public boolean keyPressed(final Key key, final InputState state) {
    // grab our text
    String text = _textEntry.getText();

    // get some meta key states
    final boolean shiftKeyDown = isShiftDown(state);
    final boolean ctrlKeyDown = isCtrtlDown(state);

    // get our current caret location
    int caretPosition = _textEntry.getCaretPosition();
    if (caretPosition > text.length()) {
      caretPosition = _textEntry.setCaretPosition(text.length());
    }

    // get our text selection object
    final TextSelection selection = _textEntry.getSelection();

    // divide the text based on caret position.
    String s1 = text.substring(0, caretPosition);
    String s2 = text.substring(caretPosition, text.length());

    // look to handle special keys
    RenderedText uiText = _textEntry._uiText;
    switch (key) {
      case ENTER: {
        if (handleEnterKey()) {
          return true;
        }
        break;
      }
      case X: {
        // CUT
        if (!_textEntry.isEditable() || !ctrlKeyDown) {
          break;
        }

        if (_textEntry.isCopyable() && selection.getSelectionLength() > 0) {
          CopyPasteManager.INSTANCE.setClipBoardContents(_textEntry.getSelectedText());
          _textEntry.deleteSelectedText();
          caretPosition = _textEntry.setCaretPosition(selection.getStartIndex());
          _textEntry.clearSelection();
        }

        return true;
      }

      case C: {
        // COPY
        if (!ctrlKeyDown) {
          break;
        }

        if (_textEntry.isCopyable() && selection.getSelectionLength() > 0) {
          CopyPasteManager.INSTANCE.setClipBoardContents(_textEntry.getSelectedText());
        }

        return true;
      }

      case V: {
        // PASTE
        if (!_textEntry.isEditable() || !ctrlKeyDown) {
          break;
        }

        final String clipContents = CopyPasteManager.INSTANCE.getClipBoardContents();
        if (clipContents != null) {
          if (selection.getStartIndex() < selection.getEndIndex()) {
            _textEntry.deleteSelectedText();
            uiText = _textEntry._uiText;
            text = _textEntry.getText();
            caretPosition = _textEntry.setCaretPosition(selection.getStartIndex());
            _textEntry.clearSelection();
            s1 = text.substring(0, caretPosition);
            s2 = text.substring(caretPosition, text.length());
          }
          final int before = uiText != null ? uiText.getData()._characters.size() : 0;
          _textEntry.setText(s1 + clipContents + s2);
          uiText = _textEntry._uiText;
          final int after = uiText.getData()._characters.size();
          _textEntry.setCaretPosition(caretPosition + after - before);
        }

        return true;
      }

      case BACKSPACE: {
        // Backspace
        if (!_textEntry.isEditable()) {
          return false;
        }

        if (selection.getSelectionLength() > 0) {
          _textEntry.deleteSelectedText();
          _textEntry.setCaretPosition(selection.getStartIndex());
          _textEntry.clearSelection();
        } else if (s1.length() > 0) {
          _textEntry.setText(s1.substring(0, s1.length() - 1) + s2);
          _textEntry.setCaretPosition(caretPosition - 1);
        }

        return true;
      }

      case DELETE: {
        // delete
        if (!_textEntry.isEditable()) {
          return false;
        }

        if (selection.getSelectionLength() > 0) {
          _textEntry.deleteSelectedText();
          _textEntry.setCaretPosition(selection.getStartIndex());
          _textEntry.clearSelection();
        } else if (s2.length() > 0) {
          _textEntry.setText(s1 + s2.substring(1, s2.length()));
        }

        return true;
      }

      case RIGHT: {
        // check if a selection should be started
        if (shiftKeyDown) {
          selection.checkStart();
        }

        // TODO: Handle word selection when ctrl down
        _textEntry.setCaretPosition(caretPosition + 1);

        if (shiftKeyDown) {
          selection.rightKey();
        } else {
          selection.reset();
        }

        _textEntry.fireComponentDirty();
        return true;
      }

      case LEFT: {
        // check if a selection should be started
        if (shiftKeyDown) {
          selection.checkStart();
        }

        // TODO: Handle word selection when ctrl down
        _textEntry.setCaretPosition(caretPosition - 1);

        if (shiftKeyDown) {
          selection.leftKey();
        } else {
          selection.reset();
        }

        _textEntry.fireComponentDirty();
        return true;
      }

      case UP: {
        // check if a selection should be started
        if (shiftKeyDown) {
          selection.checkStart();
        }

        if (uiText == null) {
          _textEntry.setCaretPosition(0);
        } else {
          final int line = uiText.getLineFromCaretPosition(caretPosition);
          if (line == 0) {
            _textEntry.setCaretPosition(0);
          } else {
            final Vector2 pos = uiText.findCaretTranslation(caretPosition, null);
            pos.setY(pos.getY() + uiText.getData()._lineHeights.get(line - 1) + 1);
            _textEntry.setCaretPosition(uiText.findCaretPosition((int) pos.getX(), (int) pos.getY()));
          }
        }

        if (shiftKeyDown) {
          selection.upKey();
        } else {
          selection.reset();
        }

        _textEntry.fireComponentDirty();
        return true;
      }

      case DOWN: {
        // check if a selection should be started
        if (shiftKeyDown) {
          selection.checkStart();
        }

        if (uiText == null) {
          _textEntry.setCaretPosition(0);
        } else {
          final int line = uiText.getLineFromCaretPosition(caretPosition);
          if (line >= uiText.getData()._lineHeights.size() - 1) {
            _textEntry.setCaretPosition(text.length());
          } else {
            final Vector2 pos = uiText.findCaretTranslation(caretPosition, null);
            pos.setY(pos.getY() - 1);
            _textEntry.setCaretPosition(uiText.findCaretPosition((int) pos.getX(), (int) pos.getY()));
          }
        }

        if (shiftKeyDown) {
          selection.downKey();
        } else {
          selection.reset();
        }

        _textEntry.fireComponentDirty();
        return true;
      }

      case HOME: {
        // check if a selection should be started
        if (shiftKeyDown) {
          selection.checkStart();
        }

        // go to start if ctrl is down, beginning of line otherwise
        if (ctrlKeyDown || uiText == null) {
          _textEntry.setCaretPosition(0);
        } else {
          final int line = uiText.getLineFromCaretPosition(caretPosition);
          if (line == 0) {
            _textEntry.setCaretPosition(0);
          } else {
            _textEntry.setCaretPosition(uiText.getData()._lineEnds.get(line - 1) + 1);
          }
        }

        if (shiftKeyDown) {
          selection.upKey();
        } else {
          selection.reset();
        }

        _textEntry.fireComponentDirty();
        return true;
      }

      case END: {
        // check if a selection should be started
        if (shiftKeyDown) {
          selection.checkStart();
        }

        // go to end if ctrl is down, beginning of line otherwise
        if (ctrlKeyDown || uiText == null) {
          _textEntry.setCaretPosition(text.length());
        } else {
          final int line = uiText.getLineFromCaretPosition(caretPosition);
          if (line >= uiText.getData()._lineHeights.size() - 1) {
            _textEntry.setCaretPosition(text.length());
          } else {
            _textEntry.setCaretPosition(uiText.getData()._lineEnds.get(line));
          }
        }
        if (shiftKeyDown) {
          selection.downKey();
        } else {
          selection.reset();
        }
        _textEntry.fireComponentDirty();
        return true;
      }

      default:
        // not special, ignore
        break;
    }

    return true;
  }

  @Override
  public boolean characterReceived(final char value, final InputState state) {

    if (_textEntry.isEditable()) {
      // grab our text
      String text = _textEntry.getText();

      // get our current caret location
      int caretPosition = _textEntry.getCaretPosition();
      if (caretPosition > text.length()) {
        caretPosition = _textEntry.setCaretPosition(text.length());
      }

      // get our text selection object
      final TextSelection selection = _textEntry.getSelection();

      // divide the text based on caret position.
      String s1 = text.substring(0, caretPosition);
      String s2 = text.substring(caretPosition, text.length());

      char c = value;
      if (c == '\r') {
        c = '\n';
      }

      if (c >= 32 && c != (char) -1 || c == '\n' || c == '\t') {
        if (selection.getSelectionLength() > 0) {
          _textEntry.deleteSelectedText();
          text = _textEntry.getText();
          if (text == null) {
            text = "";
          }
          caretPosition = _textEntry.setCaretPosition(selection.getStartIndex());
          _textEntry.clearSelection();
          s1 = text.substring(0, caretPosition);
          s2 = text.substring(caretPosition, text.length());
        }

        if (c == '\t') {
          _textEntry.setText(s1 + String.format("%" + DefaultLatinTextEntryKeyHandler.TAB_SIZE + "s", ' ') + s2);
          caretPosition = _textEntry.setCaretPosition(caretPosition + DefaultLatinTextEntryKeyHandler.TAB_SIZE);
        } else {
          _textEntry.setText(s1 + c + s2);
          caretPosition = _textEntry.setCaretPosition(caretPosition + 1);
        }
      }
    }

    return false;
  }

  protected boolean isShiftDown(final InputState state) {
    return state.getKeyboardState().isAtLeastOneDown(Key.LEFT_SHIFT, Key.RIGHT_SHIFT);
  }

  protected boolean isCtrtlDown(final InputState state) {
    if (System.getProperty("mrj.version") != null) {
      // mac
      return state.getKeyboardState().isAtLeastOneDown(Key.LEFT_META, Key.RIGHT_META);
    } else {
      // non-mac
      return state.getKeyboardState().isAtLeastOneDown(Key.LEFT_CONTROL, Key.RIGHT_CONTROL);
    }
  }

  protected boolean handleEnterKey() {
    return false;
  }
}
