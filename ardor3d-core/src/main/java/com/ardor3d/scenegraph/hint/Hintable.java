/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.scenegraph.hint;

/**
 * Interface used for describing objects that deal with the SceneHints class. This interface allows non-specific access
 * to object hierarchy.
 */
public interface Hintable {

    /**
     * @return a hierarchical parent to be used for determining inheritance of certain SceneHint fields.
     */
    Hintable getParentHintable();

    /**
     * @return the SceneHints object used by this Hintable.
     */
    SceneHints getSceneHints();
}
