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

import java.util.function.BiConsumer;

import com.ardor3d.extension.interact.InteractManager;
import com.ardor3d.extension.interact.data.SpatialState;
import com.ardor3d.extension.interact.widget.AbstractInteractWidget;
import com.ardor3d.input.mouse.MouseState;

/**
 * Filter used to modify {@link SpatialState} information prior to it being applied to a Spatial by the
 * {@link InteractManager}.
 */
public interface UpdateFilter {

    /**
     * Called after a successful application of mouse/key input.
     *
     * @param manager
     * @param widget
     */
    void applyFilter(InteractManager manager, AbstractInteractWidget widget);

    /**
     * Callback for when a control begins a drag operation.
     *
     * @param manager
     * @param widget
     * @param state
     */
    void beginDrag(InteractManager manager, AbstractInteractWidget widget, MouseState state);

    /**
     * Callback for when a control ends a drag operation.
     *
     * @param manager
     * @param widget
     */
    void endDrag(InteractManager manager, AbstractInteractWidget widget, MouseState state);

    static UpdateFilter applyFilterAdapter(final BiConsumer<InteractManager, AbstractInteractWidget> c) {
        return new UpdateFilterAdapter() {
            @Override
            public void applyFilter(final InteractManager manager, final AbstractInteractWidget widget) {
                c.accept(manager, widget);
            }
        };
    }
}
