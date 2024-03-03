/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.light;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

import com.ardor3d.bounding.BoundingVolume;
import com.ardor3d.image.Texture;
import com.ardor3d.light.Light.Type;
import com.ardor3d.light.shadow.DirectionalShadowData;
import com.ardor3d.math.Plane;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderPhase;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.material.IUniformSupplier;
import com.ardor3d.renderer.material.uniform.Ardor3dStateProperty;
import com.ardor3d.renderer.material.uniform.UniformRef;
import com.ardor3d.renderer.material.uniform.UniformSource;
import com.ardor3d.renderer.material.uniform.UniformType;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.SceneIndexer;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.util.Ardor3dException;

public class LightManager implements IUniformSupplier {

  public static final String DefaultPropertyKey = "lightProps";

  public static int FIRST_SHADOW_INDEX = 8;
  public static int MAX_LIGHTS = 8;

  protected LightComparator lightComparator = new LightComparator();
  protected List<WeakReference<Light>> _lightRefs = new ArrayList<>();

  protected final List<UniformRef> _cachedUniforms = new ArrayList<>();

  public LightManager() {
    for (int i = 0; i < LightManager.MAX_LIGHTS; i++) {
      _cachedUniforms.add(new UniformRef("lights[" + i + "]", UniformType.UniformSupplier, UniformSource.Ardor3dState,
          Ardor3dStateProperty.Light, i, null));
      _cachedUniforms.add(new UniformRef("spotShadowMaps[" + i + "]", UniformType.Int1, UniformSource.Ardor3dState,
          Ardor3dStateProperty.ShadowTexture, i, null));
      _cachedUniforms.add(new UniformRef("pointShadowMaps[" + i + "]", UniformType.Int1, UniformSource.Ardor3dState,
          Ardor3dStateProperty.ShadowTexture, i, null));
    }

    _cachedUniforms.add(new UniformRef("globalAmbient", UniformType.Float3, UniformSource.Ardor3dState,
        Ardor3dStateProperty.GlobalAmbientLight));

    _cachedUniforms.add(new UniformRef("dirShadowLight", UniformType.UniformSupplier, UniformSource.Ardor3dState,
        Ardor3dStateProperty.Light, -1, null));
    _cachedUniforms.add(new UniformRef("dirShadowMap", UniformType.Int1, UniformSource.Ardor3dState,
        Ardor3dStateProperty.ShadowTexture, -1, null));
    for (int i = 0; i < DirectionalShadowData.MAX_SPLITS; i++) {
      final int index = i;
      _cachedUniforms.add(new UniformRef("splitDistances[" + i + "]", UniformType.Float1, UniformSource.Supplier,
          (Supplier<Float>) () -> getDirectionalCSMSplit(index)));
    }
  }

  protected float getDirectionalCSMSplit(final int index) {
    final Light light = getCurrentLight(-1);
    if (light == null || light.getType() != Type.Directional) {
      return Float.MAX_VALUE;
    }

    final double distance = ((DirectionalLight) light).getShadowData().getSplit(index);
    return distance == Double.MAX_VALUE ? Float.MAX_VALUE : (float) distance;
  }

  public void sortLightsFor(final Mesh mesh) {
    lightComparator.setBoundingVolume(mesh.getWorldBound());
    Collections.sort(_lightRefs, lightComparator);
  }

  public Light getCurrentLight(final int index) {
    final Light light0 = _lightRefs.isEmpty() ? null : _lightRefs.get(0).get();
    final boolean hasDSM = light0 != null && light0.isShadowCaster() && light0.getType() == Type.Directional;
    if (index == -1) {
      return hasDSM ? light0 : null;
    }

    final int newIndex = (hasDSM ? index + 1 : index);
    if (_lightRefs.size() <= newIndex) {
      return null;
    }
    final var light = _lightRefs.get(newIndex).get();
    if (light == null || !light.isEnabled()) {
      return null;
    }
    return light;
  }

  public Texture getCurrentShadowTexture(final int index) {
    final var light = getCurrentLight(index);
    return light != null ? light.getShadowData().getTexture() : null;
  }

  public void addLights(final Spatial spat) {
    if (spat instanceof Light) {
      _lightRefs.add(new WeakReference<>((Light) spat));
    } else if (spat instanceof Node) {
      final var node = (Node) spat;
      final var children = node.getChildren();
      for (int i = 0, maxI = children.size(); i < maxI; i++) {
        addLights(children.get(i));
      }
    }
  }

