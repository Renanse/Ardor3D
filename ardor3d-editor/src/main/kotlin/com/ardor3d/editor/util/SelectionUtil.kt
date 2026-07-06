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

import com.ardor3d.scenegraph.Spatial

/**
 * Selection helpers shared by scene operations.
 */
object SelectionUtil {

    /**
     * Filters [selection] down to its top-most spatials: any spatial whose ancestor is also
     * selected is dropped, since operating on the ancestor already covers it (deleting or
     * duplicating both a node and its child would otherwise process the child twice).
     * Selection order is preserved.
     */
    fun topMost(selection: List<Spatial>): List<Spatial> {
        val selected = selection.toHashSet()
        return selection.filter { spatial ->
            var ancestor = spatial.parent
            while (ancestor != null) {
                if (ancestor in selected) {
                    return@filter false
                }
                ancestor = ancestor.parent
            }
            true
        }
    }
}
