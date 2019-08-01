/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.renderer.state.record;

public class ColorMaskStateRecord extends StateRecord {
    public boolean red = true;
    public boolean green = true;
    public boolean blue = true;
    public boolean alpha = true;

    public boolean is(final boolean red, final boolean green, final boolean blue, final boolean alpha) {
        if (this.alpha != alpha) {
            return false;
        } else if (this.red != red) {
            return false;
        } else if (this.green != green) {
            return false;
        } else if (this.blue != blue) {
            return false;
        } else {
            return true;
        }
    }

    public void set(final boolean red, final boolean green, final boolean blue, final boolean alpha) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
    }

    @Override
    public void invalidate() {
        super.invalidate();

        red = green = blue = alpha = true;
    }
}
