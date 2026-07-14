/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.editor.play

import com.ardor3d.scenegraph.Node
import com.ardor3d.util.export.binary.BinaryExporter
import com.ardor3d.util.export.binary.BinaryImporter
import java.io.ByteArrayOutputStream

/**
 * The editor's edit/play boundary: Unity-style snapshot-on-enter, discard-on-exit.
 *
 * On [start] the current document is serialized to a `byte[]` with the same [BinaryExporter] the
 * editor writes `.a3d` files with; play-mode code (a game, in a later phase) then mutates the live
 * document freely. On [stop] the snapshot is deserialized back into a fresh document that replaces
 * the mutated one, so every play-mode change is discarded and the pre-play scene is restored
 * exactly. Because the game mutates the scene directly - never through editor commands - play never
 * enters the edit-mode undo history.
 *
 * The snapshot/restore pair is deliberately GL-free and side-effect-free (it only reads and builds
 * scene graphs), so it can be unit-tested headlessly and reused wherever a scene needs a disposable
 * working copy. Installing the restored root back into the running editor - swapping the document,
 * reindexing, re-deriving materials - is the caller's job, as is routing play-mode input to the
 * game (see [GameInputController]) and driving its [GameMode].
 */
class PlaySession {
    // The pre-play snapshot, held for the lifetime of the session. Non-null iff a session is active.
    private var snapshotBytes: ByteArray? = null

    /** True between [start] and [stop]. */
    val isActive: Boolean get() = snapshotBytes != null

    /**
     * Enters play: captures a byte-for-byte snapshot of [document] so [stop] can restore it. The
     * document itself is left untouched and stays live for play-mode code to mutate.
     */
    fun start(document: Node) {
        check(!isActive) { "play session already active" }
        snapshotBytes = serialize(document)
    }

    /**
     * Leaves play: rebuilds the pre-play document from the snapshot (discarding all play-mode
     * mutations) and ends the session. The returned [Node] is a fresh graph the caller installs as
     * the new document root.
     */
    fun stop(): Node {
        val bytes = snapshotBytes ?: error("play session not active")
        val restored = deserialize(bytes)
        snapshotBytes = null
        return restored
    }

    /**
     * The exact bytes captured at [start], or null when no session is active. Exposed so a test can
     * confirm the snapshot faithfully captures the pre-play document.
     */
    val snapshot: ByteArray? get() = snapshotBytes

    companion object {
        /** Serializes [node] to Ardor3D's binary format - the same format `.a3d` files use. */
        fun serialize(node: Node): ByteArray {
            val out = ByteArrayOutputStream()
            BinaryExporter().save(node, out)
            return out.toByteArray()
        }

        /** Rebuilds a scene-graph [Node] from bytes produced by [serialize]. */
        fun deserialize(bytes: ByteArray): Node =
            BinaryImporter().load(bytes) as? Node
                ?: throw IllegalStateException("snapshot did not contain a scene-root Node")
    }
}
