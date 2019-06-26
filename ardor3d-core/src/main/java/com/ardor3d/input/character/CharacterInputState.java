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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CharacterInputState {

    public static final CharacterInputState NOTHING = new CharacterInputState();

    protected final List<CharacterInputEvent> _eventsSinceLastState = new ArrayList<>();

    public CharacterInputState() {
    }

    public void addEvent(final CharacterInputEvent event) {
        _eventsSinceLastState.add(event);
    }

    public List<CharacterInputEvent> getEvents() {
        return Collections.unmodifiableList(_eventsSinceLastState);
    }

    public void clearEvents() {
        _eventsSinceLastState.clear();
    }
}
