/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.editor.hierarchy

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.onClick
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ardor3d.editor.EditorState
import com.ardor3d.editor.SceneOperations
import com.ardor3d.editor.command.ReparentCommand
import com.ardor3d.editor.util.SelectionUtil
import com.ardor3d.light.Light
import com.ardor3d.scenegraph.Mesh
import com.ardor3d.scenegraph.Node
import com.ardor3d.scenegraph.Spatial
import com.ardor3d.scenegraph.hint.CullHint

/**
 * Shared state for drag-to-reparent: the spatial being dragged, the currently valid drop
 * target, and each visible row's layout coordinates for pointer hit-testing.
 */
private class DragReparentState {
    var dragging: Spatial? by mutableStateOf(null)
    var dropTarget: Node? by mutableStateOf(null)
    val rowCoords = mutableMapOf<Spatial, LayoutCoordinates>()

    fun reset() {
        dragging = null
        dropTarget = null
    }
}

@Composable
fun HierarchyPanel(
    editorState: EditorState,
    operations: SceneOperations,
    modifier: Modifier = Modifier
) {
    // Track expanded nodes by identity
    val expandedNodes = remember { mutableStateMapOf<Int, Boolean>() }

    // Drag-to-reparent bookkeeping
    val dragState = remember { DragReparentState() }

    // Spatial currently being renamed via the context menu, if any
    var renameTarget by remember { mutableStateOf<Spatial?>(null) }

    // Anchor for shift-click range selection: the last plainly-clicked or toggled item
    var selectionAnchor by remember { mutableStateOf<Spatial?>(null) }

    // Observe structure version so the tree refreshes on attach/detach/rename, and property
    // version so row decorations (visibility) refresh on undo/redo
    @Suppress("UNUSED_VARIABLE")
    val version = editorState.structureVersion

    @Suppress("UNUSED_VARIABLE")
    val propertyTick = editorState.propertyVersion

    // Plain click selects; ctrl-click toggles; shift-click selects the visible range from the
    // anchor (falling back to a plain select when the anchor is gone).
    val onItemClick: (Spatial, Boolean, Boolean) -> Unit = { spatial, ctrl, shift ->
        val anchor = selectionAnchor
        when {
            ctrl -> {
                editorState.toggleSelection(spatial)
                selectionAnchor = spatial
            }

            shift && anchor != null -> {
                val visible = mutableListOf<Spatial>()
                flattenVisible(editorState.sceneRoot, expandedNodes, 0, visible)
                val anchorIndex = visible.indexOfFirst { it === anchor }
                val clickedIndex = visible.indexOfFirst { it === spatial }
                if (anchorIndex >= 0 && clickedIndex >= 0) {
                    val range = if (anchorIndex <= clickedIndex) anchorIndex..clickedIndex
                    else clickedIndex..anchorIndex
                    // The clicked item leads so it becomes the primary selection
                    val ordered = range.map { visible[it] }
                    editorState.selectAll(listOf(spatial) + ordered.filter { it !== spatial })
                } else {
                    editorState.select(spatial)
                    selectionAnchor = spatial
                }
            }

            else -> {
                editorState.select(spatial)
                selectionAnchor = spatial
            }
        }
    }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header with scene info
            Surface(
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Hierarchy",
                        style = MaterialTheme.typography.titleSmall
                    )
                    // Show object count
                    Text(
                        text = "${countSpatials(editorState.sceneRoot)} objects",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider()

            // Tree content
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
            ) {
                item {
                    NodeTreeItem(
                        spatial = editorState.sceneRoot,
                        editorState = editorState,
                        operations = operations,
                        expandedNodes = expandedNodes,
                        onRequestRename = { renameTarget = it },
                        onItemClick = onItemClick,
                        dragState = dragState,
                        depth = 0
                    )
                }
            }
        }
    }

    // Rename dialog
    renameTarget?.let { target ->
        var newName by remember(target) { mutableStateOf(target.name ?: "") }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    operations.renameSpatial(target, newName)
                    renameTarget = null
                }) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("Cancel") }
            }
        )
    }
}

