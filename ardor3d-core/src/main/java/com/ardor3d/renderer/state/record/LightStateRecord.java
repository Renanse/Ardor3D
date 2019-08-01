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

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.util.geom.BufferUtils;

public class LightStateRecord extends StateRecord {
    private final List<LightRecord> lightList = new ArrayList<LightRecord>();
    private int lightMask;
    private int backLightMask;
    private boolean twoSidedOn;
    public ColorRGBA globalAmbient = new ColorRGBA(-1, -1, -1, -1);
    private boolean enabled;
    private boolean localViewer;
    private boolean separateSpecular;

    // buffer for light colors.
    public FloatBuffer lightBuffer = BufferUtils.createColorBuffer(1);

    public int getBackLightMask() {
        return backLightMask;
    }

    public void setBackLightMask(final int backLightMask) {
        this.backLightMask = backLightMask;
    }

    public LightRecord getLightRecord(final int index) {
        if (lightList.size() <= index) {
            return null;
        }

        return lightList.get(index);
    }

    public void setLightRecord(final LightRecord lr, final int index) {
        while (lightList.size() <= index) {
            lightList.add(null);
        }

        lightList.set(index, lr);
    }

    public int getLightMask() {
        return lightMask;
    }

    public void setLightMask(final int lightMask) {
        this.lightMask = lightMask;
    }

    public boolean isTwoSidedOn() {
        return twoSidedOn;
    }

    public void setTwoSidedOn(final boolean twoSidedOn) {
        this.twoSidedOn = twoSidedOn;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isLocalViewer() {
        return localViewer;
    }

    public void setLocalViewer(final boolean localViewer) {
        this.localViewer = localViewer;
    }

    public boolean isSeparateSpecular() {
        return separateSpecular;
    }

    public void setSeparateSpecular(final boolean seperateSpecular) {
        separateSpecular = seperateSpecular;
    }

    @Override
    public void invalidate() {
        super.invalidate();
        for (final LightRecord record : lightList) {
            record.invalidate();
        }

        lightMask = -1;
        backLightMask = -1;
        twoSidedOn = false;
        enabled = false;
        localViewer = false;
        separateSpecular = false;
        globalAmbient.set(-1, -1, -1, -1);
    }

    @Override
    public void validate() {
        super.validate();
        for (final LightRecord record : lightList) {
            record.validate();
        }
    }
}
