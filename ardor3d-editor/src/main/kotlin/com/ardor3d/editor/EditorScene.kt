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

import com.ardor3d.editor.command.AttachChildCommand
import com.ardor3d.editor.command.CompositeCommand
import com.ardor3d.editor.command.DetachChildCommand
import com.ardor3d.editor.command.ReorderChildCommand
import com.ardor3d.editor.command.ReparentCommand
import com.ardor3d.editor.command.SetterCommand
import com.ardor3d.editor.io.ModelImport
import com.ardor3d.editor.util.Insertion
import com.ardor3d.editor.util.SelectionUtil
import com.ardor3d.editor.interact.GizmoUndoFilter
import com.ardor3d.editor.menu.ShapeType
import com.ardor3d.extension.interact.InteractManager
import com.ardor3d.extension.interact.widget.MoveWidget
import com.ardor3d.extension.interact.widget.RotateWidget
import com.ardor3d.extension.interact.widget.SimpleScaleWidget
import com.ardor3d.framework.Scene
import com.ardor3d.framework.Updater
import com.ardor3d.framework.lwjgl3.Lwjgl3CanvasRenderer
import com.ardor3d.framework.lwjgl3.awt.Lwjgl3AwtCanvas
import com.ardor3d.input.PhysicalLayer
import com.ardor3d.input.control.FirstPersonControl
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
import com.ardor3d.math.*
import com.ardor3d.renderer.ContextManager
import com.ardor3d.renderer.Renderer
import com.ardor3d.renderer.queue.RenderBucketType
import com.ardor3d.renderer.state.ZBufferState
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
import com.ardor3d.util.geom.Debugger
import java.util.logging.Level
import java.util.logging.Logger

