/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.editor.viewport

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Rotate90DegreesCcw
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ardor3d.editor.EditorState
import com.ardor3d.editor.TransformMode

@Composable
fun ViewportToolbar(
    editorState: EditorState,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        tonalElevation = 4.dp,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Translate tool
            TransformToolButton(
                icon = Icons.Default.OpenWith,
                contentDescription = "Translate",
                selected = editorState.transformMode == TransformMode.TRANSLATE,
                onClick = { editorState.transformMode = TransformMode.TRANSLATE }
            )

            // Rotate tool
            TransformToolButton(
                icon = Icons.Default.Rotate90DegreesCcw,
                contentDescription = "Rotate",
                selected = editorState.transformMode == TransformMode.ROTATE,
                onClick = { editorState.transformMode = TransformMode.ROTATE }
            )

            // Scale tool
            TransformToolButton(
                icon = Icons.Default.ZoomOutMap,
                contentDescription = "Scale",
                selected = editorState.transformMode == TransformMode.SCALE,
                onClick = { editorState.transformMode = TransformMode.SCALE }
            )
        }
    }
}

@Composable
private fun TransformToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(32.dp),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = if (selected)
                MaterialTheme.colorScheme.primaryContainer
            else
                Color.Transparent
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp),
            tint = if (selected)
                MaterialTheme.colorScheme.onPrimaryContainer
            else
                MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ViewportStatusBar(
    editorState: EditorState,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color(0xFF252526),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Selection info
            val selectionText = when (editorState.selection.size) {
                0 -> "No selection"
                1 -> editorState.selection.first().name ?: "(unnamed)"
                else -> "${editorState.selection.size} objects selected"
            }
            Text(
                text = selectionText,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFAAAAAA)
            )

            // Transform mode
            Text(
                text = editorState.transformMode.name,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFAAAAAA)
            )
        }
    }
}
