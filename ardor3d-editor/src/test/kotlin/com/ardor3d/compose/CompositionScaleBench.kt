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

import androidx.compose.runtime.Composable
import com.ardor3d.scenegraph.Node
import org.junit.Assume
import org.junit.Test
import java.lang.management.ManagementFactory

/**
 * Curve bench for the composed scene layer. Not a gate: the deterministic pass/fail assertions
 * live in [CompositionScaleGateTest]; this prints the numbers the curves are read from - initial
 * composition and teardown time, retained memory per spatial against an imperative twin of the
 * same tree, leaf-write latency and allocation, many-writes-one-frame cost, and keyed-churn cost.
 * Churn against a single global list ("flat", "chunked") recomposes the scope that reads that
 * list and reconciles the whole keyed run - its curve locates the cliff where dynamic collections
 * must leave global-list composition. The "cells" shape is the stable-partition alternative that
 * is supposed to stay off that cliff; its churn rewrites one cell's membership state.
 *
 * The composed memory column includes the per-leaf MutableState objects the shapes read - the
 * state model is part of what the approach costs, so it is deliberately not subtracted.
 *
 * Run explicitly (env vars, not -D: Gradle does not forward system properties to test JVMs);
 * A3D_BENCH_SHAPES optionally limits the run to a comma-separated subset of shape names:
 *
 *   A3D_COMPOSE_BENCH=1 ./gradlew :ardor3d-editor:test --tests 'com.ardor3d.compose.CompositionScaleBench'
 */
class CompositionScaleBench {

    // Lazy so instantiating the (normally skipped) bench never touches com.sun.management.
    private val threads by lazy { ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean }
    private val self by lazy { Thread.currentThread().id }

    private class ChurnOps(val remove: () -> Unit, val insert: () -> Unit, val move: () -> Unit)

    private class BenchTarget(
        val leafCount: Int,
        val content: @Composable () -> Unit,
        val poke: (Int, Double) -> Unit,
        val churn: ChurnOps?
    )

    private class ShapeCase(
        val name: String,
        val imperative: () -> Node,
        val makeScene: () -> BenchTarget
    )

    /** Global-list churn: remove/re-insert the middle element, move the first to the end. */
    private fun listChurn(model: ScaleModel): ChurnOps {
        var pulled = -1
        return ChurnOps(
            remove = {
                val ids = model.ids.value
                pulled = ids[ids.size / 2]
                model.ids.value = ids - pulled
            },
            insert = { model.ids.value = model.ids.value.toMutableList().apply { add(size / 2, pulled) } },
            move = {
                val ids = model.ids.value
                model.ids.value = ids.drop(1) + ids.first()
            }
        )
    }

    /** Stable-partition churn: the same operations expressed as single-cell membership edits. */
    private fun cellChurn(model: CellModel): ChurnOps {
        val c = model.cells.size / 2
        var pulled = -1
        return ChurnOps(
            remove = {
                val cell = model.cells[c].value
                pulled = cell[cell.size / 2]
                model.cells[c].value = cell - pulled
            },
            insert = { model.cells[c].value = model.cells[c].value.toMutableList().apply { add(size / 2, pulled) } },
            move = { // transfer one leaf to the neighboring cell: the cross-parent case
                val from = model.cells[c].value
                val id = from.first()
                model.cells[c].value = from.drop(1)
                model.cells[c + 1].value = model.cells[c + 1].value + id
            }
        )
    }

    @Test
    fun printScaleCurves() {
        Assume.assumeTrue(
            "set A3D_COMPOSE_BENCH=1 to run the bench",
            System.getenv("A3D_COMPOSE_BENCH") == "1"
        )
        val only = System.getenv("A3D_BENCH_SHAPES")?.split(',')?.map { it.trim() }?.toSet()

        val rt = Runtime.getRuntime()
        println()
        println("## Composition scale bench")
        println()
        println(
            "JVM ${System.getProperty("java.version")}, max heap ${rt.maxMemory() / (1 shl 20)} MiB, " +
                "${rt.availableProcessors()} cores"
        )
        println()
        println(
            "| shape | spatials | initial ms | teardown ms | imperative B/sp | composed B/sp | overhead B/sp " +
                "| leaf write us | leaf write B | 100 writes us | remove us | insert us | move us |"
        )
        println("|---|---|---|---|---|---|---|---|---|---|---|---|---|")

        for (target in listOf(1_000, 10_000, 50_000)) {
            val depth = 4
            val branching = Math.round(Math.pow(target.toDouble(), 1.0 / depth)).toInt()
            val treeLeaves = generateSequence(1) { it * branching }.elementAt(depth)
            val entities = target / 3
            val chunkSize = Math.round(Math.sqrt(target.toDouble())).toInt()

            val cases = listOf(
                ShapeCase("flat", { buildFlatImperative(target) }) {
                    val m = ScaleModel(target)
                    BenchTarget(target, { FlatScene(m) }, m::poke, listChurn(m))
                },
                ShapeCase("chunked", { buildChunkedImperative(target, chunkSize) }) {
                    val m = ScaleModel(target)
                    BenchTarget(target, { ChunkedScene(m, chunkSize) }, m::poke, listChurn(m))
                },
                ShapeCase("cells", { buildChunkedImperative(target, chunkSize) }) {
                    val m = CellModel(target, chunkSize)
                    BenchTarget(target, { CellScene(m) }, m::poke, cellChurn(m))
                },
                ShapeCase("tree", { buildTreeImperative(branching, depth) }) {
                    val m = ScaleModel(treeLeaves)
                    BenchTarget(treeLeaves, { TreeScene(m, branching, depth) }, m::poke, null)
                },
                ShapeCase("entity", { buildEntityImperative(entities) }) {
                    val m = ScaleModel(entities)
                    BenchTarget(entities, { EntityScene(m) }, m::poke, listChurn(m))
                }
            )
            cases.filter { only == null || it.name in only }.forEach { println(measureCase(it)) }
        }
    }

