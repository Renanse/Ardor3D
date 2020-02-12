/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.interact.filter;

import com.ardor3d.extension.interact.InteractManager;
import com.ardor3d.extension.interact.widget.AbstractInteractWidget;
import com.ardor3d.input.mouse.MouseState;

public abstract class UpdateFilterAdapter implements UpdateFilter {

    @Override
    public void applyFilter(final InteractManager manager, final AbstractInteractWidget widget) {
    }

    @Override
    public void beginDrag(final InteractManager manager, final AbstractInteractWidget widget, final MouseState state) {
    }

    @Override
    public void endDrag(final InteractManager manager, final AbstractInteractWidget widget, final MouseState state) {
    }

}
