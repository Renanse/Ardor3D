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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.management.ManagementFactory

/**
 * The composition host and applier, tested headlessly - composing is pure scene-graph construction
 * (no GL, no materials): setContent emits its tree synchronously, state writes restructure only on
 * the next [SceneComposition.frame], keyed reorders move the existing nodes instead of rebuilding
 * them, attribute changes rewrite in place, and - the property the whole approach hangs on - an
 * idle frame produces zero applier traffic of any kind.
 */
class SceneCompositionTest {

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
    fun idleFramesAllocateNothing() {
        val root = Node("root")
        SceneComposition(root).use { scene ->
            var pieces by mutableStateOf(2)
            scene.setContent {
                SceneNode("pieces") {
                    repeat(pieces) { i -> SceneNode("piece_$i") }
                }
            }

            val threads = ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
            val self = Thread.currentThread().id
            // Warm up so JIT and any lazy caching inside the runtime are done allocating.
            repeat(10_000) { scene.frame(it.toLong()) }

            val before = threads.getThreadAllocatedBytes(self)
            repeat(10_000) { scene.frame(10_000L + it) }
            val allocated = threads.getThreadAllocatedBytes(self) - before

            assertEquals("an idle frame must not allocate on the Java heap", 0L, allocated)

            // And the gate must not have been trivially satisfied by a dead composition.
            pieces = 3
            scene.frame(20_001L)
            assertEquals(3, (root.children.single() as Node).getNumberOfChildren())
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
