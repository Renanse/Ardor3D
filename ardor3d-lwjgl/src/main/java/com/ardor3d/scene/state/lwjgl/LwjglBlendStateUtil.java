/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scene.state.lwjgl;

import org.lwjgl.opengl.ARBImaging;
import org.lwjgl.opengl.ARBMultisample;
import org.lwjgl.opengl.EXTBlendColor;
import org.lwjgl.opengl.EXTBlendEquationSeparate;
import org.lwjgl.opengl.EXTBlendFuncSeparate;
import org.lwjgl.opengl.EXTBlendMinmax;
import org.lwjgl.opengl.EXTBlendSubtract;
import org.lwjgl.opengl.GL11;

import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.BlendState.BlendEquation;
import com.ardor3d.renderer.state.BlendState.DestinationFunction;
import com.ardor3d.renderer.state.BlendState.SourceFunction;
import com.ardor3d.renderer.state.BlendState.TestFunction;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.record.BlendStateRecord;

public abstract class LwjglBlendStateUtil {

    public static void apply(final BlendState state) {
        // ask for the current state record
        final RenderContext context = ContextManager.getCurrentContext();
        final BlendStateRecord record = (BlendStateRecord) context.getStateRecord(StateType.Blend);
        final ContextCapabilities caps = context.getCapabilities();
        context.setCurrentState(StateType.Blend, state);

        if (state.isEnabled()) {
            applyBlendEquations(state.isBlendEnabled(), state, record, caps);
            applyBlendColor(state.isBlendEnabled(), state, record, caps);
            applyBlendFunctions(state.isBlendEnabled(), state, record, caps);

            applyTest(state.isTestEnabled(), state, record);

            if (caps.isMultisampleSupported()) {
                applyAlphaCoverage(state.isSampleAlphaToCoverageEnabled(), state.isSampleAlphaToOneEnabled(), record,
                        caps);
                applySampleCoverage(state.isSampleCoverageEnabled(), state, record, caps);
            }
        } else {
            // disable blend
            applyBlendEquations(false, state, record, caps);

            // disable alpha test
            applyTest(false, state, record);

            // disable sample coverage
            if (caps.isMultisampleSupported()) {
                applyAlphaCoverage(false, false, record, caps);
                applySampleCoverage(false, state, record, caps);
            }
        }

        if (!record.isValid()) {
            record.validate();
        }
    }

    protected static void applyBlendEquations(final boolean enabled, final BlendState state,
            final BlendStateRecord record, final ContextCapabilities caps) {
        if (record.isValid()) {
            if (enabled) {
                if (!record.blendEnabled) {
                    GL11.glEnable(GL11.GL_BLEND);
                    record.blendEnabled = true;
                }
                final int blendEqRGB = getGLEquationValue(state.getBlendEquationRGB(), caps);
                if (caps.isSeparateBlendEquationsSupported()) {
                    final int blendEqAlpha = getGLEquationValue(state.getBlendEquationAlpha(), caps);
                    if (record.blendEqRGB != blendEqRGB || record.blendEqAlpha != blendEqAlpha) {
                        EXTBlendEquationSeparate.glBlendEquationSeparateEXT(blendEqRGB, blendEqAlpha);
                        record.blendEqRGB = blendEqRGB;
                        record.blendEqAlpha = blendEqAlpha;
                    }
                } else if (caps.isBlendEquationSupported()) {
                    if (record.blendEqRGB != blendEqRGB) {
                        ARBImaging.glBlendEquation(blendEqRGB);
                        record.blendEqRGB = blendEqRGB;
                    }
                }
            } else if (record.blendEnabled) {
                GL11.glDisable(GL11.GL_BLEND);
                record.blendEnabled = false;
            }

        } else {
            if (enabled) {
                GL11.glEnable(GL11.GL_BLEND);
                record.blendEnabled = true;
                final int blendEqRGB = getGLEquationValue(state.getBlendEquationRGB(), caps);
                if (caps.isSeparateBlendEquationsSupported()) {
                    final int blendEqAlpha = getGLEquationValue(state.getBlendEquationAlpha(), caps);
                    EXTBlendEquationSeparate.glBlendEquationSeparateEXT(blendEqRGB, blendEqAlpha);
                    record.blendEqRGB = blendEqRGB;
                    record.blendEqAlpha = blendEqAlpha;
                } else if (caps.isBlendEquationSupported()) {
                    ARBImaging.glBlendEquation(blendEqRGB);
                    record.blendEqRGB = blendEqRGB;
                }
            } else {
                GL11.glDisable(GL11.GL_BLEND);
                record.blendEnabled = false;
            }
        }
    }

