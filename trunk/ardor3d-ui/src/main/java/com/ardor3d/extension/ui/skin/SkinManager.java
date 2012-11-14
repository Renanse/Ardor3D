/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.ui.skin;

import com.ardor3d.extension.ui.UIComponent;
import com.ardor3d.extension.ui.skin.generic.GenericSkin;

public class SkinManager {

    private static Skin _skin = null;

    public static void setDefaultSkin() {
         _skin = new GenericSkin();
    }
    
    public static void setCurrentSkin(final Skin skin) {
        SkinManager._skin = skin;
    }

    public static void applyCurrentSkin(final UIComponent component) {
        if (SkinManager._skin == null) {
                return;
        }

        // apply skin to component
        SkinManager._skin.applyTo(component);
    }

}
