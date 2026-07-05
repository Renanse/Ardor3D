/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.editor.interact

import com.ardor3d.editor.EditorState
import com.ardor3d.editor.command.SetterCommand
import com.ardor3d.extension.interact.InteractManager
import com.ardor3d.extension.interact.filter.UpdateFilterAdapter
import com.ardor3d.extension.interact.widget.AbstractInteractWidget
import com.ardor3d.input.mouse.MouseState
import com.ardor3d.math.Transform
import com.ardor3d.scenegraph.Spatial

/**
 * Records each completed gizmo drag as a single undoable transform command. The widget applies
 * the transform live while dragging; on drag end we capture the before/after pair.
 */
class GizmoUndoFilter(private val editorState: EditorState) : UpdateFilterAdapter() {

    private var target: Spatial? = null
    private var startTransform: Transform? = null

    override fun beginDrag(manager: InteractManager, widget: AbstractInteractWidget, state: MouseState?) {
        target = manager.spatialTarget
        startTransform = target?.let { Transform().set(it.transform) }
    }

    override fun endDrag(manager: InteractManager, widget: AbstractInteractWidget, state: MouseState?) {
        val spatial = target
        val start = startTransform
        target = null
        startTransform = null
        if (spatial == null || start == null || start == spatial.transform) {
            return
        }
        val end = Transform().set(spatial.transform)
        editorState.record(
            SetterCommand(
                name = "Transform ${spatial.name ?: "object"}",
                oldValue = start,
                newValue = end,
                setter = { spatial.transform = it }
            )
        )
        editorState.sealUndoMerge()
    }
}
