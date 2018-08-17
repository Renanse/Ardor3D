/**
 * Copyright (c) 2008-2010 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.jogl;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Collection;
import java.util.logging.Logger;

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES1;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GL2ES3;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLException;
import javax.media.opengl.fixedfunc.GLMatrixFunc;
import javax.media.opengl.glu.GLU;

import com.ardor3d.image.ImageDataFormat;
import com.ardor3d.image.PixelDataType;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture1D;
import com.ardor3d.image.Texture2D;
import com.ardor3d.image.Texture3D;
import com.ardor3d.image.TextureCubeMap;
import com.ardor3d.image.TextureCubeMap.Face;
import com.ardor3d.math.Matrix4;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.math.type.ReadOnlyRectangle2;
import com.ardor3d.math.type.ReadOnlyTransform;
import com.ardor3d.renderer.AbstractRenderer;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.DrawBufferTarget;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.jogl.state.record.JoglRendererRecord;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.ColorMaskState;
import com.ardor3d.renderer.state.CullState;
import com.ardor3d.renderer.state.LightState;
import com.ardor3d.renderer.state.MaterialState;
import com.ardor3d.renderer.state.OffsetState;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.ShaderState;
import com.ardor3d.renderer.state.ShadingState;
import com.ardor3d.renderer.state.StencilState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.WireframeState;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.renderer.state.record.LineRecord;
import com.ardor3d.renderer.state.record.RendererRecord;
import com.ardor3d.scene.state.jogl.JoglBlendStateUtil;
import com.ardor3d.scene.state.jogl.JoglColorMaskStateUtil;
import com.ardor3d.scene.state.jogl.JoglCullStateUtil;
import com.ardor3d.scene.state.jogl.JoglLightStateUtil;
import com.ardor3d.scene.state.jogl.JoglMaterialStateUtil;
import com.ardor3d.scene.state.jogl.JoglOffsetStateUtil;
import com.ardor3d.scene.state.jogl.JoglShaderObjectsStateUtil;
import com.ardor3d.scene.state.jogl.JoglShadingStateUtil;
import com.ardor3d.scene.state.jogl.JoglStencilStateUtil;
import com.ardor3d.scene.state.jogl.JoglTextureStateUtil;
import com.ardor3d.scene.state.jogl.JoglWireframeStateUtil;
import com.ardor3d.scene.state.jogl.JoglZBufferStateUtil;
import com.ardor3d.scene.state.jogl.util.JoglRendererUtil;
import com.ardor3d.scene.state.jogl.util.JoglTextureUtil;
import com.ardor3d.scenegraph.AbstractBufferData;
import com.ardor3d.scenegraph.AbstractBufferData.VBOAccessMode;
import com.ardor3d.scenegraph.IndexBufferData;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.scenegraph.Renderable;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.util.Ardor3dException;
import com.ardor3d.util.Constants;
import com.ardor3d.util.geom.BufferUtils;
import com.ardor3d.util.stat.StatCollector;
import com.ardor3d.util.stat.StatType;
import com.jogamp.opengl.util.GLBuffers;

/**
 * <code>JoglRenderer</code> provides an implementation of the <code>Renderer</code> interface using the JOGL API.
 *
 * @see com.ardor3d.renderer.Renderer
 */
public class JoglRenderer extends AbstractRenderer {
    private static final Logger logger = Logger.getLogger(JoglRenderer.class.getName());

    private FloatBuffer _transformBuffer;
    private final Matrix4 _transformMatrix = new Matrix4();

    /**
     * Constructor instantiates a new <code>JoglRenderer</code> object.
     */
    public JoglRenderer() {
        logger.fine("JoglRenderer created.");
    }

    @Override
    public void setBackgroundColor(final ReadOnlyColorRGBA c) {
        final GL gl = GLContext.getCurrentGL();

        _backgroundColor.set(c);
        gl.glClearColor(_backgroundColor.getRed(), _backgroundColor.getGreen(), _backgroundColor.getBlue(),
                _backgroundColor.getAlpha());
    }

    @Override
    public void renderBuckets() {
        renderBuckets(true, true);
    }

    @Override
    public void renderBuckets(final boolean doSort, final boolean doClear) {
        _processingQueue = true;
        if (doSort && doClear) {
            _queue.renderBuckets(this);
        } else {
            if (doSort) {
                _queue.sortBuckets();
            }
            _queue.renderOnly(this);
            if (doClear) {
                _queue.clearBuckets();
            }
        }
        _processingQueue = false;
    }

    /**
     * clear the render queue
     */
    @Override
    public void clearQueue() {
        _queue.clearBuckets();
    }

    @Override
    public void clearBuffers(final int buffers) {
        clearBuffers(buffers, false);
    }

