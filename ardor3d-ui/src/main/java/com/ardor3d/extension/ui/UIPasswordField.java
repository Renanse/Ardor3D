/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.ui;

public class UIPasswordField extends UITextField {

    protected char _passwordChar = '*';

    public UIPasswordField() {
        super.setCopyable(false);
    }

    public char getPasswordChar() {
        return _passwordChar;
    }

    public void setPasswordChar(final char passwordChar) {
        _passwordChar = passwordChar;
    }

    @Override
    public void setText(final String text) {
        final StringBuilder newText = new StringBuilder();
        if (text != null) {
            for (int i = text.length(); --i >= 0;) {
                newText.append(_passwordChar);
            }
        }
        super.setText(newText.toString());
        if (_uiText != null) {
            _uiText.setPlainText(text);
        }
    }

    @Override
    public void setCopyable(final boolean copyable) {
        ; // ignore
    }

    @Override
    public boolean isCopyable() {
        return false;
    }
}
