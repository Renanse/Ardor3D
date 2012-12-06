/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.state;

import java.io.IOException;

import com.ardor3d.renderer.state.record.ShadingStateRecord;
import com.ardor3d.renderer.state.record.StateRecord;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;

/**
 * <code>ShadeState</code> maintains the interpolation of color between vertices. Smooth shades the colors with proper
 * linear interpolation, while flat provides no smoothing. If this state is not enabled, Smooth is used.
 */
public class ShadingState extends RenderState {

    public enum ShadingMode {
        /**
         * Pick the color of just one vertex of a triangle and rasterize all pixels of the triangle with this color.
         */
        Flat,
        /**
         * Smoothly interpolate the color values between the three colors of the three vertices. (Default)
         */
        Smooth;
    }

    // shade mode.
    protected ShadingMode _shadeMode = ShadingMode.Smooth;

    /**
     * Constructor instantiates a new <code>ShadeState</code> object with the default mode being smooth.
     */
    public ShadingState() {}

    /**
     * <code>getShade</code> returns the current shading mode.
     * 
     * @return the current shading mode.
     */
    public ShadingMode getShadingMode() {
        return _shadeMode;
    }

    /**
     * <code>setShadeMode</code> sets the current shading mode.
     * 
     * @param shadeMode
     *            the new shading mode.
     * @throws IllegalArgumentException
     *             if shadeMode is null
     */
    public void setShadingMode(final ShadingMode shadeMode) {
        if (shadeMode == null) {
            throw new IllegalArgumentException("shadeMode can not be null.");
        }
        _shadeMode = shadeMode;
        setNeedsRefresh(true);
    }

    @Override
    public StateType getType() {
        return StateType.Shading;
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        super.write(capsule);
        capsule.write(_shadeMode, "shadeMode", ShadingMode.Smooth);
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        super.read(capsule);
        _shadeMode = capsule.readEnum("shadeMode", ShadingMode.class, ShadingMode.Smooth);
    }

    @Override
    public StateRecord createStateRecord() {
        return new ShadingStateRecord();
    }
}
