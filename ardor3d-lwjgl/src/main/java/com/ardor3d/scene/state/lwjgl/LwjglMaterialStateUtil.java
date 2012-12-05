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

import org.lwjgl.opengl.GL11;

import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.state.MaterialState;
import com.ardor3d.renderer.state.MaterialState.ColorMaterial;
import com.ardor3d.renderer.state.MaterialState.MaterialFace;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.record.MaterialStateRecord;

public abstract class LwjglMaterialStateUtil {

    public static void apply(final MaterialState state) {
        // ask for the current state record
        final RenderContext context = ContextManager.getCurrentContext();
        final MaterialStateRecord record = (MaterialStateRecord) context.getStateRecord(StateType.Material);
        context.setCurrentState(StateType.Material, state);

        if (state.isEnabled()) {
            // setup colormaterial, if changed.
            applyColorMaterial(state.getColorMaterial(), state.getColorMaterialFace(), record);

            // apply colors, if needed and not what is currently set.
            applyColor(ColorMaterial.Ambient, state.getAmbient(), state.getBackAmbient(), record);
            applyColor(ColorMaterial.Diffuse, state.getDiffuse(), state.getBackDiffuse(), record);
            applyColor(ColorMaterial.Emissive, state.getEmissive(), state.getBackEmissive(), record);
            applyColor(ColorMaterial.Specular, state.getSpecular(), state.getBackSpecular(), record);

            // set our shine
            applyShininess(state.getShininess(), state.getBackShininess(), record);
        } else {
            // apply defaults
            applyColorMaterial(MaterialState.DEFAULT_COLOR_MATERIAL, MaterialState.DEFAULT_COLOR_MATERIAL_FACE, record);

            applyColor(ColorMaterial.Ambient, MaterialState.DEFAULT_AMBIENT, MaterialState.DEFAULT_AMBIENT, record);
            applyColor(ColorMaterial.Diffuse, MaterialState.DEFAULT_DIFFUSE, MaterialState.DEFAULT_DIFFUSE, record);
            applyColor(ColorMaterial.Emissive, MaterialState.DEFAULT_EMISSIVE, MaterialState.DEFAULT_EMISSIVE, record);
            applyColor(ColorMaterial.Specular, MaterialState.DEFAULT_SPECULAR, MaterialState.DEFAULT_SPECULAR, record);

            applyShininess(MaterialState.DEFAULT_SHININESS, MaterialState.DEFAULT_SHININESS, record);
        }

        if (!record.isValid()) {
            record.validate();
        }
    }

    private static void applyColor(final ColorMaterial glMatColor, final ReadOnlyColorRGBA frontColor,
            final ReadOnlyColorRGBA backColor, final MaterialStateRecord record) {
        final int glMat = getGLColorMaterial(glMatColor);
        if (frontColor.equals(backColor)) {
            // consolidate to one call
            if (!isVertexProvidedColor(MaterialFace.FrontAndBack, glMatColor, record)) {
                if (!record.isValid() || !record.isSetColor(MaterialFace.FrontAndBack, glMatColor, frontColor, record)) {
                    record.tempColorBuff.clear();
                    record.tempColorBuff.put(frontColor.getRed()).put(frontColor.getGreen()).put(frontColor.getBlue())
                            .put(frontColor.getAlpha());
                    record.tempColorBuff.flip();
                    GL11.glMaterial(getGLMaterialFace(MaterialFace.FrontAndBack), glMat, record.tempColorBuff);
                    record.setColor(MaterialFace.FrontAndBack, glMatColor, frontColor);
                }
            }
        } else {
            if (!isVertexProvidedColor(MaterialFace.Front, glMatColor, record)) {
                if (!record.isValid() || !record.isSetColor(MaterialFace.Front, glMatColor, frontColor, record)) {
                    record.tempColorBuff.clear();
                    record.tempColorBuff.put(frontColor.getRed()).put(frontColor.getGreen()).put(frontColor.getBlue())
                            .put(frontColor.getAlpha());
                    record.tempColorBuff.flip();
                    GL11.glMaterial(getGLMaterialFace(MaterialFace.Front), glMat, record.tempColorBuff);
                    record.setColor(MaterialFace.Front, glMatColor, frontColor);
                }
            }

            if (!isVertexProvidedColor(MaterialFace.Back, glMatColor, record)) {
                if (!record.isValid() || !record.isSetColor(MaterialFace.Back, glMatColor, backColor, record)) {
                    record.tempColorBuff.clear();
                    record.tempColorBuff.put(backColor.getRed()).put(backColor.getGreen()).put(backColor.getBlue())
                            .put(backColor.getAlpha());
                    record.tempColorBuff.flip();
                    GL11.glMaterial(getGLMaterialFace(MaterialFace.Back), glMat, record.tempColorBuff);
                    record.setColor(MaterialFace.Back, glMatColor, backColor);
                }
            }
        }
    }

