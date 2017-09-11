/**
 * Copyright (c) 2008-2017 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.ui;

import com.ardor3d.extension.ui.event.ActionListener;
import com.ardor3d.extension.ui.util.SubTex;

/**
 *
 */
public class UIPieMenuItem extends UIMenuItem {

    public UIPieMenuItem(final String text) {
        this(text, null);
    }

    public UIPieMenuItem(final String text, final SubTex icon) {
        this(text, icon, true, null);
    }

    public UIPieMenuItem(final String text, final SubTex icon, final boolean closeMenuOnSelect,
            final ActionListener listener) {
        super(text, icon, closeMenuOnSelect, listener);
    }

}
