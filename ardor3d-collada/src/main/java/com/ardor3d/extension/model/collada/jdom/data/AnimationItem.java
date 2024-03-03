/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.model.collada.jdom.data;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import com.ardor3d.annotation.SavableFactory;
import com.ardor3d.extension.animation.skeletal.clip.AnimationClip;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;

@SavableFactory(factoryMethod = "initSavable")
public class AnimationItem implements Savable {
  private final String _name;
  private final List<AnimationItem> _children = new ArrayList<>();
  private AnimationClip _animationClip;

  public AnimationItem(final String name) {
    _name = name;
  }

  public AnimationClip getAnimationClip() { return _animationClip; }

  public void setAnimationClip(final AnimationClip animationClip) { _animationClip = animationClip; }

  public String getName() { return _name; }

  public List<AnimationItem> getChildren() { return _children; }

  @Override
  public String toString() {
    return "AnimationItem [name=" + _name + (_animationClip != null ? ", " + _animationClip.toString() : "") + "]";
  }

  // /////////////////
  // Methods for Savable
  // /////////////////

  @Override
  public Class<? extends AnimationItem> getClassTag() { return this.getClass(); }

  @Override
  public void read(final InputCapsule capsule) throws IOException {
    final String name = capsule.readString("name", "");
    try {
      final Field field1 = AnimationClip.class.getDeclaredField("_name");
      field1.setAccessible(true);
      field1.set(this, name);
    } catch (final Exception e) {
      e.printStackTrace();
    }
    _children.clear();
    _children.addAll(capsule.readSavableList("children", new ArrayList<>()));
    _animationClip = capsule.readSavable("animationClip", null);
  }

  @Override
  public void write(final OutputCapsule capsule) throws IOException {
    capsule.write(_name, "name", null);
    capsule.writeSavableList(_children, "children", null);
    capsule.write(_animationClip, "animationClip", null);
  }

  public static AnimationItem initSavable() {
    return new AnimationItem();
  }

  private AnimationItem() {
    this(null);
  }
}