    protected static void applyBlendColor(final boolean enabled, final BlendState state, final BlendStateRecord record,
            final ContextCapabilities caps) {
        if (enabled) {
            final boolean applyConstant = state.getDestinationFunctionRGB().usesConstantColor()
                    || state.getSourceFunctionRGB().usesConstantColor()
                    || (caps.isConstantBlendColorSupported() && (state.getDestinationFunctionAlpha()
                            .usesConstantColor() || state.getSourceFunctionAlpha().usesConstantColor()));
            if (applyConstant && caps.isConstantBlendColorSupported()) {
                final ReadOnlyColorRGBA constant = state.getConstantColor();
                if (!record.isValid() || (caps.isConstantBlendColorSupported() && !record.blendColor.equals(constant))) {
                    ARBImaging.glBlendColor(constant.getRed(), constant.getGreen(), constant.getBlue(),
                            constant.getAlpha());
                    record.blendColor.set(constant);
                }
            }
        }
    }

    protected static void applyBlendFunctions(final boolean enabled, final BlendState state,
            final BlendStateRecord record, final ContextCapabilities caps) {
        if (record.isValid()) {
            if (enabled) {
                final int glSrcRGB = getGLSrcValue(state.getSourceFunctionRGB(), caps);
                final int glDstRGB = getGLDstValue(state.getDestinationFunctionRGB(), caps);
                if (caps.isSeparateBlendFunctionsSupported()) {
                    final int glSrcAlpha = getGLSrcValue(state.getSourceFunctionAlpha(), caps);
                    final int glDstAlpha = getGLDstValue(state.getDestinationFunctionAlpha(), caps);
                    if (record.srcFactorRGB != glSrcRGB || record.dstFactorRGB != glDstRGB
                            || record.srcFactorAlpha != glSrcAlpha || record.dstFactorAlpha != glDstAlpha) {
                        EXTBlendFuncSeparate.glBlendFuncSeparateEXT(glSrcRGB, glDstRGB, glSrcAlpha, glDstAlpha);
                        record.srcFactorRGB = glSrcRGB;
                        record.dstFactorRGB = glDstRGB;
                        record.srcFactorAlpha = glSrcAlpha;
                        record.dstFactorAlpha = glDstAlpha;
                    }
                } else if (record.srcFactorRGB != glSrcRGB || record.dstFactorRGB != glDstRGB) {
                    GL11.glBlendFunc(glSrcRGB, glDstRGB);
                    record.srcFactorRGB = glSrcRGB;
                    record.dstFactorRGB = glDstRGB;
                }
            }
        } else {
            if (enabled) {
                final int glSrcRGB = getGLSrcValue(state.getSourceFunctionRGB(), caps);
                final int glDstRGB = getGLDstValue(state.getDestinationFunctionRGB(), caps);
                if (caps.isSeparateBlendFunctionsSupported()) {
                    final int glSrcAlpha = getGLSrcValue(state.getSourceFunctionAlpha(), caps);
                    final int glDstAlpha = getGLDstValue(state.getDestinationFunctionAlpha(), caps);
                    EXTBlendFuncSeparate.glBlendFuncSeparateEXT(glSrcRGB, glDstRGB, glSrcAlpha, glDstAlpha);
                    record.srcFactorRGB = glSrcRGB;
                    record.dstFactorRGB = glDstRGB;
                    record.srcFactorAlpha = glSrcAlpha;
                    record.dstFactorAlpha = glDstAlpha;
                } else {
                    GL11.glBlendFunc(glSrcRGB, glDstRGB);
                    record.srcFactorRGB = glSrcRGB;
                    record.dstFactorRGB = glDstRGB;
                }
            }
        }
    }

