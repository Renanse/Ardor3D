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

import com.ardor3d.math.ColorRGBA
import com.ardor3d.math.Quaternion
import com.ardor3d.math.Vector3
import com.ardor3d.scenegraph.Node
import com.ardor3d.scenegraph.Spatial
import com.ardor3d.scenegraph.hint.PickingHint
import com.ardor3d.scenegraph.shape.Box
import com.ardor3d.scenegraph.shape.Cylinder
import java.util.IdentityHashMap

/**
 * The scene-graph *view* of a checkers [Board] - the counterpart to the pure model in the plan's
 * state/view split. It builds a procedural board and pieces under [root] and re-syncs the pieces
 * from a board on demand; it never contains game rules, only presentation and the click-target
 * mapping the game needs.
 *
 * Building and syncing is plain scene-graph work (no GL, no materials), so it is headless-testable;
 * the caller assigns materials (e.g. `MaterialUtil.autoMaterials(root)`) once it is in the editor.
 *
 * Picking model: only the playable board tiles are pickable and mapped to squares; pieces and
 * highlights are non-pickable, so a click always resolves to the tile (hence square) beneath the
 * pointer, and the model decides what is there.
 */
class CheckersView {
    /** Attach this under the play scene root; discarded when play stops. */
    val root = Node("Checkers")

    private val tilesRoot = Node("Tiles")
    private val piecesRoot = Node("Pieces")
    private val highlightsRoot = Node("Highlights")

    // Pickable playable tiles -> their square, and square -> the piece node currently shown on it.
    private val squareByTile = IdentityHashMap<Spatial, Square>()
    private val pieceNodeBySquare = mutableMapOf<Square, Node>()

    init {
        root.attachChild(tilesRoot)
        root.attachChild(piecesRoot)
        root.attachChild(highlightsRoot)
        buildTiles()
    }

    /** World-space centre of the top face of [square] (pieces sit here). */
    fun worldPositionOf(square: Square): Vector3 =
        Vector3(square.col - OFFSET, PIECE_Y, square.row - OFFSET)

    /** The square of a picked tile spatial, or null if it was not a pickable tile. */
    fun squareForTile(spatial: Spatial): Square? = squareByTile[spatial]

    /** The piece node currently shown on [square], or null. */
    fun pieceNodeAt(square: Square): Node? = pieceNodeBySquare[square]

    /** Number of piece spatials currently shown (test/inspection aid). */
    val pieceCount: Int get() = pieceNodeBySquare.size

    /** Rebuilds the pieces to match [board] exactly (add/move/remove/crown), snapping positions. */
    fun rebuild(board: Board) {
        piecesRoot.detachAllChildren()
        pieceNodeBySquare.clear()
        for ((square, piece) in board.occupied) {
            val node = buildPiece(piece, square)
            node.setTranslation(worldPositionOf(square))
            piecesRoot.attachChild(node)
            pieceNodeBySquare[square] = node
        }
    }

    /** Shows a legal-destination marker on each of [squares]; replaces any previous markers. */
    fun showHighlights(squares: Collection<Square>) {
        highlightsRoot.detachAllChildren()
        for (square in squares) {
            val marker = Box("highlight", Vector3.ZERO, 0.42, 0.03, 0.42)
            marker.setDefaultColor(HIGHLIGHT_COLOR)
            marker.setTranslation(square.col - OFFSET, TILE_TOP + 0.02, square.row - OFFSET)
            marker.sceneHints.setPickingHint(PickingHint.Pickable, false)
            highlightsRoot.attachChild(marker)
        }
    }

    /** Clears any destination markers. */
    fun clearHighlights() {
        highlightsRoot.detachAllChildren()
    }

    // --- construction ------------------------------------------------------------------------

    private fun buildTiles() {
        for (row in 0 until Board.SIZE) {
            for (col in 0 until Board.SIZE) {
                val square = Square(row, col)
                val tile = Box("tile_${row}_$col", Vector3.ZERO, 0.5, TILE_HALF_HEIGHT, 0.5)
                tile.setTranslation(col - OFFSET, TILE_TOP - TILE_HALF_HEIGHT, row - OFFSET)
                if (square.isPlayable) {
                    tile.setDefaultColor(DARK_TILE)
                    squareByTile[tile] = square // only playable tiles are click targets
                } else {
                    tile.setDefaultColor(LIGHT_TILE)
                    tile.sceneHints.setPickingHint(PickingHint.Pickable, false)
                }
                tilesRoot.attachChild(tile)
            }
        }
    }

    private fun buildPiece(piece: Piece, square: Square): Node {
        val node = Node("piece_${square.row}_${square.col}")
        val color = if (piece.color == PieceColor.RED) RED_PIECE else BLACK_PIECE

        val disk = Cylinder("disk", 2, 24, PIECE_RADIUS, PIECE_HEIGHT, true)
        disk.setRotation(Quaternion().fromAngleAxis(Math.PI / 2, Vector3.UNIT_X)) // stand the disk on the board
        disk.setDefaultColor(color)
        node.attachChild(disk)

        if (piece.king) {
            val crown = Cylinder("crown", 2, 24, PIECE_RADIUS * 0.6, PIECE_HEIGHT, true)
            crown.setRotation(Quaternion().fromAngleAxis(Math.PI / 2, Vector3.UNIT_X))
            crown.setTranslation(0.0, PIECE_HEIGHT, 0.0)
            crown.setDefaultColor(CROWN_COLOR)
            node.attachChild(crown)
        }

        // Pieces are presentation only - never picked (clicks resolve through the tile beneath).
        node.sceneHints.setPickingHint(PickingHint.Pickable, false)

        // Game metadata on the node, per the plan (Savable, additive): colour, king, square index.
        node.setProperty(PROP_COLOR, piece.color.name)
        node.setProperty(PROP_KING, piece.king)
        node.setProperty(PROP_SQUARE, square.row * Board.SIZE + square.col)
        return node
    }

    companion object {
        // Geometry: 1-unit squares, board centred on the origin, board top at y = 0.
        private const val OFFSET = (Board.SIZE - 1) / 2.0 // 3.5
        private const val TILE_TOP = 0.0
        private const val TILE_HALF_HEIGHT = 0.1
        private const val PIECE_RADIUS = 0.36
        private const val PIECE_HEIGHT = 0.18
        private const val PIECE_Y = TILE_TOP + PIECE_HEIGHT / 2 + 0.02

        // Metadata property keys on piece nodes.
        const val PROP_COLOR = "checkers.color"
        const val PROP_KING = "checkers.king"
        const val PROP_SQUARE = "checkers.square"

        private val DARK_TILE = ColorRGBA(0.30f, 0.20f, 0.14f, 1f)
        private val LIGHT_TILE = ColorRGBA(0.82f, 0.76f, 0.62f, 1f)
        private val RED_PIECE = ColorRGBA(0.72f, 0.14f, 0.11f, 1f)
        private val BLACK_PIECE = ColorRGBA(0.10f, 0.10f, 0.12f, 1f)
        private val CROWN_COLOR = ColorRGBA(0.90f, 0.78f, 0.25f, 1f)
        private val HIGHLIGHT_COLOR = ColorRGBA(0.30f, 0.85f, 0.35f, 1f)
    }
}
