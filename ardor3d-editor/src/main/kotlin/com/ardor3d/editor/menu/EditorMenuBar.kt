/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.editor.menu

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ardor3d.editor.EditorState
import com.ardor3d.editor.FileOperations
import com.ardor3d.editor.LightType
import com.ardor3d.editor.SceneOperations
import com.ardor3d.editor.util.SelectionUtil

enum class ShapeType {
    BOX,
    SPHERE,
    PLANE,
    CYLINDER,
    CONE,
    CAPSULE,
    TORUS,
    PYRAMID,
    TEAPOT,
    DOME,
    DISK,
    TUBE,
    GEOSPHERE,
    DODECAHEDRON,
    ICOSAHEDRON,
    OCTAHEDRON
}

@Composable
fun EditorMenuBar(
    editorState: EditorState,
    operations: SceneOperations,
    fileOperations: FileOperations,
    modifier: Modifier = Modifier
) {
    val onAddShape = operations.addShape
    var fileMenuExpanded by remember { mutableStateOf(false) }
    var editMenuExpanded by remember { mutableStateOf(false) }
    var gameObjectMenuExpanded by remember { mutableStateOf(false) }
    var shapesSubmenuExpanded by remember { mutableStateOf(false) }
    var lightsSubmenuExpanded by remember { mutableStateOf(false) }
    var gameMenuExpanded by remember { mutableStateOf(false) }

    Surface(
        tonalElevation = 2.dp,
        modifier = modifier
    ) {
        Row(modifier = Modifier.padding(horizontal = 4.dp)) {
            // File menu
            TextButton(onClick = { fileMenuExpanded = true }) {
                Text("File")
            }
            DropdownMenu(
                expanded = fileMenuExpanded,
                onDismissRequest = { fileMenuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("New Scene") },
                    onClick = {
                        fileOperations.newScene()
                        fileMenuExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Open...") },
                    onClick = {
                        fileOperations.openScene()
                        fileMenuExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Save") },
                    onClick = {
                        fileOperations.saveScene()
                        fileMenuExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Save As...") },
                    onClick = {
                        fileOperations.saveSceneAs()
                        fileMenuExpanded = false
                    }
                )
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Import Model (OBJ, DAE)...") },
                    onClick = {
                        fileOperations.importModel()
                        fileMenuExpanded = false
                    }
                )
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Exit") },
                    onClick = {
                        fileMenuExpanded = false
                        fileOperations.exit()
                    }
                )
            }

            // Edit menu
            TextButton(onClick = { editMenuExpanded = true }) {
                Text("Edit")
            }
            DropdownMenu(
                expanded = editMenuExpanded,
                onDismissRequest = { editMenuExpanded = false }
            ) {
                // Observe historyVersion so enabled state and labels refresh
                @Suppress("UNUSED_VARIABLE")
                val historyTick = editorState.historyVersion
                DropdownMenuItem(
                    text = { Text(editorState.commandStack.undoName?.let { "Undo $it" } ?: "Undo") },
                    enabled = editorState.commandStack.canUndo,
                    onClick = {
                        editorState.undo()
                        editMenuExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(editorState.commandStack.redoName?.let { "Redo $it" } ?: "Redo") },
                    enabled = editorState.commandStack.canRedo,
                    onClick = {
                        editorState.redo()
                        editMenuExpanded = false
                    }
                )
                HorizontalDivider()
                // Count what Delete/Duplicate will actually process: top-most, non-root
                val editableCount =
                    SelectionUtil.topMost(editorState.selection.filter { it.parent != null }).size
                val plural = if (editableCount > 1) " $editableCount objects" else ""
                DropdownMenuItem(
                    text = { Text("Delete$plural") },
                    enabled = editableCount > 0,
                    onClick = {
                        operations.deleteSelection()
                        editMenuExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Duplicate$plural") },
                    enabled = editableCount > 0,
                    onClick = {
                        operations.duplicateSelection()
                        editMenuExpanded = false
                    }
                )
            }

            // GameObject menu
            TextButton(onClick = { gameObjectMenuExpanded = true }) {
                Text("GameObject")
            }
            DropdownMenu(
                expanded = gameObjectMenuExpanded,
                onDismissRequest = {
                    gameObjectMenuExpanded = false
                    shapesSubmenuExpanded = false
                    lightsSubmenuExpanded = false
                }
            ) {
                DropdownMenuItem(
                    text = { Text("Create Empty") },
                    onClick = {
                        operations.createEmpty()
                        gameObjectMenuExpanded = false
                    }
                )
                HorizontalDivider()

                // 3D Object submenu (opening one submenu closes the other)
                DropdownMenuItem(
                    text = { Text("3D Object  ▶") },
                    onClick = {
                        shapesSubmenuExpanded = !shapesSubmenuExpanded
                        if (shapesSubmenuExpanded) {
                            lightsSubmenuExpanded = false
                        }
                    }
                )

                // Shapes submenu (shown inline when expanded)
                if (shapesSubmenuExpanded) {
                    listOf(
                        "Box" to ShapeType.BOX,
                        "Sphere" to ShapeType.SPHERE,
                        "Plane" to ShapeType.PLANE,
                        "Cylinder" to ShapeType.CYLINDER,
                        "Cone" to ShapeType.CONE,
                        "Capsule" to ShapeType.CAPSULE,
                        "Torus" to ShapeType.TORUS,
                        "Pyramid" to ShapeType.PYRAMID,
                        "Dome" to ShapeType.DOME,
                        "Disk" to ShapeType.DISK,
                        "Tube" to ShapeType.TUBE,
                        "GeoSphere" to ShapeType.GEOSPHERE,
                        "Dodecahedron" to ShapeType.DODECAHEDRON,
                        "Icosahedron" to ShapeType.ICOSAHEDRON,
                        "Octahedron" to ShapeType.OCTAHEDRON,
                        "Teapot" to ShapeType.TEAPOT
                    ).forEach { (label, type) ->
                        DropdownMenuItem(
                            text = { Text("    $label") },
                            onClick = {
                                onAddShape(type)
                                gameObjectMenuExpanded = false
                                shapesSubmenuExpanded = false
                            }
                        )
                    }
                }

                HorizontalDivider()

                // Light submenu
                DropdownMenuItem(
                    text = { Text("Light  ▶") },
                    onClick = {
                        lightsSubmenuExpanded = !lightsSubmenuExpanded
                        if (lightsSubmenuExpanded) {
                            shapesSubmenuExpanded = false
                        }
                    }
                )
                if (lightsSubmenuExpanded) {
                    DropdownMenuItem(
                        text = { Text("    Point Light") },
                        onClick = {
                            operations.addLight(LightType.POINT)
                            gameObjectMenuExpanded = false
                            lightsSubmenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("    Directional Light") },
                        onClick = {
                            operations.addLight(LightType.DIRECTIONAL)
                            gameObjectMenuExpanded = false
                            lightsSubmenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("    Spot Light") },
                        onClick = {
                            operations.addLight(LightType.SPOT)
                            gameObjectMenuExpanded = false
                            lightsSubmenuExpanded = false
                        }
                    )
                }

                HorizontalDivider()

                DropdownMenuItem(
                    text = { Text("Camera") },
                    onClick = {
                        operations.addCamera()
                        gameObjectMenuExpanded = false
                        shapesSubmenuExpanded = false
                        lightsSubmenuExpanded = false
                    }
                )
            }

            // Game menu - launch a sample game in play mode.
            TextButton(onClick = { gameMenuExpanded = true }) {
                Text("Game")
            }
            DropdownMenu(
                expanded = gameMenuExpanded,
                onDismissRequest = { gameMenuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Play Checkers") },
                    onClick = {
                        operations.playCheckers()
                        gameMenuExpanded = false
                    }
                )
            }
        }
    }
}
