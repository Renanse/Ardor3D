/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.editor.util

import com.ardor3d.scenegraph.Node
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReorderUtilTest {

    /** root -> [a, b, c]; b -> [b1, b2]. */
    private class Rig {
        val root = Node("root")
        val a = Node("a")
        val b = Node("b")
        val c = Node("c")
        val b1 = Node("b1")
        val b2 = Node("b2")

        init {
            root.attachChild(a)
            root.attachChild(b)
            root.attachChild(c)
            b.attachChild(b1)
            b.attachChild(b2)
        }
    }

    @Test
    fun beforeSiblingInDifferentParentInserts() {
        val rig = Rig()
        assertEquals(
            Insertion.Insert(rig.b, 0),
            ReorderUtil.resolveInsertion(rig.a, rig.b1, after = false)
        )
        assertEquals(
            Insertion.Insert(rig.b, 2),
            ReorderUtil.resolveInsertion(rig.a, rig.b2, after = true)
        )
    }

    @Test
    fun sameParentMoveDownAdjustsForRemoval() {
        val rig = Rig()
        // a dropped after c: raw index 3, minus a's own slot = 2
        assertEquals(
            Insertion.Reorder(rig.root, 2),
            ReorderUtil.resolveInsertion(rig.a, rig.c, after = true)
        )
        // a dropped before c: raw index 2, minus a's own slot = 1
        assertEquals(
            Insertion.Reorder(rig.root, 1),
            ReorderUtil.resolveInsertion(rig.a, rig.c, after = false)
        )
    }

    @Test
    fun sameParentMoveUp() {
        val rig = Rig()
        assertEquals(
            Insertion.Reorder(rig.root, 0),
            ReorderUtil.resolveInsertion(rig.c, rig.a, after = false)
        )
    }

    @Test
    fun noOpMovesResolveToNull() {
        val rig = Rig()
        // Dropping right where the child already sits, from either neighbor's edge
        assertNull(ReorderUtil.resolveInsertion(rig.b, rig.a, after = true))
        assertNull(ReorderUtil.resolveInsertion(rig.b, rig.c, after = false))
        assertNull(ReorderUtil.resolveInsertion(rig.b, rig.b, after = true))
    }

    @Test
    fun rootAndUnparentedAreInvalid() {
        val rig = Rig()
        // No inserting beside the root row, and the root itself cannot be dragged
        assertNull(ReorderUtil.resolveInsertion(rig.a, rig.root, after = false))
        assertNull(ReorderUtil.resolveInsertion(rig.root, rig.a, after = true))
    }

    @Test
    fun cyclesAreInvalid() {
        val rig = Rig()
        // Dropping b beside its own child would insert b under itself
        assertNull(ReorderUtil.resolveInsertion(rig.b, rig.b1, after = false))
        // ...and beside a grandchild, under its own descendant
        val b1x = Node("b1x")
        rig.b1.attachChild(b1x)
        assertNull(ReorderUtil.resolveInsertion(rig.b, b1x, after = true))
    }

    @Test
    fun afterExpandedNodeInsertsAsFirstChild() {
        val rig = Rig()
        assertEquals(
            Insertion.Insert(rig.b, 0),
            ReorderUtil.resolveInsertion(rig.a, rig.b, after = true, rowExpanded = true)
        )
        // Collapsed (or expanded-but-empty) keeps sibling semantics
        assertEquals(
            Insertion.Reorder(rig.root, 1),
            ReorderUtil.resolveInsertion(rig.a, rig.b, after = true, rowExpanded = false)
        )
    }

    @Test
    fun afterExpandedOwnParentReordersToTop() {
        val rig = Rig()
        assertEquals(
            Insertion.Reorder(rig.b, 0),
            ReorderUtil.resolveInsertion(rig.b2, rig.b, after = true, rowExpanded = true)
        )
        // b1 is already the first child - no-op
        assertNull(ReorderUtil.resolveInsertion(rig.b1, rig.b, after = true, rowExpanded = true))
    }
}
