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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.ardor3d.math.Vector2;
import com.ardor3d.math.type.ReadOnlyTransform;
import com.ardor3d.renderer.Renderable;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.event.DirtyType;

public class RenderedText extends Node implements Renderable {

  protected String _rawText = null;
  protected String _visibleText = null;

  protected List<StyleSpan> _parsedStyles = new LinkedList<>();

  protected float _width;
  protected float _height;
  protected final RenderedTextData _data = new RenderedTextData();

  protected boolean _styled = false;

  public RenderedText() {}

  public String getRawText() { return _rawText; }

  public void setRawText(final String rawText) { _rawText = rawText; }

  public String getVisibleText() { return _visibleText; }

  public void setVisibleText(final String visibleText) { _visibleText = visibleText; }

  public void setHeight(final float height) { _height = height; }

  public float getHeight() { return _height; }

  public void setWidth(final float width) { _width = width; }

  public float getWidth() { return _width; }

  public List<StyleSpan> getParsedStyleSpans() { return _parsedStyles; }

  public void setParsedStyleSpans(final Collection<StyleSpan> spans) {
    _parsedStyles.clear();
    _parsedStyles.addAll(spans);
  }

  @Override
  public int attachChild(final Spatial child) {
    if (!(child instanceof TextMesh)) {
      throw new IllegalArgumentException("Must be a TextMesh!");
    }
    return super.attachChild(child);
  }

  @Override
  public int attachChildAt(final Spatial child, final int index) {
    if (!(child instanceof TextMesh)) {
      throw new IllegalArgumentException("Must be a TextMesh!");
    }
    return super.attachChildAt(child, index);
  }

  @Override
  public void draw(final Renderer r) {
    if (!r.isProcessingQueue()) {
      if (r.checkAndAdd(this)) {
        return;
      }
    }

    r.draw((Renderable) this);
  }

  @Override
  public void updateWorldBound(final boolean recurse) {
    clearDirty(DirtyType.Bounding);
  }

  @Override
  public boolean render(final Renderer renderer) {
    boolean rendered = false;
    for (int i = getNumberOfChildren() - 1; i >= 0; i--) {
      rendered |= ((TextMesh) _children.get(i)).render(renderer);
    }
    return rendered;
  }

  @Override
  public void setWorldTransform(final ReadOnlyTransform transform) {
    for (int i = getNumberOfChildren() - 1; i >= 0; i--) {
      ((TextMesh) _children.get(i)).setWorldTransform(transform);
    }
  }

  public RenderedTextData getData() { return _data; }

  public Vector2 findCaretTranslation(final int caretPosition, final Vector2 store) {
    Vector2 rVal = store;
    if (rVal == null) {
      rVal = new Vector2(0, 0);
    }

    if (_data._lineHeights == null) {
      return rVal;
    }

    final int line = getLineFromCaretPosition(caretPosition);
    rVal.setY(getYOffsetAtLine(line));

    if (caretPosition < _data._xStarts.size()) {
      rVal.setX(_data._xStarts.get(caretPosition));
    } else {
      final CharacterDescriptor charDesc = _data._characters.get(caretPosition - 1);
      rVal.setX(_data._xStarts.get(caretPosition - 1) + (int) Math.round(charDesc.getScale() * charDesc.getXAdvance()));
    }

    return rVal;
  }

  public int findCaretPosition(final int x, final int y) {
    final int maxLines = _data._lineEnds.size();
    int position = 0, line = 0;
    for (int height = _data.getTotalHeight(); line < maxLines; line++) {
      height -= _data._lineHeights.get(line);
      if (height < y) {
        break;
      }

      position = _data._lineEnds.get(line) + 1;
    }

    if (line < maxLines) {
      int maxPos = _data._lineEnds.get(line);
      if (line == maxLines - 1) {
        maxPos++;
      }
      for (; position < maxPos;) {
        if (_data._xStarts.get(position) + _data._characters.get(position).getXAdvance() / 2 < x) {
          position++;
        } else {
          break;
        }
      }
    }

    return position;
  }

  public int getLineFromCaretPosition(final int position) {
    int line;
    final int max = _data._lineEnds.size();
    for (line = 0; line < max; line++) {
      final int endPos = _data._lineEnds.get(line);
      if (endPos >= position) {
        return line;
      }
    }

    return Math.max(0, max - 1);
  }

  public int getFontHeightFromCaretPosition(final int caretPosition) {
    if (caretPosition < _data._fontHeights.size()) {
      return _data._fontHeights.get(caretPosition);
    } else {
      return _data._fontHeights.get(_data._fontHeights.size() - 1);
    }
  }

  public int getYOffsetAtLine(final int line) {
    int offset = _data.getTotalHeight();
    for (int i = 0, maxI = Math.min(line + 1, _data._lineHeights.size()); i < maxI; i++) {
      offset -= _data._lineHeights.get(i);
    }
    return offset;
  }

  public static class RenderedTextData {
    public List<Integer> _xStarts = new ArrayList<>();
    public List<Integer> _lineHeights = new ArrayList<>();
    public List<Integer> _lineEnds = new ArrayList<>();
    public List<Integer> _fontHeights = new ArrayList<>();
    public List<CharacterDescriptor> _characters = new ArrayList<>();

    public RenderedTextData() {}

    public void reset() {
      _xStarts.clear();
      _lineHeights.clear();
      _lineEnds.clear();
      _fontHeights.clear();
      _characters.clear();
    }

    public int getTotalHeight() {
      int height = 0;

      for (int i = 0, maxI = _lineHeights.size(); i < maxI; i++) {
        height += _lineHeights.get(i);
      }

      return height;
    }
  }

  public void setStyled(final boolean styled) { _styled = styled; }

  public boolean isStyled() { return _styled; }
}