  public void removeLights(final Spatial spat) {
    if (spat instanceof Light) {
      for (int i = _lightRefs.size(); --i >= 0;) {
        final var ref = _lightRefs.get(i);
        final var light = ref.get();
        if (light == spat) {
          _lightRefs.remove(i);
          return;
        }
      }
    } else if (spat instanceof Node) {
      final var children = ((Node) spat).getChildren();
      for (int i = 0, maxI = children.size(); i < maxI; i++) {
        removeLights(children.get(i));
      }
    }
  }

  public void cleanLights() {
    for (int i = _lightRefs.size(); --i >= 0;) {
      final var ref = _lightRefs.get(i);
      if (ref.get() == null) {
        _lightRefs.remove(i);
      }
    }
  }

  protected static class LightComparator implements Comparator<WeakReference<Light>> {
    private BoundingVolume _bv;

    public void setBoundingVolume(final BoundingVolume bv) { _bv = bv; }

    @Override
    public int compare(final WeakReference<Light> l1, final WeakReference<Light> l2) {
      final double v1 = getValueFor(l1, _bv);
      final double v2 = getValueFor(l2, _bv);
      final double cmp = v1 - v2;
      if (0 > cmp) {
        return 1;
      } else if (0 < cmp) {
        return -1;
      }
      return 0;
    }
  }

  protected static double getValueFor(final WeakReference<Light> lref, final BoundingVolume val) {
    final Light l = lref.get();
    if (l == null || !l.isEnabled()) {
      return Double.NEGATIVE_INFINITY;
    } else if (l.getType() == Light.Type.Directional) {
      return getColorValue(l) + (l.isShadowCaster() ? 99999.0 : 0.0);
    } else if (l.getType() == Light.Type.Point) {
      return getValueFor((PointLight) l, val);
    } else if (l.getType() == Light.Type.Spot) {
      return getValueFor((SpotLight) l, val);
    }
    // If a new type of light was added and this was not updated throw exception.
    throw new Ardor3dException("Unhandled light type: " + l.getType());
  }

  protected static double getValueFor(final PointLight l, final BoundingVolume val) {
    if (val == null) {
      return 0;
    }

    final ReadOnlyVector3 location = l.getWorldTranslation();
    // TODO: filter out lights that would not affect this BV
    final double dist = val.distanceTo(location);

    final double color = getColorValue(l);
    final double amlat = l.getConstant() + l.getLinear() * dist + l.getQuadratic() * dist * dist;

    return color / amlat;
  }

  protected static double getValueFor(final SpotLight l, final BoundingVolume val) {
    if (val == null) {
      return 0;
    }
    final ReadOnlyVector3 direction = l.getWorldDirection();
    final ReadOnlyVector3 location = l.getWorldTranslation();
    // TODO: filter out lights that would not affect this BV
    // direction is copied into Plane, not reused.
    final Plane p = new Plane(direction, direction.dot(location));
    if (val.whichSide(p) != Plane.Side.Inside) {
      return getValueFor((PointLight) l, val);
    }

    return 0;
  }

  protected static double getColorValue(final Light l) {
    return l.getIntensity() * strength(l.getColor());
  }

  protected static double strength(final ReadOnlyColorRGBA color) {
    return Math.sqrt(
        color.getRed() * color.getRed() + color.getGreen() * color.getGreen() + color.getBlue() * color.getBlue());
  }

  public void renderShadowMaps(final Renderer renderer, final SceneIndexer indexer) {
    // For each of our lights
    final var context = ContextManager.getCurrentContext();
    final var oldPhase = context.getRenderPhase();
    try {
      context.setRenderPhase(RenderPhase.ShadowTexture);
      for (int i = _lightRefs.size(); --i >= 0;) {
        final var lref = _lightRefs.get(i);
        final var light = lref.get();

        // if light has expired, is disabled or is not a caster, ignore
        if (light == null || !light.isEnabled() || !light.isShadowCaster()) {
          continue;
        }

        light.getShadowData().updateShadows(renderer, indexer);
      }
    } finally {
      context.setRenderPhase(oldPhase);
    }
  }

  @Override
  public void applyDefaultUniformValues() {
    // Nothing to do here
  }

  @Override
  public List<UniformRef> getUniforms() { return _cachedUniforms; }
}