    @Override
    public void clearBuffers(final int buffers, final boolean strict) {
        final GL gl = GLContext.getCurrentGL();

        int clear = 0;

        if ((buffers & Renderer.BUFFER_COLOR) != 0) {
            clear |= GL.GL_COLOR_BUFFER_BIT;
        }

        if ((buffers & Renderer.BUFFER_DEPTH) != 0) {
            clear |= GL.GL_DEPTH_BUFFER_BIT;

            // make sure no funny business is going on in the z before clearing.
            if (defaultStateList.containsKey(RenderState.StateType.ZBuffer)) {
                defaultStateList.get(RenderState.StateType.ZBuffer).setNeedsRefresh(true);
                doApplyState(defaultStateList.get(RenderState.StateType.ZBuffer));
            }
        }

        if ((buffers & Renderer.BUFFER_STENCIL) != 0) {
            clear |= GL.GL_STENCIL_BUFFER_BIT;

            gl.glClearStencil(_stencilClearValue);
            gl.glStencilMask(~0);
            gl.glClear(GL.GL_STENCIL_BUFFER_BIT);
        }

        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord record = context.getRendererRecord();

        if (strict) {
            // grab our camera to get width and height info.
            final Camera cam = Camera.getCurrentCamera();

            gl.glEnable(GL.GL_SCISSOR_TEST);
            gl.glScissor(0, 0, cam.getWidth(), cam.getHeight());
            record.setClippingTestEnabled(true);
        }

        gl.glClear(clear);

        if (strict) {
            // put us back.
            JoglRendererUtil.applyScissors(record);
        }
    }

    @Override
    public void flushFrame(final boolean doSwap) {
        final GL gl = GLContext.getCurrentGL();

        renderBuckets();

        gl.glFlush();
        if (doSwap) {

            doApplyState(defaultStateList.get(RenderState.StateType.ColorMask));

            if (Constants.stats) {
                StatCollector.startStat(StatType.STAT_DISPLAYSWAP_TIMER);
            }

            checkCardError();
            GLContext.getCurrent().getGLDrawable().swapBuffers();
            if (Constants.stats) {
                StatCollector.endStat(StatType.STAT_DISPLAYSWAP_TIMER);
            }
        }

        if (Constants.stats) {
            StatCollector.addStat(StatType.STAT_FRAMES, 1);
        }
    }

    @Override
    public void setOrtho() {
        if (_inOrthoMode) {
            throw new Ardor3dException("Already in Orthographic mode.");
        }
        // set up ortho mode
        final JoglRendererRecord matRecord = (JoglRendererRecord) ContextManager.getCurrentContext()
                .getRendererRecord();
        // JoglRendererUtil.switchMode(matRecord, GLMatrixFunc.GL_PROJECTION);
        matRecord.getMatrixBackend().pushMatrix();
        matRecord.getMatrixBackend().loadIdentity();

        final Camera camera = Camera.getCurrentCamera();
        final double viewportWidth = camera.getWidth() * (camera.getViewPortRight() - camera.getViewPortLeft());
        final double viewportHeight = camera.getHeight() * (camera.getViewPortTop() - camera.getViewPortBottom());
        matRecord.getMatrixBackend().setOrtho(0, viewportWidth, 0, viewportHeight, -1, 1);
        // JoglRendererUtil.switchMode(matRecord, GLMatrixFunc.GL_MODELVIEW);
        matRecord.getMatrixBackend().pushMatrix();
        matRecord.getMatrixBackend().loadIdentity();
        _inOrthoMode = true;
    }

    @Override
    public void unsetOrtho() {
        if (!_inOrthoMode) {
            throw new Ardor3dException("Not in Orthographic mode.");
        }
        // remove ortho mode, and go back to original
        // state
        final JoglRendererRecord matRecord = (JoglRendererRecord) ContextManager.getCurrentContext()
                .getRendererRecord();
        // JoglRendererUtil.switchMode(matRecord, GLMatrixFunc.GL_PROJECTION);
        matRecord.getMatrixBackend().popMatrix();
        // JoglRendererUtil.switchMode(matRecord, GLMatrixFunc.GL_MODELVIEW);
        matRecord.getMatrixBackend().popMatrix();
        _inOrthoMode = false;
    }

    @Override
    public void grabScreenContents(final ByteBuffer store, final ImageDataFormat format, final PixelDataType type,
            final int x, final int y, final int w, final int h) {
        final GL gl = GLContext.getCurrentGL();

        final int pixFormat = JoglTextureUtil.getGLPixelFormat(format);
        final int pixDataType = JoglTextureUtil.getGLPixelDataType(type);
        // N.B: it expects depth = 1 & pack = true
        gl.glReadPixels(x, y, w, h, pixFormat, pixDataType, store);
    }

    @Override
    public int getExpectedBufferSizeToGrabScreenContents(final ImageDataFormat format, final PixelDataType type,
            final int w, final int h) {
        final GL gl = GLContext.getCurrentGL();
        final int pixFormat = JoglTextureUtil.getGLPixelFormat(format);
        final int pixDataType = JoglTextureUtil.getGLPixelDataType(type);
        final int[] tmp = new int[1];
        return GLBuffers.sizeof(gl, tmp, pixFormat, pixDataType, w, h, 1, true);
    }

