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
import org.junit.Assert.assertTrue
import org.junit.Test

class CompositeCommandTest {

    private class Recorder {
        val events = mutableListOf<String>()
    }

    private fun logCommand(recorder: Recorder, id: String, structural: Boolean = false) = object : EditorCommand {
        override val name: String = id
        override val affectsStructure: Boolean = structural
        override fun execute() {
            recorder.events.add("execute:$id")
        }

        override fun undo() {
            recorder.events.add("undo:$id")
        }
    }

    @Test
    fun executesInOrderAndUndoesInReverse() {
        val recorder = Recorder()
        val composite = CompositeCommand(
            name = "Delete 3 objects",
            commands = listOf(
                logCommand(recorder, "a"),
                logCommand(recorder, "b"),
                logCommand(recorder, "c")
            )
        )

        composite.execute()
        assertEquals(listOf("execute:a", "execute:b", "execute:c"), recorder.events)

        recorder.events.clear()
        composite.undo()
        assertEquals(listOf("undo:c", "undo:b", "undo:a"), recorder.events)
    }

    @Test
    fun affectsStructureWhenAnyChildDoes() {
        val recorder = Recorder()
        assertFalse(
            CompositeCommand("x", listOf(logCommand(recorder, "a"), logCommand(recorder, "b"))).affectsStructure
        )
        assertTrue(
            CompositeCommand(
                "x",
                listOf(logCommand(recorder, "a"), logCommand(recorder, "b", structural = true))
            ).affectsStructure
        )
    }

    @Test
    fun callbacksFireAfterChildren() {
        val recorder = Recorder()
        val composite = CompositeCommand(
            name = "x",
            commands = listOf(logCommand(recorder, "a")),
            onExecuted = { recorder.events.add("onExecuted") },
            onUndone = { recorder.events.add("onUndone") }
        )

        composite.execute()
        assertEquals(listOf("execute:a", "onExecuted"), recorder.events)

        recorder.events.clear()
        composite.undo()
        assertEquals(listOf("undo:a", "onUndone"), recorder.events)
    }

    @Test
    fun isASingleUndoStepOnTheStack() {
        val parent = Node("parent")
        val a = Node("a")
        val b = Node("b")
        parent.attachChild(a)
        parent.attachChild(b)
        val stack = CommandStack()

        stack.execute(
            CompositeCommand(
                name = "Delete 2 objects",
                commands = listOf(DetachChildCommand(parent, a), DetachChildCommand(parent, b))
            )
        )
        assertEquals(0, parent.numberOfChildren)
        assertEquals(1, stack.undoCount)
        assertEquals("Delete 2 objects", stack.undoName)

        stack.undo()
        assertEquals(listOf("a", "b"), parent.children.map { it.name })
        assertFalse(stack.canUndo)

        stack.redo()
        assertEquals(0, parent.numberOfChildren)
    }

    @Test
    fun neverMerges() {
        val recorder = Recorder()
        val first = CompositeCommand("x", listOf(logCommand(recorder, "a")))
        val second = CompositeCommand("y", listOf(logCommand(recorder, "b")))
        assertFalse(first.mergeWith(second))
    }
}
