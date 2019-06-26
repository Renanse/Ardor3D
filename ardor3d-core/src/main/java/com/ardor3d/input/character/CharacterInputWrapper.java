/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.input.character;

import com.google.common.collect.PeekingIterator;

/**
 * Defines the API for character input wrappers - these classes supply characters input into the system in some manner,
 * be it from a keyboard or some other device.
 */
public interface CharacterInputWrapper {
    /**
     * Allows the keyboard wrapper implementation to initialise itself.
     */
    public void init();

    /**
     * Returns a peeking iterator that allows the client to loop through all keyboard events that have not yet been
     * handled.
     *
     * @return an iterator that allows the client to check which events have still not been handled
     */
    public PeekingIterator<CharacterInputEvent> getCharacterEvents();
}
