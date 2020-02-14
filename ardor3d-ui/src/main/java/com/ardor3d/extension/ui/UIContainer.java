/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.ui;

import java.nio.FloatBuffer;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ardor3d.extension.ui.layout.RowLayout;
import com.ardor3d.extension.ui.layout.UILayout;
import com.ardor3d.extension.ui.util.UIQuad;
import com.ardor3d.image.Texture.MagnificationFilter;
import com.ardor3d.image.Texture.MinificationFilter;
import com.ardor3d.image.Texture.WrapMode;
import com.ardor3d.image.Texture2D;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Rectangle2;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.material.uniform.AlphaTestConsts;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.BlendState.DestinationFunction;
import com.ardor3d.renderer.state.BlendState.SourceFunction;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.texture.TextureRenderer;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.scenegraph.hint.LightCombineMode;
import com.ardor3d.scenegraph.hint.PickingHint;
import com.ardor3d.scenegraph.hint.TextureCombineMode;
import com.ardor3d.util.GameTaskQueueManager;

/**
 * Defines a component that can hold and manage other components or containers, using a layout manager to position and
 * potentially resize them.
 */
public abstract class UIContainer extends UIComponent {
    private static final Logger _logger = Logger.getLogger(UIContainer.class.getName());
    public static int STANDIN_TEXTURE_SIZE = 4096;

    /** Layout responsible for managing the size and position of this container's contents. */
    private UILayout _layout = new RowLayout(true);

    /** Toggles whether or not we add our content bounds to the current clip space during draw. */
    private boolean _doClip = true;

    /** Flag indicating whether a current render operation is drawing a "cached" version of a container. */
    private static boolean _drawingStandin = false;

    /**
     * A flag indicating that some part of this container needs repainting. On the next draw call, we should update our
     * cached texture, if using one.
     */
    private boolean _dirty = true;
    /**
     * If true, use a cached texture to display this container (on a simple quad) instead of drawing all of its
     * components.
     */
    private boolean _useStandin = false;

    /** The quad used to draw the cached texture version of this container, if set to use one. */
    private UIQuad _standin = null;
    /** The texture used to store the contents of this container. */
    private Texture2D _fakeTexture;

    // TODO: We likely need to change this TextureRenderer to be context sensitive.
    /** A texture renderer to use for cache operations. */
    protected static TextureRenderer _textureRenderer;

    /** The minification filter to use for standin (if used) */
    private MinificationFilter _minificationFilter = MinificationFilter.BilinearNoMipMaps;

    /** A store for the clip rectangle. */
    private final Rectangle2 _clipRectangleStore = new Rectangle2();

    /**
     * Checks to see if a given UIComponent is in this container.
     *
     * @param component
     *            the component to look for
     * @return true if the given component is in this container.
     */
    public boolean contains(final UIComponent component) {
        return contains(component, false);
    }

