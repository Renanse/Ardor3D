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

public class StyleSpan implements Comparable<StyleSpan> {
    protected int _spanStart;
    protected int _spanLength;
    protected String _style;
    protected Object _value;

    public StyleSpan() {}

    public StyleSpan(final String style, final Object value, final int start, final int length) {
        _style = style;
        _value = value;
        _spanStart = start;
        _spanLength = length;
    }

    public int getSpanStart() {
        return _spanStart;
    }

    public void setSpanStart(final int spanStart) {
        _spanStart = spanStart;
    }

    public int getSpanLength() {
        return _spanLength;
    }

    public void setSpanLength(final int length) {
        _spanLength = length;
    }

    public String getStyle() {
        return _style;
    }

    public void setStyle(final String style) {
        _style = style;
    }

    public Object getValue() {
        return _value;
    }

    public void setValue(final Object value) {
        _value = value;
    }

    @Override
    public int compareTo(final StyleSpan span) {
        if (_spanStart == span._spanStart) {
            return span._spanLength - _spanLength;
        }
        return _spanStart - span._spanStart;
    }
}
