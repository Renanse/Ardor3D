/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.editor.checkers

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ardor3d.compose.SceneComposition
import com.ardor3d.compose.SpatialApplier
import com.ardor3d.math.ColorRGBA
import com.ardor3d.scenegraph.Node
import com.ardor3d.scenegraph.Spatial
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * The composed scene view of the board, tested headlessly - composing is pure scene-graph work (no
 * GL, no materials): the opening lays out 24 pieces on the right squares, a capture restructures on
 * the next frame, crowning adds the crown to the *same* piece node, a plain move is zero structural
 * traffic (the node survives; only its translation snaps), highlight markers recolor in place, and
 * pickable tiles map back to their squares.
 */
class BoardSceneTest {

    /** Counts structural applier traffic, so tests can assert what a change did (or didn't) touch. */
    private class CountingApplier(root: Node) : SpatialApplier(root) {
        var structural = 0
            private set

        fun reset() {
            structural = 0
        }

        override fun insertTopDown(index: Int, instance: Spatial) {
            structural++
            super.insertTopDown(index, instance)
        }

        override fun insertBottomUp(index: Int, instance: Spatial) {
            structural++
            super.insertBottomUp(index, instance)
        }

        override fun remove(index: Int, count: Int) {
            structural++
            super.remove(index, count)
        }

        override fun move(from: Int, to: Int, count: Int) {
            structural++
            super.move(from, to, count)
        }

        override fun onClear() {
            structural++
            super.onClear()
        }
    }

    /** A composed board over mutable snapshot state, driven frame by frame. */
    private class Harness(initial: Board = Board.initial()) : AutoCloseable {
        val root = Node("Checkers")
        val applier = CountingApplier(root)
        var board by mutableStateOf(initial)
        var highlights by mutableStateOf<Map<Square, HighlightKind>>(emptyMap())

        var syncs = 0
            private set

        private val composition = SceneComposition(applier)
        private var frames = 0L

        init {
            composition.setContent {
                BoardScene(board = { board }, highlights = { highlights }, onSync = { syncs++ })
            }
        }

        fun frame() = composition.frame(++frames)

        override fun close() = composition.close()
    }

    private fun sq(row: Int, col: Int) = Square(row, col)

    @Test
    fun openingLaysOutTwentyFourPieces() {
        Harness().use { h ->
            val pieces = h.root.getChild("Pieces") as Node
            assertEquals(24, pieces.numberOfChildren)
            assertNotNull("a red man sits on (0,0)", pieceNodeAt(h.root, sq(0, 0)))
            assertNotNull("a black man sits on (7,7)", pieceNodeAt(h.root, sq(7, 7)))
            assertNull("the middle rows are empty", pieceNodeAt(h.root, sq(3, 1)))

            // The piece node sits at the square's world position.
            val node = pieceNodeAt(h.root, sq(0, 0))!!
            val expected = worldPositionOf(sq(0, 0))
            assertEquals(expected.x, node.translation.x, 1e-9)
            assertEquals(expected.z, node.translation.z, 1e-9)
        }
    }

    @Test
    fun captureRestructuresOnTheNextFrame() {
        val board = Board.of(PieceColor.RED, mapOf(sq(2, 2) to Piece(PieceColor.RED), sq(3, 3) to Piece(PieceColor.BLACK)))
        Harness(board).use { h ->
            val pieces = h.root.getChild("Pieces") as Node
            assertEquals(2, pieces.numberOfChildren)
            val victim = pieceNodeAt(h.root, sq(3, 3))!!

            h.board = h.board.apply(h.board.legalMoves().single()) // the forced jump to (4,4)
            h.frame()

            assertEquals(1, pieces.numberOfChildren)
            assertNotNull(pieceNodeAt(h.root, sq(4, 4)))
            assertNull("the captured piece's node is gone", pieceNodeAt(h.root, sq(3, 3)))
            assertNull("and orphaned", victim.parent)
        }
    }

    @Test
    fun plainMoveIsZeroStructuralTrafficAndKeepsTheNode() {
        val board = Board.of(PieceColor.RED, mapOf(sq(2, 2) to Piece(PieceColor.RED), sq(7, 7) to Piece(PieceColor.BLACK)))
        Harness(board).use { h ->
            val node = pieceNodeAt(h.root, sq(2, 2))!!

            h.applier.reset()
            h.board = h.board.apply(h.board.legalMovesFrom(sq(2, 2)).first { it.to == sq(3, 3) })
            h.frame()

            assertEquals("a plain move must not restructure the scene", 0, h.applier.structural)
            assertSame("the piece keeps its node", node, pieceNodeAt(h.root, sq(3, 3)))
            val dest = worldPositionOf(sq(3, 3))
            assertEquals(dest.x, node.translation.x, 1e-9)
            assertEquals(dest.z, node.translation.z, 1e-9)
        }
    }

    @Test
    fun crowningAddsTheCrownToTheSamePieceNode() {
        val board = Board.of(PieceColor.RED, mapOf(sq(6, 2) to Piece(PieceColor.RED), sq(0, 0) to Piece(PieceColor.BLACK)))
        Harness(board).use { h ->
            val node = pieceNodeAt(h.root, sq(6, 2))!!
            assertEquals("a man is just its disk", 1, node.numberOfChildren)
            assertEquals(false, node.getProperty(PROP_KING, true))

            h.board = h.board.apply(h.board.legalMovesFrom(sq(6, 2)).first { it.to == sq(7, 3) })
            h.frame()

            assertSame("crowning keeps the node", node, pieceNodeAt(h.root, sq(7, 3)))
            assertEquals("and adds the crown", 2, node.numberOfChildren)
            assertEquals(true, node.getProperty(PROP_KING, false))
        }
    }

    @Test
    fun highlightMarkersRecolorInPlace() {
        Harness().use { h ->
            h.highlights = mapOf(sq(3, 1) to HighlightKind.MOVE)
            h.frame()
            val markers = h.root.getChild("Highlights") as Node
            assertEquals(1, markers.numberOfChildren)
            val marker = markers.getChild(0)
            val moveColor = ColorRGBA(marker.getDefaultColor())

            h.applier.reset()
            h.highlights = mapOf(sq(3, 1) to HighlightKind.MOVE_HOVER)
            h.frame()

            assertSame("the same marker, recolored", marker, markers.getChild(0))
            assertNotEquals(moveColor, ColorRGBA(marker.getDefaultColor()))
            assertEquals("a kind change must not restructure", 0, h.applier.structural)

            h.highlights = emptyMap()
            h.frame()
            assertEquals(0, markers.numberOfChildren)
        }
    }

    @Test
    fun recompositionConfinesItselfToTheChangedSubtree() {
        Harness().use { h ->
            val base = h.syncs // the initial composition of the pieces and highlights subtrees

            h.highlights = mapOf(sq(2, 0) to HighlightKind.HOVER_PIECE)
            h.frame()
            assertEquals("a highlight change recomposes only the highlights", base + 1, h.syncs)

            h.board = h.board.apply(h.board.legalMoves().first())
            h.frame()
            assertEquals("a board change recomposes only the pieces", base + 2, h.syncs)
        }
    }

    @Test
    fun tilesMapBackToTheirSquares() {
        Harness().use { h ->
            val tiles = h.root.getChild("Tiles") as Node
            assertEquals(64, tiles.numberOfChildren)

            val playable = tiles.children.first { it.name == "tile_2_0" }
            assertEquals(sq(2, 0), squareForTile(playable))

            val light = tiles.children.first { it.name == "tile_2_1" }
            assertNull("light tiles are not click targets", squareForTile(light))
        }
    }
}