    private static boolean isVertexProvidedColor(final MaterialFace face, final ColorMaterial glMatColor,
            final MaterialStateRecord record) {
        if (face != record.colorMaterialFace) {
            return false;
        }
        switch (glMatColor) {
            case Ambient:
                return record.colorMaterial == ColorMaterial.Ambient
                        || record.colorMaterial == ColorMaterial.AmbientAndDiffuse;
            case Diffuse:
                return record.colorMaterial == ColorMaterial.Diffuse
                        || record.colorMaterial == ColorMaterial.AmbientAndDiffuse;
            case Specular:
                return record.colorMaterial == ColorMaterial.Specular;
            case Emissive:
                return record.colorMaterial == ColorMaterial.Emissive;
        }
        return false;
    }

    private static void applyColorMaterial(final ColorMaterial colorMaterial, final MaterialFace face,
            final MaterialStateRecord record) {
        if (!record.isValid() || face != record.colorMaterialFace || colorMaterial != record.colorMaterial) {
            if (colorMaterial == ColorMaterial.None) {
                GL11.glDisable(GL11.GL_COLOR_MATERIAL);
            } else {
                final int glMat = getGLColorMaterial(colorMaterial);
                final int glFace = getGLMaterialFace(face);

                GL11.glColorMaterial(glFace, glMat);
                GL11.glEnable(GL11.GL_COLOR_MATERIAL);
                record.resetColorsForCM(face, colorMaterial);
            }
            record.colorMaterial = colorMaterial;
            record.colorMaterialFace = face;
        }
    }

    private static void applyShininess(final float frontShininess, final float backShininess,
            final MaterialStateRecord record) {
        if (frontShininess == backShininess) {
            // consolidate to one call
            if (!record.isValid() || frontShininess != record.frontShininess || record.backShininess != backShininess) {
                GL11.glMaterialf(getGLMaterialFace(MaterialFace.FrontAndBack), GL11.GL_SHININESS, frontShininess);
                record.backShininess = record.frontShininess = frontShininess;
            }
        } else {
            if (!record.isValid() || frontShininess != record.frontShininess) {
                GL11.glMaterialf(getGLMaterialFace(MaterialFace.Front), GL11.GL_SHININESS, frontShininess);
                record.frontShininess = frontShininess;
            }

            if (!record.isValid() || backShininess != record.backShininess) {
                GL11.glMaterialf(getGLMaterialFace(MaterialFace.Back), GL11.GL_SHININESS, backShininess);
                record.backShininess = backShininess;
            }
        }
    }

    /**
     * Converts the color material setting of this state to a GL constant.
     * 
     * @return the GL constant
     */
    private static int getGLColorMaterial(final ColorMaterial material) {
        switch (material) {
            case None:
                return GL11.GL_NONE;
            case Ambient:
                return GL11.GL_AMBIENT;
            case Diffuse:
                return GL11.GL_DIFFUSE;
            case AmbientAndDiffuse:
                return GL11.GL_AMBIENT_AND_DIFFUSE;
            case Emissive:
                return GL11.GL_EMISSION;
            case Specular:
                return GL11.GL_SPECULAR;
        }
        throw new IllegalArgumentException("invalid color material setting: " + material);
    }

    /**
     * Converts the material face setting of this state to a GL constant.
     * 
     * @return the GL constant
     */
    private static int getGLMaterialFace(final MaterialFace face) {
        switch (face) {
            case Front:
                return GL11.GL_FRONT;
            case Back:
                return GL11.GL_BACK;
            case FrontAndBack:
                return GL11.GL_FRONT_AND_BACK;
        }
        throw new IllegalArgumentException("invalid material face setting: " + face);
    }
}
