/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.ui.backdrop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.ardor3d.extension.ui.UIComponent;
import com.ardor3d.extension.ui.util.Insets;
import com.ardor3d.extension.ui.util.SubTexUtil;
import com.ardor3d.extension.ui.util.TransformedSubTex;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.Renderer;

/**
 * This backdrop paints one or more transformable, ordered images on a colored plane behind the
 * component.
 */
public class MultiImageBackdrop extends SolidBackdrop {

  /** The image(s) to draw. */
  private final List<TransformedSubTex> _images = new ArrayList<>();

  /**
   * Construct this back drop, using the default, no alpha backdrop color.
   */
  public MultiImageBackdrop() {
    this(ColorRGBA.BLACK_NO_ALPHA);
  }

  /**
   * Construct this back drop, using the given backdrop color.
   *
   * @param backDropColor
   *          the color of the backdrop
   */
  public MultiImageBackdrop(final ReadOnlyColorRGBA backDropColor) {
    super(backDropColor);
  }

  public void addImage(final TransformedSubTex entry) {
    _images.add(entry);
  }

  public boolean removeImage(final TransformedSubTex entry) {
    return _images.remove(entry);
  }

  public List<TransformedSubTex> getImages() { return _images; }

  @Override
  public void draw(final Renderer renderer, final UIComponent comp) {
    super.draw(renderer, comp);

    if (_images.size() > 1) {
      Collections.sort(_images);
    }

    final double bgwidth = UIBackdrop.getBackdropWidth(comp);
    final double bgheight = UIBackdrop.getBackdropHeight(comp);

    final double[] store = new double[2];
    for (final TransformedSubTex entry : _images) {

      MultiImageBackdrop.getDimensions(entry, comp, bgwidth, bgheight, store);
      final Insets margin = comp.getMargin() != null ? comp.getMargin() : Insets.EMPTY;
      final Insets border = comp.getBorder() != null ? comp.getBorder() : Insets.EMPTY;

      store[0] += margin.getLeft() + border.getLeft();
      store[1] += margin.getBottom() + border.getBottom();

      SubTexUtil.drawTransformedSubTex(renderer, entry, (int) Math.round(store[0]), (int) Math.round(store[1]),
          entry.getWidth(), entry.getHeight(), false, comp.getWorldTransform());
    }
  }

  public static void getDimensions(final TransformedSubTex entry, final UIComponent comp, final double bgwidth,
      final double bgheight, final double[] store) {

    switch (entry.getAlignment()) {
      case TOP:
      case MIDDLE:
      case BOTTOM:
        store[0] = bgwidth / 2;
        break;
      case TOP_RIGHT:
      case RIGHT:
      case BOTTOM_RIGHT:
        store[0] = bgwidth;
        break;
      case TOP_LEFT:
      case LEFT:
      case BOTTOM_LEFT:
        store[0] = 0;
    }

    switch (entry.getAlignment()) {
      case TOP_LEFT:
      case TOP:
      case TOP_RIGHT:
        store[1] = bgheight;
        break;
      case LEFT:
      case MIDDLE:
      case RIGHT:
        store[1] = bgheight / 2;
        break;
      case BOTTOM_LEFT:
      case BOTTOM:
      case BOTTOM_RIGHT:
        store[1] = 0;
    }
  }
}
