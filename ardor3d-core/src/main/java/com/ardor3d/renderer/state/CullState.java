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
import com.ardor3d.renderer.state.record.CullStateRecord;
import com.ardor3d.renderer.state.record.StateRecord;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;

/**
 * <code>CullState</code> determines which side of a model will be visible when it is rendered. By default, both sides
 * are visible. Define front as the side that traces its vertices counter clockwise and back as the side that traces its
 * vertices clockwise, a side (front or back) can be culled, or not shown when the model is rendered. Instead, the side
 * will be transparent. <br>
 * <b>NOTE:</b> Any object that is placed in the transparent queue with two sided transparency will not use the
 * cullstate that is attached to it. Instead, using the CullStates necessary for rendering two sided transparency.
 */
public class CullState extends RenderState {

    public enum Face {
        /** Neither front or back face is culled. This is default. */
        None,
        /** Cull the front faces. */
        Front,
        /** Cull the back faces. */
        Back,
        /** Cull both the front and back faces. */
        FrontAndBack;
    }

    public enum PolygonWind {
        /** Polygons whose vertices are specified in CCW order are front facing. This is default. */
        CounterClockWise,
        /** Polygons whose vertices are specified in CW order are front facing. */
        ClockWise;
    }

    /** The cull face set for this CullState. */
    private Face cullFace = Face.None;

    /** The polygonWind order set for this CullState. */
    private PolygonWind polygonWind = PolygonWind.CounterClockWise;

    @Override
    public StateType getType() {
        return StateType.Cull;
    }

    /**
     * @param face
     *            The new face to cull.
     */
    public void setCullFace(final Face face) {
        cullFace = face;
        setNeedsRefresh(true);
    }

    /**
     * @return the currently set face to cull.
     */
    public Face getCullFace() {
        return cullFace;
    }

    /**
     * @param windOrder
     *            The new polygonWind order.
     */
    public void setPolygonWind(final PolygonWind windOrder) {
        polygonWind = windOrder;
        setNeedsRefresh(true);
    }

    /**
     * @return the currently set polygonWind order.
     */
    public PolygonWind getPolygonWind() {
        return polygonWind;
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        super.write(capsule);
        capsule.write(cullFace, "cullFace", Face.None);
        capsule.write(polygonWind, "polygonWind", PolygonWind.CounterClockWise);
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        super.read(capsule);
        cullFace = capsule.readEnum("cullFace", Face.class, Face.None);
        polygonWind = capsule.readEnum("polygonWind", PolygonWind.class, PolygonWind.CounterClockWise);
    }

    @Override
    public StateRecord createStateRecord(final ContextCapabilities caps) {
        return new CullStateRecord();
    }
}
