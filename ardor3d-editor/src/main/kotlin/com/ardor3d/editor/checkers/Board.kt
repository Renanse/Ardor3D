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

/**
 * An immutable checkers position and the rules over it: American/English draughts on an 8x8 board,
 * 12 pieces a side, men move and capture one diagonal forward, kings any diagonal, captures are
 * mandatory and a jump chain must be played to its end. Pure - no Ardor3D dependency, fully
 * unit-testable; [apply] returns a new [Board] rather than mutating.
 *
 * A side that has no legal move on its turn has lost (out of pieces, or blocked). A game with no
 * capture and no man move for [DRAW_PLIES] plies is a draw.
 */
class Board private constructor(
    private val pieces: Map<Square, Piece>,
    /** Whose turn it is. */
    val toMove: PieceColor,
    /** Plies since the last capture or man move - the draw clock. */
    val pliesSinceProgress: Int
) {
    /** The piece on [square], or null if empty. */
    fun pieceAt(square: Square): Piece? = pieces[square]

    /** Every occupied square, as an immutable view. */
    val occupied: Map<Square, Piece> get() = pieces

    /** Count of [color]'s pieces still on the board. */
    fun count(color: PieceColor): Int = pieces.values.count { it.color == color }

    /**
     * Every legal move for [toMove]. If any capture exists, only captures are returned (mandatory
     * capture); each jump entry is a maximal chain (you must keep jumping while you can).
     */
    fun legalMoves(): List<Move> {
        val mine = pieces.entries.filter { it.value.color == toMove }
        val jumps = mine.flatMap { jumpMoves(it.key, it.value) }
        if (jumps.isNotEmpty()) return jumps
        return mine.flatMap { simpleMoves(it.key, it.value) }
    }

    /** Legal moves for the piece on [square] (empty if it is not [toMove]'s, or a capture exists elsewhere). */
    fun legalMovesFrom(square: Square): List<Move> = legalMoves().filter { it.from == square }

    /**
     * Applies [move], returning the resulting position with the turn passed to the opponent. Handles
     * capture removal, crowning a man that reaches the far row, and the draw clock.
     */
    fun apply(move: Move): Board {
        val next = pieces.toMutableMap()
        val piece = next.remove(move.from) ?: error("no piece to move at ${move.from}")
        for (captured in move.captured) next.remove(captured)
        val crowned = !piece.king && isKingRow(piece.color, move.to)
        next[move.to] = if (crowned) piece.copy(king = true) else piece
        // A capture or any man move is progress; only quiet king shuffling advances the draw clock.
        val progress = move.isCapture || !piece.king
        return Board(next, toMove.opponent, if (progress) 0 else pliesSinceProgress + 1)
    }

    /** The winner, or null if the game is not decided by a side being unable to move. */
    fun winner(): PieceColor? = if (legalMoves().isEmpty()) toMove.opponent else null

    /** Whether the position is a draw by the no-progress rule (and not already a win). */
    fun isDraw(): Boolean = winner() == null && pliesSinceProgress >= DRAW_PLIES

    /** Whether the game is over (a side has won, or it is a draw). */
    fun isGameOver(): Boolean = winner() != null || isDraw()

    // --- move generation ---------------------------------------------------------------------

    private fun simpleMoves(from: Square, piece: Piece): List<Move> =
        stepDirections(piece).mapNotNull { (dRow, dCol) ->
            val to = from.offset(dRow, dCol)
            // Diagonal neighbours of a playable square are always playable, so on-board + empty suffices.
            if (to.onBoard && pieceAt(to) == null) Move(from, listOf(to), emptyList()) else null
        }

    private fun jumpMoves(from: Square, piece: Piece): List<Move> {
        val out = mutableListOf<Move>()
        extendJumps(origin = from, piece = piece, at = from, path = emptyList(), captured = emptyList(), out = out)
        return out
    }

    /**
     * Depth-first over jump continuations, emitting only maximal chains (a chain that could jump
     * again is never emitted short). A man that lands on its king row is crowned and stops there.
     */
    private fun extendJumps(
        origin: Square,
        piece: Piece,
        at: Square,
        path: List<Square>,
        captured: List<Square>,
        out: MutableList<Move>
    ) {
        for ((dRow, dCol) in stepDirections(piece)) {
            val over = at.offset(dRow, dCol)
            val land = at.offset(2 * dRow, 2 * dCol)
            if (!land.onBoard) continue
            val jumped = pieceAt(over) ?: continue
            if (jumped.color != piece.color.opponent) continue
            if (over in captured) continue // never jump the same piece twice in one chain
            // Landing must be empty. Nothing has moved on this board yet, so the only occupied square
            // that is legitimately free to land on is the piece's own (vacated) origin.
            if (pieceAt(land) != null && land != origin) continue

            val nextPath = path + land
            val nextCaptured = captured + over
            val crowns = !piece.king && isKingRow(piece.color, land)
            if (crowns) {
                // Reaching the king row ends the move (no continuing as a fresh king this turn).
                out.add(Move(origin, nextPath, nextCaptured))
            } else {
                val sizeBefore = out.size
                extendJumps(origin, piece, land, nextPath, nextCaptured, out)
                // If no longer chain grew from here, this square is the end of a maximal chain.
                if (out.size == sizeBefore) {
                    out.add(Move(origin, nextPath, nextCaptured))
                }
            }
        }
    }

    private fun stepDirections(piece: Piece): List<Pair<Int, Int>> {
        if (piece.king) return KING_DIRECTIONS
        val forward = if (piece.color == PieceColor.RED) 1 else -1
        return listOf(forward to 1, forward to -1)
    }

    private fun isKingRow(color: PieceColor, square: Square): Boolean =
        (color == PieceColor.RED && square.row == SIZE - 1) || (color == PieceColor.BLACK && square.row == 0)

    companion object {
        /** Board edge length in squares. */
        const val SIZE = 8

        /** Plies without a capture or man move after which the game is a draw (40 moves a side). */
        const val DRAW_PLIES = 80

        private val KING_DIRECTIONS = listOf(1 to 1, 1 to -1, -1 to 1, -1 to -1)

        /** The standard opening position: 12 RED on the low three rows, 12 BLACK on the high three, RED to move. */
        fun initial(): Board {
            val pieces = mutableMapOf<Square, Piece>()
            for (row in 0 until SIZE) {
                for (col in 0 until SIZE) {
                    val square = Square(row, col)
                    if (!square.isPlayable) continue
                    when (row) {
                        0, 1, 2 -> pieces[square] = Piece(PieceColor.RED)
                        5, 6, 7 -> pieces[square] = Piece(PieceColor.BLACK)
                    }
                }
            }
            return Board(pieces, PieceColor.RED, 0)
        }

        /** Builds a board from an explicit placement - used by tests to set up specific positions. */
        fun of(toMove: PieceColor, pieces: Map<Square, Piece>, pliesSinceProgress: Int = 0): Board =
            Board(pieces.toMap(), toMove, pliesSinceProgress)
    }
}
