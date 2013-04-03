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

import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.state.record.ColorMaskStateRecord;
import com.ardor3d.renderer.state.record.StateRecord;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;

/**
 * <code>ColorMaskState</code>
 */
public class ColorMaskState extends RenderState {

    protected boolean blue = true;
    protected boolean green = true;
    protected boolean red = true;
    protected boolean alpha = true;

    @Override
    public StateType getType() {
        return StateType.ColorMask;
    }

    public void setAll(final boolean on) {
        blue = on;
        green = on;
        red = on;
        alpha = on;
        setNeedsRefresh(true);
    }

    /**
     * @return Returns the alpha.
     */
    public boolean getAlpha() {
        return alpha;
    }

    /**
     * @param alpha
     *            The alpha to set.
     */
    public void setAlpha(final boolean alpha) {
        this.alpha = alpha;
        setNeedsRefresh(true);
    }

    /**
     * @return Returns the blue.
     */
    public boolean getBlue() {
        return blue;
    }

    /**
     * @param blue
     *            The blue to set.
     */
    public void setBlue(final boolean blue) {
        this.blue = blue;
        setNeedsRefresh(true);
    }

    /**
     * @return Returns the green.
     */
    public boolean getGreen() {
        return green;
    }

    /**
     * @param green
     *            The green to set.
     */
    public void setGreen(final boolean green) {
        this.green = green;
        setNeedsRefresh(true);
    }

    /**
     * @return Returns the red.
     */
    public boolean getRed() {
        return red;
    }

    /**
     * @param red
     *            The red to set.
     */
    public void setRed(final boolean red) {
        this.red = red;
        setNeedsRefresh(true);
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        super.write(capsule);
        capsule.write(blue, "blue", true);
        capsule.write(green, "green", true);
        capsule.write(red, "red", true);
        capsule.write(alpha, "alpha", true);
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        super.read(capsule);
        blue = capsule.readBoolean("blue", true);
        green = capsule.readBoolean("green", true);
        red = capsule.readBoolean("red", true);
        alpha = capsule.readBoolean("alpha", true);
    }

    @Override
    public StateRecord createStateRecord(final ContextCapabilities caps) {
        return new ColorMaskStateRecord();
    }
}
