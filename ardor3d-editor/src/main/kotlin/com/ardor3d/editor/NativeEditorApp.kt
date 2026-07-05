package com.ardor3d.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.ardor3d.editor.hierarchy.HierarchyPanel
import com.ardor3d.editor.inspector.InspectorPanel
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
import com.ardor3d.input.logical.InputTrigger
import com.ardor3d.input.logical.LogicalLayer
import com.ardor3d.input.logical.MouseButtonClickedCondition
import com.ardor3d.input.mouse.MouseButton
import com.ardor3d.intersection.BoundingPickResults
import com.ardor3d.intersection.PickingUtil
import com.ardor3d.light.PointLight
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
import com.ardor3d.scenegraph.hint.PickingHint
import com.ardor3d.scenegraph.shape.*
import com.ardor3d.util.*
import com.ardor3d.util.geom.Debugger
import com.ardor3d.util.resource.ResourceLocatorTool
import com.ardor3d.util.resource.SimpleResourceLocator
import org.lwjgl.opengl.awt.GLData
import java.awt.Dimension
import javax.swing.Timer as SwingTimer

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
        ex.printStackTrace()
    }
}

fun main() = application {
    // Initialize resource locators before anything else
    addDefaultResourceLocators()
    val windowState = rememberWindowState(size = DpSize(1600.dp, 900.dp))

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "Ardor3D Editor"
    ) {
        EditorTheme {
            NativeEditorApp()
        }
    }
}

@Composable
fun NativeEditorApp() {
    val editorState = remember { EditorState() }
    val editorScene = remember { EditorScene(editorState) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Menu bar at top
            EditorMenuBar(
                onAddShape = { shapeType -> editorScene.addShape(shapeType) }
            )

            // Main content
            Row(modifier = Modifier.weight(1f)) {
                // Left panel: Hierarchy
                HierarchyPanel(
                    editorState = editorState,
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
    // Use editorState's sceneRoot directly so inspector changes are reflected
    private val root: Node get() = editorState.sceneRoot
    private val gridNode = createGrid()
    private val testCube = createTestCube()
    private val light = PointLight()
    private val lightGizmo = createLightGizmo()

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
     * Updates the interact manager's target to match the editor selection.
     */
    fun updateInteractTarget() {
        val selected = editorState.primarySelection
        if (selected != null && selected != gridNode && selected != lightGizmo) {
            interactManager?.setSpatialTarget(selected)
        } else {
            interactManager?.setSpatialTarget(null)
        }
    }

    // Counter for generating unique shape names
    private val shapeCounters = mutableMapOf<ShapeType, Int>()

    /**
     * Adds a new shape to the scene.
     */
    fun addShape(shapeType: ShapeType) {
        val count = shapeCounters.getOrDefault(shapeType, 0) + 1
        shapeCounters[shapeType] = count

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

        // Add to scene
        root.attachChild(shape)
        MaterialUtil.autoMaterials(shape)

        // Select the new shape
        editorState.select(shape)
        updateInteractTarget()
    }

    init {
        // Setup ZBufferState for depth testing
        val zBuffer = ZBufferState()
        zBuffer.isEnabled = true
        zBuffer.function = ZBufferState.TestFunction.LessThanOrEqualTo
        root.setRenderState(zBuffer)

        // Setup render bucket
        root.sceneHints.renderBucketType = RenderBucketType.Opaque

        // Add grid (not pickable)
        gridNode.sceneHints.setPickingHint(PickingHint.Pickable, false)
        root.attachChild(gridNode)

        // Add test cube
        root.attachChild(testCube)

        // Setup light
        light.name = "Main Light"
        light.color = ColorRGBA.WHITE
        light.intensity = 0.75f
        light.setTranslation(5.0, 8.0, 5.0)
        light.isEnabled = true
        root.attachChild(light)

        // Add light gizmo visualization (not pickable by default, but can be made pickable)
        lightGizmo.sceneHints.setPickingHint(PickingHint.Pickable, false)
        root.attachChild(lightGizmo)

        MaterialUtil.autoMaterials(root)
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

            // Perform pick
            val results = BoundingPickResults()
            results.setCheckDistance(true)
            PickingUtil.findPick(root, pickRay, results)

            if (results.number > 0) {
                // Get the closest picked object
                val pickData = results.getPickData(0)
                val picked = pickData.target as? Spatial
                if (picked != null && picked != gridNode && picked != lightGizmo) {
                    editorState.select(picked)
                    updateInteractTarget()
                }
            } else {
                // Clicked on nothing - clear selection
                editorState.clearSelection()
                updateInteractTarget()
            }
        }

        layer.registerTrigger(pickTrigger)
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

        // Render interact widgets (transform gizmos)
        interactManager?.render(renderer)

        // Draw selection highlight (bounding box) for selected objects (only if no gizmo active)
        if (interactManager?.spatialTarget == null) {
            for (selected in editorState.selection) {
                if (selected != gridNode && selected != lightGizmo) {
                    Debugger.drawBounds(selected, renderer, false)
                }
            }
        }

        Debugger.drawBounds(root, renderer, true)

        return true
    }

    override fun doPick(pickRay: Ray3): BoundingPickResults {
        val results = BoundingPickResults()
        results.setCheckDistance(true)
        PickingUtil.findPick(root, pickRay, results)
        return results
    }

    override fun init() {
        // Initialization for the updater
    }

    override fun update(timer: ReadOnlyTimer) {
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

        // Update light gizmo position to match light
        lightGizmo.setTranslation(light.translation)

        // Update geometric state for all scene objects
        root.updateGeometricState(timer.timePerFrame, true)

        // Notify UI to refresh transform values (for live animation display)
        editorState.notifyTransformChanged()
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
