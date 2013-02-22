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

import java.util.ArrayList;
import java.util.List;

import com.ardor3d.extension.ui.event.ActionEvent;
import com.ardor3d.extension.ui.event.ActionListener;
import com.ardor3d.extension.ui.util.ButtonGroup;
import com.ardor3d.extension.ui.util.SubTex;
import com.ardor3d.input.InputState;
import com.ardor3d.input.MouseButton;
import com.google.common.collect.ImmutableSet;

/**
 * A state based component that can be interacted with via the mouse to trigger actions.
 */
public class UIButton extends AbstractLabelUIComponent {

    /** Default state to use for the button. */
    protected LabelState _defaultState = new DefaultState();
    /** State to use when the mouse button is disabled. */
    protected LabelState _disabledState = new LabelState();

    /** State to use when the mouse button is pressed and held down over this button. */
    protected LabelState _pressedState = new PressedState();

    /** State to use when the mouse is hovering over this button. */
    protected LabelState _mouseOverState = new MouseOverState();

    /** State to use when the mouse button is currently selected. */
    protected LabelState _selectedState = new SelectedState();
    /** State to use when the mouse is hovering over this button while currently selected. */
    protected LabelState _mouseOverSelectedState = new MouseOverSelectedState();
    /** State to use when the mouse button is disabled while currently selected. */
    protected LabelState _disabledSelectedState = new LabelState();

    /** List of action listeners notified when this button is pressed. */
    private final List<ActionListener> _listeners = new ArrayList<ActionListener>();

    /** True if this is button is selectable/toggleable. */
    private boolean _selectable = false;
    /** True if this is button is currently selected. */
    private boolean _selected = false;

    /** ButtonGroup this button belongs to. */
    private ButtonGroup _group = null;

    /** Configurable action command */
    private String _actionCommand = null;

    /**
     * Construct a new, blank button
     */
    public UIButton() {
        this("");
    }

    /**
     * Construct a new button with the given text.
     * 
     * @param text
     */
    public UIButton(final String text) {
        this(text, null);
    }

    /**
     * Construct a new button with the given text and icon.
     * 
     * @param text
     * @param icon
     */
    public UIButton(final String text, final SubTex icon) {
        setConsumeMouseEvents(true);
        setIcon(icon);

        applySkin();
        setButtonText(text);

        switchState(getDefaultState());
    }

    /**
     * @return true if the pressed state is our current state.
     */
    public boolean isPressed() {
        return getCurrentState().equals(getPressedState());
    }

    /**
     * @return true if the mouse over state is our current state.
     */
    public boolean isMouseOver() {
        return getCurrentState().equals(getMouseOverState());
    }

    /**
     * Add the specified listener to this button's list of listeners notified when pressed.
     * 
     * @param listener
     */
    public void addActionListener(final ActionListener listener) {
        _listeners.add(listener);
    }

    /**
     * Remove the given listener from the notification list.
     * 
     * @param listener
     */
    public boolean removeActionListener(final ActionListener listener) {
        return _listeners.remove(listener);
    }

    /**
     * Removes all of this button's listeners from notification list.
     */
    public void removeAllListeners() {
        _listeners.clear();
    }

    /**
     * Notifies all of this button's registered listeners that this button was pressed.
     */
    public void fireActionEvent() {
        if (!isEnabled()) {
            return;
        }
        final ActionEvent event = new ActionEvent(this);
        for (final ActionListener l : _listeners) {
            l.actionPerformed(event);
        }
    }

    public LabelState getMouseOverState() {
        return _mouseOverState;
    }

    public LabelState getMouseOverSelectedState() {
        return _mouseOverSelectedState;
    }

    public LabelState getPressedState() {
        return _pressedState;
    }

    public LabelState getSelectedState() {
        return _selectedState;
    }

    public LabelState getDisabledSelectedState() {
        return _disabledSelectedState;
    }

    /**
     * always mark consumed.
     */
    @Override
    public boolean mousePressed(final MouseButton button, final InputState state) {
        // always consume
        super.mousePressed(button, state);
        return true;
    }

    /**
     * always mark consumed.
     */
    @Override
    public boolean mouseReleased(final MouseButton button, final InputState state) {
        // always consume
        super.mouseReleased(button, state);
        return true;
    }

    /**
     * @return true if button is selectable and is selected.
     */
    public boolean isSelected() {
        if (!_selectable) {
            return false;
        }
        return _selected;
    }

    /**
     * If selectable, set this button's state to either selected (true) or default (false) - or disabled versions of
     * each if currently disabled.
     * 
     * @param selected
     */
    public void setSelected(boolean selected) {
        if (!_selectable) {
            return;
        }

        if (_group != null) {
            _group.setSelected(this, selected);
            selected = _group.isSelected(this);
        }

        if (isSelected() == selected) {
            return;
        }

        _selected = selected;

        if (_selected) {
            if (getCurrentState() instanceof PressedState) {
                switchState(getMouseOverSelectedState());
            } else {
                switchState(isEnabled() ? getSelectedState() : getDisabledSelectedState());
            }
        } else {
            if (getCurrentState() instanceof MouseOverSelectedState) {
                switchState(getMouseOverState());
            } else {
                switchState(isEnabled() ? getDefaultState() : getDisabledState());
            }
        }
    }

