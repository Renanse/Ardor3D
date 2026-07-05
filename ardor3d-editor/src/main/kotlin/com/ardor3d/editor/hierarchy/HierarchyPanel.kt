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
import com.ardor3d.light.Light
import com.ardor3d.scenegraph.Mesh
import com.ardor3d.scenegraph.Node
import com.ardor3d.scenegraph.Spatial

@Composable
fun HierarchyPanel(
    editorState: EditorState,
    modifier: Modifier = Modifier
) {
    // Track expanded nodes by their names (or a unique id)
    val expandedNodes = remember { mutableStateMapOf<String, Boolean>() }

    // Observe structure version so the tree refreshes on attach/detach/rename
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
                        expandedNodes = expandedNodes,
                        depth = 0
                    )
                }
            }
        }
    }
}

/**
 * Checks if a spatial is an editor-internal object that should be hidden from the hierarchy.
 */
private fun isEditorInternal(spatial: Spatial): Boolean {
    val name = spatial.name ?: return false
    return name.startsWith("Editor") || name.startsWith("LightGizmo")
}

/**
 * Counts total spatials in the tree (for header display), excluding editor internals.
 */
private fun countSpatials(spatial: Spatial): Int {
    if (isEditorInternal(spatial)) return 0
    var count = 1
    if (spatial is Node) {
        for (child in spatial.children) {
            count += countSpatials(child)
        }
    }
    return count
}

/**
 * Gets a unique key for a spatial for tracking expanded state.
 */
private fun getSpatialKey(spatial: Spatial): String {
    // Use name + hashCode for uniqueness
    return "${spatial.name ?: "unnamed"}_${System.identityHashCode(spatial)}"
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
            val visibleCount = spatial.children.count { !isEditorInternal(it) }
            if (visibleCount > 0) "Node ($visibleCount)" else "Node"
        }
        else -> "Spatial"
    }
}

@Composable
private fun NodeTreeItem(
    spatial: Spatial,
    editorState: EditorState,
    expandedNodes: MutableMap<String, Boolean>,
    depth: Int
) {
    val spatialKey = getSpatialKey(spatial)
    val expanded = expandedNodes.getOrPut(spatialKey) { depth == 0 }  // Root expanded by default
    val isSelected = editorState.isSelected(spatial)
    // Count only non-editor children for display
    val visibleChildCount = if (spatial is Node) {
        spatial.children.count { !isEditorInternal(it) }
    } else 0
    val hasChildren = visibleChildCount > 0

    Column {
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
                    text = "$visibleChildCount",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }

        // Children (recursive), filtering out editor-internal objects
        if (expanded && spatial is Node) {
            spatial.children
                .filter { !isEditorInternal(it) }
                .forEach { child ->
                    NodeTreeItem(
                        spatial = child,
                        editorState = editorState,
                        expandedNodes = expandedNodes,
                        depth = depth + 1
                    )
                }
        }
    }
}

