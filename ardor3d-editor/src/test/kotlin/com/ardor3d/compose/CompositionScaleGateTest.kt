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

import com.ardor3d.scenegraph.Node
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.management.ManagementFactory

/**
 * Scale gates for the composed scene layer: the checkers board proved the paradigm at ~90 nodes,
 * and these assert the properties that must survive at 10k-50k. The load-bearing one is that the
 * cost of a change tracks the change, never the scene: one leaf write recomposes one leaf and
 * allocates the same bytes in a 50k-node scene as in a 1k-node one, keyed churn produces the same
 * applier traffic at any size, and an idle frame at 50k spatials is as silent and allocation-free
 * as the two-piece scene in [SceneCompositionTest].
 *
 * Every assertion here is a deterministic count - recompositions through the leaf-sync hook,
 * applier traffic through [CountingApplier], allocated bytes through thread-allocation accounting.
 * Wall-clock curves are deliberately not asserted; the env-gated [CompositionScaleBench] reports
 * those. Like the other measurement gates, a classic red->green is not constructible: a red here
 * is the runtime failing a scaling property, not a code path we could deliberately break.
 */
class CompositionScaleGateTest {

    /** A composition over a [CountingApplier] plus a leaf-recomposition counter. */
    private class Harness : AutoCloseable {
        val root = Node("root")
        val applier = CountingApplier(root)
        val scene = SceneComposition(applier)

        var leafSyncs = 0
            private set
        val onLeafSync: () -> Unit = { leafSyncs++ }

        private var t = 0L
        fun frame() = scene.frame(++t)

        fun resetCounts() {
            leafSyncs = 0
            applier.resetCounts()
        }

        override fun close() = scene.close()
    }

    /**
     * The allocation gates are only meaningful if thread-allocation accounting is actually on:
     * when it is unsupported or disabled, getThreadAllocatedBytes returns -1 for every call and
     * a before/after delta is 0 - the gate would pass vacuously. Fail loudly instead.
     */
    private fun allocationAccounting(): com.sun.management.ThreadMXBean {
        val threads = ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
        assertTrue(
            "thread allocation accounting must be supported and enabled for the allocation gates",
            threads.isThreadAllocatedMemorySupported && threads.isThreadAllocatedMemoryEnabled
        )
        return threads
    }

    private fun assertConfined(name: String, model: ScaleModel, pokeId: Int, compose: Harness.() -> Unit) {
        Harness().use { h ->
            h.compose()
            h.resetCounts()
            model.poke(pokeId, 1.0)
            h.frame()
            assertEquals("$name: one leaf write must recompose exactly one leaf", 1, h.leafSyncs)
            assertEquals("$name: one leaf write must cause no structural traffic", 0, h.applier.structuralOps)
        }
    }

    @Test
    fun aSingleLeafWriteRecomposesOnlyThatLeafInEveryShape() {
        val flat = ScaleModel(10_000)
        assertConfined("flat", flat, 5_000) { scene.setContent { FlatScene(flat, onLeafSync) } }

        val tree = ScaleModel(10_000) // branching 10, depth 4: 10^4 leaves
        assertConfined("tree", tree, 5_000) { scene.setContent { TreeScene(tree, 10, 4, onLeafSync) } }

        val entity = ScaleModel(3_333) // ~10k spatials at 3 per entity
        assertConfined("entity", entity, 1_666) { scene.setContent { EntityScene(entity, onLeafSync) } }
    }

    @Test
    fun scatteredWritesRecomposeExactlyThatManyLeaves() {
        val model = ScaleModel(10_000)
        Harness().use { h ->
            h.scene.setContent { FlatScene(model, h.onLeafSync) }
            h.resetCounts()

            val ids = (0 until 10).map { it * 1_000 + 37 }
            ids.forEach { model.poke(it, 2.0) }
            h.frame()

            assertEquals("K scattered writes must recompose exactly K leaves", ids.size, h.leafSyncs)
            assertEquals(0, h.applier.structuralOps)
        }
    }

    @Test
    fun leafWriteCostIsIndependentOfSceneSize() {
        val small = measureLeafWrite(1_000)
        val big = measureLeafWrite(50_000)

        assertEquals(
            "applier traffic for one leaf write must not depend on scene size",
            small.operations, big.operations
        )
        assertTrue(
            "allocation for one leaf write must not scale with scene size: " +
                "${small.medianBytes}B at 1k vs ${big.medianBytes}B at 50k leaves",
            big.medianBytes <= small.medianBytes * 2 + 512
        )
    }

    private class LeafWriteCost(val operations: Int, val medianBytes: Long)

    private fun measureLeafWrite(leafCount: Int): LeafWriteCost {
        val model = ScaleModel(leafCount)
        Harness().use { h ->
            h.scene.setContent { FlatScene(model) } // no sync hook: measure the bare path
            val threads = allocationAccounting()
            val self = Thread.currentThread().id
            var value = 1.0

            // Warm until JIT and the runtime's lazy caches are done allocating.
            repeat(2_000) { i ->
                model.poke(i % leafCount, value++)
                h.frame()
            }

            h.resetCounts()
            model.poke(leafCount / 2, value++)
            h.frame()
            val operations = h.applier.operations

            val samples = LongArray(9)
            for (i in samples.indices) {
                val before = threads.getThreadAllocatedBytes(self)
                model.poke((leafCount / 2 + i * 13) % leafCount, value++)
                h.frame()
                samples[i] = threads.getThreadAllocatedBytes(self) - before
            }
            samples.sort()
            return LeafWriteCost(operations, samples[samples.size / 2])
        }
    }

