/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.model.obj;

import java.util.ArrayList;
import java.util.List;

import com.ardor3d.math.Vector3;

public class ObjDataStore {
  private final List<Vector3> _vertices = new ArrayList<>();
  private final List<Vector3> _normals = new ArrayList<>();
  private final List<Vector3> _generatedNormals = new ArrayList<>();
  private final List<Vector3> _uvs = new ArrayList<>();

  public List<Vector3> getVertices() { return _vertices; }

  public List<Vector3> getNormals() { return _normals; }

  public List<Vector3> getGeneratedNormals() { return _generatedNormals; }

  public List<Vector3> getUvs() { return _uvs; }
}
