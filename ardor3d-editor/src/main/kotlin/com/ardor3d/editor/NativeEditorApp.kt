/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.ardor3d.editor.command.AttachChildCommand
import com.ardor3d.editor.command.CompositeCommand
import com.ardor3d.editor.command.DetachChildCommand
import com.ardor3d.editor.command.SetterCommand
import com.ardor3d.editor.util.SelectionUtil
import com.ardor3d.editor.hierarchy.HierarchyPanel
import com.ardor3d.editor.inspector.InspectorPanel
import com.ardor3d.editor.interact.GizmoUndoFilter
import com.ardor3d.editor.menu.EditorMenuBar
import com.ardor3d.editor.menu.ShapeType
import com.ardor3d.editor.ui.EditorTheme
import com.ardor3d.editor.viewport.ViewportStatusBar
import com.ardor3d.editor.viewport.ViewportToolbar
import com.ardor3d.extension.interact.InteractManager
import com.ardor3d.extension.interact.widget.MoveWidget
import com.ardor3d.extension.interact.widget.RotateWidget
import com.ardor3d.extension.interact.widget.SimpleScaleWidget
import com.ardor3d.framework.FrameHandler
import com.ardor3d.framework.Scene
import com.ardor3d.framework.Updater
import com.ardor3d.framework.lwjgl3.Lwjgl3CanvasRenderer
import com.ardor3d.framework.lwjgl3.awt.Lwjgl3AwtCanvas
import com.ardor3d.input.PhysicalLayer
import com.ardor3d.input.awt.AwtFocusWrapper
import com.ardor3d.input.awt.AwtKeyboardWrapper
import com.ardor3d.input.awt.AwtMouseManager
import com.ardor3d.input.awt.AwtMouseWrapper
import com.ardor3d.input.control.FirstPersonControl
import com.ardor3d.input.keyboard.KeyboardWrapper
import com.ardor3d.input.keyboard.Key as ArdorKey
import com.ardor3d.input.logical.InputTrigger
import com.ardor3d.input.logical.KeyPressedCondition
import com.ardor3d.input.logical.LogicalLayer
import com.ardor3d.input.logical.MouseButtonClickedCondition
import com.ardor3d.input.logical.MouseWheelMovedCondition
import com.ardor3d.input.mouse.MouseButton
import com.ardor3d.intersection.PickingUtil
import com.ardor3d.intersection.PrimitivePickResults
import com.ardor3d.light.DirectionalLight
import com.ardor3d.light.Light
import com.ardor3d.light.PointLight
import com.ardor3d.light.SpotLight
import com.ardor3d.extension.model.obj.ObjImporter
import com.ardor3d.math.*
import com.ardor3d.renderer.ContextManager
import com.ardor3d.renderer.Renderer
import com.ardor3d.renderer.material.MaterialManager
import com.ardor3d.renderer.material.reader.YamlMaterialReader
import com.ardor3d.renderer.queue.RenderBucketType
import com.ardor3d.renderer.state.ZBufferState
import com.ardor3d.scenegraph.Line
import com.ardor3d.scenegraph.Mesh
import com.ardor3d.scenegraph.Node
import com.ardor3d.scenegraph.SceneIndexer
import com.ardor3d.scenegraph.Spatial
import com.ardor3d.scenegraph.hint.CullHint
import com.ardor3d.scenegraph.hint.PickingHint
import com.ardor3d.scenegraph.shape.*
import com.ardor3d.util.*
import com.ardor3d.util.export.binary.BinaryExporter
import com.ardor3d.util.export.binary.BinaryImporter
import com.ardor3d.util.resource.URLResourceSource
import com.ardor3d.util.geom.Debugger
import com.ardor3d.util.resource.ResourceLocatorTool
import com.ardor3d.util.resource.SimpleResourceLocator
import org.lwjgl.opengl.awt.GLData
import java.awt.Dimension
import java.util.logging.Level
import java.util.logging.Logger
import javax.swing.Timer as SwingTimer

private val logger = Logger.getLogger("com.ardor3d.editor")

/**
 * Sets up the default resource locators for materials and shaders.
 */
private fun addDefaultResourceLocators() {
    try {
        // Material resources
        ResourceLocatorTool.addResourceLocator(
            ResourceLocatorTool.TYPE_MATERIAL,
            SimpleResourceLocator(
                ResourceLocatorTool.getClassPathResource(MaterialManager::class.java, "com/ardor3d/renderer/material")
            )
        )
        // Shader resources
        ResourceLocatorTool.addResourceLocator(
            ResourceLocatorTool.TYPE_SHADER,
            SimpleResourceLocator(
                ResourceLocatorTool.getClassPathResource(MaterialManager::class.java, "com/ardor3d/renderer/shader")
            )
        )
        // Set default material
        MaterialManager.INSTANCE.defaultMaterial = YamlMaterialReader.load(
            ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_MATERIAL, "basic_white.yaml")
        )
        MaterialManager.INSTANCE.defaultOccluderMaterial = YamlMaterialReader.load(
            ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_MATERIAL, "occluder/basic.yaml")
        )
    } catch (ex: Exception) {
        logger.log(Level.SEVERE, "Failed to set up default resource locators", ex)
    }
}

