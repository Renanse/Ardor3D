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
import org.junit.Test

class ReorderChildCommandTest {

    private fun rig(): Pair<Node, List<Node>> {
        val parent = Node("parent")
        val children = listOf(Node("a"), Node("b"), Node("c"), Node("d"))
        children.forEach { parent.attachChild(it) }
        return parent to children
    }

    private fun names(parent: Node) = parent.children.map { it.name }

    @Test
    fun movesChildDown() {
        val (parent, children) = rig()
        CommandStack().execute(ReorderChildCommand(parent, children[0], 2))
        assertEquals(listOf("b", "c", "a", "d"), names(parent))
    }

    @Test
    fun movesChildUp() {
        val (parent, children) = rig()
        CommandStack().execute(ReorderChildCommand(parent, children[3], 0))
        assertEquals(listOf("d", "a", "b", "c"), names(parent))
    }

    @Test
    fun undoRestoresOriginalOrder() {
        val (parent, children) = rig()
        val stack = CommandStack()
        stack.execute(ReorderChildCommand(parent, children[0], 3))
        assertEquals(listOf("b", "c", "d", "a"), names(parent))
        stack.undo()
        assertEquals(listOf("a", "b", "c", "d"), names(parent))
    }

    @Test
    fun redoReappliesTheMove() {
        val (parent, children) = rig()
        val stack = CommandStack()
        stack.execute(ReorderChildCommand(parent, children[1], 3))
        stack.undo()
        stack.redo()
        assertEquals(listOf("a", "c", "d", "b"), names(parent))
        stack.undo()
        assertEquals(listOf("a", "b", "c", "d"), names(parent))
    }

    @Test
    fun localTransformIsUntouched() {
        val (parent, children) = rig()
        children[2].setTranslation(5.0, 6.0, 7.0)
        CommandStack().execute(ReorderChildCommand(parent, children[2], 0))
        assertEquals(5.0, children[2].translation.x, 0.0)
        assertEquals(6.0, children[2].translation.y, 0.0)
        assertEquals(7.0, children[2].translation.z, 0.0)
    }
}