    protected static void applyAlphaCoverage(final boolean sampleAlphaToCoverageEnabled,
            final boolean sampleAlphaToOneEnabled, final BlendStateRecord record, final ContextCapabilities caps) {
        if (record.isValid()) {
            if (sampleAlphaToCoverageEnabled != record.sampleAlphaToCoverageEnabled) {
                if (sampleAlphaToCoverageEnabled) {
                    GL11.glEnable(ARBMultisample.GL_SAMPLE_ALPHA_TO_COVERAGE_ARB);
                } else {
                    GL11.glDisable(ARBMultisample.GL_SAMPLE_ALPHA_TO_COVERAGE_ARB);
                }
                record.sampleAlphaToCoverageEnabled = sampleAlphaToCoverageEnabled;
            }
            if (sampleAlphaToOneEnabled != record.sampleAlphaToOneEnabled) {
                if (sampleAlphaToOneEnabled) {
                    GL11.glEnable(ARBMultisample.GL_SAMPLE_ALPHA_TO_ONE_ARB);
                } else {
                    GL11.glDisable(ARBMultisample.GL_SAMPLE_ALPHA_TO_ONE_ARB);
                }
                record.sampleAlphaToOneEnabled = sampleAlphaToOneEnabled;
            }
        } else {
            if (sampleAlphaToCoverageEnabled) {
                GL11.glEnable(ARBMultisample.GL_SAMPLE_ALPHA_TO_COVERAGE_ARB);
            } else {
                GL11.glDisable(ARBMultisample.GL_SAMPLE_ALPHA_TO_COVERAGE_ARB);
            }
            record.sampleAlphaToCoverageEnabled = sampleAlphaToCoverageEnabled;
            if (sampleAlphaToOneEnabled) {
                GL11.glEnable(ARBMultisample.GL_SAMPLE_ALPHA_TO_ONE_ARB);
            } else {
                GL11.glDisable(ARBMultisample.GL_SAMPLE_ALPHA_TO_ONE_ARB);
            }
            record.sampleAlphaToOneEnabled = sampleAlphaToOneEnabled;
        }
    }

    protected static void applySampleCoverage(final boolean enabled, final BlendState state,
            final BlendStateRecord record, final ContextCapabilities caps) {

        final boolean coverageInverted = state.isSampleCoverageInverted();
        final float coverageValue = state.getSampleCoverage();

        if (record.isValid()) {
            if (enabled) {
                if (!record.sampleCoverageEnabled) {
                    GL11.glEnable(ARBMultisample.GL_SAMPLE_COVERAGE_ARB);
                    record.sampleCoverageEnabled = true;
                }
                if (record.sampleCoverageInverted != coverageInverted || record.sampleCoverage != coverageValue) {
                    ARBMultisample.glSampleCoverageARB(coverageValue, coverageInverted);
                    record.sampleCoverageInverted = coverageInverted;
                    record.sampleCoverage = coverageValue;
                }
            } else {
                if (record.sampleCoverageEnabled) {
                    GL11.glDisable(ARBMultisample.GL_SAMPLE_COVERAGE_ARB);
                    record.sampleCoverageEnabled = false;
                }
            }
        } else {
            if (enabled) {
                GL11.glEnable(ARBMultisample.GL_SAMPLE_COVERAGE_ARB);
                record.sampleCoverageEnabled = true;
                ARBMultisample.glSampleCoverageARB(coverageValue, coverageInverted);
                record.sampleCoverageInverted = coverageInverted;
                record.sampleCoverage = coverageValue;
            } else {
                GL11.glDisable(ARBMultisample.GL_SAMPLE_COVERAGE_ARB);
                record.sampleCoverageEnabled = false;
            }
        }
    }

    protected static int getGLSrcValue(final SourceFunction function, final ContextCapabilities caps) {
        switch (function) {
            case Zero:
                return GL11.GL_ZERO;
            case DestinationColor:
                return GL11.GL_DST_COLOR;
            case OneMinusDestinationColor:
                return GL11.GL_ONE_MINUS_DST_COLOR;
            case SourceAlpha:
                return GL11.GL_SRC_ALPHA;
            case OneMinusSourceAlpha:
                return GL11.GL_ONE_MINUS_SRC_ALPHA;
            case DestinationAlpha:
                return GL11.GL_DST_ALPHA;
            case OneMinusDestinationAlpha:
                return GL11.GL_ONE_MINUS_DST_ALPHA;
            case SourceAlphaSaturate:
                return GL11.GL_SRC_ALPHA_SATURATE;
            case ConstantColor:
                if (caps.isConstantBlendColorSupported()) {
                    return EXTBlendColor.GL_CONSTANT_COLOR_EXT;
                }
                // FALLS THROUGH
            case OneMinusConstantColor:
                if (caps.isConstantBlendColorSupported()) {
                    return EXTBlendColor.GL_ONE_MINUS_CONSTANT_COLOR_EXT;
                }
                // FALLS THROUGH
            case ConstantAlpha:
                if (caps.isConstantBlendColorSupported()) {
                    return EXTBlendColor.GL_CONSTANT_ALPHA_EXT;
                }
                // FALLS THROUGH
            case OneMinusConstantAlpha:
                if (caps.isConstantBlendColorSupported()) {
                    return EXTBlendColor.GL_ONE_MINUS_CONSTANT_ALPHA_EXT;
                }
                // FALLS THROUGH
            case One:
                return GL11.GL_ONE;
        }
        throw new IllegalArgumentException("Invalid source function type: " + function);
    }

