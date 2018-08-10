/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.interact.widget;

import java.io.IOException;

import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Quaternion;
import com.ardor3d.scenegraph.shape.Arrow;
import com.ardor3d.scenegraph.shape.Cylinder;
import com.ardor3d.scenegraph.shape.Pyramid;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;

/**
 * <code>InteractArrow</code> is basically a cylinder with a pyramid on top. It extends the basic Arrow shape to include
 * a customizable gap between arrow head and base, and base and origin. This shape points along the +zaxis instead.
 */
public class InteractArrow extends Arrow {

    protected double _lengthGap = 0;
    protected double _tipGap = 0;

    protected static final Quaternion rotator = new Quaternion().applyRotationX(MathUtils.HALF_PI);

    public InteractArrow() {}

    public InteractArrow(final String name) {
        this(name, 1, .25);
    }

    public InteractArrow(final String name, final double length, final double width) {
        this(name, length, width, 0, 0);
    }

    public InteractArrow(final String name, final double length, final double width, final double lengthGap,
            final double tipGap) {
        super(name);
        _length = length;
        _width = width;
        _lengthGap = lengthGap;
        _tipGap = tipGap;

        buildArrow();
    }

    @Override
    public void buildArrow() {
        detachAllChildren();

        // Start with cylinder base:
        final Cylinder base = new Cylinder("base", 4, 16, _width * 0.75, _length - _lengthGap);
        base.getMeshData().translatePoints(0, 0, (_lengthGap + _length) * 0.5);
        attachChild(base);
        base.updateModelBound();

        // Add the pyramid tip.
        final double tipLength = _length / 2.0;
        final Pyramid tip = new Pyramid("tip", 2 * _width, tipLength);
        tip.getMeshData().translatePoints(0, _tipGap + _length + 0.5 * tipLength, 0);
        tip.getMeshData().rotatePoints(InteractArrow.rotator);
        tip.getMeshData().rotateNormals(InteractArrow.rotator);

        attachChild(tip);
        tip.updateModelBound();

    }

    public double getLengthGap() {
        return _lengthGap;
    }

    public void setLengthGap(final double lengthGap) {
        _lengthGap = lengthGap;
    }

    public double getTipGap() {
        return _tipGap;
    }

    public void setTipGap(final double tipGap) {
        _tipGap = tipGap;
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        super.write(capsule);
        capsule.write(_lengthGap, "lengthGap", 0);
        capsule.write(_tipGap, "tipGap", 0);
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        super.read(capsule);
        _lengthGap = capsule.readDouble("lengthGap", 0);
        _tipGap = capsule.readDouble("tipGap", 0);
    }
}
