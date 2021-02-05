/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.ui;

import com.ardor3d.extension.ui.event.ActionListener;
import com.ardor3d.extension.ui.util.SubTex;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.util.MathUtils;

/**
 *
 */
public class UIPieMenuItem extends UIMenuItem {

  protected int _subMenuSize = 0;

  public UIPieMenuItem(final String text) {
    this(text, null);
  }

  public UIPieMenuItem(final String text, final SubTex icon) {
    this(text, icon, true, null);
  }

  public UIPieMenuItem(final String text, final SubTex icon, final boolean closeMenuOnSelect,
    final ActionListener listener) {
    super(text, icon, closeMenuOnSelect, listener);
  }

  public UIPieMenuItem(final String text, final SubTex icon, final UIPieMenu subMenu, final int size) {
    super(text, icon, false, null);
    _subMenu = subMenu;
    _subMenuSize = size;
    addActionListener(event -> showSubMenu());
  }

  @Override
  protected void showSubMenu() {
    final UIHud hud = getHud();
    if (hud == null) {
      return;
    }

    boolean setup = false;
    final Vector3 showAt = new Vector3(getWorldTranslation());
    final UIPieMenu subMenu = (UIPieMenu) _subMenu;
    if (getParent() instanceof UIPieMenu) {
      final UIPieMenu pie = (UIPieMenu) getParent();
      if (pie.getCenterItem() != this) {
        subMenu.setOuterRadius(pie.getOuterRadius() + _subMenuSize);
        subMenu.setInnerRadius(pie.getOuterRadius());
        subMenu.setTotalArcLength(pie.getSliceRadians());
        subMenu.setStartAngle(pie.getSliceIndex(this) * pie.getSliceRadians() + pie.getStartAngle());
        showAt.set(pie.getWorldTranslation());
        hud.closePopupMenusAfter(pie);
        setup = true;
      }
    }
    if (!setup) {
      subMenu.setOuterRadius(_subMenuSize);
      subMenu.setInnerRadius(0);
      subMenu.setTotalArcLength(MathUtils.TWO_PI);
      subMenu.setStartAngle(0);
    }

    subMenu.updateMinimumSizeFromContents();
    subMenu.layout();

    hud.showSubPopupMenu(subMenu);
    subMenu.showAt((int) showAt.getX(), (int) showAt.getY());
  }

}
