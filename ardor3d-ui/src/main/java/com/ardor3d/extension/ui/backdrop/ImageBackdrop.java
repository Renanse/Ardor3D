/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.ui.backdrop;

import java.util.Arrays;

import com.ardor3d.extension.ui.UIComponent;
import com.ardor3d.extension.ui.util.Alignment;
import com.ardor3d.extension.ui.util.Insets;
import com.ardor3d.extension.ui.util.SubTex;
import com.ardor3d.extension.ui.util.SubTexUtil;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.Renderer;

/**
 * This backdrop paints a single image on a colored plane behind the component, stretching it and aligning it to the
 * component based on given settings.
 */
public class ImageBackdrop extends SolidBackdrop {

    public enum StretchAxis {
        /** Stretch the image on both axis */
        Both,
        /** Stretch the image on the horizontal axis */
        Horizontal,
        /** Stretch the image on the vertical axis */
        Vertical,
        /** Do not stretch the image */
        None;
    }

    /** The image to draw. */
    protected SubTex _image = null;
    /** The axis to stretch the image on. */
    protected StretchAxis _axis = StretchAxis.Both;
    /** The alignment (to the component) to align the image to. */
    protected Alignment _alignment = Alignment.MIDDLE;

    /** used internally to hold component measurements prior to drawing backdrop */
    protected final double[] _dims = new double[4];

    /** Used to tint the image drawn by this backdrop. */
    private final ColorRGBA _tintColor = new ColorRGBA(1, 1, 1, 1);

    /**
     * Construct this back drop, using the given image.
     *
     * @param image
     */
    public ImageBackdrop(final SubTex image) {
        super(ColorRGBA.BLACK_NO_ALPHA);
        setImage(image);
    }

    /**
     * Construct this back drop, using the given image and color.
     *
     * @param image
     *            the image to draw
     * @param color
     *            the color of the backdrop
     */
    public ImageBackdrop(final SubTex image, final ReadOnlyColorRGBA color) {
        super(color);
        setImage(image);
    }

    @Override
    public void draw(final Renderer renderer, final UIComponent comp) {
        super.draw(renderer, comp);

        getDimensions(comp, _dims);
        double x = _dims[0];
        double y = _dims[1];
        final double width = _dims[2];
        final double height = _dims[3];

        final Insets margin = comp.getMargin() != null ? comp.getMargin() : Insets.EMPTY;
        final Insets border = comp.getBorder() != null ? comp.getBorder() : Insets.EMPTY;

        x += margin.getLeft() + border.getLeft();
        y += margin.getBottom() + border.getBottom();

        SubTexUtil.drawSubTex(renderer, _image, x, y, width, height, comp.getWorldTransform(), getTintColor());
    }

    public void getDimensions(final UIComponent comp, final double[] vals) {

        final double bgwidth = UIBackdrop.getBackdropWidth(comp);
        final double bgheight = UIBackdrop.getBackdropHeight(comp);

        Arrays.fill(vals, 0);
        switch (_axis) {
            case Both:
                vals[2] = bgwidth;
                vals[3] = bgheight;
                break;
            case None:
            case Horizontal:
            case Vertical:
                if (_axis.equals(StretchAxis.Horizontal)) {
                    vals[2] = bgwidth;
                } else {
                    vals[2] = _image.getWidth();
                }

                if (_axis.equals(StretchAxis.Vertical)) {
                    vals[3] = bgheight;
                } else {
                    vals[3] = _image.getHeight();
                }

                if (!_axis.equals(StretchAxis.Horizontal)) {
                    switch (_alignment) {
                        case TOP:
                        case MIDDLE:
                        case BOTTOM:
                            vals[0] = bgwidth / 2 - _image.getWidth() / 2;
                            break;
                        case TOP_RIGHT:
                        case RIGHT:
                        case BOTTOM_RIGHT:
                            vals[0] = bgwidth - _image.getWidth();
                            break;
                        case TOP_LEFT:
                        case LEFT:
                        case BOTTOM_LEFT:
                            vals[0] = 0;
                    }
                }

                if (!_axis.equals(StretchAxis.Vertical)) {
                    switch (_alignment) {
                        case TOP_LEFT:
                        case TOP:
                        case TOP_RIGHT:
                            vals[1] = bgheight - _image.getHeight();
                            break;
                        case LEFT:
                        case MIDDLE:
                        case RIGHT:
                            vals[1] = bgheight / 2 - _image.getHeight() / 2;
                            break;
                        case BOTTOM_LEFT:
                        case BOTTOM:
                        case BOTTOM_RIGHT:
                            vals[1] = 0;
                    }
                }
        }
    }

    public SubTex getImage() {
        return _image;
    }

    public void setImage(final SubTex image) {
        _image = image;
    }

    public StretchAxis getStretch() {
        return _axis;
    }

    public void setStretch(final StretchAxis axis) {
        _axis = axis;
    }

    public Alignment getAlignment() {
        return _alignment;
    }

    public void setAlignment(final Alignment alignment) {
        _alignment = alignment;
    }

    public ReadOnlyColorRGBA getTintColor() {
        return _tintColor;
    }

    /**
     * Sets a color to use for tinting the backdrop.
     *
     * @param color
     */
    public void setTintColor(final ReadOnlyColorRGBA color) {
        _tintColor.set(color);
    }
}
