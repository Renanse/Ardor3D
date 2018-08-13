/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input.logical;

import com.ardor3d.annotation.Immutable;
import com.ardor3d.input.MouseButton;
import com.ardor3d.input.MouseState;
import com.google.common.base.Predicate;

/**
 * A condition that is true if a given button was clicked (has a click count) when going from the previous input state
 * to the current one.
 */
@Immutable
public final class MouseButtonClickedCondition implements Predicate<TwoInputStates> {
    private final MouseButton _button;

    /**
     * Construct a new MouseButtonClickedCondition.
     *
     * @param button
     *            the button that should be "clicked" to trigger this condition
     * @throws NullPointerException
     *             if the button is null
     */
    public MouseButtonClickedCondition(final MouseButton button) {
        if (button == null) {
            throw new NullPointerException();
        }

        _button = button;
    }

    public boolean apply(final TwoInputStates states) {
        final MouseState currentState = states.getCurrent().getMouseState();
        final MouseState previousState = states.getPrevious().getMouseState();

        return !currentState.getButtonsReleasedSince(previousState).isEmpty()
                && currentState.getButtonsClicked().contains(_button);
    }
}
