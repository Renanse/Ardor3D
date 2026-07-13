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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Rotate90DegreesCcw
import androidx.compose.material.icons.filled.Stop
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
    onTogglePlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playing = editorState.playing
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        tonalElevation = 4.dp,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            // Transform tools are disabled while playing - there is no gizmo in the game view.
            TransformToolButton(
                icon = Icons.Default.OpenWith,
                contentDescription = "Translate (1)",
                selected = editorState.transformMode == TransformMode.TRANSLATE,
                enabled = !playing,
                onClick = { editorState.transformMode = TransformMode.TRANSLATE }
            )

            TransformToolButton(
                icon = Icons.Default.Rotate90DegreesCcw,
                contentDescription = "Rotate (2)",
                selected = editorState.transformMode == TransformMode.ROTATE,
                enabled = !playing,
                onClick = { editorState.transformMode = TransformMode.ROTATE }
            )

            TransformToolButton(
                icon = Icons.Default.ZoomOutMap,
                contentDescription = "Scale (3)",
                selected = editorState.transformMode == TransformMode.SCALE,
                enabled = !playing,
                onClick = { editorState.transformMode = TransformMode.SCALE }
            )

            VerticalToolbarDivider()

            // Play / stop: renders the viewport through the active camera object. Enabled when the
            // scene has a camera (or while already playing, so Stop is always reachable).
            PlayToggleButton(
                playing = playing,
                enabled = editorState.hasCamera || playing,
                onClick = onTogglePlay
            )
        }
    }
}

@Composable
private fun VerticalToolbarDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .height(24.dp)
            .width(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}

@Composable
private fun PlayToggleButton(
    playing: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val tint = when {
        playing -> MaterialTheme.colorScheme.error
        enabled -> Color(0xFF4CAF50) // green "go"
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    }
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(32.dp)
    ) {
        Icon(
            imageVector = if (playing) Icons.Default.Stop else Icons.Default.PlayArrow,
            contentDescription = if (playing) "Stop (exit play mode)" else "Play (view through camera)",
            modifier = Modifier.size(22.dp),
            tint = tint
        )
    }
}

@Composable
private fun TransformToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
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
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Selection info - or, while playing, the active game's status (if any) else which
            // camera the viewport is rendering through.
            if (editorState.playing) {
                val gameStatus = editorState.gameStatus
                Text(
                    text = if (gameStatus != null) "▶ $gameStatus"
                        else "▶ PLAYING — ${editorState.playCameraName ?: "camera"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                val selectionText = when (editorState.selection.size) {
                    0 -> "No selection"
                    1 -> editorState.selection.first().name ?: "(unnamed)"
                    else -> "${editorState.selection.size} objects selected"
                }
                Text(
                    text = selectionText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Transform mode and frame rate
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = editorState.transformMode.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${editorState.framesPerSecond} FPS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
