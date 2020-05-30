/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.ui.backdrop;

import com.ardor3d.extension.ui.UIComponent;
import com.ardor3d.extension.ui.UIPieMenu;
import com.ardor3d.extension.ui.util.Insets;
import com.ardor3d.extension.ui.util.UIDisk;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Transform;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.BlendState.DestinationFunction;
import com.ardor3d.renderer.state.BlendState.SourceFunction;
import com.ardor3d.renderer.state.RenderState.StateType;

/**
 * This backdrop paints a solid disk of color behind a UI component.
 */
public class SolidDiskBackdrop extends UIBackdrop {

  /** The color to draw */
  protected final ColorRGBA _color = new ColorRGBA(ColorRGBA.GRAY);
  /** The disk used across all disk backdrops to render with. */
  private static UIDisk _standin = SolidDiskBackdrop.createStandinDisk();
  static {
    SolidDiskBackdrop._standin.setRenderMaterial("ui/untextured/default_color.yaml");
  }

  /**
   * Construct this backdrop, using the given color.
   *
   * @param color
   *          the color of the backdrop
   */
  public SolidDiskBackdrop(final ReadOnlyColorRGBA color) {
    setColor(color);
  }

  /**
   * @return the color of this back drop.
   */
  public ReadOnlyColorRGBA getColor() { return _color; }

  /**
   * Set the color of this back drop.
   *
   * @param color
   *          the color to use
   */
  public void setColor(final ReadOnlyColorRGBA color) {
    if (color != null) {
      _color.set(color);
    }
  }

  @Override
  public void draw(final Renderer renderer, final UIComponent comp) {
    final float oldA = _color.getAlpha();
    if (oldA == 0) {
      // no need to draw.
      return;
    }

    _color.setAlpha(oldA * UIComponent.getCurrentOpacity());
    SolidDiskBackdrop._standin.setDefaultColor(_color);

    final Vector3 v = Vector3.fetchTempInstance();
    final Insets margin = comp.getMargin() != null ? comp.getMargin() : Insets.EMPTY;
    final Insets border = comp.getBorder() != null ? comp.getBorder() : Insets.EMPTY;
    v.set(margin.getLeft() + border.getLeft(), margin.getBottom() + border.getBottom(), 0);
    v.addLocal(comp.getContentWidth() / 2, comp.getContentHeight() / 2, 0);

    final Transform t = Transform.fetchTempInstance();
    t.set(comp.getWorldTransform());
    t.applyForwardVector(v);
    t.translate(v);
    Vector3.releaseTempInstance(v);

    SolidDiskBackdrop._standin.setWorldTransform(t);
    Transform.releaseTempInstance(t);

    double size = 0;
    if (comp instanceof UIPieMenu) {
      size = ((UIPieMenu) comp).getOuterRadius();
    } else {
      size = Math.max(UIBackdrop.getBackdropWidth(comp), UIBackdrop.getBackdropHeight(comp)) / 2;
    }

    SolidDiskBackdrop._standin.clearRenderState(StateType.Texture);
    SolidDiskBackdrop._standin.resetGeometry(size, 0, null);
    SolidDiskBackdrop._standin.render(renderer);

    _color.setAlpha(oldA);
  }

  public static UIDisk createStandinDisk() {
    final UIDisk disk = new UIDisk("standin", 60, 1, 0);

    final BlendState blend = new BlendState();
    blend.setBlendEnabled(true);
    blend.setSourceFunction(SourceFunction.SourceAlpha);
    blend.setDestinationFunction(DestinationFunction.OneMinusSourceAlpha);
    disk.setRenderState(blend);
    disk.updateWorldRenderStates(false);

    return disk;
  }
}
