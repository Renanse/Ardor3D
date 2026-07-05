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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ardor3d.editor.EditorState
import com.ardor3d.editor.SceneOperations
import com.ardor3d.light.Light
import com.ardor3d.scenegraph.Mesh
import com.ardor3d.scenegraph.Node
import com.ardor3d.scenegraph.Spatial

@Composable
fun HierarchyPanel(
    editorState: EditorState,
    operations: SceneOperations,
    modifier: Modifier = Modifier
) {
    // Track expanded nodes by identity
    val expandedNodes = remember { mutableStateMapOf<Int, Boolean>() }

    // Spatial currently being renamed via the context menu, if any
    var renameTarget by remember { mutableStateOf<Spatial?>(null) }

    // Observe structure version so the tree refreshes on attach/detach/rename
    @Suppress("UNUSED_VARIABLE")
    val version = editorState.structureVersion

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

@Composable
private fun NodeTreeItem(
    spatial: Spatial,
    editorState: EditorState,
    operations: SceneOperations,
    expandedNodes: MutableMap<Int, Boolean>,
    onRequestRename: (Spatial) -> Unit,
    depth: Int
) {
    val spatialKey = System.identityHashCode(spatial)
    val expanded = expandedNodes.getOrPut(spatialKey) { depth == 0 }  // Root expanded by default
    val isSelected = editorState.isSelected(spatial)
    val childCount = if (spatial is Node) spatial.numberOfChildren else 0
    val hasChildren = childCount > 0
    // The scene root cannot be deleted, duplicated or renamed from the tree
    val isRoot = spatial.parent == null

    Column {
        ContextMenuArea(items = {
            if (isRoot) {
                emptyList()
            } else {
                listOf(
                    ContextMenuItem("Rename") { onRequestRename(spatial) },
                    ContextMenuItem("Duplicate") { operations.duplicateSpatial(spatial) },
                    ContextMenuItem("Delete") { operations.deleteSpatial(spatial) }
                )
            }
        }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        when {
                            isSelected -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surface
                        }
                    )
                    .clickable { editorState.select(spatial) }
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

                // Name
                Text(
                    text = spatial.name ?: "(unnamed)",
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSurface
                    },
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
                    depth = depth + 1
                )
            }
        }
    }
}
