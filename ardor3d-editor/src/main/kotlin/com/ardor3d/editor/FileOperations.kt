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

/**
 * Document lifecycle operations exposed to the menu bar. Implemented in NativeEditorApp where
 * the owning window (for file dialogs) and the scene are both in scope.
 */
class FileOperations(
    val newScene: () -> Unit,
    val openScene: () -> Unit,
    val saveScene: () -> Unit,
    val saveSceneAs: () -> Unit,
    val importModel: () -> Unit,
    val exit: () -> Unit
)
