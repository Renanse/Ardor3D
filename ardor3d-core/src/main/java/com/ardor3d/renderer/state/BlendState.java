/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
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
import com.ardor3d.renderer.state.record.BlendStateRecord;
import com.ardor3d.renderer.state.record.StateRecord;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;

/**
 * <code>BlendState</code> maintains the state of the blending values of a particular node and its children. The blend
 * state provides a method for blending a source pixel with a destination pixel. The blend value provides a transparent
 * or translucent surfaces. For example, this would allow for the rendering of green glass. Where you could see all
 * objects behind this green glass but they would be tinted green.
 */
public class BlendState extends RenderState {

    public enum SourceFunction {
        /**
         * The source value of the blend function is all zeros.
         */
        Zero(false),
        /**
         * The source value of the blend function is all ones.
         */
        One(false),
        /**
         * The source value of the blend function is the destination color.
         */
        DestinationColor(false),
        /**
         * The source value of the blend function is 1 - the destination color.
         */
        OneMinusDestinationColor(false),
        /**
         * The source value of the blend function is the source alpha value.
         */
        SourceAlpha(false),
        /**
         * The source value of the blend function is 1 - the source alpha value.
         */
        OneMinusSourceAlpha(false),
        /**
         * The source value of the blend function is the destination alpha.
         */
        DestinationAlpha(false),
        /**
         * The source value of the blend function is 1 - the destination alpha.
         */
        OneMinusDestinationAlpha(false),
        /**
         * The source value of the blend function is the minimum of alpha or 1 - alpha.
         */
        SourceAlphaSaturate(false),
        /**
         * The source value of the blend function is the value of the constant color. (Rc, Gc, Bc, Ac) If not set, black
         * with alpha = 0 is used. If not supported, falls back to One.
         */
        ConstantColor(true),
        /**
         * The source value of the blend function is 1 minus the value of the constant color. (1-Rc, 1-Gc, 1-Bc, 1-Ac)
         * If color is not set, black with alpha = 0 is used. If not supported, falls back to One.
         */
        OneMinusConstantColor(true),
        /**
         * The source value of the blend function is the value of the constant color's alpha. (Ac, Ac, Ac, Ac) If not
         * set, black with alpha = 0 is used. If not supported, falls back to One.
         */
        ConstantAlpha(true),
        /**
         * The source value of the blend function is 1 minus the value of the constant color's alpha. (1-Ac, 1-Ac, 1-Ac,
         * 1-Ac) If color is not set, black with alpha = 0 is used. If not supported, falls back to One.
         */
        OneMinusConstantAlpha(true);

        private boolean usesConstantColor;

        private SourceFunction(final boolean usesConstantColor) {
            this.usesConstantColor = usesConstantColor;
        }

        public boolean usesConstantColor() {
            return usesConstantColor;
        }
    }

    public enum DestinationFunction {
        /**
         * The destination value of the blend function is all zeros.
         */
        Zero(false),
        /**
         * The destination value of the blend function is all ones.
         */
        One(false),
        /**
         * The destination value of the blend function is the source color.
         */
        SourceColor(false),
        /**
         * The destination value of the blend function is 1 - the source color.
         */
        OneMinusSourceColor(false),
        /**
         * The destination value of the blend function is the source alpha value.
         */
        SourceAlpha(false),
        /**
         * The destination value of the blend function is 1 - the source alpha value.
         */
        OneMinusSourceAlpha(false),
        /**
         * The destination value of the blend function is the destination alpha value.
         */
        DestinationAlpha(false),
        /**
         * The destination value of the blend function is 1 - the destination alpha value.
         */
        OneMinusDestinationAlpha(false),
        /**
         * The destination value of the blend function is the value of the constant color. (Rc, Gc, Bc, Ac) If not set,
         * black with alpha = 0 is used. If not supported, falls back to One.
         */
        ConstantColor(true),
        /**
         * The destination value of the blend function is 1 minus the value of the constant color. (1-Rc, 1-Gc, 1-Bc,
         * 1-Ac) If color is not set, black with alpha = 0 is used. If not supported, falls back to One.
         */
        OneMinusConstantColor(true),
        /**
         * The destination value of the blend function is the value of the constant color's alpha. (Ac, Ac, Ac, Ac) If
         * not set, black with alpha = 0 is used. If not supported, falls back to One.
         */
        ConstantAlpha(true),
        /**
         * The destination value of the blend function is 1 minus the value of the constant color's alpha. (1-Ac, 1-Ac,
         * 1-Ac, 1-Ac) If color is not set, black with alpha = 0 is used. If not supported, falls back to One.
         */
        OneMinusConstantAlpha(true);