    /**
     * @param selectable
     *            true if this button should be selectable
     */
    public void setSelectable(final boolean selectable) {
        if (isSelectable() && !selectable && isSelected()) {
            setSelected(false);
        }
        _selectable = selectable;
    }

    /**
     * @return true if button is selectable
     */
    public boolean isSelectable() {
        return _selectable;
    }

    /**
     * @param group
     *            the group this button should belong to. Removes the button from any group it currently belongs to.
     */
    public void setGroup(final ButtonGroup group) {
        final ButtonGroup oldGroup = _group;
        _group = group;

        if (oldGroup != null) {
            oldGroup.remove(this);
        }

        _group.add(this);
        if (isSelected()) {
            _group.setSelected(this, isSelected());
        }
    }

    /**
     * @return the button group this button belongs to or null if it belongs to none.
     */
    public ButtonGroup getGroup() {
        return _group;
    }

    /**
     * @param enabled
     *            if true, sets the defaultState to active, otherwise sets the disabledState or disabledSelectedState to
     *            active.
     */
    @Override
    public void setEnabled(final boolean enabled) {
        super.setEnabled(enabled);
        if (isSelectable() && isSelected()) {
            switchState(enabled ? getSelectedState() : getDisabledSelectedState());
        }
    }

    @Override
    public LabelState getDefaultState() {
        return _defaultState;
    }

    @Override
    public LabelState getDisabledState() {
        return _disabledState;
    }

    /**
     * Sets the text on this button and all contained states.
     * 
     * @param text
     *            the new text
     */
    public void setButtonText(final String text) {
        super.setText(text);
        for (final UIState state : getStates()) {
            if (state instanceof LabelState) {
                ((LabelState) state).setText(text);
            }
        }
    }

    /**
     * Sets the text on this button and all contained states.
     * 
     * @param text
     *            the new text
     * @param isStyled
     *            if true, the text may contain style markup.
     */
    public void setButtonText(final String text, final boolean isStyled) {
        super.setStyledText(isStyled);
        super.setText(text);
        for (final UIState state : getStates()) {
            if (state instanceof LabelState) {
                ((LabelState) state).setText(text);
                ((LabelState) state).setStyledText(isStyled);
            }
        }
    }

    /**
     * Sets the icon on this button and all contained states.
     * 
     * @param icon
     *            the new icon
     */
    public void setButtonIcon(final SubTex icon) {
        super.setIcon(icon);
        for (final UIState state : getStates()) {
            if (state instanceof LabelState) {
                ((LabelState) state).setIcon(icon);
            }
        }
    }

    public String getActionCommand() {
        if (_actionCommand == null) {
            return getText();
        } else {
            return _actionCommand;
        }
    }

    public void setActionCommand(final String actionCommand) {
        _actionCommand = actionCommand;
    }

    /**
     * Manually click the button.
     */
    public void doClick() {
        _pressedState.mouseReleased(MouseButton.LEFT, null);
    }

    // Button UI states that handles switching to other states based on mouse events.

    class DefaultState extends LabelState {

        @Override
        public void mouseEntered(final int mouseX, final int mouseY, final InputState state) {
            switchState(getMouseOverState());
        }

        @Override
        public boolean mousePressed(final MouseButton button, final InputState state) {
            switchState(getPressedState());
            return true;
        }
    }

    class MouseOverState extends LabelState {

        @Override
        public void mouseDeparted(final int mouseX, final int mouseY, final InputState state) {
            switchState(getDefaultState());
        }

        @Override
        public boolean mousePressed(final MouseButton button, final InputState state) {
            switchState(getPressedState());
            return true;
        }
    }

    class PressedState extends LabelState {

        @Override
        public void mouseDeparted(final int mouseX, final int mouseY, final InputState state) {
            switchState(getDefaultState());
        }

        @Override
        public boolean mouseReleased(final MouseButton button, final InputState state) {
            if (_selectable) {
                setSelected(!isSelected());
            } else {
                switchState(getMouseOverState());
            }
            fireActionEvent();
            return true;
        }
    }

    class SelectedState extends LabelState {

        @Override
        public void mouseEntered(final int mouseX, final int mouseY, final InputState state) {
            switchState(getMouseOverSelectedState());
        }

        @Override
        public boolean mouseReleased(final MouseButton button, final InputState state) {
            setSelected(!isSelected());
            fireActionEvent();
            return true;
        }
    }

    class MouseOverSelectedState extends LabelState {

        @Override
        public void mouseDeparted(final int mouseX, final int mouseY, final InputState state) {
            switchState(getSelectedState());
        }

        @Override
        public boolean mouseReleased(final MouseButton button, final InputState state) {
            setSelected(!isSelected());
            fireActionEvent();
            return true;
        }
    }

    @Override
    public ImmutableSet<UIState> getStates() {
        return ImmutableSet.of((UIState) _defaultState, _disabledState, _pressedState, _selectedState,
                _disabledSelectedState, _mouseOverState, _mouseOverSelectedState);
    }
}
