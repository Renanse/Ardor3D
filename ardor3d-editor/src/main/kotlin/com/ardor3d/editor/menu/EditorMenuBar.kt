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
                    text = { Text("Import OBJ...") },
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
                // The scene root itself cannot be deleted or duplicated
                val editableCount = editorState.selection.count { it.parent != null }
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

                // 3D Object submenu
                DropdownMenuItem(
                    text = { Text("3D Object  ▶") },
                    onClick = { shapesSubmenuExpanded = !shapesSubmenuExpanded }
                )

                // Shapes submenu (shown inline when expanded)
                if (shapesSubmenuExpanded) {
                    // Basic shapes
                    DropdownMenuItem(
                        text = { Text("    Box") },
                        onClick = {
                            onAddShape(ShapeType.BOX)
                            gameObjectMenuExpanded = false
                            shapesSubmenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("    Sphere") },
                        onClick = {
                            onAddShape(ShapeType.SPHERE)
                            gameObjectMenuExpanded = false
                            shapesSubmenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("    Plane") },
                        onClick = {
                            onAddShape(ShapeType.PLANE)
                            gameObjectMenuExpanded = false
                            shapesSubmenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("    Cylinder") },
                        onClick = {
                            onAddShape(ShapeType.CYLINDER)
                            gameObjectMenuExpanded = false
                            shapesSubmenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("    Cone") },
                        onClick = {
                            onAddShape(ShapeType.CONE)
                            gameObjectMenuExpanded = false
                            shapesSubmenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("    Capsule") },
                        onClick = {
                            onAddShape(ShapeType.CAPSULE)
                            gameObjectMenuExpanded = false
                            shapesSubmenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("    Torus") },
                        onClick = {
                            onAddShape(ShapeType.TORUS)
                            gameObjectMenuExpanded = false
                            shapesSubmenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("    Pyramid") },
                        onClick = {
                            onAddShape(ShapeType.PYRAMID)
                            gameObjectMenuExpanded = false
                            shapesSubmenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("    Dome") },
                        onClick = {
                            onAddShape(ShapeType.DOME)
                            gameObjectMenuExpanded = false
                            shapesSubmenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("    Disk") },
                        onClick = {
                            onAddShape(ShapeType.DISK)
                            gameObjectMenuExpanded = false
                            shapesSubmenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("    Tube") },
                        onClick = {
                            onAddShape(ShapeType.TUBE)
                            gameObjectMenuExpanded = false
                            shapesSubmenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("    GeoSphere") },
                        onClick = {
                            onAddShape(ShapeType.GEOSPHERE)
                            gameObjectMenuExpanded = false
                            shapesSubmenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("    Teapot") },
                        onClick = {
                            onAddShape(ShapeType.TEAPOT)
                            gameObjectMenuExpanded = false
                            shapesSubmenuExpanded = false
                        }
                    )
                }

                HorizontalDivider()

                // Light submenu
                DropdownMenuItem(
                    text = { Text("Light  ▶") },
                    onClick = { lightsSubmenuExpanded = !lightsSubmenuExpanded }
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

                DropdownMenuItem(
                    text = { Text("Camera") },
                    enabled = false,
                    onClick = { gameObjectMenuExpanded = false }
                )
            }
        }
    }
}
