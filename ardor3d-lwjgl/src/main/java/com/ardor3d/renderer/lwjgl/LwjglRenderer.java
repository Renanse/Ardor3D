/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.lwjgl;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import org.lwjgl.opengl.ARBBufferObject;
import org.lwjgl.opengl.ARBMultitexture;
import org.lwjgl.opengl.ARBPointParameters;
import org.lwjgl.opengl.ARBPointSprite;
import org.lwjgl.opengl.ARBVertexBufferObject;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.EXTFogCoord;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.OpenGLException;

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
import com.ardor3d.scene.state.lwjgl.LwjglBlendStateUtil;
import com.ardor3d.scene.state.lwjgl.LwjglClipStateUtil;
import com.ardor3d.scene.state.lwjgl.LwjglColorMaskStateUtil;
import com.ardor3d.scene.state.lwjgl.LwjglCullStateUtil;
import com.ardor3d.scene.state.lwjgl.LwjglFogStateUtil;
import com.ardor3d.scene.state.lwjgl.LwjglFragmentProgramStateUtil;
import com.ardor3d.scene.state.lwjgl.LwjglLightStateUtil;
import com.ardor3d.scene.state.lwjgl.LwjglMaterialStateUtil;
import com.ardor3d.scene.state.lwjgl.LwjglOffsetStateUtil;
import com.ardor3d.scene.state.lwjgl.LwjglShaderObjectsStateUtil;
import com.ardor3d.scene.state.lwjgl.LwjglShadingStateUtil;
import com.ardor3d.scene.state.lwjgl.LwjglStencilStateUtil;
import com.ardor3d.scene.state.lwjgl.LwjglTextureStateUtil;
import com.ardor3d.scene.state.lwjgl.LwjglVertexProgramStateUtil;
import com.ardor3d.scene.state.lwjgl.LwjglWireframeStateUtil;
import com.ardor3d.scene.state.lwjgl.LwjglZBufferStateUtil;
import com.ardor3d.scene.state.lwjgl.util.LwjglRendererUtil;
import com.ardor3d.scene.state.lwjgl.util.LwjglTextureUtil;
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

/**
 * <code>LwjglRenderer</code> provides an implementation of the <code>Renderer</code> interface using the LWJGL API.
 *
 * @see com.ardor3d.renderer.Renderer
 */
public class LwjglRenderer extends AbstractRenderer {
    private static final Logger logger = Logger.getLogger(LwjglRenderer.class.getName());

    private final FloatBuffer _transformBuffer = BufferUtils.createFloatBuffer(16);
    private final Matrix4 _transformMatrix = new Matrix4();

    /**
     * Constructor instantiates a new <code>LwjglRenderer</code> object.
     */
    public LwjglRenderer() {
        logger.fine("LwjglRenderer created.");
    }

