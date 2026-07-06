/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.editor.inspector

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ardor3d.editor.EditorState
import com.ardor3d.editor.command.SetterCommand
import com.ardor3d.editor.util.EulerUtil
import com.ardor3d.light.Light
import com.ardor3d.math.ColorRGBA
import com.ardor3d.math.Matrix3
import com.ardor3d.math.Vector3
import com.ardor3d.math.type.ReadOnlyColorRGBA
import com.ardor3d.math.type.ReadOnlyTransform
import com.ardor3d.renderer.state.RenderState
import com.ardor3d.renderer.state.WireframeState
import com.ardor3d.scenegraph.Mesh
import com.ardor3d.scenegraph.Node
import com.ardor3d.scenegraph.Spatial
import com.ardor3d.surface.ColorSurface
import java.util.Locale

@Composable
fun InspectorPanel(
    editorState: EditorState,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Surface(
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Inspector",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(12.dp)
                )
            }

            HorizontalDivider()

            // Content
            val selection = editorState.primarySelection
            if (selection != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // With a multi-selection, the inspector edits the primary item only
                    if (editorState.selection.size > 1) {
                        Text(
                            text = "${editorState.selection.size} objects selected - editing the first",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Name section
                    NameSection(selection, editorState)

                    // Transform section - observe transformVersion to trigger refresh
                    TransformSection(selection, editorState)

                    // Type-specific sections
                    when (selection) {
                        is Light -> LightSection(selection, editorState)
                        is Mesh -> MeshSection(selection, editorState)
                        is Node -> NodeSection(selection)
                    }
                }
            } else {
                // No selection
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Select an object to view its properties",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun NameSection(spatial: Spatial, editorState: EditorState) {
    // Re-read the name after undo/redo (propertyVersion) or selection change
    var name by remember(spatial, editorState.propertyVersion) { mutableStateOf(spatial.name ?: "") }

    OutlinedTextField(
        value = name,
        onValueChange = { newName ->
            name = newName
            editorState.execute(
                SetterCommand(
                    name = "Rename",
                    oldValue = spatial.name ?: "",
                    newValue = newName,
                    mergeKey = "name:${System.identityHashCode(spatial)}",
                    affectsStructure = true,
                    setter = { spatial.name = it }
                )
            )
        },
        label = { Text("Name") },
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { if (!it.isFocused) editorState.sealUndoMerge() },
        singleLine = true
    )
}

@Composable
private fun TransformSection(spatial: Spatial, editorState: EditorState) {
    // Observe transformVersion to trigger recomposition when transforms change
    val version = editorState.transformVersion
    val spatialId = System.identityHashCode(spatial)

    // The inspector owns the Euler angles it displays while editing; they re-derive from the
    // rotation matrix only when the matrix stops matching them (gizmo drag, undo/redo).
    // Re-deriving on every keystroke would make the other two fields jump near the attitude
    // (Z) +/-90 degree singularity, where the Euler decomposition is not unique.
    val eulerAngles = remember(spatial) { EulerUtil.toEulerDegrees(spatial.rotation) }
    if (!EulerUtil.representsRotation(eulerAngles, spatial.rotation)) {
        EulerUtil.toEulerDegrees(spatial.rotation).copyInto(eulerAngles)
    }

    // Edits to a single component merge into one undo step until the field loses focus.
    fun editTranslation(axis: String, newValue: Vector3) {
        editorState.execute(
            SetterCommand(
                name = "Move ${spatial.name ?: "object"}",
                oldValue = Vector3(spatial.translation),
                newValue = newValue,
                mergeKey = "translation.$axis:$spatialId",
                setter = { spatial.translation = it }
            )
        )
    }

    fun editRotation(axis: Int, valueDegrees: Double) {
        eulerAngles[axis] = valueDegrees
        editorState.execute(
            SetterCommand(
                name = "Rotate ${spatial.name ?: "object"}",
                oldValue = Matrix3(spatial.rotation),
                newValue = EulerUtil.fromEulerDegrees(eulerAngles[0], eulerAngles[1], eulerAngles[2])
                    .toRotationMatrix(Matrix3()),
                mergeKey = "rotation.$axis:$spatialId",
                setter = { spatial.setRotation(it) }
            )
        )
    }

    fun editScale(axis: String, newValue: Vector3) {
        editorState.execute(
            SetterCommand(
                name = "Scale ${spatial.name ?: "object"}",
                oldValue = Vector3(spatial.scale),
                newValue = newValue,
                mergeKey = "scale.$axis:$spatialId",
                setter = { spatial.scale = it }
            )
        )
    }

    InspectorSection(title = "Transform") {
        val transform = spatial.transform
        val sealMerge = { editorState.sealUndoMerge() }

        // Position - each component reads current values from spatial when changed
        Vector3Field(
            label = "Position",
            x = transform.translation.x,
            y = transform.translation.y,
            z = transform.translation.z,
            onXChange = { newX -> editTranslation("x", Vector3(newX, spatial.translation.y, spatial.translation.z)) },
            onYChange = { newY -> editTranslation("y", Vector3(spatial.translation.x, newY, spatial.translation.z)) },
            onZChange = { newZ -> editTranslation("z", Vector3(spatial.translation.x, spatial.translation.y, newZ)) },
            onEditFinished = sealMerge
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Rotation (as XYZ Euler angles for simplicity)
        Vector3Field(
            label = "Rotation",
            x = eulerAngles[0],
            y = eulerAngles[1],
            z = eulerAngles[2],
            onXChange = { editRotation(0, it) },
            onYChange = { editRotation(1, it) },
            onZChange = { editRotation(2, it) },
            onEditFinished = sealMerge
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Scale
        Vector3Field(
            label = "Scale",
            x = transform.scale.x,
            y = transform.scale.y,
            z = transform.scale.z,
            onXChange = { newX -> editScale("x", Vector3(newX, spatial.scale.y, spatial.scale.z)) },
            onYChange = { newY -> editScale("y", Vector3(spatial.scale.x, newY, spatial.scale.z)) },
            onZChange = { newZ -> editScale("z", Vector3(spatial.scale.x, spatial.scale.y, newZ)) },
            onEditFinished = sealMerge
        )
    }
}

@Composable
private fun MeshSection(mesh: Mesh, editorState: EditorState) {
    InspectorSection(title = "Mesh") {
        val meshData = mesh.meshData
        if (meshData != null) {
            LabeledValue("Vertices", meshData.vertexCount.toString())
            LabeledValue("Triangles", (meshData.totalPrimitiveCount).toString())
        } else {
            Text(
                text = "No mesh data",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Material section
    MaterialSection(mesh, editorState)
}

@Composable
private fun MaterialSection(mesh: Mesh, editorState: EditorState) {
    InspectorSection(title = "Material") {
        // Get or create ColorSurface for this mesh
        val surface = remember(mesh) {
            mesh.getProperty<ColorSurface>(ColorSurface.DefaultPropertyKey, null)
                ?: ColorSurface().also { mesh.setProperty(ColorSurface.DefaultPropertyKey, it) }
        }
        val surfaceId = System.identityHashCode(surface)

        // Slider/text edits to the same channel merge into one undo step.
        fun editColor(channel: String, old: ColorRGBA, new: ColorRGBA, setter: (ColorRGBA) -> Unit) {
            editorState.execute(
                SetterCommand(
                    name = "Edit $channel",
                    oldValue = old,
                    newValue = new,
                    mergeKey = "$channel:$surfaceId",
                    setter = setter
                )
            )
        }

        // Diffuse color (main color)
        ColorPropertyField(
            label = "Diffuse",
            color = surface.diffuse,
            version = editorState.propertyVersion,
            onColorChange = { editColor("Diffuse", ColorRGBA(surface.diffuse), it) { c -> surface.diffuse = c } },
            onEditFinished = { editorState.sealUndoMerge() }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Ambient color
        ColorPropertyField(
            label = "Ambient",
            color = surface.ambient,
            version = editorState.propertyVersion,
            onColorChange = { editColor("Ambient", ColorRGBA(surface.ambient), it) { c -> surface.ambient = c } },
            onEditFinished = { editorState.sealUndoMerge() }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Specular color
        ColorPropertyField(
            label = "Specular",
            color = surface.specular,
            version = editorState.propertyVersion,
            onColorChange = { editColor("Specular", ColorRGBA(surface.specular), it) { c -> surface.specular = c } },
            onEditFinished = { editorState.sealUndoMerge() }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Shininess slider
        ShininessSlider(surface, editorState)

        Spacer(modifier = Modifier.height(12.dp))

        // Emissive color (collapsed by default)
        var showEmissive by remember { mutableStateOf(false) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showEmissive = !showEmissive },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (showEmissive) "▼ Emissive" else "▶ Emissive",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (showEmissive) {
            Spacer(modifier = Modifier.height(8.dp))
            ColorPropertyField(
                label = "",
                color = surface.emissive,
                version = editorState.propertyVersion,
                onColorChange = { editColor("Emissive", ColorRGBA(surface.emissive), it) { c -> surface.emissive = c } },
                onEditFinished = { editorState.sealUndoMerge() }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(12.dp))

        // Wireframe toggle
        WireframeToggle(mesh, editorState)
    }
}

@Composable
private fun ColorPropertyField(
    label: String,
    color: ReadOnlyColorRGBA,
    version: Long = 0L,
    onColorChange: (ColorRGBA) -> Unit,
    onEditFinished: () -> Unit = {}
) {
    // Keyed on version so the sliders re-read the color after undo/redo
    var red by remember(color, version) { mutableStateOf(color.red) }
    var green by remember(color, version) { mutableStateOf(color.green) }
    var blue by remember(color, version) { mutableStateOf(color.blue) }

    Column {
        if (label.isNotEmpty()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Color preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(red, green, blue))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
        )

        Spacer(modifier = Modifier.height(8.dp))

        // RGB sliders (compact)
        ColorSlider(label = "R", value = red, color = Color.Red, onEditFinished = onEditFinished) {
            red = it
            onColorChange(ColorRGBA(red, green, blue, 1f))
        }
        ColorSlider(label = "G", value = green, color = Color.Green, onEditFinished = onEditFinished) {
            green = it
            onColorChange(ColorRGBA(red, green, blue, 1f))
        }
        ColorSlider(label = "B", value = blue, color = Color.Blue, onEditFinished = onEditFinished) {
            blue = it
            onColorChange(ColorRGBA(red, green, blue, 1f))
        }
    }
}

@Composable
private fun ShininessSlider(surface: ColorSurface, editorState: EditorState) {
    var shininess by remember(surface, editorState.propertyVersion) { mutableStateOf(surface.shininess) }

    Column {
        Text(
            text = "Shininess",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Slider(
                value = shininess,
                onValueChange = { new ->
                    shininess = new
                    editorState.execute(
                        SetterCommand(
                            name = "Edit Shininess",
                            oldValue = surface.shininess,
                            newValue = new,
                            mergeKey = "shininess:${System.identityHashCode(surface)}",
                            setter = { surface.shininess = it }
                        )
                    )
                },
                onValueChangeFinished = { editorState.sealUndoMerge() },
                valueRange = 0f..128f,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = String.format(Locale.ROOT, "%.0f", shininess),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.width(32.dp)
            )
        }
    }
}

@Composable
private fun ColorSlider(
    label: String,
    value: Float,
    color: Color,
    onEditFinished: () -> Unit = {},
    onValueChange: (Float) -> Unit
) {
    var textValue by remember(value) { mutableStateOf(String.format(Locale.ROOT, "%.2f", value)) }
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.width(14.dp)
        )
        // Slider covers 0-1 range for easy standard colors
        Slider(
            value = value.coerceIn(0f, 1f),
            onValueChange = {
                onValueChange(it)
                textValue = String.format(Locale.ROOT, "%.2f", it)
            },
            onValueChangeFinished = onEditFinished,
            valueRange = 0f..1f,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color
            )
        )
        // Text field allows HDR values > 1.0
        OutlinedTextField(
            value = if (isFocused) textValue else String.format(Locale.ROOT, "%.2f", value),
            onValueChange = { newText ->
                textValue = newText
                newText.toFloatOrNull()?.let {
                    if (it >= 0f) onValueChange(it)
                }
            },
            modifier = Modifier
                .width(56.dp)
                .onFocusChanged {
                    if (!it.isFocused && isFocused) onEditFinished()
                    isFocused = it.isFocused
                    if (it.isFocused) textValue = String.format(Locale.ROOT, "%.2f", value)
                },
            textStyle = MaterialTheme.typography.labelSmall,
            singleLine = true
        )
    }
}

@Composable
private fun WireframeToggle(mesh: Mesh, editorState: EditorState) {
    // Re-read from the mesh after undo/redo
    val isWireframe = remember(mesh, editorState.propertyVersion) {
        (mesh.getLocalRenderState(RenderState.StateType.Wireframe) as? WireframeState)?.isEnabled ?: false
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Wireframe",
            style = MaterialTheme.typography.bodyMedium
        )
        Switch(
            checked = isWireframe,
            onCheckedChange = { enabled ->
                editorState.execute(
                    SetterCommand(
                        name = if (enabled) "Enable Wireframe" else "Disable Wireframe",
                        oldValue = !enabled,
                        newValue = enabled,
                        setter = { on ->
                            if (on) {
                                val wireState = WireframeState()
                                wireState.isEnabled = true
                                mesh.setRenderState(wireState)
                            } else {
                                mesh.clearRenderState(RenderState.StateType.Wireframe)
                            }
                        }
                    )
                )
                editorState.sealUndoMerge()
            }
        )
    }
}

@Composable
private fun LightSection(light: Light, editorState: EditorState) {
    val lightId = System.identityHashCode(light)

    InspectorSection(title = "Light") {
        // Enabled switch
        val enabled = remember(light, editorState.propertyVersion) { light.isEnabled }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Enabled",
                style = MaterialTheme.typography.bodyMedium
            )
            Switch(
                checked = enabled,
                onCheckedChange = { on ->
                    editorState.execute(
                        SetterCommand(
                            name = if (on) "Enable Light" else "Disable Light",
                            oldValue = !on,
                            newValue = on,
                            setter = { light.isEnabled = it }
                        )
                    )
                    editorState.sealUndoMerge()
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Intensity slider
        var intensity by remember(light, editorState.propertyVersion) { mutableStateOf(light.intensity) }
        Text(
            text = "Intensity",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Slider(
                value = intensity,
                onValueChange = { new ->
                    intensity = new
                    editorState.execute(
                        SetterCommand(
                            name = "Edit Intensity",
                            oldValue = light.intensity,
                            newValue = new,
                            mergeKey = "intensity:$lightId",
                            setter = { light.intensity = it }
                        )
                    )
                },
                onValueChangeFinished = { editorState.sealUndoMerge() },
                valueRange = 0f..4f,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = String.format(Locale.ROOT, "%.2f", intensity),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.width(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Color
        ColorPropertyField(
            label = "Color",
            color = light.color,
            version = editorState.propertyVersion,
            onColorChange = { new ->
                editorState.execute(
                    SetterCommand(
                        name = "Edit Light Color",
                        oldValue = ColorRGBA(light.color),
                        newValue = new,
                        mergeKey = "lightColor:$lightId",
                        setter = { light.color = it }
                    )
                )
            },
            onEditFinished = { editorState.sealUndoMerge() }
        )
    }
}

@Composable
private fun NodeSection(node: Node) {
    InspectorSection(title = "Node") {
        LabeledValue("Children", node.numberOfChildren.toString())
    }
}

@Composable
private fun InspectorSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                content = content
            )
        }
    }
}

@Composable
private fun Vector3Field(
    label: String,
    x: Double,
    y: Double,
    z: Double,
    onXChange: (Double) -> Unit,
    onYChange: (Double) -> Unit,
    onZChange: (Double) -> Unit,
    onEditFinished: () -> Unit = {}
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // X
            ComponentField(
                value = x,
                label = "X",
                modifier = Modifier.weight(1f),
                onValueChange = onXChange,
                onEditFinished = onEditFinished
            )
            // Y
            ComponentField(
                value = y,
                label = "Y",
                modifier = Modifier.weight(1f),
                onValueChange = onYChange,
                onEditFinished = onEditFinished
            )
            // Z
            ComponentField(
                value = z,
                label = "Z",
                modifier = Modifier.weight(1f),
                onValueChange = onZChange,
                onEditFinished = onEditFinished
            )
        }
    }
}

@Composable
private fun ComponentField(
    value: Double,
    label: String,
    modifier: Modifier = Modifier,
    onValueChange: (Double) -> Unit,
    onEditFinished: () -> Unit = {}
) {
    var isFocused by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf("") }

    // When not focused, display the live value from the transform
    val displayText = if (isFocused) editText else String.format(Locale.ROOT, "%.3f", value)

    OutlinedTextField(
        value = displayText,
        onValueChange = { newValue ->
            editText = newValue
            newValue.toDoubleOrNull()?.let { onValueChange(it) }
        },
        label = { Text(label) },
        modifier = modifier.onFocusChanged { focusState ->
            if (focusState.isFocused && !isFocused) {
                // Entering focus - copy current value to edit buffer
                editText = String.format(Locale.ROOT, "%.3f", value)
            }
            if (!focusState.isFocused && isFocused) {
                onEditFinished()
            }
            isFocused = focusState.isFocused
        },
        singleLine = true
    )
}

@Composable
private fun LabeledValue(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
