/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
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
import com.ardor3d.renderer.state.record.ClipStateRecord;
import com.ardor3d.renderer.state.record.StateRecord;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;

/**
 * <code>ClipState</code> specifies a plane to test for clipping of the nodes. This can be used to take "slices" out of
 * geometric objects. ClipPlane can add an additional (to the normal frustum planes) six planes to clip against.
 */
public class ClipState extends RenderState {

    /**
     * Max supported number of user-defined clip planes in Ardor3D. Note that a user may or may not have access to all 6
     * (or even any!) or their particular platform. Check ContextCapabilities to confirm as necessary.
     */
    public static final int MAX_CLIP_PLANES = 6;

    protected boolean[] enabledClipPlanes = new boolean[MAX_CLIP_PLANES];

    protected double[][] planeEquations = new double[MAX_CLIP_PLANES][4];

    @Override
    public StateType getType() {
        return StateType.Clip;
    }

    /**
     * Enables/disables a specific clip plane
     * 
     * @param planeIndex
     *            Plane to enable/disable (CLIP_PLANE0-CLIP_PLANE5)
     * @param enabled
     *            true/false
     */
    public void setEnableClipPlane(final int planeIndex, final boolean enabled) {
        if (planeIndex < 0 || planeIndex >= MAX_CLIP_PLANES) {
            return;
        }

        enabledClipPlanes[planeIndex] = enabled;
        setNeedsRefresh(true);
    }

    /**
     * Sets plane equation for a specific clip plane
     * 
     * @param planeIndex
     *            Plane to set equation for (CLIP_PLANE0-CLIP_PLANE5)
     * @param clipX
     *            plane x variable
     * @param clipY
     *            plane y variable
     * @param clipZ
     *            plane z variable
     * @param clipW
     *            plane w variable
     */
    public void setClipPlaneEquation(final int planeIndex, final double clipX, final double clipY, final double clipZ,
            final double clipW) {
        if (planeIndex < 0 || planeIndex >= MAX_CLIP_PLANES) {
            return;
        }

        planeEquations[planeIndex][0] = clipX;
        planeEquations[planeIndex][1] = clipY;
        planeEquations[planeIndex][2] = clipZ;
        planeEquations[planeIndex][3] = clipW;
        setNeedsRefresh(true);
    }

    /**
     * @param index
     *            plane to check
     * @return true if given clip plane is enabled
     */
    public boolean getPlaneEnabled(final int index) {
        return enabledClipPlanes[index];
    }

    public double[] getPlaneEquations(final int plane) {
        return planeEquations[plane];
    }

    public double getPlaneEquation(final int plane, final int eqIndex) {
        return planeEquations[plane][eqIndex];
    }

    public void setPlaneEq(final int plane, final int eqIndex, final double value) {
        planeEquations[plane][eqIndex] = value;
        setNeedsRefresh(true);
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        super.write(capsule);
        capsule.write(enabledClipPlanes, "enabledClipPlanes", new boolean[MAX_CLIP_PLANES]);
        capsule.write(planeEquations, "planeEquations", new double[MAX_CLIP_PLANES][4]);
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        super.read(capsule);
        enabledClipPlanes = capsule.readBooleanArray("enabledClipPlanes", new boolean[MAX_CLIP_PLANES]);
        planeEquations = capsule.readDoubleArray2D("planeEquations", new double[MAX_CLIP_PLANES][4]);
    }

    @Override
    public StateRecord createStateRecord(final ContextCapabilities caps) {
        return new ClipStateRecord(caps);
    }
}
