/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.effect.particle;

import java.io.IOException;

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;

/**
 * <code>RampEntry</code> defines an entry for a ParticleAppearanceRamp.
 * 
 * @see ParticleAppearanceRamp
 */
public class RampEntry implements Savable {

    public static final double DEFAULT_OFFSET = 0.05f; // (5% of lifetime)
    public static final double DEFAULT_SIZE = -1; // special case -> negative = no size change at this entry
    public static final double DEFAULT_SPIN = Float.MAX_VALUE; // special case -> no spin change
    public static final double DEFAULT_MASS = Float.MAX_VALUE; // special case -> no mass change
    public static final ColorRGBA DEFAULT_COLOR = null; // special case -> no color change

    protected double _offset = DEFAULT_OFFSET;
    protected ColorRGBA _color = DEFAULT_COLOR; // no color change at this entry
    protected double _size = DEFAULT_SIZE;
    protected double _spin = DEFAULT_SPIN;
    protected double _mass = DEFAULT_MASS;

    public RampEntry() {}

    /**
     * Construct new addition to color ramp
     * 
     * @param offset
     *            amount of time (as a percent of total lifetime) between the last appearance and this one.
     */
    public RampEntry(final double offset) {
        setOffset(offset);
    }

    public ReadOnlyColorRGBA getColor() {
        return _color;
    }

    public void setColor(final ReadOnlyColorRGBA color) {
        if (color != null) {
            if (_color != null) {
                _color.set(color);
            } else {
                _color = new ColorRGBA(color);
            }
        } else {
            _color = null;
        }
    }

    public boolean hasColorSet() {
        return _color != null; // DEFAULT_COLOR is null
    }

    public double getSize() {
        return _size;
    }

    public void setSize(final double size) {
        _size = size;
    }

    public boolean hasSizeSet() {
        return _size != DEFAULT_SIZE;
    }

    public double getSpin() {
        return _spin;
    }

    public void setSpin(final double spin) {
        _spin = spin;
    }

    public boolean hasSpinSet() {
        return _spin != DEFAULT_SPIN;
    }

    public double getMass() {
        return _mass;
    }

    public void setMass(final double mass) {
        _mass = mass;
    }

    public boolean hasMassSet() {
        return _mass != DEFAULT_MASS;
    }

    public double getOffset() {
        return _offset;
    }

    public void setOffset(final double offset) {
        _offset = offset;
    }

    public Class<? extends RampEntry> getClassTag() {
        return getClass();
    }

    public void read(final InputCapsule capsule) throws IOException {
        _offset = capsule.readDouble("offsetMS", DEFAULT_OFFSET);
        _size = capsule.readDouble("size", DEFAULT_SIZE);
        _spin = capsule.readDouble("spin", DEFAULT_SPIN);
        _mass = capsule.readDouble("mass", DEFAULT_MASS);
        _color = (ColorRGBA) capsule.readSavable("color", DEFAULT_COLOR);
    }

    public void write(final OutputCapsule capsule) throws IOException {
        capsule.write(_offset, "offsetMS", DEFAULT_OFFSET);
        capsule.write(_size, "size", DEFAULT_SIZE);
        capsule.write(_spin, "spin", DEFAULT_SPIN);
        capsule.write(_mass, "mass", DEFAULT_MASS);
        capsule.write(_color, "color", DEFAULT_COLOR);
    }

    private static String convColorToHex(final ColorRGBA color) {
        if (color == null) {
            return null;
        }
        String sRed = Integer.toHexString((int) (color.getRed() * 255 + .5f));
        if (sRed.length() == 1) {
            sRed = "0" + sRed;
        }
        String sGreen = Integer.toHexString((int) (color.getGreen() * 255 + .5f));
        if (sGreen.length() == 1) {
            sGreen = "0" + sGreen;
        }
        String sBlue = Integer.toHexString((int) (color.getBlue() * 255 + .5f));
        if (sBlue.length() == 1) {
            sBlue = "0" + sBlue;
        }
        return "#" + sRed + sGreen + sBlue;
    }

    @Override
    public String toString() {

        final StringBuilder builder = new StringBuilder();
        if (_offset > 0) {
            builder.append("prev+");
            builder.append((int) (_offset * 100));
            builder.append("% age...");
        }
        if (_color != DEFAULT_COLOR) {
            builder.append("  color:");
            builder.append(convColorToHex(_color).toUpperCase());
            builder.append(" a: ");
            builder.append((int) (_color.getAlpha() * 100));
            builder.append("%");
        }

        if (_size != DEFAULT_SIZE) {
            builder.append("  size: " + _size);
        }

        if (_mass != DEFAULT_MASS) {
            builder.append("  mass: " + _spin);
        }

        if (_spin != DEFAULT_SPIN) {
            builder.append("  spin: " + _spin);
        }

        return builder.toString();
    }
}
