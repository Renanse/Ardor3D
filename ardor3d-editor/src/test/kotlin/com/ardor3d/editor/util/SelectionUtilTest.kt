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
import org.junit.Test

class SelectionUtilTest {

    @Test
    fun keepsIndependentSpatials() {
        val root = Node("root")
        val a = Node("a")
        val b = Node("b")
        root.attachChild(a)
        root.attachChild(b)

        assertEquals(listOf(a, b), SelectionUtil.topMost(listOf(a, b)))
    }

    @Test
    fun dropsSpatialsWhoseAncestorIsAlsoSelected() {
        val root = Node("root")
        val parent = Node("parent")
        val child = Node("child")
        val grandchild = Node("grandchild")
        val sibling = Node("sibling")
        root.attachChild(parent)
        parent.attachChild(child)
        child.attachChild(grandchild)
        root.attachChild(sibling)

        // Direct child and deep descendant are both covered by the selected ancestor
        assertEquals(
            listOf(parent, sibling),
            SelectionUtil.topMost(listOf(parent, child, grandchild, sibling))
        )
    }

    @Test
    fun preservesSelectionOrder() {
        val root = Node("root")
        val a = Node("a")
        val b = Node("b")
        val aChild = Node("aChild")
        root.attachChild(a)
        root.attachChild(b)
        a.attachChild(aChild)

        assertEquals(listOf(b, a), SelectionUtil.topMost(listOf(b, aChild, a)))
    }

    @Test
    fun emptySelectionYieldsEmptyList() {
        assertEquals(emptyList<Any>(), SelectionUtil.topMost(emptyList()))
    }
}
