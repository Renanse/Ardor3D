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

import com.ardor3d.extension.ui.border.EmptyBorder;
import com.ardor3d.extension.ui.layout.RowLayout;

/**
 * An simple, undecorated frame meant for showing content in the UI that can not be moved or resized by the user.
 */
public class FloatingUIContainer extends UIFrame {

    public FloatingUIContainer() {
        super(null);
        setDecorated(false);
        getContentPanel().setBorder(new EmptyBorder());
        getContentPanel().setLayout(new RowLayout(false));
        setBackdrop(null);

        applySuperSkin();
    }

    protected void applySuperSkin() {
        super.applySkin();
    }

    @Override
    protected void applySkin() {
        ;
    }
}