    @Override
    public void draw(final Spatial s) {
        if (s != null) {
            s.onDraw(this);
        }
    }

    @Override
    public boolean checkAndAdd(final Spatial s) {
        final RenderBucketType rqMode = s.getSceneHints().getRenderBucketType();
        if (rqMode != RenderBucketType.Skip) {
            getQueue().addToQueue(s, rqMode);
            return true;
        }
        return false;
    }

    /**
     * re-initializes the GL context for rendering of another piece of geometry.
     */
    protected void postdrawGeometry(final Mesh g) {
        // Nothing to do here yet
    }

    @Override
    public void flushGraphics() {
        final GL gl = GLContext.getCurrentGL();

        gl.glFlush();
    }

    @Override
    public void finishGraphics() {
        final GL gl = GLContext.getCurrentGL();

        gl.glFinish();
    }

    @Override
    public void deleteVAOs(final Collection<Integer> ids) {
        final GL gl = GLContext.getCurrentGL();

        final JoglRenderContext context = (JoglRenderContext) ContextManager.getCurrentContext();
        final IntBuffer vaoIdsBuffer = context.getDirectNioBuffersSet().getVaoIdsBuffer();
        vaoIdsBuffer.clear();
        for (final Integer i : ids) {
            if (!vaoIdsBuffer.hasRemaining()) {
                vaoIdsBuffer.flip();
                if (vaoIdsBuffer.remaining() > 0) {
                    gl.getGL3().glDeleteVertexArrays(vaoIdsBuffer.remaining(), vaoIdsBuffer);
                }
                vaoIdsBuffer.clear();
            }
            if (i != null && i != 0) {
                vaoIdsBuffer.put(i);
            }
        }
        vaoIdsBuffer.flip();
        if (vaoIdsBuffer.remaining() > 0) {
            gl.getGL3().glDeleteVertexArrays(vaoIdsBuffer.remaining(), vaoIdsBuffer);
        }
        vaoIdsBuffer.clear();
    }

    @Override
    public void deleteVAOs(final MeshData data) {
        if (data == null) {
            return;
        }

        final GL gl = GLContext.getCurrentGL();

        // ask for the current state record
        final JoglRenderContext context = (JoglRenderContext) ContextManager.getCurrentContext();

        final int id = data.getVAOID(context.getGlContextRep());
        if (id == 0) {
            // Not on card... return.
            return;
        }

        data.removeVAOID(context.getGlContextRep());

        final IntBuffer idBuff = context.getDirectNioBuffersSet().getSingleIntBuffer();
        idBuff.clear();
        idBuff.put(id);
        idBuff.flip();
        gl.getGL3().glDeleteVertexArrays(1, idBuff);
    }

    @Override
    public void deleteVBOs(final Collection<Integer> ids) {
        final GL gl = GLContext.getCurrentGL();

        final JoglRenderContext context = (JoglRenderContext) ContextManager.getCurrentContext();
        final IntBuffer vboIdsBuffer = context.getDirectNioBuffersSet().getVboIdsBuffer();
        vboIdsBuffer.clear();
        for (final Integer i : ids) {
            if (!vboIdsBuffer.hasRemaining()) {
                vboIdsBuffer.flip();
                if (vboIdsBuffer.remaining() > 0) {
                    gl.glDeleteBuffers(vboIdsBuffer.remaining(), vboIdsBuffer);
                }
                vboIdsBuffer.clear();
            }
            if (i != null && i != 0) {
                vboIdsBuffer.put(i);
            }
        }
        vboIdsBuffer.flip();
        if (vboIdsBuffer.remaining() > 0) {
            gl.glDeleteBuffers(vboIdsBuffer.remaining(), vboIdsBuffer);
        }
        vboIdsBuffer.clear();
    }

    @Override
    public void deleteVBOs(final AbstractBufferData<?> buffer) {
        if (buffer == null) {
            return;
        }

        final GL gl = GLContext.getCurrentGL();

        // ask for the current state record
        final JoglRenderContext context = (JoglRenderContext) ContextManager.getCurrentContext();

        final int id = buffer.getBufferId(context.getGlContextRep());
        if (id == 0) {
            // Not on card... return.
            return;
        }

        buffer.removeBufferId(context.getGlContextRep());

        final IntBuffer idBuff = context.getDirectNioBuffersSet().getSingleIntBuffer();
        idBuff.clear();
        idBuff.put(id);
        idBuff.flip();
        gl.glDeleteBuffers(1, idBuff);
    }

    @Override
    public void updateTexture1DSubImage(final Texture1D destination, final int dstOffsetX, final int dstWidth,
            final ByteBuffer source, final int srcOffsetX) {
        updateTexSubImage(destination, dstOffsetX, 0, 0, dstWidth, 0, 0, source, srcOffsetX, 0, 0, 0, 0, null);
    }

