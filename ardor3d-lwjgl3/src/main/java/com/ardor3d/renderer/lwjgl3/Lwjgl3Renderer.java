/**
 * Copyright (c) 2008-2018 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.lwjgl3;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GLUtil;

import com.ardor3d.image.ImageDataFormat;
import com.ardor3d.image.PixelDataType;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture1D;
import com.ardor3d.image.Texture2D;
import com.ardor3d.image.Texture3D;
import com.ardor3d.image.TextureCubeMap;
import com.ardor3d.image.TextureCubeMap.Face;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.math.type.ReadOnlyRectangle2;
import com.ardor3d.math.type.ReadOnlyTransform;
import com.ardor3d.renderer.AbstractRenderer;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.DrawBufferTarget;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.record.RendererRecord;
import com.ardor3d.scene.state.lwjgl3.Lwjgl3BlendStateUtil;
import com.ardor3d.scene.state.lwjgl3.util.Lwjgl3RendererUtil;
import com.ardor3d.scene.state.lwjgl3.util.Lwjgl3TextureUtil;
import com.ardor3d.scenegraph.AbstractBufferData;
import com.ardor3d.scenegraph.FloatBufferData;
import com.ardor3d.scenegraph.IndexBufferData;
import com.ardor3d.scenegraph.Renderable;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.hint.NormalsMode;
import com.ardor3d.util.Ardor3dException;
import com.ardor3d.util.Constants;
import com.ardor3d.util.stat.StatCollector;
import com.ardor3d.util.stat.StatType;

/**
 * <code>LwjglRenderer</code> provides an implementation of the <code>Renderer</code> interface using LWJGL version 3.
 *
 * @see com.ardor3d.renderer.Renderer
 */
public class Lwjgl3Renderer extends AbstractRenderer {
    private static final Logger logger = Logger.getLogger(Lwjgl3Renderer.class.getName());

    /**
     * Constructor instantiates a new <code>Lwjgl3Renderer</code> object.
     */
    public Lwjgl3Renderer() {
        logger.fine("Lwjgl3Renderer created.");
        GLUtil.setupDebugMessageCallback();
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

    @Override
    public void setBackgroundColor(final ReadOnlyColorRGBA color) {
        _backgroundColor.set(color);
        GL11C.glClearColor(_backgroundColor.getRed(), _backgroundColor.getGreen(), _backgroundColor.getBlue(),
                _backgroundColor.getAlpha());

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
            clear |= GL11C.GL_COLOR_BUFFER_BIT;
        }

        if ((buffers & Renderer.BUFFER_DEPTH) != 0) {
            clear |= GL11C.GL_DEPTH_BUFFER_BIT;

            // make sure no funny business is going on in the z before clearing.
            if (defaultStateList.containsKey(RenderState.StateType.ZBuffer)) {
                defaultStateList.get(RenderState.StateType.ZBuffer).setNeedsRefresh(true);
                doApplyState(defaultStateList.get(RenderState.StateType.ZBuffer));
            }
        }

        if ((buffers & Renderer.BUFFER_STENCIL) != 0) {
            clear |= GL11C.GL_STENCIL_BUFFER_BIT;

            GL11C.glClearStencil(_stencilClearValue);
            GL11C.glStencilMask(~0);
            GL11C.glClear(GL11C.GL_STENCIL_BUFFER_BIT);
        }

        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord record = context.getRendererRecord();

        if (strict) {
            // grab our camera to get width and height info.
            final Camera cam = Camera.getCurrentCamera();

            GL11C.glEnable(GL11C.GL_SCISSOR_TEST);
            GL11C.glScissor(0, 0, cam.getWidth(), cam.getHeight());
            record.setClippingTestEnabled(true);
        }

        GL11C.glClear(clear);

        if (strict) {
            // put us back.
            Lwjgl3RendererUtil.applyScissors(record);
        }
    }

