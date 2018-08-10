/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.light;

import java.io.IOException;
import java.io.Serializable;

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;

/**
 * <code>Light</code> defines the attributes of a light element. This class is abstract and intended to be sub-classed
 * by specific lighting types. A light will illuminate portions of the scene by assigning its properties to the objects
 * in the scene. This will affect the objects color values, depending on the color of the ambient, diffuse and specular
 * light components.
 * 
 * Ambient light defines the general light of the scene, that is the intensity and color of lighting if no particular
 * lights are affecting it.
 * 
 * Diffuse lighting defines the reflection of light on matte surfaces.
 * 
 * Specular lighting defines the reflection of light on shiny surfaces.
 */
public abstract class Light implements Serializable, Savable {

    private static final long serialVersionUID = 1L;

    /**
     * dark grey (.4, .4, .4, 1)
     */
    public static final ReadOnlyColorRGBA DEFAULT_AMBIENT = new ColorRGBA(0.4f, 0.4f, 0.4f, 1.0f);

    /**
     * white (1, 1, 1, 1)
     */
    public static final ReadOnlyColorRGBA DEFAULT_DIFFUSE = new ColorRGBA(1, 1, 1, 1);

    /**
     * white (1, 1, 1, 1)
     */
    public static final ReadOnlyColorRGBA DEFAULT_SPECULAR = new ColorRGBA(1, 1, 1, 1);

    public enum Type {
        Directional, Point, Spot
    }

    // light attributes.
    private final ColorRGBA _ambient = new ColorRGBA(DEFAULT_AMBIENT);
    private final ColorRGBA _diffuse = new ColorRGBA(DEFAULT_DIFFUSE);
    private final ColorRGBA _specular = new ColorRGBA(DEFAULT_SPECULAR);

    private boolean _attenuate;
    private float _constant = 1;
    private float _linear;
    private float _quadratic;

    private int _lightMask = 0;
    private int _backLightMask = 0;

    private boolean _enabled;

    private String _name;

    /** when true, indicates the lights in this lightState will cast shadows. */
    protected boolean _shadowCaster;

    /**
     * Constructor instantiates a new <code>Light</code> object. All light color values are set to white.
     * 
     */
    public Light() {}

    /**
     * 
     * <code>getType</code> returns the type of the light that has been created.
     * 
     * @return the type of light that has been created.
     */
    public abstract Type getType();

    /**
     * <code>getConstant</code> returns the value for the constant attenuation.
     * 
     * @return the value for the constant attenuation.
     */
    public float getConstant() {
        return _constant;
    }

    /**
     * <code>setConstant</code> sets the value for the constant attentuation.
     * 
     * @param constant
     *            the value for the constant attenuation.
     */
    public void setConstant(final float constant) {
        _constant = constant;
    }

    /**
     * <code>getLinear</code> returns the value for the linear attenuation.
     * 
     * @return the value for the linear attenuation.
     */
    public float getLinear() {
        return _linear;
    }

    /**
     * <code>setLinear</code> sets the value for the linear attentuation.
     * 
     * @param linear
     *            the value for the linear attenuation.
     */
    public void setLinear(final float linear) {
        _linear = linear;
    }

    /**
     * <code>getQuadratic</code> returns the value for the quadratic attentuation.
     * 
     * @return the value for the quadratic attenuation.
     */
    public float getQuadratic() {
        return _quadratic;
    }

    /**
     * <code>setQuadratic</code> sets the value for the quadratic attenuation.
     * 
     * @param quadratic
     *            the value for the quadratic attenuation.
     */
    public void setQuadratic(final float quadratic) {
        _quadratic = quadratic;
    }

    /**
     * <code>isAttenuate</code> returns true if attenuation is to be used for this light.
     * 
     * @return true if attenuation is to be used, false otherwise.
     */
    public boolean isAttenuate() {
        return _attenuate;
    }

    /**
     * <code>setAttenuate</code> sets if attenuation is to be used. True sets it on, false otherwise.
     * 
     * @param attenuate
     *            true to use attenuation, false not to.
     */
    public void setAttenuate(final boolean attenuate) {
        _attenuate = attenuate;
    }

    /**
     * 
     * <code>isEnabled</code> returns true if the light is enabled, false otherwise.
     * 
     * @return true if the light is enabled, false if it is not.
     */
    public boolean isEnabled() {
        return _enabled;
    }

    /**
     * 
     * <code>setEnabled</code> sets the light on or off. True turns it on, false turns it off.
     * 
     * @param value
     *            true to turn the light on, false to turn it off.
     */
    public void setEnabled(final boolean value) {
        _enabled = value;
    }

    /**
     * <code>getSpecular</code> returns the specular color value for this light.
     * 
     * @return the specular color value of the light.
     */
    public ReadOnlyColorRGBA getSpecular() {
        return _specular;
    }

    /**
     * <code>setSpecular</code> sets the specular color value for this light.
     * 
     * @param specular
     *            the specular color value of the light.
     */
    public void setSpecular(final ReadOnlyColorRGBA specular) {
        this._specular.set(specular);
    }

