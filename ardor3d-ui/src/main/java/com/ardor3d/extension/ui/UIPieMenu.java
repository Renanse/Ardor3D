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

import java.util.ArrayList;
import java.util.List;

import com.ardor3d.math.Rectangle2;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.util.MathUtils;
import com.ardor3d.scenegraph.Spatial;

/**
 * A special frame meant to display menu items.
 */
public class UIPieMenu extends UIPopupMenu implements IPopOver {

  public static final int DEFAULT_INNER_RADIUS = 50;

  private int _innerRadius, _outerRadius;
  private double _sliceRadians = 1.0, _totalArcLength = MathUtils.TWO_PI, _startAngle = 0.0;

  private boolean _menuDirty = true;

  private UIPieMenuItem _center;

  public UIPieMenu(final UIHud hud) {
    this(hud, UIPieMenu.DEFAULT_INNER_RADIUS, Math.min(hud.getWidth() / 2, hud.getHeight() / 2));
  }

  public UIPieMenu(final UIHud hud, final int innerRadius) {
    this(hud, innerRadius, Math.min(hud.getWidth() / 2, hud.getHeight() / 2));
  }

  public UIPieMenu(final UIHud hud, final int innerRadius, final int outerRadius) {
    super();
    _innerRadius = innerRadius;
    _outerRadius = outerRadius;
    setHud(hud);
    applySkin();
    setDoClip(false);
  }

  @Override
  public void showAt(final int x, final int y) {
    setHudXY(x, y);
    updateGeometricState(0, true);
  }

  @Override
  public void setHud(final UIHud hud) {
    _parent = hud;
    attachedToHud();
  }

  public int getInnerRadius() { return _innerRadius; }

  public int getOuterRadius() { return _outerRadius; }

  public void setInnerRadius(final int radius) {
    _menuDirty = true;
    _innerRadius = radius;
  }

  public void setOuterRadius(final int radius) {
    _menuDirty = true;
    _outerRadius = radius;
  }

  public double getTotalArcLength() { return _totalArcLength; }

  public void setTotalArcLength(final double radians) {
    _menuDirty = true;
    _totalArcLength = radians;
  }

  public double getStartAngle() { return _startAngle; }

  public void setStartAngle(final double radians) {
    _menuDirty = true;
    _startAngle = radians;
  }

  public double getSliceRadians() { return _sliceRadians; }

  @Override
  public void addItem(final UIMenuItem item) {
    _menuDirty = true;
    super.addItem(item);
  }

  @Override
  public void removeItem(final UIMenuItem item) {
    _menuDirty = true;
    super.removeItem(item);
  }

  public UIPieMenuItem getCenterItem() { return _center; }

  public void setCenterItem(final UIPieMenuItem item) {
    _menuDirty = true;
    if (_center != null) {
      remove(_center);
    }
    if (item != null) {
      add(item);
    }
    _center = item;
  }

  public void clearCenterItem() {
    setCenterItem(null);
  }

  @Override
  public void clearItems() {
    _menuDirty = true;
    removeAllComponents();
  }

  @Override
  public UIComponent getUIComponent(final int hudX, final int hudY) {
    final Vector3 vec = new Vector3(hudX - getHudX(), hudY - getHudY(), 0);

    // check we are inside the pie
    final double distSq = vec.lengthSquared();
    if (distSq < _innerRadius * _innerRadius) {
      return _center;
    }
    if (distSq > _outerRadius * _outerRadius) {
      return null;
    }

    vec.normalizeLocal();

    getRotation().applyPre(vec, vec);

    double r = MathUtils.HALF_PI - Math.atan2(vec.getY(), vec.getX()) - _startAngle;

    // move into range [0, 2pi]
    r = (r % MathUtils.TWO_PI + MathUtils.TWO_PI) % MathUtils.TWO_PI;

    int index = (int) (r / _sliceRadians);
    for (int i = 0; i < getNumberOfChildren(); i++) {
      final Spatial s = getChild(i);
      if (s == _center) {
        continue;
      }
      if (s instanceof UIComponent) {
        if (index == 0) {
          return (UIComponent) s;
        }
        index--;
      }
    }
    return null;
  }

  @Override
  public void layout() {
    if (!_menuDirty) {
      return;
    }

    final List<Spatial> content = getChildren();
    if (content == null) {
      return;
    }

    // gather our components
    final List<UIComponent> comps = new ArrayList<>();
    final Rectangle2 storeA = Rectangle2.fetchTempInstance();
    for (int i = 0; i < content.size(); i++) {
      final Spatial spat = content.get(i);
      if (spat instanceof UIComponent) {
        final UIComponent comp = (UIComponent) spat;
        final Rectangle2 minRect = comp.getRelativeMinComponentBounds(storeA);
        comp.fitComponentIn(minRect.getWidth(), minRect.getHeight());
        if (comp == _center) {
          final Rectangle2 rect = comp.getRelativeComponentBounds(storeA);
          comp.setLocalXY(-rect.getWidth() / 2, -rect.getHeight() / 2);
          continue;
        }
        comps.add(comp);
      }
    }

    // if we don't have components to layout, exit
    if (comps.isEmpty()) {
      Rectangle2.releaseTempInstance(storeA);
      return;
    }

    // Figure out slice size
    _sliceRadians = _totalArcLength / comps.size();
    final int radius = (_innerRadius + _outerRadius) / 2;
    double position = _startAngle + _sliceRadians / 2.0;
    for (int i = 0, maxI = comps.size(); i < maxI; i++) {
      final UIComponent comp = comps.get(i);

      final Rectangle2 rect = comp.getRelativeComponentBounds(storeA);
      final int x = (int) MathUtils.round(radius * Math.sin(position));
      final int y = (int) MathUtils.round(radius * Math.cos(position));

      comp.setLocalXY(x - rect.getWidth() / 2, y - rect.getHeight() / 2);

      // step forward
      position += _sliceRadians;
    }

    Rectangle2.releaseTempInstance(storeA);
    _menuDirty = false;
  }

  @Override
  public void updateMinimumSizeFromContents() {
    setLayoutMinimumContentSize(_outerRadius * 2, _outerRadius * 2);
  }

  public int getSliceIndex(final UIPieMenuItem item) {
    final List<Spatial> content = getChildren();
    if (content == null) {
      return -1;
    }

    int x = 0;
    for (int i = 0; i < content.size(); i++) {
      final Spatial spat = content.get(i);
      if (spat == _center) {
        continue;
      }
      if (spat == item) {
        return x;
      }
      if (spat instanceof UIPieMenuItem) {
        x++;
      }
    }

    return -1;
  }
}
