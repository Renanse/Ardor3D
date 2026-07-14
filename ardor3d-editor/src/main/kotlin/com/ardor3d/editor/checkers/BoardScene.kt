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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import com.ardor3d.bounding.BoundingBox
import com.ardor3d.compose.SceneNode
import com.ardor3d.compose.SceneSpatial
import com.ardor3d.compose.SpatialApplier
import com.ardor3d.light.PointLight
import com.ardor3d.math.ColorRGBA
import com.ardor3d.math.Quaternion
import com.ardor3d.math.Vector3
import com.ardor3d.scenegraph.Node
import com.ardor3d.scenegraph.Spatial
import com.ardor3d.scenegraph.hint.PickingHint
import com.ardor3d.scenegraph.shape.Box
import com.ardor3d.scenegraph.shape.Cylinder

/**
 * The scene-graph *view* of a checkers [Board], declared as a composition: [BoardScene] is a pure
 * function from the game state to the scene, and recomposition *is* the sync - the structural
 * diffing a hand-written rebuild()/setHighlights() view has to do itself is the Compose runtime's
 * job here. It contains no game rules, only presentation and the click-target mapping the game
 * needs.
 *
 * Attribute ownership: composition owns structure, and owns an attribute only through a
 * `set(value)` keyed on the model value it derives from - such a setter re-runs only when that
 * value changes. That is what legitimizes the move slide's *imperative* translation writes: while
 * a piece's square is unchanged, composition never touches its translation, and when the square
 * does change, the snap it writes is the slide's own endpoint.
 *
 * Picking model (unchanged from the imperative view): only playable board tiles are pickable and
 * carry their square (see [squareForTile]); pieces and highlights are non-pickable, so a click
 * always resolves to the tile beneath the pointer and the model decides what is there.
 *
 * Materialization happens at creation: each mesh factory hands the fresh spatial to [materialize],
 * so geometry composed later (a crown, a highlight marker) gets its material the moment it exists -
 * no caller-side re-walks.
 */
@Composable
fun BoardScene(
    board: () -> Board,
    highlights: () -> Map<Square, HighlightKind>,
    materialize: (Spatial) -> Unit = {},
    onSync: () -> Unit = {}
) {
    // State arrives as functions and is read inside each subtree's own scope, so a board change
    // recomposes only the pieces and a highlight change only the markers; tiles and light read
    // nothing and are composed exactly once.
    BoardLight()
    BoardTiles(materialize)
    BoardPieces(board, materialize, onSync)
    BoardHighlights(highlights, materialize, onSync)
}

/** Metadata property keys on composed spatials (same key strings the imperative view used). */
const val PROP_COLOR = "checkers.color"
const val PROP_KING = "checkers.king"
const val PROP_SQUARE = "checkers.square"
const val PROP_TILE = "checkers.tile"

/** World-space centre of the top face of [square] (pieces sit here). */
fun worldPositionOf(square: Square): Vector3 =
    Vector3(square.col - OFFSET, PIECE_Y, square.row - OFFSET)

/** The square of a picked tile spatial, or null if it was not a pickable board tile. */
fun squareForTile(spatial: Spatial): Square? {
    val index: Int = spatial.getProperty(PROP_TILE, -1)
    if (index < 0) return null
    return Square(index / Board.SIZE, index % Board.SIZE)
}

/** The piece node currently shown on [square] under [boardRoot] (as last composed), or null. */
fun pieceNodeAt(boardRoot: Node, square: Square): Node? {
    val pieces = boardRoot.getChild("Pieces") as? Node ?: return null
    val index = square.row * Board.SIZE + square.col
    return pieces.children.firstOrNull { it.getProperty(PROP_SQUARE, -1) == index } as? Node
}

// --- the subtrees --------------------------------------------------------------------------

@Composable
private fun BoardLight() {
    // Own light so the board reads no matter how the host scene is lit.
    SceneSpatial(factory = {
        PointLight().apply {
            name = "Checkers Light"
            setTranslation(2.0, 9.0, 3.0)
            intensity = 1.1f
            isEnabled = true
        }
    })
}

@Composable
private fun BoardTiles(materialize: (Spatial) -> Unit) {
    SceneNode("Tiles") {
        for (row in 0 until Board.SIZE) {
            for (col in 0 until Board.SIZE) {
                SceneSpatial(factory = { buildTile(Square(row, col)).also(materialize) })
            }
        }
    }
}

@Composable
private fun BoardPieces(board: () -> Board, materialize: (Spatial) -> Unit, onSync: () -> Unit) {
    SceneNode("Pieces") {
        val current = board()
        // Keyed by the model's stable piece identity and kept in identity order: a piece that moves
        // keeps its node *and* its place among siblings, so a plain move is zero structural traffic -
        // just the translation snap in BoardPiece's update.
        for (entry in current.occupied.entries.sortedBy { current.idAt(it.key) }) {
            val id = current.idAt(entry.key)
            key(id) {
                BoardPiece(id, entry.value, entry.key, materialize)
            }
        }
        SideEffect { onSync() }
    }
}

@Composable
private fun BoardPiece(id: Int, piece: Piece, square: Square, materialize: (Spatial) -> Unit) {
    ComposeNode<Node, SpatialApplier>(
        factory = {
            Node("piece_$id").apply {
                // Pieces are presentation only - never picked (clicks resolve through the tile beneath).
                sceneHints.setPickingHint(PickingHint.Pickable, false)
            }
        },
        update = {
            // Placement is owned through the square (see the file doc's ownership policy).
            set(square) {
                setTranslation(worldPositionOf(it))
                setProperty(PROP_SQUARE, it.row * Board.SIZE + it.col)
            }
            set(piece.color) { setProperty(PROP_COLOR, it.name) }
            set(piece.king) { setProperty(PROP_KING, it) }
        }
    ) {
        SceneSpatial(factory = { buildDisk(piece.color).also(materialize) })
        if (piece.king) {
            SceneSpatial(factory = { buildCrown().also(materialize) })
        }
    }
}

