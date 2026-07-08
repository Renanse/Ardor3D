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
    fun keylessCompositesNeverMerge() {
        val recorder = Recorder()
        val first = CompositeCommand("x", listOf(logCommand(recorder, "a")))
        val second = CompositeCommand("y", listOf(logCommand(recorder, "b")))
        assertFalse(first.mergeWith(second))
    }

    /** A keystroke-like composite: one SetterCommand per target, all writing into [values]. */
    private fun setterComposite(values: DoubleArray, mergeKey: String, vararg newValues: Double) =
        CompositeCommand(
            name = "Move ${newValues.size} objects",
            commands = newValues.mapIndexed { i, newValue ->
                SetterCommand(
                    name = "Move $i",
                    oldValue = values[i],
                    newValue = newValue,
                    mergeKey = "x:$i",
                    setter = { values[i] = it }
                )
            },
            mergeKey = mergeKey
        )

    @Test
    fun keyedCompositesMergeIntoOneUndoStep() {
        val values = doubleArrayOf(0.0, 10.0)
        val stack = CommandStack()

        // Two keystrokes of a continuous edit over both targets
        stack.execute(setterComposite(values, "x:0,1", 1.0, 11.0))
        stack.execute(setterComposite(values, "x:0,1", 1.5, 11.5))

        assertEquals(1, stack.undoCount)
        assertEquals(1.5, values[0], 0.0)
        assertEquals(11.5, values[1], 0.0)

        // One undo returns both targets to their pre-gesture values
        stack.undo()
        assertEquals(0.0, values[0], 0.0)
        assertEquals(10.0, values[1], 0.0)

        stack.redo()
        assertEquals(1.5, values[0], 0.0)
        assertEquals(11.5, values[1], 0.0)
    }

    @Test
    fun differentKeysOrSizesDoNotMerge() {
        val values = doubleArrayOf(0.0, 10.0)
        assertFalse(
            setterComposite(values, "x:0,1", 1.0, 11.0)
                .mergeWith(setterComposite(values, "y:0,1", 2.0, 12.0))
        )
        assertFalse(
            setterComposite(values, "x:0,1", 1.0, 11.0)
                .mergeWith(
                    CompositeCommand(
                        name = "Move 1 object",
                        commands = listOf(
                            SetterCommand("Move 0", values[0], 2.0, mergeKey = "x:0", setter = { values[0] = it })
                        ),
                        mergeKey = "x:0,1"
                    )
                )
        )
    }

    @Test
    fun sealBlocksCompositeMerging() {
        val values = doubleArrayOf(0.0, 10.0)
        val stack = CommandStack()
        stack.execute(setterComposite(values, "x:0,1", 1.0, 11.0))
        stack.sealMerge()
        stack.execute(setterComposite(values, "x:0,1", 2.0, 12.0))
        assertEquals(2, stack.undoCount)
    }

    @Test
    fun nonSetterChildrenDoNotMerge() {
        val recorder = Recorder()
        val first = CompositeCommand("x", listOf(logCommand(recorder, "a")), mergeKey = "k")
        val second = CompositeCommand("x", listOf(logCommand(recorder, "b")), mergeKey = "k")
        assertFalse(first.mergeWith(second))
    }
}
