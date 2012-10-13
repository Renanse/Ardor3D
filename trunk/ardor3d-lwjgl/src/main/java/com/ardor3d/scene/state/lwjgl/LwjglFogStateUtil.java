/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scene.state.lwjgl;

import org.lwjgl.opengl.EXTFogCoord;
import org.lwjgl.opengl.GL11;

import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.state.FogState;
import com.ardor3d.renderer.state.FogState.CoordinateSource;
import com.ardor3d.renderer.state.FogState.DensityFunction;
import com.ardor3d.renderer.state.FogState.Quality;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.record.FogStateRecord;

public abstract class LwjglFogStateUtil {

    public static void apply(final FogState state) {
        // ask for the current state record
        final RenderContext context = ContextManager.getCurrentContext();
        final FogStateRecord record = (FogStateRecord) context.getStateRecord(StateType.Fog);
        context.setCurrentState(StateType.Fog, state);

        if (state.isEnabled()) {
            enableFog(true, record);

            if (record.isValid()) {
                if (record.fogStart != state.getStart()) {
                    GL11.glFogf(GL11.GL_FOG_START, state.getStart());
                    record.fogStart = state.getStart();
                }
                if (record.fogEnd != state.getEnd()) {
                    GL11.glFogf(GL11.GL_FOG_END, state.getEnd());
                    record.fogEnd = state.getEnd();
                }
                if (record.density != state.getDensity()) {
                    GL11.glFogf(GL11.GL_FOG_DENSITY, state.getDensity());
                    record.density = state.getDensity();
                }
            } else {
                GL11.glFogf(GL11.GL_FOG_START, state.getStart());
                record.fogStart = state.getStart();
                GL11.glFogf(GL11.GL_FOG_END, state.getEnd());
                record.fogEnd = state.getEnd();
                GL11.glFogf(GL11.GL_FOG_DENSITY, state.getDensity());
                record.density = state.getDensity();
            }

            final ReadOnlyColorRGBA fogColor = state.getColor();
            applyFogColor(fogColor, record);
            applyFogMode(state.getDensityFunction(), record);
            applyFogHint(state.getQuality(), record);
            applyFogSource(state.getSource(), record, context.getCapabilities());
        } else {
            enableFog(false, record);
        }

        if (!record.isValid()) {
            record.validate();
        }
    }

    private static void enableFog(final boolean enable, final FogStateRecord record) {
        if (record.isValid()) {
            if (enable && !record.enabled) {
                GL11.glEnable(GL11.GL_FOG);
                record.enabled = true;
            } else if (!enable && record.enabled) {
                GL11.glDisable(GL11.GL_FOG);
                record.enabled = false;
            }
        } else {
            if (enable) {
                GL11.glEnable(GL11.GL_FOG);
            } else {
                GL11.glDisable(GL11.GL_FOG);
            }
            record.enabled = enable;
        }
    }

    private static void applyFogColor(final ReadOnlyColorRGBA color, final FogStateRecord record) {
        if (!record.isValid() || !color.equals(record.fogColor)) {
            record.fogColor.set(color);
            record.colorBuff.clear();
            record.colorBuff.put(record.fogColor.getRed()).put(record.fogColor.getGreen())
                    .put(record.fogColor.getBlue()).put(record.fogColor.getAlpha());
            record.colorBuff.flip();
            GL11.glFog(GL11.GL_FOG_COLOR, record.colorBuff);
        }
    }

    private static void applyFogSource(final CoordinateSource source, final FogStateRecord record,
            final ContextCapabilities caps) {
        if (caps.isFogCoordinatesSupported()) {
            if (!record.isValid() || !source.equals(record.source)) {
                if (source == CoordinateSource.Depth) {
                    GL11.glFogi(EXTFogCoord.GL_FOG_COORDINATE_SOURCE_EXT, EXTFogCoord.GL_FRAGMENT_DEPTH_EXT);
                } else {
                    GL11.glFogi(EXTFogCoord.GL_FOG_COORDINATE_SOURCE_EXT, EXTFogCoord.GL_FOG_COORDINATE_EXT);
                }
            }
        }
    }

    private static void applyFogMode(final DensityFunction densityFunction, final FogStateRecord record) {
        int glMode = 0;
        switch (densityFunction) {
            case Exponential:
                glMode = GL11.GL_EXP;
                break;
            case Linear:
                glMode = GL11.GL_LINEAR;
                break;
            case ExponentialSquared:
                glMode = GL11.GL_EXP2;
                break;
        }

        if (!record.isValid() || record.fogMode != glMode) {
            GL11.glFogi(GL11.GL_FOG_MODE, glMode);
            record.fogMode = glMode;
        }
    }

    private static void applyFogHint(final Quality quality, final FogStateRecord record) {
        int glHint = 0;
        switch (quality) {
            case PerVertex:
                glHint = GL11.GL_FASTEST;
                break;
            case PerPixel:
                glHint = GL11.GL_NICEST;
                break;
        }

        if (!record.isValid() || record.fogHint != glHint) {
            GL11.glHint(GL11.GL_FOG_HINT, glHint);
            record.fogHint = glHint;
        }
    }
}
