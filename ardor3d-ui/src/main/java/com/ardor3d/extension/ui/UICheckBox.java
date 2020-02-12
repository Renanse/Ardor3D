/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.ui;

import com.ardor3d.extension.ui.util.SubTex;

/**
 * A extension of button that is specifically selectable. This class is defined distinctly from UIButton to allow for
 * specific skinning.
 */
public class UICheckBox extends UIButton {

    public UICheckBox() {
        this("");
    }

    public UICheckBox(final String text) {
        this(text, null);
    }

    public UICheckBox(final String text, final SubTex icon) {
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