        private boolean usesConstantColor;

        private DestinationFunction(final boolean usesConstantColor) {
            this.usesConstantColor = usesConstantColor;
        }

        public boolean usesConstantColor() {
            return usesConstantColor;
        }
    }

    public enum TestFunction {
        /**
         * Never passes the depth test.
         */
        Never,
        /**
         * Always passes the depth test.
         */
        Always,
        /**
         * Pass the test if this alpha is equal to the reference alpha.
         */
        EqualTo,
        /**
         * Pass the test if this alpha is not equal to the reference alpha.
         */
        NotEqualTo,
        /**
         * Pass the test if this alpha is less than the reference alpha.
         */
        LessThan,
        /**
         * Pass the test if this alpha is less than or equal to the reference alpha.
         */
        LessThanOrEqualTo,
        /**
         * Pass the test if this alpha is greater than the reference alpha.
         */
        GreaterThan,
        /**
         * Pass the test if this alpha is greater than or equal to the reference alpha.
         */
        GreaterThanOrEqualTo;

    }

    public enum BlendEquation {
        /**
         * Sets the blend equation so that the source and destination data are added. (Default) Clamps to [0,1] Useful
         * for things like antialiasing and transparency.
         */
        Add,
        /**
         * Sets the blend equation so that the source and destination data are subtracted (Src - Dest). Clamps to [0,1]
         * Falls back to Add if supportsSubtract is false.
         */
        Subtract,
        /**
         * Same as Subtract, but the order is reversed (Dst - Src). Clamps to [0,1] Falls back to Add if
         * supportsSubtract is false.
         */
        ReverseSubtract,
        /**
         * sets the blend equation so that each component of the result color is the minimum of the corresponding
         * components of the source and destination colors. This and Max are useful for applications that analyze image
         * data (image thresholding against a constant color, for example). Falls back to Add if supportsMinMax is
         * false.
         */
        Min,
        /**
         * sets the blend equation so that each component of the result color is the maximum of the corresponding
         * components of the source and destination colors. This and Min are useful for applications that analyze image
         * data (image thresholding against a constant color, for example). Falls back to Add if supportsMinMax is
         * false.
         */
        Max;
    }

    // attributes
    /** The current value of if blend is enabled. */
    private boolean _blendEnabled = false;

    /** The blend color used in constant blend operations. */
    private ColorRGBA _constantColor = new ColorRGBA(0, 0, 0, 0);

    /** The current source blend function. */
    private SourceFunction _sourceFunctionRGB = SourceFunction.SourceAlpha;
    /** The current destination blend function. */
    private DestinationFunction _destinationFunctionRGB = DestinationFunction.OneMinusSourceAlpha;
    /** The current blend equation. */
    private BlendEquation _blendEquationRGB = BlendEquation.Add;

    /** The current source blend function. */
    private SourceFunction _sourceFunctionAlpha = SourceFunction.SourceAlpha;
    /** The current destination blend function. */
    private DestinationFunction _destinationFunctionAlpha = DestinationFunction.OneMinusSourceAlpha;
    /** The current blend equation. */
    private BlendEquation _blendEquationAlpha = BlendEquation.Add;

    /** If enabled, alpha testing done. */
    private boolean _testEnabled = false;
    /** Alpha test value. */
    private TestFunction _testFunction = TestFunction.GreaterThan;
    /** The reference value to which incoming alpha values are compared. */
    private float _reference = 0;

