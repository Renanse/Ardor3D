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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import com.ardor3d.scenegraph.Node

/**
 * Scene shapes for measuring the composition layer at scale, each a pure function from a
 * [ScaleModel] to the composed tree. Every leaf reads its own state inside its own scope, so
 * writing one leaf's state invalidates exactly that leaf - the property the scale gates assert
 * still holds at 50k nodes. Three shapes bracket how scenes stress the slot table differently:
 *
 * - [FlatScene]: every leaf a keyed sibling under one node - the sibling-list worst case.
 * - [TreeScene]: fixed-depth branching interior - slot-table depth without list width.
 * - [EntityScene]: many small keyed subtrees (a group node holding two leaves) - the shape real
 *   scenes are actually made of.
 */
internal open class LeafStates(val leafCount: Int) {
    private val states = ArrayList<MutableState<Double>>(leafCount)

    /** One state per leaf, grown on demand so churn can introduce ids beyond [leafCount]. */
    fun offset(id: Int): MutableState<Double> {
        while (states.size <= id) states.add(mutableStateOf(0.0))
        return states[id]
    }

    /** Writes a leaf's state with a value guaranteed to differ, so the write always invalidates. */
    fun poke(id: Int, value: Double) {
        offset(id).value = value
    }
}

internal class ScaleModel(leafCount: Int) : LeafStates(leafCount) {
    /** Keyed membership and order of the leaves/entities; the churn scenarios rewrite this. */
    val ids: MutableState<List<Int>> = mutableStateOf(List(leafCount) { it })
}

/**
 * Stable-partition model: each cell owns its membership list as its own state, so churn rewrites
 * one cell and invalidates one cell's scope - the composition pattern the engine would actually
 * use for a large dynamic collection, as opposed to [ScaleModel.ids]'s single global list.
 */
internal class CellModel(leafCount: Int, cellSize: Int) : LeafStates(leafCount) {
    val cells: List<MutableState<List<Int>>> =
        (0 until (leafCount + cellSize - 1) / cellSize).map { c ->
            mutableStateOf(((c * cellSize) until minOf((c + 1) * cellSize, leafCount)).toList())
        }
}

/**
 * One leaf of a scale scene. The state read sits inside [SceneSpatial]'s update, so an [offset]
 * write invalidates only that innermost scope - tighter even than this function's own scope, which
 * is why [onLeafSync] hooks the `set` apply block rather than a SideEffect here: it fires exactly
 * when this leaf's node has its translation rewritten, the precise "only the changed leaf was
 * touched" signal the scale gates count.
 */
@Composable
internal fun BenchLeaf(id: Int, offset: State<Double>, onLeafSync: (() -> Unit)? = null) {
    SceneSpatial(
        factory = { Node("leaf_$id") },
        update = {
            set(offset.value) {
                setTranslation(it, 0.0, 0.0)
                onLeafSync?.invoke()
            }
        }
    )
}

/** All leaves as keyed siblings under a single node. */
@Composable
internal fun FlatScene(model: ScaleModel, onLeafSync: (() -> Unit)? = null) {
    SceneNode("flat") {
        for (id in model.ids.value) {
            key(id) { BenchLeaf(id, model.offset(id), onLeafSync) }
        }
    }
}

/**
 * A balanced tree of [SceneNode] interiors with [BenchLeaf] leaves: `branching^depth` leaves, ids
 * assigned positionally so the same id always lands at the same spot.
 */
@Composable
internal fun TreeScene(model: ScaleModel, branching: Int, depth: Int, onLeafSync: (() -> Unit)? = null) {
    val leaves = generateSequence(1) { it * branching }.elementAt(depth)
    require(model.leafCount == leaves) {
        "TreeScene emits exactly branching^depth leaves; model has ${model.leafCount}, shape yields $leaves"
    }
    TreeLevel(0, model.leafCount, branching, depth, model, onLeafSync)
}

@Composable
private fun TreeLevel(
    base: Int,
    span: Int,
    branching: Int,
    depth: Int,
    model: ScaleModel,
    onLeafSync: (() -> Unit)?
) {
    if (depth == 0) {
        BenchLeaf(base, model.offset(base), onLeafSync)
    } else {
        SceneNode("n_$base") {
            val childSpan = span / branching
            repeat(branching) { i ->
                TreeLevel(base + i * childSpan, childSpan, branching, depth - 1, model, onLeafSync)
            }
        }
    }
}

