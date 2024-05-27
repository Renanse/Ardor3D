/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.scenegraph.visitor;

import com.ardor3d.renderer.material.IShaderUtils;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Spatial;

/**
 * <code>DeleteShaderProgramsVisitor</code> is a visitor that will clear all uploaded
 * shader programs associated with RenderMaterials in a given part of the scene graph.
 */
public class DeleteShaderProgramsVisitor implements Visitor {
  final IShaderUtils _utils;

  public DeleteShaderProgramsVisitor(final IShaderUtils utils) {
    _utils = utils;
  }

  @Override
  public void visit(final Spatial spatial) {
    if (spatial instanceof Mesh mesh) {
      var material = mesh.getRenderMaterial();
      if (material == null) return;
      material.getTechniques().forEach(technique ->
          technique.getPasses().forEach(pass ->
              pass.cleanProgram(_utils)));
    }
  }
}
