/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.renderer.state.record;

public abstract class StateRecord {

    // If false, don't trust any of the values in this record.
    protected boolean valid = false;

    /**
     * @return true if ardor3d thinks this state holds trusted information about the opengl state machine.
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Invalidate this record - iow, we don't trust this record's information about the opengl state machine.
     */
    public void invalidate() {
        valid = false;
    }

    /**
     * Validate this record - iow, we trust this record's information about the opengl state machine.
     */
    public void validate() {
        valid = true;
    }

}