fun main() = application {
    // Initialize resource locators before anything else. The application block is a
    // composable, so guard against re-running on recomposition.
    remember { addDefaultResourceLocators() }
    val editorState = remember { EditorState() }
    val windowState = rememberWindowState(size = DpSize(1600.dp, 900.dp))

    // Confirm before losing unsaved changes on exit
    val requestExit = {
        if (!editorState.dirty || javax.swing.JOptionPane.showConfirmDialog(
                null,
                "You have unsaved changes. Exit anyway?",
                "Exit",
                javax.swing.JOptionPane.OK_CANCEL_OPTION
            ) == javax.swing.JOptionPane.OK_OPTION
        ) {
            exitApplication()
        }
    }

    Window(
        onCloseRequest = requestExit,
        state = windowState,
        title = "Ardor3D Editor - ${editorState.documentTitle}",
        onPreviewKeyEvent = { event ->
            val modifier = event.isCtrlPressed || event.isMetaPressed
            if (event.type == KeyEventType.KeyDown && modifier) {
                when (event.key) {
                    Key.Z -> {
                        if (event.isShiftPressed) editorState.redo() else editorState.undo()
                        true
                    }

                    Key.Y -> {
                        editorState.redo()
                        true
                    }

                    else -> false
                }
            } else {
                false
            }
        }
    ) {
        EditorTheme {
            NativeEditorApp(editorState, window, requestExit)
        }
    }
}

/**
 * Shows an AWT file dialog owned by [owner]. Returns the chosen file, appending [extension]
 * on save when the user leaves it off.
 */
private fun promptForFile(owner: java.awt.Frame, title: String, save: Boolean, extension: String): java.io.File? {
    val dialog = java.awt.FileDialog(owner, title, if (save) java.awt.FileDialog.SAVE else java.awt.FileDialog.LOAD)
    dialog.setFilenameFilter { _, name -> name.endsWith(".$extension", ignoreCase = true) }
    dialog.isVisible = true
    val fileName = dialog.file ?: return null
    val fixed = if (save && !fileName.endsWith(".$extension", ignoreCase = true)) "$fileName.$extension" else fileName
    return java.io.File(dialog.directory, fixed)
}

/**
 * Asks to proceed when the document has unsaved changes.
 */
private fun confirmDiscard(editorState: EditorState, owner: java.awt.Frame, what: String): Boolean {
    if (!editorState.dirty) {
        return true
    }
    return javax.swing.JOptionPane.showConfirmDialog(
        owner,
        "You have unsaved changes. $what anyway?",
        what,
        javax.swing.JOptionPane.OK_CANCEL_OPTION
    ) == javax.swing.JOptionPane.OK_OPTION
}

