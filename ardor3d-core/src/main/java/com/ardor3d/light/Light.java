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
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.material.IUniformSupplier;
import com.ardor3d.renderer.material.uniform.UniformRef;
import com.ardor3d.renderer.material.uniform.UniformSource;
import com.ardor3d.renderer.material.uniform.UniformType;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;

/**
 * <code>Light</code> defines the attributes of a light element. This class is abstract and intended
 * to be sub-classed by specific lighting types. A light will illuminate portions of the scene by
 * assigning its properties to the objects in the scene. This will affect the objects color value.
 *
 * Ambient light defines the general light of the scene, that is the intensity and color of lighting
 * if no particular lights are affecting it.
 *
 * Diffuse lighting defines the reflection of light on matte surfaces.
 *
 * Specular lighting defines the reflection of light on shiny surfaces.
 */
public abstract class Light extends Spatial implements Serializable, Savable, IUniformSupplier {

  private static final long serialVersionUID = 1L;

  protected final List<UniformRef> cachedUniforms = new ArrayList<>();

  /**
   * white (1, 1, 1, 1)
   */
  public static final ReadOnlyColorRGBA DEFAULT_COLOR = new ColorRGBA(1, 1, 1, 1);

  public static final float DEFAULT_INTENSITY = 1f;

  public enum Type {
    Directional, Point, Spot
  }

  // light attributes.
  private final ColorRGBA _color = new ColorRGBA(Light.DEFAULT_COLOR);

  private float _intensity = Light.DEFAULT_INTENSITY;

  private boolean _enabled = true;

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
    cachedUniforms.add(new UniformRef("enabled", UniformType.Int1, UniformSource.Supplier,
        (Supplier<Integer>) () -> isEnabled() ? 1 : 0));

    cachedUniforms.add(new UniformRef("color", UniformType.Float3, UniformSource.Supplier,
        (Supplier<ReadOnlyColorRGBA>) this::getColor));
    cachedUniforms.add(
        new UniformRef("intensity", UniformType.Float1, UniformSource.Supplier, (Supplier<Float>) this::getIntensity));

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
   * <code>getColor</code> returns the color value for this light.
   *
   * @return the color value for this light.
   */
  public ReadOnlyColorRGBA getColor() { return _color; }

  /**
   * <code>setDiffuse</code> sets the color value for this light.
   *
   * @param color
   *          the color value for this light.
   */
  public void setColor(final ReadOnlyColorRGBA color) {
    _color.set(color);
  }

  public float getIntensity() { return _intensity; }

  public void setIntensity(final float intensity) { _intensity = intensity; }

  /**
   * @return Returns whether this light is able to cast shadows.
   */
  public boolean isShadowCaster() { return _shadowCaster; }

  /**
   * @param mayCastShadows
   *          true if this light can be used to derive shadows (when used in conjunction with an
   *          appropriate RenderMaterial.)
   */
  public void setShadowCaster(final boolean mayCastShadows) { _shadowCaster = mayCastShadows; }

  @Override
  public List<UniformRef> getUniforms() { return cachedUniforms; }

  @Override
  public void draw(final Renderer renderer) {
    // ignore
  }

  @Override
  public void updateWorldBound(final boolean recurse) {
    // ignore - maybe useful later for calculating contribution?
  }

  @Override
  public void write(final OutputCapsule capsule) throws IOException {
    super.write(capsule);
    capsule.write(_color, "diffuse", (ColorRGBA) Light.DEFAULT_COLOR);
    capsule.write(_intensity, "intensity", Light.DEFAULT_INTENSITY);
    capsule.write(_enabled, "enabled", false);
    capsule.write(_shadowCaster, "shadowCaster", false);
  }

  @Override
  public void read(final InputCapsule capsule) throws IOException {
    super.read(capsule);
    _color.set(capsule.readSavable("diffuse", (ColorRGBA) Light.DEFAULT_COLOR));
    _intensity = capsule.readFloat("intensity", Light.DEFAULT_INTENSITY);
    _enabled = capsule.readBoolean("enabled", false);
    _shadowCaster = capsule.readBoolean("shadowCaster", false);
  }

  @Override
  public Class<? extends Light> getClassTag() { return this.getClass(); }
}
