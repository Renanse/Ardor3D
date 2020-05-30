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

import java.util.Map;

import com.ardor3d.extension.ui.StateBasedUIComponent;
import com.ardor3d.extension.ui.Textable;
import com.ardor3d.extension.ui.UIComponent;
import com.ardor3d.extension.ui.UIState;
import com.ardor3d.extension.ui.util.Alignment;
import com.ardor3d.input.InputState;
import com.ardor3d.input.mouse.MouseButton;
import com.ardor3d.math.Rectangle2;
import com.ardor3d.math.Transform;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Renderer;
import com.google.common.collect.ImmutableSet;

public abstract class AbstractUITextEntryComponent extends StateBasedUIComponent implements Textable {
  /** tracking variable for dirty use only */
  protected boolean _caretIsShowing = false;

  /** A store for the clip rectangle. */
  private final Rectangle2 _clipRectangleStore = new Rectangle2();

  protected final Vector2 _caretLoc = new Vector2();

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

  /**
   * Alignment value to use to position the icon/text within the overall dimensions of this component.
   */
  protected Alignment _alignment = Alignment.BOTTOM_LEFT;

  protected TextCaret _caret = new TextCaret();

  protected TextSelection _selection = new TextSelection() {
    @Override
    public int getCaretPosition() { return AbstractUITextEntryComponent.this.getCaretPosition(); }

    @Override
    public void setCaretPosition(final int position) {
      AbstractUITextEntryComponent.this.setCaretPosition(position);
    }

    @Override
    public RenderedText getRenderedText() { return _uiText; }
  };

  protected abstract UIKeyHandler getKeyHandler();

  @Override
  public UIState getDefaultState() { return _defaultState; }

  @Override
  public UIState getDisabledState() { return _disabledState; }

  public UIState getWritingState() { return _writingState; }

  @Override
  public String getRawText() { return _uiText != null ? _uiText.getRawText() : null; }

  @Override
  public String getText() {
    return _uiText != null && _uiText.getVisibleText() != null ? _uiText.getVisibleText() : "";
  }

  /**
   * Set the text for this component. Also updates the minimum size of the component.
   *
   * @param rawText
   *          the new text
   */
  @Override
  public void setText(String rawText) {
    rawText = validateInputText(rawText, _uiText != null ? _uiText.getRawText() : null);

    if (rawText != null) {
      int maxWidth = getMaximumLocalComponentWidth() - getTotalLeft() - getTotalRight();
      if (maxWidth <= 0) {
        maxWidth = -1;
      }

      _uiText =
          TextFactory.INSTANCE.generateText(formatRawText(rawText), isStyledText(), getFontStyles(), _uiText, maxWidth);
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
   * Applies default formatting to raw text for this component prior to passing it to rendered text
   * generation.
   *
   * @param rawText
   * @return by default, returns the rawText as is.
   */
  protected String formatRawText(final String rawText) {
    return rawText;
  }

  @Override
  public boolean isStyledText() { return _styled; }

  @Override
  public void setStyledText(final boolean value) { _styled = value; }

  public boolean isCopyable() { return _copyable; }

  public void setCopyable(final boolean copyable) { _copyable = copyable; }

  /**
   * Set the position of the text caret as an index to the current set text string. If the specified
   * position is after the last possible index, it is set to the last possible index.
   *
   * @param index
   *          the new position
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

    if (_uiText != null) {
      _uiText.findCaretTranslation(index, _caretLoc);
      getCaret().setPosX(Math.round(_caretLoc.getXf()));
      getCaret().setPosY(Math.round(_caretLoc.getYf()));
    } else {
      getCaret().setPosX(0);
      getCaret().setPosY(0);
    }

    return _caretPosition;
  }

  public int getCaretPosition() { return _caretPosition; }

  public int getSelectionLength() { return _selection.getSelectionLength(); }

  public void clearSelection() {
    _selection.reset();
  }

  public boolean isEditable() { return _editable; }

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

  public String getSelectedText() {
    if (_selection.getSelectionLength() != 0) {
      final String text = getText();
      final int start = getSelection().getStartIndex();
      return text.substring(start, start + getSelectionLength());
    }

    return "";
  }

  /**
   * @param editable
   *          true if the text of this component can be changed by keyboard interaction
   */
  public void setEditable(final boolean editable) { _editable = editable; }

  public TextCaret getCaret() { return _caret; }

  public TextSelection getSelection() { return _selection; }

  public Alignment getAlignment() { return _alignment; }

  public void setAlignment(final Alignment alignment) { _alignment = alignment; }

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
  public void updateGeometricState(final double time, final boolean initiator) {
    if (getCurrentState().equals(_writingState) && _caretIsShowing != getCaret().isShowing()) {
      fireComponentDirty();
      _caretIsShowing = !_caretIsShowing;
    }
    super.updateGeometricState(time, initiator);
  }

  @Override
  protected void drawComponent(final Renderer r) {
    // figure out our offsets using alignment and edge info
    final double x = _alignment.alignX(getContentWidth(), _uiText != null ? _uiText.getWidth() : 1) + getTotalLeft();
    final double y =
        _alignment.alignY(getContentHeight(), _uiText != null ? _uiText.getHeight() : 1) + getTotalBottom();

    // Draw our text, if we have any
    if (_uiText != null) {
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
        _clipRectangleStore.set(getHudX() + getTotalLeft(), getHudY() + getTotalBottom(), getContentWidth(),
            getContentHeight());
        r.getScissorUtils().pushClip(_clipRectangleStore);
      }

      _uiText.render(r);
      if (needsPop) {
        r.getScissorUtils().popClip();
      }
    }

    // Draw our caret, if we have one.
    if (isEditable() && getCurrentState().equals(_writingState) && getCaret().isShowing()) {
      if (_uiText == null) {
        getCaret().draw(r, this, UIComponent.getDefaultFontSize(), x, y);
      } else {
        getCaret().draw(r, this, _uiText.getFontHeightFromCaretPosition(getCaretPosition()), x, y);
      }
    }
  }

  @Override
  public ImmutableSet<UIState> getStates() { return ImmutableSet.of(_defaultState, _disabledState, _writingState); }

  protected class DefaultTextEntryState extends UIState {
    @Override
    public boolean mousePressed(final MouseButton button, final InputState state) {
      switchState(_writingState);

      final int x = state.getMouseState().getX() - AbstractUITextEntryComponent.this.getHudX()
          - AbstractUITextEntryComponent.this.getPadding().getLeft();
      final int y = state.getMouseState().getY() - AbstractUITextEntryComponent.this.getHudY()
          - AbstractUITextEntryComponent.this.getPadding().getBottom();

      setCaretPosition(_uiText != null ? _uiText.findCaretPosition(x, y) : 0);
      clearSelection();

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
