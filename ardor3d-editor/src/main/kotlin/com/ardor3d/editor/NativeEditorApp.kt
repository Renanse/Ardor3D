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
import androidx.compose.runtime.SideEffect
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
import com.ardor3d.editor.io.ModelImport
import com.ardor3d.editor.hierarchy.HierarchyPanel
import com.ardor3d.editor.inspector.InspectorPanel
import com.ardor3d.editor.menu.EditorMenuBar
import com.ardor3d.editor.ui.EditorTheme
import com.ardor3d.editor.viewport.ViewportStatusBar
import com.ardor3d.editor.viewport.ViewportToolbar
import com.ardor3d.framework.FrameHandler
import com.ardor3d.framework.lwjgl3.Lwjgl3CanvasRenderer
import com.ardor3d.framework.lwjgl3.awt.Lwjgl3AwtCanvas
import com.ardor3d.image.util.awt.AWTImageLoader
import com.ardor3d.input.PhysicalLayer
import com.ardor3d.input.awt.AwtFocusWrapper
import com.ardor3d.input.awt.AwtKeyboardWrapper
import com.ardor3d.input.awt.AwtMouseManager
import com.ardor3d.input.awt.AwtMouseWrapper
import com.ardor3d.input.keyboard.KeyboardWrapper
import com.ardor3d.input.logical.LogicalLayer
import com.ardor3d.renderer.material.MaterialManager
import com.ardor3d.renderer.material.reader.YamlMaterialReader
import com.ardor3d.util.*
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
        // Image loading: register the AWT/ImageIO loader for PNG, JPG, etc. Without it only the
        // built-in DDS/HDR/TGA/ABI handlers exist, so texture PNGs (e.g. the gizmo readout's bitmap
        // font atlas) fail to decode and fall back to the magenta "missing texture" image.
        AWTImageLoader.registerLoader()

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
        // Without material/shader locators and default materials the editor cannot render
        // anything meaningful - fail fast rather than launch in a broken state.
        logger.log(Level.SEVERE, "Failed to set up default resource locators", ex)
        throw IllegalStateException("Editor startup failed: could not load default materials/shaders", ex)
    }
}

/**
 * Bridges the window-level shortcut handler in [main] to the operations built inside
 * [NativeEditorApp] (they need the window and scene, which don't exist yet at Window setup).
 */
class ShortcutActions {
    var fileOperations: FileOperations? = null
    var sceneOperations: SceneOperations? = null
}

fun main() = application {
    // Initialize resource locators before anything else. The application block is a
    // composable, so guard against re-running on recomposition.
    remember { addDefaultResourceLocators() }
    val editorState = remember { EditorState() }
    val shortcuts = remember { ShortcutActions() }
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

                    Key.D -> {
                        shortcuts.sceneOperations?.duplicateSelection?.invoke()
                        true
                    }

                    Key.N -> {
                        shortcuts.fileOperations?.newScene?.invoke()
                        true
                    }

                    Key.O -> {
                        shortcuts.fileOperations?.openScene?.invoke()
                        true
                    }

                    Key.S -> {
                        if (event.isShiftPressed) {
                            shortcuts.fileOperations?.saveSceneAs?.invoke()
                        } else {
                            shortcuts.fileOperations?.saveScene?.invoke()
                        }
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
            NativeEditorApp(editorState, window, requestExit, shortcuts)
        }
    }
}

/**
 * Shows an AWT file dialog owned by [owner]. Accepts any of [extensions]; on save, the first
 * one is appended when the user leaves it off.
 */
private fun promptForFile(
    owner: java.awt.Frame,
    title: String,
    save: Boolean,
    vararg extensions: String
): java.io.File? {
    val dialog = java.awt.FileDialog(owner, title, if (save) java.awt.FileDialog.SAVE else java.awt.FileDialog.LOAD)
    dialog.setFilenameFilter { _, name -> extensions.any { name.endsWith(".$it", ignoreCase = true) } }
    dialog.isVisible = true
    val fileName = dialog.file ?: return null
    val fixed = if (save && extensions.none { fileName.endsWith(".$it", ignoreCase = true) }) {
        "$fileName.${extensions.first()}"
    } else {
        fileName
    }
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
fun NativeEditorApp(
    editorState: EditorState,
    window: java.awt.Frame,
    requestExit: () -> Unit,
    shortcuts: ShortcutActions = ShortcutActions()
) {
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
            reparentSpatial = editorScene::reparentSpatial,
            insertSpatial = editorScene::insertSpatial,
            toggleVisibility = editorScene::toggleVisibility,
            createEmpty = editorScene::createEmptyNode
        )
    }
    val fileOperations = remember(editorScene, window) {
        fun saveTo(file: java.io.File?) {
            (file ?: promptForFile(window, "Save Scene", save = true, "a3d"))
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
                    promptForFile(window, "Open Scene", save = false, "a3d")
                        ?.let(editorScene::openScene)
                }
            },
            saveScene = { saveTo(editorState.currentFile) },
            saveSceneAs = { saveTo(null) },
            importModel = {
                promptForFile(window, "Import Model", save = false, *ModelImport.supportedExtensions.toTypedArray())
                    ?.let(editorScene::importModelFile)
            },
            exit = requestExit
        )
    }

    // Expose the operations to the window-level shortcut handler
    SideEffect {
        shortcuts.fileOperations = fileOperations
        shortcuts.sceneOperations = sceneOperations
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
