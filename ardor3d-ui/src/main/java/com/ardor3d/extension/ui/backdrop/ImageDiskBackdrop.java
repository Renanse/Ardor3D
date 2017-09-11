/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.ui.backdrop;

import com.ardor3d.extension.ui.UIComponent;
import com.ardor3d.extension.ui.UIPieMenu;
import com.ardor3d.extension.ui.util.Insets;
import com.ardor3d.extension.ui.util.SubTex;
import com.ardor3d.extension.ui.util.UIDisk;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Transform;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.TextureState;

public class ImageDiskBackdrop extends SolidDiskBackdrop {

    /** The image to draw. */
    protected SubTex _image = null;

    protected TextureState _texState = new TextureState();

    /** The disk used across all disk backdrops to render with. */
    private static UIDisk _standin = SolidDiskBackdrop.createStandinDisk();

    /**
     * Construct this back drop, using the given image.
     *
     * @param image
     */
    public ImageDiskBackdrop(final SubTex image) {
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
    public ImageDiskBackdrop(final SubTex image, final ReadOnlyColorRGBA color) {
        super(color);
        setImage(image);
    }

    @Override
    public void draw(final Renderer renderer, final UIComponent comp) {
        final float oldA = _color.getAlpha();
        if (oldA == 0) {
            // no need to draw.
            return;
        }

        _color.setAlpha(oldA * UIComponent.getCurrentOpacity());
        ImageDiskBackdrop._standin.setDefaultColor(_color);

        final Vector3 v = Vector3.fetchTempInstance();
        final Insets margin = comp.getMargin() != null ? comp.getMargin() : Insets.EMPTY;
        final Insets border = comp.getBorder() != null ? comp.getBorder() : Insets.EMPTY;
        v.set(margin.getLeft() + border.getLeft(), margin.getBottom() + border.getBottom(), 0);
        v.addLocal(comp.getContentWidth() / 2, comp.getContentHeight() / 2, 0);

        final Transform t = Transform.fetchTempInstance();
        t.set(comp.getWorldTransform());
        t.applyForwardVector(v);
        t.translate(v);
        Vector3.releaseTempInstance(v);

        ImageDiskBackdrop._standin.setWorldTransform(t);
        Transform.releaseTempInstance(t);

        double size = 0;
        if (comp instanceof UIPieMenu) {
            size = ((UIPieMenu) comp).getOuterRadius();
        } else {
            size = Math.max(UIBackdrop.getBackdropWidth(comp), UIBackdrop.getBackdropHeight(comp)) / 2;
        }

        if (_texState.getNumberOfSetTextures() == 0 || _texState.getTexture().getTextureKey() == null
                || !_texState.getTexture().getTextureKey().equals(_image.getTexture().getTextureKey())) {
            _texState.setTexture(_image.getTexture());
            ImageDiskBackdrop._standin.setRenderState(_texState);
            ImageDiskBackdrop._standin.updateWorldRenderStates(false);
        }

        ImageDiskBackdrop._standin.resetGeometry(size, 0, _image);
        ImageDiskBackdrop._standin.render(renderer);

        _color.setAlpha(oldA);
    }

    public SubTex getImage() {
        return _image;
    }

    public void setImage(final SubTex image) {
        _image = image;
    }
}
