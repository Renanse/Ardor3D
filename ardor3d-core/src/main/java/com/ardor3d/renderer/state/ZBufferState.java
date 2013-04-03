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
import com.ardor3d.renderer.state.record.StateRecord;
import com.ardor3d.renderer.state.record.ZBufferStateRecord;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;

/**
 * <code>ZBufferState</code> maintains how the use of the depth buffer is to occur. Depth buffer comparisons are used to
 * evaluate what incoming fragment will be used. This buffer is based on z depth, or distance between the pixel source
 * and the eye.
 */
public class ZBufferState extends RenderState {

    public enum TestFunction {
        /**
         * Depth comparison never passes.
         */
        Never,
        /**
         * Depth comparison always passes.
         */
        Always,
        /**
         * Passes if the incoming value is the same as the stored value.
         */
        EqualTo,
        /**
         * Passes if the incoming value is not equal to the stored value.
         */
        NotEqualTo,
        /**
         * Passes if the incoming value is less than the stored value.
         */
        LessThan,
        /**
         * Passes if the incoming value is less than or equal to the stored value.
         */
        LessThanOrEqualTo,
        /**
         * Passes if the incoming value is greater than the stored value.
         */
        GreaterThan,
        /**
         * Passes if the incoming value is greater than or equal to the stored value.
         */
        GreaterThanOrEqualTo;

    }

    /** Depth function. */
    protected TestFunction _function = TestFunction.LessThan;
    /** Depth mask is writable or not. */
    protected boolean _writable = true;

    /**
     * Constructor instantiates a new <code>ZBufferState</code> object. The initial values are TestFunction.LessThan and
     * depth writing on.
     */
    public ZBufferState() {}

    /**
     * <code>getFunction</code> returns the current depth function.
     * 
     * @return the depth function currently used.
     */
    public TestFunction getFunction() {
        return _function;
    }

    /**
     * <code>setFunction</code> sets the depth function.
     * 
     * @param function
     *            the depth function.
     * @throws IllegalArgumentException
     *             if function is null
     */
    public void setFunction(final TestFunction function) {
        if (function == null) {
            throw new IllegalArgumentException("function can not be null.");
        }
        _function = function;
        setNeedsRefresh(true);
    }

    /**
     * <code>isWritable</code> returns if the depth mask is writable or not.
     * 
     * @return true if the depth mask is writable, false otherwise.
     */
    public boolean isWritable() {
        return _writable;
    }

    /**
     * <code>setWritable</code> sets the depth mask writable or not.
     * 
     * @param writable
     *            true to turn on depth writing, false otherwise.
     */
    public void setWritable(final boolean writable) {
        _writable = writable;
        setNeedsRefresh(true);
    }

    @Override
    public StateType getType() {
        return StateType.ZBuffer;
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        super.write(capsule);
        capsule.write(_function, "function", TestFunction.LessThan);
        capsule.write(_writable, "writable", true);
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        super.read(capsule);
        _function = capsule.readEnum("function", TestFunction.class, TestFunction.LessThan);
        _writable = capsule.readBoolean("writable", true);
    }

    @Override
    public StateRecord createStateRecord(final ContextCapabilities caps) {
        return new ZBufferStateRecord();
    }
}
