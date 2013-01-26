/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scene.state.jogl;

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES1;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GLContext;

import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.jogl.JoglRenderer;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.BlendState.BlendEquation;
import com.ardor3d.renderer.state.BlendState.DestinationFunction;
import com.ardor3d.renderer.state.BlendState.SourceFunction;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.record.BlendStateRecord;

public abstract class JoglBlendStateUtil {

    public static void apply(final JoglRenderer renderer, final BlendState state) {
        // ask for the current state record
        final RenderContext context = ContextManager.getCurrentContext();
        final BlendStateRecord record = (BlendStateRecord) context.getStateRecord(StateType.Blend);
        final ContextCapabilities caps = context.getCapabilities();
        context.setCurrentState(StateType.Blend, state);

        final GL gl = GLContext.getCurrentGL();

        if (state.isEnabled()) {
            applyBlendEquations(gl, state.isBlendEnabled(), state, record, caps);
            applyBlendColor(gl, state.isBlendEnabled(), state, record, caps);
            applyBlendFunctions(gl, state.isBlendEnabled(), state, record, caps);

            applyTest(gl, state.isTestEnabled(), state, record);

            if (caps.isMultisampleSupported()) {
                applyAlphaCoverage(gl, state.isSampleAlphaToCoverageEnabled(), state.isSampleAlphaToOneEnabled(),
                        record, caps);
                applySampleCoverage(gl, state.isSampleCoverageEnabled(), state, record, caps);
            }
        } else {
            // disable blend
            applyBlendEquations(gl, false, state, record, caps);

            // disable alpha test
            applyTest(gl, false, state, record);

            // disable sample coverage
            if (caps.isMultisampleSupported()) {
                applyAlphaCoverage(gl, false, false, record, caps);
                applySampleCoverage(gl, false, state, record, caps);
            }
        }

        if (!record.isValid()) {
            record.validate();
        }
    }

    private static void applyBlendEquations(final GL gl, final boolean enabled, final BlendState state,
            final BlendStateRecord record, final ContextCapabilities caps) {
        if (record.isValid()) {
            if (enabled) {
                if (!record.blendEnabled) {
                    gl.glEnable(GL.GL_BLEND);
                    record.blendEnabled = true;
                }
                final int blendEqRGB = getGLEquationValue(state.getBlendEquationRGB(), caps);
                if (caps.isSeparateBlendEquationsSupported()) {
                    final int blendEqAlpha = getGLEquationValue(state.getBlendEquationAlpha(), caps);
                    if (record.blendEqRGB != blendEqRGB || record.blendEqAlpha != blendEqAlpha) {
                        gl.glBlendEquationSeparate(blendEqRGB, blendEqAlpha);
                        record.blendEqRGB = blendEqRGB;
                        record.blendEqAlpha = blendEqAlpha;
                    }
                } else if (caps.isBlendEquationSupported()) {
                    if (record.blendEqRGB != blendEqRGB) {
                        gl.glBlendEquation(blendEqRGB);
                        record.blendEqRGB = blendEqRGB;
                    }
                }
            } else if (record.blendEnabled) {
                gl.glDisable(GL.GL_BLEND);
                record.blendEnabled = false;
            }

        } else {
            if (enabled) {
                gl.glEnable(GL.GL_BLEND);
                record.blendEnabled = true;
                final int blendEqRGB = getGLEquationValue(state.getBlendEquationRGB(), caps);
                if (caps.isSeparateBlendEquationsSupported()) {
                    final int blendEqAlpha = getGLEquationValue(state.getBlendEquationAlpha(), caps);
                    gl.glBlendEquationSeparate(blendEqRGB, blendEqAlpha);
                    record.blendEqRGB = blendEqRGB;
                    record.blendEqAlpha = blendEqAlpha;
                } else if (caps.isBlendEquationSupported()) {
                    gl.glBlendEquation(blendEqRGB);
                    record.blendEqRGB = blendEqRGB;
                }
            } else {
                gl.glDisable(GL.GL_BLEND);
                record.blendEnabled = false;
            }
        }
    }

