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

/**
 * Used in conjunction with {@link StateBasedUIComponent} to describe the appearance and behavior of a specific
 * component state.
 */
public class UIState extends UIComponent {

    public UIState() {
        setBackdrop(null);
        setBorder(null);
        setForegroundColor(null);
        setMargin(null);
        setPadding(null);
    }

    /**
     * Applies the local characteristics of this state to the given UIComponent.
     * 
     * @param component
     *            the UI component to apply this state to
     */
    public void setupAppearance(final UIComponent component) {
        if (getBackdrop() != null) {
            component.setBackdrop(getBackdrop());
        }
        if (getBorder() != null) {
            component.setBorder(getBorder());
        }
        if (getLocalForegroundColor() != null) {
            component.setForegroundColor(getLocalForegroundColor());
        }
        if (getMargin() != null) {
            component.setMargin(getMargin());
        }
        if (getPadding() != null) {
            component.setPadding(getPadding());
        }
        if (getTooltipText() != null) {
            component.setTooltipText(getTooltipText());
        }
    }

    /**
     * Called right before a state loses its status as being the "current" state of a UIComponent.
     */
    public void release() {}

    @Deprecated
    @Override
    public int getHudX() {
        throw new RuntimeException("Do not call getHudX directly on a ui state.  Call on associated component instead.");
    }

    @Deprecated
    @Override
    public int getHudY() {
        throw new RuntimeException("Do not call getHudY directly on a ui state.  Call on associated component instead.");
    }
}
