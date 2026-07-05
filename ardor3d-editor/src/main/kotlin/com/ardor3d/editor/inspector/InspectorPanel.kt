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
import com.ardor3d.math.ColorRGBA
import com.ardor3d.math.type.ReadOnlyColorRGBA
import com.ardor3d.math.type.ReadOnlyTransform
import com.ardor3d.renderer.state.RenderState
import com.ardor3d.renderer.state.WireframeState
import com.ardor3d.scenegraph.Mesh
import com.ardor3d.scenegraph.Node
import com.ardor3d.scenegraph.Spatial
import com.ardor3d.surface.ColorSurface

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
                    // Name section
                    NameSection(selection)

                    // Transform section - observe transformVersion to trigger refresh
                    TransformSection(selection, editorState)

                    // Type-specific sections
                    when (selection) {
                        is Mesh -> MeshSection(selection)
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
private fun NameSection(spatial: Spatial) {
    var name by remember(spatial) { mutableStateOf(spatial.name ?: "") }

    OutlinedTextField(
        value = name,
        onValueChange = {
            name = it
            spatial.name = it
        },
        label = { Text("Name") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

@Composable
private fun TransformSection(spatial: Spatial, editorState: EditorState) {
    // Observe transformVersion to trigger recomposition when transforms change
    val version = editorState.transformVersion

    InspectorSection(title = "Transform") {
        val transform = spatial.transform

        // Position - each component reads current values from spatial when changed
        Vector3Field(
            label = "Position",
            x = transform.translation.x,
            y = transform.translation.y,
            z = transform.translation.z,
            onXChange = { newX ->
                val t = spatial.translation
                spatial.translation = com.ardor3d.math.Vector3(newX, t.y, t.z)
                editorState.notifyTransformChanged()
            },
            onYChange = { newY ->
                val t = spatial.translation
                spatial.translation = com.ardor3d.math.Vector3(t.x, newY, t.z)
                editorState.notifyTransformChanged()
            },
            onZChange = { newZ ->
                val t = spatial.translation
                spatial.translation = com.ardor3d.math.Vector3(t.x, t.y, newZ)
                editorState.notifyTransformChanged()
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Rotation (as Euler angles for simplicity)
        val rotMatrix = spatial.rotation
        val quat = com.ardor3d.math.Quaternion().fromRotationMatrix(rotMatrix)
        val angles = quat.toEulerAngles(null)
        Vector3Field(
            label = "Rotation",
            x = Math.toDegrees(angles[0]),
            y = Math.toDegrees(angles[1]),
            z = Math.toDegrees(angles[2]),
            onXChange = { newX ->
                val currentAngles = com.ardor3d.math.Quaternion().fromRotationMatrix(spatial.rotation).toEulerAngles(null)
                val q = com.ardor3d.math.Quaternion()
                q.fromEulerAngles(Math.toRadians(newX), currentAngles[1], currentAngles[2])
                spatial.setRotation(q)
                editorState.notifyTransformChanged()
            },
            onYChange = { newY ->
                val currentAngles = com.ardor3d.math.Quaternion().fromRotationMatrix(spatial.rotation).toEulerAngles(null)
                val q = com.ardor3d.math.Quaternion()
                q.fromEulerAngles(currentAngles[0], Math.toRadians(newY), currentAngles[2])
                spatial.setRotation(q)
                editorState.notifyTransformChanged()
            },
            onZChange = { newZ ->
                val currentAngles = com.ardor3d.math.Quaternion().fromRotationMatrix(spatial.rotation).toEulerAngles(null)
                val q = com.ardor3d.math.Quaternion()
                q.fromEulerAngles(currentAngles[0], currentAngles[1], Math.toRadians(newZ))
                spatial.setRotation(q)
                editorState.notifyTransformChanged()
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Scale
        Vector3Field(
            label = "Scale",
            x = transform.scale.x,
            y = transform.scale.y,
            z = transform.scale.z,
            onXChange = { newX ->
                val s = spatial.scale
                spatial.scale = com.ardor3d.math.Vector3(newX, s.y, s.z)
                editorState.notifyTransformChanged()
            },
            onYChange = { newY ->
                val s = spatial.scale
                spatial.scale = com.ardor3d.math.Vector3(s.x, newY, s.z)
                editorState.notifyTransformChanged()
            },
            onZChange = { newZ ->
                val s = spatial.scale
                spatial.scale = com.ardor3d.math.Vector3(s.x, s.y, newZ)
                editorState.notifyTransformChanged()
            }
        )
    }
}

@Composable
private fun MeshSection(mesh: Mesh) {
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
    MaterialSection(mesh)
}

@Composable
private fun MaterialSection(mesh: Mesh) {
    InspectorSection(title = "Material") {
        // Get or create ColorSurface for this mesh
        val surface = remember(mesh) {
            mesh.getProperty<ColorSurface>(ColorSurface.DefaultPropertyKey, null)
                ?: ColorSurface().also { mesh.setProperty(ColorSurface.DefaultPropertyKey, it) }
        }

        // Diffuse color (main color)
        ColorPropertyField(
            label = "Diffuse",
            color = surface.diffuse,
            onColorChange = { surface.diffuse = it }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Ambient color
        ColorPropertyField(
            label = "Ambient",
            color = surface.ambient,
            onColorChange = { surface.ambient = it }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Specular color
        ColorPropertyField(
            label = "Specular",
            color = surface.specular,
            onColorChange = { surface.specular = it }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Shininess slider
        ShininessSlider(surface)

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
                onColorChange = { surface.emissive = it }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(12.dp))

        // Wireframe toggle
        WireframeToggle(mesh)
    }
}

@Composable
private fun ColorPropertyField(
    label: String,
    color: ReadOnlyColorRGBA,
    onColorChange: (ColorRGBA) -> Unit
) {
    var red by remember(color) { mutableStateOf(color.red) }
    var green by remember(color) { mutableStateOf(color.green) }
    var blue by remember(color) { mutableStateOf(color.blue) }

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
        ColorSlider(label = "R", value = red, color = Color.Red) {
            red = it
            onColorChange(ColorRGBA(red, green, blue, 1f))
        }
        ColorSlider(label = "G", value = green, color = Color.Green) {
            green = it
            onColorChange(ColorRGBA(red, green, blue, 1f))
        }
        ColorSlider(label = "B", value = blue, color = Color.Blue) {
            blue = it
            onColorChange(ColorRGBA(red, green, blue, 1f))
        }
    }
}

@Composable
private fun ShininessSlider(surface: ColorSurface) {
    var shininess by remember(surface) { mutableStateOf(surface.shininess) }

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
                onValueChange = {
                    shininess = it
                    surface.shininess = it
                },
                valueRange = 0f..128f,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = String.format("%.0f", shininess),
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
    onValueChange: (Float) -> Unit
) {
    var textValue by remember(value) { mutableStateOf(String.format("%.2f", value)) }
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
                textValue = String.format("%.2f", it)
            },
            valueRange = 0f..1f,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color
            )
        )
        // Text field allows HDR values > 1.0
        OutlinedTextField(
            value = if (isFocused) textValue else String.format("%.2f", value),
            onValueChange = { newText ->
                textValue = newText
                newText.toFloatOrNull()?.let {
                    if (it >= 0f) onValueChange(it)
                }
            },
            modifier = Modifier
                .width(56.dp)
                .onFocusChanged {
                    isFocused = it.isFocused
                    if (it.isFocused) textValue = String.format("%.2f", value)
                },
            textStyle = MaterialTheme.typography.labelSmall,
            singleLine = true
        )
    }
}

@Composable
private fun WireframeToggle(mesh: Mesh) {
    // Check if mesh has wireframe state
    var isWireframe by remember(mesh) {
        val state = mesh.getLocalRenderState(RenderState.StateType.Wireframe) as? WireframeState
        mutableStateOf(state?.isEnabled ?: false)
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
                isWireframe = enabled
                if (enabled) {
                    val wireState = WireframeState()
                    wireState.isEnabled = true
                    mesh.setRenderState(wireState)
                } else {
                    mesh.clearRenderState(RenderState.StateType.Wireframe)
                }
            }
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
    onZChange: (Double) -> Unit
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
                onValueChange = onXChange
            )
            // Y
            ComponentField(
                value = y,
                label = "Y",
                modifier = Modifier.weight(1f),
                onValueChange = onYChange
            )
            // Z
            ComponentField(
                value = z,
                label = "Z",
                modifier = Modifier.weight(1f),
                onValueChange = onZChange
            )
        }
    }
}

@Composable
private fun ComponentField(
    value: Double,
    label: String,
    modifier: Modifier = Modifier,
    onValueChange: (Double) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf("") }

    // When not focused, display the live value from the transform
    val displayText = if (isFocused) editText else String.format("%.3f", value)

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
                editText = String.format("%.3f", value)
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
