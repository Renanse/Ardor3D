/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.renderer.state;

import java.io.IOException;

import com.ardor3d.renderer.ContextCapabilities;
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

    @Override
    public StateType getType() {
        return StateType.Wireframe;
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

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        super.write(capsule);
        capsule.write(_face, "face", Face.FrontAndBack);
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        super.read(capsule);
        _face = capsule.readEnum("face", Face.class, Face.FrontAndBack);
    }

    @Override
    public StateRecord createStateRecord(final ContextCapabilities caps) {
        return new WireframeStateRecord();
    }
}
