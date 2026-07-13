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

import com.ardor3d.math.Quaternion
import com.ardor3d.math.Vector3
import com.ardor3d.scenegraph.Node
import com.ardor3d.scenegraph.shape.Box
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Headless test of the edit/play boundary primitive: entering play snapshots the document, and
 * leaving it rebuilds the pre-play scene from that snapshot no matter what play mode did to the live
 * scene in between. This is the "does [com.ardor3d.util.export.binary.BinaryExporter] round-trip
 * everything?" check the game-mode plan calls out as Phase 1's risk; it needs no GL context, so it
 * always runs.
 *
 * Note on fidelity: the invariant is *semantic*, not byte-for-byte. Re-serializing a deserialized
 * graph produces output of identical length but with the id/location table renumbered - equal-
 * content objects that shared an instance in the authored graph come back as distinct instances, so
 * the exporter is not a byte-level fixed point. That is benign (no data is lost or added) and lives
 * in core, which this work must not touch; what matters here is that the restored scene faithfully
 * reproduces the pre-play structure, transforms, and properties while discarding play's mutations.
 */
class PlaySessionTest {

    /** A small but non-trivial document: nested nodes, meshes, non-identity transforms, a property. */
    private fun buildDocument(): Node {
        val root = Node("Scene Root")

        val group = Node("Group")
        group.setTranslation(1.0, 2.0, 3.0)
        group.setRotation(Quaternion().fromAngleAxis(Math.PI / 3, Vector3.UNIT_Y))

        val box = Box("Box", Vector3.ZERO, 0.5, 0.5, 0.5)
        box.setTranslation(0.0, 0.5, 0.0)
        box.setScale(2.0, 1.0, 1.0)
        box.setProperty("gameSquare", "e4")   // spatial property must survive the round trip (Phase 3)
        group.attachChild(box)

        root.attachChild(group)
        root.attachChild(Box("LooseBox", Vector3.ZERO, 1.0, 1.0, 1.0))
        return root
    }

    @Test
    fun startSnapshotsAndStopRestoresPreplayScene() {
        val document = buildDocument()
        val before = PlaySession.serialize(document)

        val session = PlaySession()
        assertFalse("a fresh session is inactive", session.isActive)

        session.start(document)
        assertTrue("session is active after start", session.isActive)
        assertNotNull("start captures a snapshot", session.snapshot)
        assertArrayEquals("the held snapshot is the pre-play document's serialized form",
            before, session.snapshot)

        // Play mode mutates the live document freely - add, remove, move things.
        document.attachChild(Node("PlayerSpawn"))
        document.detachChildAt(0)
        document.setTranslation(99.0, 99.0, 99.0)

        val restored = session.stop()
        assertFalse("session is inactive after stop", session.isActive)

        // The restored graph reproduces the pre-play scene exactly, structure and data, with every
        // play-mode mutation discarded.
        assertEquals("Scene Root", restored.name)
        assertEquals("root children restored (mutations discarded)", 2, restored.numberOfChildren)
        assertVector("root translation is the authored identity", Vector3(0.0, 0.0, 0.0), restored.translation)

        val group = restored.getChild(0) as Node
        assertEquals("Group", group.name)
        assertVector("group translation preserved", Vector3(1.0, 2.0, 3.0), group.translation)
        assertEquals("group child preserved", 1, group.numberOfChildren)

        val box = group.getChild(0)
        assertEquals("Box", box.name)
        assertVector("box translation preserved", Vector3(0.0, 0.5, 0.0), box.translation)
        assertVector("box scale preserved", Vector3(2.0, 1.0, 1.0), box.scale)
        assertEquals("box spatial property preserved", "e4", box.getProperty("gameSquare", ""))

        assertEquals("LooseBox", restored.getChild(1).name)
    }

    @Test
    fun restoredDocumentIsIndependentOfTheMutatedOne() {
        val document = buildDocument()
        val session = PlaySession()
        session.start(document)

        val restored = session.stop()

        // The restored graph is a fresh copy: further edits to the (discarded) play document can't
        // touch it, and it carries the original structure.
        document.attachChild(Node("StrayEdit"))
        assertEquals("restored document has the original child count",
            2, restored.numberOfChildren)
        assertEquals("restored group is preserved", "Group", restored.getChild(0).name)
    }

    @Test
    fun stopWithoutStartFails() {
        val session = PlaySession()
        try {
            session.stop()
            org.junit.Assert.fail("stop() on an inactive session should throw")
        } catch (expected: IllegalStateException) {
            // expected
        }
    }

    private fun assertVector(message: String, expected: Vector3, actual: com.ardor3d.math.type.ReadOnlyVector3) {
        assertEquals("$message (x)", expected.x, actual.x, 1e-9)
        assertEquals("$message (y)", expected.y, actual.y, 1e-9)
        assertEquals("$message (z)", expected.z, actual.z, 1e-9)
    }
}