@Composable
private fun BoardHighlights(
    highlights: () -> Map<Square, HighlightKind>,
    materialize: (Spatial) -> Unit,
    onSync: () -> Unit
) {
    SceneNode("Highlights") {
        for ((square, kind) in highlights()) {
            key(square) {
                SceneSpatial(
                    factory = { buildHighlightMarker().also(materialize) },
                    update = {
                        set(square) { setTranslation(it.col - OFFSET, TILE_TOP + 0.02, it.row - OFFSET) }
                        set(kind) { defaultColor = colorFor(it) }
                    }
                )
            }
        }
        SideEffect { onSync() }
    }
}

// --- construction --------------------------------------------------------------------------

private fun buildTile(square: Square): Box {
    val tile = Box("tile_${square.row}_${square.col}", Vector3.ZERO, 0.5, TILE_HALF_HEIGHT, 0.5)
    tile.setTranslation(square.col - OFFSET, TILE_TOP - TILE_HALF_HEIGHT, square.row - OFFSET)
    // A finite model bound is required for picking: the default bound is an infinite
    // BoundingSphere, whose ray broad-phase fails, so findPick would skip the tile.
    tile.modelBound = BoundingBox()
    tile.updateModelBound()
    if (square.isPlayable) {
        tile.defaultColor = DARK_TILE
        tile.setProperty(PROP_TILE, square.row * Board.SIZE + square.col) // a click target
    } else {
        tile.defaultColor = LIGHT_TILE
        tile.sceneHints.setPickingHint(PickingHint.Pickable, false)
    }
    return tile
}

private fun buildDisk(color: PieceColor): Cylinder {
    val disk = Cylinder("disk", 2, 24, PIECE_RADIUS, PIECE_HEIGHT, true)
    disk.setRotation(Quaternion().fromAngleAxis(Math.PI / 2, Vector3.UNIT_X)) // stand the disk on the board
    disk.defaultColor = if (color == PieceColor.RED) RED_PIECE else BLACK_PIECE
    return disk
}

private fun buildCrown(): Cylinder {
    val crown = Cylinder("crown", 2, 24, PIECE_RADIUS * 0.6, PIECE_HEIGHT, true)
    crown.setRotation(Quaternion().fromAngleAxis(Math.PI / 2, Vector3.UNIT_X))
    crown.setTranslation(0.0, PIECE_HEIGHT, 0.0)
    crown.defaultColor = CROWN_COLOR
    return crown
}

private fun buildHighlightMarker(): Box {
    val marker = Box("highlight", Vector3.ZERO, 0.46, 0.03, 0.46)
    marker.sceneHints.setPickingHint(PickingHint.Pickable, false)
    return marker
}

private fun colorFor(kind: HighlightKind): ColorRGBA = when (kind) {
    HighlightKind.SELECTED -> SELECTED_COLOR
    HighlightKind.HOVER_PIECE -> HOVER_PIECE_COLOR
    HighlightKind.MOVE -> MOVE_COLOR
    HighlightKind.MOVE_HOVER -> MOVE_HOVER_COLOR
    HighlightKind.PREVIEW -> PREVIEW_COLOR
}

// Geometry: 1-unit squares, board centred on the origin, board top at y = 0.
private const val OFFSET = (Board.SIZE - 1) / 2.0 // 3.5
private const val TILE_TOP = 0.0
private const val TILE_HALF_HEIGHT = 0.1
private const val PIECE_RADIUS = 0.36
private const val PIECE_HEIGHT = 0.18
private const val PIECE_Y = TILE_TOP + PIECE_HEIGHT / 2 + 0.02

private val DARK_TILE = ColorRGBA(0.30f, 0.20f, 0.14f, 1f)
private val LIGHT_TILE = ColorRGBA(0.82f, 0.76f, 0.62f, 1f)
private val RED_PIECE = ColorRGBA(0.72f, 0.14f, 0.11f, 1f)
private val BLACK_PIECE = ColorRGBA(0.10f, 0.10f, 0.12f, 1f)
private val CROWN_COLOR = ColorRGBA(0.90f, 0.78f, 0.25f, 1f)

// Highlight colours (see HighlightKind).
private val SELECTED_COLOR = ColorRGBA(1.0f, 0.85f, 0.20f, 1f)   // the committed piece
private val HOVER_PIECE_COLOR = ColorRGBA(0.95f, 0.92f, 0.55f, 1f) // a piece being hovered (preview)
private val MOVE_COLOR = ColorRGBA(0.20f, 0.80f, 0.32f, 1f)      // a legal destination
private val MOVE_HOVER_COLOR = ColorRGBA(0.35f, 0.95f, 0.95f, 1f) // the destination under the pointer
private val PREVIEW_COLOR = ColorRGBA(0.22f, 0.55f, 0.50f, 1f)   // previewed destinations (no selection)

/** How a highlighted square is shown - drives its colour. */
enum class HighlightKind {
    /** The piece the player has clicked to move. */
    SELECTED,

    /** A movable piece under the pointer, previewing its moves (no selection yet). */
    HOVER_PIECE,

    /** A legal destination of the selected piece. */
    MOVE,

    /** A legal destination currently under the pointer. */
    MOVE_HOVER,

    /** A destination previewed from hovering a piece (no selection). */
    PREVIEW
}
