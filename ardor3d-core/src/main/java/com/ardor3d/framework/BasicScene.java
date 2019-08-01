/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.framework;

import com.ardor3d.intersection.PickResults;
import com.ardor3d.intersection.PickingUtil;
import com.ardor3d.intersection.PrimitivePickResults;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.util.ContextGarbageCollector;
import com.ardor3d.util.GameTaskQueue;
import com.ardor3d.util.GameTaskQueueManager;

public class BasicScene implements Scene {
    private final Node _root;

    public BasicScene() {
        _root = new Node("root");
    }

    public PickResults doPick(final com.ardor3d.math.Ray3 pickRay) {
        final PrimitivePickResults pickResults = new PrimitivePickResults();
        pickResults.setCheckDistance(true);
        PickingUtil.findPick(_root, pickRay, pickResults);
        processPicks(pickResults);
        return pickResults;
    }

    protected void processPicks(final PrimitivePickResults pickResults) {}

    public Node getRoot() {
        return _root;
    }

    @Override
    public boolean render(final Renderer renderer) {
        // Execute renderQueue item
        GameTaskQueueManager.getManager(ContextManager.getCurrentContext()).getQueue(GameTaskQueue.RENDER)
                .execute(renderer);

        // Clean up card garbage such as textures, vbos, etc.
        ContextGarbageCollector.doRuntimeCleanup(renderer);

        _root.onDraw(renderer);
        return true;
    }
}