    private fun measureCase(case: ShapeCase): String {
        // Retained memory: the imperative twin first, then the composed scene, deltas over settled heap.
        gcSettle()
        val baseline = usedHeap()
        var imperative: Node? = case.imperative()
        val spatialCount = countSpatials(imperative!!)
        gcSettle()
        val imperativeBytes = usedHeap() - baseline
        @Suppress("UNUSED_VALUE")
        imperative = null

        gcSettle()
        val composedBaseline = usedHeap()
        var target = case.makeScene()
        var scene = SceneComposition(Node("root"))
        scene.setContent(target.content)
        gcSettle()
        val composedBytes = usedHeap() - composedBaseline
        scene.close()

        // Initial composition and teardown, medians over fresh compositions; the last stays open
        // for the write and churn measurements, and its close supplies the final teardown sample.
        val initialNs = LongArray(5)
        val teardownNs = LongArray(5)
        for (i in 0 until 5) {
            target = case.makeScene()
            scene = SceneComposition(Node("root"))
            val t0 = System.nanoTime()
            scene.setContent(target.content)
            initialNs[i] = System.nanoTime() - t0
            if (i < 4) {
                val t1 = System.nanoTime()
                scene.close()
                teardownNs[i] = System.nanoTime() - t1
            }
        }

        var t = 0L
        fun frame() = scene.frame(++t)
        var value = 1.0
        val leafCount = target.leafCount

        repeat(1_000) { i ->
            target.poke(i % leafCount, value++)
            frame()
        }

        val writeNs = LongArray(9)
        val writeBytes = LongArray(9)
        for (i in writeNs.indices) {
            val id = (leafCount / 2 + i * 13) % leafCount
            val b0 = threads.getThreadAllocatedBytes(self)
            val t0 = System.nanoTime()
            target.poke(id, value++)
            frame()
            writeNs[i] = System.nanoTime() - t0
            writeBytes[i] = threads.getThreadAllocatedBytes(self) - b0
        }

        val k100Ns = LongArray(5)
        for (i in k100Ns.indices) {
            val t0 = System.nanoTime()
            repeat(100) { k -> target.poke((k * 97 + i) % leafCount, value++) }
            frame()
            k100Ns[i] = System.nanoTime() - t0
        }

        // The global-list shapes' churn runs seconds-per-op at 50k; medians of 3 over fewer warmup
        // cycles keep the bench's wall time sane where single samples are already second-scale.
        val churn = target.churn
        val churnReps = if (leafCount > 10_000) 3 else 5
        val churnWarmup = if (leafCount > 10_000) 2 else 10
        val removeNs = LongArray(churnReps)
        val insertNs = LongArray(churnReps)
        val moveNs = LongArray(churnReps)
        if (churn != null) {
            repeat(churnWarmup) { // warm the structural paths before timing them
                churn.move()
                frame()
            }
            for (i in removeNs.indices) {
                var t0 = System.nanoTime()
                churn.remove()
                frame()
                removeNs[i] = System.nanoTime() - t0

                t0 = System.nanoTime()
                churn.insert()
                frame()
                insertNs[i] = System.nanoTime() - t0

                t0 = System.nanoTime()
                churn.move()
                frame()
                moveNs[i] = System.nanoTime() - t0
            }
        }

        val tClose = System.nanoTime()
        scene.close()
        teardownNs[4] = System.nanoTime() - tClose

        fun ms(ns: LongArray) = "%.1f".format(median(ns) / 1e6)
        fun us(ns: LongArray) = "%.1f".format(median(ns) / 1e3)
        val churnCols = if (churn != null) "${us(removeNs)} | ${us(insertNs)} | ${us(moveNs)}" else "- | - | -"

        return "| ${case.name} | $spatialCount | ${ms(initialNs)} | ${ms(teardownNs)} " +
            "| ${imperativeBytes / spatialCount} | ${composedBytes / spatialCount} " +
            "| ${(composedBytes - imperativeBytes) / spatialCount} " +
            "| ${us(writeNs)} | ${median(writeBytes)} | ${us(k100Ns)} | $churnCols |"
    }

    private fun median(samples: LongArray): Long {
        val sorted = samples.sortedArray()
        return sorted[sorted.size / 2]
    }

    private fun gcSettle() = repeat(3) {
        System.gc()
        Thread.sleep(50)
    }

    private fun usedHeap(): Long = Runtime.getRuntime().let { it.totalMemory() - it.freeMemory() }
}