@Composable
fun NativeEditorApp(editorState: EditorState, window: java.awt.Frame, requestExit: () -> Unit) {
    val editorScene = remember { EditorScene(editorState) }
    val sceneOperations = remember(editorScene) {
        SceneOperations(
            addShape = editorScene::addShape,
            addLight = editorScene::addLight,
            deleteSpatial = editorScene::deleteSpatial,
            duplicateSpatial = editorScene::duplicateSpatial,
            deleteSelection = editorScene::deleteSelection,
            duplicateSelection = editorScene::duplicateSelection,
            renameSpatial = editorScene::renameSpatial,
            createEmpty = editorScene::createEmptyNode
        )
    }
    val fileOperations = remember(editorScene, window) {
        fun saveTo(file: java.io.File?) {
            (file ?: promptForFile(window, "Save Scene", save = true, extension = "a3d"))
                ?.let(editorScene::saveScene)
        }
        FileOperations(
            newScene = {
                if (confirmDiscard(editorState, window, "New Scene")) {
                    editorScene.newScene()
                }
            },
            openScene = {
                if (confirmDiscard(editorState, window, "Open")) {
                    promptForFile(window, "Open Scene", save = false, extension = "a3d")
                        ?.let(editorScene::openScene)
                }
            },
            saveScene = { saveTo(editorState.currentFile) },
            saveSceneAs = { saveTo(null) },
            importModel = {
                promptForFile(window, "Import OBJ Model", save = false, extension = "obj")
                    ?.let(editorScene::importObjModel)
            },
            exit = requestExit
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Menu bar at top
            EditorMenuBar(
                editorState = editorState,
                operations = sceneOperations,
                fileOperations = fileOperations
            )

            // Main content
            Row(modifier = Modifier.weight(1f)) {
                // Left panel: Hierarchy
                HierarchyPanel(
                    editorState = editorState,
                    operations = sceneOperations,
                    modifier = Modifier
                        .width(250.dp)
                        .fillMaxHeight()
                )

                // Divider
                VerticalDivider()

                // Center: Viewport with embedded Ardor3D canvas
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Toolbar at top
                        ViewportToolbar(
                            editorState = editorState,
                            modifier = Modifier.padding(8.dp)
                        )

                        // Ardor3D Canvas
                        SwingPanel(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            factory = {
                                createArdor3DCanvas(editorScene)
                            }
                        )

                        // Status bar at bottom
                        ViewportStatusBar(
                            editorState = editorState,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

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
}

@Composable
private fun VerticalDivider() {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}

/**
 * Creates the Ardor3D canvas for embedding in Compose.
 */
private fun createArdor3DCanvas(scene: EditorScene): Lwjgl3AwtCanvas {
    val logicalLayer = LogicalLayer()

    val canvasRenderer = Lwjgl3CanvasRenderer(scene)

    val data = GLData().apply {
        majorVersion = 3
        minorVersion = 3
        profile = GLData.Profile.CORE
        forwardCompatible = true
        samples = 4
        depthSize = 24
        stencilSize = 8
    }

    val canvas = Lwjgl3AwtCanvas(data, canvasRenderer)
    canvas.preferredSize = Dimension(800, 600)
    canvas.minimumSize = Dimension(100, 100)

    // Setup input
    val mouseManager = AwtMouseManager(canvas)
    val mouseWrapper = AwtMouseWrapper(canvas, mouseManager)
    val keyboardWrapper = AwtKeyboardWrapper(canvas)
    val focusWrapper = AwtFocusWrapper(canvas)
    val physicalLayer = PhysicalLayer.Builder()
        // Cast disambiguates: AwtKeyboardWrapper is both a KeyboardWrapper and a CharacterInputWrapper
        .with(keyboardWrapper as KeyboardWrapper)
        .with(mouseWrapper)
        .with(focusWrapper)
        .build()

    logicalLayer.registerInput(canvas, physicalLayer)
    scene.logicalLayer = logicalLayer
    scene.canvasRenderer = canvasRenderer
    scene.canvas = canvas

    // Setup interact manager for transform widgets
    scene.setupInteractManager(canvas, physicalLayer, logicalLayer)

    // Setup camera after canvas is realized
    // Note: resize handling is done in render() to ensure correct dimensions
    canvas.addComponentListener(object : java.awt.event.ComponentAdapter() {
        override fun componentResized(e: java.awt.event.ComponentEvent) {
            // Trigger a resize check on next render by resetting lastWidth/lastHeight
            scene.resetLastDimensions()
        }
    })

    // Animation timer - drives the render loop
    val timer = Timer()
    val frameHandler = FrameHandler(timer)
    frameHandler.addCanvas(canvas)
    frameHandler.addUpdater(scene)

    // Use Swing Timer for render loop (runs on EDT)
    // Note: FrameHandler.updateFrame() already calls timer.update() internally
    val renderTimer = SwingTimer(16) { // ~60 FPS
        if (canvas.isVisible && canvas.isDisplayable) {
            frameHandler.updateFrame()
        }
    }
    renderTimer.start()

    return canvas
}

/**
 * Editor scene that wraps the scene root and provides the Scene interface for Ardor3D.
 */
class EditorScene(private val editorState: EditorState) : Scene, Updater {
    // Use editorState's sceneRoot directly so inspector changes are reflected.
    // This is the document: what the user edits (and, later, saves).
    private val root: Node get() = editorState.sceneRoot

    // Editor-only visuals (grid, light gizmos) live outside the document so
    // they never appear in the hierarchy, picking, or saved scenes.
    private val overlayRoot = Node("EditorOverlay")
    private val gridNode = createGrid()

    // One overlay gizmo per light in the document, refreshed on structure changes.
    private val lightGizmos = mutableMapOf<Light, Node>()
    private var lastStructureVersion = -1L

    // Document lifecycle actions deferred to update() so they run with the GL context current.
    private val pendingActions = java.util.concurrent.ConcurrentLinkedQueue<() -> Unit>()

    // Frame counting for the status bar FPS display
    private var fpsAccumulatedTime = 0.0
    private var fpsFrameCount = 0

    var logicalLayer: LogicalLayer? = null
        set(value) {
            field = value
            // Setup FPS camera controls when logical layer is available
            value?.let { layer ->
                firstPersonControl = FirstPersonControl.setupTriggers(layer, Vector3.UNIT_Y, true)
                firstPersonControl?.moveSpeed = 10.0  // Slower for editor use
                firstPersonControl?.isClampVerticalAngle = true

                // Setup mouse click for object picking
                setupPickingTrigger(layer)
            }
        }
    var canvasRenderer: Lwjgl3CanvasRenderer? = null
    var canvas: Lwjgl3AwtCanvas? = null
    private var firstPersonControl: FirstPersonControl? = null
    private var initialized = false
    private var lastWidth = 0
    private var lastHeight = 0

    // Interact manager and widgets for transform gizmos
    private var interactManager: InteractManager? = null
    private var moveWidget: MoveWidget? = null
    private var rotateWidget: RotateWidget? = null
    private var scaleWidget: SimpleScaleWidget? = null
    private var lastTransformMode: TransformMode? = null

    // Reconciliation caches used by update() to notify the UI only on real changes.
    private var lastSelection: Spatial? = null
    private val lastSelectedTransform = Transform()
    private var transformCacheValid = false

    fun resetLastDimensions() {
        lastWidth = 0
        lastHeight = 0
    }

    /**
     * Sets up the interact manager and transform widgets.
     */
    fun setupInteractManager(canvas: Lwjgl3AwtCanvas, physicalLayer: PhysicalLayer, logicalLayer: LogicalLayer) {
        interactManager = InteractManager()
        interactManager?.setupInput(canvas, physicalLayer, logicalLayer)

        // Create transform widgets
        moveWidget = MoveWidget().withXAxis().withYAxis().withZAxis()
        rotateWidget = RotateWidget().withXAxis().withYAxis().withZAxis()
        // SimpleScaleWidget only supports one axis - use Y for uniform scaling
        scaleWidget = SimpleScaleWidget().withArrow(Vector3.UNIT_Y, ColorRGBA(0f, 1f, 0f, 0.65f))

        interactManager?.addWidget(moveWidget)
        interactManager?.addWidget(rotateWidget)
        interactManager?.addWidget(scaleWidget)

        // Record completed gizmo drags in the undo history
        interactManager?.addFilterToWidgets(GizmoUndoFilter(editorState))

        // Set default widget based on editor state
        updateActiveWidget()
    }

    /**
     * Updates the active widget based on the current transform mode.
     */
    fun updateActiveWidget() {
        val widget = when (editorState.transformMode) {
            TransformMode.TRANSLATE -> moveWidget
            TransformMode.ROTATE -> rotateWidget
            TransformMode.SCALE -> scaleWidget
        }
        interactManager?.setActiveWidget(widget)
    }

    /**
     * Updates the interact manager's target to match the editor selection. The scene root
     * itself never gets a gizmo.
     */
    private fun updateInteractTarget() {
        val selected = editorState.primarySelection
        if (selected != null && selected.parent != null) {
            interactManager?.setSpatialTarget(selected)
        } else {
            interactManager?.setSpatialTarget(null)
        }
    }

    // Counters for generating unique object names, keyed by type name
    private val shapeCounters = mutableMapOf<String, Int>()

    /**
     * Adds a new shape to the scene.
     */
    fun addShape(shapeType: ShapeType) {
        val count = shapeCounters.merge(shapeType.name, 1, Int::plus)!!

        val shape: Mesh = when (shapeType) {
            ShapeType.BOX -> Box("Box$count", Vector3.ZERO, 0.5, 0.5, 0.5)
            ShapeType.SPHERE -> Sphere("Sphere$count", 16, 16, 0.5)
            ShapeType.PLANE -> Quad("Plane$count", 2.0, 2.0).apply {
                // Rotate to lay flat on XZ plane (face up)
                setRotation(Quaternion().fromAngleAxis(-Math.PI / 2, Vector3.UNIT_X))
            }
            ShapeType.CYLINDER -> Cylinder("Cylinder$count", 8, 16, 0.5, 1.0, true)
            ShapeType.CONE -> Cone("Cone$count", 16, 16, 0.5, 1.0, true)
            ShapeType.CAPSULE -> Capsule("Capsule$count", 8, 8, 8, 0.25, 1.0)
            ShapeType.TORUS -> Torus("Torus$count", 16, 16, 0.2, 0.5)
            ShapeType.PYRAMID -> Pyramid("Pyramid$count", 1.0, 1.0)
            ShapeType.TEAPOT -> Teapot("Teapot$count")
            ShapeType.DOME -> Dome("Dome$count", 16, 16, 0.5)
            ShapeType.DISK -> Disk("Disk$count", 8, 16, 0.5)
            ShapeType.TUBE -> Tube("Tube$count", 0.3, 0.5, 1.0, 16, 8)
            ShapeType.GEOSPHERE -> GeoSphere("GeoSphere$count", true, 0.5, 2, GeoSphere.TextureMode.Original)
            ShapeType.DODECAHEDRON -> Dodecahedron("Dodecahedron$count", 0.5)
            ShapeType.ICOSAHEDRON -> Icosahedron("Icosahedron$count", 0.5)
            ShapeType.OCTAHEDRON -> Octahedron("Octahedron$count", 0.5)
        }

        // Position slightly above the grid
        shape.setTranslation(0.0, 0.5, 0.0)

        // Ensure it has a bounding volume for picking
        shape.modelBound = com.ardor3d.bounding.BoundingBox()
        shape.updateModelBound()

        // Materials are assigned once here; re-attaching on redo keeps them.
        MaterialUtil.autoMaterials(shape)

        editorState.execute(
            AttachChildCommand(
                parent = root,
                child = shape,
                onExecuted = { editorState.select(shape) },
                onUndone = { if (editorState.isSelected(shape)) editorState.clearSelection() }
            )
        )
    }

    /**
     * Adds a new light to the scene (undoable) and selects it.
     */
    fun addLight(lightType: LightType) {
        val count = shapeCounters.merge(lightType.name, 1, Int::plus)!!
        val light: Light = when (lightType) {
            LightType.POINT -> PointLight().apply {
                name = "Point Light $count"
                setTranslation(0.0, 3.0, 0.0)
            }

            LightType.DIRECTIONAL -> DirectionalLight().apply {
                name = "Directional Light $count"
                setTranslation(0.0, 5.0, 0.0)
                // Aim down toward the scene by default
                setRotation(Quaternion().fromAngleAxis(-Math.PI / 2, Vector3.UNIT_X))
            }

            LightType.SPOT -> SpotLight().apply {
                name = "Spot Light $count"
                setTranslation(0.0, 5.0, 0.0)
                setRotation(Quaternion().fromAngleAxis(-Math.PI / 2, Vector3.UNIT_X))
            }
        }
        light.color = ColorRGBA.WHITE
        light.intensity = 1.0f
        light.isEnabled = true

        editorState.execute(
            AttachChildCommand(
                parent = root,
                child = light,
                name = "Add ${light.name}",
                onExecuted = { editorState.select(light) },
                onUndone = { if (editorState.isSelected(light)) editorState.clearSelection() }
            )
        )
        editorState.sealUndoMerge()
    }

    init {
        // Setup ZBufferState for depth testing on both roots
        for (node in listOf(root, overlayRoot)) {
            val zBuffer = ZBufferState()
            zBuffer.isEnabled = true
            zBuffer.function = ZBufferState.TestFunction.LessThanOrEqualTo
            node.setRenderState(zBuffer)
            node.sceneHints.renderBucketType = RenderBucketType.Opaque
        }

        // Editor overlay: grid (light gizmos are added per light as the scene changes)
        gridNode.sceneHints.setPickingHint(PickingHint.Pickable, false)
        overlayRoot.attachChild(gridNode)

        populateDefaultScene()

        MaterialUtil.autoMaterials(root)
        MaterialUtil.autoMaterials(overlayRoot)
        refreshLightGizmos()
    }

    /**
     * Fills the document with the default starter content (a cube and a light).
     */
    private fun populateDefaultScene() {
        root.attachChild(createTestCube())

        val light = PointLight()
        light.name = "Main Light"
        light.color = ColorRGBA.WHITE
        light.intensity = 0.75f
        light.setTranslation(5.0, 8.0, 5.0)
        light.isEnabled = true
        root.attachChild(light)
    }

    /**
     * Collects all lights in the document.
     */
    private fun collectLights(spatial: Spatial, into: MutableList<Light>) {
        if (spatial is Light) {
            into.add(spatial)
        }
        if (spatial is Node) {
            for (child in spatial.children) {
                collectLights(child, into)
            }
        }
    }

    /**
     * Keeps one overlay gizmo per light in the document.
     */
    private fun refreshLightGizmos() {
        val lights = mutableListOf<Light>()
        collectLights(root, lights)

        val stale = lightGizmos.keys - lights.toSet()
        for (light in stale) {
            lightGizmos.remove(light)?.let(overlayRoot::detachChild)
        }
        for (light in lights) {
            lightGizmos.getOrPut(light) {
                val gizmo = createLightGizmo()
                overlayRoot.attachChild(gizmo)
                MaterialUtil.autoMaterials(gizmo)
                gizmo
            }
        }
    }

    /**
     * Sets up mouse click trigger for object picking.
     */
    private fun setupPickingTrigger(layer: LogicalLayer) {
        // Use MouseButtonClickedCondition - fires on release when it's a proper click (not drag)
        val clickCondition = MouseButtonClickedCondition(MouseButton.LEFT)

        val pickTrigger = InputTrigger(clickCondition) { source, inputStates, tpf ->
            val camera = canvasRenderer?.camera ?: return@InputTrigger
            val mouseState = inputStates.current?.mouseState ?: return@InputTrigger

            // Get pick ray from mouse position
            val screenPos = Vector2(mouseState.x.toDouble(), mouseState.y.toDouble())
            val pickRay = camera.getPickRay(screenPos, false, null)

            // Perform pick against actual primitives so clicks near (but not on)
            // an object don't select it
            val results = PrimitivePickResults()
            results.setCheckDistance(true)
            PickingUtil.findPick(root, pickRay, results)

            // Ctrl-click toggles membership in the selection instead of replacing it
            val keyboard = inputStates.current.keyboardState
            val toggle = keyboard.isDown(ArdorKey.LEFT_CONTROL) || keyboard.isDown(ArdorKey.RIGHT_CONTROL) ||
                keyboard.isDown(ArdorKey.LEFT_META) || keyboard.isDown(ArdorKey.RIGHT_META)

            if (results.number > 0) {
                // Get the closest picked object
                val pickData = results.getPickData(0)
                val picked = pickData.target as? Spatial
                if (picked != null) {
                    if (toggle) {
                        editorState.toggleSelection(picked)
                    } else {
                        editorState.select(picked)
                    }
                }
            } else if (!toggle) {
                // Clicked on nothing - clear selection
                editorState.clearSelection()
            }
        }

        layer.registerTrigger(pickTrigger)

        // Delete key removes the current selection (canvas-focused only)
        val deleteTrigger = InputTrigger(KeyPressedCondition(ArdorKey.DELETE)) { _, _, _ ->
            deleteSelection()
        }
        layer.registerTrigger(deleteTrigger)

        // F frames the selection (or the whole scene)
        layer.registerTrigger(InputTrigger(KeyPressedCondition(ArdorKey.F)) { _, _, _ ->
            frameSelection()
        })

        // Mouse wheel dollies the camera along its view direction
        layer.registerTrigger(InputTrigger(MouseWheelMovedCondition()) { _, inputStates, _ ->
            val camera = canvasRenderer?.camera ?: return@InputTrigger
            val clicks = inputStates.current.mouseState.dwheel
            if (clicks != 0) {
                val move = Vector3(camera.direction).multiplyLocal(clicks * 0.75)
                camera.location = Vector3(camera.location).addLocal(move)
            }
        })

        // 1/2/3 switch transform mode (matching the viewport toolbar)
        val modeKeys = listOf(
            ArdorKey.ONE to TransformMode.TRANSLATE,
            ArdorKey.TWO to TransformMode.ROTATE,
            ArdorKey.THREE to TransformMode.SCALE
        )
        for ((key, mode) in modeKeys) {
            layer.registerTrigger(InputTrigger(KeyPressedCondition(key)) { _, _, _ ->
                editorState.transformMode = mode
            })
        }
    }

    /**
     * Moves the camera to frame the current selection, or the whole scene when nothing is
     * selected.
     */
    fun frameSelection() {
        val camera = canvasRenderer?.camera ?: return
        val target = editorState.primarySelection ?: root
        val bound = target.worldBound
        val center = Vector3(bound?.center ?: target.worldTranslation)
        val radius = when (bound) {
            is com.ardor3d.bounding.BoundingSphere -> bound.radius
            is com.ardor3d.bounding.BoundingBox ->
                Vector3(bound.xExtent, bound.yExtent, bound.zExtent).length()

            else -> 5.0
        }.coerceAtLeast(0.5)
        val newLocation = Vector3(center).subtractLocal(Vector3(camera.direction).multiplyLocal(radius * 3.0))
        camera.location = newLocation
        camera.lookAt(center, Vector3.UNIT_Y)
    }

    /**
     * Deletes the given spatial from the document (undoable). The scene root cannot be deleted.
     */
    fun deleteSpatial(spatial: Spatial) {
        val parent = spatial.parent ?: return
        editorState.execute(
            DetachChildCommand(
                parent = parent,
                child = spatial,
                onExecuted = { if (editorState.isSelected(spatial)) editorState.clearSelection() },
                onUndone = { editorState.select(spatial) }
            )
        )
        editorState.sealUndoMerge()
    }

    /**
     * Deletes every selected spatial as one undo step. The scene root is never deleted, and a
     * spatial whose selected ancestor already covers it is skipped.
     */
    fun deleteSelection() {
        val targets = SelectionUtil.topMost(editorState.selection).filter { it.parent != null }
        when {
            targets.isEmpty() -> return
            targets.size == 1 -> deleteSpatial(targets[0])
            else -> {
                editorState.execute(
                    CompositeCommand(
                        name = "Delete ${targets.size} objects",
                        commands = targets.map { DetachChildCommand(parent = it.parent!!, child = it) },
                        onExecuted = { editorState.clearSelection() },
                        onUndone = { editorState.selectAll(targets) }
                    )
                )
                editorState.sealUndoMerge()
            }
        }
    }

    /**
     * Duplicates the given spatial as a sibling (undoable) and selects the copy.
     */
    fun duplicateSpatial(spatial: Spatial) {
        val parent = spatial.parent ?: return
        val copy = spatial.makeCopy(true)
        copy.name = "${spatial.name ?: "object"} Copy"
        MaterialUtil.autoMaterials(copy)
        editorState.execute(
            AttachChildCommand(
                parent = parent,
                child = copy,
                name = "Duplicate ${spatial.name ?: "object"}",
                onExecuted = { editorState.select(copy) },
                onUndone = { if (editorState.isSelected(copy)) editorState.clearSelection() }
            )
        )
        editorState.sealUndoMerge()
    }

    /**
     * Duplicates every selected spatial as one undo step and selects the copies.
     */
    fun duplicateSelection() {
        val targets = SelectionUtil.topMost(editorState.selection).filter { it.parent != null }
        when {
            targets.isEmpty() -> return
            targets.size == 1 -> duplicateSpatial(targets[0])
            else -> {
                val copies = targets.map { spatial ->
                    val copy = spatial.makeCopy(true)
                    copy.name = "${spatial.name ?: "object"} Copy"
                    MaterialUtil.autoMaterials(copy)
                    spatial.parent!! to copy
                }
                editorState.execute(
                    CompositeCommand(
                        name = "Duplicate ${targets.size} objects",
                        commands = copies.map { (parent, copy) -> AttachChildCommand(parent = parent, child = copy) },
                        onExecuted = { editorState.selectAll(copies.map { it.second }) },
                        onUndone = {
                            if (copies.any { editorState.isSelected(it.second) }) {
                                editorState.clearSelection()
                            }
                        }
                    )
                )
                editorState.sealUndoMerge()
            }
        }
    }

    /**
     * Renames the given spatial (undoable).
     */
    fun renameSpatial(spatial: Spatial, newName: String) {
        val oldName = spatial.name ?: ""
        if (oldName == newName) {
            return
        }
        editorState.execute(
            SetterCommand(
                name = "Rename",
                oldValue = oldName,
                newValue = newName,
                affectsStructure = true,
                setter = { spatial.name = it }
            )
        )
        editorState.sealUndoMerge()
    }

    /**
     * Replaces the current document with a fresh default scene.
     */
    fun newScene() {
        pendingActions.add {
            installDocumentRoot(Node("Scene Root"))
            populateDefaultScene()
            MaterialUtil.autoMaterials(root)
            refreshLightGizmos()
            editorState.markSaved(null)
        }
    }

    /**
     * Writes the document to [file] using Ardor3D's binary format.
     */
    fun saveScene(file: java.io.File) {
        pendingActions.add {
            try {
                BinaryExporter().save(root, file)
                editorState.markSaved(file)
            } catch (ex: Exception) {
                logger.log(Level.SEVERE, "Failed to save scene to $file", ex)
            }
        }
    }

    /**
     * Loads a binary scene file as the new document.
     */
    fun openScene(file: java.io.File) {
        pendingActions.add {
            try {
                val loaded = BinaryImporter().load(file) as? Node
                if (loaded == null) {
                    logger.severe("$file did not contain a scene root Node")
                    return@add
                }
                installDocumentRoot(loaded)
                editorState.markSaved(file)
            } catch (ex: Exception) {
                logger.log(Level.SEVERE, "Failed to open scene $file", ex)
            }
        }
    }

    /**
     * Imports a Wavefront OBJ file as a child of the scene root (undoable).
     */
    fun importObjModel(file: java.io.File) {
        pendingActions.add {
            try {
                val importer = ObjImporter().setLoadTextures(true)
                file.parentFile?.let { importer.setTextureLocator(SimpleResourceLocator(it.toURI())) }
                val imported = importer.load(URLResourceSource(file.toURI().toURL())).scene
                imported.name = file.name.substringBeforeLast('.')
                // Make sure everything is pickable
                imported.acceptVisitor({ spatial ->
                    if (spatial is Mesh && spatial.modelBound == null) {
                        spatial.setModelBound(com.ardor3d.bounding.BoundingBox())
                    }
                }, false)
                MaterialUtil.autoMaterials(imported)
                imported.updateGeometricState(0.0, true)
                editorState.execute(
                    AttachChildCommand(
                        parent = root,
                        child = imported,
                        name = "Import ${file.name}",
                        onExecuted = { editorState.select(imported) },
                        onUndone = { if (editorState.isSelected(imported)) editorState.clearSelection() }
                    )
                )
                editorState.sealUndoMerge()
            } catch (ex: Exception) {
                logger.log(Level.SEVERE, "Failed to import model $file", ex)
            }
        }
    }

    /**
     * Swaps the document root, moving SceneIndexer registration and resetting editor caches.
     */
    private fun installDocumentRoot(newRoot: Node) {
        val oldRoot = root
        if (initialized) {
            SceneIndexer.getCurrent()?.removeSceneRoot(oldRoot)
        }

        val zBuffer = ZBufferState()
        zBuffer.isEnabled = true
        zBuffer.function = ZBufferState.TestFunction.LessThanOrEqualTo
        newRoot.setRenderState(zBuffer)
        newRoot.sceneHints.renderBucketType = RenderBucketType.Opaque

        editorState.replaceSceneRoot(newRoot)
        MaterialUtil.autoMaterials(newRoot)
        if (initialized) {
            SceneIndexer.getCurrent()?.addSceneRoot(newRoot)
        }

        lastStructureVersion = editorState.structureVersion
        refreshLightGizmos()
        interactManager?.setSpatialTarget(null)
        lastSelection = null
        transformCacheValid = false
        newRoot.updateGeometricState(0.0, true)
    }

    /**
     * Creates an empty Node under the selected node (or the scene root) and selects it.
     */
    fun createEmptyNode() {
        val parent = (editorState.primarySelection as? Node) ?: root
        val node = Node("Empty")
        editorState.execute(
            AttachChildCommand(
                parent = parent,
                child = node,
                name = "Create Empty",
                onExecuted = { editorState.select(node) },
                onUndone = { if (editorState.isSelected(node)) editorState.clearSelection() }
            )
        )
        editorState.sealUndoMerge()
    }

    fun setupCamera() {
        val camera = canvasRenderer?.camera ?: return
        // Position camera to look at the scene from a nice angle
        camera.setLocation(Vector3(8.0, 6.0, 12.0))
        camera.lookAt(Vector3.ZERO, Vector3.UNIT_Y)
    }

    override fun render(renderer: Renderer): Boolean {
        // Setup on first render (GL context now available)
        if (!initialized) {
            setupCamera()

            // Set background color - nice dark blue-gray
            renderer.setBackgroundColor(ColorRGBA(0.15f, 0.16f, 0.2f, 1f))

            // Register with scene indexer now that context exists
            SceneIndexer.getCurrent()?.addSceneRoot(root)
            initialized = true
        }

        // Execute updateQueue item
        GameTaskQueueManager.getManager(ContextManager.getCurrentContext()).getQueue(GameTaskQueue.RENDER).execute(renderer)

        // Clean up card garbage such as textures, vbos, etc.
        ContextGarbageCollector.doRuntimeCleanup(renderer)

        // Check if canvas size changed and update camera frustum
        val camera = canvasRenderer?.camera
        val cvs = canvas
        if (camera != null && cvs != null) {
            // getContentWidth/Height now handle DPI scaling via AwtDpiScaler
            val w = cvs.contentWidth
            val h = cvs.contentHeight

            if (w > 0 && h > 0 && (w != lastWidth || h != lastHeight)) {
                lastWidth = w
                lastHeight = h
                camera.resize(w, h)
                camera.setFrustumPerspective(
                    45.0,
                    w.toDouble() / h.toDouble(),
                    0.1,
                    1000.0
                )
            }
        }

        // Let scene indexer do its thing (shadows, etc.)
        SceneIndexer.getCurrent()?.onRender(renderer)

        renderer.draw(root)
        renderer.draw(overlayRoot)

        // Render interact widgets (transform gizmos)
        interactManager?.render(renderer)

        // Draw selection highlight (bounding box) for selected objects. The gizmo target
        // already has a widget on it, so skip its bounds.
        val gizmoTarget = interactManager?.spatialTarget
        for (selected in editorState.selection) {
            if (selected !== gizmoTarget) {
                Debugger.drawBounds(selected, renderer, false)
            }
        }

        return true
    }

    override fun doPick(pickRay: Ray3): PrimitivePickResults {
        val results = PrimitivePickResults()
        results.setCheckDistance(true)
        PickingUtil.findPick(root, pickRay, results)
        return results
    }

    override fun init() {
        // Initialization for the updater
    }

    override fun update(timer: ReadOnlyTimer) {
        // Run deferred document actions (new/open/save/import) with the context current
        while (true) {
            val action = pendingActions.poll() ?: break
            action()
        }

        // Execute updateQueue item
        GameTaskQueueManager.getManager(ContextManager.getCurrentContext()).getQueue(GameTaskQueue.UPDATE).execute()

        // Check if transform mode changed and update active widget
        if (lastTransformMode != editorState.transformMode) {
            lastTransformMode = editorState.transformMode
            updateActiveWidget()
        }

        interactManager?.let {
            it.logicalLayer.checkTriggers(timer.timePerFrame)
            it.update(timer)
        } ?: logicalLayer?.checkTriggers(timer.timePerFrame)

        // Sync the gizmo target with the editor selection (which may have been
        // changed by the hierarchy panel, picking, or programmatically).
        if (lastSelection !== editorState.primarySelection) {
            lastSelection = editorState.primarySelection
            transformCacheValid = false
            updateInteractTarget()
        }

        // Notify the UI when the selected spatial's transform actually changed
        // (e.g. dragged by a gizmo) so the inspector refreshes.
        val selected = lastSelection
        if (selected != null) {
            if (!transformCacheValid || lastSelectedTransform != selected.transform) {
                lastSelectedTransform.set(selected.transform)
                transformCacheValid = true
                editorState.notifyTransformChanged()
            }
        }

        // Re-resolve light gizmos when the structure changed (add, delete, open, etc.)
        if (lastStructureVersion != editorState.structureVersion) {
            lastStructureVersion = editorState.structureVersion
            refreshLightGizmos()
        }

        // Keep light gizmos over their lights
        for ((light, gizmo) in lightGizmos) {
            gizmo.setTranslation(light.worldTranslation)
        }

        // Update geometric state for the document and the editor overlay
        root.updateGeometricState(timer.timePerFrame, true)
        overlayRoot.updateGeometricState(timer.timePerFrame, true)

        // Status bar FPS, refreshed twice a second
        fpsAccumulatedTime += timer.timePerFrame
        fpsFrameCount++
        if (fpsAccumulatedTime >= 0.5) {
            editorState.framesPerSecond = (fpsFrameCount / fpsAccumulatedTime).toInt()
            fpsAccumulatedTime = 0.0
            fpsFrameCount = 0
        }
    }

    /**
     * Creates an editor grid on the XZ plane.
     */
    private fun createGrid(): Node {
        val gridNode = Node("EditorGrid")

        val gridLines = 21  // Number of lines (odd for center line)
        val gridSpacing = 1.0f  // 1 unit spacing
        val gridSize = (gridLines - 1) / 2 * gridSpacing  // Half-size of grid

        // Create vertices for grid lines
        val vertices = mutableListOf<Vector3>()

        for (i in 0 until gridLines) {
            val coord = (i - gridLines / 2) * gridSpacing
            // Lines along X axis
            vertices.add(Vector3(-gridSize.toDouble(), 0.0, coord.toDouble()))
            vertices.add(Vector3(gridSize.toDouble(), 0.0, coord.toDouble()))
            // Lines along Z axis
            vertices.add(Vector3(coord.toDouble(), 0.0, -gridSize.toDouble()))
            vertices.add(Vector3(coord.toDouble(), 0.0, gridSize.toDouble()))
        }

        val grid = Line("Grid", vertices.toTypedArray(), null, null, null)
        grid.setDefaultColor(ColorRGBA(0.35f, 0.35f, 0.4f, 1f))  // Subtle gray
        grid.lineWidth = 1.0f
        gridNode.attachChild(grid)

        // Add colored axis lines (X=red, Z=blue) at center
        val axisLength = gridSize.toDouble() + 0.5

        // X axis (red)
        val xAxisVerts = arrayOf(
            Vector3(-axisLength, 0.01, 0.0),
            Vector3(axisLength, 0.01, 0.0)
        )
        val xAxis = Line("XAxis", xAxisVerts, null, null, null)
        xAxis.setDefaultColor(ColorRGBA(0.8f, 0.2f, 0.2f, 1f))
        xAxis.lineWidth = 2.0f
        gridNode.attachChild(xAxis)

        // Z axis (blue)
        val zAxisVerts = arrayOf(
            Vector3(0.0, 0.01, -axisLength),
            Vector3(0.0, 0.01, axisLength)
        )
        val zAxis = Line("ZAxis", zAxisVerts, null, null, null)
        zAxis.setDefaultColor(ColorRGBA(0.2f, 0.2f, 0.8f, 1f))
        zAxis.lineWidth = 2.0f
        gridNode.attachChild(zAxis)

        return gridNode
    }

    /**
     * Creates a test cube for the scene.
     */
    private fun createTestCube(): Box {
        val cube = Box("TestCube", Vector3.ZERO, 1.0, 1.0, 1.0)
        cube.setTranslation(0.0, 1.0, 0.0)
        cube.modelBound = com.ardor3d.bounding.BoundingBox()
        return cube
    }

    /**
     * Creates a visual gizmo to show light position.
     */
    private fun createLightGizmo(): Node {
        val gizmoNode = Node("LightGizmo")

        // Small yellow sphere to represent light
        val sphere = Sphere("LightSphere", 8, 8, 0.3)
        sphere.setDefaultColor(ColorRGBA(1f, 0.9f, 0.3f, 1f))  // Yellow
        gizmoNode.attachChild(sphere)

        // Add rays emanating from the light to make it more visible
        val rayLength = 0.6
        val rayColor = ColorRGBA(1f, 0.95f, 0.5f, 0.7f)

        // Create 6 rays pointing in cardinal directions
        val directions = listOf(
            Vector3.UNIT_X, Vector3.NEG_UNIT_X,
            Vector3.UNIT_Y, Vector3.NEG_UNIT_Y,
            Vector3.UNIT_Z, Vector3.NEG_UNIT_Z
        )

        for ((index, dir) in directions.withIndex()) {
            val rayVerts = arrayOf(
                Vector3.ZERO,
                Vector3(dir).multiplyLocal(rayLength)
            )
            val ray = Line("LightRay$index", rayVerts, null, null, null)
            ray.setDefaultColor(rayColor)
            ray.lineWidth = 2.0f
            gizmoNode.attachChild(ray)
        }

        return gizmoNode
    }
}
