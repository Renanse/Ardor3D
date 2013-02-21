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
import com.ardor3d.extension.ui.layout.BorderLayout;
import com.ardor3d.extension.ui.layout.BorderLayoutData;

/**
 * NOTE: Experimental still.
 */
public class UIScrollBar extends UIPanel {

    private final Orientation orientation;
    private final UISlider slider;
    private final UIButton btTopLeft;
    private final UIButton btBottomRight;
    private int sliderLength;
    /** List of action listeners notified when this scrollbar is changed. */
    private final List<ActionListener> _listeners = new ArrayList<ActionListener>();

    public UIScrollBar(final Orientation orientation) {
        setLayout(new BorderLayout());
        this.orientation = orientation;
        slider = new UISlider(orientation);
        slider.setLayoutData(BorderLayoutData.CENTER);
        btTopLeft = new UIButton(orientation == Orientation.Vertical ? "^" : "<");
        btBottomRight = new UIButton(orientation == Orientation.Vertical ? "v" : ">");
        add(btTopLeft);
        add(btBottomRight);
        add(slider);
        if (orientation == Orientation.Vertical) {
            btTopLeft.setLayoutData(BorderLayoutData.NORTH);
            btBottomRight.setLayoutData(BorderLayoutData.SOUTH);
        } else {
            btTopLeft.setLayoutData(BorderLayoutData.WEST);
            btBottomRight.setLayoutData(BorderLayoutData.EAST);
        }
        applySkin();

        updateMinimumSizeFromContents();
        compact();

        layout();
        final ActionListener al = new ActionListener() {
            public void actionPerformed(final ActionEvent event) {
                int direction;
                if (event.getSource() == btTopLeft) {
                    direction = UIScrollBar.this.orientation == Orientation.Horizontal ? -1 : 1;
                } else {
                    direction = UIScrollBar.this.orientation == Orientation.Horizontal ? 1 : -1;
                }
                int offset = slider.getValue();
                if (direction < 0) {
                    offset -= 10;
                    if (offset < 0) {
                        offset = 0;
                    }
                } else {
                    offset += 10;
                    if (offset > slider.getModel().getMaxValue()) {
                        offset = slider.getModel().getMaxValue();
                    }
                }
                slider.setValue(offset);
                fireChangeEvent();
            }
        };
        btTopLeft.addActionListener(al);
        btBottomRight.addActionListener(al);
        slider.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent event) {
                fireChangeEvent();
            }
        });
    }

    private void fireChangeEvent() {
        final ActionEvent event = new ActionEvent(this);
        for (final ActionListener l : _listeners) {
            l.actionPerformed(event);
        }
    }

    /**
     * Add the specified listener to this scrollbar's list of listeners notified when it's changed.
     * 
     * @param listener
     */
    public void addActionListener(final ActionListener listener) {
        _listeners.add(listener);
    }

    public Orientation getOrientation() {
        return orientation;
    }

    public int getSliderLength() {
        return sliderLength;
    }

    public int getValue() {
        return slider.getValue();
    }

    public void setValue(final int offset) {
        slider.setValue(offset);
        layout();
        fireComponentDirty();
    }

    public void setMaxValue(final int maxOffset) {
        slider.getModel().setMaxValue(maxOffset);
    }

    public UISlider getSlider() {
        return slider;
    }

    public UIButton getBtTopLeft() {
        return btTopLeft;
    }

    public UIButton getBtBottomRight() {
        return btBottomRight;
    }
}