private val logger = Logger.getLogger("com.ardor3d.editor")

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
                parent = creationParent(),
                child = shape,
                onExecuted = { editorState.select(shape) },
                onUndone = { if (editorState.isSelected(shape)) editorState.clearSelection() }
            )
        )
    }

    /**
     * New objects are created under the selected Node (matching Create Empty), or the scene
     * root when the selection is not a Node.
     */
    private fun creationParent(): Node = (editorState.primarySelection as? Node) ?: root

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
                parent = creationParent(),
                child = light,
                name = "Add ${light.name}",
                onExecuted = { editorState.select(light) },
                onUndone = { if (editorState.isSelected(light)) editorState.clearSelection() }
            )
        )
        editorState.sealUndoMerge()
    }

    /**
     * State captured and applied by the visibility (eye) toggle: the toggled spatial's local
     * cull hint plus the enabled flag of every Light in its subtree. A culled light still
     * illuminates the scene - LightManager gathers lights by isEnabled() and ignores cull hints -
     * so hiding a spatial must also disable the lights it contains, and show/undo restores them.
     */
    private data class VisibilityState(val cullHint: CullHint, val lightEnabled: Map<Light, Boolean>)

    /**
     * Toggles viewport visibility of the given spatial (undoable). Hiding culls the subtree,
     * which also removes it from picking (findPick skips CullHint.Always subtrees) and disables
     * every light within it, so no other hints are modified. Undo restores the exact previous
     * state; Show restores Inherit and re-enables the subtree's lights.
     */
    fun toggleVisibility(spatial: Spatial) {
        // Read the local hint (setCullHint writes local): resolving up-tree would restore a
        // spatial's own Inherit as the parent's Dynamic after a hide/show cycle.
        val hide = spatial.sceneHints.localCullHint != CullHint.Always
        val subtreeLights = mutableListOf<Light>()
        collectLights(spatial, subtreeLights)
        val oldState = VisibilityState(
            cullHint = spatial.sceneHints.localCullHint,
            lightEnabled = subtreeLights.associateWith { it.isEnabled }
        )
        val newState = VisibilityState(
            cullHint = if (hide) CullHint.Always else CullHint.Inherit,
            lightEnabled = subtreeLights.associateWith { !hide }
        )
        editorState.execute(
            SetterCommand(
                name = if (hide) "Hide ${spatial.name ?: "object"}" else "Show ${spatial.name ?: "object"}",
                oldValue = oldState,
                newValue = newState,
                setter = { state ->
                    spatial.sceneHints.cullHint = state.cullHint
                    state.lightEnabled.forEach { (light, enabled) -> light.isEnabled = enabled }
                }
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
            val toggle = inputStates.current.keyboardState.isAtLeastOneDown(
                ArdorKey.LEFT_CONTROL, ArdorKey.RIGHT_CONTROL, ArdorKey.LEFT_META, ArdorKey.RIGHT_META
            )

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

        // Escape clears the selection
        layer.registerTrigger(InputTrigger(KeyPressedCondition(ArdorKey.ESCAPE)) { _, _, _ ->
            editorState.clearSelection()
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
     * The selected spatials an operation should actually process: the undeletable scene root
     * is dropped first (so it cannot shadow its selected children), then covered descendants.
     */
    private fun operableSelection(): List<Spatial> =
        SelectionUtil.topMost(editorState.selection.filter { it.parent != null })

    /**
     * Deletes every selected spatial as one undo step. The scene root is never deleted, and a
     * spatial whose selected ancestor already covers it is skipped.
     */
    fun deleteSelection() {
        val targets = operableSelection()
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
     * Builds the ready-to-attach copy a Duplicate operation adds to the scene.
     */
    private fun makeDuplicate(spatial: Spatial): Spatial {
        val copy = spatial.makeCopy(true)
        copy.name = "${spatial.name ?: "object"} Copy"
        MaterialUtil.autoMaterials(copy)
        return copy
    }

    /**
     * Duplicates the given spatial as a sibling (undoable) and selects the copy.
     */
    fun duplicateSpatial(spatial: Spatial) {
        val parent = spatial.parent ?: return
        val copy = makeDuplicate(spatial)
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
        val targets = operableSelection()
        when {
            targets.isEmpty() -> return
            targets.size == 1 -> duplicateSpatial(targets[0])
            else -> {
                val copies = targets.map { spatial -> spatial.parent!! to makeDuplicate(spatial) }
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
     * Moves the given spatial - or the whole selection when it is part of one - under
     * [newParent] as one undo step, preserving world transforms. Invalid moves (the scene root,
     * the current parent, a descendant of a moved spatial) are skipped.
     */
    fun reparentSpatial(spatial: Spatial, newParent: Node) {
        val requested = if (editorState.isSelected(spatial) && editorState.selection.size > 1) {
            operableSelection()
        } else {
            listOf(spatial)
        }
        val targets = requested.filter { ReparentCommand.isValidReparent(it, newParent) }
        when {
            targets.isEmpty() -> return
            targets.size == 1 -> editorState.execute(ReparentCommand(child = targets[0], newParent = newParent))
            else -> editorState.execute(
                CompositeCommand(
                    name = "Reparent ${targets.size} objects",
                    commands = targets.map { ReparentCommand(child = it, newParent = newParent) }
                )
            )
        }
        editorState.sealUndoMerge()
    }

    /**
     * Moves the given spatial to a resolved insertion point - a reorder among its current
     * siblings, or an indexed placement under a new parent (world transform preserved). Edge
     * drops move only the dragged spatial, not the rest of a multi-selection.
     */
    fun insertSpatial(spatial: Spatial, insertion: Insertion) {
        val command = when (insertion) {
            is Insertion.Reorder -> ReorderChildCommand(insertion.parent, spatial, insertion.newIndex)
            is Insertion.Insert -> ReparentCommand(spatial, insertion.parent, insertIndex = insertion.index)
        }
        editorState.execute(command)
        editorState.sealUndoMerge()
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
                reportError("Failed to save scene to ${file.name}", ex)
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
                    reportError("${file.name} did not contain a scene root Node", null)
                    return@add
                }
                installDocumentRoot(loaded)
                editorState.markSaved(file)
            } catch (ex: Exception) {
                reportError("Failed to open scene ${file.name}", ex)
            }
        }
    }

    /**
     * Logs a failure and surfaces it to the user. The dialog is deferred with invokeLater so a
     * modal never pumps the event queue from inside the render update that ran this action.
     */
    private fun reportError(message: String, ex: Exception?) {
        if (ex != null) {
            logger.log(Level.SEVERE, message, ex)
        } else {
            logger.severe(message)
        }
        javax.swing.SwingUtilities.invokeLater {
            javax.swing.JOptionPane.showMessageDialog(
                null,
                if (ex != null) "$message:\n${ex.message ?: ex.javaClass.simpleName}" else message,
                "Ardor3D Editor",
                javax.swing.JOptionPane.ERROR_MESSAGE
            )
        }
    }

    /**
     * Imports a model file (Wavefront OBJ or COLLADA) as a child of the scene root (undoable).
     */
    fun importModelFile(file: java.io.File) {
        pendingActions.add {
            try {
                val imported = ModelImport.load(file)
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
                reportError("Failed to import model ${file.name}", ex)
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

        // Keep light gizmos over their lights, hidden along with them
        for ((light, gizmo) in lightGizmos) {
            gizmo.setTranslation(light.worldTranslation)
            gizmo.sceneHints.cullHint =
                if (light.sceneHints.cullHint == CullHint.Always) CullHint.Always else CullHint.Inherit
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
}