    /**
     * <code>getDiffuse</code> returns the diffuse color value for this light.
     * 
     * @return the diffuse color value for this light.
     */
    public ReadOnlyColorRGBA getDiffuse() {
        return _diffuse;
    }

    /**
     * <code>setDiffuse</code> sets the diffuse color value for this light.
     * 
     * @param diffuse
     *            the diffuse color value for this light.
     */
    public void setDiffuse(final ReadOnlyColorRGBA diffuse) {
        this._diffuse.set(diffuse);
    }

    /**
     * <code>getAmbient</code> returns the ambient color value for this light.
     * 
     * @return the ambient color value for this light.
     */
    public ReadOnlyColorRGBA getAmbient() {
        return _ambient;
    }

    /**
     * <code>setAmbient</code> sets the ambient color value for this light.
     * 
     * @param ambient
     *            the ambient color value for this light.
     */
    public void setAmbient(final ReadOnlyColorRGBA ambient) {
        this._ambient.set(ambient);
    }

    /**
     * @return Returns the lightMask - default is 0 or not masked.
     */
    public int getLightMask() {
        return _lightMask;
    }

    /**
     * <code>setLightMask</code> sets what attributes of this light to apply as an int comprised of bitwise |'ed values
     * from LightState.Mask_XXXX. LightMask.MASK_GLOBALAMBIENT is ignored.
     * 
     * @param lightMask
     *            The lightMask to set.
     */
    public void setLightMask(final int lightMask) {
        _lightMask = lightMask;
    }

    /**
     * Saves the light mask to a back store. That backstore is recalled with popLightMask. Despite the name, this is not
     * a stack and additional pushes will simply overwrite the backstored value.
     */
    public void pushLightMask() {
        _backLightMask = _lightMask;
    }

    /**
     * Recalls the light mask from a back store or 0 if none was pushed.
     * 
     * @see com.ardor3d.light.Light#pushLightMask()
     */
    public void popLightMask() {
        _lightMask = _backLightMask;
    }

    /**
     * @return Returns whether this light is able to cast shadows.
     */
    public boolean isShadowCaster() {
        return _shadowCaster;
    }

    /**
     * @param mayCastShadows
     *            true if this light can be used to derive shadows (when used in conjunction with a shadow pass.)
     */
    public void setShadowCaster(final boolean mayCastShadows) {
        _shadowCaster = mayCastShadows;
    }

    /**
     * Copies the light values from the given light into this Light.
     * 
     * @param light
     *            the Light to copy from.
     */
    public void copyFrom(final Light light) {
        _ambient.set(light._ambient);
        _attenuate = light._attenuate;
        _constant = light._constant;
        _diffuse.set(light._diffuse);
        _enabled = light._enabled;
        _linear = light._linear;
        _quadratic = light._quadratic;
        _shadowCaster = light._shadowCaster;
        _specular.set(light._specular);
    }

    public String getName() {
        return _name;
    }

    public void setName(final String name) {
        this._name = name;
    }

    public void write(final OutputCapsule capsule) throws IOException {
        capsule.write(_ambient, "ambient", new ColorRGBA(DEFAULT_AMBIENT));
        capsule.write(_diffuse, "diffuse", new ColorRGBA(DEFAULT_DIFFUSE));
        capsule.write(_specular, "specular", new ColorRGBA(DEFAULT_SPECULAR));
        capsule.write(_attenuate, "attenuate", false);
        capsule.write(_constant, "constant", 1);
        capsule.write(_linear, "linear", 0);
        capsule.write(_quadratic, "quadratic", 0);
        capsule.write(_lightMask, "lightMask", 0);
        capsule.write(_backLightMask, "backLightMask", 0);
        capsule.write(_enabled, "enabled", false);
        capsule.write(_shadowCaster, "shadowCaster", false);
        capsule.write(_name, "name", null);
    }

    public void read(final InputCapsule capsule) throws IOException {
        _ambient.set((ColorRGBA) capsule.readSavable("ambient", new ColorRGBA(DEFAULT_AMBIENT)));
        _diffuse.set((ColorRGBA) capsule.readSavable("diffuse", new ColorRGBA(DEFAULT_DIFFUSE)));
        _specular.set((ColorRGBA) capsule.readSavable("specular", new ColorRGBA(DEFAULT_SPECULAR)));
        _attenuate = capsule.readBoolean("attenuate", false);
        _constant = capsule.readFloat("constant", 1);
        _linear = capsule.readFloat("linear", 0);
        _quadratic = capsule.readFloat("quadratic", 0);
        _lightMask = capsule.readInt("lightMask", 0);
        _backLightMask = capsule.readInt("backLightMask", 0);
        _enabled = capsule.readBoolean("enabled", false);
        _shadowCaster = capsule.readBoolean("shadowCaster", false);
        _name = capsule.readString("name", null);
    }

    public Class<? extends Light> getClassTag() {
        return this.getClass();
    }
}
