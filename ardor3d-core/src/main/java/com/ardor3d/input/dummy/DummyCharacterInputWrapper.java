/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.input.dummy;

import com.ardor3d.input.character.CharacterInputEvent;
import com.ardor3d.input.character.CharacterInputWrapper;
import com.google.common.collect.PeekingIterator;

public class DummyCharacterInputWrapper implements CharacterInputWrapper {

    public static final DummyCharacterInputWrapper INSTANCE = new DummyCharacterInputWrapper();

    final PeekingIterator<CharacterInputEvent> empty = new PeekingIterator<CharacterInputEvent>() {
        public boolean hasNext() {
            return false;
        }

        public void remove() {
        }

        public CharacterInputEvent peek() {
            return null;
        }

        public CharacterInputEvent next() {
            return null;
        }
    };

    public PeekingIterator<CharacterInputEvent> getCharacterEvents() {
        return empty;
    }

    public void init() {
        ; // ignore, does nothing
    }

}
