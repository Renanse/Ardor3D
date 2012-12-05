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

import com.ardor3d.extension.ui.event.ActionEvent;
import com.ardor3d.extension.ui.event.ActionListener;
import com.ardor3d.extension.ui.util.SubTex;

/**
 * A simple extension of Button to be used in popup menus. XXX: may be enhanced later to deal with submenus, etc.
 */
public class UIMenuItem extends UIButton {
    public UIMenuItem(final String text) {
        this(text, null);
    }

    public UIMenuItem(final String text, final SubTex icon) {
        super(text, icon);

        // Close menus when a menu item is clicked
        addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent event) {
                final UIHud hud = getHud();
                if (hud != null) {
                    hud.closePopupMenus();
                }
            }
        });
    }
}
