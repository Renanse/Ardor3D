/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.scenegraph.shape;

import java.io.IOException;

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Matrix3;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.hint.LightCombineMode;
import com.ardor3d.scenegraph.hint.TextureCombineMode;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;

/**
 * <code>AxisRods</code> is a convenience shape representing three axes in space.
 */
public class AxisRods extends Node {

    protected static final ColorRGBA xAxisColor = new ColorRGBA(1, 0, 0, .4f);
    protected static final ColorRGBA yAxisColor = new ColorRGBA(0, 1, 0, .25f);
    protected static final ColorRGBA zAxisColor = new ColorRGBA(0, 0, 1, .4f);

    protected double length;
    protected double width;
    protected boolean rightHanded;

    protected Arrow xAxis;
    protected Arrow yAxis;
    protected Arrow zAxis;

    public AxisRods() {}

    public AxisRods(final String name) {
        this(name, true, 1);
    }

    public AxisRods(final String name, final boolean rightHanded, final double baseScale) {
        this(name, rightHanded, baseScale, baseScale * 0.125);
    }

    public AxisRods(final String name, final boolean rightHanded, final double length, final double width) {
        super(name);
        this.length = length;
        this.width = width;
        this.rightHanded = rightHanded;
        getSceneHints().setLightCombineMode(LightCombineMode.Off);
        getSceneHints().setTextureCombineMode(TextureCombineMode.Off);

        buildAxis();
    }

    protected void buildAxis() {
        xAxis = new Arrow("_xAxis", length, width);
        xAxis.setDefaultColor(xAxisColor);
        xAxis.setRotation(new Matrix3().fromAngles(0, 0, -90 * MathUtils.DEG_TO_RAD));
        xAxis.setTranslation(length * .5, 0, 0);
        attachChild(xAxis);

        yAxis = new Arrow("yAxis", length, width);
        yAxis.setDefaultColor(yAxisColor);
        yAxis.setTranslation(0, length * .5, 0);
        attachChild(yAxis);

        zAxis = new Arrow("zAxis", length, width);
        zAxis.setDefaultColor(zAxisColor);
        if (rightHanded) {
            zAxis.setRotation(new Matrix3().fromAngles(90 * MathUtils.DEG_TO_RAD, 0, 0));
            zAxis.setTranslation(0, 0, length * .5);
        } else {
            zAxis.setRotation(new Matrix3().fromAngles(-90 * MathUtils.DEG_TO_RAD, 0, 0));
            zAxis.setTranslation(0, 0, -length * .5);
        }
        attachChild(zAxis);
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        super.write(capsule);
        capsule.write(length, "length", 1);
        capsule.write(width, "width", 0.125);
        capsule.write(rightHanded, "rightHanded", true);
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        super.read(capsule);
        length = capsule.readDouble("length", 1);
        width = capsule.readDouble("width", 0.125);
        rightHanded = capsule.readBoolean("rightHanded", true);
        buildAxis();
    }

    public double getLength() {
        return length;
    }

    public void setLength(final double length) {
        this.length = length;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(final double width) {
        this.width = width;
    }

    public Arrow getxAxis() {
        return xAxis;
    }

    public Arrow getyAxis() {
        return yAxis;
    }

    public Arrow getzAxis() {
        return zAxis;
    }
}
