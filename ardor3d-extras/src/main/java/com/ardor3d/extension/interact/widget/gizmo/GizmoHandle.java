/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.interact.widget.gizmo;

import java.util.ArrayList;
import java.util.List;

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;

/**
 * One interactive part of a gizmo: a subtree of meshes with a shared identity, base color, drag
 * axis and fade rule. The owning gizmo colors the whole part as a unit for hover highlighting and
 * view-angle fading.
 */
public class GizmoHandle {

  /** How this handle fades as the camera's view direction changes. */
  public enum FadeMode {
    /** Never fades. */
    None,
    /** Fades out as its axis approaches the view direction (axis drag is ill-conditioned there). */
    Axis,
    /** Fades out as its plane approaches edge-on to the camera (its axis is the plane normal). */
    Plane
  }

  protected final GizmoPart _part;
  protected final Node _root;
  protected final ColorRGBA _baseColor = new ColorRGBA();
  protected final Vector3 _axis = new Vector3();
  protected final FadeMode _fadeMode;
  protected final List<Mesh> _meshes = new ArrayList<>();

  /** Most recently calculated view-angle fade, applied as an alpha multiplier. */
  protected double _fadeAlpha = 1.0;

  /**
   * @param part
   *          identity of this handle within its gizmo.
   * @param root
   *          subtree holding the handle's meshes (visuals and any invisible pick proxies).
   * @param baseColor
   *          un-highlighted color; its alpha is the handle's base opacity.
   * @param axis
   *          the axis this handle drags along (or plane normal, or zero if unused) in the gizmo's
   *          local space.
   * @param fadeMode
   *          how this handle fades with the view angle.
   */
  public GizmoHandle(final GizmoPart part, final Node root, final ReadOnlyColorRGBA baseColor,
      final ReadOnlyVector3 axis, final FadeMode fadeMode) {
    _part = part;
    _root = root;
    _baseColor.set(baseColor);
    _axis.set(axis);
    _fadeMode = fadeMode;

    collectMeshes(root);
  }

  protected void collectMeshes(final Spatial spatial) {
    if (spatial instanceof Mesh mesh) {
      _meshes.add(mesh);
    } else if (spatial instanceof Node node) {
      for (int i = 0; i < node.getNumberOfChildren(); i++) {
        collectMeshes(node.getChild(i));
      }
    }
  }

  /**
   * Set the given color on all of this handle's meshes, replacing the alpha with the given value.
   */
  public void applyColor(final ReadOnlyColorRGBA rgb, final float alpha) {
    for (int i = _meshes.size(); --i >= 0;) {
      final Mesh mesh = _meshes.get(i);
      mesh.setDefaultColor(rgb.getRed(), rgb.getGreen(), rgb.getBlue(), alpha);
    }
  }

  public boolean contains(final Spatial spatial) {
    return spatial instanceof Mesh && _meshes.contains(spatial);
  }

  public GizmoPart getPart() { return _part; }

  public Node getRoot() { return _root; }

  public ColorRGBA getBaseColor() { return _baseColor; }

  public ReadOnlyVector3 getAxis() { return _axis; }

  public FadeMode getFadeMode() { return _fadeMode; }

  public double getFadeAlpha() { return _fadeAlpha; }

  public void setFadeAlpha(final double alpha) { _fadeAlpha = alpha; }

  public List<Mesh> getMeshes() { return _meshes; }
}
