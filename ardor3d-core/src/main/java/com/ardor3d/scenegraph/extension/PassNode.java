/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.scenegraph.extension;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;

public class PassNode extends Node {

  private List<PassNodeState> _passNodeStates = new ArrayList<>();

  public PassNode(final String name) {
    super(name);
  }

  public PassNode() {
    super();
  }

  @Override
  public void draw(final Renderer r) {
    if (_children == null) {
      return;
    }

    final RenderContext context = ContextManager.getCurrentContext();
    r.getQueue().pushBuckets();
    for (final PassNodeState pass : _passNodeStates) {
      if (!pass.isEnabled()) {
        continue;
      }

      pass.applyPassNodeStates(context);

      Spatial child;
      for (int i = 0, cSize = _children.size(); i < cSize; i++) {
        child = _children.get(i);
        if (child != null) {
          child.onDraw(r);
        }
      }
      r.renderBuckets();

      context.popEnforcedStates();
    }
    r.getQueue().popBuckets();
  }

  public void addPass(final PassNodeState toAdd) {
    _passNodeStates.add(toAdd);
  }

  public void insertPass(final PassNodeState toAdd, final int index) {
    _passNodeStates.add(index, toAdd);
  }

  public boolean containsPass(final PassNodeState s) {
    return _passNodeStates.contains(s);
  }

  public boolean removePass(final PassNodeState toRemove) {
    return _passNodeStates.remove(toRemove);
  }

  public PassNodeState getPass(final int index) {
    return _passNodeStates.get(index);
  }

  public int nrPasses() {
    return _passNodeStates.size();
  }

  public void clearAll() {
    _passNodeStates.clear();
  }

  @Override
  public void write(final OutputCapsule capsule) throws IOException {
    super.write(capsule);
    capsule.writeSavableList(_passNodeStates, "passNodeStates", null);
  }

  @Override
  public void read(final InputCapsule capsule) throws IOException {
    super.read(capsule);
    _passNodeStates = capsule.readSavableList("passNodeStates", null);
  }
}
