/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.editor.checkers

import com.ardor3d.editor.play.GameContext
import com.ardor3d.intersection.PrimitivePickResults
import com.ardor3d.renderer.Camera
import com.ardor3d.scenegraph.Node
import com.ardor3d.scenegraph.Spatial

/**
 * A headless [GameContext] for driving [CheckersGame] in tests: picking sees nothing (tests
 * steer by square through the game's test seams), materialization is a no-op, and the last
 * status line is captured for assertion.
 */
internal class FakeGameContext : GameContext {
    override val sceneRoot: Node = Node("scene")
    override val camera: Camera = Camera(1, 1)
    var lastStatus: String? = null
    override fun pick(x: Int, y: Int): PrimitivePickResults = PrimitivePickResults()
    override fun setStatus(text: String?) {
        lastStatus = text
    }

    override fun materialize(spatial: Spatial) { /* no material system in tests */ }
}
