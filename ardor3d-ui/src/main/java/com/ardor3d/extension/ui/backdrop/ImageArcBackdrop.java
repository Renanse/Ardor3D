/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.ui.backdrop;

import com.ardor3d.extension.ui.UIComponent;
import com.ardor3d.extension.ui.util.SubTex;
import com.ardor3d.extension.ui.util.UIArc;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.TextureState;

public class ImageArcBackdrop extends SolidArcBackdrop {

    /** The image to draw. */
    protected SubTex _image = null;

    protected TextureState _texState = new TextureState();

    /** The arc used across all arc backdrops to render with. */
    private static UIArc _standin = SolidArcBackdrop.createStandinArc();
    static {
        ImageArcBackdrop._standin.setRenderMaterial("ui/textured/default_color.yaml");
    }

    /**
     * Construct this back drop, using the given image.
     *
     * @param image
     */
    public ImageArcBackdrop(final SubTex image) {
        this(image, ColorRGBA.WHITE);
    }

    /**
     * Construct this back drop, using the given image and color.
     *
     * @param image
     *            the image to draw
     * @param color
     *            the color of the backdrop
     */
    public ImageArcBackdrop(final SubTex image, final ReadOnlyColorRGBA color) {
        super(color);
        setImage(image);
    }

    @Override
    public void draw(final Renderer renderer, final UIComponent comp) {

        if (_texState.getNumberOfSetTextures() == 0 || _texState.getTexture().getTextureKey() == null
                || !_texState.getTexture().getTextureKey().equals(_image.getTexture().getTextureKey())) {
            _texState.setTexture(_image.getTexture());
            ImageArcBackdrop._standin.setRenderState(_texState);
            ImageArcBackdrop._standin.updateWorldRenderStates(false);
        }

        drawBackdrop(ImageArcBackdrop._standin, renderer, comp, _image);
    }

    public SubTex getImage() {
        return _image;
    }

    public void setImage(final SubTex image) {
        _image = image;
    }
}