    protected static int getGLDstValue(final DestinationFunction function, final ContextCapabilities caps) {
        switch (function) {
            case Zero:
                return GL11.GL_ZERO;
            case SourceColor:
                return GL11.GL_SRC_COLOR;
            case OneMinusSourceColor:
                return GL11.GL_ONE_MINUS_SRC_COLOR;
            case SourceAlpha:
                return GL11.GL_SRC_ALPHA;
            case OneMinusSourceAlpha:
                return GL11.GL_ONE_MINUS_SRC_ALPHA;
            case DestinationAlpha:
                return GL11.GL_DST_ALPHA;
            case OneMinusDestinationAlpha:
                return GL11.GL_ONE_MINUS_DST_ALPHA;
            case ConstantColor:
                if (caps.isConstantBlendColorSupported()) {
                    return EXTBlendColor.GL_CONSTANT_COLOR_EXT;
                }
                // FALLS THROUGH
            case OneMinusConstantColor:
                if (caps.isConstantBlendColorSupported()) {
                    return EXTBlendColor.GL_ONE_MINUS_CONSTANT_COLOR_EXT;
                }
                // FALLS THROUGH
            case ConstantAlpha:
                if (caps.isConstantBlendColorSupported()) {
                    return EXTBlendColor.GL_CONSTANT_ALPHA_EXT;
                }
                // FALLS THROUGH
            case OneMinusConstantAlpha:
                if (caps.isConstantBlendColorSupported()) {
                    return EXTBlendColor.GL_ONE_MINUS_CONSTANT_ALPHA_EXT;
                }
                // FALLS THROUGH
            case One:
                return GL11.GL_ONE;
        }
        throw new IllegalArgumentException("Invalid destination function type: " + function);
    }

    protected static int getGLEquationValue(final BlendEquation eq, final ContextCapabilities caps) {
        switch (eq) {
            case Min:
                if (caps.isMinMaxBlendEquationsSupported()) {
                    return EXTBlendMinmax.GL_MIN_EXT;
                }
                // FALLS THROUGH
            case Max:
                if (caps.isMinMaxBlendEquationsSupported()) {
                    return EXTBlendMinmax.GL_MAX_EXT;
                } else {
                    return ARBImaging.GL_FUNC_ADD;
                }
            case Subtract:
                if (caps.isSubtractBlendEquationsSupported()) {
                    return EXTBlendSubtract.GL_FUNC_SUBTRACT_EXT;
                }
                // FALLS THROUGH
            case ReverseSubtract:
                if (caps.isSubtractBlendEquationsSupported()) {
                    return EXTBlendSubtract.GL_FUNC_REVERSE_SUBTRACT_EXT;
                }
                // FALLS THROUGH
            case Add:
                return ARBImaging.GL_FUNC_ADD;
        }
        throw new IllegalArgumentException("Invalid blend equation: " + eq);
    }

    protected static void applyTest(final boolean enabled, final BlendState state, final BlendStateRecord record) {
        if (record.isValid()) {
            if (enabled) {
                if (!record.testEnabled) {
                    GL11.glEnable(GL11.GL_ALPHA_TEST);
                    record.testEnabled = true;
                }
                final int glFunc = getGLFuncValue(state.getTestFunction());
                if (record.alphaFunc != glFunc || record.alphaRef != state.getReference()) {
                    GL11.glAlphaFunc(glFunc, state.getReference());
                    record.alphaFunc = glFunc;
                    record.alphaRef = state.getReference();
                }
            } else if (record.testEnabled) {
                GL11.glDisable(GL11.GL_ALPHA_TEST);
                record.testEnabled = false;
            }

        } else {
            if (enabled) {
                GL11.glEnable(GL11.GL_ALPHA_TEST);
                record.testEnabled = true;
                final int glFunc = getGLFuncValue(state.getTestFunction());
                GL11.glAlphaFunc(glFunc, state.getReference());
                record.alphaFunc = glFunc;
                record.alphaRef = state.getReference();
            } else {
                GL11.glDisable(GL11.GL_ALPHA_TEST);
                record.testEnabled = false;
            }
        }
    }

    protected static int getGLFuncValue(final TestFunction function) {
        switch (function) {
            case Never:
                return GL11.GL_NEVER;
            case LessThan:
                return GL11.GL_LESS;
            case EqualTo:
                return GL11.GL_EQUAL;
            case LessThanOrEqualTo:
                return GL11.GL_LEQUAL;
            case GreaterThan:
                return GL11.GL_GREATER;
            case NotEqualTo:
                return GL11.GL_NOTEQUAL;
            case GreaterThanOrEqualTo:
                return GL11.GL_GEQUAL;
            case Always:
                return GL11.GL_ALWAYS;
        }
        throw new IllegalArgumentException("Invalid test function type: " + function);
    }
}
