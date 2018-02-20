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

import com.ardor3d.extension.ui.backdrop.UIBackdrop;
import com.ardor3d.extension.ui.border.UIBorder;
import com.ardor3d.extension.ui.util.Insets;
import com.ardor3d.input.InputState;
import com.ardor3d.input.Key;
import com.ardor3d.input.MouseButton;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.google.common.collect.ImmutableSet;

/**
 * StateBasedUIComponent describes a component that acts as a state machine, containing multiple states, one of which is
 * currently active. Each of these states may override the current appearance and behavior of the component, making this
 * class an ideal base for components such as "mouse over" buttons, components that can be visibly "disabled",
 * toggleable components, etc.
 */
public abstract class StateBasedUIComponent extends UIComponent {

    /** The state currently active on this component. */
    private UIState _currentState = null;

    /**
     * @return the state that represents the default appearance and behavior for this component.
     */
    public abstract UIState getDefaultState();

    /**
     * @return the state that represents the disabled appearance and behavior for this component.
     */
    public abstract UIState getDisabledState();

    /**
     * @return this component's currently active state
     */
    public UIState getCurrentState() {
        return _currentState;
    }

    /**
     * @param enabled
     *            if true, sets the defaultState to active, otherwise sets the disabledState to active.
     */
    @Override
    public void setEnabled(final boolean enabled) {
        super.setEnabled(enabled);
        if (enabled) {
            switchState(getDefaultState());
        } else {
            switchState(getDisabledState());
        }
    }

    /**
     * Set our current active state.
     *
     * @param nextState
     *            the state to set as active for this component. If null, this is a no-op.
     */
    public final void switchState(final UIState nextState) {
        if (nextState == null) {
            return;
        }

        // Release any current state.
        if (_currentState != null) {
            _currentState.release();
        }

        // initialize component from state
        nextState.setupAppearance(this);

        // set as current and fire dirty to notify we have changed.
        _currentState = nextState;
        fireComponentDirty();
    }

    /**
     * Re-apply the current state.
     */
    public void refreshState() {
        switchState(_currentState);
    }

    /**
     * Sets the text color on this component and (optionally) all contained states.
     *
     * @param color
     *            the new foreground color
     * @param allStates
     *            if true, set across all contained states as well as self.
     */
    public void setForegroundColor(final ReadOnlyColorRGBA color, final boolean allStates) {
        super.setForegroundColor(color);
        if (allStates) {
            for (final UIState state : getStates()) {
                state.setForegroundColor(color);
            }
        }
    }

    public void setMargin(final Insets margin, final boolean allStates) {
        super.setMargin(margin);
        if (allStates) {
            for (final UIState state : getStates()) {
                state.setMargin(margin);
            }
        }
    }

    public void setPadding(final Insets padding, final boolean allStates) {
        super.setPadding(padding);
        if (allStates) {
            for (final UIState state : getStates()) {
                state.setPadding(padding);
            }
        }
    }

    public void setBorder(final UIBorder border, final boolean allStates) {
        super.setBorder(border);
        if (allStates) {
            for (final UIState state : getStates()) {
                state.setBorder(border);
            }
        }
    }

    public void setBackdrop(final UIBackdrop backDrop, final boolean allStates) {
        super.setBackdrop(backDrop);
        if (allStates) {
            for (final UIState state : getStates()) {
                state.setBackdrop(backDrop);
            }
        }
    }

    // Redirect input / event methods to current state.

    @Override
    public void mouseDeparted(final int mouseX, final int mouseY, final InputState state) {
        getCurrentState().mouseDeparted(mouseX, mouseY, state);
        super.mouseDeparted(mouseX, mouseY, state);
    }

    @Override
    public void mouseEntered(final int mouseX, final int mouseY, final InputState state) {
        getCurrentState().mouseEntered(mouseX, mouseY, state);
        super.mouseEntered(mouseX, mouseY, state);
    }

    @Override
    public boolean mouseMoved(final int mouseX, final int mouseY, final InputState state) {
        return getCurrentState().mouseMoved(mouseX, mouseY, state) || super.mouseMoved(mouseX, mouseY, state);
    }

    @Override
    public boolean mouseClicked(final MouseButton button, final InputState state) {
        return getCurrentState().mouseClicked(button, state) || super.mouseClicked(button, state);
    }

    @Override
    public boolean mousePressed(final MouseButton button, final InputState state) {
        return getCurrentState().mousePressed(button, state) || super.mousePressed(button, state);
    }

    @Override
    public boolean mouseReleased(final MouseButton button, final InputState state) {
        return getCurrentState().mouseReleased(button, state) || super.mouseReleased(button, state);
    }

    @Override
    public boolean mouseWheel(final int wheelDx, final InputState state) {
        return getCurrentState().mouseWheel(wheelDx, state) || super.mouseWheel(wheelDx, state);
    }

    @Override
    public boolean keyPressed(final Key key, final InputState state) {
        return getCurrentState().keyPressed(key, state) || super.keyPressed(key, state);
    }

    @Override
    public boolean keyReleased(final Key key, final InputState state) {
        return getCurrentState().keyReleased(key, state) || super.keyReleased(key, state);
    }

    @Override
    public boolean keyHeld(final Key key, final InputState state) {
        return getCurrentState().keyHeld(key, state) || super.keyHeld(key, state);
    }

    @Override
    public void gainedFocus() {
        getCurrentState().gainedFocus();
        super.gainedFocus();
    }

    @Override
    public void lostFocus() {
        getCurrentState().lostFocus();
        super.lostFocus();
    }

    /**
     * @return an ImmutableSet of possible states for this component.
     */
    public abstract ImmutableSet<UIState> getStates();
}
