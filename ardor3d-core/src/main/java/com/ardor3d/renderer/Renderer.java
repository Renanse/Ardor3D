/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.renderer;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import com.ardor3d.image.ImageDataFormat;
import com.ardor3d.image.PixelDataType;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.math.type.ReadOnlyMatrix4;
import com.ardor3d.math.type.ReadOnlyTransform;
import com.ardor3d.renderer.material.IShaderUtils;
import com.ardor3d.renderer.queue.RenderQueue;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.texture.ITextureUtils;
import com.ardor3d.renderer.texture.TextureRenderer;
import com.ardor3d.scenegraph.IndexBufferData;
import com.ardor3d.scenegraph.Renderable;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.util.Ardor3dException;

/**
 * <code>Renderer</code> defines an interface for classes that handle displaying of graphics data to a render context.
 *
 * All rendering state and tasks should be handled through this class.
 */
public interface Renderer {

    /**
     * No buffer.
     */
    public static int BUFFER_NONE = 0x00;
    /**
     * A buffer storing color information generally for display to the user.
     */
    public static int BUFFER_COLOR = 0x01;
    /**
     * A depth buffer allows sorting of pixels by depth or distance from the view port.
     */
    public static int BUFFER_DEPTH = 0x02;
    /**
     * A buffer used for masking out areas of the screen to prevent drawing.
     */
    public static int BUFFER_STENCIL = 0x04;

    /**
     * Convenience for those that find it too hard to do bitwise or. :)
     */
    public static int BUFFER_COLOR_AND_DEPTH = BUFFER_COLOR | BUFFER_DEPTH;

    /**
     * <code>setBackgroundColor</code> sets the color of window. This color will be shown for any pixel that is not set
     * via typical rendering operations.
     *
     * @param c
     *            the color to set the background to.
     */
    void setBackgroundColor(ReadOnlyColorRGBA c);

    /**
     * <code>getBackgroundColor</code> retrieves the clear color of the current OpenGL context.
     *
     * @return the current clear color.
     */
    ReadOnlyColorRGBA getBackgroundColor();

    /**
     * <code>clearBuffers</code> clears the given buffers (specified as a bitwise &).
     *
     * @param buffers
     *            the buffers to clear.
     * @see #BUFFER_COLOR
     * @see #BUFFER_DEPTH
     * @see #BUFFER_ACCUMULATION
     * @see #BUFFER_STENCIL
     */
    void clearBuffers(int buffers);

    /**
     * <code>clearBuffers</code> clears the given buffers (specified as a bitwise &).
     *
     * @param buffers
     *            the buffers to clear.
     * @param strict
     *            if true, we'll limit the clearing to just the viewport area specified by the current camera.
     * @see #BUFFER_COLOR
     * @see #BUFFER_DEPTH
     * @see #BUFFER_ACCUMULATION
     * @see #BUFFER_STENCIL
     */
    void clearBuffers(int buffers, boolean strict);

    /**
     * <code>flushFrame</code> handles rendering any items still remaining in the render buckets and optionally swaps
     * the back buffer with the currently displayed buffer.
     *
     * @param doSwap
     *            if true, will ask the underlying implementation to blit the back buffer contents to the display
     *            buffer. Usually this will be true, unless you are in a windowing toolkit that handles doing this for
     *            you.
     */
    void flushFrame(boolean doSwap);

    /**
     * render queues - will first sort, then render, then finally clear the queue
     */
    void renderBuckets();

    /**
     * render queues
     *
     * @param doSort
     *            if true, the queues will be sorted prior to rendering.
     * @param doClear
     *            if true, the queues will be emptied after rendering.
     */
    void renderBuckets(boolean doSort, boolean doClear);

    /**
     * clear the render queue
     */
    void clearQueue();

    /**
     * <code>grabScreenContents</code> reads a block of data as bytes from the current framebuffer. The format
     * determines how many bytes per pixel are read and thus how big the buffer must be that you pass in.
     *
     * @param store
     *            a buffer to store contents in.
     * @param format
     *            the format to read in bytes for.
     * @param x
     *            - x starting point of block
     * @param y
     *            - y starting point of block
     * @param w
     *            - width of block
     * @param h
     *            - height of block
     */
    void grabScreenContents(ByteBuffer store, ImageDataFormat format, int x, int y, int w, int h);

    void grabScreenContents(final ByteBuffer store, final ImageDataFormat format, final PixelDataType type, final int x,
            final int y, final int w, final int h);

