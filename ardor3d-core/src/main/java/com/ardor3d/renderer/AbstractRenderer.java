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
import java.util.EnumMap;
import java.util.List;

import com.ardor3d.image.ImageDataFormat;
import com.ardor3d.image.PixelDataType;
import com.ardor3d.image.util.ImageUtils;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.queue.RenderQueue;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.record.RendererRecord;
import com.ardor3d.scenegraph.FloatBufferData;
import com.ardor3d.util.Constants;
import com.ardor3d.util.stat.StatCollector;
import com.ardor3d.util.stat.StatType;

/**
 * Provides some common base level method implementations for Renderers.
 */
public abstract class AbstractRenderer implements Renderer {
    // clear color
    protected final ColorRGBA _backgroundColor = new ColorRGBA(ColorRGBA.BLACK);

    protected boolean _processingQueue;

    protected RenderQueue _queue = new RenderQueue();

    protected boolean _inOrthoMode;

    protected int _stencilClearValue;

    protected RenderLogic renderLogic;

    /** List of default rendering states for this specific renderer type */
    protected final EnumMap<RenderState.StateType, RenderState> defaultStateList = new EnumMap<RenderState.StateType, RenderState>(
            RenderState.StateType.class);

    public AbstractRenderer() {
        for (final RenderState.StateType type : RenderState.StateType.values()) {
            final RenderState state = RenderState.createState(type);
            state.setEnabled(false);
            defaultStateList.put(type, state);
        }
    }

    public boolean isInOrthoMode() {
        return _inOrthoMode;
    }

    public ReadOnlyColorRGBA getBackgroundColor() {
        return _backgroundColor;
    }

    public RenderQueue getQueue() {
        return _queue;
    }

    public boolean isProcessingQueue() {
        return _processingQueue;
    }

    public void applyState(final StateType type, final RenderState state) {
        if (Constants.stats) {
            StatCollector.startStat(StatType.STAT_STATES_TIMER);
        }

        final RenderState tempState = getProperRenderState(type, state);
        final RenderContext context = ContextManager.getCurrentContext();
        if (!RenderState._quickCompare.contains(type) || tempState.needsRefresh()
                || tempState != context.getCurrentState(type)) {
            doApplyState(tempState);
            tempState.setNeedsRefresh(false);
        }

        if (Constants.stats) {
            StatCollector.endStat(StatType.STAT_STATES_TIMER);
        }
    }

    protected abstract void doApplyState(RenderState state);

    protected void addStats(final IndexMode indexMode, final int vertCount) {
        final int primCount = IndexMode.getPrimitiveCount(indexMode, vertCount);
        switch (indexMode) {
            case Triangles:
            case TriangleFan:
            case TriangleStrip:
                StatCollector.addStat(StatType.STAT_TRIANGLE_COUNT, primCount);
                break;
            case Lines:
            case LineLoop:
            case LineStrip:
                StatCollector.addStat(StatType.STAT_LINE_COUNT, primCount);
                break;
            case Points:
                StatCollector.addStat(StatType.STAT_POINT_COUNT, primCount);
                break;
        }
    }

    protected int getTotalInterleavedSize(final RenderContext context, final FloatBufferData vertexCoords,
            final FloatBufferData normalCoords, final FloatBufferData colorCoords,
            final List<FloatBufferData> textureCoords) {

        int bufferSizeBytes = 0;
        if (normalCoords != null) {
            bufferSizeBytes += normalCoords.getBufferLimit() * 4;
        }
        if (colorCoords != null) {
            bufferSizeBytes += colorCoords.getBufferLimit() * 4;
        }
        if (textureCoords != null) {
            final TextureState ts = (TextureState) context.getCurrentState(RenderState.StateType.Texture);
            if (ts != null) {
                boolean exists;
                for (int i = 0; i < TextureState.MAX_TEXTURES; i++) {
                    exists = i < textureCoords.size() && textureCoords.get(i) != null
                            && i <= ts.getMaxTextureIndexUsed();

                    if (!exists) {
                        continue;
                    }

                    final FloatBufferData textureBufferData = textureCoords.get(i);
                    if (textureBufferData != null) {
                        bufferSizeBytes += textureBufferData.getBufferLimit() * 4;
                    }
                }
            }
        }
        if (vertexCoords != null) {
            bufferSizeBytes += vertexCoords.getBufferLimit() * 4;
        }

        return bufferSizeBytes;
    }

    public int getStencilClearValue() {
        return _stencilClearValue;
    }

    public void setStencilClearValue(final int stencilClearValue) {
        _stencilClearValue = stencilClearValue;
    }

    public boolean isClipTestEnabled() {
        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord record = context.getRendererRecord();
        return record.isClippingTestEnabled();
    }

    public RenderState getProperRenderState(final StateType type, final RenderState current) {
        final RenderContext context = ContextManager.getCurrentContext();

        // first look up in enforced states
        final RenderState state = context.hasEnforcedStates() ? context.getEnforcedState(type) : null;

        // Not there? Use the state we received
        if (state == null) {
            if (current != null) {
                return current;
            } else {
                return defaultStateList.get(type);
            }
        } else {
            return state;
        }
    }

    public void setRenderLogic(final RenderLogic renderLogic) {
        this.renderLogic = renderLogic;
    }

    @Override
    public void grabScreenContents(final ByteBuffer store, final ImageDataFormat format, final int x, final int y,
            final int w, final int h) {
        grabScreenContents(store, format, PixelDataType.UnsignedByte, x, y, w, h);
    }

    @Override
    public int getExpectedBufferSizeToGrabScreenContents(final ImageDataFormat format, final PixelDataType type,
            final int w, final int h) {
        final int size = w * h * ImageUtils.getPixelByteSize(format, type);
        return size;
    }
}
