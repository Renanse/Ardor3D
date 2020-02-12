/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.effect.water;

/**
 * Sample implementation of a water height generator.
 */
public class WaterHeightGenerator implements HeightGenerator {
    private double scalexsmall = 0.04;
    private double scaleysmall = 0.02;
    private double scalexbig = 0.015;
    private double scaleybig = 0.01;
    private double heightsmall = 3.0;
    private double heightbig = 10.0;
    private double speedsmall = 1.0;
    private double speedbig = 0.5;
    private int octaves = 2;

    public double getHeight(final double x, final double z, final double time) {
        final double zval = z * scaleybig * 4 + time * speedbig * 4;
        double height = Math.sin(zval);
        height *= heightbig;

        if (octaves > 0) {
            final double height2 = ImprovedNoise.noise(x * scaleybig, z * scalexbig, time * speedbig) * heightbig;
            height = height * 0.4 + height2 * 0.6;
        }
        if (octaves > 1) {
            height += ImprovedNoise.noise(x * scaleysmall, z * scalexsmall, time * speedsmall) * heightsmall;
        }
        if (octaves > 2) {
            height += ImprovedNoise.noise(x * scaleysmall * 2.0, z * scalexsmall * 2.0, time * speedsmall * 1.5)
                    * heightsmall * 0.5;
        }
        if (octaves > 3) {
            height += ImprovedNoise.noise(x * scaleysmall * 4.0, z * scalexsmall * 4.0, time * speedsmall * 2.0)
                    * heightsmall * 0.25;
        }

        return height; // + waterHeight
    }

    public double getScalexsmall() {
        return scalexsmall;
    }

    public void setScalexsmall(final double scalexsmall) {
        this.scalexsmall = scalexsmall;
    }

    public double getScaleysmall() {
        return scaleysmall;
    }

    public void setScaleysmall(final double scaleysmall) {
        this.scaleysmall = scaleysmall;
    }

    public double getScalexbig() {
        return scalexbig;
    }

    public void setScalexbig(final double scalexbig) {
        this.scalexbig = scalexbig;
    }

    public double getScaleybig() {
        return scaleybig;
    }

    public void setScaleybig(final double scaleybig) {
        this.scaleybig = scaleybig;
    }

    public double getHeightsmall() {
        return heightsmall;
    }

    public void setHeightsmall(final double heightsmall) {
        this.heightsmall = heightsmall;
    }

    public double getHeightbig() {
        return heightbig;
    }

    public void setHeightbig(final double heightbig) {
        this.heightbig = heightbig;
    }

    public double getSpeedsmall() {
        return speedsmall;
    }

    public void setSpeedsmall(final double speedsmall) {
        this.speedsmall = speedsmall;
    }

    public double getSpeedbig() {
        return speedbig;
    }

    public void setSpeedbig(final double speedbig) {
        this.speedbig = speedbig;
    }

    public int getOctaves() {
        return octaves;
    }

    public void setOctaves(final int octaves) {
        this.octaves = octaves;
    }

    @Override
    public double getMaximumHeight() {
        return 15.0;
    }
}