    // Note that we will NOT ever swap - canvas will need to do that.
    public void flushFrame(final boolean doSwap) {
        renderBuckets();

        GL11C.glFlush();
        if (doSwap) {
            doApplyState(defaultStateList.get(RenderState.StateType.ColorMask));
        }

        if (Constants.stats) {
            StatCollector.addStat(StatType.STAT_FRAMES, 1);
        }
    }

    @Override
    public void setOrtho() {
        // TODO Auto-generated method stub

    }

    @Override
    public void unsetOrtho() {
        // TODO Auto-generated method stub

    }

    @Override
    public void grabScreenContents(final ByteBuffer store, final ImageDataFormat format, final PixelDataType type,
            final int x, final int y, final int w, final int h) {
        final int pixFormat = Lwjgl3TextureUtil.getGLPixelFormat(format);
        final int pixDataType = Lwjgl3TextureUtil.getGLPixelDataType(type);
        GL11C.glReadPixels(x, y, w, h, pixFormat, pixDataType, store);
    }

    @Override
    public void draw(final Spatial s) {
        if (s != null) {
            s.onDraw(this);
        }
    }

    public void flushGraphics() {
        GL11C.glFlush();
    }

    public void finishGraphics() {
        GL11C.glFinish();
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

    @Override
    public void deleteVBOs(final Collection<Integer> ids) {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteVBOs(final AbstractBufferData<?> buffer) {
        // TODO Auto-generated method stub

    }

    @Override
    public void unbindVBO() {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateTexture1DSubImage(final Texture1D destination, final int dstOffsetX, final int dstWidth,
            final ByteBuffer source, final int srcOffsetX) {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateTexture2DSubImage(final Texture2D destination, final int dstOffsetX, final int dstOffsetY,
            final int dstWidth, final int dstHeight, final ByteBuffer source, final int srcOffsetX,
            final int srcOffsetY, final int srcTotalWidth) {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateTexture3DSubImage(final Texture3D destination, final int dstOffsetX, final int dstOffsetY,
            final int dstOffsetZ, final int dstWidth, final int dstHeight, final int dstDepth, final ByteBuffer source,
            final int srcOffsetX, final int srcOffsetY, final int srcOffsetZ, final int srcTotalWidth,
            final int srcTotalHeight) {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateTextureCubeMapSubImage(final TextureCubeMap destination, final Face dstFace,
            final int dstOffsetX, final int dstOffsetY, final int dstWidth, final int dstHeight,
            final ByteBuffer source, final int srcOffsetX, final int srcOffsetY, final int srcTotalWidth) {
        // TODO Auto-generated method stub

    }

    @Override
    public void checkCardError() throws Ardor3dException {
        ; // We're using GLUtil with its callback for this
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
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void undoTransforms(final ReadOnlyTransform transform) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setupVertexData(final FloatBufferData vertexCoords) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setupNormalData(final FloatBufferData normalCoords) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setupColorData(final FloatBufferData colorCoords) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setupTextureData(final List<FloatBufferData> textureCoords) {
        // TODO Auto-generated method stub

    }

    @Override
    public void drawElements(final IndexBufferData<?> indices, final int[] indexLengths, final IndexMode[] indexModes,
            final int primcount) {
        // TODO Auto-generated method stub

    }

    @Override
    public void drawArrays(final FloatBufferData vertexBuffer, final int[] indexLengths, final IndexMode[] indexModes,
            final int primcount) {
        // TODO Auto-generated method stub

    }

    @Override
    public void drawElementsVBO(final IndexBufferData<?> indices, final int[] indexLengths,
            final IndexMode[] indexModes, final int primcount) {
        // TODO Auto-generated method stub

    }

    @Override
    public void applyNormalsMode(final NormalsMode normMode, final ReadOnlyTransform worldTransform) {
        // TODO Auto-generated method stub

    }

    @Override
    public void applyDefaultColor(final ReadOnlyColorRGBA defaultColor) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setupVertexDataVBO(final FloatBufferData vertexCoords) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setupNormalDataVBO(final FloatBufferData normalCoords) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setupColorDataVBO(final FloatBufferData colorCoords) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setupTextureDataVBO(final List<FloatBufferData> textureCoords) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setupInterleavedDataVBO(final FloatBufferData interleaved, final FloatBufferData vertexCoords,
            final FloatBufferData normalCoords, final FloatBufferData colorCoords,
            final List<FloatBufferData> textureCoords) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setProjectionMatrix(final FloatBuffer matrix) {
        // TODO Auto-generated method stub

    }

    @Override
    public FloatBuffer getProjectionMatrix(final FloatBuffer store) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setModelViewMatrix(final FloatBuffer matrix) {
        // TODO Auto-generated method stub

    }

    @Override
    public FloatBuffer getModelViewMatrix(final FloatBuffer store) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setViewport(final int x, final int y, final int width, final int height) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setDepthRange(final double depthRangeNear, final double depthRangeFar) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setDrawBuffer(final DrawBufferTarget target) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setupLineParameters(final float lineWidth, final boolean antialiased) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setupPointParameters(final float pointSize, final boolean antialiased, final boolean isSprite,
            final boolean useDistanceAttenuation, final FloatBuffer attenuationCoefficients, final float minPointSize,
            final float maxPointSize) {
        // TODO Auto-generated method stub

    }

    @Override
    public void loadTexture(final Texture texture, final int unit) {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteTexture(final Texture texture) {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteTextureIds(final Collection<Integer> ids) {
        // TODO Auto-generated method stub

    }

    @Override
    public void pushClip(final ReadOnlyRectangle2 rectangle) {
        // TODO Auto-generated method stub

    }

    @Override
    public void pushEmptyClip() {
        // TODO Auto-generated method stub

    }

    @Override
    public void popClip() {
        // TODO Auto-generated method stub

    }

    @Override
    public void clearClips() {
        // TODO Auto-generated method stub

    }

    @Override
    public void setClipTestEnabled(final boolean enabled) {
        // TODO Auto-generated method stub

    }

    @Override
    protected void doApplyState(final RenderState state) {
        if (state == null) {
            logger.warning("tried to apply a null state.");
            return;
        }
        switch (state.getType()) {
            case Texture:
                // LwjglTextureStateUtil.apply((TextureState) state);
                return;
            case Light:
                // LwjglLightStateUtil.apply((LightState) state);
                return;
            case Blend:
                Lwjgl3BlendStateUtil.apply((BlendState) state);
                return;
            case Clip:
                // LwjglClipStateUtil.apply((ClipState) state);
                return;
            case ColorMask:
                // LwjglColorMaskStateUtil.apply((ColorMaskState) state);
                return;
            case Cull:
                // LwjglCullStateUtil.apply((CullState) state);
                return;
            case Fog:
                // LwjglFogStateUtil.apply((FogState) state);
                return;
            case FragmentProgram:
                // LwjglFragmentProgramStateUtil.apply((FragmentProgramState) state);
                return;
            case GLSLShader:
                // LwjglShaderObjectsStateUtil.apply(this, (GLSLShaderObjectsState) state);
                return;
            case Material:
                // LwjglMaterialStateUtil.apply((MaterialState) state);
                return;
            case Offset:
                // LwjglOffsetStateUtil.apply(this, (OffsetState) state);
                return;
            case Shading:
                // LwjglShadingStateUtil.apply((ShadingState) state);
                return;
            case Stencil:
                // LwjglStencilStateUtil.apply((StencilState) state);
                return;
            case VertexProgram:
                // LwjglVertexProgramStateUtil.apply((VertexProgramState) state);
                return;
            case Wireframe:
                // LwjglWireframeStateUtil.apply(this, (WireframeState) state);
                return;
            case ZBuffer:
                // LwjglZBufferStateUtil.apply((ZBufferState) state);
                return;
        }
        throw new IllegalArgumentException("Unknown state: " + state);
    }

}
