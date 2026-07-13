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

import com.ardor3d.input.InputState
import com.ardor3d.input.keyboard.Key
import com.ardor3d.input.mouse.ButtonState
import com.ardor3d.input.mouse.MouseButton

/**
 * A read-only, per-frame view of player input handed to [GameMode.update]. Positions are in canvas
 * pixels with Ardor3D's bottom-left origin (the same coordinates [GameContext.pick] expects).
 *
 * The distinction that matters for turn-based games: pointer position and key/button state are
 * *polled* (whatever they are this frame), while [clicks] are *edge events* - a click is a
 * completed press+release that did not drag, reported once on the frame it completes and never
 * again. So "did the player click a square this frame?" is [clicks]/[click]; "is the pointer over
 * this square right now?" is [mouseX]/[mouseY].
 */
interface GameInput {
    /** Pointer X in canvas pixels (bottom-left origin). */
    val mouseX: Int

    /** Pointer Y in canvas pixels (bottom-left origin). */
    val mouseY: Int

    /** Clicks (press+release, not drags) completed this frame, in the order they occurred. */
    val clicks: List<ClickEvent>

    /** The last click completed this frame, or null - convenience over [clicks]. */
    val click: ClickEvent? get() = clicks.lastOrNull()

    /** Whether [key] is currently held down. */
    fun isKeyDown(key: Key): Boolean

    /** Whether [button] is currently held down. */
    fun isButtonDown(button: MouseButton): Boolean

    companion object {
        /** An input with nothing pressed and no clicks - used to tick a game with no input source. */
        val EMPTY: GameInput = object : GameInput {
            override val mouseX = 0
            override val mouseY = 0
            override val clicks = emptyList<ClickEvent>()
            override fun isKeyDown(key: Key) = false
            override fun isButtonDown(button: MouseButton) = false
        }
    }
}

/** A completed mouse click at a canvas pixel (bottom-left origin). */
data class ClickEvent(val x: Int, val y: Int, val button: MouseButton)

/**
 * The mutable accumulator behind a live [GameInput]. [GameInputController] owns one, calls
 * [beginFrame] to clear the previous frame's edge events, then drives Ardor3D input into it via
 * [capture] and [recordClick]; the same object is handed to the game as a [GameInput].
 */
internal class MutableGameInput : GameInput {
    private var mouseState: com.ardor3d.input.mouse.MouseState? = null
    private var keyboardState: com.ardor3d.input.keyboard.KeyboardState? = null
    private val _clicks = mutableListOf<ClickEvent>()

    override val mouseX: Int get() = mouseState?.x ?: 0
    override val mouseY: Int get() = mouseState?.y ?: 0
    override val clicks: List<ClickEvent> get() = _clicks
    override fun isKeyDown(key: Key): Boolean = keyboardState?.isDown(key) ?: false
    override fun isButtonDown(button: MouseButton): Boolean =
        mouseState?.getButtonState(button) == ButtonState.DOWN

    /** Clears per-frame edge events (clicks). Polled state (pointer, keys) persists as last-known. */
    fun beginFrame() {
        _clicks.clear()
    }

    /** Adopts [state] as the current polled pointer/keyboard state. */
    fun capture(state: InputState) {
        mouseState = state.mouseState
        keyboardState = state.keyboardState
    }

    /** Records a completed click for this frame. */
    fun recordClick(x: Int, y: Int, button: MouseButton) {
        _clicks.add(ClickEvent(x, y, button))
    }
}
