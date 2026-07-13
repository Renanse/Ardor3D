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

import com.ardor3d.input.keyboard.Key
import com.ardor3d.input.mouse.MouseButton
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure test of the [GameInput] contract: the per-frame click accumulation / reset that
 * [GameInputController] relies on, and the do-nothing [GameInput.EMPTY]. No GL or input hardware -
 * the capture-from-Ardor3D-input path is covered end-to-end by the render test.
 */
class GameInputTest {

    @Test
    fun defaultsBeforeAnyInput() {
        val input = MutableGameInput()
        assertEquals(0, input.mouseX)
        assertEquals(0, input.mouseY)
        assertTrue(input.clicks.isEmpty())
        assertNull(input.click)
        assertFalse(input.isKeyDown(Key.A))
        assertFalse(input.isButtonDown(MouseButton.LEFT))
    }

    @Test
    fun clicksAccumulateWithinAFrameThenClearOnNext() {
        val input = MutableGameInput()

        input.beginFrame()
        input.recordClick(10, 20, MouseButton.LEFT)
        input.recordClick(30, 40, MouseButton.RIGHT)
        assertEquals(2, input.clicks.size)
        assertEquals(ClickEvent(10, 20, MouseButton.LEFT), input.clicks[0])
        assertEquals("click is the last one this frame", ClickEvent(30, 40, MouseButton.RIGHT), input.click)

        input.beginFrame()
        assertTrue("beginFrame clears the previous frame's clicks", input.clicks.isEmpty())
        assertNull(input.click)
    }

    @Test
    fun emptyInputHasNothingPressed() {
        val input = GameInput.EMPTY
        assertEquals(0, input.mouseX)
        assertEquals(0, input.mouseY)
        assertTrue(input.clicks.isEmpty())
        assertNull(input.click)
        assertFalse(input.isKeyDown(Key.ESCAPE))
        assertFalse(input.isButtonDown(MouseButton.LEFT))
    }
}