    @Override
    public void updateTexture2DSubImage(final Texture2D destination, final int dstOffsetX, final int dstOffsetY,
            final int dstWidth, final int dstHeight, final ByteBuffer source, final int srcOffsetX,
            final int srcOffsetY, final int srcTotalWidth) {
        updateTexSubImage(destination, dstOffsetX, dstOffsetY, 0, dstWidth, dstHeight, 0, source, srcOffsetX,
                srcOffsetY, 0, srcTotalWidth, 0, null);
    }

    @Override
    public void updateTexture3DSubImage(final Texture3D destination, final int dstOffsetX, final int dstOffsetY,
            final int dstOffsetZ, final int dstWidth, final int dstHeight, final int dstDepth, final ByteBuffer source,
            final int srcOffsetX, final int srcOffsetY, final int srcOffsetZ, final int srcTotalWidth,
            final int srcTotalHeight) {
        updateTexSubImage(destination, dstOffsetX, dstOffsetY, dstOffsetZ, dstWidth, dstHeight, dstDepth, source,
                srcOffsetX, srcOffsetY, srcOffsetZ, srcTotalWidth, srcTotalHeight, null);
    }

    @Override
    public void updateTextureCubeMapSubImage(final TextureCubeMap destination, final TextureCubeMap.Face dstFace,
            final int dstOffsetX, final int dstOffsetY, final int dstWidth, final int dstHeight,
            final ByteBuffer source, final int srcOffsetX, final int srcOffsetY, final int srcTotalWidth) {
        updateTexSubImage(destination, dstOffsetX, dstOffsetY, 0, dstWidth, dstHeight, 0, source, srcOffsetX,
                srcOffsetY, 0, srcTotalWidth, 0, dstFace);
    }

    private void updateTexSubImage(final Texture destination, final int dstOffsetX, final int dstOffsetY,
            final int dstOffsetZ, final int dstWidth, final int dstHeight, final int dstDepth, final ByteBuffer source,
            final int srcOffsetX, final int srcOffsetY, final int srcOffsetZ, final int srcTotalWidth,
            final int srcTotalHeight, final Face dstFace) {

        final GL gl = GLContext.getCurrentGL();

        // Ignore textures that do not have an id set
        if (destination.getTextureIdForContext(ContextManager.getCurrentContext().getGlContextRep()) == 0) {
            logger.warning("Attempting to update a texture that is not currently on the card.");
            return;
        }

        // Determine the original texture configuration, so that this method can
        // restore the texture configuration to its original state.
        final int origAlignment[] = new int[1];
        gl.glGetIntegerv(GL.GL_UNPACK_ALIGNMENT, origAlignment, 0);
        final int origRowLength = 0;
        final int origImageHeight = 0;
        final int origSkipPixels = 0;
        final int origSkipRows = 0;
        final int origSkipImages = 0;

        final int alignment = 1;

        int rowLength;
        if (srcTotalWidth == dstWidth) {
            // When the row length is zero, then the width parameter is used.
            // We use zero in these cases in the hope that we can avoid two
            // unnecessary calls to glPixelStorei.
            rowLength = 0;
        } else {
            // The number of pixels in a row is different than the number of
            // pixels in the region to be uploaded to the texture.
            rowLength = srcTotalWidth;
        }

        int imageHeight;
        if (srcTotalHeight == dstHeight) {
            // When the image height is zero, then the height parameter is used.
            // We use zero in these cases in the hope that we can avoid two
            // unnecessary calls to glPixelStorei.
            imageHeight = 0;
        } else {
            // The number of pixels in a row is different than the number of
            // pixels in the region to be uploaded to the texture.
            imageHeight = srcTotalHeight;
        }

        // Grab pixel format
        final int pixelFormat;
        if (destination.getImage() != null) {
            pixelFormat = JoglTextureUtil.getGLPixelFormat(destination.getImage().getDataFormat());
        } else {
            pixelFormat = JoglTextureUtil.getGLPixelFormatFromStoreFormat(destination.getTextureStoreFormat());
        }

        // bind...
        JoglTextureStateUtil.doTextureBind(destination, 0, false);

        // Update the texture configuration (when necessary).

        if (origAlignment[0] != alignment) {
            gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, alignment);
        }
        if (origRowLength != rowLength) {
            gl.glPixelStorei(GL2ES2.GL_UNPACK_ROW_LENGTH, rowLength);
        }
        if (origSkipPixels != srcOffsetX) {
            gl.glPixelStorei(GL2ES2.GL_UNPACK_SKIP_PIXELS, srcOffsetX);
        }
        // NOTE: The below will be skipped for texture types that don't support them because we are passing in 0's.
        if (origSkipRows != srcOffsetY) {
            gl.glPixelStorei(GL2ES2.GL_UNPACK_SKIP_ROWS, srcOffsetY);
        }
        if (origImageHeight != imageHeight) {
            gl.glPixelStorei(GL2ES3.GL_UNPACK_IMAGE_HEIGHT, imageHeight);
        }
        if (origSkipImages != srcOffsetZ) {
            gl.glPixelStorei(GL2ES3.GL_UNPACK_SKIP_IMAGES, srcOffsetZ);
        }

