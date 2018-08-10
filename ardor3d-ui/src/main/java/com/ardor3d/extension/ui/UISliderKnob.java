/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.ui;

import com.ardor3d.extension.ui.event.DragListener;
import com.ardor3d.extension.ui.model.SliderModel;
import com.ardor3d.input.InputState;
import com.ardor3d.input.MouseButton;
import com.ardor3d.math.MathUtils;

/**
 * Defines the knob (aka slider or scrubber) on a slider. Generally created by UISlider directly.
 */
public class UISliderKnob extends UIContainer {

    /** The current relative position of the knob handle, as a percent. */
    protected float _position = 0;

    /** The handle for our knob. Generally decorated with an icon. */
    private UILabel _knobLabel = null;

    /** The slider we are part of. */
    private UISlider _parentSlider = null;

    /** A reusable drag listener, added to the hud when we are attached. */
    private final DragListener _dragListener = new KnobDragListener();

    /**
     * Construct a new knob for the given slider.
     *
     * @param slider
     *            the parent slider.
     */
    UISliderKnob(final UISlider slider) {
        super();

        // set slider
        _parentSlider = slider;

        // create and attach our knob handle
        _knobLabel = new UILabel("");
        _knobLabel.getSceneHints().setAllPickingHints(true);
        attachChild(_knobLabel);
    }

    /**
     * @return the UILabel that represents the handle of the knob
     */
    public UILabel getKnobLabel() {
        return _knobLabel;
    }

    /**
     * @return the current position (as a percent) of the knob handle [0.0, 1.0]
     */
    public float getPosition() {
        return _position;
    }

    /**
     * Sets the current position of the knob handle
     *
     * @param newPosition
     *            the new position as a percent [0.0, 1.0]
     */
    public void setPosition(final float newPosition) {
        // ensure we are in the valid range.
        assert newPosition >= 0 && newPosition <= 1 : "newPosition must be in [0f, 1f]";
        _position = newPosition;

        // Set our handles relative location using the position, our size and the size of the handle
        if (_parentSlider.getOrientation() == Orientation.Horizontal) {
            _knobLabel.setLocalX((int) ((getContentWidth() - _knobLabel.getLocalComponentWidth()) * _position));
            _knobLabel.setLocalY((getContentHeight() - _knobLabel.getLocalComponentHeight()) / 2);
        } else {
            _knobLabel.setLocalX((getContentWidth() - _knobLabel.getLocalComponentWidth()) / 2);
            _knobLabel.setLocalY((int) ((getContentHeight() - _knobLabel.getLocalComponentHeight()) * _position));
        }

        // let the ui system know we've changed something.
        fireComponentDirty();
    }

    @Override
    public void layout() {
        ; // do nothing
    }

    /**
     * @return the relative location (x or y) of the leading edge of the knob handle. An x value if the we are
     *         horizontal or a y value if we are vertical. This is used for various mouse interactions.
     */
    protected int getSliderFrontEdge() {
        if (_parentSlider.getOrientation() == Orientation.Horizontal) {
            final int size = getLocalComponentWidth() - _knobLabel.getLocalComponentWidth();
            return Math.round(_position * size * getWorldScale().getXf());
        } else {
            final int size = getLocalComponentHeight() - _knobLabel.getLocalComponentHeight();
            return Math.round(_position * size * getWorldScale().getYf());
        }
    }

    @Override
    public void attachedToHud() {
        super.attachedToHud();

        // Add our drag handler to the hud
        getHud().addDragListener(_dragListener);
    }

    @Override
    public void detachedFromHud() {
        super.detachedFromHud();

        // Remove our drag listener from the hud
        if (getHud() != null) {
            getHud().removeDragListener(_dragListener);
        }
    }

    @Override
    public void setEnabled(final boolean enabled) {
        super.setEnabled(enabled);

        // set knob handle enabled / disabled
        _knobLabel.setEnabled(enabled);
    }

    /**
     * Enable clicking in slider to step the knob one way or the other.
     */
    @Override
    public boolean mousePressed(final MouseButton button, final InputState state) {
        // XXX: perhaps we should use a variable "step size"?
        // XXX: perhaps allow repeating steps until mouse up?

        // skip out if not enabled
        if (!UISliderKnob.this.isEnabled()) {
            return false;
        }

        // Get mouse location
        final int mouseX = state.getMouseState().getX();
        final int mouseY = state.getMouseState().getY();
        if (!_knobLabel.insideMargin(mouseX, mouseY)) {
            if (_parentSlider.getOrientation() == Orientation.Horizontal) {
                final int x = mouseX - UISliderKnob.this.getHudX();
                // check which side we are on and step
                if (x < getSliderFrontEdge()) {
                    _parentSlider.setValue(_parentSlider.getValue() - 1);
                } else {
                    _parentSlider.setValue(_parentSlider.getValue() + 1);
                }
            } else {
                final int y = mouseY - UISliderKnob.this.getHudY();

                // check which side we are on and step
                if (y < getSliderFrontEdge()) {
                    _parentSlider.setValue(_parentSlider.getValue() - 1);
                } else {
                    _parentSlider.setValue(_parentSlider.getValue() + 1);
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Our knob handle drag listener class
     */
    private class KnobDragListener implements DragListener {
        private int _initialHudLoc;
        private int _delta;

        public void startDrag(final int x, final int y) {
            // skip out if not enabled.
            if (!isEnabled()) {
                return;
            }

            // remember our starting hud location
            if (_parentSlider.getOrientation() == Orientation.Horizontal) {
                _initialHudLoc = getHudX();
            } else {
                _initialHudLoc = getHudY();
            }

            // remember the current delta to the mouse
            if (_parentSlider.getOrientation() == Orientation.Horizontal) {
                _delta = getSliderFrontEdge() - (x - _initialHudLoc);
            } else {
                _delta = getSliderFrontEdge() - (y - _initialHudLoc);
            }
        }

        public void drag(final int x, final int y) {
            if (!isEnabled()) {
                return;
            }

            // calculate our new handle position based on the current mouse position.
            float position;
            if (_parentSlider.getOrientation() == Orientation.Horizontal) {
                position = (x - _initialHudLoc + _delta)
                        / ((getLocalComponentWidth() - _knobLabel.getLocalComponentWidth()) * getWorldScale().getXf());

            } else {
                position = (y - _initialHudLoc + _delta)
                        / ((getLocalComponentHeight() - _knobLabel.getLocalComponentHeight()) * getWorldScale().getYf());
            }

            // clamp to [0, 1] and assign
            position = MathUtils.clamp(position, 0f, 1f);
            setPosition(position);

            // set the value on the slider's model directly to avoid circular looping logic.
            final SliderModel model = _parentSlider.getModel();
            model.setCurrentValue(
                    Math.round(getPosition() * (model.getMaxValue() - model.getMinValue())) + model.getMinValue(),
                    _parentSlider);
        }

        public void endDrag(final UIComponent dropOn, final int x, final int y) {
            // call back to our parent slider, allowing for snapping
            _parentSlider.knobReleased();
        }

        public boolean isDragHandle(final UIComponent w, final int x, final int y) {
            // if not enabled, we don't slide
            if (!isEnabled()) {
                return false;
            }

            // Our slide handle is the label.
            return w.equals(_knobLabel);
        }
    }
}