    /** Enables conversion of alpha values to masks - a form of dithering. */
    private boolean _sampleAlphaToCoverageEnabled = false;
    /** Replaces alpha sample with max value. */
    private boolean _sampleAlphaToOneEnabled = false;
    /** Enables fragment mask modification. */
    private boolean _sampleCoverageEnabled = false;
    /** a mask that modifies the coverage of multi-sampled pixel fragments. Must be [0, 1] */
    private float _sampleCoverage = 1.0f;
    /** If enabled, sample coverage mask is inverted. */
    private boolean _sampleCoverageInverted = false;

    /**
     * Constructor instantiates a new <code>BlendState</code> object with default values.
     */
    public BlendState() {}

    @Override
    public StateType getType() {
        return StateType.Blend;
    }

    /**
     * <code>isBlendEnabled</code> returns true if blending is turned on, otherwise false is returned.
     *
     * @return true if blending is enabled, false otherwise.
     */
    public boolean isBlendEnabled() {
        return _blendEnabled;
    }

    /**
     * <code>setBlendEnabled</code> sets whether or not blending is enabled.
     *
     * @param value
     *            true to enable the blending, false to disable it.
     */
    public void setBlendEnabled(final boolean value) {
        _blendEnabled = value;
        setNeedsRefresh(true);
    }

    /**
     * <code>setSrcFunction</code> sets the source function for the blending equation for both rgb and alpha values.
     *
     * @param function
     *            the source function for the blending equation.
     * @throws IllegalArgumentException
     *             if function is null
     */
    public void setSourceFunction(final SourceFunction function) {
        setSourceFunctionRGB(function);
        setSourceFunctionAlpha(function);
    }

    /**
     * <code>setSrcFunction</code> sets the source function for the blending equation. If supportsSeparateFunc is false,
     * this value will be used for RGB and Alpha.
     *
     * @param function
     *            the source function for the blending equation.
     * @throws IllegalArgumentException
     *             if function is null
     */
    public void setSourceFunctionRGB(final SourceFunction function) {
        if (function == null) {
            throw new IllegalArgumentException("function can not be null.");
        }
        _sourceFunctionRGB = function;
        setNeedsRefresh(true);
    }

    /**
     * <code>setSourceFunctionAlpha</code> sets the source function for the blending equation used with alpha values.
     *
     * @param function
     *            the source function for the blending equation for alpha values.
     * @throws IllegalArgumentException
     *             if function is null
     */
    public void setSourceFunctionAlpha(final SourceFunction function) {
        if (function == null) {
            throw new IllegalArgumentException("function can not be null.");
        }
        _sourceFunctionAlpha = function;
        setNeedsRefresh(true);
    }

    /**
     * <code>getSourceFunction</code> returns the source function for the blending function.
     *
     * @return the source function for the blending function.
     */
    public SourceFunction getSourceFunctionRGB() {
        return _sourceFunctionRGB;
    }

    /**
     * <code>getSourceFunction</code> returns the source function for the blending function.
     *
     * @return the source function for the blending function.
     */
    public SourceFunction getSourceFunctionAlpha() {
        return _sourceFunctionAlpha;
    }

    /**
     * <code>setDestinationFunction</code> sets the destination function for the blending equation for both Alpha and
     * RGB values.
     *
     * @param function
     *            the destination function for the blending equation.
     * @throws IllegalArgumentException
     *             if function is null
     */
    public void setDestinationFunction(final DestinationFunction function) {
        setDestinationFunctionRGB(function);
        setDestinationFunctionAlpha(function);
    }

    /**
     * <code>setDestinationFunctionRGB</code> sets the destination function for the blending equation. If
     * supportsSeparateFunc is false, this value will be used for RGB and Alpha.
     *
     * @param function
     *            the destination function for the blending equation for RGB values.
     * @throws IllegalArgumentException
     *             if function is null
     */
    public void setDestinationFunctionRGB(final DestinationFunction function) {
        if (function == null) {
            throw new IllegalArgumentException("function can not be null.");
        }
        _destinationFunctionRGB = function;
        setNeedsRefresh(true);
    }

    /**
     * <code>setDestinationFunctionAlpha</code> sets the destination function for the blending equation.
     *
     * @param function
     *            the destination function for the blending equation for Alpha values.
     * @throws IllegalArgumentException
     *             if function is null
     */
    public void setDestinationFunctionAlpha(final DestinationFunction function) {
        if (function == null) {
            throw new IllegalArgumentException("function can not be null.");
        }
        _destinationFunctionAlpha = function;
        setNeedsRefresh(true);
    }

