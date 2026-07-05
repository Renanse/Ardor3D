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

/**
 * Undo/redo history. Not thread safe; the editor runs all mutations on the AWT EDT.
 *
 * @param limit maximum number of undo steps kept; the oldest step is dropped beyond this.
 * @param onCommandApplied called after any command is executed, undone, redone or merged, with
 *        the command in question - used by the editor to refresh UI state.
 */
class CommandStack(
    private val limit: Int = 100,
    private val onCommandApplied: (EditorCommand) -> Unit = {}
) {
    private val undoStack = ArrayDeque<EditorCommand>()
    private val redoStack = ArrayDeque<EditorCommand>()
    private var mergeSealed = true

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()
    val undoName: String? get() = undoStack.lastOrNull()?.name
    val redoName: String? get() = redoStack.lastOrNull()?.name
    val undoCount: Int get() = undoStack.size

    /**
     * Executes the command and records it in the history.
     */
    fun execute(command: EditorCommand) {
        command.execute()
        record(command)
    }

    /**
     * Records a command whose effect has already been applied (e.g. a completed gizmo drag).
     */
    fun record(command: EditorCommand) {
        redoStack.clear()
        val top = undoStack.lastOrNull()
        if (!mergeSealed && top != null && top.mergeWith(command)) {
            onCommandApplied(command)
            return
        }
        undoStack.addLast(command)
        if (undoStack.size > limit) {
            undoStack.removeFirst()
        }
        mergeSealed = false
        onCommandApplied(command)
    }

    /**
     * Marks a gesture boundary: the next recorded command will not merge with the current top
     * of the undo stack. Called on focus loss / slider release.
     */
    fun sealMerge() {
        mergeSealed = true
    }

    fun undo(): Boolean {
        val command = undoStack.removeLastOrNull() ?: return false
        command.undo()
        redoStack.addLast(command)
        mergeSealed = true
        onCommandApplied(command)
        return true
    }

    fun redo(): Boolean {
        val command = redoStack.removeLastOrNull() ?: return false
        command.execute()
        undoStack.addLast(command)
        mergeSealed = true
        onCommandApplied(command)
        return true
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
        mergeSealed = true
    }
}
