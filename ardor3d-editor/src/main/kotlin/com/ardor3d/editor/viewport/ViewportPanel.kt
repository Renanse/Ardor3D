package com.ardor3d.editor.viewport

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Rotate90DegreesCcw
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ardor3d.editor.EditorState
import com.ardor3d.editor.TransformMode

@Composable
fun ViewportPanel(
    editorState: EditorState,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // Viewport background (placeholder for GL rendering)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF2D2D30))
        ) {
            // Placeholder text - will be replaced with actual GL viewport
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "3D Viewport",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color(0xFF808080)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Ardor3D rendering will appear here",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF606060)
                )
            }
        }

        // Toolbar overlay (top-left)
        ViewportToolbar(
            editorState = editorState,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        )

        // Status bar (bottom)
        ViewportStatusBar(
            editorState = editorState,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
        )
    }
}

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
                contentDescription = "Translate (W)",
                selected = editorState.transformMode == TransformMode.TRANSLATE,
                onClick = { editorState.transformMode = TransformMode.TRANSLATE }
            )

            // Rotate tool
            TransformToolButton(
                icon = Icons.Default.Rotate90DegreesCcw,
                contentDescription = "Rotate (E)",
                selected = editorState.transformMode == TransformMode.ROTATE,
                onClick = { editorState.transformMode = TransformMode.ROTATE }
            )

            // Scale tool
            TransformToolButton(
                icon = Icons.Default.ZoomOutMap,
                contentDescription = "Scale (R)",
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
