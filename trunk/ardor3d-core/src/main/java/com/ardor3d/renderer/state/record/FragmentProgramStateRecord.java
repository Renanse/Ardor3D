/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.state.record;

import com.ardor3d.renderer.state.FragmentProgramState;

public class FragmentProgramStateRecord extends StateRecord {
    private FragmentProgramState reference = null;

    public FragmentProgramState getReference() {
        return reference;
    }

    public void setReference(final FragmentProgramState reference) {
        this.reference = reference;
    }

    @Override
    public void invalidate() {
        super.invalidate();
        reference = null;
    }
}
