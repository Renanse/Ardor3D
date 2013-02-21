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

import com.ardor3d.extension.ui.text.RenderedText;
import com.ardor3d.extension.ui.text.TextFactory;
import com.ardor3d.extension.ui.util.Alignment;
import com.ardor3d.extension.ui.util.Dimension;
import com.ardor3d.extension.ui.util.SubTex;
import com.ardor3d.extension.ui.util.SubTexUtil;
import com.ardor3d.math.Transform;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Renderer;

/**
 * A state component containing a text label and an icon. These are separated by an optional gap and can also be given a
 * specific alignment. By default, the text is aligned LEFT and has no icon or gap.
 */
public abstract class AbstractLabelUIComponent extends StateBasedUIComponent implements Textable {

    /** Distance between text and icon if both are present. */
    private int _gap = 0;

    /** Alignment value to use to position the icon/text within the overall dimensions of this component. */
    private Alignment _alignment = Alignment.LEFT;

    /** The icon the draw on this icon. */
    private SubTex _icon = null;

    /** The size to draw our icon at. */
    private final Dimension _iconDimensions = new Dimension();

    /** The text object to use for drawing label text. */
    private RenderedText _uiText;

    /** If true, our text could be marked up with style information. */
    protected boolean _styled = false;

    @Override
    public void updateMinimumSizeFromContents() {
        int width = 0;
        int height = 0;

        final String textVal = getText();
        if (textVal != null && textVal.length() > 0) {
            width += Math.round(_uiText.getWidth());
            height += Math.round(_uiText.getHeight());
        }

        if (_iconDimensions != null) {
            width += _iconDimensions.getWidth();
            if (textVal != null && textVal.length() > 0) {
                width += _gap;
            }

            height = Math.max(_iconDimensions.getHeight(), height);
        }

        setMinimumContentSize(width, height);
        if (getContentWidth() < width) {
            setContentWidth(width);
        }
        if (getContentHeight() < height) {
            setContentHeight(height);
        }
        fireComponentDirty();
    }

    /**
     * @return the currently set text value of this label.
     */
    public String getText() {
        return _uiText != null ? _uiText.getText() : null;
    }

    /**
     * Set the text for this component. Also updates the minimum size of the component.
     * 
     * @param text
     *            the new text
     */
    public void setText(String text) {
        if (text != null && text.length() == 0) {
            text = null;
        }

        if (text != null) {
            _uiText = TextFactory.INSTANCE.generateText(text, isStyledText(), getFontStyles(), _uiText, -1);
        } else {
            _uiText = null;
        }

        updateMinimumSizeFromContents();
    }

    public boolean isStyledText() {
        return _styled;
    }

    public void setStyledText(final boolean value) {
        _styled = value;
        fireStyleChanged();
        fireComponentDirty();
    }

    @Override
    protected void updateChildren(final double time) {
        super.updateChildren(time);
        if (_uiText != null) {
            _uiText.updateGeometricState(time);
        }
    }

    public Alignment getAlignment() {
        return _alignment;
    }

    public void setAlignment(final Alignment alignment) {
        _alignment = alignment;
    }

    public int getGap() {
        return _gap;
    }

    /**
     * Note: Also updates the minimum size of the component.
     * 
     * @param gap
     *            the size of the gap, in pixels, between the text and the label text. This is only used if both icon
     *            and text are set.
     */
    public void setGap(final int gap) {
        _gap = gap;
        updateMinimumSizeFromContents();
    }

    public SubTex getIcon() {
        return _icon;
    }

    /**
     * Note: Also updates the minimum size of the component.
     * 
     * @param icon
     *            the new icon for this label.
     */
    public void setIcon(final SubTex icon) {
        _icon = icon;
        if (icon != null && _iconDimensions.getHeight() == 0 && _iconDimensions.getWidth() == 0) {
            updateIconDimensionsFromIcon();
        }
        updateMinimumSizeFromContents();
    }

    /**
     * Set the icon dimensions from the currently set icon. If no icon is set, the dimensions are set to 0x0.
     */
    public void updateIconDimensionsFromIcon() {
        if (_icon != null) {
            _iconDimensions.set(_icon.getWidth(), _icon.getHeight());
        } else {
            _iconDimensions.set(0, 0);
        }
        updateMinimumSizeFromContents();
    }

    /**
     * Overrides any currently set icon size. Call this after setting the icon to prevent overriding.
     * 
     * @param dimensions
     *            a new icon size.
     */
    public void setIconDimensions(final Dimension dimensions) {
        _iconDimensions.set(dimensions);
        updateMinimumSizeFromContents();
    }

    public Dimension getIconDimensions() {
        return _iconDimensions;
    }

    @Override
    public void fireStyleChanged() {
        super.fireStyleChanged();
        setText(getText());
    }

    @Override
    protected void drawComponent(final Renderer renderer) {

        double x = 0;
        double y = 0;
        int width = 0;
        final boolean hasTextObject = getText() != null;

        // Gather our width... check for icon and text and gap.
        if (_icon != null) {
            width = _iconDimensions.getWidth();
            if (hasTextObject && getText().length() > 0) {
                width += _gap;
            }
        } else if (!hasTextObject) {
            // no text OR icon, so no content to render.
            return;
        }

        if (hasTextObject) {
            width += Math.round(_uiText.getWidth());
        }

        // find left most x location of content (icon+text) based on alignment.
        x = _alignment.alignX(getContentWidth(), width);

        if (_icon != null) {
            // find bottom most y location of icon based on alignment.
            // TODO: recheck for proper vertical alignment with new text impl
            // if (hasTextObject && _uiText.getHeight() > _iconDimensions.getHeight()) {
            // final int trailing = _uiText.getFont().getLineHeight() - _uiText.getFont().getBaseHeight();
            // y = _alignment.alignY(getContentHeight() - trailing, _iconDimensions.getHeight()) + trailing - 1;
            // } else {
            y = _alignment.alignY(getContentHeight(), _iconDimensions.getHeight());
            // }

            final double dix = getTotalLeft();
            final double diy = getTotalBottom();
            // draw icon
            SubTexUtil.drawSubTex(renderer, _icon, dix + x, diy + y, _iconDimensions.getWidth(), _iconDimensions
                    .getHeight(), getWorldTransform());
            // shift X over by width of icon and gap
            x += _iconDimensions.getWidth() + _gap;
        }

        if (hasTextObject) {
            // find bottom most y location of text based on alignment.
            y = _alignment.alignY(getContentHeight(), Math.round(_uiText.getHeight()));

            // set our text location
            final Vector3 v = Vector3.fetchTempInstance();
            // note: we round to get the text pixel aligned... otherwise it can get blurry
            v.set(Math.round(x + getTotalLeft()), Math.round(y + getTotalBottom()), 0);

            final Transform t = Transform.fetchTempInstance();
            t.set(getWorldTransform());
            t.applyForwardVector(v);
            t.translate(v);
            Vector3.releaseTempInstance(v);

            _uiText.setWorldTransform(t);
            Transform.releaseTempInstance(t);

            // TODO: alpha of text...
            _uiText.render(renderer);
        }
    }

    public RenderedText getTextObject() {
        return _uiText;
    }
}
