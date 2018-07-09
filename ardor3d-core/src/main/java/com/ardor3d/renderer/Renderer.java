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

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Collection;
import java.util.List;

import com.ardor3d.image.ImageDataFormat;
import com.ardor3d.image.PixelDataType;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture1D;
import com.ardor3d.image.Texture2D;
import com.ardor3d.image.Texture3D;
import com.ardor3d.image.TextureCubeMap;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.math.type.ReadOnlyRectangle2;
import com.ardor3d.math.type.ReadOnlyTransform;
import com.ardor3d.renderer.queue.RenderQueue;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.scenegraph.AbstractBufferData;
import com.ardor3d.scenegraph.FloatBufferData;
import com.ardor3d.scenegraph.IndexBufferData;
import com.ardor3d.scenegraph.Renderable;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.hint.NormalsMode;
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
     * Often a higher precision buffer used to gather rendering results over time.
     */
    public static int BUFFER_ACCUMULATION = 0x04;
    /**
     * A buffer used for masking out areas of the screen to prevent drawing.
     */
    public static int BUFFER_STENCIL = 0x08;

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
     *
     * <code>setOrtho</code> sets the display system to be in orthographic mode. If the system has already been set to
     * orthographic mode a <code>Ardor3dException</code> is thrown. The origin (0,0) is the bottom left of the screen.
     *
     */
    void setOrtho();

    /**
     *
     * <code>unsetOrhto</code> unsets the display system from orthographic mode back into regular projection mode. If
     * the system is not in orthographic mode a <code>Ardor3dException</code> is thrown.
     *
     *
     */
    void unsetOrtho();

    /**
     * @return true if the renderer is currently in ortho mode.
     */
    boolean isInOrthoMode();

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

    void grabScreenContents(final ByteBuffer store, final ImageDataFormat format, final PixelDataType type,
            final int x, final int y, final int w, final int h);

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
     * Attempts to delete the VBOs with the given id. Ignores null ids or ids < 1.
     *
     * @param ids
     */
    void deleteVBOs(Collection<Integer> ids);

    /**
     * Attempts to delete any VBOs associated with this buffer that are relevant to the current RenderContext.
     *
     * @param ids
     */
    void deleteVBOs(AbstractBufferData<?> buffer);

    /**
     * Unbind the current VBO elements.
     */
    void unbindVBO();

    /**
     * Update all or a portion of an existing one dimensional texture object.
     *
     * @param destination
     *            the texture to update. Should already have been sent to the card (have a valid texture id.)
     * @param dstOffsetX
     *            the offset into the destination to start our update.
     * @param dstWidth
     *            the width of the region to update.
     * @param source
     *            the data to update from.
     * @param srcOffsetX
     *            the optional offset into our source data.
     */
    void updateTexture1DSubImage(Texture1D destination, int dstOffsetX, int dstWidth, ByteBuffer source, int srcOffsetX);

    /**
     * Update all or a portion of an existing two dimensional texture object.
     *
     * @param destination
     *            the texture to update. Should already have been sent to the card (have a valid texture id.)
     * @param dstOffsetX
     *            the x offset into the destination to start our update.
     * @param dstOffsetY
     *            the y offset into the destination to start our update.
     * @param dstWidth
     *            the width of the region to update.
     * @param dstHeight
     *            the height of the region to update.
     * @param source
     *            the data to update from.
     * @param srcOffsetX
     *            the optional X offset into our source data.
     * @param srcOffsetY
     *            the optional Y offset into our source data.
     * @param srcTotalWidth
     *            the total width of our source data, so we can properly walk through it.
     */
    void updateTexture2DSubImage(Texture2D destination, int dstOffsetX, int dstOffsetY, int dstWidth, int dstHeight,
            ByteBuffer source, int srcOffsetX, int srcOffsetY, int srcTotalWidth);

    /**
     * Update all or a portion of an existing one dimensional texture object.
     *
     * @param destination
     *            the texture to update. Should already have been sent to the card (have a valid texture id.)
     * @param dstOffsetX
     *            the x offset into the destination to start our update.
     * @param dstOffsetY
     *            the y offset into the destination to start our update.
     * @param dstOffsetZ
     *            the z offset into the destination to start our update.
     * @param dstWidth
     *            the width of the region to update.
     * @param dstHeight
     *            the height of the region to update.
     * @param dstDepth
     *            the depth of the region to update. eg. 1 == one slice
     * @param source
     *            the data to update from.
     * @param srcOffsetX
     *            the optional X offset into our source data.
     * @param srcOffsetY
     *            the optional Y offset into our source data.
     * @param srcOffsetZ
     *            the optional Z offset into our source data.
     * @param srcTotalWidth
     *            the total width of our source data, so we can properly walk through it.
     * @param srcTotalHeight
     *            the total height of our source data, so we can properly walk through it.
     */
    void updateTexture3DSubImage(Texture3D destination, int dstOffsetX, int dstOffsetY, int dstOffsetZ, int dstWidth,
            int dstHeight, int dstDepth, ByteBuffer source, int srcOffsetX, int srcOffsetY, int srcOffsetZ,
            int srcTotalWidth, int srcTotalHeight);

    /**
     * Update all or a portion of a single two dimensional face on an existing cubemap texture object.
     *
     * @param destination
     *            the texture to update. Should already have been sent to the card (have a valid texture id.)
     * @param dstFace
     *            the face to update.
     * @param dstOffsetX
     *            the x offset into the destination to start our update.
     * @param dstOffsetY
     *            the y offset into the destination to start our update.
     * @param dstWidth
     *            the width of the region to update.
     * @param dstHeight
     *            the height of the region to update.
     * @param source
     *            the data to update from.
     * @param srcOffsetX
     *            the optional X offset into our source data.
     * @param srcOffsetY
     *            the optional Y offset into our source data.
     * @param srcTotalWidth
     *            the total width of our source data, so we can properly walk through it.
     */
    void updateTextureCubeMapSubImage(TextureCubeMap destination, TextureCubeMap.Face dstFace, int dstOffsetX,
            int dstOffsetY, int dstWidth, int dstHeight, ByteBuffer source, int srcOffsetX, int srcOffsetY,
            int srcTotalWidth);

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

    /**
     * <code>doTransforms</code> sets the current transform.
     *
     * @param transform
     *            transform to apply.
     */
    boolean doTransforms(ReadOnlyTransform transform);

    /**
     * <code>undoTransforms</code> reverts the latest transform.
     *
     * @param transform
     *            transform to revert.
     */
    void undoTransforms(ReadOnlyTransform transform);

    // TODO: Arrays
    void setupVertexData(FloatBufferData vertexCoords);

    void setupNormalData(FloatBufferData normalCoords);

    void setupColorData(FloatBufferData colorCoords);

    void setupFogData(FloatBufferData fogCoords);

    void setupTextureData(List<FloatBufferData> textureCoords);

    void drawElements(IndexBufferData<?> indices, int[] indexLengths, IndexMode[] indexModes, int primcount);

    void drawArrays(FloatBufferData vertexBuffer, int[] indexLengths, IndexMode[] indexModes, int primcount);

    void drawElementsVBO(IndexBufferData<?> indices, int[] indexLengths, IndexMode[] indexModes, int primcount);

    void applyNormalsMode(NormalsMode normMode, ReadOnlyTransform worldTransform);

    void applyDefaultColor(ReadOnlyColorRGBA defaultColor);

    // TODO: VBO
    void setupVertexDataVBO(FloatBufferData vertexCoords);

    void setupNormalDataVBO(FloatBufferData normalCoords);

    void setupColorDataVBO(FloatBufferData colorCoords);

    void setupFogDataVBO(FloatBufferData fogCoords);

    void setupTextureDataVBO(List<FloatBufferData> textureCoords);

    void setupInterleavedDataVBO(FloatBufferData interleaved, FloatBufferData vertexCoords,
            FloatBufferData normalCoords, FloatBufferData colorCoords, List<FloatBufferData> textureCoords);

    void setProjectionMatrix(FloatBuffer matrix);

    /**
     * Gets the current projection matrix in row major order
     *
     * @param store
     *            The buffer to store in. If null or remaining is < 16, a new FloatBuffer will be created and returned.
     * @return
     */
    FloatBuffer getProjectionMatrix(FloatBuffer store);

    void setModelViewMatrix(FloatBuffer matrix);

    /**
     * Gets the current modelview matrix in row major order
     *
     * @param store
     *            The buffer to store in. If null or remaining is < 16, a new FloatBuffer will be created and returned.
     * @return
     */
    FloatBuffer getModelViewMatrix(FloatBuffer store);

    void setViewport(int x, int y, int width, int height);

    void setDepthRange(double depthRangeNear, double depthRangeFar);

    /**
     * Specify which color buffers are to be drawn into.
     *
     * @param target
     */
    void setDrawBuffer(DrawBufferTarget target);

    /**
     * This is a workaround until we make shared Record classes, or open up lower level opengl calls abstracted from
     * lwjgl/jogl.
     *
     * @param lineWidth
     * @param stippleFactor
     * @param stipplePattern
     * @param antialiased
     */
    void setupLineParameters(float lineWidth, int stippleFactor, short stipplePattern, boolean antialiased);

    /**
     * This is a workaround until we make shared Record classes, or open up lower level opengl calls abstracted from
     * lwjgl/jogl.
     *
     * @param pointSize
     * @param antialiased
     * @param isSprite
     * @param useDistanceAttenuation
     * @param attenuationCoefficients
     * @param minPointSize
     * @param maxPointSize
     */
    void setupPointParameters(float pointSize, boolean antialiased, boolean isSprite, boolean useDistanceAttenuation,
            FloatBuffer attenuationCoefficients, float minPointSize, float maxPointSize);

    /**
     * Apply the given state to the current RenderContext using this Renderer.
     *
     * @param type
     *            the state type
     * @param state
     *            the render state. If null, the renderer's default is applied instead.
     */
    void applyState(StateType type, RenderState state);

    /**
     * Loads a texture onto the card for the current OpenGL context.
     *
     * @param texture
     *            the texture obejct to load.
     * @param unit
     *            the texture unit to load into
     */
    void loadTexture(Texture texture, int unit);

    /**
     * Explicitly remove this Texture from the graphics card. Note, the texture is only removed for the current context.
     * If the texture is used in other contexts, those uses are not touched. If the texture is not used in this context,
     * this is a no-op.
     *
     * @param texture
     *            the Texture object to remove.
     */
    void deleteTexture(Texture texture);

    /**
     * Removes the given texture ids from the current OpenGL context.
     *
     * @param ids
     *            a list/set of ids to remove.
     */
    void deleteTextureIds(Collection<Integer> ids);

    /**
     * Add the given rectangle to the clip stack, clipping the rendering area by the given rectangle intersected with
     * any existing scissor entries. Enable clipping if this is the first rectangle in the stack.
     *
     * @param rectangle
     */
    void pushClip(ReadOnlyRectangle2 rectangle);

    /**
     * Push a clip onto the stack indicating that future clips should not intersect with clips prior to this one.
     * Basically this allows you to ignore prior clips for nested drawing. Make sure you pop this using
     * {@link #popClip()}.
     */
    void pushEmptyClip();

    /**
     * Pop the most recent rectangle from the stack and adjust the rendering area accordingly.
     */
    void popClip();

    /**
     * Clear all rectangles from the clip stack and disable clipping.
     */
    void clearClips();

    /**
     * @param enabled
     *            toggle clipping without effecting the current clip stack.
     */
    void setClipTestEnabled(boolean enabled);

    /**
     * @return true if the renderer believes clipping is enabled
     */
    boolean isClipTestEnabled();

    /**
     * @param type
     *            the state type to grab
     * @return the appropriate render state for the current context for the current type. This is the enforced state if
     *         one exists or the given current state if not null. Otherwise, the Renderer's default state is returned.
     */
    RenderState getProperRenderState(StateType type, RenderState current);

    /**
     * Set rendering logic that will be called during drawing of renderables
     *
     * @param logic
     *            logic to use in rendering. call with null to reset rendering.
     * @see RenderLogic
     */
    void setRenderLogic(RenderLogic logic);

}
