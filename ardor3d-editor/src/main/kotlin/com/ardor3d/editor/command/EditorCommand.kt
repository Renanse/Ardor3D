/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.editor.command

import com.ardor3d.scenegraph.Node
import com.ardor3d.scenegraph.Spatial

/**
 * A reversible editor operation. All scene mutations made through the editor UI go through
 * commands so they participate in undo/redo.
 */
interface EditorCommand {
    /** Human-readable name, shown in Edit > Undo/Redo. */
    val name: String

    /** True if this command changes scene structure (attach/detach/rename) rather than properties. */
    val affectsStructure: Boolean get() = false

    fun execute()

    fun undo()

    /**
     * Attempts to absorb [next] into this command so continuous gestures (slider drags, typing)
     * undo as a single step. Returns true if absorbed.
     */
    fun mergeWith(next: EditorCommand): Boolean = false
}

/**
 * Generic command that applies a value through a setter. Commands with the same non-null
 * [mergeKey] coalesce while the owning [CommandStack]'s merge window is open, keeping the
 * original old value and the latest new value.
 */
class SetterCommand<T>(
    override val name: String,
    private val oldValue: T,
    private var newValue: T,
    private val mergeKey: String? = null,
    override val affectsStructure: Boolean = false,
    private val setter: (T) -> Unit
) : EditorCommand {

    override fun execute() = setter(newValue)

    override fun undo() = setter(oldValue)

    override fun mergeWith(next: EditorCommand): Boolean {
        if (mergeKey == null || next !is SetterCommand<*> || next.mergeKey != mergeKey) {
            return false
        }
        @Suppress("UNCHECKED_CAST")
        newValue = next.newValue as T
        return true
    }
}

/**
 * Attaches [child] to [parent] on execute and detaches it on undo. The optional callbacks let
 * callers keep selection state in sync.
 */
class AttachChildCommand(
    private val parent: Node,
    private val child: Spatial,
    override val name: String = "Add ${child.name ?: "object"}",
    private val onExecuted: () -> Unit = {},
    private val onUndone: () -> Unit = {}
) : EditorCommand {

    override val affectsStructure: Boolean get() = true

    override fun execute() {
        parent.attachChild(child)
        onExecuted()
    }

    override fun undo() {
        parent.detachChild(child)
        onUndone()
    }
}