    /**
     * <code>getDestinationFunction</code> returns the destination function for the blending function.
     *
     * @return the destination function for the blending function.
     */
    public DestinationFunction getDestinationFunctionRGB() {
        return _destinationFunctionRGB;
    }

    /**
     * <code>getDestinationFunction</code> returns the destination function for the blending function.
     *
     * @return the destination function for the blending function.
     */
    public DestinationFunction getDestinationFunctionAlpha() {
        return _destinationFunctionAlpha;
    }

    public void setBlendEquation(final BlendEquation blendEquation) {
        setBlendEquationRGB(blendEquation);
        setBlendEquationAlpha(blendEquation);
    }

    public void setBlendEquationRGB(final BlendEquation blendEquation) {
        if (blendEquation == null) {
            throw new IllegalArgumentException("blendEquation can not be null.");
        }
        _blendEquationRGB = blendEquation;
    }

    public void setBlendEquationAlpha(final BlendEquation blendEquation) {
        if (blendEquation == null) {
            throw new IllegalArgumentException("blendEquation can not be null.");
        }
        _blendEquationAlpha = blendEquation;
    }

    public BlendEquation getBlendEquationRGB() {
        return _blendEquationRGB;
    }

    public BlendEquation getBlendEquationAlpha() {
        return _blendEquationAlpha;
    }

    /**
     * <code>isTestEnabled</code> returns true if alpha testing is enabled, false otherwise.
     *
     * @return true if alpha testing is enabled, false otherwise.
     */
    public boolean isTestEnabled() {
        return _testEnabled;
    }

    /**
     * <code>setTestEnabled</code> turns alpha testing on and off. True turns on the testing, while false disables it.
     *
     * @param value
     *            true to enabled alpha testing, false to disable it.
     */
    public void setTestEnabled(final boolean value) {
        _testEnabled = value;
        setNeedsRefresh(true);
    }

    /**
     * <code>setTestFunction</code> sets the testing function used for the alpha testing. If an invalid value is passed,
     * the default TF_ALWAYS is used.
     *
     * @param function
     *            the testing function used for the alpha testing.
     * @throws IllegalArgumentException
     *             if function is null
     */
    public void setTestFunction(final TestFunction function) {
        if (function == null) {
            throw new IllegalArgumentException("function can not be null.");
        }
        _testFunction = function;
        setNeedsRefresh(true);
    }

    /**
     * <code>getTestFunction</code> returns the testing function used for the alpha testing.
     *
     * @return the testing function used for the alpha testing.
     */
    public TestFunction getTestFunction() {
        return _testFunction;
    }

    /**
     * <code>setReference</code> sets the reference value that incoming alpha values are compared to when doing alpha
     * testing. This is clamped to [0, 1].
     *
     * @param reference
     *            the reference value that alpha values are compared to.
     */
    public void setReference(float reference) {
        if (reference < 0) {
            reference = 0;
        }

        if (reference > 1) {
            reference = 1;
        }
        _reference = reference;
        setNeedsRefresh(true);
    }

    /**
     * <code>getReference</code> returns the reference value that incoming alpha values are compared to.
     *
     * @return the reference value that alpha values are compared to.
     */
    public float getReference() {
        return _reference;
    }

    /**
     * @return the color used in constant blending functions. (0,0,0,0) is the default.
     */
    public ReadOnlyColorRGBA getConstantColor() {
        return _constantColor;
    }

    public void setConstantColor(final ReadOnlyColorRGBA constantColor) {
        _constantColor.set(constantColor);
    }

    public boolean isSampleAlphaToCoverageEnabled() {
        return _sampleAlphaToCoverageEnabled;
    }

    public void setSampleAlphaToCoverageEnabled(final boolean sampleAlphaToCoverageEnabled) {
        _sampleAlphaToCoverageEnabled = sampleAlphaToCoverageEnabled;
    }

