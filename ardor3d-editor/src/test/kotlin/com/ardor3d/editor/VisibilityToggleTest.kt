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

import com.ardor3d.light.PointLight
import com.ardor3d.scenegraph.Node
import com.ardor3d.scenegraph.hint.CullHint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The visibility (eye) toggle must disable every light inside a hidden subtree, not just the
 * toggled spatial when it happens to be a Light: LightManager gathers lights by isEnabled(),
 * ignoring cull hints, so a culled light would keep illuminating the scene. Show and undo must
 * restore them.
 */
class VisibilityToggleTest {

    @Test
    fun hidingANodeDisablesDescendantLightsAndShowUndoRestoreThem() {
        val state = EditorState()
        val scene = EditorScene(state)

        // A group node with a child light, added to the document root.
        val group = Node("Group")
        val light = PointLight().apply {
            name = "Child Light"
            isEnabled = true
        }
        group.attachChild(light)
        state.sceneRoot.attachChild(group)

        // Hide the group: geometry is culled and the descendant light is disabled.
        scene.toggleVisibility(group)
        assertEquals(CullHint.Always, group.sceneHints.localCullHint)
        assertFalse("a hidden subtree's light must be disabled", light.isEnabled)

        // Undo restores the exact local cull hint (Inherit, not the resolved Dynamic) and the light.
        state.undo()
        assertEquals(CullHint.Inherit, group.sceneHints.localCullHint)
        assertTrue("undo must re-enable the descendant light", light.isEnabled)

        // Hide again, then Show (a fresh toggle, not undo) must re-enable the light too.
        scene.toggleVisibility(group)
        assertFalse(light.isEnabled)
        scene.toggleVisibility(group)
        assertEquals(CullHint.Inherit, group.sceneHints.localCullHint)
        assertTrue("showing must re-enable the descendant light", light.isEnabled)
    }
}
