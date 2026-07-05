/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.editor

import com.ardor3d.editor.menu.ShapeType
import com.ardor3d.scenegraph.Spatial

/**
 * Undoable scene operations exposed to the UI panels. Implemented by the scene owner
 * (EditorScene) and passed to menus/panels so they stay decoupled from scene internals.
 */
class SceneOperations(
    val addShape: (ShapeType) -> Unit,
    val deleteSpatial: (Spatial) -> Unit,
    val duplicateSpatial: (Spatial) -> Unit,
    val renameSpatial: (Spatial, String) -> Unit,
    val createEmpty: () -> Unit
)
