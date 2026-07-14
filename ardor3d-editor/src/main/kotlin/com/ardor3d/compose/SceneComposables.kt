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
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.Updater
import com.ardor3d.scenegraph.Node
import com.ardor3d.scenegraph.Spatial

/**
 * A [Node] in the composed scene - the grouping composable. [content] composes its children; the
 * node instance is created once and kept across recompositions, so children come and go without
 * their siblings being rebuilt.
 */
@Composable
fun SceneNode(name: String, content: @Composable () -> Unit = {}) {
    ComposeNode<Node, SpatialApplier>(
        factory = { Node(name) },
        update = {
            set(name) { this.name = it }
        },
        content = content
    )
}

/**
 * Emits the [Spatial] built by [factory] as a leaf of the composed scene - the doorway for concrete
 * content (shapes, imported models, lights). [factory] runs once, when the spatial enters the
 * composition; [update] runs against that instance on every applying recomposition, with
 * [Updater.set]/[Updater.update] deciding what actually changed and skipping the rest.
 */
@Composable
fun <S : Spatial> SceneSpatial(
    factory: () -> S,
    update: @DisallowComposableCalls Updater<S>.() -> Unit = {}
) {
    ComposeNode<S, SpatialApplier>(factory = factory, update = update)
}
