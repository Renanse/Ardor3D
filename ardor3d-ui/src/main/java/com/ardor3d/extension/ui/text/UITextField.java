/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.ui.text;

import java.util.ArrayList;
import java.util.List;

import com.ardor3d.extension.ui.UIState;
import com.ardor3d.extension.ui.event.ActionEvent;
import com.ardor3d.extension.ui.event.ActionListener;

public class UITextField extends AbstractUITextEntryComponent {

    protected UIKeyHandler _keyHandler;

    /** List of action listeners notified when the enter key is pressed. */
    private final List<ActionListener> _listeners = new ArrayList<ActionListener>();

    public UITextField() {
        _disabledState = new UIState();
        _defaultState = new DefaultTextEntryState();
        _writingState = new TextEntryWritingState(this);
        setEditable(true);
        applySkin();
        switchState(getDefaultState());
    }

    public UITextField(final String rawText) {
        this();
        setText(rawText);
    }

    @Override
    protected UIKeyHandler getKeyHandler() {
        if (_keyHandler == null) {
            _keyHandler = new DefaultLatinTextEntryKeyHandler(this) {
                @Override
                protected boolean handleEnterKey() {
                    // ACTION
                    fireActionEvent();
                    clearFocus();
                    return true;
                }
            };
        }
        return _keyHandler;
    }

    public void setKeyHandler(final UIKeyHandler handler) {
        _keyHandler = handler;
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
     * Removes all of this field's listeners from notification list.
     */
    public void removeAllListeners() {
        _listeners.clear();
    }

    /**
     * Notifies all of this field's registered listeners
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

}
