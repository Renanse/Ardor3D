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
    onAddShape: (ShapeType) -> Unit,
    modifier: Modifier = Modifier
) {
    var fileMenuExpanded by remember { mutableStateOf(false) }
    var editMenuExpanded by remember { mutableStateOf(false) }
    var gameObjectMenuExpanded by remember { mutableStateOf(false) }
    var shapesSubmenuExpanded by remember { mutableStateOf(false) }

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
                    onClick = { fileMenuExpanded = false }
                )
                DropdownMenuItem(
                    text = { Text("Open...") },
                    onClick = { fileMenuExpanded = false }
                )
                DropdownMenuItem(
                    text = { Text("Save") },
                    onClick = { fileMenuExpanded = false }
                )
                DropdownMenuItem(
                    text = { Text("Save As...") },
                    onClick = { fileMenuExpanded = false }
                )
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Exit") },
                    onClick = { fileMenuExpanded = false }
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
                DropdownMenuItem(
                    text = { Text("Undo") },
                    onClick = { editMenuExpanded = false }
                )
                DropdownMenuItem(
                    text = { Text("Redo") },
                    onClick = { editMenuExpanded = false }
                )
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = { editMenuExpanded = false }
                )
                DropdownMenuItem(
                    text = { Text("Duplicate") },
                    onClick = { editMenuExpanded = false }
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
                }
            ) {
                DropdownMenuItem(
                    text = { Text("Create Empty") },
                    onClick = { gameObjectMenuExpanded = false }
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
                DropdownMenuItem(
                    text = { Text("Light") },
                    onClick = { gameObjectMenuExpanded = false }
                )
                DropdownMenuItem(
                    text = { Text("Camera") },
                    onClick = { gameObjectMenuExpanded = false }
                )
            }
        }
    }
}
