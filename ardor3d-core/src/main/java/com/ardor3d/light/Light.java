/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.light;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.material.IUniformSupplier;
import com.ardor3d.renderer.material.uniform.UniformRef;
import com.ardor3d.renderer.material.uniform.UniformSource;
import com.ardor3d.renderer.material.uniform.UniformType;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;

/**
 * <code>Light</code> defines the attributes of a light element. This class is abstract and intended
 * to be sub-classed by specific lighting types. A light will illuminate portions of the scene by
 * assigning its properties to the objects in the scene. This will affect the objects color values,
 * depending on the color of the ambient, diffuse and specular light components.
 *
 * Ambient light defines the general light of the scene, that is the intensity and color of lighting
 * if no particular lights are affecting it.
 *
 * Diffuse lighting defines the reflection of light on matte surfaces.
 *
 * Specular lighting defines the reflection of light on shiny surfaces.
 */
public abstract class Light implements Serializable, Savable, IUniformSupplier {

  private static final long serialVersionUID = 1L;

  protected final List<UniformRef> cachedUniforms = new ArrayList<>();

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

  private boolean _enabled;

  private String _name;

  /** when true, indicates the lights in this lightState will cast shadows. */
  protected boolean _shadowCaster;

  /**
   * Constructor instantiates a new <code>Light</code> object. All light color values are set to
   * white.
   *
   */
  public Light() {
    cachedUniforms
        .add(new UniformRef("type", UniformType.Int1, UniformSource.Supplier, (Supplier<Type>) this::getType));
    cachedUniforms.add(new UniformRef("ambient", UniformType.Float3, UniformSource.Supplier,
        (Supplier<ReadOnlyColorRGBA>) this::getAmbient));
    cachedUniforms.add(new UniformRef("diffuse", UniformType.Float3, UniformSource.Supplier,
        (Supplier<ReadOnlyColorRGBA>) this::getDiffuse));
    cachedUniforms.add(new UniformRef("specular", UniformType.Float3, UniformSource.Supplier,
        (Supplier<ReadOnlyColorRGBA>) this::getSpecular));
  }

  /**
   *
   * <code>getType</code> returns the type of the light that has been created.
   *
   * @return the type of light that has been created.
   */
  public abstract Type getType();

  /**
   *
   * <code>isEnabled</code> returns true if the light is enabled, false otherwise.
   *
   * @return true if the light is enabled, false if it is not.
   */
  public boolean isEnabled() { return _enabled; }

  /**
   *
   * <code>setEnabled</code> sets the light on or off. True turns it on, false turns it off.
   *
   * @param value
   *          true to turn the light on, false to turn it off.
   */
  public void setEnabled(final boolean value) { _enabled = value; }

  /**
   * <code>getSpecular</code> returns the specular color value for this light.
   *
   * @return the specular color value of the light.
   */
  public ReadOnlyColorRGBA getSpecular() { return _specular; }

  /**
   * <code>setSpecular</code> sets the specular color value for this light.
   *
   * @param specular
   *          the specular color value of the light.
   */
  public void setSpecular(final ReadOnlyColorRGBA specular) {
    _specular.set(specular);
  }

  /**
   * <code>getDiffuse</code> returns the diffuse color value for this light.
   *
   * @return the diffuse color value for this light.
   */
  public ReadOnlyColorRGBA getDiffuse() { return _diffuse; }

  /**
   * <code>setDiffuse</code> sets the diffuse color value for this light.
   *
   * @param diffuse
   *          the diffuse color value for this light.
   */
  public void setDiffuse(final ReadOnlyColorRGBA diffuse) {
    _diffuse.set(diffuse);
  }

  /**
   * <code>getAmbient</code> returns the ambient color value for this light.
   *
   * @return the ambient color value for this light.
   */
  public ReadOnlyColorRGBA getAmbient() { return _ambient; }

  /**
   * <code>setAmbient</code> sets the ambient color value for this light.
   *
   * @param ambient
   *          the ambient color value for this light.
   */
  public void setAmbient(final ReadOnlyColorRGBA ambient) {
    _ambient.set(ambient);
  }

  /**
   * @return Returns whether this light is able to cast shadows.
   */
  public boolean isShadowCaster() { return _shadowCaster; }

  /**
   * @param mayCastShadows
   *          true if this light can be used to derive shadows (when used in conjunction with a shadow
   *          pass.)
   */
  public void setShadowCaster(final boolean mayCastShadows) { _shadowCaster = mayCastShadows; }

  public String getName() { return _name; }

  public void setName(final String name) { _name = name; }

  @Override
  public List<UniformRef> getUniforms() { return cachedUniforms; }

  @Override
  public void write(final OutputCapsule capsule) throws IOException {
    capsule.write(_ambient, "ambient", (ColorRGBA) DEFAULT_AMBIENT);
    capsule.write(_diffuse, "diffuse", (ColorRGBA) DEFAULT_DIFFUSE);
    capsule.write(_specular, "specular", (ColorRGBA) DEFAULT_SPECULAR);
    capsule.write(_enabled, "enabled", false);
    capsule.write(_shadowCaster, "shadowCaster", false);
    capsule.write(_name, "name", null);
  }

  @Override
  public void read(final InputCapsule capsule) throws IOException {
    _ambient.set(capsule.readSavable("ambient", (ColorRGBA) DEFAULT_AMBIENT));
    _diffuse.set(capsule.readSavable("diffuse", (ColorRGBA) DEFAULT_DIFFUSE));
    _specular.set(capsule.readSavable("specular", (ColorRGBA) DEFAULT_SPECULAR));
    _enabled = capsule.readBoolean("enabled", false);
    _shadowCaster = capsule.readBoolean("shadowCaster", false);
    _name = capsule.readString("name", null);
  }

  @Override
  public Class<? extends Light> getClassTag() { return this.getClass(); }
}
