package com.ardor3d.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ardor3d.scenegraph.Node
import com.ardor3d.scenegraph.Spatial

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

    // Is the editor currently playing/simulating?
    var isPlaying: Boolean by mutableStateOf(false)

    // Version counter - incremented when transforms change to trigger UI refresh
    var transformVersion: Long by mutableStateOf(0L)
        private set

    /**
     * Called when a spatial's transform is modified to trigger UI updates.
     */
    fun notifyTransformChanged() {
        transformVersion++
    }

    /**
     * Select a single spatial, clearing previous selection.
     */
    fun select(spatial: Spatial?) {
        _selection.clear()
        spatial?.let { _selection.add(it) }
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
