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
import java.util.EnumSet;

import com.ardor3d.renderer.state.record.OffsetStateRecord;
import com.ardor3d.renderer.state.record.StateRecord;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;

/**
 * <code>OffsetState</code> controls depth offset for rendering.
 */
public class OffsetState extends RenderState {

    public enum OffsetType {
        /** Apply offset to filled polygons. */
        Fill,

        /** Apply offset to lines. */
        Line,

        /** Apply offset to points. */
        Point;
    }

    private final EnumSet<OffsetType> _enabledOffsets = EnumSet.noneOf(OffsetType.class);

    private float _factor;

    private float _units;

    /**
     * Constructor instantiates a new <code>OffsetState</code> object.
     */
    public OffsetState() {}

    /**
     * Sets an offset param to the zbuffer to be used when comparing an incoming fragment for depth buffer pass/fail.
     * 
     * @param offset
     *            Is multiplied by an implementation-specific value to create a constant depth offset. The initial value
     *            is 0.
     */
    public void setFactor(final float factor) {
        _factor = factor;
        setNeedsRefresh(true);
    }

    /**
     * @return the currently set offset factor.
     */
    public float getFactor() {
        return _factor;
    }

    /**
     * Sets an offset param to the zbuffer to be used when comparing an incoming fragment for depth buffer pass/fail.
     * 
     * @param units
     *            Is multiplied by an implementation-specific value to create a constant depth offset. The initial value
     *            is 0.
     */
    public void setUnits(final float units) {
        _units = units;
        setNeedsRefresh(true);
    }

    /**
     * @return the currently set offset units.
     */
    public float getUnits() {
        return _units;
    }

    /**
     * Enable or disable depth offset for a particular type.
     * 
     * @param type
     * @param enabled
     */
    public void setTypeEnabled(final OffsetType type, final boolean enabled) {
        if (enabled) {
            _enabledOffsets.add(type);
        } else {
            _enabledOffsets.remove(type);
        }
        setNeedsRefresh(true);
    }

    /**
     * 
     * @param type
     *            the type to check
     * @return true if offset is enabled for that type. (default is false for all types.)
     */
    public boolean isTypeEnabled(final OffsetType type) {
        return _enabledOffsets.contains(type);
    }

    @Override
    public StateType getType() {
        return StateType.Offset;
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        super.write(capsule);
        capsule.write(_factor, "factor", 0);
        capsule.write(_units, "units", 0);
        capsule.write(_enabledOffsets.contains(OffsetType.Fill), "typeFill", false);
        capsule.write(_enabledOffsets.contains(OffsetType.Line), "typeLine", false);
        capsule.write(_enabledOffsets.contains(OffsetType.Point), "typePoint", false);
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        super.read(capsule);
        _factor = capsule.readFloat("factor", 0);
        _units = capsule.readFloat("units", 0);
        _enabledOffsets.clear();
        if (capsule.readBoolean("typeFill", false)) {
            _enabledOffsets.add(OffsetType.Fill);
        }
        if (capsule.readBoolean("typeLine", false)) {
            _enabledOffsets.add(OffsetType.Line);
        }
        if (capsule.readBoolean("typePoint", false)) {
            _enabledOffsets.add(OffsetType.Point);
        }
    }

    @Override
    public StateRecord createStateRecord() {
        return new OffsetStateRecord();
    }
}
