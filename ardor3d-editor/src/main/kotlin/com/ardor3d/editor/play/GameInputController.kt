/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.editor.play

import com.ardor3d.framework.Canvas
import com.ardor3d.input.PhysicalLayer
import com.ardor3d.input.logical.InputTrigger
import com.ardor3d.input.logical.LogicalLayer
import java.util.function.Predicate

/**
 * The play-mode input routing switch. It owns its own [LogicalLayer] on the editor's shared
 * [PhysicalLayer]; while playing, the editor drives this layer instead of its selection/gizmo input,
 * so input reaches the game rather than editor tools.
 *
 * Only [poll] drains input, and the editor calls it only while playing - so it never competes with
 * the editor's own layers (which don't run during play), and draining every play frame keeps input
 * events from piling up unseen. A single catch-all trigger runs on every drained input state, which
 * is what makes click detection reliable: a press+release that both land within one frame are
 * distinct states in the drained batch, so no click is missed by only sampling frame endpoints.
 */
class GameInputController(canvas: Canvas, physicalLayer: PhysicalLayer) {
    private val logicalLayer = LogicalLayer()
    private val input = MutableGameInput()

    init {
        logicalLayer.registerInput(canvas, physicalLayer)
        logicalLayer.registerTrigger(InputTrigger(Predicate { true }) { _, states, _ ->
            val current = states.current
            input.capture(current)
            val mouse = current.mouseState
            for (button in mouse.buttonsClicked) {
                input.recordClick(mouse.x, mouse.y, button)
            }
        })
    }

    /**
     * Advances input by one play frame: clears the previous frame's clicks, drains the physical
     * layer (firing the capture trigger for each new state), and returns the frame's [GameInput].
     * The returned view is reused each frame - read it within the same update, don't retain it.
     */
    fun poll(timePerFrame: Double): GameInput {
        input.beginFrame()
        logicalLayer.checkTriggers(timePerFrame)
        return input
    }
}
