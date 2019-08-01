/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.math;

import org.junit.Test;

public class TestObjectPool {

    @Test(expected = RuntimeException.class)
    public void testPoolReleaseNullError() {
        Vector2.releaseTempInstance(null);
    }

    @Test(expected = RuntimeException.class)
    public void testPoolBadClass() {
        ObjectPool.create(Poolable.class, 10).fetch();
    }

    @Test
    public void testPoolSize() {
        final ObjectPool<Vector2> pool = ObjectPool.create(Vector2.class, 10);
        for (int i = 0; i < 11; i++) {
            pool.release(new Vector2());
        }
    }
}
