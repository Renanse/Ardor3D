/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.state.record;

import java.nio.FloatBuffer;
import java.util.logging.Logger;

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.state.MaterialState.ColorMaterial;
import com.ardor3d.renderer.state.MaterialState.MaterialFace;
import com.ardor3d.util.geom.BufferUtils;

public class MaterialStateRecord extends StateRecord {
    private static final Logger logger = Logger.getLogger(MaterialStateRecord.class.getName());

    public ColorRGBA frontAmbient = new ColorRGBA(-1, -1, -1, -1);
    public ColorRGBA frontDiffuse = new ColorRGBA(-1, -1, -1, -1);
    public ColorRGBA frontSpecular = new ColorRGBA(-1, -1, -1, -1);
    public ColorRGBA frontEmissive = new ColorRGBA(-1, -1, -1, -1);
    public float frontShininess = Float.NEGATIVE_INFINITY;

    public ColorRGBA backAmbient = new ColorRGBA(-1, -1, -1, -1);
    public ColorRGBA backDiffuse = new ColorRGBA(-1, -1, -1, -1);
    public ColorRGBA backSpecular = new ColorRGBA(-1, -1, -1, -1);
    public ColorRGBA backEmissive = new ColorRGBA(-1, -1, -1, -1);
    public float backShininess = Float.NEGATIVE_INFINITY;

    public ColorMaterial colorMaterial = null;
    public MaterialFace colorMaterialFace = null;

    public FloatBuffer tempColorBuff = BufferUtils.createColorBuffer(1);

    public boolean isSetColor(final MaterialFace face, final ColorMaterial glMatColor, final ReadOnlyColorRGBA color,
            final MaterialStateRecord record) {
        if (face == MaterialFace.Front) {
            switch (glMatColor) {
                case Ambient:
                    return color.equals(frontAmbient);
                case Diffuse:
                    return color.equals(frontDiffuse);
                case Specular:
                    return color.equals(frontSpecular);
                case Emissive:
                    return color.equals(frontEmissive);
                default:
                    logger.warning("bad isSetColor");
            }
        } else if (face == MaterialFace.FrontAndBack) {
            switch (glMatColor) {
                case Ambient:
                    return color.equals(frontAmbient) && color.equals(backAmbient);
                case Diffuse:
                    return color.equals(frontDiffuse) && color.equals(backDiffuse);
                case Specular:
                    return color.equals(frontSpecular) && color.equals(backSpecular);
                case Emissive:
                    return color.equals(frontEmissive) && color.equals(backEmissive);
                default:
                    logger.warning("bad isSetColor");
            }
        } else if (face == MaterialFace.Back) {
            switch (glMatColor) {
                case Ambient:
                    return color.equals(backAmbient);
                case Diffuse:
                    return color.equals(backDiffuse);
                case Specular:
                    return color.equals(backSpecular);
                case Emissive:
                    return color.equals(backEmissive);
                default:
                    logger.warning("bad isSetColor");
            }
        }
        return false;
    }

    public boolean isSetShininess(final MaterialFace face, final float shininess, final MaterialStateRecord record) {
        if (face == MaterialFace.Front) {
            return shininess == frontShininess;
        } else if (face == MaterialFace.FrontAndBack) {
            return shininess == frontShininess && shininess == backShininess;
        } else if (face == MaterialFace.Back) {
            return shininess == backShininess;
        }
        return false;
    }

    public void setColor(final MaterialFace face, final ColorMaterial glMatColor, final ReadOnlyColorRGBA color) {
        if (face == MaterialFace.Front || face == MaterialFace.FrontAndBack) {
            switch (glMatColor) {
                case Ambient:
                    frontAmbient.set(color);
                    break;
                case Diffuse:
                    frontDiffuse.set(color);
                    break;
                case Specular:
                    frontSpecular.set(color);
                    break;
                case Emissive:
                    frontEmissive.set(color);
                    break;
                default:
                    logger.warning("bad setColor");
            }
        }
        if (face == MaterialFace.Back || face == MaterialFace.FrontAndBack) {
            switch (glMatColor) {
                case Ambient:
                    backAmbient.set(color);
                    break;
                case Diffuse:
                    backDiffuse.set(color);
                    break;
                case Specular:
                    backSpecular.set(color);
                    break;
                case Emissive:
                    backEmissive.set(color);
                    break;
                default:
                    logger.warning("bad setColor");
            }
        }
    }

    public void resetColorsForCM(final MaterialFace face, final ColorMaterial glMatColor) {
        if (face == MaterialFace.Front || face == MaterialFace.FrontAndBack) {
            switch (glMatColor) {
                case Ambient:
                    frontAmbient.set(-1, -1, -1, -1);
                    break;
                case Diffuse:
                    frontDiffuse.set(-1, -1, -1, -1);
                    break;
                case AmbientAndDiffuse:
                    frontAmbient.set(-1, -1, -1, -1);
                    frontDiffuse.set(-1, -1, -1, -1);
                    break;
                case Emissive:
                    frontEmissive.set(-1, -1, -1, -1);
                    break;
                case Specular:
                    frontSpecular.set(-1, -1, -1, -1);
                    break;
            }
        }
        if (face == MaterialFace.Back || face == MaterialFace.FrontAndBack) {
            switch (glMatColor) {
                case Ambient:
                    backAmbient.set(-1, -1, -1, -1);
                    break;
                case Diffuse:
                    backDiffuse.set(-1, -1, -1, -1);
                    break;
                case AmbientAndDiffuse:
                    backAmbient.set(-1, -1, -1, -1);
                    backDiffuse.set(-1, -1, -1, -1);
                    break;
                case Emissive:
                    backEmissive.set(-1, -1, -1, -1);
                    break;
                case Specular:
                    backSpecular.set(-1, -1, -1, -1);
                    break;
            }
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();

        frontAmbient.set(-1, -1, -1, -1);
        frontDiffuse.set(-1, -1, -1, -1);
        frontSpecular.set(-1, -1, -1, -1);
        frontEmissive.set(-1, -1, -1, -1);
        frontShininess = Float.NEGATIVE_INFINITY;

        backAmbient.set(-1, -1, -1, -1);
        backDiffuse.set(-1, -1, -1, -1);
        backSpecular.set(-1, -1, -1, -1);
        backEmissive.set(-1, -1, -1, -1);
        backShininess = Float.NEGATIVE_INFINITY;

        colorMaterial = null;
        colorMaterialFace = null;
    }
}