    private static void applyBlendColor(final GL gl, final boolean enabled, final BlendState state,
            final BlendStateRecord record, final ContextCapabilities caps) {
        if (enabled) {
            final boolean applyConstant = state.getDestinationFunctionRGB().usesConstantColor()
                    || state.getSourceFunctionRGB().usesConstantColor()
                    || (caps.isConstantBlendColorSupported() && (state.getDestinationFunctionAlpha()
                            .usesConstantColor() || state.getSourceFunctionAlpha().usesConstantColor()));
            if (applyConstant && caps.isConstantBlendColorSupported()) {
                final ReadOnlyColorRGBA constant = state.getConstantColor();
                if (!record.isValid() || (caps.isConstantBlendColorSupported() && !record.blendColor.equals(constant))) {
                    gl.getGL2GL3().glBlendColor(constant.getRed(), constant.getGreen(), constant.getBlue(),
                            constant.getAlpha());
                    record.blendColor.set(constant);
                }
            }
        }
    }

    private static void applyBlendFunctions(final GL gl, final boolean enabled, final BlendState state,
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
                        gl.glBlendFuncSeparate(glSrcRGB, glDstRGB, glSrcAlpha, glDstAlpha);
                        record.srcFactorRGB = glSrcRGB;
                        record.dstFactorRGB = glDstRGB;
                        record.srcFactorAlpha = glSrcAlpha;
                        record.dstFactorAlpha = glDstAlpha;
                    }
                } else if (record.srcFactorRGB != glSrcRGB || record.dstFactorRGB != glDstRGB) {
                    gl.glBlendFunc(glSrcRGB, glDstRGB);
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
                    gl.glBlendFuncSeparate(glSrcRGB, glDstRGB, glSrcAlpha, glDstAlpha);
                    record.srcFactorRGB = glSrcRGB;
                    record.dstFactorRGB = glDstRGB;
                    record.srcFactorAlpha = glSrcAlpha;
                    record.dstFactorAlpha = glDstAlpha;
                } else {
                    gl.glBlendFunc(glSrcRGB, glDstRGB);
                    record.srcFactorRGB = glSrcRGB;
                    record.dstFactorRGB = glDstRGB;
                }
            }
        }
    }

    protected static void applyAlphaCoverage(final GL gl, final boolean sampleAlphaToCoverageEnabled,
            final boolean sampleAlphaToOneEnabled, final BlendStateRecord record, final ContextCapabilities caps) {
        if (record.isValid()) {
            if (sampleAlphaToCoverageEnabled != record.sampleAlphaToCoverageEnabled) {
                if (sampleAlphaToCoverageEnabled) {
                    gl.glEnable(GL.GL_SAMPLE_ALPHA_TO_COVERAGE);
                } else {
                    gl.glDisable(GL.GL_SAMPLE_ALPHA_TO_COVERAGE);
                }
                record.sampleAlphaToCoverageEnabled = sampleAlphaToCoverageEnabled;
            }
            if (sampleAlphaToOneEnabled != record.sampleAlphaToOneEnabled) {
                if (sampleAlphaToOneEnabled) {
                    gl.glEnable(GL.GL_SAMPLE_ALPHA_TO_ONE);
                } else {
                    gl.glDisable(GL.GL_SAMPLE_ALPHA_TO_ONE);
                }
                record.sampleAlphaToOneEnabled = sampleAlphaToOneEnabled;
            }
        } else {
            if (sampleAlphaToCoverageEnabled) {
                gl.glEnable(GL.GL_SAMPLE_ALPHA_TO_COVERAGE);
            } else {
                gl.glDisable(GL.GL_SAMPLE_ALPHA_TO_COVERAGE);
            }
            record.sampleAlphaToCoverageEnabled = sampleAlphaToCoverageEnabled;
            if (sampleAlphaToOneEnabled) {
                gl.glEnable(GL.GL_SAMPLE_ALPHA_TO_ONE);
            } else {
                gl.glDisable(GL.GL_SAMPLE_ALPHA_TO_ONE);
            }
            record.sampleAlphaToOneEnabled = sampleAlphaToOneEnabled;
        }
    }

    protected static void applySampleCoverage(final GL gl, final boolean enabled, final BlendState state,
            final BlendStateRecord record, final ContextCapabilities caps) {

        final boolean coverageInverted = state.isSampleCoverageInverted();
        final float coverageValue = state.getSampleCoverage();

        if (record.isValid()) {
            if (enabled) {
                if (!record.sampleCoverageEnabled) {
                    gl.glEnable(GL.GL_SAMPLE_COVERAGE);
                    record.sampleCoverageEnabled = true;
                }
                if (record.sampleCoverageInverted != coverageInverted || record.sampleCoverage != coverageValue) {
                    gl.glSampleCoverage(coverageValue, coverageInverted);
                    record.sampleCoverageInverted = coverageInverted;
                    record.sampleCoverage = coverageValue;
                }
            } else {
                if (record.sampleCoverageEnabled) {
                    gl.glDisable(GL.GL_SAMPLE_COVERAGE);
                    record.sampleCoverageEnabled = false;
                }
            }
        } else {
            if (enabled) {
                gl.glEnable(GL.GL_SAMPLE_COVERAGE);
                record.sampleCoverageEnabled = true;
                gl.glSampleCoverage(coverageValue, coverageInverted);
                record.sampleCoverageInverted = coverageInverted;
                record.sampleCoverage = coverageValue;
            } else {
                gl.glDisable(GL.GL_SAMPLE_COVERAGE);
                record.sampleCoverageEnabled = false;
            }
        }
    }

    private static int getGLSrcValue(final SourceFunction function, final ContextCapabilities caps) {
        switch (function) {
            case Zero:
                return GL.GL_ZERO;
            case DestinationColor:
                return GL.GL_DST_COLOR;
            case OneMinusDestinationColor:
                return GL.GL_ONE_MINUS_DST_COLOR;
            case SourceAlpha:
                return GL.GL_SRC_ALPHA;
            case OneMinusSourceAlpha:
                return GL.GL_ONE_MINUS_SRC_ALPHA;
            case DestinationAlpha:
                return GL.GL_DST_ALPHA;
            case OneMinusDestinationAlpha:
                return GL.GL_ONE_MINUS_DST_ALPHA;
            case SourceAlphaSaturate:
                return GL.GL_SRC_ALPHA_SATURATE;
            case ConstantColor:
                if (caps.isConstantBlendColorSupported()) {
                    return GL2ES2.GL_CONSTANT_COLOR;
                }
                // FALLS THROUGH
            case OneMinusConstantColor:
                if (caps.isConstantBlendColorSupported()) {
                    return GL2ES2.GL_ONE_MINUS_CONSTANT_COLOR;
                }
                // FALLS THROUGH
            case ConstantAlpha:
                if (caps.isConstantBlendColorSupported()) {
                    return GL2ES2.GL_CONSTANT_ALPHA;
                }
                // FALLS THROUGH
            case OneMinusConstantAlpha:
                if (caps.isConstantBlendColorSupported()) {
                    return GL2ES2.GL_ONE_MINUS_CONSTANT_ALPHA;
                }
                // FALLS THROUGH
            case One:
                return GL.GL_ONE;
        }
        throw new IllegalArgumentException("Invalid source function type: " + function);
    }

    private static int getGLDstValue(final DestinationFunction function, final ContextCapabilities caps) {
        switch (function) {
            case Zero:
                return GL.GL_ZERO;
            case SourceColor:
                return GL.GL_SRC_COLOR;
            case OneMinusSourceColor:
                return GL.GL_ONE_MINUS_SRC_COLOR;
            case SourceAlpha:
                return GL.GL_SRC_ALPHA;
            case OneMinusSourceAlpha:
                return GL.GL_ONE_MINUS_SRC_ALPHA;
            case DestinationAlpha:
                return GL.GL_DST_ALPHA;
            case OneMinusDestinationAlpha:
                return GL.GL_ONE_MINUS_DST_ALPHA;
            case ConstantColor:
                if (caps.isConstantBlendColorSupported()) {
                    return GL2ES2.GL_CONSTANT_COLOR;
                }
                // FALLS THROUGH
            case OneMinusConstantColor:
                if (caps.isConstantBlendColorSupported()) {
                    return GL2ES2.GL_ONE_MINUS_CONSTANT_COLOR;
                }
                // FALLS THROUGH
            case ConstantAlpha:
                if (caps.isConstantBlendColorSupported()) {
                    return GL2ES2.GL_CONSTANT_ALPHA;
                }
                // FALLS THROUGH
            case OneMinusConstantAlpha:
                if (caps.isConstantBlendColorSupported()) {
                    return GL2ES2.GL_ONE_MINUS_CONSTANT_ALPHA;
                }
                // FALLS THROUGH
            case One:
                return GL.GL_ONE;
        }
        throw new IllegalArgumentException("Invalid destination function type: " + function);
    }

    private static int getGLEquationValue(final BlendEquation eq, final ContextCapabilities caps) {
        switch (eq) {
            case Min:
                if (caps.isMinMaxBlendEquationsSupported()) {
                    return GL2GL3.GL_MIN;
                }
                // FALLS THROUGH
            case Max:
                if (caps.isMinMaxBlendEquationsSupported()) {
                    return GL2GL3.GL_MAX;
                } else {
                    return GL.GL_FUNC_ADD;
                }
            case Subtract:
                if (caps.isSubtractBlendEquationsSupported()) {
                    return GL.GL_FUNC_SUBTRACT;
                }
                // FALLS THROUGH
            case ReverseSubtract:
                if (caps.isSubtractBlendEquationsSupported()) {
                    return GL.GL_FUNC_REVERSE_SUBTRACT;
                }
                // FALLS THROUGH
            case Add:
                return GL.GL_FUNC_ADD;
        }
        throw new IllegalArgumentException("Invalid blend equation: " + eq);
    }

    private static void applyTest(final GL gl, final boolean enabled, final BlendState state,
            final BlendStateRecord record) {
        if (record.isValid()) {
            if (enabled) {
                if (!record.testEnabled) {
                    gl.glEnable(GL2ES1.GL_ALPHA_TEST);
                    record.testEnabled = true;
                }
                final int glFunc = getGLFuncValue(state.getTestFunction());
                if (record.alphaFunc != glFunc || record.alphaRef != state.getReference()) {
                    gl.getGL2().glAlphaFunc(glFunc, state.getReference());
                    record.alphaFunc = glFunc;
                    record.alphaRef = state.getReference();
                }
            } else if (record.testEnabled) {
                gl.glDisable(GL2ES1.GL_ALPHA_TEST);
                record.testEnabled = false;
            }

        } else {
            if (enabled) {
                gl.glEnable(GL2ES1.GL_ALPHA_TEST);
                record.testEnabled = true;
                final int glFunc = getGLFuncValue(state.getTestFunction());
                gl.getGL2().glAlphaFunc(glFunc, state.getReference());
                record.alphaFunc = glFunc;
                record.alphaRef = state.getReference();
            } else {
                gl.glDisable(GL2ES1.GL_ALPHA_TEST);
                record.testEnabled = false;
            }
        }
    }

    private static int getGLFuncValue(final BlendState.TestFunction function) {
        switch (function) {
            case Never:
                return GL.GL_NEVER;
            case LessThan:
                return GL.GL_LESS;
            case EqualTo:
                return GL.GL_EQUAL;
            case LessThanOrEqualTo:
                return GL.GL_LEQUAL;
            case GreaterThan:
                return GL.GL_GREATER;
            case NotEqualTo:
                return GL.GL_NOTEQUAL;
            case GreaterThanOrEqualTo:
                return GL.GL_GEQUAL;
            case Always:
                return GL.GL_ALWAYS;
        }
        throw new IllegalArgumentException("Invalid test function type: " + function);
    }
}
