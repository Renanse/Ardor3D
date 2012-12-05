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

import org.lwjgl.opengl.GL11;

import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.lwjgl.LwjglRenderer;
import com.ardor3d.renderer.state.OffsetState;
import com.ardor3d.renderer.state.OffsetState.OffsetType;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.record.OffsetStateRecord;

public abstract class LwjglOffsetStateUtil {

    public static void apply(final LwjglRenderer renderer, final OffsetState state) {
        // ask for the current state record
        final RenderContext context = ContextManager.getCurrentContext();
        final OffsetStateRecord record = (OffsetStateRecord) context.getStateRecord(StateType.Offset);
        context.setCurrentState(StateType.Offset, state);

        if (state.isEnabled()) {
            // enable any set offset types
            setOffsetEnabled(OffsetType.Fill, state.isTypeEnabled(OffsetType.Fill), record);
            setOffsetEnabled(OffsetType.Line, state.isTypeEnabled(OffsetType.Line), record);
            setOffsetEnabled(OffsetType.Point, state.isTypeEnabled(OffsetType.Point), record);

            // set factor and units.
            setOffset(state.getFactor(), state.getUnits(), record);
        } else {
            // disable all offset types
            setOffsetEnabled(OffsetType.Fill, false, record);
            setOffsetEnabled(OffsetType.Line, false, record);
            setOffsetEnabled(OffsetType.Point, false, record);

            // set factor and units to default 0, 0.
            setOffset(0, 0, record);
        }

        if (!record.isValid()) {
            record.validate();
        }
    }

    private static void setOffsetEnabled(final OffsetType type, final boolean typeEnabled,
            final OffsetStateRecord record) {
        final int glType = getGLType(type);
        if (!record.isValid() || typeEnabled != record.enabledOffsets.contains(type)) {
            if (typeEnabled) {
                GL11.glEnable(glType);
            } else {
                GL11.glDisable(glType);
            }
        }
    }

    private static void setOffset(final float factor, final float units, final OffsetStateRecord record) {
        if (!record.isValid() || record.factor != factor || record.units != units) {
            GL11.glPolygonOffset(factor, units);
            record.factor = factor;
            record.units = units;
        }
    }

    private static int getGLType(final OffsetType type) {
        switch (type) {
            case Fill:
                return GL11.GL_POLYGON_OFFSET_FILL;
            case Line:
                return GL11.GL_POLYGON_OFFSET_LINE;
            case Point:
                return GL11.GL_POLYGON_OFFSET_POINT;
        }
        throw new IllegalArgumentException("invalid type: " + type);
    }
}
