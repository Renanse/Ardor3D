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

import com.ardor3d.extension.ui.event.ActionEvent;
import com.ardor3d.extension.ui.event.ActionListener;
import com.ardor3d.extension.ui.layout.BorderLayout;
import com.ardor3d.extension.ui.layout.BorderLayoutData;
import com.ardor3d.math.Rectangle2;
import com.ardor3d.renderer.Renderer;

/**
 * NOTE: Experimental still.
 */
public class UIScrollPanel extends UIPanel {

    private final UIScrollBar verticalScrollBar;
    private final UIScrollBar horizontalScrollBar;
    private UIComponent view;
    private int offsetX;
    private int offsetY;

    /** A store for the clip rectangle. */
    private final Rectangle2 _clipRectangleStore = new Rectangle2();

    public UIScrollPanel() {
        this(null);
    }

    public UIScrollPanel(final UIComponent view) {
        setLayout(new BorderLayout());
        if (view != null) {
            this.view = view;
            view.setLayoutData(BorderLayoutData.CENTER);
            add(view);
        }
        horizontalScrollBar = new UIScrollBar(Orientation.Horizontal);
        horizontalScrollBar.setLayoutData(BorderLayoutData.SOUTH);
        verticalScrollBar = new UIScrollBar(Orientation.Vertical);
        verticalScrollBar.setLayoutData(BorderLayoutData.EAST);
        add(horizontalScrollBar);
        add(verticalScrollBar);
        setDoClip(true);
        horizontalScrollBar.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent event) {
                offsetX = horizontalScrollBar.getValue();
                fireComponentDirty();
                updateScrollBarSliders();
            }
        });
        verticalScrollBar.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent event) {
                offsetY = verticalScrollBar.getValue();
                fireComponentDirty();
                updateScrollBarSliders();
            }
        });
        applySkin();
    }

    @Override
    protected void drawComponent(final Renderer renderer) {
        _clipRectangleStore.set(getHudX() + getTotalLeft(),
                getHudY() + getTotalBottom() + horizontalScrollBar.getContentHeight(), getContentWidth()
                - verticalScrollBar.getContentWidth(),
                getContentHeight() - horizontalScrollBar.getContentHeight());
        renderer.pushClip(_clipRectangleStore);

        // temporary translate the view - this is a hack and there may be a better solution
        final int x = view.getLocalX();
        final int y = view.getLocalY();
        view.setLocalXY(x - offsetX, y - offsetY);
        view.updateWorldTransform(true);
        view.draw(renderer);
        view.setLocalXY(x, y);
        renderer.popClip();
        horizontalScrollBar.onDraw(renderer);
        verticalScrollBar.onDraw(renderer);
    }

    @Override
    public void updateMinimumSizeFromContents() {
        super.updateMinimumSizeFromContents();
        setLayoutMinimumContentSize(verticalScrollBar.getLocalComponentWidth() + 1,
                horizontalScrollBar.getLocalComponentHeight() + 1);
        updateScrollBarSliders();
    }

    private void updateScrollBarSliders() {
        if (view != null) {
            verticalScrollBar.setValue(offsetY);
            verticalScrollBar.setMaxValue(view.getLocalComponentHeight() - getContentHeight()
                    + horizontalScrollBar.getLocalComponentHeight());
            horizontalScrollBar.setValue(offsetX);
            horizontalScrollBar.setMaxValue(view.getLocalComponentWidth() - getContentWidth()
                    + verticalScrollBar.getLocalComponentWidth());
            verticalScrollBar.fireComponentDirty();
            horizontalScrollBar.fireComponentDirty();
        }
    }

    @Override
    public void layout() {
        super.layout();
        updateScrollBarSliders();
    }
}