/**
 * The single expansion rule shared by rendering and range selection: a node is expanded when
 * toggled so, and only the root starts expanded.
 */
private fun isExpanded(expandedNodes: Map<Int, Boolean>, spatial: Spatial, depth: Int): Boolean =
    expandedNodes[System.identityHashCode(spatial)] ?: (depth == 0)

/**
 * Collects the spatials currently visible in the tree, in display order.
 */
private fun flattenVisible(
    spatial: Spatial,
    expandedNodes: Map<Int, Boolean>,
    depth: Int,
    into: MutableList<Spatial>
) {
    into.add(spatial)
    if (isExpanded(expandedNodes, spatial, depth) && spatial is Node) {
        for (child in spatial.children) {
            flattenVisible(child, expandedNodes, depth + 1, into)
        }
    }
}

/**
 * Counts total spatials in the tree (for header display).
 */
private fun countSpatials(spatial: Spatial): Int {
    var count = 1
    if (spatial is Node) {
        for (child in spatial.children) {
            count += countSpatials(child)
        }
    }
    return count
}

/**
 * Gets the appropriate icon for a spatial type.
 */
private fun getIconForSpatial(spatial: Spatial): ImageVector {
    return when (spatial) {
        is Light -> Icons.Default.LightMode
        is Mesh -> Icons.Default.ViewInAr
        is Node -> Icons.Default.Folder
        else -> Icons.Default.Circle
    }
}

/**
 * Gets a description of the spatial type.
 */
