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
import com.ardor3d.scenegraph.Spatial

/**
 * A resolved hierarchy insertion point for an edge drop: either a move among the dragged
 * spatial's current siblings, or an indexed insert under a different parent.
 */
sealed interface Insertion {
    /** Move within the current parent to [newIndex] (post-removal indexing). */
    data class Reorder(val parent: Node, val newIndex: Int) : Insertion

    /** Insert under a different [parent] at [index]. */
    data class Insert(val parent: Node, val index: Int) : Insertion
}

object ReorderUtil {

    /**
     * True when [spatial] is [root] or lives somewhere under it - i.e. is still part of the
     * scene being edited. Drop handlers must check this on their resolved target: a drop spot
     * can go stale between hover and release (e.g. a mid-drag undo detaching the hovered
     * node), and inserting under a detached node makes the dragged object silently vanish.
     */
    fun isInScene(spatial: Spatial, root: Node): Boolean =
        spatial === root || spatial.hasAncestor(root)

    /**
     * Resolves dropping [child] on the edge of [row] - before its top edge, or when [after] is
     * true below its bottom edge - into a concrete [Insertion], or null when the drop is
     * invalid (root row, cycle, the row itself) or would change nothing.
     *
     * Dropping just below an expanded node with children ([rowExpanded]) inserts at the top of
     * that node's children, matching what an indicator line drawn between the node and its
     * first child suggests. Otherwise the insertion lands among [row]'s siblings.
     *
     * [row] is assumed to belong to the scene being edited; callers holding a possibly stale
     * row must verify with [isInScene] first (a parentless [row] here means the scene root).
     */
    fun resolveInsertion(
        child: Spatial,
        row: Spatial,
        after: Boolean,
        rowExpanded: Boolean = false
    ): Insertion? {
        if (child === row || child.parent == null) {
            return null
        }

        val parent: Node
        val rawIndex: Int
        if (after && rowExpanded && row is Node && row.numberOfChildren > 0) {
            parent = row
            rawIndex = 0
        } else {
            parent = row.parent ?: return null
            rawIndex = parent.children.indexOf(row) + if (after) 1 else 0
        }

        if (parent === child || (child is Node && parent.hasAncestor(child))) {
            return null
        }

        return if (parent === child.parent) {
            val current = parent.children.indexOf(child)
            // Lifting the child out shifts later siblings down one
            val newIndex = if (rawIndex > current) rawIndex - 1 else rawIndex
            if (newIndex == current) null else Insertion.Reorder(parent, newIndex)
        } else {
            Insertion.Insert(parent, rawIndex)
        }
    }
}
