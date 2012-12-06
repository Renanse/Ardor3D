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

import java.util.List;

import com.ardor3d.extension.ui.event.ActionEvent;
import com.ardor3d.extension.ui.event.ActionListener;
import com.ardor3d.extension.ui.model.DefaultSliderModel;
import com.ardor3d.extension.ui.model.SliderModel;
import com.google.common.collect.Lists;

/**
 * A widget allowing display and control of a choice from a range of values.
 */
public class UISlider extends UIContainer {

    /** Our data model */
    private SliderModel _model;

    /** The panel or decoration in the back of the slider. */
    private final UIPanel _backPanel = new UIPanel();

    /** The knob used to display and control the value of this slider. */
    private final UISliderKnob _knob;

    /** List of action listeners notified when this slider is changed. */
    private final List<ActionListener> _listeners = Lists.newArrayList();

    /** The orientation of this slider knob. */
    private final Orientation _orientation;

    /** Do we snap to the integer values? */
    private boolean _snapToValues = true;

    /**
     * create a slider widget with a default range of [0,100]. Initial value is 50.
     * 
     * @param orientation
     *            the orientation of the slider (Orientation.Horizontal or Orientation.Vertical)
     */
    public UISlider(final Orientation orientation) {
        this(orientation, 0, 100, 50);
    }

    /**
     * create a slider widget with a default range of [minValue,maxOffset] and the given initialValue.
     * 
     * @param orientation
     *            the orientation of the slider (Orientation.Horizontal or Orientation.Vertical)
     * @param minValue
     *            the minimum value the slider can take (inclusive).
     * @param maxValue
     *            the maximum value the slider can take (inclusive). Must be greater than or equal to minValue.
     * @param initialValue
     *            the starting value of the slider. Must be between min and max values.
     */
    public UISlider(final Orientation orientation, final int minValue, final int maxValue, final int initialValue) {
        assert orientation != null : "orientation must not be null.";
        assert minValue <= maxValue : "minValue must be less than maxValue.";
        assert minValue <= initialValue && initialValue <= maxValue : "initialValue must be between minValue and maxValue.";

        // Set our orientation
        _orientation = orientation;

        // Add our back panel
        attachChild(_backPanel);

        // Create a default data model
        _model = new DefaultSliderModel(minValue, maxValue);

        // set up our knob and attach it.
        _knob = new UISliderKnob(this);
        attachChild(_knob);

        // Set our initial value
        setValue(initialValue);

        // Apply our skin.
        applySkin();
    }

    /**
     * Notifies any listeners that this slider has updated its value.
     */
    public void fireChangeEvent() {
        if (!isEnabled()) {
            return;
        }

        final ActionEvent event = new ActionEvent(this);
        for (final ActionListener l : _listeners) {
            l.actionPerformed(event);
        }
    }

    /**
     * @return our orientation.
     */
    public Orientation getOrientation() {
        return _orientation;
    }

    @Override
    public void layout() {
        // Keep the knob sized to our content area. This lets the knob control its handle placement.
        _knob.setLocalComponentSize(getContentWidth(), getContentHeight());

        // Update the knob's relative position, based on the slider's range and value.
        updateKnob();

        // Set the backing panel's position and size based on orientation. We'll center it on the perpendicular axis.
        if (getOrientation() == Orientation.Horizontal) {
            _backPanel.setLocalComponentSize(getContentWidth(), _knob.getMinimumLocalComponentHeight());
            _backPanel.setLocalXY(0, (getContentHeight() - _backPanel.getLocalComponentHeight()) / 2);
        } else {
            _backPanel.setLocalComponentSize(_knob.getMinimumLocalComponentWidth(), getContentHeight());
            _backPanel.setLocalXY((getContentWidth() - _backPanel.getLocalComponentWidth()) / 2, 0);
        }

        // lay out the back panel's contents, if any.
        _backPanel.layout();
    }

    @Override
    public UIComponent getUIComponent(final int x, final int y) {
        // prevent picking when disabled.
        if (!isEnabled()) {
            return null;
        }

        return super.getUIComponent(x, y);
    }

    @Override
    public void updateMinimumSizeFromContents() {
        super.updateMinimumSizeFromContents();

        // Our size may have have changed, so force an update of the knob's position.
        updateKnob();
    }

    /**
     * Set the value on this slider
     * 
     * @param value
     *            the new value. Clamps between min and max values.
     */
    public void setValue(final int value) {
        _model.setCurrentValue(value, this);
        updateKnob();
    }

    /**
     * Update our knob's position.
     */
    private void updateKnob() {
        if ((float) (_model.getMaxValue() - _model.getMinValue()) != 0) {
            _knob.setPosition(_model.getCurrentValue() / (float) (_model.getMaxValue() - _model.getMinValue()));
        } else {
            _knob.setPosition(_model.getMinValue());
        }
    }

    /**
     * @return the current data model's current value.
     */
    public int getValue() {
        return _model.getCurrentValue();
    }

    /**
     * @return the data model for the slider.
     */
    public SliderModel getModel() {
        return _model;
    }

    /**
     * @param model
     *            the new data model for this slider. Must not be null.
     */
    public void setModel(final DefaultSliderModel model) {
        assert model != null : "model can not be null.";
        _model = model;
        updateKnob();
    }

    /**
     * Add the specified listener to this slider's list of listeners notified when it has changed.
     * 
     * @param listener
     *            the listener to add
     */
    public void addActionListener(final ActionListener listener) {
        _listeners.add(listener);
    }

    /**
     * Remove a listener from this slider's list of listeners.
     * 
     * @param listener
     *            the listener to remove
     * @return true if the listener was removed.
     */
    public boolean removeActionListener(final ActionListener listener) {
        return _listeners.remove(listener);
    }

    /**
     * Called by the knob when our knob is released. Snaps to the nearest value.
     */
    void knobReleased() {
        if (_snapToValues) {
            setValue(Math.round(_knob.getPosition() * (_model.getMaxValue() - _model.getMinValue())));
        }
    }

    @Override
    public void setEnabled(final boolean enabled) {
        super.setEnabled(enabled);
        _knob.setEnabled(enabled);
        _backPanel.setEnabled(false);
    }

    /**
     * @return the knob associated with this slider.
     */
    public UISliderKnob getKnob() {
        return _knob;
    }

    /**
     * @return the back panel associated with this slider.
     */
    public UIPanel getBackPanel() {
        return _backPanel;
    }

    /**
     * @param snap
     *            true if we snap the slider to the integer representations on mouse release.
     */
    public void setSnapToValues(final boolean snap) {
        _snapToValues = snap;
    }

    /**
     * @return true if we snap the slider to the integer representations on mouse release.
     */
    public boolean isSnapToValues() {
        return _snapToValues;
    }
}