        // Upload the image region into the texture.
        try {
            switch (destination.getType()) {
                case TwoDimensional:
                    gl.glTexSubImage2D(GL.GL_TEXTURE_2D, 0, dstOffsetX, dstOffsetY, dstWidth, dstHeight, pixelFormat,
                            GL.GL_UNSIGNED_BYTE, source);
                    break;
                case OneDimensional:
                    gl.getGL2GL3().glTexSubImage1D(GL2GL3.GL_TEXTURE_1D, 0, dstOffsetX, dstWidth, pixelFormat,
                            GL.GL_UNSIGNED_BYTE, source);
                    break;
                case ThreeDimensional:
                    gl.getGL2ES2().glTexSubImage3D(GL2ES2.GL_TEXTURE_3D, 0, dstOffsetX, dstOffsetY, dstOffsetZ,
                            dstWidth, dstHeight, dstDepth, pixelFormat, GL.GL_UNSIGNED_BYTE, source);
                    break;
                case CubeMap:
                    gl.glTexSubImage2D(JoglTextureStateUtil.getGLCubeMapFace(dstFace), 0, dstOffsetX, dstOffsetY,
                            dstWidth, dstHeight, pixelFormat, GL.GL_UNSIGNED_BYTE, source);
                    break;
                default:
                    throw new Ardor3dException("Unsupported type for updateTextureSubImage: " + destination.getType());
            }
        } finally {
            // Restore the texture configuration (when necessary)...
            // Restore alignment.
            if (origAlignment[0] != alignment) {
                gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, origAlignment[0]);
            }
            // Restore row length.
            if (origRowLength != rowLength) {
                gl.glPixelStorei(GL2ES2.GL_UNPACK_ROW_LENGTH, origRowLength);
            }
            // Restore skip pixels.
            if (origSkipPixels != srcOffsetX) {
                gl.glPixelStorei(GL2ES2.GL_UNPACK_SKIP_PIXELS, origSkipPixels);
            }
            // Restore skip rows.
            if (origSkipRows != srcOffsetY) {
                gl.glPixelStorei(GL2ES2.GL_UNPACK_SKIP_ROWS, origSkipRows);
            }
            // Restore image height.
            if (origImageHeight != imageHeight) {
                gl.glPixelStorei(GL2ES3.GL_UNPACK_IMAGE_HEIGHT, origImageHeight);
            }
            // Restore skip images.
            if (origSkipImages != srcOffsetZ) {
                gl.glPixelStorei(GL2ES3.GL_UNPACK_SKIP_IMAGES, origSkipImages);
            }
        }
    }

    @Override
    public void checkCardError() throws Ardor3dException {
        final GL gl = GLContext.getCurrentGL();
        final GLU glu = new GLU();

        try {
            final int errorCode = gl.glGetError();
            if (errorCode != GL.GL_NO_ERROR) {
                throw new GLException(glu.gluErrorString(errorCode));
            }
        } catch (final GLException exception) {
            throw new Ardor3dException("Error in opengl: " + exception.getMessage(), exception);
        }
    }

    @Override
    public void draw(final Renderable renderable) {
        if (renderLogic != null) {
            renderLogic.apply(renderable);
        }
        renderable.render(this);
        if (renderLogic != null) {
            renderLogic.restore(renderable);
        }
    }

    @Override
    public boolean doTransforms(final ReadOnlyTransform transform) {
        // set world matrix
        if (!transform.isIdentity()) {
            synchronized (_transformMatrix) {
                final JoglRenderContext context = (JoglRenderContext) ContextManager.getCurrentContext();
                if (_transformBuffer == null) {
                    _transformBuffer = context.getDirectNioBuffersSet().getTransformBuffer();
                }
                _transformBuffer.clear();

                transform.getGLApplyMatrix(_transformBuffer);

                final JoglRendererRecord matRecord = context.getRendererRecord();
                // JoglRendererUtil.switchMode(matRecord, GLMatrixFunc.GL_MODELVIEW);
                matRecord.getMatrixBackend().pushMatrix();
                matRecord.getMatrixBackend().multMatrix(_transformBuffer);
                return true;
            }
        }
        return false;
    }

    @Override
    public void undoTransforms(final ReadOnlyTransform transform) {
        final JoglRenderContext context = (JoglRenderContext) ContextManager.getCurrentContext();
        final JoglRendererRecord matRecord = context.getRendererRecord();
        // JoglRendererUtil.switchMode(matRecord, GLMatrixFunc.GL_MODELVIEW);
        matRecord.getMatrixBackend().popMatrix();
    }

    @Override
    public boolean prepareForDraw(final MeshData data, final ShaderState currentShader) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void applyMatrices(final ReadOnlyTransform worldTransform, final ShaderState currentShader) {
        // TODO Auto-generated method stub

    }

    @Override
    public void drawArrays(final int start, final int count, final IndexMode mode) {
        final int glIndexMode = getGLIndexMode(mode);

        GLContext.getCurrentGL().getGL3().glDrawArrays(glIndexMode, start, count);

        if (Constants.stats) {
            addStats(mode, count);
        }
    }

    @Override
    public void drawArraysInstanced(final int start, final int count, final IndexMode mode, final int instanceCount) {
        final int glIndexMode = getGLIndexMode(mode);

        GLContext.getCurrentGL().getGL3().glDrawArraysInstanced(glIndexMode, start, count, instanceCount);

        if (Constants.stats) {
            addStats(mode, count * instanceCount);
        }
    }

    @Override
    public void drawElements(final IndexBufferData<?> indices, final int start, final int count, final IndexMode mode) {
        final int type = getGLDataType(indices);
        final int byteSize = indices.getByteCount();
        final int glIndexMode = getGLIndexMode(mode);

        GLContext.getCurrentGL().getGL3().glDrawElements(glIndexMode, count, type, (long) start * byteSize);

        if (Constants.stats) {
            addStats(mode, count);
        }
    }

    @Override
    public void drawElementsInstanced(final IndexBufferData<?> indices, final int start, final int count,
            final IndexMode mode, final int instanceCount) {
        final int type = getGLDataType(indices);
        final int byteSize = indices.getByteCount();
        final int glIndexMode = getGLIndexMode(mode);

        GLContext.getCurrentGL().getGL3().glDrawElementsInstanced(glIndexMode, count, type, (long) start * byteSize,
                instanceCount);

        if (Constants.stats) {
            addStats(mode, count * instanceCount);
        }

    }

    protected static int setupBufferObject(final AbstractBufferData<? extends Buffer> data, final boolean isEBO,
            final RenderContext context) {

        final GL gl = GLContext.getCurrentGL().getGL3();

        int id = data.getBufferId(context.getGlContextRep());
        if (id != 0) {
            return id;
        }

        final Buffer dataBuffer = data.getBuffer();
        if (dataBuffer != null) {
            // XXX: should we be rewinding? Maybe make that the programmer's responsibility.
            dataBuffer.rewind();
            id = makeVBOId();
            data.setBufferId(context.getGlContextRep(), id);

            final int target = isEBO ? GL.GL_ELEMENT_ARRAY_BUFFER : GL.GL_ARRAY_BUFFER;
            gl.glBindBuffer(target, id);
            gl.glBufferData(target, dataBuffer.limit() * data.getByteCount(), dataBuffer,
                    getGLVBOAccessMode(data.getVboAccessMode()));
        } else {
            throw new Ardor3dException("Attempting to create a buffer object with no Buffer value.");
        }
        return id;
    }

    private static int makeVBOId() {
        final GL gl = GLContext.getCurrentGL();

        final JoglRenderContext context = (JoglRenderContext) ContextManager.getCurrentContext();

        final IntBuffer idBuff = context.getDirectNioBuffersSet().getSingleIntBuffer();
        idBuff.clear();
        gl.glGenBuffers(1, idBuff);
        return idBuff.get(0);
    }

    private static int getGLVBOAccessMode(final VBOAccessMode vboAccessMode) {
        int glMode = GL.GL_STATIC_DRAW;
        switch (vboAccessMode) {
            case StaticDraw:
                glMode = GL.GL_STATIC_DRAW;
                break;
            case StaticRead:
                glMode = GL2ES3.GL_STATIC_READ;
                break;
            case StaticCopy:
                glMode = GL2ES3.GL_STATIC_COPY;
                break;
            case DynamicDraw:
                glMode = GL.GL_DYNAMIC_DRAW;
                break;
            case DynamicRead:
                glMode = GL2ES3.GL_DYNAMIC_READ;
                break;
            case DynamicCopy:
                glMode = GL2ES3.GL_DYNAMIC_COPY;
                break;
            case StreamDraw:
                glMode = GL2ES2.GL_STREAM_DRAW;
                break;
            case StreamRead:
                glMode = GL2ES3.GL_STREAM_READ;
                break;
            case StreamCopy:
                glMode = GL2ES3.GL_STREAM_COPY;
                break;
        }
        return glMode;
    }

    private int getGLIndexMode(final IndexMode indexMode) {
        int glMode = GL.GL_TRIANGLES;
        switch (indexMode) {
            case Triangles:
                glMode = GL.GL_TRIANGLES;
                break;
            case TriangleStrip:
                glMode = GL.GL_TRIANGLE_STRIP;
                break;
            case TriangleFan:
                glMode = GL.GL_TRIANGLE_FAN;
                break;
            case Lines:
                glMode = GL.GL_LINES;
                break;
            case LineStrip:
                glMode = GL.GL_LINE_STRIP;
                break;
            case LineLoop:
                glMode = GL.GL_LINE_LOOP;
                break;
            case Points:
                glMode = GL.GL_POINTS;
                break;
        }
        return glMode;
    }

    private int getGLDataType(final IndexBufferData<?> indices) {
        if (indices.getBuffer() instanceof ByteBuffer) {
            return GL.GL_UNSIGNED_BYTE;
        } else if (indices.getBuffer() instanceof ShortBuffer) {
            return GL.GL_UNSIGNED_SHORT;
        } else if (indices.getBuffer() instanceof IntBuffer) {
            return GL.GL_UNSIGNED_INT;
        }

        throw new IllegalArgumentException("Unknown buffer type: " + indices.getBuffer());
    }

    @Override
    public void setModelViewMatrix(final FloatBuffer matrix) {
        final JoglRendererRecord matRecord = (JoglRendererRecord) ContextManager.getCurrentContext()
                .getRendererRecord();
        // JoglRendererUtil.switchMode(matRecord, GLMatrixFunc.GL_MODELVIEW);

        loadMatrix(matrix);
    }

    @Override
    public void setProjectionMatrix(final FloatBuffer matrix) {
        final JoglRendererRecord matRecord = (JoglRendererRecord) ContextManager.getCurrentContext()
                .getRendererRecord();
        // JoglRendererUtil.switchMode(matRecord, GLMatrixFunc.GL_PROJECTION);

        loadMatrix(matrix);
    }

    private void loadMatrix(final FloatBuffer matrix) {
        final JoglRendererRecord matRecord = (JoglRendererRecord) ContextManager.getCurrentContext()
                .getRendererRecord();
        matRecord.getMatrixBackend().loadMatrix(matrix);
    }

    @Override
    public FloatBuffer getModelViewMatrix(final FloatBuffer store) {
        return getMatrix(GLMatrixFunc.GL_MODELVIEW_MATRIX, store);
    }

    @Override
    public FloatBuffer getProjectionMatrix(final FloatBuffer store) {
        return getMatrix(GLMatrixFunc.GL_PROJECTION_MATRIX, store);
    }

    private FloatBuffer getMatrix(final int matrixType, final FloatBuffer store) {
        FloatBuffer result = store;
        if (result == null || result.remaining() < 16) {
            result = BufferUtils.createFloatBuffer(16);
        }
        final JoglRendererRecord matRecord = (JoglRendererRecord) ContextManager.getCurrentContext()
                .getRendererRecord();
        matRecord.getMatrixBackend().getMatrix(matrixType, result);
        return result;
    }

    @Override
    public void setViewport(final int x, final int y, final int width, final int height) {
        GLContext.getCurrentGL().glViewport(x, y, width, height);
    }

    @Override
    public void setDepthRange(final double depthRangeNear, final double depthRangeFar) {
        GLContext.getCurrentGL().glDepthRange(depthRangeNear, depthRangeFar);
    }

    @Override
    public void setDrawBuffer(final DrawBufferTarget target) {
        final RendererRecord record = ContextManager.getCurrentContext().getRendererRecord();
        if (record.getDrawBufferTarget() != target) {
            int buffer = GL.GL_BACK;
            switch (target) {
                case Back:
                    break;
                case Front:
                    buffer = GL.GL_FRONT;
                    break;
                case BackLeft:
                    buffer = GL2GL3.GL_BACK_LEFT;
                    break;
                case BackRight:
                    buffer = GL2GL3.GL_BACK_RIGHT;
                    break;
                case FrontLeft:
                    buffer = GL2GL3.GL_FRONT_LEFT;
                    break;
                case FrontRight:
                    buffer = GL2GL3.GL_FRONT_RIGHT;
                    break;
                case FrontAndBack:
                    buffer = GL.GL_FRONT_AND_BACK;
                    break;
                case Left:
                    buffer = GL2GL3.GL_LEFT;
                    break;
                case Right:
                    buffer = GL2GL3.GL_RIGHT;
                    break;
                case None:
                    buffer = GL.GL_NONE;
                    break;
            }

            final GL gl = GLContext.getCurrentGL();
            if (gl.isGL2GL3()) {
                gl.getGL2GL3().glDrawBuffer(buffer);
            }
            record.setDrawBufferTarget(target);
        }
    }

    @Override
    public void setupLineParameters(final float lineWidth, final boolean antialiased) {
        final GL gl = GLContext.getCurrentGL();

        final LineRecord lineRecord = ContextManager.getCurrentContext().getLineRecord();

        if (!lineRecord.isValid() || lineRecord.width != lineWidth) {
            gl.glLineWidth(lineWidth);
            lineRecord.width = lineWidth;
        }

        if (antialiased) {
            if (!lineRecord.isValid() || !lineRecord.smoothed) {
                gl.glEnable(GL.GL_LINE_SMOOTH);
                lineRecord.smoothed = true;
            }
            if (!lineRecord.isValid() || lineRecord.smoothHint != GL.GL_NICEST) {
                gl.glHint(GL.GL_LINE_SMOOTH_HINT, GL.GL_NICEST);
                lineRecord.smoothHint = GL.GL_NICEST;
            }
        } else if (!lineRecord.isValid() || lineRecord.smoothed) {
            gl.glDisable(GL.GL_LINE_SMOOTH);
            lineRecord.smoothed = false;
        }

        if (!lineRecord.isValid()) {
            lineRecord.validate();
        }
    }

    @Override
    public void setupPointParameters(final float pointSize, final boolean antialiased, final boolean isSprite,
            final boolean useDistanceAttenuation, final FloatBuffer attenuationCoefficients, final float minPointSize,
            final float maxPointSize) {
        final RenderContext context = ContextManager.getCurrentContext();
        final GL gl = GLContext.getCurrentGL();

        // TODO: make this into a pointrecord call
        if (gl.isGL2GL3()) {
            gl.getGL2GL3().glPointSize(pointSize);
        }
        if (antialiased) {
            if (gl.isGL2ES1()) {
                gl.glEnable(GL2ES1.GL_POINT_SMOOTH);
                gl.glHint(GL2ES1.GL_POINT_SMOOTH_HINT, GL.GL_NICEST);
            }
        }

        if (isSprite) {
            if (gl.isGL2ES1()) {
                gl.glEnable(GL2ES1.GL_POINT_SPRITE);
                gl.getGL2ES1().glTexEnvi(GL2ES1.GL_POINT_SPRITE, GL2ES1.GL_COORD_REPLACE, GL.GL_TRUE);
            }
        }

        if (useDistanceAttenuation) {
            if (gl.isGL2GL3()) {
                gl.getGL2GL3().glPointParameterfv(GL2ES1.GL_POINT_DISTANCE_ATTENUATION, attenuationCoefficients);
                gl.getGL2GL3().glPointParameterf(GL2ES1.GL_POINT_SIZE_MIN, minPointSize);
                gl.getGL2GL3().glPointParameterf(GL2ES1.GL_POINT_SIZE_MAX, maxPointSize);
            }
        }
    }

    @Override
    protected void doApplyState(final RenderState state) {
        switch (state.getType()) {
            case Texture:
                JoglTextureStateUtil.apply(this, (TextureState) state);
                return;
            case Light:
                JoglLightStateUtil.apply(this, (LightState) state);
                return;
            case Blend:
                JoglBlendStateUtil.apply(this, (BlendState) state);
                return;
            case Clip:
                // JoglClipStateUtil.apply(this, (ClipState) state);
                return;
            case ColorMask:
                JoglColorMaskStateUtil.apply(this, (ColorMaskState) state);
                return;
            case Cull:
                JoglCullStateUtil.apply(this, (CullState) state);
                return;
            case Fog:
                // JoglFogStateUtil.apply(this, (FogState) state);
                return;
            case Shader:
                JoglShaderObjectsStateUtil.apply(this, (ShaderState) state);
                return;
            case Material:
                JoglMaterialStateUtil.apply(this, (MaterialState) state);
                return;
            case Offset:
                JoglOffsetStateUtil.apply(this, (OffsetState) state);
                return;
            case Shading:
                JoglShadingStateUtil.apply(this, (ShadingState) state);
                return;
            case Stencil:
                JoglStencilStateUtil.apply(this, (StencilState) state);
                return;
            case Wireframe:
                JoglWireframeStateUtil.apply(this, (WireframeState) state);
                return;
            case ZBuffer:
                JoglZBufferStateUtil.apply(this, (ZBufferState) state);
                return;
        }
        throw new IllegalArgumentException("Unknown state: " + state);
    }

    @Override
    public void deleteTexture(final Texture texture) {
        JoglTextureStateUtil.deleteTexture(texture);
    }

    @Override
    public void loadTexture(final Texture texture, final int unit) {
        JoglTextureStateUtil.load(texture, unit);
    }

    @Override
    public void deleteTextureIds(final Collection<Integer> ids) {
        JoglTextureStateUtil.deleteTextureIds(ids);
    }

    @Override
    public void clearClips() {
        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord record = context.getRendererRecord();
        record.getScissorClips().clear();

        JoglRendererUtil.applyScissors(record);
    }

    @Override
    public void popClip() {
        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord record = context.getRendererRecord();
        record.getScissorClips().pop();

        JoglRendererUtil.applyScissors(record);
    }

    @Override
    public void pushClip(final ReadOnlyRectangle2 rectangle) {
        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord record = context.getRendererRecord();
        record.getScissorClips().push(rectangle);

        JoglRendererUtil.applyScissors(record);
    }

    @Override
    public void pushEmptyClip() {
        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord record = context.getRendererRecord();
        record.getScissorClips().push(null);

        JoglRendererUtil.applyScissors(record);
    }

    @Override
    public void setClipTestEnabled(final boolean enabled) {
        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord record = context.getRendererRecord();

        JoglRendererUtil.setClippingEnabled(record, enabled);
    }

    public void checkAndSetTextureArrayUnit(final int unit, final GL gl, final RendererRecord record,
            final ContextCapabilities caps) {
        if (record.getCurrentTextureArraysUnit() != unit) {
            if (gl.isGL2ES1()) {
                gl.getGL2ES1().glClientActiveTexture(GL.GL_TEXTURE0 + unit);
            }
            record.setCurrentTextureArraysUnit(unit);
        }
    }
}