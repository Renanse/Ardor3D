/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer;

import java.util.EnumMap;
import java.util.List;

import com.ardor3d.framework.Scene;
import com.ardor3d.image.Texture;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.scenegraph.Spatial;

/**
 * <code>TextureRenderer</code> defines an abstract class that handles rendering a scene to a buffer and copying it to a
 * texture. Creation of this object is usually handled via a TextureRendererFactory. Note that currently, only Texture2D
 * is supported by Pbuffer versions of this class. Texture2D and TextureCubeMap are supported in FBO mode.
 */
public interface TextureRenderer {

    /**
     * <code>getCamera</code> retrieves the camera this renderer is using.
     * 
     * @return the camera this renderer is using.
     */
    Camera getCamera();

    /**
     * 
     * @param scene
     *            the scene to render.
     * @param tex
     *            the Texture to render to. This should be a Texture2D or TextureCubeMap. If the latter, its
     *            currentRTTFace will determine which cube face is drawn to.
     * @param clear
     *            which buffers to clear before rendering, if any.
     * @see Renderer#BUFFER_COLOR et al
     */
    void render(Scene scene, Texture tex, int clear);

    /**
     * NOTE: If more than one texture is given, copy-texture is used regardless of card capabilities to decrease render
     * time.
     * 
     * @param scene
     *            the scene to render.
     * @param texs
     *            a list of Textures to render to. These should be of type Texture2D or TextureCubeMap. If the latter,
     *            its currentRTTFace will determine which cube face is drawn to.
     * @param clear
     *            which buffers to clear before rendering, if any.
     * @see Renderer#BUFFER_COLOR et al
     */
    void render(Scene scene, List<Texture> texs, int clear);

    /**
     * 
     * @param spat
     *            the scene to render.
     * @param tex
     *            the Texture to render to. This should be a Texture2D or TextureCubeMap. If the latter, its
     *            currentRTTFace will determine which cube face is drawn to.
     * @param clear
     *            which buffers to clear before rendering, if any.
     * @see Renderer#BUFFER_COLOR et al
     */
    void render(Spatial spat, Texture tex, int clear);

    /**
     * NOTE: If more than one texture is given, copy-texture is used regardless of card capabilities to decrease render
     * time.
     * 
     * @param spat
     *            the scene to render.
     * @param texs
     *            a list of Textures to render to. These should be of type Texture2D or TextureCubeMap. If the latter,
     *            its currentRTTFace will determine which cube face is drawn to.
     * @param clear
     *            which buffers to clear before rendering, if any.
     * @see Renderer#BUFFER_COLOR et al
     */
    void render(Spatial spat, List<Texture> texs, int clear);

    /**
     * NOTE: If more than one texture is given, copy-texture is used regardless of card capabilities to decrease render
     * time.
     * 
     * @param spats
     *            an array of Spatials to render.
     * @param tex
     *            the Texture to render to. This should be a Texture2D or TextureCubeMap. If the latter, its
     *            currentRTTFace will determine which cube face is drawn to.
     * @param clear
     *            which buffers to clear before rendering, if any.
     * @see Renderer#BUFFER_COLOR et al
     */
    void render(List<? extends Spatial> spats, Texture tex, int clear);

    /**
     * NOTE: If more than one texture is given, copy-texture is used regardless of card capabilities to decrease render
     * time.
     * 
     * @param spats
     *            an array of Spatials to render.
     * @param texs
     *            a list of Textures to render to. These should be of type Texture2D or TextureCubeMap. If the latter,
     *            its currentRTTFace will determine which cube face is drawn to.
     * @param clear
     *            which buffers to clear before rendering, if any.
     * @see Renderer#BUFFER_COLOR et al
     */
    void render(List<? extends Spatial> spats, List<Texture> texs, int clear);

    /**
     * <code>setBackgroundColor</code> sets the color of window. This color will be shown for any pixel that is not set
     * via typical rendering operations.
     * 
     * @param c
     *            the color to set the background to.
     */
    void setBackgroundColor(ReadOnlyColorRGBA c);

    /**
     * <code>getBackgroundColor</code> retrieves the color used for the window background.
     * 
     * @return the background color that is currently set to the background.
     */
    ReadOnlyColorRGBA getBackgroundColor();

    /**
     * <code>setupTexture</code> initializes a Texture object for use with TextureRenderer. Generates a valid gl texture
     * id for this texture and sets up data storage for it. The texture will be equal to the texture renderer's size.
     * 
     * Note that the texture renderer's size is not necessarily what is specified in the constructor.
     * 
     * @param tex
     *            The texture to setup for use in Texture Rendering. This should be of type Texture2D or TextureCubeMap.
     */
    void setupTexture(Texture tex);

    /**
     * <code>copyToTexture</code> copies the current frame buffer contents to the given Texture. What is copied is based
     * on the rttFormat of the texture object when it was setup. Note that the contents are copied with no scaling
     * applied, so the texture must be big enough such that xoffset + width <= texture's width and yoffset + height <=
     * texture's height.
     * 
     * @param tex
     *            The Texture to copy into. This should be a Texture2D or TextureCubeMap. If the latter, its
     *            currentRTTFace will determine which cube face is drawn to.
     * @param x
     *            the x offset into the framebuffer
     * @param y
     *            the y offset into the framebuffer
     * @param width
     *            the width of the rectangle to read from the framebuffer and copy 1:1 to the texture
     * @param height
     *            the width of the rectangle to read from the framebuffer and copy 1:1 to the texture
     * @param xoffset
     *            the x offset into the texture to draw at
     * @param yoffset
     *            the y offset into the texture to draw at
     */
    void copyToTexture(Texture tex, int x, int y, int width, int height, int xoffset, int yoffset);

    /**
     * Any wrapping up and cleaning up of TextureRenderer information is performed here.
     */
    void cleanup();

    /**
     * Set up this textureRenderer for use with multiple targets. If you are going to use this texture renderer to
     * render to more than one texture, call this with true.
     * 
     * @param multi
     *            true if you plan to use this texture renderer to render different content to more than one texture.
     */
    void setMultipleTargets(boolean multi);

    int getWidth();

    int getHeight();

    /**
     * Enforce a particular state whenever this texture renderer is used. In other words, the given state will override
     * any state of the same type set on a scene object rendered with this texture renderer.
     * 
     * @param state
     *            state to enforce
     */
    void enforceState(RenderState state);

    void enforceStates(EnumMap<StateType, RenderState> states);

    /**
     * @param type
     *            state type to clear
     */
    void clearEnforcedState(StateType type);

    /**
     * Clear all enforced states on this texture renderer.
     */
    void clearEnforcedStates();
}