/**
 * Entity-style scene: each id is a small keyed subtree - a group node holding a state-reading body
 * leaf and a static trim leaf - so a scene of E entities emits 3E+1 spatials.
 */
@Composable
internal fun EntityScene(model: ScaleModel, onLeafSync: (() -> Unit)? = null) {
    SceneNode("entities") {
        for (id in model.ids.value) {
            key(id) {
                SceneNode("entity_$id") {
                    BenchLeaf(id, model.offset(id), onLeafSync)
                    SceneSpatial(factory = { Node("trim_$id") })
                }
            }
        }
    }
}

/**
 * [FlatScene]'s leaves regrouped under fixed-size keyed chunk nodes, chunked *positionally* from
 * the single global ids list. Measured verdict: this rescues little (~3x, not orders of
 * magnitude) - a global edit shifts membership across chunk boundaries, and keyed content does
 * not move between parents in Compose, so shifted leaves are disposed and recreated chunk after
 * chunk. Kept as the negative control for [CellScene], the stable-partition pattern that
 * actually works.
 */
@Composable
internal fun ChunkedScene(model: ScaleModel, chunkSize: Int, onLeafSync: (() -> Unit)? = null) {
    SceneNode("chunked") {
        val chunks = model.ids.value.chunked(chunkSize)
        for ((index, chunk) in chunks.withIndex()) {
            key(index) {
                SceneNode("chunk_$index") {
                    for (id in chunk) {
                        key(id) { BenchLeaf(id, model.offset(id), onLeafSync) }
                    }
                }
            }
        }
    }
}

/**
 * Stable-partition scene over a [CellModel]: leaves live under the cell whose membership list
 * names them, and a membership edit invalidates exactly that cell's scope. One sharp edge is part
 * of what this shape measures: a leaf transferred between cells crosses keyed parents, so Compose
 * disposes and recreates it - membership churn is cheap, but a transfer costs one node rebuild.
 */
@Composable
internal fun CellScene(model: CellModel, onLeafSync: (() -> Unit)? = null) {
    SceneNode("cells") {
        for ((index, cell) in model.cells.withIndex()) {
            key(index) {
                SceneNode("cell_$index") {
                    for (id in cell.value) {
                        key(id) { BenchLeaf(id, model.offset(id), onLeafSync) }
                    }
                }
            }
        }
    }
}

/** Counts every spatial under (and including) [root] - the honest denominator for per-node costs. */
internal fun countSpatials(root: Node): Int {
    var count = 1
    for (child in root.children) {
        count += if (child is Node) countSpatials(child) else 1
    }
    return count
}

/** Imperative twin of [FlatScene], for the memory baseline: what the scene graph alone costs. */
internal fun buildFlatImperative(leafCount: Int): Node {
    val root = Node("flat")
    repeat(leafCount) { i -> root.attachChild(Node("leaf_$i")) }
    return root
}

/** Imperative twin of [TreeScene]. */
internal fun buildTreeImperative(branching: Int, depth: Int): Node {
    if (depth == 0) return Node("leaf")
    val node = Node("n")
    repeat(branching) { node.attachChild(buildTreeImperative(branching, depth - 1)) }
    return node
}

/** Imperative twin of [ChunkedScene]. */
internal fun buildChunkedImperative(leafCount: Int, chunkSize: Int): Node {
    val root = Node("chunked")
    var chunkIndex = 0
    var i = 0
    while (i < leafCount) {
        val chunkNode = Node("chunk_$chunkIndex")
        val end = minOf(i + chunkSize, leafCount)
        while (i < end) {
            chunkNode.attachChild(Node("leaf_$i"))
            i++
        }
        root.attachChild(chunkNode)
        chunkIndex++
    }
    return root
}

/** Imperative twin of [EntityScene]. */
internal fun buildEntityImperative(entityCount: Int): Node {
    val root = Node("entities")
    repeat(entityCount) { i ->
        val entity = Node("entity_$i")
        entity.attachChild(Node("leaf_$i"))
        entity.attachChild(Node("trim_$i"))
        root.attachChild(entity)
    }
    return root
}
