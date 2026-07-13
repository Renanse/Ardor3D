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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ardor3d.editor.command.CommandStack
import com.ardor3d.editor.command.EditorCommand
import com.ardor3d.scenegraph.Node
import com.ardor3d.scenegraph.Spatial
import java.io.File

/**
 * Global editor state that manages selection, scene, and editor mode.
 */
class EditorState {
    // Scene root - the root of the scene graph being edited
    var sceneRoot: Node by mutableStateOf(createDefaultScene())
        private set

    // Currently selected spatials
    private val _selection = mutableStateListOf<Spatial>()
    val selection: List<Spatial> get() = _selection

    // Current transform mode
    var transformMode: TransformMode by mutableStateOf(TransformMode.TRANSLATE)

    // Render loop frame rate, updated by the scene for the status bar
    var framesPerSecond: Int by mutableStateOf(0)

    // Play mode: when true the viewport renders through a camera object instead of the editor
    // fly camera, with editor overlays and controls suspended. This is view state, not a
    // document edit - it never touches the undo history or the dirty flag.
    var playing: Boolean by mutableStateOf(false)
        private set

    // Name of the camera object play mode is rendering through, for the status bar.
    var playCameraName: String? by mutableStateOf(null)
        private set

    // Whether the document contains at least one camera object, so the Play button can enable
    // itself. Refreshed by the scene as the structure changes.
    var hasCamera: Boolean by mutableStateOf(false)
        private set

    /** Set by the scene when entering/leaving play mode. */
    fun setPlaying(playing: Boolean, cameraName: String?) {
        this.playing = playing
        this.playCameraName = cameraName
    }

    /** Set by the scene as camera objects are added/removed. */
    fun updateHasCamera(hasCamera: Boolean) {
        this.hasCamera = hasCamera
    }

    // Version counter observed by the inspector - bumped when a transform
    // actually changes so the UI recomposes only when needed.
    var transformVersion: Long by mutableStateOf(0L)
        private set

    // Version counter observed by the hierarchy - bumped when the scene
    // structure changes (attach/detach/rename).
    var structureVersion: Long by mutableStateOf(0L)
        private set

    // Version counter observed by property editors (name, colors, flags) so
    // locally-remembered field state re-reads after undo/redo.
    var propertyVersion: Long by mutableStateOf(0L)
        private set

    // Version counter observed by menu items to refresh undo/redo enabled state.
    var historyVersion: Long by mutableStateOf(0L)
        private set

    // File the document was loaded from / saved to, if any
    var currentFile: File? by mutableStateOf(null)
        private set

    // True when the document has changes not yet written to currentFile
    var dirty: Boolean by mutableStateOf(false)
        private set

    // Shown in the window title
    val documentTitle: String
        get() = (currentFile?.name ?: "Untitled") + (if (dirty) " *" else "")

    // Undo/redo history. All edits made through the UI should go through execute()/record().
    val commandStack = CommandStack { command ->
        if (command.affectsStructure) {
            structureVersion++
        }
        transformVersion++
        propertyVersion++
        historyVersion++
        dirty = true
    }

    /**
     * Swaps in a new document root (New Scene / Open), clearing selection and history.
     */
    fun replaceSceneRoot(newRoot: Node) {
        clearSelection()
        commandStack.clear()
        sceneRoot = newRoot
        structureVersion++
        transformVersion++
        propertyVersion++
        historyVersion++
    }

    /**
     * Marks the document as persisted to [file] (or as a fresh document when null).
     */
    fun markSaved(file: File?) {
        currentFile = file
        dirty = false
    }

    /** Executes an [EditorCommand] and records it for undo. */
    fun execute(command: EditorCommand) = commandStack.execute(command)

    /** Records a command whose effect is already applied (e.g. a finished gizmo drag). */
    fun record(command: EditorCommand) = commandStack.record(command)

    fun undo() {
        commandStack.undo()
    }

    fun redo() {
        commandStack.redo()
    }

    /** Marks a gesture boundary so subsequent edits start a new undo step. */
    fun sealUndoMerge() = commandStack.sealMerge()

    /**
     * Called when a spatial's transform is modified to trigger UI updates.
     */
    fun notifyTransformChanged() {
        transformVersion++
    }

    /**
     * Called when the scene structure changes (attach, detach, rename) to trigger UI updates.
     */
    fun notifyStructureChanged() {
        structureVersion++
    }

    /**
     * Select a single spatial, clearing previous selection.
     */
    fun select(spatial: Spatial?) {
        _selection.clear()
        spatial?.let { _selection.add(it) }
    }

    /**
     * Replaces the selection with [spatials]; the first entry becomes the primary selection.
     */
    fun selectAll(spatials: List<Spatial>) {
        _selection.clear()
        for (spatial in spatials) {
            if (spatial !in _selection) {
                _selection.add(spatial)
            }
        }
    }

    /**
     * Add a spatial to the current selection.
     */
    fun addToSelection(spatial: Spatial) {
        if (spatial !in _selection) {
            _selection.add(spatial)
        }
    }

    /**
     * Remove a spatial from the current selection.
     */
    fun removeFromSelection(spatial: Spatial) {
        _selection.remove(spatial)
    }

    /**
     * Toggle selection of a spatial.
     */
    fun toggleSelection(spatial: Spatial) {
        if (spatial in _selection) {
            _selection.remove(spatial)
        } else {
            _selection.add(spatial)
        }
    }

    /**
     * Clear all selection.
     */
    fun clearSelection() {
        _selection.clear()
    }

    /**
     * Check if a spatial is selected.
     */
    fun isSelected(spatial: Spatial): Boolean = spatial in _selection

    /**
     * Get the primary (first) selected spatial, if any.
     */
    val primarySelection: Spatial?
        get() = _selection.firstOrNull()

    private fun createDefaultScene(): Node {
        return Node("Scene Root")
    }
}

enum class TransformMode {
    TRANSLATE,
    ROTATE,
    SCALE
}
