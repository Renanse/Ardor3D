/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.compose

import org.junit.Assert.assertTrue
import java.lang.management.ManagementFactory

/**
 * The allocation gates are only meaningful if thread-allocation accounting is actually on:
 * when it is unsupported or disabled, getThreadAllocatedBytes returns -1 for every call and
 * a before/after delta is 0 - the gate would pass vacuously. Enable it when the JVM merely
 * left it off; fail loudly only when the platform cannot support it at all.
 */
internal fun allocationAccounting(): com.sun.management.ThreadMXBean {
    val threads = ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
    assertTrue(
        "thread allocation accounting must be supported for the allocation gates",
        threads.isThreadAllocatedMemorySupported
    )
    if (!threads.isThreadAllocatedMemoryEnabled) {
        threads.isThreadAllocatedMemoryEnabled = true
    }
    return threads
}