    /**
     * Gets the expected size (in bytes) of the buffer used to call <code>grabScreenContents</code>
     *
     * @param format
     *            the format to read in data for.
     * @param type
     *            the format to read in data for.
     * @param w
     *            - width of block
     * @param h
     *            - height of block
     * @return the expected size (in bytes) of the buffer used to call <code>grabScreenContents</code>
     */
    int getExpectedBufferSizeToGrabScreenContents(final ImageDataFormat format, final PixelDataType type, final int w,
            final int h);

    /**
     * <code>draw</code> renders a scene. As it receives a base class of <code>Spatial</code> the renderer hands off
     * management of the scene to spatial for it to determine when a <code>Geometry</code> leaf is reached.
     *
     * @param s
     *            the scene to render.
     */
    void draw(Spatial s);

    /**
     * <code>flush</code> tells the graphics hardware to send through all currently waiting commands in the buffer.
     */
    void flushGraphics();

    /**
     * <code>finish</code> is similar to flush, however it blocks until all waiting hardware graphics commands have been
     * finished.
     */
    void finishGraphics();

    /**
     * Get the render queue associated with this Renderer.
     *
     * @return RenderQueue
     */
    RenderQueue getQueue();

    /**
     * Return true if this renderer is in the middle of processing its RenderQueue.
     *
     * @return boolean
     */
    boolean isProcessingQueue();

    /**
     * Check a given Spatial to see if it should be queued. return true if it was queued.
     *
     * @param s
     *            Spatial to check
     * @return true if it was queued.
     */
    boolean checkAndAdd(Spatial s);

    /**
     * Check the underlying rendering system (opengl, etc.) for exceptions.
     *
     * @throws Ardor3dException
     *             if an error is found.
     */
    void checkCardError() throws Ardor3dException;

    /**
     * <code>draw</code> renders the renderable to the back buffer.
     *
     * @param renderable
     *            the text object to be rendered.
     */
    void draw(Renderable renderable);

    void drawArrays(int start, int count, IndexMode mode);

    void drawArraysInstanced(int start, int count, IndexMode mode, int instanceCount);

    void drawElements(final IndexBufferData<?> indices, final int start, final int count, final IndexMode mode);

    void drawElementsInstanced(final IndexBufferData<?> indices, final int start, final int count, final IndexMode mode,
            int instanceCount);

    FloatBuffer getMatrix(RenderMatrixType type);

    void setMatrix(RenderMatrixType type, FloatBuffer matrix);

    void setMatrix(RenderMatrixType type, ReadOnlyMatrix4 matrix, boolean rowMajor);

    void setMatrix(RenderMatrixType type, ReadOnlyTransform transform);

    void computeNormalMatrix(boolean modelIsUniformScale);

    void setViewport(int x, int y, int width, int height);

    void setDepthRange(double depthRangeNear, double depthRangeFar);

    /**
     * Specify which color buffers are to be drawn into.
     *
     * @param target
     */
    void setDrawBuffer(DrawBufferTarget target);

    /**
     * Set our GL_POINT size. Either point size is controlled by the shader, or if that is false, a single point size is
     * used as given.
     *
     * @param shaderSizeControl
     *            if true, point size is controlled from our vertex shader via <code>gl_PointSize</code>.
     * @param staticPointSize
     *            the size to make our points, only used if <code>shaderSizeControl</code> is false;
     */
    void setPointSize(final boolean shaderSizeControl, final float staticPointSize);

    /**
     * Apply the given state to the current RenderContext using this Renderer.
     *
     * @param type
     *            the state type
     * @param state
     *            the render state. If null, the renderer's default is applied instead.
     * @return the actual state we applied - could be different from state if another state is enforced, or if state was
     *         null.
     */
    RenderState applyState(StateType type, RenderState state);

    /**
     * @param type
     *            the state type to grab
     * @return the appropriate render state for the current context for the current type. This is the enforced state if
     *         one exists or the given current state if not null. Otherwise, the Renderer's default state is returned.
     */
    RenderState getProperRenderState(StateType type, RenderState current);

    /**
     * @return the renderer specific shader utils class.
     */
    IShaderUtils getShaderUtils();

    /**
     * @return the renderer specific texture utils class.
     */
    ITextureUtils getTextureUtils();

    /**
     * @return the renderer specific texture utils class.
     */
    IScissorUtils getScissorUtils();

    /**
     * Creates a TextureRenderer backed by this Renderer. Must have a current context set (see
     * {@link ContextManager#getCurrentContext()}) so the renderer can query hardware capabilities.
     *
     * @param width
     * @param height
     * @param depthBits
     * @param samples
     * @return the texture renderer
     */
    TextureRenderer createTextureRenderer(final int width, final int height, final int depthBits, final int samples);

}
