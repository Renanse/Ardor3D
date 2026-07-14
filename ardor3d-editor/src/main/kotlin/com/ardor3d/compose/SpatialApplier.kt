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

import androidx.compose.runtime.AbstractApplier
import com.ardor3d.scenegraph.Node
import com.ardor3d.scenegraph.Spatial

/**
 * The Compose [applier][androidx.compose.runtime.Applier] over the Ardor3D scene graph: it maps the
 * runtime's structural tree changes (insert/remove/move) onto a [Node]'s child list, so a
 * composition can emit and maintain spatials the way Compose UI maintains its widget tree. All
 * structural work goes through [Node.attachChildAt]/[Node.detachChildAt] - never the raw child
 * list - so parenting and dirty-flag bookkeeping stay exactly what imperative scene code produces.
 *
 * Only structural changes flow through here; attribute updates from `set`/`update` blocks run
 * directly against the emitted spatial. Open so tests can instrument the traffic.
 */
open class SpatialApplier(root: Node) : AbstractApplier<Spatial>(root) {

    override fun insertTopDown(index: Int, instance: Spatial) {
        // Subtrees are built bottom-up (see below); nothing to do on the way down.
    }

    override fun insertBottomUp(index: Int, instance: Spatial) {
        // Bottom-up means a new subtree is fully assembled while still detached and attaches to the
        // live tree exactly once, so attach bookkeeping propagates into it once, not per descendant.
        currentNode().attachChildAt(instance, index)
    }

    override fun remove(index: Int, count: Int) {
        val node = currentNode()
        repeat(count) { node.detachChildAt(index) }
    }

    override fun move(from: Int, to: Int, count: Int) {
        if (from == to || count == 0) return
        val node = currentNode()
        // The applier contract gives `to` relative to the child list before the removal.
        val dest = if (from > to) to else to - count
        if (count == 1) {
            node.attachChildAt(node.detachChildAt(from), dest)
        } else {
            val moved = ArrayList<Spatial>(count)
            repeat(count) { moved.add(node.detachChildAt(from)) }
            moved.forEachIndexed { i, child -> node.attachChildAt(child, dest + i) }
        }
    }

    override fun onClear() {
        (root as Node).detachAllChildren()
    }

    private fun currentNode(): Node = current as? Node
        ?: error("Spatials can only be composed under a Node; current is ${current.javaClass.simpleName} '${current.name}'")
}
