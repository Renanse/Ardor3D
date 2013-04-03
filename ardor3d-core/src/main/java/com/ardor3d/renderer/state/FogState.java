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

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.state.record.FogStateRecord;
import com.ardor3d.renderer.state.record.StateRecord;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;

/**
 * <code>FogState</code> maintains the fog qualities for a node and it's children. The fogging function, color, start,
 * end and density are all set and maintained. Please note that fog does not affect alpha.
 */
public class FogState extends RenderState {

    public enum DensityFunction {
        /**
         * The fog blending function defined as: (end - z) / (end - start).
         */
        Linear,
        /**
         * The fog blending function defined as: e^-(density*z)
         */
        Exponential,
        /**
         * The fog blending function defined as: e^((-density*z)^2)
         */
        ExponentialSquared;
    }

    public enum CoordinateSource {
        /** The source of the fogging value is based on the depth buffer */
        Depth,
        /** The source of the fogging value is based on the specified fog coordinates */
        FogCoords
    }

    public enum Quality {
        /**
         * Each vertex color is altered by the fogging function.
         */
        PerVertex,
        /**
         * Each pixel color is altered by the fogging function.
         */
        PerPixel;
    }

    // fogging attributes.
    protected float start = 0;
    protected float end = 1;
    protected float density = 1.0f;
    protected final ColorRGBA color = new ColorRGBA();
    protected DensityFunction densityFunction = DensityFunction.Exponential;
    protected Quality quality = Quality.PerVertex;
    protected CoordinateSource source = CoordinateSource.Depth;

    /**
     * Constructor instantiates a new <code>FogState</code> with default fog values.
     */
    public FogState() {}

    /**
     * <code>setQuality</code> sets the quality used for the fog attributes.
     * 
     * @param quality
     *            the quality used for the fog application.
     * @throws IllegalArgumentException
     *             if quality is null
     */
    public void setQuality(final Quality quality) {
        if (quality == null) {
            throw new IllegalArgumentException("quality can not be null.");
        }
        this.quality = quality;
        setNeedsRefresh(true);
    }

    /**
     * <code>setDensityFunction</code> sets the density function used for the fog blending.
     * 
     * @param function
     *            the function used for the fog density.
     * @throws IllegalArgumentException
     *             if function is null
     */
    public void setDensityFunction(final DensityFunction function) {
        if (function == null) {
            throw new IllegalArgumentException("function can not be null.");
        }
        densityFunction = function;
        setNeedsRefresh(true);
    }

    /**
     * <code>setColor</code> sets the color of the fog.
     * 
     * @param color
     *            the color of the fog. This value is COPIED into the state. Further changes to the object after calling
     *            this method will have no affect on this state.
     */
    public void setColor(final ReadOnlyColorRGBA color) {
        this.color.set(color);
        setNeedsRefresh(true);
    }

    /**
     * <code>setDensity</code> sets the density of the fog. This value is clamped to [0, 1].
     * 
     * @param density
     *            the density of the fog.
     */
    public void setDensity(float density) {
        if (density < 0) {
            density = 0;
        }

        if (density > 1) {
            density = 1;
        }
        this.density = density;
        setNeedsRefresh(true);
    }

    /**
     * <code>setEnd</code> sets the end distance, or the distance where fog is at it's thickest.
     * 
     * @param end
     *            the distance where the fog is the thickest.
     */
    public void setEnd(final float end) {
        this.end = end;
        setNeedsRefresh(true);
    }

    /**
     * <code>setStart</code> sets the start distance, or where fog begins to be applied.
     * 
     * @param start
     *            the start distance of the fog.
     */
    public void setStart(final float start) {
        this.start = start;
        setNeedsRefresh(true);
    }

    public void setSource(final CoordinateSource source) {
        this.source = source;
    }

    public CoordinateSource getSource() {
        return source;
    }

    @Override
    public StateType getType() {
        return StateType.Fog;
    }

    public Quality getQuality() {
        return quality;
    }

    public ReadOnlyColorRGBA getColor() {
        return color;
    }

    public float getDensity() {
        return density;
    }

    public DensityFunction getDensityFunction() {
        return densityFunction;
    }

    public float getEnd() {
        return end;
    }

    public float getStart() {
        return start;
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        super.write(capsule);
        capsule.write(start, "start", 0);
        capsule.write(end, "end", 0);
        capsule.write(density, "density", 0);
        capsule.write(color, "color", new ColorRGBA(ColorRGBA.WHITE));
        capsule.write(densityFunction, "densityFunction", DensityFunction.Exponential);
        capsule.write(quality, "applyFunction", Quality.PerPixel);
        capsule.write(source, "source", CoordinateSource.Depth);
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        super.read(capsule);
        start = capsule.readFloat("start", 0);
        end = capsule.readFloat("end", 0);
        density = capsule.readFloat("density", 0);
        color.set((ColorRGBA) capsule.readSavable("color", new ColorRGBA(ColorRGBA.WHITE)));
        densityFunction = capsule.readEnum("densityFunction", DensityFunction.class, DensityFunction.Exponential);
        quality = capsule.readEnum("applyFunction", Quality.class, Quality.PerPixel);
        source = capsule.readEnum("source", CoordinateSource.class, CoordinateSource.Depth);
    }

    @Override
    public StateRecord createStateRecord(final ContextCapabilities caps) {
        return new FogStateRecord();
    }

}