    public boolean isSampleAlphaToOneEnabled() {
        return _sampleAlphaToOneEnabled;
    }

    public void setSampleAlphaToOneEnabled(final boolean sampleAlphaToOneEnabled) {
        _sampleAlphaToOneEnabled = sampleAlphaToOneEnabled;
    }

    public boolean isSampleCoverageEnabled() {
        return _sampleCoverageEnabled;
    }

    public void setSampleCoverageEnabled(final boolean sampleCoverageEnabled) {
        _sampleCoverageEnabled = sampleCoverageEnabled;
    }

    public float getSampleCoverage() {
        return _sampleCoverage;
    }

    /**
     * @param value
     *            new sample coverage value - must be in range [0f, 1f]
     * @throws IllegalArgumentException
     *             if value is not in correct range.
     */
    public void setSampleCoverage(final float value) {
        if (value > 1.0f || value < 0.0f) {
            throw new IllegalArgumentException("value must be in range [0f, 1f]");
        }
        _sampleCoverage = value;
    }

    public boolean isSampleCoverageInverted() {
        return _sampleCoverageInverted;
    }

    public void setSampleCoverageInverted(final boolean sampleCoverageInverted) {
        _sampleCoverageInverted = sampleCoverageInverted;
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        super.write(capsule);
        capsule.write(_blendEnabled, "blendEnabled", false);
        capsule.write(_sourceFunctionRGB, "sourceFunctionRGB", SourceFunction.SourceAlpha);
        capsule.write(_destinationFunctionRGB, "destinationFunctionRGB", DestinationFunction.OneMinusSourceAlpha);
        capsule.write(_blendEquationRGB, "blendEquationRGB", BlendEquation.Add);
        capsule.write(_sourceFunctionAlpha, "sourceFunctionAlpha", SourceFunction.SourceAlpha);
        capsule.write(_destinationFunctionAlpha, "destinationFunctionAlpha", DestinationFunction.OneMinusSourceAlpha);
        capsule.write(_blendEquationAlpha, "blendEquationAlpha", BlendEquation.Add);
        capsule.write(_testEnabled, "testEnabled", false);
        capsule.write(_testFunction, "test", TestFunction.GreaterThan);
        capsule.write(_reference, "reference", 0);
        capsule.write(_constantColor, "constantColor", null);
        capsule.write(_sampleAlphaToCoverageEnabled, "sampleAlphaToCoverageEnabled", false);
        capsule.write(_sampleAlphaToOneEnabled, "sampleAlphaToOneEnabled", false);
        capsule.write(_sampleCoverageEnabled, "sampleCoverageEnabled", false);
        capsule.write(_sampleCoverageInverted, "sampleCoverageInverted", false);
        capsule.write(_sampleCoverage, "sampleCoverage", 1.0f);
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        super.read(capsule);
        _blendEnabled = capsule.readBoolean("blendEnabled", false);
        _sourceFunctionRGB = capsule.readEnum("sourceFunctionRGB", SourceFunction.class, SourceFunction.SourceAlpha);
        _destinationFunctionRGB = capsule.readEnum("destinationFunctionRGB", DestinationFunction.class,
                DestinationFunction.OneMinusSourceAlpha);
        _blendEquationRGB = capsule.readEnum("blendEquationRGB", BlendEquation.class, BlendEquation.Add);
        _sourceFunctionAlpha = capsule.readEnum("sourceFunctionAlpha", SourceFunction.class,
                SourceFunction.SourceAlpha);
        _destinationFunctionAlpha = capsule.readEnum("destinationFunctionAlpha", DestinationFunction.class,
                DestinationFunction.OneMinusSourceAlpha);
        _blendEquationAlpha = capsule.readEnum("blendEquationAlpha", BlendEquation.class, BlendEquation.Add);
        _testEnabled = capsule.readBoolean("testEnabled", false);
        _testFunction = capsule.readEnum("test", TestFunction.class, TestFunction.GreaterThan);
        _reference = capsule.readFloat("reference", 0);
        _constantColor = capsule.readSavable("constantColor", null);
    }

    @Override
    public StateRecord createStateRecord(final ContextCapabilities caps) {
        return new BlendStateRecord();
    }

}