    @Test
    fun keyedChurnTrafficIsIndependentOfSceneSize() {
        val small = measureChurn(1_000)
        val big = measureChurn(50_000)

        assertEquals("traffic for removing one keyed leaf", small.remove, big.remove)
        assertEquals("traffic for inserting one keyed leaf", small.insert, big.insert)
        assertEquals("traffic for moving one keyed leaf", small.move, big.move)
    }

    private class ChurnTraffic(val remove: Int, val insert: Int, val move: Int)

    private fun measureChurn(leafCount: Int): ChurnTraffic {
        val model = ScaleModel(leafCount)
        Harness().use { h ->
            h.scene.setContent { FlatScene(model) }

            fun trafficFor(mutate: (List<Int>) -> List<Int>): Int {
                h.resetCounts()
                model.ids.value = mutate(model.ids.value)
                h.frame()
                return h.applier.operations
            }

            val mid = leafCount / 2
            val remove = trafficFor { it - mid }
            val insert = trafficFor { it.toMutableList().apply { add(mid, mid) } }
            val move = trafficFor { it.drop(1) + it.first() }
            return ChurnTraffic(remove, insert, move)
        }
    }

    @Test
    fun cellMembershipChurnIsIndependentOfSceneSize() {
        val small = measureCellChurn(1_000)
        val big = measureCellChurn(50_000)

        assertEquals("traffic for removing one leaf from a cell", small.removeOps, big.removeOps)
        assertEquals("traffic for re-inserting it", small.insertOps, big.insertOps)
        assertEquals("traffic for transferring one leaf between cells", small.transferOps, big.transferOps)

        for (churn in listOf(small, big)) {
            assertEquals("a cell remove must not rebuild any surviving leaf", 0, churn.removeRebuilds)
            assertEquals("a cell insert rebuilds exactly the inserted leaf", 1, churn.insertRebuilds)
            assertEquals(
                "a cross-cell transfer rebuilds exactly the moved leaf (keyed content does not cross parents)",
                1, churn.transferRebuilds
            )
        }
    }

    private class CellChurn(
        val removeOps: Int, val insertOps: Int, val transferOps: Int,
        val removeRebuilds: Int, val insertRebuilds: Int, val transferRebuilds: Int
    )

    private fun measureCellChurn(leafCount: Int): CellChurn {
        val cellSize = Math.round(Math.sqrt(leafCount.toDouble())).toInt()
        val model = CellModel(leafCount, cellSize)
        Harness().use { h ->
            h.scene.setContent { CellScene(model, h.onLeafSync) }

            // A rebuilt leaf announces itself: its first applied update fires the leaf-sync hook.
            fun opsAndRebuilds(mutate: () -> Unit): Pair<Int, Int> {
                h.resetCounts()
                mutate()
                h.frame()
                return h.applier.operations to h.leafSyncs
            }

            val c = model.cells.size / 2
            var pulled = -1
            val remove = opsAndRebuilds {
                val cell = model.cells[c].value
                pulled = cell[cell.size / 2]
                model.cells[c].value = cell - pulled
            }
            val insert = opsAndRebuilds {
                model.cells[c].value = model.cells[c].value.toMutableList().apply { add(size / 2, pulled) }
            }
            val transfer = opsAndRebuilds {
                val from = model.cells[c].value
                val id = from.first()
                model.cells[c].value = from.drop(1)
                model.cells[c + 1].value = model.cells[c + 1].value + id
            }
            return CellChurn(remove.first, insert.first, transfer.first, remove.second, insert.second, transfer.second)
        }
    }

    @Test
    fun idleFramesStaySilentAndAllocationFreeAt50kSpatials() {
        val model = ScaleModel(16_667) // 3 spatials per entity: ~50k emitted
        Harness().use { h ->
            h.scene.setContent { EntityScene(model, h.onLeafSync) }
            assertTrue("the scene must actually be at scale", countSpatials(h.root) >= 50_000)

            val threads = allocationAccounting()
            val self = Thread.currentThread().id
            repeat(10_000) { h.frame() } // warmup, mirroring the small-scene gate

            h.resetCounts()
            val before = threads.getThreadAllocatedBytes(self)
            repeat(10_000) { h.frame() }
            val allocated = threads.getThreadAllocatedBytes(self) - before

            assertEquals("an idle frame at 50k spatials must not allocate", 0L, allocated)
            assertEquals("an idle frame at 50k spatials must not touch the applier", 0, h.applier.operations)

            // And the quiet stretch must not have starved a real change.
            model.poke(8_000, 1.0)
            h.frame()
            assertEquals(1, h.leafSyncs)
        }
    }
}
