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
import com.ardor3d.renderer.state.record.MaterialStateRecord;
import com.ardor3d.renderer.state.record.StateRecord;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;

/**
 * MaterialState defines parameters used in conjunction with the lighting model to produce a surface color. Please note
 * therefore that this state has no effect if lighting is disabled. It is also worth noting that material properties set
 * for Front face will in fact affect both front and back unless two sided lighting is enabled.
 * 
 * @see LightState
 * @see LightState#setTwoSidedLighting(boolean)
 */
public class MaterialState extends RenderState {

    public enum ColorMaterial {
        /** Mesh colors are ignored. This is default. */
        None,

        /** Mesh colors determine material ambient color. */
        Ambient,

        /** Mesh colors determine material diffuse color. */
        Diffuse,

        /** Mesh colors determine material ambient and diffuse colors. */
        AmbientAndDiffuse,

        /** Mesh colors determine material specular colors. */
        Specular,

        /** Mesh colors determine material emissive color. */
        Emissive;
    }

    public enum MaterialFace {
        /** Apply material property to front face only. */
        Front,

        /**
         * Apply material property to back face only. Note that this only has an affect if two sided lighting is
         * enabled.
         */
        Back,

        /** Apply material property to front and back faces. */
        FrontAndBack;
    }

    /** Default ambient color for all material states. (.2, .2, .2, 1) */
    public static final ReadOnlyColorRGBA DEFAULT_AMBIENT = new ColorRGBA(0.2f, 0.2f, 0.2f, 1.0f);

    /** Default diffuse color for all material states. (.8, .8, .8, 1) */
    public static final ReadOnlyColorRGBA DEFAULT_DIFFUSE = new ColorRGBA(0.8f, 0.8f, 0.8f, 1.0f);

    /** Default specular color for all material states. (0, 0, 0, 1) */
    public static final ReadOnlyColorRGBA DEFAULT_SPECULAR = new ColorRGBA(0.0f, 0.0f, 0.0f, 1.0f);

    /** Default emissive color for all material states. (0, 0, 0, 1) */
    public static final ReadOnlyColorRGBA DEFAULT_EMISSIVE = new ColorRGBA(0.0f, 0.0f, 0.0f, 1.0f);

    /** Default shininess for all material states. */
    public static final float DEFAULT_SHININESS = 0.0f;

    /** Default color material mode for all material states. */
    public static final ColorMaterial DEFAULT_COLOR_MATERIAL = ColorMaterial.None;

    /** Default color material face for all material states. */
    public static final MaterialFace DEFAULT_COLOR_MATERIAL_FACE = MaterialFace.FrontAndBack;

    // front face attributes of the material (used for back face if lighting is not two sided)
    protected final ColorRGBA _frontAmbient = new ColorRGBA(DEFAULT_AMBIENT);
    protected final ColorRGBA _frontDiffuse = new ColorRGBA(DEFAULT_DIFFUSE);
    protected final ColorRGBA _frontSpecular = new ColorRGBA(DEFAULT_SPECULAR);
    protected final ColorRGBA _frontEmissive = new ColorRGBA(DEFAULT_EMISSIVE);
    protected float _frontShininess = DEFAULT_SHININESS;

    // back face attributes of the material (only used if lighting is two sided)
    protected final ColorRGBA _backAmbient = new ColorRGBA(DEFAULT_AMBIENT);
    protected final ColorRGBA _backDiffuse = new ColorRGBA(DEFAULT_DIFFUSE);
    protected final ColorRGBA _backSpecular = new ColorRGBA(DEFAULT_SPECULAR);
    protected final ColorRGBA _backEmissive = new ColorRGBA(DEFAULT_EMISSIVE);
    protected float _backShininess = DEFAULT_SHININESS;

    protected ColorMaterial _colorMaterial = DEFAULT_COLOR_MATERIAL;
    protected MaterialFace _colorMaterialFace = DEFAULT_COLOR_MATERIAL_FACE;

    /**
     * Constructor instantiates a new <code>MaterialState</code> object.
     */
    public MaterialState() {}

    /**
     * @return the ambient color (or front face color, if two sided lighting is used) of this material.
     */
    public ReadOnlyColorRGBA getAmbient() {
        return _frontAmbient;
    }

    /**
     * @return the ambient back face color of this material. This is only used if two sided lighting is used.
     */
    public ReadOnlyColorRGBA getBackAmbient() {
        return _backAmbient;
    }

    /**
     * Sets the ambient color for front and back to the given value.
     * 
     * @param ambient
     *            the new ambient color
     */
    public void setAmbient(final ReadOnlyColorRGBA ambient) {
        setAmbient(MaterialFace.FrontAndBack, ambient);
    }

