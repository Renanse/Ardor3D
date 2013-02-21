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

import com.ardor3d.extension.ui.util.ButtonGroup;
import com.ardor3d.extension.ui.util.SubTex;

/**
 * A extension of button that is specifically selectable and is generally used with a {@link ButtonGroup}. This class is
 * defined distinctly from UIButton to allow for specific skinning.
 */
public class UIRadioButton extends UIButton {

    public UIRadioButton() {
        this("");
    }

    public UIRadioButton(final String text) {
        this(text, null);
    }

    public UIRadioButton(final String text, final SubTex icon) {
        super(text, icon);
        super.setSelectable(true);
    }

    /**
     * Ignored
     */
    @Override
    public void setSelectable(final boolean selectable) {
        ;
    }

    /**
     * @return true
     */
    @Override
    public boolean isSelectable() {
        return true;
    }
}
