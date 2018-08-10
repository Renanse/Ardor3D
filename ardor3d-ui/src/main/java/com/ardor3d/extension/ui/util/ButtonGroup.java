/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.ui.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.ardor3d.extension.ui.UIButton;

/**
 * Defines a group of buttons. This is generally used with radio or toggle buttons to indicate a group wherein only one
 * button should be pressed at a time.
 */
public class ButtonGroup {

    /** The list of buttons participating in this group. */
    private final List<UIButton> _buttons = new ArrayList<UIButton>();

    /** The currently selected button. */
    private UIButton _selected;

    public ButtonGroup() {}

    /**
     * Add a button to this button group if not already present.
     * 
     * @param button
     *            the button to be added
     * @throws NullPointerException
     *             if button is null
     */
    public void add(final UIButton button) {

        // Make sure the button has the right group set.
        if (button.getGroup() != this) {
            button.setGroup(this);
        }

        // Proceed if the button is not already part of the this.
        if (!_buttons.contains(button)) {
            // Add to list
            _buttons.add(button);

            // See if we're selected...
            if (button.isSelected()) {
                // ..if we are and nothing else is selected in this group, then this is our "selected" value.
                if (_selected == null) {
                    _selected = button;
                }
                // ..otherwise, the existing "selected" takes priority, so turn off this button.
                else {
                    button.setSelected(false);
                }
            }
        }
    }

    /**
     * Removes the button from the group.
     * 
     * @param button
     *            the button to remove. no-op if null.
     */
    public void remove(final UIButton button) {
        if (button == null) {
            return;
        }
        _buttons.remove(button);
        if (button == _selected) {
            _selected = null;
        }
        button.setGroup(null);
    }

    /**
     * @return an Iterator of buttons currently in this group
     */
    public Iterator<UIButton> getButtons() {
        return _buttons.iterator();
    }

    /**
     * @return the currently selected button or null if none have been selected yet.
     */
    public UIButton getSelection() {
        return _selected;
    }

    /**
     * Sets the selected value for the given button. Only one button in the group may be selected at a time.
     * 
     * @param button
     *            the button we are changing
     * @param selected
     *            true if this button is to be selected and all others in the group should be de-selected.
     */
    public void setSelected(final UIButton button, final boolean selected) {
        if (selected && button != null && button != _selected) {
            final UIButton oldSelection = _selected;
            _selected = button;
            if (oldSelection != null) {
                oldSelection.setSelected(false);
            }
            button.setSelected(true);
        }
    }

    /**
     * @return true if the button is the currently selected button
     */
    public boolean isSelected(final UIButton button) {
        return button == _selected;
    }

    /**
     * @return the number of buttons currently in the group.
     */
    public int getButtonCount() {
        return _buttons.size();
    }
}