    /**
     * @param face
     *            the face to apply the ambient color to
     * @param ambient
     *            the new ambient color
     */
    public void setAmbient(final MaterialFace face, final ReadOnlyColorRGBA ambient) {
        if (face == MaterialFace.Front || face == MaterialFace.FrontAndBack) {
            _frontAmbient.set(ambient);
        }
        if (face == MaterialFace.Back || face == MaterialFace.FrontAndBack) {
            _backAmbient.set(ambient);
        }
        setNeedsRefresh(true);
    }

    /**
     * @return the diffuse color (or front face color, if two sided lighting is used) of this material.
     */
    public ReadOnlyColorRGBA getDiffuse() {
        return _frontDiffuse;
    }

    /**
     * @return the diffuse back face color of this material. This is only used if two sided lighting is used.
     */
    public ReadOnlyColorRGBA getBackDiffuse() {
        return _backDiffuse;
    }

    /**
     * Sets the diffuse color for front and back to the given value.
     * 
     * @param diffuse
     *            the new diffuse color
     */
    public void setDiffuse(final ReadOnlyColorRGBA diffuse) {
        setDiffuse(MaterialFace.FrontAndBack, diffuse);
    }

    /**
     * @param face
     *            the face to apply the diffuse color to
     * @param diffuse
     *            the new diffuse color
     */
    public void setDiffuse(final MaterialFace face, final ReadOnlyColorRGBA diffuse) {
        if (face == MaterialFace.Front || face == MaterialFace.FrontAndBack) {
            _frontDiffuse.set(diffuse);
        }
        if (face == MaterialFace.Back || face == MaterialFace.FrontAndBack) {
            _backDiffuse.set(diffuse);
        }
        setNeedsRefresh(true);
    }

    /**
     * @return the emissive color (or front face color, if two sided lighting is used) of this material.
     */
    public ReadOnlyColorRGBA getEmissive() {
        return _frontEmissive;
    }

    /**
     * @return the emissive back face color of this material. This is only used if two sided lighting is used.
     */
    public ReadOnlyColorRGBA getBackEmissive() {
        return _backEmissive;
    }

    /**
     * Sets the emissive color for front and back to the given value.
     * 
     * @param emissive
     *            the new emissive color
     */
    public void setEmissive(final ReadOnlyColorRGBA emissive) {
        setEmissive(MaterialFace.FrontAndBack, emissive);
    }

    /**
     * @param face
     *            the face to apply the emissive color to
     * @param emissive
     *            the new emissive color
     */
    public void setEmissive(final MaterialFace face, final ReadOnlyColorRGBA emissive) {
        if (face == MaterialFace.Front || face == MaterialFace.FrontAndBack) {
            _frontEmissive.set(emissive);
        }
        if (face == MaterialFace.Back || face == MaterialFace.FrontAndBack) {
            _backEmissive.set(emissive);
        }
        setNeedsRefresh(true);
    }

    /**
     * @return the specular color (or front face color, if two sided lighting is used) of this material.
     */
    public ReadOnlyColorRGBA getSpecular() {
        return _frontSpecular;
    }

    /**
     * @return the specular back face color of this material. This is only used if two sided lighting is used.
     */
    public ReadOnlyColorRGBA getBackSpecular() {
        return _backSpecular;
    }

    /**
     * Sets the specular color for front and back to the given value.
     * 
     * @param specular
     *            the new specular color
     */
    public void setSpecular(final ReadOnlyColorRGBA specular) {
        setSpecular(MaterialFace.FrontAndBack, specular);
    }

    /**
     * @param face
     *            the face to apply the specular color to
     * @param specular
     *            the new specular color
     */
    public void setSpecular(final MaterialFace face, final ReadOnlyColorRGBA specular) {
        if (face == MaterialFace.Front || face == MaterialFace.FrontAndBack) {
            _frontSpecular.set(specular);
        }
        if (face == MaterialFace.Back || face == MaterialFace.FrontAndBack) {
            _backSpecular.set(specular);
        }
        setNeedsRefresh(true);
    }

    /**
     * @return the shininess value (or front face shininess value, if two sided lighting is used) of the material.
     */
    public float getShininess() {
        return _frontShininess;
    }

    /**
     * @return the shininess value of the back face of this material. This is only used if two sided lighting is used.
     */
    public float getBackShininess() {
        return _backShininess;
    }

    /**
     * Sets the shininess value for front and back to the given value.
     * 
     * @param shininess
     *            the new shininess for this material. Must be between 0 and 128. Higher numbers result in "tighter"
     *            specular reflections.
     */
    public void setShininess(final float shininess) {
        setShininess(MaterialFace.FrontAndBack, shininess);
    }

