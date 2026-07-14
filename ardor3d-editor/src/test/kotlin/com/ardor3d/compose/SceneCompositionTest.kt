/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ardor3d.scenegraph.Node
import com.ardor3d.scenegraph.Spatial
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The composition host and applier, tested headlessly - composing is pure scene-graph construction
 * (no GL, no materials): setContent emits its tree synchronously, state writes restructure only on
 * the next [SceneComposition.frame], keyed reorders move the existing nodes instead of rebuilding
 * them, attribute changes rewrite in place, and - the property the whole approach hangs on - an
 * idle frame produces zero applier traffic of any kind.
 */
class SceneCompositionTest {

    /** Instruments every applier entry point, structural and navigational, for the silence gate. */
    private class CountingApplier(root: Node) : SpatialApplier(root) {
        var structuralOps = 0
            private set
        var navigations = 0
            private set

        val operations: Int get() = structuralOps + navigations

        fun resetCounts() {
            structuralOps = 0
            navigations = 0
        }

        override fun insertTopDown(index: Int, instance: Spatial) {
            structuralOps++
            super.insertTopDown(index, instance)
        }

        override fun insertBottomUp(index: Int, instance: Spatial) {
            structuralOps++
            super.insertBottomUp(index, instance)
        }

        override fun remove(index: Int, count: Int) {
            structuralOps++
            super.remove(index, count)
        }

        override fun move(from: Int, to: Int, count: Int) {
            structuralOps++
            super.move(from, to, count)
        }

        override fun onClear() {
            structuralOps++
            super.onClear()
        }

        override fun down(node: Spatial) {
            navigations++
            super.down(node)
        }

        override fun up() {
            navigations++
            super.up()
        }
    }

    @Test
    fun setContentEmitsTheTreeSynchronously() {
        val root = Node("root")
        SceneComposition(root).use { scene ->
            scene.setContent {
                SceneNode("board") {
                    SceneNode("tiles")
                    SceneNode("pieces")
                }
            }

            val board = root.children.single() as Node
            assertEquals("board", board.name)
            assertEquals(listOf("tiles", "pieces"), board.children.map { it.name })
        }
    }

    @Test
    fun structuralChangeAppliesOnTheNextFrame() {
        val root = Node("root")
        SceneComposition(root).use { scene ->
            var crowned by mutableStateOf(false)
            scene.setContent {
                SceneNode("piece") {
                    if (crowned) SceneSpatial(factory = { Node("crown") })
                }
            }
            val piece = root.children.single() as Node
            assertEquals(0, piece.getNumberOfChildren())

            crowned = true
            assertEquals("a state write stays pending until the frame", 0, piece.getNumberOfChildren())
            scene.frame(1L)
            assertEquals(1, piece.getNumberOfChildren())
            assertEquals("crown", piece.children.single().name)

            crowned = false
            scene.frame(2L)
            assertEquals(0, piece.getNumberOfChildren())
        }
    }

    @Test
    fun keyedReorderMovesTheExistingNodes() {
        val root = Node("root")
        SceneComposition(root).use { scene ->
            var order by mutableStateOf(listOf("a", "b", "c"))
            scene.setContent {
                SceneNode("list") {
                    for (id in order) key(id) { SceneNode(id) }
                }
            }
            val list = root.children.single() as Node
            val a = list.getChild(0)
            val c = list.getChild(2)

            order = listOf("c", "a", "b")
            scene.frame(1L)

            assertEquals(listOf("c", "a", "b"), list.children.map { it.name })
            assertSame("the node moved rather than being rebuilt", a, list.getChild(1))
            assertSame(c, list.getChild(0))
        }
    }

    @Test
    fun removalDetachesAndOrphansTheDroppedNodes() {
        val root = Node("root")
        SceneComposition(root).use { scene ->
            var pieces by mutableStateOf(3)
            scene.setContent {
                SceneNode("pieces") {
                    repeat(pieces) { i -> SceneNode("piece_$i") }
                }
            }
            val parent = root.children.single() as Node
            assertEquals(3, parent.getNumberOfChildren())
            val dropped = parent.getChild(2)

            pieces = 1
            scene.frame(1L)

            assertEquals(1, parent.getNumberOfChildren())
            assertEquals("piece_0", parent.getChild(0).name)
            assertNull("a removed spatial is orphaned, not just hidden", dropped.parent)
        }
    }

    @Test
    fun attributeChangeRewritesInPlaceWithoutStructuralTraffic() {
        val root = Node("root")
        val applier = CountingApplier(root)
        SceneComposition(applier).use { scene ->
            var name by mutableStateOf("piece")
            scene.setContent { SceneNode(name) }
            val node = root.children.single()

            applier.resetCounts()
            name = "king"
            scene.frame(1L)

            assertSame("the same node instance is renamed, not replaced", node, root.children.single())
            assertEquals("king", node.name)
            assertEquals("an attribute change must not restructure", 0, applier.structuralOps)
        }
    }

    @Test
    fun idleFramesAreCompletelySilent() {
        val root = Node("root")
        val applier = CountingApplier(root)
        SceneComposition(applier).use { scene ->
            var pieces by mutableStateOf(2)
            scene.setContent {
                SceneNode("pieces") {
                    repeat(pieces) { i -> SceneNode("piece_$i") }
                }
            }
            assertTrue("initial composition reaches the applier", applier.operations > 0)

            applier.resetCounts()
            repeat(100) { scene.frame(it.toLong()) }
            assertEquals("an idle frame must not touch the applier at all", 0, applier.operations)

            // The quiet stretch must not have starved a real change: it still lands next frame.
            pieces = 3
            scene.frame(100L)
            assertTrue(applier.structuralOps > 0)
            assertEquals(3, (root.children.single() as Node).getNumberOfChildren())
        }
    }
}
