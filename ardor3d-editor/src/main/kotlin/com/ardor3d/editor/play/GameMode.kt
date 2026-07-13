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

/**
 * A game's behavior, driven by the editor's play mode. This is the editor/sample-side sandbox for
 * the behavior-model questions a future Ardor3D 2.0 must answer (an ordered service layer, explicit
 * update phases, a jME-`AppState`-style split): patterns proven here are *candidate designs*, not
 * promoted into core by this work.
 *
 * Lifecycle, all on the render/update thread:
 * - [onStart] once when play begins, with the play-session [GameContext]. Set up the model and its
 *   scene view here.
 * - [update] every frame while playing, with the elapsed time and this frame's [GameInput]. Advance
 *   the model, then sync the scene view from it.
 * - [onStop] once when play ends, *before* the pre-play scene is restored (so references are still
 *   valid). Release anything held.
 *
 * Everything a mode does to the scene during play is discarded on stop (see the edit/play boundary),
 * so a mode never has to undo its own mutations.
 */
interface GameMode {
    /** Called once when play begins. */
    fun onStart(context: GameContext)

    /** Called every frame while playing. */
    fun update(timePerFrame: Double, input: GameInput)

    /** Called once when play ends, before the pre-play scene is restored. */
    fun onStop()
}