    /**
     * @param face
     *            the face to apply the shininess color to
     * @param shininess
     *            the new shininess for this material. Must be between 0 and 128. Higher numbers result in "tighter"
     *            specular reflections.
     */
    public void setShininess(final MaterialFace face, final float shininess) {
        if (shininess < 0 || shininess > 128) {
            throw new IllegalArgumentException("Shininess must be between 0 and 128.");
        }
        if (face == MaterialFace.Front || face == MaterialFace.FrontAndBack) {
            _frontShininess = shininess;
        }
        if (face == MaterialFace.Back || face == MaterialFace.FrontAndBack) {
            _backShininess = shininess;
        }
        setNeedsRefresh(true);
    }

    /**
     * @return the color material mode of this material, which determines how geometry colors affect the material.
     * @see ColorMaterial
     */
    public ColorMaterial getColorMaterial() {
        return _colorMaterial;
    }

    /**
     * @param material
     *            the new color material mode
     * @throws IllegalArgumentException
     *             if material is null
     */
    public void setColorMaterial(final ColorMaterial material) {
        if (material == null) {
            throw new IllegalArgumentException("material can not be null.");
        }
        _colorMaterial = material;
        setNeedsRefresh(true);
    }

    /**
     * @return the color material face of this material, which determines how geometry colors affect the material.
     * @see ColorMaterial
     */
    public MaterialFace getColorMaterialFace() {
        return _colorMaterialFace;
    }

    /**
     * @param face
     *            the new color material face
     * @throws IllegalArgumentException
     *             if face is null
     */
    public void setColorMaterialFace(final MaterialFace face) {
        if (face == null) {
            throw new IllegalArgumentException("face can not be null.");
        }
        _colorMaterialFace = face;
        setNeedsRefresh(true);
    }

    @Override
    public StateType getType() {
        return StateType.Material;
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        super.write(capsule);
        capsule.write(_frontAmbient, "frontAmbient", (ColorRGBA) DEFAULT_AMBIENT);
        capsule.write(_frontDiffuse, "frontDiffuse", (ColorRGBA) DEFAULT_DIFFUSE);
        capsule.write(_frontSpecular, "frontSpecular", (ColorRGBA) DEFAULT_SPECULAR);
        capsule.write(_frontEmissive, "frontEmissive", (ColorRGBA) DEFAULT_EMISSIVE);
        capsule.write(_frontShininess, "frontShininess", DEFAULT_SHININESS);
        capsule.write(_backAmbient, "backAmbient", (ColorRGBA) DEFAULT_AMBIENT);
        capsule.write(_backDiffuse, "backDiffuse", (ColorRGBA) DEFAULT_DIFFUSE);
        capsule.write(_backSpecular, "backSpecular", (ColorRGBA) DEFAULT_SPECULAR);
        capsule.write(_backEmissive, "backEmissive", (ColorRGBA) DEFAULT_EMISSIVE);
        capsule.write(_backShininess, "backShininess", DEFAULT_SHININESS);
        capsule.write(_colorMaterial, "colorMaterial", DEFAULT_COLOR_MATERIAL);
        capsule.write(_colorMaterialFace, "colorMaterialFace", DEFAULT_COLOR_MATERIAL_FACE);
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        super.read(capsule);
        _frontAmbient.set((ColorRGBA) capsule.readSavable("frontAmbient", (ColorRGBA) DEFAULT_AMBIENT));
        _frontDiffuse.set((ColorRGBA) capsule.readSavable("frontDiffuse", (ColorRGBA) DEFAULT_DIFFUSE));
        _frontSpecular.set((ColorRGBA) capsule.readSavable("frontSpecular", (ColorRGBA) DEFAULT_SPECULAR));
        _frontEmissive.set((ColorRGBA) capsule.readSavable("frontEmissive", (ColorRGBA) DEFAULT_EMISSIVE));
        _frontShininess = capsule.readFloat("frontShininess", DEFAULT_SHININESS);
        _backAmbient.set((ColorRGBA) capsule.readSavable("backAmbient", (ColorRGBA) DEFAULT_AMBIENT));
        _backDiffuse.set((ColorRGBA) capsule.readSavable("backDiffuse", (ColorRGBA) DEFAULT_DIFFUSE));
        _backSpecular.set((ColorRGBA) capsule.readSavable("backSpecular", (ColorRGBA) DEFAULT_SPECULAR));
        _backEmissive.set((ColorRGBA) capsule.readSavable("backEmissive", (ColorRGBA) DEFAULT_EMISSIVE));
        _backShininess = capsule.readFloat("backShininess", DEFAULT_SHININESS);
        _colorMaterial = capsule.readEnum("colorMaterial", ColorMaterial.class, DEFAULT_COLOR_MATERIAL);
        _colorMaterialFace = capsule.readEnum("colorMaterialFace", MaterialFace.class, DEFAULT_COLOR_MATERIAL_FACE);
    }

    @Override
    public StateRecord createStateRecord(final ContextCapabilities caps) {
        return new MaterialStateRecord();
    }
}
