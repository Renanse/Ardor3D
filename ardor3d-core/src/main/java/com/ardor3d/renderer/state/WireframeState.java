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

import com.ardor3d.renderer.state.record.StateRecord;
import com.ardor3d.renderer.state.record.WireframeStateRecord;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;

/**
 * <code>WireframeState</code> maintains whether a node and it's children should be drawn in wireframe or solid fill. By
 * default all nodes are rendered solid.
 */
public class WireframeState extends RenderState {

    public enum Face {
        /** The front will be wireframed, but the back will be solid. */
        Front,
        /** The back will be wireframed, but the front will be solid. */
        Back,
        /** Both sides of the model are wireframed. */
        FrontAndBack;
    }

    /** Default wireframe of front and back. */
    protected Face _face = Face.FrontAndBack;
    /** Default line width of 1 pixel. */
    protected float _lineWidth = 1.0f;
    /** Default line style */
    protected boolean _antialiased = false;

    @Override
    public StateType getType() {
        return StateType.Wireframe;
    }

    /**
     * <code>setLineWidth</code> sets the width of lines the wireframe is drawn in. Attempting to set a line width
     * smaller than 0.0 throws an <code>IllegalArgumentException</code>.
     * 
     * @param width
     *            the line width, in pixels
     */
    public void setLineWidth(final float width) {
        if (width < 0.0f) {
            throw new IllegalArgumentException("Line width must be positive");
        }

        _lineWidth = width;
        setNeedsRefresh(true);
    }

    /**
     * Returns the current lineWidth.
     * 
     * @return the current LineWidth
     */
    public float getLineWidth() {
        return _lineWidth;
    }

    /**
     * <code>setFace</code> sets which face will recieve the wireframe.
     * 
     * @param face
     *            which face will be rendered in wireframe.
     * @throws IllegalArgumentException
     *             if face is null
     */
    public void setFace(final Face face) {
        if (face == null) {
            throw new IllegalArgumentException("face can not be null.");
        }
        _face = face;
        setNeedsRefresh(true);
    }

    /**
     * Returns the face state of this wireframe state.
     * 
     * @return The face state (one of WS_FRONT, WS_BACK, or WS_FRONT_AND_BACK)
     */
    public Face getFace() {
        return _face;
    }

    /**
     * Set whether this wireframe should use antialiasing when drawing lines. May decrease performance. If you want to
     * enabled antialiasing, you should also use an alphastate with a source of SourceFunction.SourceAlpha and a
     * destination of DB_ONE_MINUS_SRC_ALPHA or DB_ONE.
     * 
     * @param antialiased
     *            true for using smoothed antialiased lines.
     */
    public void setAntialiased(final boolean antialiased) {
        _antialiased = antialiased;
        setNeedsRefresh(true);
    }

    /**
     * @return whether this wireframe uses antialiasing for drawing lines.
     */
    public boolean isAntialiased() {
        return _antialiased;
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        super.write(capsule);
        capsule.write(_face, "face", Face.FrontAndBack);
        capsule.write(_lineWidth, "lineWidth", 1);
        capsule.write(_antialiased, "antialiased", false);
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        super.read(capsule);
        _face = capsule.readEnum("face", Face.class, Face.FrontAndBack);
        _lineWidth = capsule.readFloat("lineWidth", 1);
        _antialiased = capsule.readBoolean("antialiased", false);
    }

    @Override
    public StateRecord createStateRecord() {
        return new WireframeStateRecord();
    }
}
