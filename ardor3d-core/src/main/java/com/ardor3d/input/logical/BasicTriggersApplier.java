/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input.logical;

import java.util.Set;

import com.ardor3d.framework.Canvas;

public class BasicTriggersApplier implements LogicalTriggersApplier {

    public void checkAndPerformTriggers(final Set<InputTrigger> triggers, final Canvas source,
            final TwoInputStates states, final double tpf) {
        for (final InputTrigger trigger : triggers) {
            trigger.performIfValid(source, states, tpf);
        }
    }

}
