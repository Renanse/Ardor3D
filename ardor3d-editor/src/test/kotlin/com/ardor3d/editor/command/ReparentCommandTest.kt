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

import com.ardor3d.math.Quaternion
import com.ardor3d.math.Vector3
import com.ardor3d.math.type.ReadOnlyTransform
import com.ardor3d.scenegraph.Node
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ReparentCommandTest {

    private val epsilon = 1e-9

    private fun assertTransformEquals(expected: ReadOnlyTransform, actual: ReadOnlyTransform) {
        for (i in 0..2) {
            for (j in 0..2) {
                assertEquals(
                    "matrix[$i][$j]",
                    expected.matrix.getValue(i, j),
                    actual.matrix.getValue(i, j),
                    epsilon
                )
            }
        }
        assertEquals("tx", expected.translation.x, actual.translation.x, epsilon)
        assertEquals("ty", expected.translation.y, actual.translation.y, epsilon)
        assertEquals("tz", expected.translation.z, actual.translation.z, epsilon)
        assertEquals("sx", expected.scale.x, actual.scale.x, epsilon)
        assertEquals("sy", expected.scale.y, actual.scale.y, epsilon)
        assertEquals("sz", expected.scale.z, actual.scale.z, epsilon)
    }

    /** root -> a(1,2,3), b(10,0,0 rotated 90 about Y); child under a at (1,0,0). */
    private class Rig {
        val root = Node("root")
        val a = Node("a")
        val b = Node("b")
        val child = Node("child")

        init {
            root.attachChild(a)
            root.attachChild(b)
            a.attachChild(child)
            a.setTranslation(1.0, 2.0, 3.0)
            b.setTranslation(10.0, 0.0, 0.0)
            b.setRotation(Quaternion().fromAngleAxis(Math.PI / 2, Vector3.UNIT_Y))
            child.setTranslation(1.0, 0.0, 0.0)
            root.updateGeometricState(0.0, true)
        }
    }

    @Test
    fun movesChildToNewParentPreservingWorldTransform() {
        val rig = Rig()
        val worldBefore = com.ardor3d.math.Transform().set(rig.child.worldTransform)

        val stack = CommandStack()
        stack.execute(ReparentCommand(child = rig.child, newParent = rig.b))
        rig.root.updateGeometricState(0.0, true)

        assertSame(rig.b, rig.child.parent)
        assertEquals(0, rig.a.numberOfChildren)
        assertTransformEquals(worldBefore, rig.child.worldTransform)
    }

    @Test
    fun keepWorldTransformFalseKeepsLocalTransform() {
        val rig = Rig()

        val stack = CommandStack()
        stack.execute(ReparentCommand(child = rig.child, newParent = rig.b, keepWorldTransform = false))
        rig.root.updateGeometricState(0.0, true)

        assertSame(rig.b, rig.child.parent)
        assertEquals(1.0, rig.child.translation.x, epsilon)
        assertEquals(0.0, rig.child.translation.y, epsilon)
        assertEquals(0.0, rig.child.translation.z, epsilon)
    }

    @Test
    fun undoRestoresParentIndexAndLocalTransform() {
        val rig = Rig()
        // Give child siblings so the restored index is observable
        val before = Node("before")
        val after = Node("after")
        rig.a.detachChild(rig.child)
        rig.a.attachChild(before)
        rig.a.attachChild(rig.child)
        rig.a.attachChild(after)
        rig.root.updateGeometricState(0.0, true)
        val localBefore = com.ardor3d.math.Transform().set(rig.child.transform)

        val stack = CommandStack()
        stack.execute(ReparentCommand(child = rig.child, newParent = rig.b))
        rig.root.updateGeometricState(0.0, true)

        stack.undo()
        rig.root.updateGeometricState(0.0, true)

        assertSame(rig.a, rig.child.parent)
        assertEquals(listOf("before", "child", "after"), rig.a.children.map { it.name })
        assertTransformEquals(localBefore, rig.child.transform)
    }

    @Test
    fun redoMovesAgainPreservingWorldTransform() {
        val rig = Rig()
        val worldBefore = com.ardor3d.math.Transform().set(rig.child.worldTransform)

        val stack = CommandStack()
        stack.execute(ReparentCommand(child = rig.child, newParent = rig.b))
        rig.root.updateGeometricState(0.0, true)
        stack.undo()
        rig.root.updateGeometricState(0.0, true)
        stack.redo()
        rig.root.updateGeometricState(0.0, true)

        assertSame(rig.b, rig.child.parent)
        assertTransformEquals(worldBefore, rig.child.worldTransform)
    }

    @Test
    fun insertIndexPlacesChildAmongNewSiblings() {
        val rig = Rig()
        val b1 = Node("b1")
        val b2 = Node("b2")
        rig.b.attachChild(b1)
        rig.b.attachChild(b2)
        rig.root.updateGeometricState(0.0, true)
        val worldBefore = com.ardor3d.math.Transform().set(rig.child.worldTransform)

        val stack = CommandStack()
        stack.execute(ReparentCommand(child = rig.child, newParent = rig.b, insertIndex = 1))
        rig.root.updateGeometricState(0.0, true)

        assertEquals(listOf("b1", "child", "b2"), rig.b.children.map { it.name })
        assertTransformEquals(worldBefore, rig.child.worldTransform)

        stack.undo()
        rig.root.updateGeometricState(0.0, true)
        assertSame(rig.a, rig.child.parent)
        assertEquals(listOf("b1", "b2"), rig.b.children.map { it.name })
    }

    @Test
    fun validationRejectsBadTargets() {
        val rig = Rig()
        val grandchild = Node("grandchild")
        rig.child.attachChild(grandchild)

        // Self, own descendant, current parent and an unparented spatial are all invalid
        assertFalse(ReparentCommand.isValidReparent(rig.child, rig.child))
        assertFalse(ReparentCommand.isValidReparent(rig.a, rig.child))
        assertFalse(ReparentCommand.isValidReparent(rig.child, rig.a))
        assertFalse(ReparentCommand.isValidReparent(rig.root, rig.b))

        // A sibling node and the root are valid targets
        assertTrue(ReparentCommand.isValidReparent(rig.child, rig.b))
        assertTrue(ReparentCommand.isValidReparent(rig.child, rig.root))
    }
}