    /**
     * Checks to see if a given UIComponent is in this container or (if instructed) its subcontainers.
     *
     * @param component
     *            the component to look for
     * @param recurse
     *            if true, recursively check any sub-containers for the given component.
     * @return if the given component is found
     */
    public boolean contains(final UIComponent component, final boolean recurse) {
        for (int i = getNumberOfChildren(); --i >= 0;) {
            final Spatial child = getChild(i);
            if (child.equals(component)) {
                return true;
            } else if (recurse && component instanceof UIContainer) {
                if (((UIContainer) component).contains(component, true)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Add a component to this container.
     *
     * @param component
     *            the component to add
     */
    public void add(final UIComponent component) {
        attachChild(component);
        // send relation event if we already have a hud
        if (getHud() != null) {
            component.attachedToHud();
        }
        fireStyleChanged();
        fireComponentDirty();
    }

    /**
     * Remove a component from this container.
     *
     * @param component
     *            the component to remove
     */
    public void remove(final UIComponent component) {
        if (getHud() != null) {
            component.detachedFromHud();
        }

        detachChild(component);
    }

    /**
     * Removes all UI components from this container. If other types of Spatials are attached to this container, they
     * are ignored.
     *
     * @param comp
     *            the component to remove
     */
    public void removeAllComponents() {
        for (int i = getNumberOfChildren(); --i >= 0;) {
            final Spatial child = getChild(i);
            if (child instanceof UIComponent) {
                remove((UIComponent) child);
            }
        }
    }

    @Override
    public void detachAllChildren() {
        // Override to make sure ui events are called for detach.
        removeAllComponents();

        // do the rest as normal
        super.detachAllChildren();
    }

    /**
     * @param layout
     *            the new layout to use with this container. If null, no layout is done by this container.
     */
    public void setLayout(final UILayout layout) {
        _layout = layout;
    }

    /**
     * @return the layout currently used by this container or null if no layout is used.
     */
    public UILayout getLayout() {
        return _layout;
    }

    @Override
    public void layout() {
        if (_layout != null) {
            _layout.layoutContents(this);
        }

        // call layout on children
        for (int x = 0, max = getNumberOfChildren(); x < max; x++) {
            final Spatial child = getChild(x);
            if (child instanceof UIComponent) {
                ((UIComponent) child).layout();
            }
        }
    }

    @Override
    public void updateMinimumSizeFromContents() {
        // call update on children first
        for (int x = 0, max = getNumberOfChildren(); x < max; x++) {
            final Spatial child = getChild(x);
            if (child instanceof UIComponent) {
                ((UIComponent) child).updateMinimumSizeFromContents();
            }
        }

        // update our min size.
        if (_layout != null) {
            _layout.updateMinimumSizeFromContents(this);
        }
    }

    @Override
    public void attachedToHud() {
        Spatial child;
        for (int i = 0, cSize = getNumberOfChildren(); i < cSize; i++) {
            child = getChild(i);
            if (child != null) {
                if (child instanceof UIComponent) {
                    ((UIComponent) child).attachedToHud();
                }
            }
        }
    }

    @Override
    public void detachedFromHud() {
        Spatial child;
        for (int i = 0, cSize = getNumberOfChildren(); i < cSize; i++) {
            child = getChild(i);
            if (child != null) {
                if (child instanceof UIComponent) {
                    ((UIComponent) child).detachedFromHud();
                }
            }
        }

        // clean up visuals created for this container
        clearStandin();
    }

    @Override
    public UIComponent getUIComponent(final int hudX, final int hudY) {
        if (!getSceneHints().isPickingHintEnabled(PickingHint.Pickable) || !insideMargin(hudX, hudY)) {
            return null;
        }

        UIComponent ret = null;
        UIComponent found = this;

        for (int i = 0; i < getNumberOfChildren(); i++) {
            final Spatial s = getChild(i);
            if (s instanceof UIComponent) {
                final UIComponent comp = (UIComponent) s;
                ret = comp.getUIComponent(hudX, hudY);

                if (ret != null) {
                    found = ret;
                }
            }
        }

        return found;
    }

    @Override
    protected void drawComponent(final Renderer renderer) {
        if (getNumberOfChildren() > 0) {
            // If asked, clip to just the internal region of this container.
            boolean needsPop = false;
            if (_doClip && getWorldRotation().isIdentity()) {
                _clipRectangleStore.set(getHudX() + getTotalLeft(), getHudY() + getTotalBottom(), getContentWidth(),
                        getContentHeight());
                renderer.getScissorUtils().pushClip(_clipRectangleStore);
                needsPop = true;
            }
            Spatial child;
            for (int i = 0, cSize = getNumberOfChildren(); i < cSize; i++) {
                child = getChild(i);
                if (child != null) {
                    child.onDraw(renderer);
                }
            }
            if (needsPop) {
                renderer.getScissorUtils().popClip();
            }
        }
    }

    @Override
    public synchronized void draw(final Renderer renderer) {

        // if we are not using standins, just draw as a normal Node.
        if (!_useStandin) {
            super.draw(renderer);
            return;
        }

        final int width = getLocalComponentWidth();
        final int height = getLocalComponentHeight();
        final int dispWidth = Camera.getCurrentCamera().getWidth();
        final int dispHeight = Camera.getCurrentCamera().getHeight();

        // If we are currently in the process of rendering this container to a texture...
        if (UIContainer._drawingStandin) {
            getHud().getHudCamera().apply(renderer);

            // hold onto our old translation
            final ReadOnlyVector3 wTrans = getWorldTranslation();
            final double x = wTrans.getX(), y = wTrans.getY(), z = wTrans.getZ();
            final Matrix3 rot = Matrix3.fetchTempInstance().set(getWorldRotation());

            // set our new translation so that we are drawn in the bottom left corner of the texture.
            double newX = 0, newY = 0;
            if (width > dispWidth && x < 0) {
                newX = x;
            }
            if (height > dispHeight && y < 0) {
                newY = y;
            }
            setWorldTranslation(newX, newY, 0);
            setWorldRotation(Matrix3.IDENTITY);
            updateWorldTransform(true, false);

            // draw to texture
            super.draw(renderer);

            // replace our old translation
            setWorldTranslation(x, y, z);
            setWorldRotation(rot);
            updateWorldTransform(true);

            // exit
            return;
        }

        // Calculate our standin's translation (and size) so that we are drawn in the bottom left corner of the texture.
        // Take into account containers that are bigger than the screen.
        int newWidth = width, newHeight = height;
        int x = getHudX();
        int y = getHudY();
        if (width > dispWidth && x < 0) {
            newWidth += getHudX();
            x = 0;
        }
        if (height > dispHeight && y < 0) {
            newHeight += getHudY();
            y = 0;
        }

        // Otherwise we are not rendering to texture and we are using standins...
        // So check if we are dirty.
        if (isDirty() || _standin == null) {
            getHud().getCanvas().getCanvasRenderer().getCamera().apply(renderer);

            // Check if we have a standin yet
            if (_standin == null) {
                try {
                    buildStandin(renderer);
                } catch (final Exception e) {
                    _useStandin = false;
                    UIContainer._logger.warning("Unable to create standin: " + e.getMessage());
                    UIContainer._logger.logp(Level.SEVERE, getClass().getName(), "draw(Renderer)", "Exception", e);
                }
            }

            // Check if we have a texture renderer yet before going further
            if (UIContainer._textureRenderer != null) {
                UIContainer._drawingStandin = true;
                // Save aside our opacity
                final float op = getLocalOpacity();
                // Set our opacity to 1.0 for the cached texture
                setOpacity(1.0f);
                // render the container to a texture
                UIContainer._textureRenderer.renderSpatial(this, _fakeTexture, Renderer.BUFFER_COLOR_AND_DEPTH);
                // return our old transparency
                setOpacity(op);
                UIContainer._drawingStandin = false;

                // Prepare the texture coordinates for our container.
                float dW = newWidth / (float) UIContainer._textureRenderer.getWidth();
                if (dW > 1) {
                    dW = 1;
                }
                float dH = newHeight / (float) UIContainer._textureRenderer.getHeight();
                if (dH > 1) {
                    dH = 1;
                }
                final FloatBuffer tbuf = _standin.getMeshData().getTextureBuffer(0);
                tbuf.clear();
                tbuf.put(0).put(dH);
                tbuf.put(0).put(0);
                tbuf.put(dW).put(0);
                tbuf.put(dW).put(dH);
                tbuf.rewind();

                _dirty = false;
            }
            getHud().getHudCamera().apply(renderer);
        }

        // Now, render the standin quad.
        if (_standin != null) {
            // See if we need to change the dimensions of our standin quad.

            if (newWidth != _standin.getWidth() || newHeight != _standin.getHeight()) {
                _standin.resize(newWidth, newHeight);
            }

            // Prepare our default color with the correct alpha value for opacity.
            final ColorRGBA color = ColorRGBA.fetchTempInstance();
            color.set(1, 1, 1, getCombinedOpacity());
            _standin.setDefaultColor(color);
            ColorRGBA.releaseTempInstance(color);

            // Position standin quad properly
            _standin.setWorldTranslation(x, y, getWorldTranslation().getZ());
            _standin.setWorldRotation(getWorldRotation());

            final boolean clipTest = renderer.getScissorUtils().isClipTestEnabled();
            renderer.getScissorUtils().setClipTestEnabled(false);
            // draw our standin quad with cached container texture.
            _standin.draw(renderer);
            renderer.getScissorUtils().setClipTestEnabled(clipTest);
        }
    }

    /**
     * Build our standin quad and (as necessary) a texture renderer.
     *
     * @param renderer
     *            the renderer to use if we need to generate a texture renderer
     */
    private void buildStandin(final Renderer renderer) {
        // Check for and create a texture renderer if none exists yet.
        if (UIContainer._textureRenderer == null) {
            final int maxRBS = ContextManager.getCurrentContext().getCapabilities().getMaxRenderBufferSize();
            final int size = maxRBS > 0 ? Math.min(UIContainer.STANDIN_TEXTURE_SIZE, maxRBS)
                    : UIContainer.STANDIN_TEXTURE_SIZE;
            UIContainer._textureRenderer = renderer.createTextureRenderer(size, size, 16, 0);
            if (UIContainer._textureRenderer != null) {
                UIContainer._textureRenderer.setBackgroundColor(new ColorRGBA(0f, 1f, 0f, 0f));
            } else {
                // Can't make standin.
                _useStandin = false;
                return;
            }
        }

        _standin = new UIQuad("container_standin", 1, 1);
        _standin.setRenderMaterial("ui/textured/default_color.yaml");

        // no frustum culling checks
        _standin.getSceneHints().setCullHint(CullHint.Never);
        // no lighting
        _standin.getSceneHints().setLightCombineMode(LightCombineMode.Off);
        // a single texture
        _standin.getSceneHints().setTextureCombineMode(TextureCombineMode.Replace);
        // immediate mode
        _standin.getSceneHints().setRenderBucketType(RenderBucketType.Skip);

        // Add an alpha blend state
        final BlendState blend = new BlendState();
        blend.setBlendEnabled(true);
        blend.setSourceFunction(SourceFunction.SourceAlpha);
        blend.setDestinationFunction(DestinationFunction.OneMinusSourceAlpha);
        _standin.setRenderState(blend);

        // throw out fragments with alpha of 0.
        _standin.setProperty(AlphaTestConsts.KEY_AlphaTestType, AlphaTestConsts.TestFunction.GreaterThan);
        _standin.setProperty(AlphaTestConsts.KEY_AlphaReference, 0f);

        // Update our standin's texture
        resetFakeTexture();

        // Update the standin, getting states, etc. all set.
        _standin.updateGeometricState(0);
    }

    private void resetFakeTexture() {
        // create a texture to cache the contents to
        _fakeTexture = new Texture2D();
        _fakeTexture.setMagnificationFilter(MagnificationFilter.Bilinear);
        _fakeTexture.setMinificationFilter(_minificationFilter);
        _fakeTexture.setTextureStoreFormat(TextureStoreFormat.RGBA8);
        _fakeTexture.setWrap(WrapMode.EdgeClamp);
        UIContainer._textureRenderer.setupTexture(_fakeTexture);

        // Set a texturestate on the standin, using the fake texture
        final TextureState ts = new TextureState();
        ts.setTexture(_fakeTexture);
        _standin.setRenderState(ts);
    }

    /**
     * Causes our shared texture renderer - used to draw cached versions of all containers - to be recreated on the next
     * render loop.
     */
    public static void resetTextureRenderer(final Object queueKey) {
        final Callable<Void> exe = new Callable<Void>() {
            public Void call() {
                if (UIContainer._textureRenderer != null) {
                    UIContainer._textureRenderer.cleanup();
                }
                UIContainer._textureRenderer = null;
                return null;
            }
        };
        GameTaskQueueManager.getManager(queueKey).render(exe);
    }

    /**
     * @return true if this container has had recent content changes that would require a repaint.
     */
    public boolean isDirty() {
        return _dirty;
    }

    /**
     * @param dirty
     *            true if this container has had recent content changes that would require a repaint.
     */
    public void setDirty(final boolean dirty) {
        _dirty = dirty;
    }

    /**
     * Set ourselves dirty.
     */
    @Override
    public void fireComponentDirty() {
        super.fireComponentDirty();
        setDirty(true);
    }

    @Override
    public void fireStyleChanged() {
        super.fireStyleChanged();
        Spatial child;
        for (int i = 0, cSize = getNumberOfChildren(); i < cSize; i++) {
            child = getChild(i);
            if (child != null) {
                if (child instanceof UIComponent) {
                    ((UIComponent) child).fireStyleChanged();
                }
            }
        }
    }

    /**
     * Release our standin and cached texture for gc. If needed again, they will be created from scratch.
     */
    public void clearStandin() {
        _fakeTexture = null;
        _standin = null;
    }

    /**
     *
     * @param doClip
     *            true (default) if we want this container to clip the drawing of its contents to the dimensions of its
     *            content area.
     */
    public void setDoClip(final boolean doClip) {
        _doClip = doClip;
    }

    public boolean isDoClip() {
        return _doClip;
    }

    /**
     * @param use
     *            if true, we will draw the container's contents to a cached texture and use that to display this
     *            container (on a simple quad) instead of drawing all of its components each time. When the container is
     *            marked as dirty, we will update the contents of the texture.
     */
    public void setUseStandin(final boolean use) {
        _useStandin = use;
        if (!_useStandin) {
            clearStandin();
        }
    }

    /**
     *
     * @return true if we should use a cached texture copy to draw this container.
     * @see #setUseStandin(boolean)
     */
    public boolean isUseStandin() {
        return _useStandin;
    }

    /**
     * @return true if we are currently rendering a container to texture.
     */
    public static boolean isDrawingStandin() {
        return UIContainer._drawingStandin;
    }

    /**
     * Set the minification filter for the standin.
     *
     * @param filter
     */
    public void setMinificationFilter(final MinificationFilter filter) {
        _minificationFilter = filter;
        if (_fakeTexture != null) {
            resetFakeTexture();
        }
    }

    /**
     * @return the minification filter used for standin.
     */
    public MinificationFilter getMinificationFilter() {
        return _minificationFilter;
    }
}
