package com.ardor3d.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.ardor3d.editor.hierarchy.HierarchyPanel
import com.ardor3d.editor.inspector.InspectorPanel
import com.ardor3d.editor.ui.EditorTheme
import com.ardor3d.editor.viewport.ViewportPanel

fun main() = application {
    val windowState = rememberWindowState(size = DpSize(1600.dp, 900.dp))

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "Ardor3D Editor"
    ) {
        EditorTheme {
            EditorApp()
        }
    }
}

@Composable
fun EditorApp() {
    val editorState = remember { EditorState() }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Left panel: Hierarchy
            HierarchyPanel(
                editorState = editorState,
                modifier = Modifier
                    .width(250.dp)
                    .fillMaxHeight()
            )

            // Divider
            VerticalDivider()

            // Center: Viewport
            ViewportPanel(
                editorState = editorState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )

            // Divider
            VerticalDivider()

            // Right panel: Inspector
            InspectorPanel(
                editorState = editorState,
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight()
            )
        }
    }
}

@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}
