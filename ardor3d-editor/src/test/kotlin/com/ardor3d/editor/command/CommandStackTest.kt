/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.editor.command

import com.ardor3d.scenegraph.Node
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandStackTest {

    private class Holder(var value: Int = 0)

    private fun setCommand(holder: Holder, from: Int, to: Int, mergeKey: String? = null) =
        SetterCommand(
            name = "Set",
            oldValue = from,
            newValue = to,
            mergeKey = mergeKey,
            setter = { holder.value = it }
        )

    @Test
    fun executeAppliesAndSupportsUndoRedo() {
        val stack = CommandStack()
        val holder = Holder()

        stack.execute(setCommand(holder, 0, 1))
        stack.execute(setCommand(holder, 1, 2))
        assertEquals(2, holder.value)
        assertTrue(stack.canUndo)
        assertFalse(stack.canRedo)

        stack.undo()
        assertEquals(1, holder.value)
        stack.undo()
        assertEquals(0, holder.value)
        assertFalse(stack.canUndo)
        assertTrue(stack.canRedo)

        stack.redo()
        stack.redo()
        assertEquals(2, holder.value)
        assertFalse(stack.canRedo)
    }

    @Test
    fun executeClearsRedoHistory() {
        val stack = CommandStack()
        val holder = Holder()

        stack.execute(setCommand(holder, 0, 1))
        stack.undo()
        assertTrue(stack.canRedo)

        stack.execute(setCommand(holder, 0, 5))
        assertFalse(stack.canRedo)
        assertEquals(5, holder.value)
    }

    @Test
    fun sameKeyCommandsMergeIntoOneUndoStep() {
        val stack = CommandStack()
        val holder = Holder()

        // Simulates dragging a slider from 0 to 3
        stack.execute(setCommand(holder, 0, 1, mergeKey = "k"))
        stack.execute(setCommand(holder, 1, 2, mergeKey = "k"))
        stack.execute(setCommand(holder, 2, 3, mergeKey = "k"))
        assertEquals(3, holder.value)
        assertEquals(1, stack.undoCount)

        stack.undo()
        assertEquals(0, holder.value)
        assertFalse(stack.canUndo)

        stack.redo()
        assertEquals(3, holder.value)
    }

    @Test
    fun sealMergeStartsANewUndoStep() {
        val stack = CommandStack()
        val holder = Holder()

        stack.execute(setCommand(holder, 0, 1, mergeKey = "k"))
        stack.sealMerge()
        stack.execute(setCommand(holder, 1, 2, mergeKey = "k"))
        assertEquals(2, stack.undoCount)

        stack.undo()
        assertEquals(1, holder.value)
        stack.undo()
        assertEquals(0, holder.value)
    }

    @Test
    fun differentKeysDoNotMerge() {
        val stack = CommandStack()
        val holder = Holder()

        stack.execute(setCommand(holder, 0, 1, mergeKey = "a"))
        stack.execute(setCommand(holder, 1, 2, mergeKey = "b"))
        assertEquals(2, stack.undoCount)
    }

    @Test
    fun nullKeysNeverMerge() {
        val stack = CommandStack()
        val holder = Holder()

        stack.execute(setCommand(holder, 0, 1))
        stack.execute(setCommand(holder, 1, 2))
        assertEquals(2, stack.undoCount)
    }

    @Test
    fun undoBeyondLimitEvictsOldestStep() {
        val stack = CommandStack(limit = 2)
        val holder = Holder()

        stack.execute(setCommand(holder, 0, 1))
        stack.sealMerge()
        stack.execute(setCommand(holder, 1, 2))
        stack.sealMerge()
        stack.execute(setCommand(holder, 2, 3))
        assertEquals(2, stack.undoCount)

        stack.undo()
        stack.undo()
        assertFalse(stack.canUndo)
        // Oldest step (0 -> 1) was evicted, so we end at its result, not at 0
        assertEquals(1, holder.value)
    }

    @Test
    fun recordPushesWithoutReExecuting() {
        val stack = CommandStack()
        val holder = Holder(7)

        // Effect already applied (e.g. gizmo drag); record must not call execute()
        stack.record(setCommand(holder, 3, 9))
        assertEquals(7, holder.value)

        stack.undo()
        assertEquals(3, holder.value)
        stack.redo()
        assertEquals(9, holder.value)
    }

    @Test
    fun namesReflectStackTops() {
        val stack = CommandStack()
        val holder = Holder()

        assertNull(stack.undoName)
        assertNull(stack.redoName)

        stack.execute(SetterCommand("Move object", 0, 1, setter = { holder.value = it }))
        assertEquals("Move object", stack.undoName)

        stack.undo()
        assertEquals("Move object", stack.redoName)
        assertNull(stack.undoName)
    }

    @Test
    fun attachChildCommandAttachesAndDetaches() {
        val parent = Node("parent")
        val child = Node("child")
        var executed = 0
        var undone = 0
        val stack = CommandStack()

        stack.execute(AttachChildCommand(parent, child, onExecuted = { executed++ }, onUndone = { undone++ }))
        assertEquals(1, parent.numberOfChildren)
        assertEquals(1, executed)

        stack.undo()
        assertEquals(0, parent.numberOfChildren)
        assertEquals(1, undone)

        stack.redo()
        assertEquals(1, parent.numberOfChildren)
        assertEquals(2, executed)
    }

    @Test
    fun clearEmptiesBothStacks() {
        val stack = CommandStack()
        val holder = Holder()

        stack.execute(setCommand(holder, 0, 1))
        stack.undo()
        stack.execute(setCommand(holder, 0, 2))
        stack.clear()
        assertFalse(stack.canUndo)
        assertFalse(stack.canRedo)
    }
}
