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

import java.util.Collection;
import java.util.List;

import com.ardor3d.math.Vector2;
import com.ardor3d.math.type.ReadOnlyTransform;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Renderable;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.event.DirtyType;
import com.ardor3d.util.scenegraph.RenderDelegate;
import com.google.common.collect.Lists;

public class RenderedText extends Node implements Renderable {

    protected String _plainText = null;
    protected List<StyleSpan> _parsedStyles = Lists.newLinkedList();

    protected float _width;
    protected float _height;
    protected final RenderedTextData data = new RenderedTextData();

    protected boolean _styled = false;

    public RenderedText() {}

    public String getText() {
        if (!isStyled()) {
            return _plainText;
        } else {
            return TextFactory.INSTANCE.getMarkedUpText(_plainText, _parsedStyles);
        }
    }

    public void setPlainText(final String text) {
        _plainText = text;
    }

    public String getPlainText() {
        return _plainText;
    }

    public void setHeight(final float height) {
        _height = height;
    }

    public float getHeight() {
        return _height;
    }

    public void setWidth(final float width) {
        _width = width;
    }

    public float getWidth() {
        return _width;
    }

    public List<StyleSpan> getParsedStyleSpans() {
        return _parsedStyles;
    }

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

        final RenderDelegate delegate = getCurrentRenderDelegate();
        if (delegate == null) {
            r.draw((Renderable) this);
        } else {
            delegate.render(this, r);
        }
    }

    @Override
    public void updateWorldBound(final boolean recurse) {
        clearDirty(DirtyType.Bounding);
    }

    @Override
    public void render(final Renderer renderer) {
        for (int i = getNumberOfChildren() - 1; i >= 0; i--) {
            ((TextMesh) _children.get(i)).render(renderer);
        }
    }

    @Override
    public void setWorldTransform(final ReadOnlyTransform transform) {
        for (int i = getNumberOfChildren() - 1; i >= 0; i--) {
            ((TextMesh) _children.get(i)).setWorldTransform(transform);
        }
    }

    public RenderedTextData getData() {
        return data;
    }

    public Vector2 findCaretTranslation(final int caretPosition, final Vector2 store) {
        Vector2 rVal = store;
        if (rVal == null) {
            rVal = new Vector2(0, 0);
        }

        if (data._lineHeights == null) {
            return rVal;
        }

        int lineHeight = data._lineHeights.get(0);
        int cursorY = 0;
        for (int j = 1; j < data._lineEnds.size(); j++) {
            if (data._lineEnds.get(j) < caretPosition) {
                cursorY += lineHeight;
                lineHeight = data._lineHeights.get(j);
            } else {
                break;
            }
        }

        if (caretPosition < data._xStarts.size()) {
            rVal.setX(data._xStarts.get(caretPosition));
        } else {
            final CharacterDescriptor charDesc = data._characters.get(caretPosition - 1);
            rVal.setX(data._xStarts.get(caretPosition - 1)
                    + (int) Math.round(charDesc.getScale() * charDesc.getXAdvance()));
        }
        rVal.setY(cursorY);

        return rVal;
    }

    public int findCaretPosition(final int x, final int y) {
        int height = 0;
        int position = 0;
        final int max = data._lineEnds.size();
        int line;
        for (line = 0; line < max; line++) {
            height += data._lineHeights.get(line);

            if (height < y) {
                position = data._lineEnds.get(line) + 1;
            } else {
                break;
            }
        }

        if (line < max) {
            for (int i = 0, maxX = data._lineEnds.get(line); i <= maxX; i++) {
                if (data._xStarts.get(position) + data._characters.get(position).getXAdvance() / 2 < x) {
                    position++;
                } else {
                    break;
                }
            }
        }

        return position;
    }

    public int getLineHeight(final int caretPosition) {
        if (caretPosition < data._fontHeights.size()) {
            return data._fontHeights.get(caretPosition);
        } else {
            return data._fontHeights.get(data._fontHeights.size() - 1);
        }
    }

    public static class RenderedTextData {
        public List<Integer> _xStarts = Lists.newArrayList();
        public List<Integer> _lineHeights = Lists.newArrayList();
        public List<Integer> _lineEnds = Lists.newArrayList();
        public List<Integer> _fontHeights = Lists.newArrayList();
        public List<CharacterDescriptor> _characters = Lists.newArrayList();

        public RenderedTextData() {}

        public void reset() {
            _xStarts.clear();
            _lineHeights.clear();
            _lineEnds.clear();
            _fontHeights.clear();
            _characters.clear();
        }
    }

    public void setStyled(final boolean styled) {
        _styled = styled;
    }

    public boolean isStyled() {
        return _styled;
    }

    public void setOpacity(float alpha) {
        for (int i = getNumberOfChildren() - 1; i >= 0; i--) {
            ((TextMesh) _children.get(i)).setOpacity(alpha);
        }
    }
    
}