    public void setBackgroundColor(final ReadOnlyColorRGBA color) {
        _backgroundColor.set(color);
        GL11.glClearColor(_backgroundColor.getRed(), _backgroundColor.getGreen(), _backgroundColor.getBlue(),
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
    public void clearQueue() {
        _queue.clearBuckets();
    }

    public void clearBuffers(final int buffers) {
        clearBuffers(buffers, false);
    }

    public void clearBuffers(final int buffers, final boolean strict) {

        int clear = 0;

        if ((buffers & Renderer.BUFFER_COLOR) != 0) {
            clear |= GL11.GL_COLOR_BUFFER_BIT;
        }

        if ((buffers & Renderer.BUFFER_DEPTH) != 0) {
            clear |= GL11.GL_DEPTH_BUFFER_BIT;

            // make sure no funny business is going on in the z before clearing.
            if (defaultStateList.containsKey(RenderState.StateType.ZBuffer)) {
                defaultStateList.get(RenderState.StateType.ZBuffer).setNeedsRefresh(true);
                doApplyState(defaultStateList.get(RenderState.StateType.ZBuffer));
            }
        }

        if ((buffers & Renderer.BUFFER_STENCIL) != 0) {
            clear |= GL11.GL_STENCIL_BUFFER_BIT;

            GL11.glClearStencil(_stencilClearValue);
            GL11.glStencilMask(~0);
            GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
        }

        if ((buffers & Renderer.BUFFER_ACCUMULATION) != 0) {
            clear |= GL11.GL_ACCUM_BUFFER_BIT;
        }

        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord record = context.getRendererRecord();

        if (strict) {
            // grab our camera to get width and height info.
            final Camera cam = Camera.getCurrentCamera();

            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor(0, 0, cam.getWidth(), cam.getHeight());
            record.setClippingTestEnabled(true);
        }

        GL11.glClear(clear);

        if (strict) {
            // put us back.
            LwjglRendererUtil.applyScissors(record);
        }
    }

    public void flushFrame(final boolean doSwap) {
        renderBuckets();

        GL11.glFlush();
        if (doSwap) {
            doApplyState(defaultStateList.get(RenderState.StateType.ColorMask));

            if (Constants.stats) {
                StatCollector.startStat(StatType.STAT_DISPLAYSWAP_TIMER);
            }
            Display.update();
            if (Constants.stats) {
                StatCollector.endStat(StatType.STAT_DISPLAYSWAP_TIMER);
            }
        }

        if (Constants.stats) {
            StatCollector.addStat(StatType.STAT_FRAMES, 1);
        }
    }

    public void setOrtho() {
        if (_inOrthoMode) {
            throw new Ardor3dException("Already in Orthographic mode.");
        }
        // set up ortho mode
        final RendererRecord matRecord = ContextManager.getCurrentContext().getRendererRecord();
        LwjglRendererUtil.switchMode(matRecord, GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        final Camera camera = Camera.getCurrentCamera();
        final double viewportWidth = camera.getWidth() * (camera.getViewPortRight() - camera.getViewPortLeft());
        final double viewportHeight = camera.getHeight() * (camera.getViewPortTop() - camera.getViewPortBottom());
        GL11.glOrtho(0, viewportWidth, 0, viewportHeight, -1, 1);
        LwjglRendererUtil.switchMode(matRecord, GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        _inOrthoMode = true;
    }

    public void unsetOrtho() {
        if (!_inOrthoMode) {
            throw new Ardor3dException("Not in Orthographic mode.");
        }
        // remove ortho mode, and go back to original
        // state
        final RendererRecord matRecord = ContextManager.getCurrentContext().getRendererRecord();
        LwjglRendererUtil.switchMode(matRecord, GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        LwjglRendererUtil.switchMode(matRecord, GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
        _inOrthoMode = false;
    }

    public void grabScreenContents(final ByteBuffer buff, final ImageDataFormat format, final PixelDataType type,
            final int x, final int y, final int w, final int h) {
        final int pixFormat = LwjglTextureUtil.getGLPixelFormat(format);
        final int pixDataType = LwjglTextureUtil.getGLPixelDataType(type);
        GL11.glReadPixels(x, y, w, h, pixFormat, pixDataType, buff);
    }

    public void draw(final Spatial s) {
        if (s != null) {
            s.onDraw(this);
        }
    }

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

    public void flushGraphics() {
        GL11.glFlush();
    }

    public void finishGraphics() {
        GL11.glFinish();
    }

    public void applyNormalsMode(final NormalsMode normalsMode, final ReadOnlyTransform worldTransform) {
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
                                    && rendRecord.getNormalMode() != GL12.GL_RESCALE_NORMAL) {
                                if (rendRecord.getNormalMode() == GL11.GL_NORMALIZE) {
                                    GL11.glDisable(GL11.GL_NORMALIZE);
                                }
                                GL11.glEnable(GL12.GL_RESCALE_NORMAL);
                                rendRecord.setNormalMode(GL12.GL_RESCALE_NORMAL);
                            } else if (rendRecord.getNormalMode() != GL11.GL_NORMALIZE) {
                                if (rendRecord.getNormalMode() == GL12.GL_RESCALE_NORMAL) {
                                    GL11.glDisable(GL12.GL_RESCALE_NORMAL);
                                }
                                GL11.glEnable(GL11.GL_NORMALIZE);
                                rendRecord.setNormalMode(GL11.GL_NORMALIZE);
                            }
                        } else {
                            if (rendRecord.getNormalMode() == GL12.GL_RESCALE_NORMAL) {
                                GL11.glDisable(GL12.GL_RESCALE_NORMAL);
                            } else if (rendRecord.getNormalMode() == GL11.GL_NORMALIZE) {
                                GL11.glDisable(GL11.GL_NORMALIZE);
                            }
                            rendRecord.setNormalMode(GL11.GL_ZERO);
                        }
                    } else {
                        if (!worldTransform.getMatrix().isIdentity()) {
                            // *might* be scaled...
                            if (rendRecord.getNormalMode() != GL11.GL_NORMALIZE) {
                                if (rendRecord.getNormalMode() == GL12.GL_RESCALE_NORMAL) {
                                    GL11.glDisable(GL12.GL_RESCALE_NORMAL);
                                }
                                GL11.glEnable(GL11.GL_NORMALIZE);
                                rendRecord.setNormalMode(GL11.GL_NORMALIZE);
                            }
                        } else {
                            // not scaled
                            if (rendRecord.getNormalMode() == GL12.GL_RESCALE_NORMAL) {
                                GL11.glDisable(GL12.GL_RESCALE_NORMAL);
                            } else if (rendRecord.getNormalMode() == GL11.GL_NORMALIZE) {
                                GL11.glDisable(GL11.GL_NORMALIZE);
                            }
                            rendRecord.setNormalMode(GL11.GL_ZERO);
                        }
                    }
                    break;
                case AlwaysNormalize:
                    if (rendRecord.getNormalMode() != GL11.GL_NORMALIZE) {
                        if (rendRecord.getNormalMode() == GL12.GL_RESCALE_NORMAL) {
                            GL11.glDisable(GL12.GL_RESCALE_NORMAL);
                        }
                        GL11.glEnable(GL11.GL_NORMALIZE);
                        rendRecord.setNormalMode(GL11.GL_NORMALIZE);
                    }
                    break;
                case UseProvided:
                default:
                    if (rendRecord.getNormalMode() == GL12.GL_RESCALE_NORMAL) {
                        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
                    } else if (rendRecord.getNormalMode() == GL11.GL_NORMALIZE) {
                        GL11.glDisable(GL11.GL_NORMALIZE);
                    }
                    rendRecord.setNormalMode(GL11.GL_ZERO);
                    break;
            }
        } else {
            if (rendRecord.getNormalMode() == GL12.GL_RESCALE_NORMAL) {
                GL11.glDisable(GL12.GL_RESCALE_NORMAL);
            } else if (rendRecord.getNormalMode() == GL11.GL_NORMALIZE) {
                GL11.glDisable(GL11.GL_NORMALIZE);
            }
            rendRecord.setNormalMode(GL11.GL_ZERO);
        }
    }

    public void applyDefaultColor(final ReadOnlyColorRGBA defaultColor) {
        if (defaultColor != null) {
            GL11.glColor4f(defaultColor.getRed(), defaultColor.getGreen(), defaultColor.getBlue(),
                    defaultColor.getAlpha());
        } else {
            GL11.glColor4f(1, 1, 1, 1);
        }
    }

    public void deleteVBOs(final Collection<Integer> ids) {
        final IntBuffer idBuffer = BufferUtils.createIntBuffer(ids.size());
        idBuffer.clear();
        for (final Integer i : ids) {
            if (i != null && i != 0) {
                idBuffer.put(i);
            }
        }
        idBuffer.flip();
        if (idBuffer.remaining() > 0) {
            ARBBufferObject.glDeleteBuffersARB(idBuffer);
        }
    }

    public void deleteVBOs(final AbstractBufferData<?> buffer) {
        if (buffer == null) {
            return;
        }

        // ask for the current state record
        final RenderContext context = ContextManager.getCurrentContext();

        final int id = buffer.getVBOID(context.getGlContextRep());
        if (id == 0) {
            // Not on card... return.
            return;
        }

        buffer.removeVBOID(context.getGlContextRep());

        final IntBuffer idBuff = BufferUtils.createIntBuffer(1);
        idBuff.put(id);
        idBuff.flip();
        ARBBufferObject.glDeleteBuffersARB(idBuff);
    }

    public void updateTexture1DSubImage(final Texture1D destination, final int dstOffsetX, final int dstWidth,
            final ByteBuffer source, final int srcOffsetX) {
        updateTexSubImage(destination, dstOffsetX, 0, 0, dstWidth, 0, 0, source, srcOffsetX, 0, 0, 0, 0, null);
    }

    public void updateTexture2DSubImage(final Texture2D destination, final int dstOffsetX, final int dstOffsetY,
            final int dstWidth, final int dstHeight, final ByteBuffer source, final int srcOffsetX,
            final int srcOffsetY, final int srcTotalWidth) {
        updateTexSubImage(destination, dstOffsetX, dstOffsetY, 0, dstWidth, dstHeight, 0, source, srcOffsetX,
                srcOffsetY, 0, srcTotalWidth, 0, null);
    }

    public void updateTexture3DSubImage(final Texture3D destination, final int dstOffsetX, final int dstOffsetY,
            final int dstOffsetZ, final int dstWidth, final int dstHeight, final int dstDepth, final ByteBuffer source,
            final int srcOffsetX, final int srcOffsetY, final int srcOffsetZ, final int srcTotalWidth,
            final int srcTotalHeight) {
        updateTexSubImage(destination, dstOffsetX, dstOffsetY, dstOffsetZ, dstWidth, dstHeight, dstDepth, source,
                srcOffsetX, srcOffsetY, srcOffsetZ, srcTotalWidth, srcTotalHeight, null);
    }

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

        // Ignore textures that do not have an id set
        if (destination.getTextureIdForContext(ContextManager.getCurrentContext().getGlContextRep()) == 0) {
            logger.warning("Attempting to update a texture that is not currently on the card.");
            return;
        }

        // Determine the original texture configuration, so that this method can
        // restore the texture configuration to its original state.
        final IntBuffer idBuff = BufferUtils.createIntBuffer(16);
        GL11.glGetInteger(GL11.GL_UNPACK_ALIGNMENT, idBuff);
        final int origAlignment = idBuff.get(0);
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
            pixelFormat = LwjglTextureUtil.getGLPixelFormat(destination.getImage().getDataFormat());
        } else {
            pixelFormat = LwjglTextureUtil.getGLPixelFormatFromStoreFormat(destination.getTextureStoreFormat());
        }

