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

import com.ardor3d.math.Transform
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
 * Groups several commands into one undo step: executes them in order and undoes them in
 * reverse. Used for operations over a multi-selection (delete, duplicate). The optional
 * callbacks run after all children and let callers keep selection state in sync.
 */
class CompositeCommand(
    override val name: String,
    private val commands: List<EditorCommand>,
    private val onExecuted: () -> Unit = {},
    private val onUndone: () -> Unit = {}
) : EditorCommand {

    override val affectsStructure: Boolean = commands.any { it.affectsStructure }

    override fun execute() {
        commands.forEach { it.execute() }
        onExecuted()
    }

    override fun undo() {
        commands.asReversed().forEach { it.undo() }
        onUndone()
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

/**
 * Moves [child] from its current parent under [newParent] (appended as the last child). By
 * default the child's world transform is preserved by rewriting its local transform against the
 * new parent; pass [keepWorldTransform] = false to keep the local transform instead. Undo
 * restores the original parent, child index and local transform.
 *
 * World transforms must be current when the command executes (the editor updates them every
 * frame). Callers should validate with [isValidReparent] first; an invalid move throws.
 */
class ReparentCommand(
    private val child: Spatial,
    private val newParent: Node,
    private val keepWorldTransform: Boolean = true,
    override val name: String = "Reparent ${child.name ?: "object"}"
) : EditorCommand {

    override val affectsStructure: Boolean get() = true

    private var oldParent: Node? = null
    private var oldIndex = -1
    private val oldLocal = Transform()

    override fun execute() {
        val parent = requireNotNull(child.parent) { "cannot reparent the scene root" }
        oldParent = parent
        oldIndex = parent.children.indexOf(child)
        oldLocal.set(child.transform)

        val newLocal = if (keepWorldTransform) {
            newParent.worldTransform.invert(Transform()).multiply(child.worldTransform, Transform())
        } else {
            null
        }
        // attachChild detaches from the old parent (and rejects cycles) internally
        newParent.attachChild(child)
        newLocal?.let { child.transform = it }
    }

    override fun undo() {
        val parent = oldParent ?: return
        parent.attachChildAt(child, oldIndex)
        child.transform = oldLocal
    }

    companion object {
        /**
         * True when [child] can be moved under [newParent]: the child must not be the scene
         * root, and the target must not be the child itself, its current parent (a no-op) or
         * one of its own descendants (a cycle).
         */
        fun isValidReparent(child: Spatial, newParent: Node): Boolean {
            if (child.parent == null || newParent === child || newParent === child.parent) {
                return false
            }
            return !(child is Node && newParent.hasAncestor(child))
        }
    }
}

/**
 * Detaches [child] from [parent] on execute and re-attaches it at its original child index on
 * undo, so undoing a delete restores hierarchy order.
 */
class DetachChildCommand(
    private val parent: Node,
    private val child: Spatial,
    override val name: String = "Delete ${child.name ?: "object"}",
    private val onExecuted: () -> Unit = {},
    private val onUndone: () -> Unit = {}
) : EditorCommand {

    private var index = -1

    override val affectsStructure: Boolean get() = true

    override fun execute() {
        index = parent.children.indexOf(child)
        parent.detachChild(child)
        onExecuted()
    }

    override fun undo() {
        if (index in 0..parent.numberOfChildren) {
            parent.attachChildAt(child, index)
        } else {
            parent.attachChild(child)
        }
        onUndone()
    }
}
