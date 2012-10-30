/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input.lwjgl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.lwjgl.input.Controller;
import org.lwjgl.input.Controllers;

import com.ardor3d.input.ControllerEvent;
import com.ardor3d.input.ControllerInfo;
import com.ardor3d.input.ControllerState;
import com.ardor3d.input.ControllerWrapper;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Lists;
import com.google.common.collect.PeekingIterator;

public class LwjglControllerWrapper implements ControllerWrapper {

    protected static boolean _inited = false;
    protected final List<ControllerEvent> _events = Collections.synchronizedList(new ArrayList<ControllerEvent>());
    protected LwjglControllerEventIterator _eventsIt = new LwjglControllerEventIterator();
    protected final List<ControllerInfo> _controllers = Lists.newArrayList();

    private class LwjglControllerEventIterator extends AbstractIterator<ControllerEvent> implements
            PeekingIterator<ControllerEvent> {

        @Override
        protected ControllerEvent computeNext() {
            if (_events.size() > 0) {
                return _events.remove(0);
            } else {
                return endOfData();
            }
        }
    }

    public PeekingIterator<ControllerEvent> getEvents() {
        init();
        if (!_eventsIt.hasNext()) {
            _eventsIt = new LwjglControllerEventIterator();
        }

        while (Controllers.next()) {
            final Controller source = Controllers.getEventSource();
            if (Controllers.isEventButton()) {
                _events.add(new ControllerEvent(Controllers.getEventNanoseconds(), source.getName(), source
                        .getButtonName(Controllers.getEventControlIndex()), source.isButtonPressed(Controllers
                        .getEventControlIndex()) ? 1f : 0f));
            } else if (Controllers.isEventAxis()) {
                _events.add(new ControllerEvent(Controllers.getEventNanoseconds(), source.getName(), source
                        .getAxisName(Controllers.getEventControlIndex()), source.getAxisValue(Controllers
                        .getEventControlIndex())));
            } else if (Controllers.isEventPovX()) {
                _events.add(new ControllerEvent(Controllers.getEventNanoseconds(), source.getName(), "Pov X", source
                        .getPovX()));
            } else if (Controllers.isEventPovY()) {
                _events.add(new ControllerEvent(Controllers.getEventNanoseconds(), source.getName(), "Pov Y", source
                        .getPovY()));
            }
        }

        return _eventsIt;
    }

    public synchronized void init() {
        if (_inited) {
            return;
        }
        try {
            Controllers.create();
            for (int i = 0, max = Controllers.getControllerCount(); i < max; i++) {
                final Controller controller = Controllers.getController(i);
                _controllers.add(getControllerInfo(controller));
                for (int j = 0; j < controller.getAxisCount(); j++) {
                    ControllerState.NOTHING.set(controller.getName(), controller.getAxisName(j), 0);
                }
                for (int j = 0; j < controller.getButtonCount(); j++) {
                    ControllerState.NOTHING.set(controller.getName(), controller.getButtonName(j), 0);
                }
            }
        } catch (final Exception e) {
            e.printStackTrace();
        } finally {
            _inited = true;
        }
    }

    protected ControllerInfo getControllerInfo(final Controller controller) {
        final List<String> axisNames = Lists.newArrayList();
        final List<String> buttonNames = Lists.newArrayList();

        for (int i = 0; i < controller.getAxisCount(); i++) {
            axisNames.add(controller.getAxisName(i));
        }
        for (int i = 0; i < controller.getButtonCount(); i++) {
            buttonNames.add(controller.getButtonName(i));
        }

        return new ControllerInfo(controller.getName(), axisNames, buttonNames);
    }

    @Override
    public int getControllerCount() {
        init();
        return Controllers.getControllerCount();
    }

    @Override
    public ControllerInfo getControllerInfo(final int controllerIndex) {
        init();
        return _controllers.get(controllerIndex);
    }
}