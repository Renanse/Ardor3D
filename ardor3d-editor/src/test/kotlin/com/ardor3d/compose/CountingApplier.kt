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
import com.ardor3d.scenegraph.Spatial

/**
 * Instruments every applier entry point, structural and navigational, so tests can assert not just
 * what the composed tree looks like but how much traffic it took to get there - including the
 * silence gates, which assert none at all, and the scale gates, which assert the traffic for a
 * change is the same in a 50k-node scene as in a 1k-node one.
 */
internal class CountingApplier(root: Node) : SpatialApplier(root) {
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
