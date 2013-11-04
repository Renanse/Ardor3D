/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
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
import java.util.List;
import java.util.logging.Logger;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2ES1;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GL2ES3;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLException;
import javax.media.opengl.fixedfunc.GLLightingFunc;
import javax.media.opengl.fixedfunc.GLMatrixFunc;
import javax.media.opengl.fixedfunc.GLPointerFunc;
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
import com.ardor3d.math.type.ReadOnlyVector3;
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
import com.ardor3d.renderer.state.ClipState;
import com.ardor3d.renderer.state.ColorMaskState;
import com.ardor3d.renderer.state.CullState;
import com.ardor3d.renderer.state.FogState;
import com.ardor3d.renderer.state.FragmentProgramState;
import com.ardor3d.renderer.state.GLSLShaderObjectsState;
import com.ardor3d.renderer.state.LightState;
import com.ardor3d.renderer.state.MaterialState;
import com.ardor3d.renderer.state.OffsetState;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.ShadingState;
import com.ardor3d.renderer.state.StencilState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.VertexProgramState;
import com.ardor3d.renderer.state.WireframeState;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.renderer.state.record.LineRecord;
import com.ardor3d.renderer.state.record.RendererRecord;
import com.ardor3d.scene.state.jogl.JoglBlendStateUtil;
import com.ardor3d.scene.state.jogl.JoglClipStateUtil;
import com.ardor3d.scene.state.jogl.JoglColorMaskStateUtil;
import com.ardor3d.scene.state.jogl.JoglCullStateUtil;
import com.ardor3d.scene.state.jogl.JoglFogStateUtil;
import com.ardor3d.scene.state.jogl.JoglFragmentProgramStateUtil;
import com.ardor3d.scene.state.jogl.JoglLightStateUtil;
import com.ardor3d.scene.state.jogl.JoglMaterialStateUtil;
import com.ardor3d.scene.state.jogl.JoglOffsetStateUtil;
import com.ardor3d.scene.state.jogl.JoglShaderObjectsStateUtil;
import com.ardor3d.scene.state.jogl.JoglShadingStateUtil;
import com.ardor3d.scene.state.jogl.JoglStencilStateUtil;
import com.ardor3d.scene.state.jogl.JoglTextureStateUtil;
import com.ardor3d.scene.state.jogl.JoglVertexProgramStateUtil;
import com.ardor3d.scene.state.jogl.JoglWireframeStateUtil;
import com.ardor3d.scene.state.jogl.JoglZBufferStateUtil;
import com.ardor3d.scene.state.jogl.util.JoglRendererUtil;
import com.ardor3d.scene.state.jogl.util.JoglTextureUtil;
import com.ardor3d.scenegraph.AbstractBufferData;
import com.ardor3d.scenegraph.AbstractBufferData.VBOAccessMode;
import com.ardor3d.scenegraph.FloatBufferData;
import com.ardor3d.scenegraph.IndexBufferData;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Renderable;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.hint.NormalsMode;
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

        if ((buffers & Renderer.BUFFER_ACCUMULATION) != 0) {
            clear |= GL2.GL_ACCUM_BUFFER_BIT;
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
        JoglRendererUtil.switchMode(matRecord, GLMatrixFunc.GL_PROJECTION);
        matRecord.getMatrixBackend().pushMatrix();
        matRecord.getMatrixBackend().loadIdentity();

        final Camera camera = Camera.getCurrentCamera();
        final double viewportWidth = camera.getWidth() * (camera.getViewPortRight() - camera.getViewPortLeft());
        final double viewportHeight = camera.getHeight() * (camera.getViewPortTop() - camera.getViewPortBottom());
        matRecord.getMatrixBackend().setOrtho(0, viewportWidth, 0, viewportHeight, -1, 1);
        JoglRendererUtil.switchMode(matRecord, GLMatrixFunc.GL_MODELVIEW);
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
        JoglRendererUtil.switchMode(matRecord, GLMatrixFunc.GL_PROJECTION);
        matRecord.getMatrixBackend().popMatrix();
        JoglRendererUtil.switchMode(matRecord, GLMatrixFunc.GL_MODELVIEW);
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
    public void applyNormalsMode(final NormalsMode normalsMode, final ReadOnlyTransform worldTransform) {
        final GL gl = GLContext.getCurrentGL();
        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord rendRecord = context.getRendererRecord();
        if (normalsMode != NormalsMode.Off) {
            final ContextCapabilities caps = context.getCapabilities();
            switch (normalsMode) {
                case NormalizeIfScaled:
                    if (worldTransform.isRotationMatrix()) {
                        final ReadOnlyVector3 scale = worldTransform.getScale();
                        if (!(scale.getX() == 1.0 && scale.getY() == 1.0 && scale.getZ() == 1.0)) {
                            if (scale.getX() == scale.getY() && scale.getY() == scale.getZ()
                                    && caps.isOpenGL1_2Supported()
                                    && rendRecord.getNormalMode() != GL2ES1.GL_RESCALE_NORMAL) {
                                if (rendRecord.getNormalMode() == GLLightingFunc.GL_NORMALIZE) {
                                    gl.glDisable(GLLightingFunc.GL_NORMALIZE);
                                }
                                gl.glEnable(GL2ES1.GL_RESCALE_NORMAL);
                                rendRecord.setNormalMode(GL2ES1.GL_RESCALE_NORMAL);
                            } else if (rendRecord.getNormalMode() != GLLightingFunc.GL_NORMALIZE) {
                                if (rendRecord.getNormalMode() == GL2ES1.GL_RESCALE_NORMAL) {
                                    gl.glDisable(GL2ES1.GL_RESCALE_NORMAL);
                                }
                                gl.glEnable(GLLightingFunc.GL_NORMALIZE);
                                rendRecord.setNormalMode(GLLightingFunc.GL_NORMALIZE);
                            }
                        } else {
                            if (rendRecord.getNormalMode() == GL2ES1.GL_RESCALE_NORMAL) {
                                gl.glDisable(GL2ES1.GL_RESCALE_NORMAL);
                            } else if (rendRecord.getNormalMode() == GLLightingFunc.GL_NORMALIZE) {
                                gl.glDisable(GLLightingFunc.GL_NORMALIZE);
                            }
                            rendRecord.setNormalMode(GL.GL_ZERO);
                        }
                    } else {
                        if (!worldTransform.getMatrix().isIdentity()) {
                            // *might* be scaled...
                            if (rendRecord.getNormalMode() != GLLightingFunc.GL_NORMALIZE) {
                                if (rendRecord.getNormalMode() == GL2ES1.GL_RESCALE_NORMAL) {
                                    gl.glDisable(GL2ES1.GL_RESCALE_NORMAL);
                                }
                                gl.glEnable(GLLightingFunc.GL_NORMALIZE);
                                rendRecord.setNormalMode(GLLightingFunc.GL_NORMALIZE);
                            }
                        } else {
                            // not scaled
                            if (rendRecord.getNormalMode() == GL2ES1.GL_RESCALE_NORMAL) {
                                gl.glDisable(GL2ES1.GL_RESCALE_NORMAL);
                            } else if (rendRecord.getNormalMode() == GLLightingFunc.GL_NORMALIZE) {
                                gl.glDisable(GLLightingFunc.GL_NORMALIZE);
                            }
                            rendRecord.setNormalMode(GL.GL_ZERO);
                        }
                    }
                    break;
                case AlwaysNormalize:
                    if (rendRecord.getNormalMode() != GLLightingFunc.GL_NORMALIZE) {
                        if (rendRecord.getNormalMode() == GL2ES1.GL_RESCALE_NORMAL) {
                            gl.glDisable(GL2ES1.GL_RESCALE_NORMAL);
                        }
                        gl.glEnable(GLLightingFunc.GL_NORMALIZE);
                        rendRecord.setNormalMode(GLLightingFunc.GL_NORMALIZE);
                    }
                    break;
                case UseProvided:
                default:
                    if (rendRecord.getNormalMode() == GL2ES1.GL_RESCALE_NORMAL) {
                        gl.glDisable(GL2ES1.GL_RESCALE_NORMAL);
                    } else if (rendRecord.getNormalMode() == GLLightingFunc.GL_NORMALIZE) {
                        gl.glDisable(GLLightingFunc.GL_NORMALIZE);
                    }
                    rendRecord.setNormalMode(GL.GL_ZERO);
                    break;
            }
        } else {
            if (rendRecord.getNormalMode() == GL2ES1.GL_RESCALE_NORMAL) {
                gl.glDisable(GL2ES1.GL_RESCALE_NORMAL);
            } else if (rendRecord.getNormalMode() == GLLightingFunc.GL_NORMALIZE) {
                gl.glDisable(GLLightingFunc.GL_NORMALIZE);
            }
            rendRecord.setNormalMode(GL.GL_ZERO);
        }
    }

    @Override
    public void applyDefaultColor(final ReadOnlyColorRGBA defaultColor) {
        final GL gl = GLContext.getCurrentGL();
        if (gl.isGL2ES1()) {
            if (defaultColor != null) {
                gl.getGL2ES1().glColor4f(defaultColor.getRed(), defaultColor.getGreen(), defaultColor.getBlue(),
                        defaultColor.getAlpha());
            } else {
                gl.getGL2ES1().glColor4f(1, 1, 1, 1);
            }
        }
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
                    gl.glDeleteTextures(vboIdsBuffer.remaining(), vboIdsBuffer);
                }
                vboIdsBuffer.clear();
            }
            if (i != null && i != 0) {
                vboIdsBuffer.put(i);
            }
        }
        vboIdsBuffer.flip();
        if (vboIdsBuffer.remaining() > 0) {
            gl.glDeleteTextures(vboIdsBuffer.remaining(), vboIdsBuffer);
        }
        vboIdsBuffer.clear();
    }

    @Override
    public void deleteDisplayLists(final Collection<Integer> ids) {
        final GL gl = GLContext.getCurrentGL();
        for (final Integer i : ids) {
            if (i != null && i != 0) {
                gl.getGL2().glDeleteLists(i, 1);
            }
        }
    }

    @Override
    public void deleteVBOs(final AbstractBufferData<?> buffer) {
        if (buffer == null) {
            return;
        }

        final GL gl = GLContext.getCurrentGL();

        // ask for the current state record
        final JoglRenderContext context = (JoglRenderContext) ContextManager.getCurrentContext();

        final int id = buffer.getVBOID(context.getGlContextRep());
        if (id == 0) {
            // Not on card... return.
            return;
        }

        buffer.removeVBOID(context.getGlContextRep());

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
                JoglRendererUtil.switchMode(matRecord, GLMatrixFunc.GL_MODELVIEW);
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
        JoglRendererUtil.switchMode(matRecord, GLMatrixFunc.GL_MODELVIEW);
        matRecord.getMatrixBackend().popMatrix();
    }

    @Override
    public void setupVertexData(final FloatBufferData vertexBufferData) {
        final GL gl = GLContext.getCurrentGL();

        final FloatBuffer vertexBuffer = vertexBufferData != null ? vertexBufferData.getBuffer() : null;

        if (vertexBuffer == null) {
            if (gl.isGL2GL3()) {
                gl.getGL2GL3().glDisableClientState(GLPointerFunc.GL_VERTEX_ARRAY);
            }
        } else {
            if (gl.isGL2GL3()) {
                gl.getGL2GL3().glEnableClientState(GLPointerFunc.GL_VERTEX_ARRAY);
            }
            vertexBuffer.rewind();
            if (gl.isGL2ES1() && vertexBufferData != null) {
                gl.getGL2ES1().glVertexPointer(vertexBufferData.getValuesPerTuple(), GL.GL_FLOAT, 0, vertexBuffer);
            }
        }
    }

    @Override
    public void setupNormalData(final FloatBufferData normalBufferData) {
        final GL gl = GLContext.getCurrentGL();

        final FloatBuffer normalBuffer = normalBufferData != null ? normalBufferData.getBuffer() : null;

        if (normalBuffer == null) {
            if (gl.isGL2GL3()) {
                gl.getGL2GL3().glDisableClientState(GLPointerFunc.GL_NORMAL_ARRAY);
            }
        } else {
            if (gl.isGL2GL3()) {
                gl.getGL2GL3().glEnableClientState(GLPointerFunc.GL_NORMAL_ARRAY);
            }
            normalBuffer.rewind();
            if (gl.isGL2ES1()) {
                gl.getGL2ES1().glNormalPointer(GL.GL_FLOAT, 0, normalBuffer);
            }
        }
    }

    @Override
    public void setupColorData(final FloatBufferData colorBufferData) {
        final GL gl = GLContext.getCurrentGL();

        final FloatBuffer colorBuffer = colorBufferData != null ? colorBufferData.getBuffer() : null;

        if (colorBuffer == null) {
            if (gl.isGL2GL3()) {
                gl.getGL2GL3().glDisableClientState(GLPointerFunc.GL_COLOR_ARRAY);
            }
        } else {
            if (gl.isGL2GL3()) {
                gl.getGL2GL3().glEnableClientState(GLPointerFunc.GL_COLOR_ARRAY);
            }
            colorBuffer.rewind();
            if (gl.isGL2ES1() && colorBufferData != null) {
                gl.getGL2ES1().glColorPointer(colorBufferData.getValuesPerTuple(), GL.GL_FLOAT, 0, colorBuffer);
            }
        }
    }

    @Override
    public void setupFogData(final FloatBufferData fogBufferData) {
        final GL gl = GLContext.getCurrentGL();

        final FloatBuffer fogBuffer = fogBufferData != null ? fogBufferData.getBuffer() : null;

        if (gl.isGL2()) {
            if (fogBuffer == null) {
                gl.getGL2().glDisableClientState(GL2.GL_FOG_COORDINATE_ARRAY);
            } else {
                gl.getGL2().glEnableClientState(GL2.GL_FOG_COORDINATE_ARRAY);
                fogBuffer.rewind();
                gl.getGL2().glFogCoordPointer(GL.GL_FLOAT, 0, fogBuffer);
            }
        }

    }

    @Override
    public void setupTextureData(final List<FloatBufferData> textureCoords) {
        final GL gl = GLContext.getCurrentGL();

        final RenderContext context = ContextManager.getCurrentContext();
        final ContextCapabilities caps = context.getCapabilities();
        final RendererRecord rendRecord = context.getRendererRecord();

        final TextureState ts = (TextureState) context.getCurrentState(RenderState.StateType.Texture);
        int enabledTextures = rendRecord.getEnabledTextures();
        final boolean valid = rendRecord.isTexturesValid();
        boolean isOn, wasOn;
        if (ts != null) {
            final int max = caps.isMultitextureSupported() ? Math.min(caps.getNumberOfFragmentTexCoordUnits(),
                    TextureState.MAX_TEXTURES) : 1;
            for (int i = 0; i < max; i++) {
                wasOn = (enabledTextures & (2 << i)) != 0;
                isOn = textureCoords != null && i < textureCoords.size() && textureCoords.get(i) != null
                        && textureCoords.get(i).getBuffer() != null;

                if (!isOn) {
                    if (valid && !wasOn) {
                        continue;
                    } else {
                        checkAndSetTextureArrayUnit(i, gl, rendRecord, caps);

                        // disable bit in tracking int
                        enabledTextures &= ~(2 << i);

                        // disable state
                        if (gl.isGL2GL3()) {
                            gl.getGL2GL3().glDisableClientState(GLPointerFunc.GL_TEXTURE_COORD_ARRAY);
                        }

                        continue;
                    }
                } else {
                    checkAndSetTextureArrayUnit(i, gl, rendRecord, caps);

                    if (!valid || !wasOn) {
                        // enable state
                        if (gl.isGL2GL3()) {
                            gl.getGL2GL3().glEnableClientState(GLPointerFunc.GL_TEXTURE_COORD_ARRAY);
                        }

                        // enable bit in tracking int
                        enabledTextures |= (2 << i);
                    }

                    @SuppressWarnings("null")
                    final FloatBufferData textureBufferData = textureCoords.get(i);
                    final FloatBuffer textureBuffer = textureBufferData.getBuffer();

                    if (gl.isGL2GL3()) {
                        gl.getGL2GL3().glEnableClientState(GLPointerFunc.GL_TEXTURE_COORD_ARRAY);
                    }
                    textureBuffer.rewind();
                    if (gl.isGL2ES1()) {
                        gl.getGL2ES1().glTexCoordPointer(textureBufferData.getValuesPerTuple(), GL.GL_FLOAT, 0,
                                textureBuffer);
                    }
                }
            }
        }

        rendRecord.setEnabledTextures(enabledTextures);
        rendRecord.setTexturesValid(true);
    }

    @Override
    public void drawElements(final IndexBufferData<?> indices, final int[] indexLengths, final IndexMode[] indexModes,
            final int primcount) {
        if (indices == null || indices.getBuffer() == null) {
            logger.severe("Missing indices for drawElements call without VBO");
            return;
        }

        final GL gl = GLContext.getCurrentGL();

        final int type = getGLDataType(indices);
        if (indexLengths == null) {
            final int glIndexMode = getGLIndexMode(indexModes[0]);

            indices.position(0);

            if (gl.isGL2()) {
                if (primcount < 0) {
                    gl.getGL2().glDrawElements(glIndexMode, indices.getBufferLimit(), type, indices.getBuffer());
                } else {
                    gl.getGL2().glDrawElementsInstanced(glIndexMode, indices.getBufferLimit(), type,
                            indices.getBuffer(), primcount);
                }
            }

            if (Constants.stats) {
                addStats(indexModes[0], indices.getBufferLimit());
            }
        } else {
            int offset = 0;
            int indexModeCounter = 0;
            for (int i = 0; i < indexLengths.length; i++) {
                final int count = indexLengths[i];

                final int glIndexMode = getGLIndexMode(indexModes[indexModeCounter]);

                indices.getBuffer().position(offset);
                indices.getBuffer().limit(offset + count);

                if (gl.isGL2()) {
                    if (primcount < 0) {
                        gl.getGL2().glDrawElements(glIndexMode, count, type, indices.getBuffer());
                    } else {
                        gl.getGL2().glDrawElementsInstanced(glIndexMode, count, type, indices.getBuffer(), primcount);
                    }
                }

                if (Constants.stats) {
                    addStats(indexModes[indexModeCounter], count);
                }

                offset += count;

                if (indexModeCounter < indexModes.length - 1) {
                    indexModeCounter++;
                }
            }
        }
    }

    public static int setupVBO(final AbstractBufferData<? extends Buffer> data, final RenderContext context) {
        if (data == null) {
            return 0;
        }

        final GL gl = GLContext.getCurrentGL();

        final RendererRecord rendRecord = context.getRendererRecord();
        int vboID = data.getVBOID(context.getGlContextRep());
        if (vboID != 0) {
            updateVBO(data, rendRecord, vboID, 0);
            return vboID;
        }

        final Buffer dataBuffer = data.getBuffer();
        if (dataBuffer != null) {
            // XXX: should we be rewinding? Maybe make that the programmer's responsibility.
            dataBuffer.rewind();
            vboID = makeVBOId();
            data.setVBOID(context.getGlContextRep(), vboID);

            rendRecord.invalidateVBO();
            JoglRendererUtil.setBoundVBO(rendRecord, vboID);
            gl.glBufferData(GL.GL_ARRAY_BUFFER, dataBuffer.limit() * data.getByteCount(), dataBuffer,
                    getGLVBOAccessMode(data.getVboAccessMode()));
        } else {
            throw new Ardor3dException("Attempting to create a vbo id for an AbstractBufferData with no Buffer value.");
        }
        return vboID;
    }

    private static void updateVBO(final AbstractBufferData<? extends Buffer> data, final RendererRecord rendRecord,
            final int vboID, final int offsetBytes) {
        if (data.isNeedsRefresh()) {
            final GL gl = GLContext.getCurrentGL();
            final Buffer dataBuffer = data.getBuffer();
            dataBuffer.rewind();
            JoglRendererUtil.setBoundVBO(rendRecord, vboID);
            gl.glBufferSubData(GL.GL_ARRAY_BUFFER, offsetBytes, dataBuffer.limit() * data.getByteCount(), dataBuffer);
            data.setNeedsRefresh(false);
        }
    }

    private int setupIndicesVBO(final IndexBufferData<?> data, final RenderContext context,
            final RendererRecord rendRecord) {
        if (data == null) {
            return 0;
        }

        final GL gl = GLContext.getCurrentGL();

        int vboID = data.getVBOID(context.getGlContextRep());
        if (vboID != 0) {
            if (data.isNeedsRefresh()) {
                final Buffer dataBuffer = data.getBuffer();
                dataBuffer.rewind();
                JoglRendererUtil.setBoundElementVBO(rendRecord, vboID);
                gl.glBufferSubData(GL.GL_ELEMENT_ARRAY_BUFFER, 0, dataBuffer.limit() * data.getByteCount(), dataBuffer);
                data.setNeedsRefresh(false);
            }

            return vboID;
        }

        final Buffer dataBuffer = data.getBuffer();
        if (dataBuffer != null) {
            // XXX: should we be rewinding? Maybe make that the programmer's responsibility.
            dataBuffer.rewind();
            vboID = makeVBOId();
            data.setVBOID(context.getGlContextRep(), vboID);

            rendRecord.invalidateVBO();
            JoglRendererUtil.setBoundElementVBO(rendRecord, vboID);
            gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, dataBuffer.limit() * data.getByteCount(), dataBuffer,
                    getGLVBOAccessMode(data.getVboAccessMode()));
        } else {
            throw new Ardor3dException("Attempting to create a vbo id for a IndexBufferData with no Buffer value.");
        }
        return vboID;
    }

    @Override
    public void setupVertexDataVBO(final FloatBufferData data) {
        final GL gl = GLContext.getCurrentGL();

        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord rendRecord = context.getRendererRecord();

        final int vboID = setupVBO(data, context);

        if (vboID != 0) {
            if (gl.isGL2GL3()) {
                gl.getGL2GL3().glEnableClientState(GLPointerFunc.GL_VERTEX_ARRAY);
            }
            JoglRendererUtil.setBoundVBO(rendRecord, vboID);
            if (gl.isGL2ES1()) {
                gl.getGL2ES1().glVertexPointer(data.getValuesPerTuple(), GL.GL_FLOAT, 0, 0);
            }
        } else {
            if (gl.isGL2GL3()) {
                gl.getGL2GL3().glDisableClientState(GLPointerFunc.GL_VERTEX_ARRAY);
            }
        }
    }

    @Override
    public void setupNormalDataVBO(final FloatBufferData data) {
        final GL gl = GLContext.getCurrentGL();

        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord rendRecord = context.getRendererRecord();

        final int vboID = setupVBO(data, context);

        if (vboID != 0) {
            if (gl.isGL2GL3()) {
                gl.getGL2GL3().glEnableClientState(GLPointerFunc.GL_NORMAL_ARRAY);
            }
            JoglRendererUtil.setBoundVBO(rendRecord, vboID);
            if (gl.isGL2ES1()) {
                gl.getGL2ES1().glNormalPointer(GL.GL_FLOAT, 0, 0);
            }
        } else {
            if (gl.isGL2GL3()) {
                gl.getGL2GL3().glDisableClientState(GLPointerFunc.GL_NORMAL_ARRAY);
            }
        }
    }

    @Override
    public void setupColorDataVBO(final FloatBufferData data) {
        final GL gl = GLContext.getCurrentGL();

        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord rendRecord = context.getRendererRecord();

        final int vboID = setupVBO(data, context);

        if (vboID != 0) {
            if (gl.isGL2GL3()) {
                gl.getGL2GL3().glEnableClientState(GLPointerFunc.GL_COLOR_ARRAY);
            }
            JoglRendererUtil.setBoundVBO(rendRecord, vboID);
            if (gl.isGL2ES1()) {
                gl.getGL2ES1().glColorPointer(data.getValuesPerTuple(), GL.GL_FLOAT, 0, 0);
            }
        } else {
            if (gl.isGL2GL3()) {
                gl.getGL2GL3().glDisableClientState(GLPointerFunc.GL_COLOR_ARRAY);
            }
        }
    }

    @Override
    public void setupFogDataVBO(final FloatBufferData data) {
        final GL gl = GLContext.getCurrentGL();

        final RenderContext context = ContextManager.getCurrentContext();
        final ContextCapabilities caps = context.getCapabilities();

        if (!caps.isFogCoordinatesSupported()) {
            return;
        }

        final RendererRecord rendRecord = context.getRendererRecord();
        final int vboID = setupVBO(data, context);

        if (vboID != 0) {
            if (gl.isGL2GL3()) {
                gl.getGL2GL3().glEnableClientState(GL2.GL_FOG_COORDINATE_ARRAY);
            }
            JoglRendererUtil.setBoundVBO(rendRecord, vboID);
            if (gl.isGL2()) {
                gl.getGL2().glFogCoordPointer(GL.GL_FLOAT, 0, 0);
            }
        } else {
            if (gl.isGL2GL3()) {
                gl.getGL2GL3().glDisableClientState(GL2.GL_FOG_COORDINATE_ARRAY);
            }
        }
    }

    @Override
    public void setupTextureDataVBO(final List<FloatBufferData> textureCoords) {
        final GL gl = GLContext.getCurrentGL();

        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord rendRecord = context.getRendererRecord();
        final ContextCapabilities caps = context.getCapabilities();

        final TextureState ts = (TextureState) context.getCurrentState(RenderState.StateType.Texture);
        int enabledTextures = rendRecord.getEnabledTextures();
        final boolean valid = rendRecord.isTexturesValid();
        boolean exists, wasOn;
        if (ts != null) {
            final int max = caps.isMultitextureSupported() ? Math.min(caps.getNumberOfFragmentTexCoordUnits(),
                    TextureState.MAX_TEXTURES) : 1;
            for (int i = 0; i < max; i++) {
                wasOn = (enabledTextures & (2 << i)) != 0;
                exists = textureCoords != null && i < textureCoords.size();

                if (!exists) {
                    if (valid && !wasOn) {
                        continue;
                    } else {
                        checkAndSetTextureArrayUnit(i, gl, rendRecord, caps);

                        // disable bit in tracking int
                        enabledTextures &= ~(2 << i);

                        // disable state
                        if (gl.isGL2GL3()) {
                            gl.getGL2GL3().glDisableClientState(GLPointerFunc.GL_TEXTURE_COORD_ARRAY);
                        }

                        continue;
                    }
                } else {
                    checkAndSetTextureArrayUnit(i, gl, rendRecord, caps);

                    // grab a vboID and make sure it exists and is up to date.
                    @SuppressWarnings("null")
                    final FloatBufferData data = textureCoords.get(i);
                    final int vboID = setupVBO(data, context);

                    // Found good vbo
                    if (vboID != 0) {
                        if (!valid || !wasOn) {
                            // enable bit in tracking int
                            enabledTextures |= (2 << i);

                            // enable state
                            if (gl.isGL2GL3()) {
                                gl.getGL2GL3().glEnableClientState(GLPointerFunc.GL_TEXTURE_COORD_ARRAY);
                            }
                        }

                        // set our active vbo
                        JoglRendererUtil.setBoundVBO(rendRecord, vboID);

                        // send data
                        if (gl.isGL2ES1()) {
                            gl.getGL2ES1().glTexCoordPointer(data.getValuesPerTuple(), GL.GL_FLOAT, 0, 0);
                        }
                    }
                    // Not a good vbo, disable it.
                    else {
                        if (!valid || wasOn) {
                            // disable bit in tracking int
                            enabledTextures &= ~(2 << i);

                            // disable state
                            if (gl.isGL2GL3()) {
                                gl.getGL2GL3().glDisableClientState(GLPointerFunc.GL_TEXTURE_COORD_ARRAY);
                            }
                        }
                    }
                }
            }
        }

        rendRecord.setEnabledTextures(enabledTextures);
        rendRecord.setTexturesValid(true);
    }

    @Override
    public void setupInterleavedDataVBO(final FloatBufferData interleaved, final FloatBufferData vertexCoords,
            final FloatBufferData normalCoords, final FloatBufferData colorCoords,
            final List<FloatBufferData> textureCoords) {
        final GL gl = GLContext.getCurrentGL();

        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord rendRecord = context.getRendererRecord();
        final ContextCapabilities caps = context.getCapabilities();

        final int lengthBytes = getTotalInterleavedSize(context, vertexCoords, normalCoords, colorCoords, textureCoords);
        int currLengthBytes = 0;
        if (interleaved.getBufferLimit() > 0) {
            interleaved.getBuffer().rewind();
            currLengthBytes = Math.round(interleaved.getBuffer().get());
        }

        if (lengthBytes != currLengthBytes || interleaved.getVBOID(context.getGlContextRep()) == 0
                || interleaved.isNeedsRefresh()) {
            initializeInterleavedVBO(context, interleaved, vertexCoords, normalCoords, colorCoords, textureCoords,
                    lengthBytes);
        }

        final int vboID = interleaved.getVBOID(context.getGlContextRep());
        JoglRendererUtil.setBoundVBO(rendRecord, vboID);

        int offsetBytes = 0;

        if (normalCoords != null) {
            updateVBO(normalCoords, rendRecord, vboID, offsetBytes);
            if (gl.isGL2ES1()) {
                gl.getGL2ES1().glNormalPointer(GL.GL_FLOAT, 0, offsetBytes);
            }
            if (gl.isGL2GL3()) {
                gl.getGL2GL3().glEnableClientState(GLPointerFunc.GL_NORMAL_ARRAY);
            }
            offsetBytes += normalCoords.getBufferLimit() * 4;
        } else {
            if (gl.isGL2GL3()) {
                gl.getGL2GL3().glDisableClientState(GLPointerFunc.GL_NORMAL_ARRAY);
            }
        }

        if (colorCoords != null) {
            updateVBO(colorCoords, rendRecord, vboID, offsetBytes);
            if (gl.isGL2ES1()) {
                gl.getGL2ES1().glColorPointer(colorCoords.getValuesPerTuple(), GL.GL_FLOAT, 0, offsetBytes);
            }
            if (gl.isGL2GL3()) {
                gl.getGL2GL3().glEnableClientState(GLPointerFunc.GL_COLOR_ARRAY);
            }
            offsetBytes += colorCoords.getBufferLimit() * 4;
        } else {
            if (gl.isGL2GL3()) {
                gl.getGL2GL3().glDisableClientState(GLPointerFunc.GL_COLOR_ARRAY);
            }
        }

        if (textureCoords != null) {
            final TextureState ts = (TextureState) context.getCurrentState(RenderState.StateType.Texture);
            int enabledTextures = rendRecord.getEnabledTextures();
            final boolean valid = rendRecord.isTexturesValid();
            boolean exists, wasOn;
            if (ts != null) {
                final int max = caps.isMultitextureSupported() ? Math.min(caps.getNumberOfFragmentTexCoordUnits(),
                        TextureState.MAX_TEXTURES) : 1;
                for (int i = 0; i < max; i++) {
                    wasOn = (enabledTextures & (2 << i)) != 0;
                    exists = i < textureCoords.size() && textureCoords.get(i) != null
                            && i <= ts.getMaxTextureIndexUsed();

                    if (!exists) {
                        if (valid && !wasOn) {
                            continue;
                        } else {
                            checkAndSetTextureArrayUnit(i, gl, rendRecord, caps);

                            // disable bit in tracking int
                            enabledTextures &= ~(2 << i);

                            // disable state
                            if (gl.isGL2GL3()) {
                                gl.getGL2GL3().glDisableClientState(GLPointerFunc.GL_TEXTURE_COORD_ARRAY);
                            }

                            continue;
                        }

                    } else {
                        checkAndSetTextureArrayUnit(i, gl, rendRecord, caps);

                        // grab a vboID and make sure it exists and is up to date.
                        final FloatBufferData textureBufferData = textureCoords.get(i);
                        updateVBO(textureBufferData, rendRecord, vboID, offsetBytes);

                        if (!valid || !wasOn) {
                            // enable bit in tracking int
                            enabledTextures |= (2 << i);

                            // enable state
                            if (gl.isGL2GL3()) {
                                gl.getGL2GL3().glEnableClientState(GLPointerFunc.GL_TEXTURE_COORD_ARRAY);
                            }
                        }

                        // send data
                        if (gl.isGL2ES1()) {
                            gl.getGL2ES1().glTexCoordPointer(textureBufferData.getValuesPerTuple(), GL.GL_FLOAT, 0,
                                    offsetBytes);
                        }
                        offsetBytes += textureBufferData.getBufferLimit() * 4;
                    }
                }
            }

            rendRecord.setEnabledTextures(enabledTextures);
            rendRecord.setTexturesValid(true);
        }

        if (vertexCoords != null) {
            updateVBO(vertexCoords, rendRecord, vboID, offsetBytes);
            if (gl.isGL2ES1()) {
                gl.getGL2ES1().glVertexPointer(vertexCoords.getValuesPerTuple(), GL.GL_FLOAT, 0, offsetBytes);
            }
            if (gl.isGL2GL3()) {
                gl.getGL2GL3().glEnableClientState(GLPointerFunc.GL_VERTEX_ARRAY);
            }
        } else {
            if (gl.isGL2GL3()) {
                gl.getGL2GL3().glDisableClientState(GLPointerFunc.GL_VERTEX_ARRAY);
            }
        }
    }

    @SuppressWarnings("null")
    private void initializeInterleavedVBO(final RenderContext context, final FloatBufferData interleaved,
            final FloatBufferData vertexCoords, final FloatBufferData normalCoords, final FloatBufferData colorCoords,
            final List<FloatBufferData> textureCoords, final int bufferSize) {

        // keep around buffer size
        if (interleaved.getBufferCapacity() != 1) {
            final FloatBuffer buffer = BufferUtils.createFloatBufferOnHeap(1);
            interleaved.setBuffer(buffer);
        }
        interleaved.getBuffer().rewind();
        interleaved.getBuffer().put(bufferSize);

        final GL gl = GLContext.getCurrentGL();

        final RendererRecord rendRecord = context.getRendererRecord();
        final ContextCapabilities caps = context.getCapabilities();

        final int vboID = makeVBOId();
        interleaved.setVBOID(context.getGlContextRep(), vboID);

        rendRecord.invalidateVBO();
        JoglRendererUtil.setBoundVBO(rendRecord, vboID);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, bufferSize, null, getGLVBOAccessMode(interleaved.getVboAccessMode()));

        int offset = 0;
        if (normalCoords != null) {
            normalCoords.getBuffer().rewind();
            gl.glBufferSubData(GL.GL_ARRAY_BUFFER, offset, normalCoords.getBufferLimit() * 4, normalCoords.getBuffer());
            offset += normalCoords.getBufferLimit() * 4;
        }
        if (colorCoords != null) {
            colorCoords.getBuffer().rewind();
            gl.glBufferSubData(GL.GL_ARRAY_BUFFER, offset, colorCoords.getBufferLimit() * 4, colorCoords.getBuffer());
            offset += colorCoords.getBufferLimit() * 4;
        }
        if (textureCoords != null) {
            final TextureState ts = (TextureState) context.getCurrentState(RenderState.StateType.Texture);
            if (ts != null) {
                for (int i = 0; i <= ts.getMaxTextureIndexUsed() && i < caps.getNumberOfFragmentTexCoordUnits(); i++) {
                    if (i >= textureCoords.size()) {
                        continue;
                    }

                    final FloatBufferData textureBufferData = textureCoords.get(i);
                    final FloatBuffer textureBuffer = textureBufferData != null ? textureBufferData.getBuffer() : null;
                    if (textureBuffer != null) {
                        textureBuffer.rewind();
                        gl.glBufferSubData(GL.GL_ARRAY_BUFFER, offset, textureBufferData.getBufferLimit() * 4,
                                textureBuffer);
                        offset += textureBufferData.getBufferLimit() * 4;
                    }
                }
            }
        }
        if (vertexCoords != null) {
            vertexCoords.getBuffer().rewind();
            gl.glBufferSubData(GL.GL_ARRAY_BUFFER, offset, vertexCoords.getBufferLimit() * 4, vertexCoords.getBuffer());
        }

        interleaved.setNeedsRefresh(false);
    }

    @Override
    public void drawElementsVBO(final IndexBufferData<?> indices, final int[] indexLengths,
            final IndexMode[] indexModes, final int primcount) {
        final GL gl = GLContext.getCurrentGL();

        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord rendRecord = context.getRendererRecord();

        final int vboID = setupIndicesVBO(indices, context, rendRecord);

        JoglRendererUtil.setBoundElementVBO(rendRecord, vboID);

        if (indexLengths == null) {
            final int glIndexMode = getGLIndexMode(indexModes[0]);

            final int type = getGLDataType(indices);

            if (primcount < 0) {
                gl.glDrawElements(glIndexMode, indices.getBufferLimit(), type, 0);
            } else {
                if (gl.isGL2()) {
                    gl.getGL2().glDrawElementsInstanced(glIndexMode, indices.getBufferLimit(), type,
                            indices.getBuffer(), primcount);
                }
            }

            if (Constants.stats) {
                addStats(indexModes[0], indices.getBufferLimit());
            }
        } else {
            int offset = 0;
            int indexModeCounter = 0;
            for (int i = 0; i < indexLengths.length; i++) {
                final int count = indexLengths[i];

                final int glIndexMode = getGLIndexMode(indexModes[indexModeCounter]);

                final int type = getGLDataType(indices);
                final int byteSize = indices.getByteCount();
                // offset in this call is done in bytes.
                gl.glDrawElements(glIndexMode, count, type, offset * byteSize);

                if (primcount < 0) {
                    gl.glDrawElements(glIndexMode, count, type, offset * byteSize);
                } else {
                    final int previousPos = indices.getBuffer().position();
                    indices.getBuffer().position(offset * byteSize);
                    if (gl.isGL2()) {
                        gl.getGL2().glDrawElementsInstanced(glIndexMode, count, type, indices.getBuffer(), primcount);
                    }
                    indices.getBuffer().position(previousPos);
                }

                if (Constants.stats) {
                    addStats(indexModes[indexModeCounter], count);
                }

                offset += count;

                if (indexModeCounter < indexModes.length - 1) {
                    indexModeCounter++;
                }
            }
        }
    }

    @Override
    public void drawArrays(final FloatBufferData vertexBuffer, final int[] indexLengths, final IndexMode[] indexModes,
            final int primcount) {
        final GL gl = GLContext.getCurrentGL();

        if (indexLengths == null) {
            final int glIndexMode = getGLIndexMode(indexModes[0]);

            if (primcount < 0) {
                gl.glDrawArrays(glIndexMode, 0, vertexBuffer.getTupleCount());
            } else {
                if (gl.isGL2GL3()) {
                    gl.getGL2GL3().glDrawArraysInstanced(glIndexMode, 0, vertexBuffer.getTupleCount(), primcount);
                }
            }

            if (Constants.stats) {
                addStats(indexModes[0], vertexBuffer.getTupleCount());
            }
        } else {
            int offset = 0;
            int indexModeCounter = 0;
            for (int i = 0; i < indexLengths.length; i++) {
                final int count = indexLengths[i];

                final int glIndexMode = getGLIndexMode(indexModes[indexModeCounter]);

                if (primcount < 0) {
                    gl.glDrawArrays(glIndexMode, offset, count);
                } else {
                    if (gl.isGL2GL3()) {
                        gl.getGL2GL3().glDrawArraysInstanced(glIndexMode, offset, count, primcount);
                    }
                }

                if (Constants.stats) {
                    addStats(indexModes[indexModeCounter], count);
                }

                offset += count;

                if (indexModeCounter < indexModes.length - 1) {
                    indexModeCounter++;
                }
            }
        }
    }

    private static int makeVBOId() {
        final GL gl = GLContext.getCurrentGL();

        final JoglRenderContext context = (JoglRenderContext) ContextManager.getCurrentContext();

        final IntBuffer idBuff = context.getDirectNioBuffersSet().getSingleIntBuffer();
        idBuff.clear();
        gl.glGenBuffers(1, idBuff);
        return idBuff.get(0);
    }

    @Override
    public void unbindVBO() {
        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord rendRecord = context.getRendererRecord();
        JoglRendererUtil.setBoundVBO(rendRecord, 0);
        JoglRendererUtil.setBoundElementVBO(rendRecord, 0);
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
            case Quads:
                glMode = GL2GL3.GL_QUADS;
                break;
            case QuadStrip:
                glMode = GL2.GL_QUAD_STRIP;
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
        JoglRendererUtil.switchMode(matRecord, GLMatrixFunc.GL_MODELVIEW);

        loadMatrix(matrix);
    }

    @Override
    public void setProjectionMatrix(final FloatBuffer matrix) {
        final JoglRendererRecord matRecord = (JoglRendererRecord) ContextManager.getCurrentContext()
                .getRendererRecord();
        JoglRendererUtil.switchMode(matRecord, GLMatrixFunc.GL_PROJECTION);

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
                case None:
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
                case Aux0:
                    buffer = GL2.GL_AUX0;
                    break;
                case Aux1:
                    buffer = GL2.GL_AUX1;
                    break;
                case Aux2:
                    buffer = GL2.GL_AUX2;
                    break;
                case Aux3:
                    buffer = GL2.GL_AUX3;
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
    public void setupLineParameters(final float lineWidth, final int stippleFactor, final short stipplePattern,
            final boolean antialiased) {
        final GL gl = GLContext.getCurrentGL();

        final LineRecord lineRecord = ContextManager.getCurrentContext().getLineRecord();

        if (!lineRecord.isValid() || lineRecord.width != lineWidth) {
            gl.glLineWidth(lineWidth);
            lineRecord.width = lineWidth;
        }

        if (stipplePattern != (short) 0xFFFF) {
            if (!lineRecord.isValid() || !lineRecord.stippled) {
                gl.glEnable(GL2.GL_LINE_STIPPLE);
                lineRecord.stippled = true;
            }

            if (!lineRecord.isValid() || stippleFactor != lineRecord.stippleFactor
                    || stipplePattern != lineRecord.stipplePattern) {
                gl.getGL2().glLineStipple(stippleFactor, stipplePattern);
                lineRecord.stippleFactor = stippleFactor;
                lineRecord.stipplePattern = stipplePattern;
            }
        } else if (!lineRecord.isValid() || lineRecord.stippled) {
            gl.glDisable(GL2.GL_LINE_STIPPLE);
            lineRecord.stippled = false;
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

        if (isSprite && context.getCapabilities().isPointSpritesSupported()) {
            if (gl.isGL2ES1()) {
                gl.glEnable(GL2ES1.GL_POINT_SPRITE);
                gl.getGL2ES1().glTexEnvi(GL2ES1.GL_POINT_SPRITE, GL2ES1.GL_COORD_REPLACE, GL.GL_TRUE);
            }
        }

        if (useDistanceAttenuation && context.getCapabilities().isPointParametersSupported()) {
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
                JoglClipStateUtil.apply(this, (ClipState) state);
                return;
            case ColorMask:
                JoglColorMaskStateUtil.apply(this, (ColorMaskState) state);
                return;
            case Cull:
                JoglCullStateUtil.apply(this, (CullState) state);
                return;
            case Fog:
                JoglFogStateUtil.apply(this, (FogState) state);
                return;
            case FragmentProgram:
                JoglFragmentProgramStateUtil.apply(this, (FragmentProgramState) state);
                return;
            case GLSLShader:
                JoglShaderObjectsStateUtil.apply(this, (GLSLShaderObjectsState) state);
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
            case VertexProgram:
                JoglVertexProgramStateUtil.apply(this, (VertexProgramState) state);
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

    /**
     * Start a new display list. All further renderer commands that can be stored in a display list are part of this new
     * list until {@link #endDisplayList()} is called.
     * 
     * @return id of new display list
     */
    @Override
    public int startDisplayList() {
        final GL gl = GLContext.getCurrentGL();

        final int id = gl.getGL2().glGenLists(1);

        gl.getGL2().glNewList(id, GL2.GL_COMPILE);

        return id;
    }

    /**
     * Ends a display list. Will likely cause an OpenGL exception is a display list is not currently being generated.
     */
    @Override
    public void endDisplayList() {
        final GL gl = GLContext.getCurrentGL();
        gl.getGL2().glEndList();
    }

    /**
     * Draw the given display list.
     */
    @Override
    public void renderDisplayList(final int displayListID) {
        final GL gl = GLContext.getCurrentGL();

        gl.getGL2().glCallList(displayListID);
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
        if (record.getCurrentTextureArraysUnit() != unit && caps.isMultitextureSupported()) {
            if (gl.isGL2ES1()) {
                gl.getGL2ES1().glClientActiveTexture(GL.GL_TEXTURE0 + unit);
            }
            record.setCurrentTextureArraysUnit(unit);
        }
    }
}