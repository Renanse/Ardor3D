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
import com.ardor3d.input.logical.MouseButtonClickedCondition
import com.ardor3d.input.mouse.MouseButton
import java.util.function.Predicate

/**
 * The play-mode input routing switch. It owns its own [LogicalLayer] on the editor's shared
 * [PhysicalLayer]; while playing, the editor drives this layer instead of its selection/gizmo input,
 * so input reaches the game rather than editor tools.
 *
 * Only [poll] drains input, and the editor calls it only while playing - so it never competes with
 * the editor's own layers (which don't run during play), and draining every play frame keeps input
 * events from piling up unseen.
 *
 * Pointer position and keys are captured by a catch-all trigger (idempotent - safe to re-run against
 * the stale last state when no new input arrives). Clicks, however, must be *edge* events: a
 * [MouseButtonClickedCondition] fires once on the press-release transition, so a click is reported
 * exactly once - not re-reported every frame from a state whose click count is still set (which made
 * selection "stick" to the mouse and behave erratically).
 */
class GameInputController(canvas: Canvas, physicalLayer: PhysicalLayer) {
    private val logicalLayer = LogicalLayer()
    private val input = MutableGameInput()

    init {
        logicalLayer.registerInput(canvas, physicalLayer)
        logicalLayer.registerTrigger(InputTrigger(Predicate { true }) { _, states, _ ->
            input.capture(states.current)
        })
        for (button in listOf(MouseButton.LEFT, MouseButton.RIGHT)) {
            logicalLayer.registerTrigger(InputTrigger(MouseButtonClickedCondition(button)) { _, states, _ ->
                val mouse = states.current.mouseState
                input.recordClick(mouse.x, mouse.y, button)
            })
        }
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