        // bind...
        LwjglTextureStateUtil.doTextureBind(destination, 0, false);

        // Update the texture configuration (when necessary).

        if (origAlignment != alignment) {
            GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, alignment);
        }
        if (origRowLength != rowLength) {
            GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, rowLength);
        }
        if (origSkipPixels != srcOffsetX) {
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, srcOffsetX);
        }
        // NOTE: The below will be skipped for texture types that don't support them because we are passing in 0's.
        if (origSkipRows != srcOffsetY) {
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, srcOffsetY);
        }
        if (origImageHeight != imageHeight) {
            GL11.glPixelStorei(GL12.GL_UNPACK_IMAGE_HEIGHT, imageHeight);
        }
        if (origSkipImages != srcOffsetZ) {
            GL11.glPixelStorei(GL12.GL_UNPACK_SKIP_IMAGES, srcOffsetZ);
        }

        // Upload the image region into the texture.
        try {
            switch (destination.getType()) {
                case TwoDimensional:
                    GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, dstOffsetX, dstOffsetY, dstWidth, dstHeight,
                            pixelFormat, GL11.GL_UNSIGNED_BYTE, source);
                    break;
                case OneDimensional:
                    GL11.glTexSubImage1D(GL11.GL_TEXTURE_1D, 0, dstOffsetX, dstWidth, pixelFormat,
                            GL11.GL_UNSIGNED_BYTE, source);
                    break;
                case ThreeDimensional:
                    GL12.glTexSubImage3D(GL12.GL_TEXTURE_3D, 0, dstOffsetX, dstOffsetY, dstOffsetZ, dstWidth,
                            dstHeight, dstDepth, pixelFormat, GL11.GL_UNSIGNED_BYTE, source);
                    break;
                case CubeMap:
                    GL11.glTexSubImage2D(LwjglTextureStateUtil.getGLCubeMapFace(dstFace), 0, dstOffsetX, dstOffsetY,
                            dstWidth, dstHeight, pixelFormat, GL11.GL_UNSIGNED_BYTE, source);
                    break;
                default:
                    throw new Ardor3dException("Unsupported type for updateTextureSubImage: " + destination.getType());
            }
        } finally {
            // Restore the texture configuration (when necessary)...
            // Restore alignment.
            if (origAlignment != alignment) {
                GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, origAlignment);
            }
            // Restore row length.
            if (origRowLength != rowLength) {
                GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, origRowLength);
            }
            // Restore skip pixels.
            if (origSkipPixels != srcOffsetX) {
                GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, origSkipPixels);
            }
            // Restore skip rows.
            if (origSkipRows != srcOffsetY) {
                GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, origSkipRows);
            }
            // Restore image height.
            if (origImageHeight != imageHeight) {
                GL11.glPixelStorei(GL12.GL_UNPACK_IMAGE_HEIGHT, origImageHeight);
            }
            // Restore skip images.
            if (origSkipImages != srcOffsetZ) {
                GL11.glPixelStorei(GL12.GL_UNPACK_SKIP_IMAGES, origSkipImages);
            }
        }
    }

    public void checkCardError() throws Ardor3dException {
        try {
            org.lwjgl.opengl.Util.checkGLError();
        } catch (final OpenGLException exception) {
            throw new Ardor3dException("Error in opengl: " + exception.getMessage(), exception);
        }
    }

    public void draw(final Renderable renderable) {
        if (renderLogic != null) {
            renderLogic.apply(renderable);
        }
        renderable.render(this);
        if (renderLogic != null) {
            renderLogic.restore(renderable);
        }
    }

    public boolean doTransforms(final ReadOnlyTransform transform) {
        // set world matrix
        if (!transform.isIdentity()) {
            synchronized (_transformMatrix) {
                transform.getGLApplyMatrix(_transformBuffer);

                final RendererRecord matRecord = ContextManager.getCurrentContext().getRendererRecord();
                LwjglRendererUtil.switchMode(matRecord, GL11.GL_MODELVIEW);
                GL11.glPushMatrix();
                GL11.glMultMatrix(_transformBuffer);
                return true;
            }
        }
        return false;
    }

    public void undoTransforms(final ReadOnlyTransform transform) {
        final RendererRecord matRecord = ContextManager.getCurrentContext().getRendererRecord();
        LwjglRendererUtil.switchMode(matRecord, GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
    }

    public void setupVertexData(final FloatBufferData vertexBufferData) {
        final FloatBuffer vertexBuffer = vertexBufferData != null ? vertexBufferData.getBuffer() : null;

        if (vertexBuffer == null) {
            GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
        } else {
            GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
            vertexBuffer.rewind();
            GL11.glVertexPointer(vertexBufferData.getValuesPerTuple(), 0, vertexBuffer);
        }
    }

    public void setupNormalData(final FloatBufferData normalBufferData) {
        final FloatBuffer normalBuffer = normalBufferData != null ? normalBufferData.getBuffer() : null;

        if (normalBuffer == null) {
            GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
        } else {
            GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
            normalBuffer.rewind();
            GL11.glNormalPointer(0, normalBuffer);
        }
    }

    public void setupColorData(final FloatBufferData colorBufferData) {
        final FloatBuffer colorBuffer = colorBufferData != null ? colorBufferData.getBuffer() : null;

        if (colorBuffer == null) {
            GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);
        } else {
            GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
            colorBuffer.rewind();
            GL11.glColorPointer(colorBufferData.getValuesPerTuple(), 0, colorBuffer);
        }
    }

    public void setupFogData(final FloatBufferData fogBufferData) {
        final FloatBuffer fogBuffer = fogBufferData != null ? fogBufferData.getBuffer() : null;

        if (fogBuffer == null) {
            GL11.glDisableClientState(EXTFogCoord.GL_FOG_COORDINATE_ARRAY_EXT);
        } else {
            GL11.glEnableClientState(EXTFogCoord.GL_FOG_COORDINATE_ARRAY_EXT);
            fogBuffer.rewind();
            EXTFogCoord.glFogCoordPointerEXT(0, fogBuffer);
        }
    }

    public void setupTextureData(final List<FloatBufferData> textureCoords) {
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
                        checkAndSetTextureArrayUnit(i, rendRecord, caps);

                        // disable bit in tracking int
                        enabledTextures &= ~(2 << i);

                        // disable state
                        GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);

                        continue;
                    }
                } else {
                    checkAndSetTextureArrayUnit(i, rendRecord, caps);

                    if (!valid || !wasOn) {
                        // enable state
                        GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);

                        // enable bit in tracking int
                        enabledTextures |= (2 << i);
                    }

                    final FloatBufferData textureBufferData = textureCoords.get(i);
                    final FloatBuffer textureBuffer = textureBufferData.getBuffer();

                    GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
                    textureBuffer.rewind();
                    GL11.glTexCoordPointer(textureBufferData.getValuesPerTuple(), 0, textureBuffer);
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

        if (indexLengths == null) {
            final int glIndexMode = getGLIndexMode(indexModes[0]);

            indices.position(0);
            if (indices.getBuffer() instanceof IntBuffer) {
                if (primcount < 0) {
                    GL11.glDrawElements(glIndexMode, (IntBuffer) indices.getBuffer());
                } else {
                    GL31.glDrawElementsInstanced(glIndexMode, (IntBuffer) indices.getBuffer(), primcount);
                }
            } else if (indices.getBuffer() instanceof ShortBuffer) {
                if (primcount < 0) {
                    GL11.glDrawElements(glIndexMode, (ShortBuffer) indices.getBuffer());
                } else {
                    GL31.glDrawElementsInstanced(glIndexMode, (ShortBuffer) indices.getBuffer(), primcount);
                }

            } else if (indices.getBuffer() instanceof ByteBuffer) {
                if (primcount < 0) {
                    GL11.glDrawElements(glIndexMode, (ByteBuffer) indices.getBuffer());
                } else {
                    GL31.glDrawElementsInstanced(glIndexMode, (ByteBuffer) indices.getBuffer(), primcount);
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
                if (indices.getBuffer() instanceof IntBuffer) {
                    if (primcount < 0) {
                        GL11.glDrawElements(glIndexMode, (IntBuffer) indices.getBuffer());
                    } else {
                        GL31.glDrawElementsInstanced(glIndexMode, (IntBuffer) indices.getBuffer(), primcount);
                    }

                } else if (indices.getBuffer() instanceof ShortBuffer) {
                    if (primcount < 0) {
                        GL11.glDrawElements(glIndexMode, (ShortBuffer) indices.getBuffer());
                    } else {
                        GL31.glDrawElementsInstanced(glIndexMode, (ShortBuffer) indices.getBuffer(), primcount);
                    }

                } else if (indices.getBuffer() instanceof ByteBuffer) {
                    if (primcount < 0) {
                        GL11.glDrawElements(glIndexMode, (ByteBuffer) indices.getBuffer());
                    } else {
                        GL31.glDrawElementsInstanced(glIndexMode, (ByteBuffer) indices.getBuffer(), primcount);
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
            LwjglRendererUtil.setBoundVBO(rendRecord, vboID);
            if (dataBuffer instanceof FloatBuffer) {
                ARBBufferObject.glBufferDataARB(ARBVertexBufferObject.GL_ARRAY_BUFFER_ARB, (FloatBuffer) dataBuffer,
                        getGLVBOAccessMode(data.getVboAccessMode()));
            } else if (dataBuffer instanceof ByteBuffer) {
                ARBBufferObject.glBufferDataARB(ARBVertexBufferObject.GL_ARRAY_BUFFER_ARB, (ByteBuffer) dataBuffer,
                        getGLVBOAccessMode(data.getVboAccessMode()));
            } else if (dataBuffer instanceof IntBuffer) {
                ARBBufferObject.glBufferDataARB(ARBVertexBufferObject.GL_ARRAY_BUFFER_ARB, (IntBuffer) dataBuffer,
                        getGLVBOAccessMode(data.getVboAccessMode()));
            } else if (dataBuffer instanceof ShortBuffer) {
                ARBBufferObject.glBufferDataARB(ARBVertexBufferObject.GL_ARRAY_BUFFER_ARB, (ShortBuffer) dataBuffer,
                        getGLVBOAccessMode(data.getVboAccessMode()));
            }
        } else {
            throw new Ardor3dException("Attempting to create a vbo id for a FloatBufferData with no Buffer value.");
        }
        return vboID;
    }

    public static void updateVBO(final AbstractBufferData<? extends Buffer> data, final RendererRecord rendRecord,
            final int vboID, final int offsetBytes) {
        if (data.isNeedsRefresh()) {
            final Buffer dataBuffer = data.getBuffer();
            dataBuffer.rewind();
            LwjglRendererUtil.setBoundVBO(rendRecord, vboID);
            if (dataBuffer instanceof FloatBuffer) {
                ARBBufferObject.glBufferSubDataARB(ARBVertexBufferObject.GL_ARRAY_BUFFER_ARB, offsetBytes,
                        (FloatBuffer) dataBuffer);
            } else if (dataBuffer instanceof ByteBuffer) {
                ARBBufferObject.glBufferSubDataARB(ARBVertexBufferObject.GL_ARRAY_BUFFER_ARB, offsetBytes,
                        (ByteBuffer) dataBuffer);
            } else if (dataBuffer instanceof IntBuffer) {
                ARBBufferObject.glBufferSubDataARB(ARBVertexBufferObject.GL_ARRAY_BUFFER_ARB, offsetBytes,
                        (IntBuffer) dataBuffer);
            } else if (dataBuffer instanceof ShortBuffer) {
                ARBBufferObject.glBufferSubDataARB(ARBVertexBufferObject.GL_ARRAY_BUFFER_ARB, offsetBytes,
                        (ShortBuffer) dataBuffer);
            }
            data.setNeedsRefresh(false);
        }
    }

    private int setupIndicesVBO(final IndexBufferData<?> data, final RenderContext context,
            final RendererRecord rendRecord) {
        if (data == null) {
            return 0;
        }

        int vboID = data.getVBOID(context.getGlContextRep());
        if (vboID != 0) {
            if (data.isNeedsRefresh()) {
                final Buffer dataBuffer = data.getBuffer();
                dataBuffer.rewind();
                LwjglRendererUtil.setBoundElementVBO(rendRecord, vboID);
                if (dataBuffer instanceof IntBuffer) {
                    ARBBufferObject.glBufferSubDataARB(ARBVertexBufferObject.GL_ELEMENT_ARRAY_BUFFER_ARB, 0,
                            (IntBuffer) dataBuffer);
                } else if (dataBuffer instanceof ShortBuffer) {
                    ARBBufferObject.glBufferSubDataARB(ARBVertexBufferObject.GL_ELEMENT_ARRAY_BUFFER_ARB, 0,
                            (ShortBuffer) dataBuffer);
                } else if (dataBuffer instanceof ByteBuffer) {
                    ARBBufferObject.glBufferSubDataARB(ARBVertexBufferObject.GL_ELEMENT_ARRAY_BUFFER_ARB, 0,
                            (ByteBuffer) dataBuffer);
                }
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
            LwjglRendererUtil.setBoundElementVBO(rendRecord, vboID);
            if (dataBuffer instanceof IntBuffer) {
                ARBBufferObject.glBufferDataARB(ARBVertexBufferObject.GL_ELEMENT_ARRAY_BUFFER_ARB,
                        (IntBuffer) dataBuffer, getGLVBOAccessMode(data.getVboAccessMode()));
            } else if (dataBuffer instanceof ShortBuffer) {
                ARBBufferObject.glBufferDataARB(ARBVertexBufferObject.GL_ELEMENT_ARRAY_BUFFER_ARB,
                        (ShortBuffer) dataBuffer, getGLVBOAccessMode(data.getVboAccessMode()));
            } else if (dataBuffer instanceof ByteBuffer) {
                ARBBufferObject.glBufferDataARB(ARBVertexBufferObject.GL_ELEMENT_ARRAY_BUFFER_ARB,
                        (ByteBuffer) dataBuffer, getGLVBOAccessMode(data.getVboAccessMode()));
            }
        } else {
            throw new Ardor3dException("Attempting to create a vbo id for a IndexBufferData with no Buffer value.");
        }
        return vboID;
    }

    public void setupVertexDataVBO(final FloatBufferData data) {
        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord rendRecord = context.getRendererRecord();

        final int vboID = setupVBO(data, context);

        if (vboID != 0) {
            GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
            LwjglRendererUtil.setBoundVBO(rendRecord, vboID);
            GL11.glVertexPointer(data.getValuesPerTuple(), GL11.GL_FLOAT, 0, 0);
        } else {
            GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
        }
    }

    public void setupNormalDataVBO(final FloatBufferData data) {
        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord rendRecord = context.getRendererRecord();

        final int vboID = setupVBO(data, context);

        if (vboID != 0) {
            GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
            LwjglRendererUtil.setBoundVBO(rendRecord, vboID);
            GL11.glNormalPointer(GL11.GL_FLOAT, 0, 0);
        } else {
            GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
        }
    }

    public void setupColorDataVBO(final FloatBufferData data) {
        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord rendRecord = context.getRendererRecord();

        final int vboID = setupVBO(data, context);

        if (vboID != 0) {
            GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
            LwjglRendererUtil.setBoundVBO(rendRecord, vboID);
            GL11.glColorPointer(data.getValuesPerTuple(), GL11.GL_FLOAT, 0, 0);
        } else {
            GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);
        }
    }

    public void setupFogDataVBO(final FloatBufferData data) {
        final RenderContext context = ContextManager.getCurrentContext();
        final ContextCapabilities caps = context.getCapabilities();

        if (!caps.isFogCoordinatesSupported()) {
            return;
        }

        final RendererRecord rendRecord = context.getRendererRecord();
        final int vboID = setupVBO(data, context);

        if (vboID != 0) {
            GL11.glEnableClientState(EXTFogCoord.GL_FOG_COORDINATE_ARRAY_EXT);
            LwjglRendererUtil.setBoundVBO(rendRecord, vboID);
            EXTFogCoord.glFogCoordPointerEXT(GL11.GL_FLOAT, 0, 0);
        } else {
            GL11.glDisableClientState(EXTFogCoord.GL_FOG_COORDINATE_ARRAY_EXT);
        }
    }

    public void setupTextureDataVBO(final List<FloatBufferData> textureCoords) {
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
                        checkAndSetTextureArrayUnit(i, rendRecord, caps);

                        // disable bit in tracking int
                        enabledTextures &= ~(2 << i);

                        // disable state
                        GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);

                        continue;
                    }
                } else {
                    checkAndSetTextureArrayUnit(i, rendRecord, caps);

                    // grab a vboID and make sure it exists and is up to date.
                    final FloatBufferData data = textureCoords.get(i);
                    final int vboID = setupVBO(data, context);

                    // Found good vbo
                    if (vboID != 0) {
                        if (!valid || !wasOn) {
                            // enable bit in tracking int
                            enabledTextures |= (2 << i);

                            // enable state
                            GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
                        }

                        // set our active vbo
                        LwjglRendererUtil.setBoundVBO(rendRecord, vboID);

                        // send data
                        GL11.glTexCoordPointer(data.getValuesPerTuple(), GL11.GL_FLOAT, 0, 0);
                    }
                    // Not a good vbo, disable it.
                    else {
                        if (!valid || wasOn) {
                            // disable bit in tracking int
                            enabledTextures &= ~(2 << i);

                            // disable state
                            GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
                        }
                    }
                }
            }
        }

        rendRecord.setEnabledTextures(enabledTextures);
        rendRecord.setTexturesValid(true);
    }

    public void setupInterleavedDataVBO(final FloatBufferData interleaved, final FloatBufferData vertexCoords,
            final FloatBufferData normalCoords, final FloatBufferData colorCoords,
            final List<FloatBufferData> textureCoords) {
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
        LwjglRendererUtil.setBoundVBO(rendRecord, vboID);

        int offsetBytes = 0;

        if (normalCoords != null) {
            updateVBO(normalCoords, rendRecord, vboID, offsetBytes);
            GL11.glNormalPointer(GL11.GL_FLOAT, 0, offsetBytes);
            GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
            offsetBytes += normalCoords.getBufferLimit() * 4;
        } else {
            GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
        }

        if (colorCoords != null) {
            updateVBO(colorCoords, rendRecord, vboID, offsetBytes);
            GL11.glColorPointer(colorCoords.getValuesPerTuple(), GL11.GL_FLOAT, 0, offsetBytes);
            GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
            offsetBytes += colorCoords.getBufferLimit() * 4;
        } else {
            GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);
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
                    exists = textureCoords != null && i < textureCoords.size() && textureCoords.get(i) != null
                            && i <= ts.getMaxTextureIndexUsed();

                    if (!exists) {
                        if (valid && !wasOn) {
                            continue;
                        } else {
                            checkAndSetTextureArrayUnit(i, rendRecord, caps);

                            // disable bit in tracking int
                            enabledTextures &= ~(2 << i);

                            // disable state
                            GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);

                            continue;
                        }

                    } else {
                        checkAndSetTextureArrayUnit(i, rendRecord, caps);

                        // grab a vboID and make sure it exists and is up to date.
                        final FloatBufferData textureBufferData = textureCoords.get(i);
                        updateVBO(textureBufferData, rendRecord, vboID, offsetBytes);

                        if (!valid || !wasOn) {
                            // enable bit in tracking int
                            enabledTextures |= (2 << i);

                            // enable state
                            GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
                        }

                        // send data
                        GL11.glTexCoordPointer(textureBufferData.getValuesPerTuple(), GL11.GL_FLOAT, 0, offsetBytes);
                        offsetBytes += textureBufferData.getBufferLimit() * 4;
                    }
                }
            }

            rendRecord.setEnabledTextures(enabledTextures);
            rendRecord.setTexturesValid(true);
        }

        if (vertexCoords != null) {
            updateVBO(vertexCoords, rendRecord, vboID, offsetBytes);
            GL11.glVertexPointer(vertexCoords.getValuesPerTuple(), GL11.GL_FLOAT, 0, offsetBytes);
            GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        } else {
            GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
        }
    }

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

        final RendererRecord rendRecord = context.getRendererRecord();
        final ContextCapabilities caps = context.getCapabilities();

        final int vboID = makeVBOId();
        interleaved.setVBOID(context.getGlContextRep(), vboID);

        rendRecord.invalidateVBO();
        LwjglRendererUtil.setBoundVBO(rendRecord, vboID);
        ARBBufferObject.glBufferDataARB(ARBVertexBufferObject.GL_ARRAY_BUFFER_ARB, bufferSize,
                getGLVBOAccessMode(interleaved.getVboAccessMode()));

        int offsetBytes = 0;
        if (normalCoords != null) {
            normalCoords.getBuffer().rewind();
            ARBBufferObject.glBufferSubDataARB(ARBVertexBufferObject.GL_ARRAY_BUFFER_ARB, offsetBytes,
                    normalCoords.getBuffer());
            offsetBytes += normalCoords.getBufferLimit() * 4;
        }
        if (colorCoords != null) {
            colorCoords.getBuffer().rewind();
            ARBBufferObject.glBufferSubDataARB(ARBVertexBufferObject.GL_ARRAY_BUFFER_ARB, offsetBytes,
                    colorCoords.getBuffer());
            offsetBytes += colorCoords.getBufferLimit() * 4;
        }
        if (textureCoords != null) {
            final TextureState ts = (TextureState) context.getCurrentState(RenderState.StateType.Texture);
            if (ts != null) {
                for (int i = 0; i <= ts.getMaxTextureIndexUsed() && i < caps.getNumberOfFragmentTexCoordUnits(); i++) {
                    if (textureCoords == null || i >= textureCoords.size()) {
                        continue;
                    }

                    final FloatBufferData textureBufferData = textureCoords.get(i);
                    final FloatBuffer textureBuffer = textureBufferData != null ? textureBufferData.getBuffer() : null;
                    if (textureBuffer != null) {
                        textureBuffer.rewind();
                        ARBBufferObject.glBufferSubDataARB(ARBVertexBufferObject.GL_ARRAY_BUFFER_ARB, offsetBytes,
                                textureBuffer);
                        offsetBytes += textureBufferData.getBufferLimit() * 4;
                    }
                }
            }
        }
        if (vertexCoords != null) {
            vertexCoords.getBuffer().rewind();
            ARBBufferObject.glBufferSubDataARB(ARBVertexBufferObject.GL_ARRAY_BUFFER_ARB, offsetBytes,
                    vertexCoords.getBuffer());
        }

        interleaved.setNeedsRefresh(false);
    }

    @Override
    public void drawElementsVBO(final IndexBufferData<?> indices, final int[] indexLengths,
            final IndexMode[] indexModes, final int primcount) {
        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord rendRecord = context.getRendererRecord();

        final int vboID = setupIndicesVBO(indices, context, rendRecord);

        LwjglRendererUtil.setBoundElementVBO(rendRecord, vboID);

        if (indexLengths == null) {
            final int glIndexMode = getGLIndexMode(indexModes[0]);

            final int type = getGLDataType(indices);
            if (primcount < 0) {
                GL11.glDrawElements(glIndexMode, indices.getBufferLimit(), type, 0);
            } else {
                GL31.glDrawElementsInstanced(glIndexMode, indices.getBufferLimit(), type, 0, primcount);
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
                if (primcount < 0) {
                    GL11.glDrawElements(glIndexMode, count, type, offset * byteSize);
                } else {
                    GL31.glDrawElementsInstanced(glIndexMode, count, type, offset * byteSize, primcount);
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
    public void drawArrays(final FloatBufferData vertices, final int[] indexLengths, final IndexMode[] indexModes,
            final int primcount) {
        if (indexLengths == null) {
            final int glIndexMode = getGLIndexMode(indexModes[0]);

            if (primcount < 0) {
                GL11.glDrawArrays(glIndexMode, 0, vertices.getTupleCount());
            } else {
                GL31.glDrawArraysInstanced(glIndexMode, 0, vertices.getTupleCount(), primcount);
            }

            if (Constants.stats) {
                addStats(indexModes[0], vertices.getTupleCount());
            }
        } else {
            int offset = 0;
            int indexModeCounter = 0;
            for (int i = 0; i < indexLengths.length; i++) {
                final int count = indexLengths[i];

                final int glIndexMode = getGLIndexMode(indexModes[indexModeCounter]);

                if (primcount < 0) {
                    GL11.glDrawArrays(glIndexMode, offset, count);
                } else {
                    GL31.glDrawArraysInstanced(glIndexMode, offset, count, primcount);
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

    public static int makeVBOId() {
        return ARBBufferObject.glGenBuffersARB();
    }

    public void unbindVBO() {
        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord rendRecord = context.getRendererRecord();
        LwjglRendererUtil.setBoundVBO(rendRecord, 0);
        LwjglRendererUtil.setBoundElementVBO(rendRecord, 0);
    }

    private static int getGLVBOAccessMode(final VBOAccessMode vboAccessMode) {
        int glMode = ARBBufferObject.GL_STATIC_DRAW_ARB;
        switch (vboAccessMode) {
            case StaticDraw:
                glMode = ARBBufferObject.GL_STATIC_DRAW_ARB;
                break;
            case StaticRead:
                glMode = ARBBufferObject.GL_STATIC_READ_ARB;
                break;
            case StaticCopy:
                glMode = ARBBufferObject.GL_STATIC_COPY_ARB;
                break;
            case DynamicDraw:
                glMode = ARBBufferObject.GL_DYNAMIC_DRAW_ARB;
                break;
            case DynamicRead:
                glMode = ARBBufferObject.GL_DYNAMIC_READ_ARB;
                break;
            case DynamicCopy:
                glMode = ARBBufferObject.GL_DYNAMIC_COPY_ARB;
                break;
            case StreamDraw:
                glMode = ARBBufferObject.GL_STREAM_DRAW_ARB;
                break;
            case StreamRead:
                glMode = ARBBufferObject.GL_STREAM_READ_ARB;
                break;
            case StreamCopy:
                glMode = ARBBufferObject.GL_STREAM_COPY_ARB;
                break;
        }
        return glMode;
    }

    private int getGLIndexMode(final IndexMode indexMode) {
        int glMode = GL11.GL_TRIANGLES;
        switch (indexMode) {
            case Triangles:
                glMode = GL11.GL_TRIANGLES;
                break;
            case TriangleStrip:
                glMode = GL11.GL_TRIANGLE_STRIP;
                break;
            case TriangleFan:
                glMode = GL11.GL_TRIANGLE_FAN;
                break;
            case Lines:
                glMode = GL11.GL_LINES;
                break;
            case LineStrip:
                glMode = GL11.GL_LINE_STRIP;
                break;
            case LineLoop:
                glMode = GL11.GL_LINE_LOOP;
                break;
            case Points:
                glMode = GL11.GL_POINTS;
                break;
        }
        return glMode;
    }

    private int getGLDataType(final IndexBufferData<?> indices) {
        if (indices.getBuffer() instanceof ByteBuffer) {
            return GL11.GL_UNSIGNED_BYTE;
        } else if (indices.getBuffer() instanceof ShortBuffer) {
            return GL11.GL_UNSIGNED_SHORT;
        } else if (indices.getBuffer() instanceof IntBuffer) {
            return GL11.GL_UNSIGNED_INT;
        }

        throw new IllegalArgumentException("Unknown buffer type: " + indices.getBuffer());
    }

    public void setModelViewMatrix(final FloatBuffer matrix) {
        final RendererRecord matRecord = ContextManager.getCurrentContext().getRendererRecord();
        LwjglRendererUtil.switchMode(matRecord, GL11.GL_MODELVIEW);
        loadMatrix(matrix);
    }

    public void setProjectionMatrix(final FloatBuffer matrix) {
        final RendererRecord matRecord = ContextManager.getCurrentContext().getRendererRecord();
        LwjglRendererUtil.switchMode(matRecord, GL11.GL_PROJECTION);
        loadMatrix(matrix);
    }

    private void loadMatrix(final FloatBuffer matrix) {
        GL11.glLoadMatrix(matrix);
    }

    public FloatBuffer getModelViewMatrix(final FloatBuffer store) {
        return getMatrix(GL11.GL_MODELVIEW_MATRIX, store);
    }

    public FloatBuffer getProjectionMatrix(final FloatBuffer store) {
        return getMatrix(GL11.GL_PROJECTION_MATRIX, store);
    }

    private FloatBuffer getMatrix(final int matrixType, final FloatBuffer store) {
        FloatBuffer result = store;
        if (result.remaining() < 16) {
            result = BufferUtils.createFloatBuffer(16);
        }
        GL11.glGetFloat(matrixType, store);
        return result;
    }

    public void setViewport(final int x, final int y, final int width, final int height) {
        GL11.glViewport(x, y, width, height);
    }

    public void setDepthRange(final double depthRangeNear, final double depthRangeFar) {
        GL11.glDepthRange(depthRangeNear, depthRangeFar);
    }

    public void setDrawBuffer(final DrawBufferTarget target) {
        final RendererRecord record = ContextManager.getCurrentContext().getRendererRecord();
        if (record.getDrawBufferTarget() != target) {
            int buffer = GL11.GL_BACK;
            switch (target) {
                case Back:
                    break;
                case Front:
                    buffer = GL11.GL_FRONT;
                    break;
                case BackLeft:
                    buffer = GL11.GL_BACK_LEFT;
                    break;
                case BackRight:
                    buffer = GL11.GL_BACK_RIGHT;
                    break;
                case FrontLeft:
                    buffer = GL11.GL_FRONT_LEFT;
                    break;
                case FrontRight:
                    buffer = GL11.GL_FRONT_RIGHT;
                    break;
                case FrontAndBack:
                    buffer = GL11.GL_FRONT_AND_BACK;
                    break;
                case Left:
                    buffer = GL11.GL_LEFT;
                    break;
                case Right:
                    buffer = GL11.GL_RIGHT;
                    break;
                case Aux0:
                    buffer = GL11.GL_AUX0;
                    break;
                case Aux1:
                    buffer = GL11.GL_AUX1;
                    break;
                case Aux2:
                    buffer = GL11.GL_AUX2;
                    break;
                case Aux3:
                    buffer = GL11.GL_AUX3;
                    break;
            }

            GL11.glDrawBuffer(buffer);
            record.setDrawBufferTarget(target);
        }
    }

    public void setupLineParameters(final float lineWidth, final boolean antialiased) {
        final LineRecord lineRecord = ContextManager.getCurrentContext().getLineRecord();

        if (!lineRecord.isValid() || lineRecord.width != lineWidth) {
            GL11.glLineWidth(lineWidth);
            lineRecord.width = lineWidth;
        }

        if (antialiased) {
            if (!lineRecord.isValid() || !lineRecord.smoothed) {
                GL11.glEnable(GL11.GL_LINE_SMOOTH);
                lineRecord.smoothed = true;
            }
            if (!lineRecord.isValid() || lineRecord.smoothHint != GL11.GL_NICEST) {
                GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
                lineRecord.smoothHint = GL11.GL_NICEST;
            }
        } else if (!lineRecord.isValid() || lineRecord.smoothed) {
            GL11.glDisable(GL11.GL_LINE_SMOOTH);
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

        // TODO: make a record for point states
        GL11.glPointSize(pointSize);

        if (isSprite && context.getCapabilities().isPointSpritesSupported()) {
            GL11.glEnable(ARBPointSprite.GL_POINT_SPRITE_ARB);
            GL11.glTexEnvi(ARBPointSprite.GL_POINT_SPRITE_ARB, ARBPointSprite.GL_COORD_REPLACE_ARB, GL11.GL_TRUE);
        }

        if (useDistanceAttenuation && context.getCapabilities().isPointParametersSupported()) {
            ARBPointParameters.glPointParameterARB(ARBPointParameters.GL_POINT_DISTANCE_ATTENUATION_ARB,
                    attenuationCoefficients);
            ARBPointParameters.glPointParameterfARB(ARBPointParameters.GL_POINT_SIZE_MIN_ARB, minPointSize);
            ARBPointParameters.glPointParameterfARB(ARBPointParameters.GL_POINT_SIZE_MAX_ARB, maxPointSize);
        }

        if (antialiased) {
            GL11.glEnable(GL11.GL_POINT_SMOOTH);
            GL11.glHint(GL11.GL_POINT_SMOOTH_HINT, GL11.GL_NICEST);
        }
    }

    @Override
    public void doApplyState(final RenderState state) {
        if (state == null) {
            logger.warning("tried to apply a null state.");
            return;
        }
        switch (state.getType()) {
            case Texture:
                LwjglTextureStateUtil.apply((TextureState) state);
                return;
            case Light:
                LwjglLightStateUtil.apply((LightState) state);
                return;
            case Blend:
                LwjglBlendStateUtil.apply((BlendState) state);
                return;
            case Clip:
                LwjglClipStateUtil.apply((ClipState) state);
                return;
            case ColorMask:
                LwjglColorMaskStateUtil.apply((ColorMaskState) state);
                return;
            case Cull:
                LwjglCullStateUtil.apply((CullState) state);
                return;
            case Fog:
                LwjglFogStateUtil.apply((FogState) state);
                return;
            case FragmentProgram:
                LwjglFragmentProgramStateUtil.apply((FragmentProgramState) state);
                return;
            case GLSLShader:
                LwjglShaderObjectsStateUtil.apply(this, (GLSLShaderObjectsState) state);
                return;
            case Material:
                LwjglMaterialStateUtil.apply((MaterialState) state);
                return;
            case Offset:
                LwjglOffsetStateUtil.apply(this, (OffsetState) state);
                return;
            case Shading:
                LwjglShadingStateUtil.apply((ShadingState) state);
                return;
            case Stencil:
                LwjglStencilStateUtil.apply((StencilState) state);
                return;
            case VertexProgram:
                LwjglVertexProgramStateUtil.apply((VertexProgramState) state);
                return;
            case Wireframe:
                LwjglWireframeStateUtil.apply(this, (WireframeState) state);
                return;
            case ZBuffer:
                LwjglZBufferStateUtil.apply((ZBufferState) state);
                return;
        }
        throw new IllegalArgumentException("Unknown state: " + state);
    }

    public void deleteTexture(final Texture texture) {
        LwjglTextureStateUtil.deleteTexture(texture);
    }

    public void loadTexture(final Texture texture, final int unit) {
        LwjglTextureStateUtil.load(texture, unit);
    }

    public void deleteTextureIds(final Collection<Integer> ids) {
        LwjglTextureStateUtil.deleteTextureIds(ids);
    }

    public void clearClips() {
        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord record = context.getRendererRecord();
        record.getScissorClips().clear();

        LwjglRendererUtil.applyScissors(record);
    }

    public void popClip() {
        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord record = context.getRendererRecord();
        record.getScissorClips().pop();

        LwjglRendererUtil.applyScissors(record);
    }

    public void pushClip(final ReadOnlyRectangle2 rectangle) {
        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord record = context.getRendererRecord();
        record.getScissorClips().push(rectangle);

        LwjglRendererUtil.applyScissors(record);
    }

    public void pushEmptyClip() {
        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord record = context.getRendererRecord();
        record.getScissorClips().push(null);

        LwjglRendererUtil.applyScissors(record);
    }

    public void setClipTestEnabled(final boolean enabled) {
        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord record = context.getRendererRecord();

        LwjglRendererUtil.setClippingEnabled(record, enabled);
    }

    public void checkAndSetTextureArrayUnit(final int unit, final RendererRecord record, final ContextCapabilities caps) {
        if (record.getCurrentTextureArraysUnit() != unit && caps.isMultitextureSupported()) {
            ARBMultitexture.glClientActiveTextureARB(ARBMultitexture.GL_TEXTURE0_ARB + unit);
            record.setCurrentTextureArraysUnit(unit);
        }
    }

}