private fun getTypeDescription(spatial: Spatial): String {
    return when (spatial) {
        is Light -> "Light"
        is Mesh -> "Mesh"
        is Node -> {
            val childCount = spatial.numberOfChildren
            if (childCount > 0) "Node ($childCount)" else "Node"
        }

        else -> "Spatial"
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NodeTreeItem(
    spatial: Spatial,
    editorState: EditorState,
    operations: SceneOperations,
    expandedNodes: MutableMap<Int, Boolean>,
    onRequestRename: (Spatial) -> Unit,
    onItemClick: (Spatial, Boolean, Boolean) -> Unit,
    dragState: DragReparentState,
    depth: Int,
    inheritedHidden: Boolean = false
) {
    val spatialKey = System.identityHashCode(spatial)
    val expanded = isExpanded(expandedNodes, spatial, depth)
    val isSelected = editorState.isSelected(spatial)
    val childCount = if (spatial is Node) spatial.numberOfChildren else 0
    val hasChildren = childCount > 0
    // The scene root cannot be deleted, duplicated or renamed from the tree
    val isRoot = spatial.parent == null
    val isHidden = spatial.sceneHints.cullHint == CullHint.Always
    val effectiveHidden = isHidden || inheritedHidden

    // Drop this row's drag hit-test coordinates when it leaves the composition
    // (delete, collapse, document swap) so the map doesn't retain dead spatials.
    DisposableEffect(spatial) {
        onDispose { dragState.rowCoords.remove(spatial) }
    }

    Column {
        ContextMenuArea(items = {
            if (isRoot) {
                emptyList()
            } else if (isSelected && editorState.selection.size > 1) {
                // Acting on an item that is part of a multi-selection acts on all of it.
                // Count what the operations will actually process: top-most, non-root.
                val count = SelectionUtil.topMost(editorState.selection.filter { it.parent != null }).size
                val what = if (count == 1) "1 object" else "$count objects"
                listOf(
                    ContextMenuItem("Duplicate $what") { operations.duplicateSelection() },
                    ContextMenuItem("Delete $what") { operations.deleteSelection() }
                )
            } else {
                buildList {
                    add(ContextMenuItem("Rename") { onRequestRename(spatial) })
                    add(ContextMenuItem("Duplicate") { operations.duplicateSpatial(spatial) })
                    add(ContextMenuItem("Delete") { operations.deleteSpatial(spatial) })
                    if (spatial.parent !== editorState.sceneRoot) {
                        add(ContextMenuItem("Move to Root") {
                            operations.reparentSpatial(spatial, editorState.sceneRoot)
                        })
                    }
                }
            }
        }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        when {
                            dragState.dropTarget === spatial -> MaterialTheme.colorScheme.tertiaryContainer
                            isSelected -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surface
                        }
                    )
                    .onGloballyPositioned { dragState.rowCoords[spatial] = it }
                    .pointerInput(spatial, isRoot) {
                        if (isRoot) {
                            return@pointerInput
                        }
                        // Drag a row onto a Node row to reparent (world transform preserved)
                        detectDragGestures(
                            onDragStart = { dragState.dragging = spatial },
                            onDrag = { change, _ ->
                                change.consume()
                                val coords = dragState.rowCoords[spatial]
                                if (coords != null && coords.isAttached) {
                                    val windowPos = coords.localToWindow(change.position)
                                    val hovered = dragState.rowCoords.entries.firstOrNull { (other, c) ->
                                        other !== spatial && c.isAttached &&
                                            c.boundsInWindow().contains(windowPos)
                                    }?.key
                                    dragState.dropTarget = (hovered as? Node)
                                        ?.takeIf { ReparentCommand.isValidReparent(spatial, it) }
                                }
                            },
                            onDragEnd = {
                                dragState.dropTarget?.let { operations.reparentSpatial(spatial, it) }
                                dragState.reset()
                            },
                            onDragCancel = { dragState.reset() }
                        )
                    }
                    .onClick(
                        keyboardModifiers = { isCtrlPressed || isMetaPressed },
                        onClick = { onItemClick(spatial, true, false) }
                    )
                    .onClick(
                        keyboardModifiers = { isShiftPressed && !isCtrlPressed && !isMetaPressed },
                        onClick = { onItemClick(spatial, false, true) }
                    )
                    .onClick(
                        keyboardModifiers = { !isCtrlPressed && !isMetaPressed && !isShiftPressed },
                        onClick = { onItemClick(spatial, false, false) }
                    )
                    .padding(start = (depth * 16).dp, top = 2.dp, bottom = 2.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Expand/collapse button
                if (hasChildren) {
                    IconButton(
                        onClick = { expandedNodes[spatialKey] = !expanded },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                            contentDescription = if (expanded) "Collapse" else "Expand",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(20.dp))
                }

                // Icon based on type
                Icon(
                    imageVector = getIconForSpatial(spatial),
                    contentDescription = getTypeDescription(spatial),
                    modifier = Modifier.size(16.dp),
                    tint = when {
                        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                        spatial is Light -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Name (dimmed when hidden directly or through a hidden ancestor)
                Text(
                    text = spatial.name ?: "(unnamed)",
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSurface
                    }.let { if (effectiveHidden) it.copy(alpha = 0.45f) else it },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Show child count for nodes
                if (hasChildren) {
                    Text(
                        text = "$childCount",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

                // Visibility (eye) toggle; the scene root is protected like its other operations
                if (!isRoot) {
                    IconButton(
                        onClick = { operations.toggleVisibility(spatial) },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = if (isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (isHidden) "Show" else "Hide",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                .copy(alpha = if (isHidden) 1f else 0.5f)
                        )
                    }
                }
            }
        }

        // Children (recursive)
        if (expanded && spatial is Node) {
            spatial.children.forEach { child ->
                NodeTreeItem(
                    spatial = child,
                    editorState = editorState,
                    operations = operations,
                    expandedNodes = expandedNodes,
                    onRequestRename = onRequestRename,
                    onItemClick = onItemClick,
                    dragState = dragState,
                    depth = depth + 1,
                    inheritedHidden = effectiveHidden
                )
            }
        }
    }
}
