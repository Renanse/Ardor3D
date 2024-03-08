/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.scene.state.lwjgl3;

import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL13C;
import org.lwjgl.opengl.GL14C;
import org.lwjgl.opengl.GL20C;

import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.BlendState.BlendEquation;
import com.ardor3d.renderer.state.BlendState.DestinationFunction;
import com.ardor3d.renderer.state.BlendState.SourceFunction;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.record.BlendStateRecord;

public abstract class Lwjgl3BlendStateUtil {

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

      applyAlphaCoverage(state.isSampleAlphaToCoverageEnabled(), state.isSampleAlphaToOneEnabled(), record, caps);
      applySampleCoverage(state.isSampleCoverageEnabled(), state, record, caps);
    } else {
      // disable blend
      applyBlendEquations(false, state, record, caps);

      // disable sample coverage
      applyAlphaCoverage(false, false, record, caps);
      applySampleCoverage(false, state, record, caps);
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
          GL11C.glEnable(GL11C.GL_BLEND);
          record.blendEnabled = true;
        }
        final int blendEqRGB = getGLEquationValue(state.getBlendEquationRGB(), caps);
        final int blendEqAlpha = getGLEquationValue(state.getBlendEquationAlpha(), caps);
        if (record.blendEqRGB != blendEqRGB || record.blendEqAlpha != blendEqAlpha) {
          GL20C.glBlendEquationSeparate(blendEqRGB, blendEqAlpha);
          record.blendEqRGB = blendEqRGB;
          record.blendEqAlpha = blendEqAlpha;
        }
      } else if (record.blendEnabled) {
        GL11C.glDisable(GL11C.GL_BLEND);
        record.blendEnabled = false;
      }

    } else {
      if (enabled) {
        GL11C.glEnable(GL11C.GL_BLEND);
        record.blendEnabled = true;
        final int blendEqRGB = getGLEquationValue(state.getBlendEquationRGB(), caps);
        final int blendEqAlpha = getGLEquationValue(state.getBlendEquationAlpha(), caps);
        GL20C.glBlendEquationSeparate(blendEqRGB, blendEqAlpha);
        record.blendEqRGB = blendEqRGB;
        record.blendEqAlpha = blendEqAlpha;
      } else {
        GL11C.glDisable(GL11C.GL_BLEND);
        record.blendEnabled = false;
      }
    }
  }

  protected static void applyBlendColor(final boolean enabled, final BlendState state, final BlendStateRecord record,
      final ContextCapabilities caps) {
    if (enabled) {
      final boolean applyConstant =
          state.getDestinationFunctionRGB().usesConstantColor() || state.getSourceFunctionRGB().usesConstantColor()
              || ((state.getDestinationFunctionAlpha().usesConstantColor()
                  || state.getSourceFunctionAlpha().usesConstantColor()));
      if (applyConstant) {
        final ReadOnlyColorRGBA constant = state.getConstantColor();
        if (!record.isValid() || (!record.blendColor.equals(constant))) {
          GL14C.glBlendColor(constant.getRed(), constant.getGreen(), constant.getBlue(), constant.getAlpha());
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
        final int glSrcAlpha = getGLSrcValue(state.getSourceFunctionAlpha(), caps);
        final int glDstAlpha = getGLDstValue(state.getDestinationFunctionAlpha(), caps);
        if (record.srcFactorRGB != glSrcRGB || record.dstFactorRGB != glDstRGB || record.srcFactorAlpha != glSrcAlpha
            || record.dstFactorAlpha != glDstAlpha) {
          GL14C.glBlendFuncSeparate(glSrcRGB, glDstRGB, glSrcAlpha, glDstAlpha);
          record.srcFactorRGB = glSrcRGB;
          record.dstFactorRGB = glDstRGB;
          record.srcFactorAlpha = glSrcAlpha;
          record.dstFactorAlpha = glDstAlpha;
        }
      }
    } else {
      if (enabled) {
        final int glSrcRGB = getGLSrcValue(state.getSourceFunctionRGB(), caps);
        final int glDstRGB = getGLDstValue(state.getDestinationFunctionRGB(), caps);
        final int glSrcAlpha = getGLSrcValue(state.getSourceFunctionAlpha(), caps);
        final int glDstAlpha = getGLDstValue(state.getDestinationFunctionAlpha(), caps);
        GL14C.glBlendFuncSeparate(glSrcRGB, glDstRGB, glSrcAlpha, glDstAlpha);
        record.srcFactorRGB = glSrcRGB;
        record.dstFactorRGB = glDstRGB;
        record.srcFactorAlpha = glSrcAlpha;
        record.dstFactorAlpha = glDstAlpha;
      }
    }
  }

  protected static void applyAlphaCoverage(final boolean sampleAlphaToCoverageEnabled,
      final boolean sampleAlphaToOneEnabled, final BlendStateRecord record, final ContextCapabilities caps) {
    if (record.isValid()) {
      if (sampleAlphaToCoverageEnabled != record.sampleAlphaToCoverageEnabled) {
        if (sampleAlphaToCoverageEnabled) {
          GL11C.glEnable(GL13C.GL_SAMPLE_ALPHA_TO_COVERAGE);
        } else {
          GL11C.glDisable(GL13C.GL_SAMPLE_ALPHA_TO_COVERAGE);
        }
        record.sampleAlphaToCoverageEnabled = sampleAlphaToCoverageEnabled;
      }
      if (sampleAlphaToOneEnabled != record.sampleAlphaToOneEnabled) {
        if (sampleAlphaToOneEnabled) {
          GL11C.glEnable(GL13C.GL_SAMPLE_ALPHA_TO_ONE);
        } else {
          GL11C.glDisable(GL13C.GL_SAMPLE_ALPHA_TO_ONE);
        }
        record.sampleAlphaToOneEnabled = sampleAlphaToOneEnabled;
      }
    } else {
      if (sampleAlphaToCoverageEnabled) {
        GL11C.glEnable(GL13C.GL_SAMPLE_ALPHA_TO_COVERAGE);
      } else {
        GL11C.glDisable(GL13C.GL_SAMPLE_ALPHA_TO_COVERAGE);
      }
      record.sampleAlphaToCoverageEnabled = sampleAlphaToCoverageEnabled;
      if (sampleAlphaToOneEnabled) {
        GL11C.glEnable(GL13C.GL_SAMPLE_ALPHA_TO_ONE);
      } else {
        GL11C.glDisable(GL13C.GL_SAMPLE_ALPHA_TO_ONE);
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
          GL11C.glEnable(GL13C.GL_SAMPLE_COVERAGE);
          record.sampleCoverageEnabled = true;
        }
        if (record.sampleCoverageInverted != coverageInverted || record.sampleCoverage != coverageValue) {
          GL13C.glSampleCoverage(coverageValue, coverageInverted);
          record.sampleCoverageInverted = coverageInverted;
          record.sampleCoverage = coverageValue;
        }
      } else {
        if (record.sampleCoverageEnabled) {
          GL11C.glDisable(GL13C.GL_SAMPLE_COVERAGE);
          record.sampleCoverageEnabled = false;
        }
      }
    } else {
      if (enabled) {
        GL11C.glEnable(GL13C.GL_SAMPLE_COVERAGE);
        record.sampleCoverageEnabled = true;
        GL13C.glSampleCoverage(coverageValue, coverageInverted);
        record.sampleCoverageInverted = coverageInverted;
        record.sampleCoverage = coverageValue;
      } else {
        GL11C.glDisable(GL13C.GL_SAMPLE_COVERAGE);
        record.sampleCoverageEnabled = false;
      }
    }
  }

  protected static int getGLSrcValue(final SourceFunction function, final ContextCapabilities caps) {
    return switch (function) {
      case Zero -> GL11C.GL_ZERO;
      case DestinationColor -> GL11C.GL_DST_COLOR;
      case OneMinusDestinationColor -> GL11C.GL_ONE_MINUS_DST_COLOR;
      case SourceAlpha -> GL11C.GL_SRC_ALPHA;
      case OneMinusSourceAlpha -> GL11C.GL_ONE_MINUS_SRC_ALPHA;
      case DestinationAlpha -> GL11C.GL_DST_ALPHA;
      case OneMinusDestinationAlpha -> GL11C.GL_ONE_MINUS_DST_ALPHA;
      case SourceAlphaSaturate -> GL11C.GL_SRC_ALPHA_SATURATE;
      case ConstantColor -> GL14C.GL_CONSTANT_COLOR;
      case OneMinusConstantColor -> GL14C.GL_ONE_MINUS_CONSTANT_COLOR;
      case ConstantAlpha -> GL14C.GL_CONSTANT_ALPHA;
      case OneMinusConstantAlpha -> GL14C.GL_ONE_MINUS_CONSTANT_ALPHA;
      case One -> GL11C.GL_ONE;
      default -> throw new IllegalArgumentException("Invalid source function type: " + function);
    };
  }

  protected static int getGLDstValue(final DestinationFunction function, final ContextCapabilities caps) {
    return switch (function) {
      case Zero -> GL11C.GL_ZERO;
      case SourceColor -> GL11C.GL_SRC_COLOR;
      case OneMinusSourceColor -> GL11C.GL_ONE_MINUS_SRC_COLOR;
      case SourceAlpha -> GL11C.GL_SRC_ALPHA;
      case OneMinusSourceAlpha -> GL11C.GL_ONE_MINUS_SRC_ALPHA;
      case DestinationAlpha -> GL11C.GL_DST_ALPHA;
      case OneMinusDestinationAlpha -> GL11C.GL_ONE_MINUS_DST_ALPHA;
      case ConstantColor -> GL14C.GL_CONSTANT_COLOR;
      case OneMinusConstantColor -> GL14C.GL_ONE_MINUS_CONSTANT_COLOR;
      case ConstantAlpha -> GL14C.GL_CONSTANT_ALPHA;
      case OneMinusConstantAlpha -> GL14C.GL_ONE_MINUS_CONSTANT_ALPHA;
      case One -> GL11C.GL_ONE;
      default -> throw new IllegalArgumentException("Invalid destination function type: " + function);
    };
  }

  protected static int getGLEquationValue(final BlendEquation eq, final ContextCapabilities caps) {
    return switch (eq) {
      case Min -> GL14C.GL_MIN;
      case Max -> GL14C.GL_MAX;
      case Subtract -> GL14C.GL_FUNC_SUBTRACT;
      case ReverseSubtract -> GL14C.GL_FUNC_REVERSE_SUBTRACT;
      case Add -> GL14C.GL_FUNC_ADD;
      default -> throw new IllegalArgumentException("Invalid blend equation: " + eq);
    };
  }
}
