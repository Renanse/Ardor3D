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
 * The pure value types of the checkers model. No Ardor3D dependency - this is the plain-data game
 * state the plan's state/view split calls for; the scene graph is only its view.
 */

/** The two sides. RED starts on the low rows and advances up-board; BLACK starts high and advances down. */
enum class PieceColor {
    RED,
    BLACK;

    val opponent: PieceColor get() = if (this == RED) BLACK else RED
}

/** A checker: its owner and whether it has been crowned a king (moves and captures in all four diagonals). */
data class Piece(val color: PieceColor, val king: Boolean = false)

/**
 * A board square by (row, col), row 0 at RED's home edge. Only "playable" (dark) squares - where
 * row + col is even - ever hold a piece; all diagonal neighbours of a playable square are playable.
 */
data class Square(val row: Int, val col: Int) {
    val onBoard: Boolean get() = row in 0 until Board.SIZE && col in 0 until Board.SIZE
    val isPlayable: Boolean get() = onBoard && (row + col) % 2 == 0

    /** The square [dRow], [dCol] away (used for stepping/jumping along diagonals). */
    fun offset(dRow: Int, dCol: Int): Square = Square(row + dRow, col + dCol)
}

/**
 * A complete legal move by one piece: a simple one-square slide, or a jump chain. [path] is the
 * landing square(s) in order (the final one is [to]); [captured] are the squares of the pieces
 * removed by a jump chain, empty for a slide.
 */
data class Move(val from: Square, val path: List<Square>, val captured: List<Square>) {
    init {
        require(path.isNotEmpty()) { "a move must have at least one landing square" }
    }

    /** The square the piece ends on. */
    val to: Square get() = path.last()

    /** Whether this move captures at least one piece. */
    val isCapture: Boolean get() = captured.isNotEmpty()
}
