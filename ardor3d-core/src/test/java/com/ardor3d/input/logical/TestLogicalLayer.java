/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.input.logical;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.EnumSet;
import java.util.LinkedList;
import java.util.function.Predicate;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ardor3d.framework.Canvas;
import com.ardor3d.input.InputState;
import com.ardor3d.input.PhysicalLayer;
import com.ardor3d.input.character.CharacterInputState;
import com.ardor3d.input.controller.ControllerState;
import com.ardor3d.input.gesture.GestureState;
import com.ardor3d.input.keyboard.Key;
import com.ardor3d.input.keyboard.KeyEvent;
import com.ardor3d.input.keyboard.KeyboardState;
import com.ardor3d.input.mouse.ButtonState;
import com.ardor3d.input.mouse.MouseButton;
import com.ardor3d.input.mouse.MouseState;

/**
 * Tests for the LogicalLayer
 */
public class TestLogicalLayer {
  LogicalLayer ll;
  PhysicalLayer pl;

  TriggerAction ta1, ta2;

  Predicate<TwoInputStates> p1;
  Predicate<TwoInputStates> p2;

  KeyboardState ks;
  MouseState ms;
  ControllerState cs;
  GestureState gs;
  CharacterInputState cis;

  Canvas canvas;

  Object[] mocks;

  @SuppressWarnings({"unchecked"})
  @Before
  public void setup() throws Exception {
    pl = createMock("Physicallayer", PhysicalLayer.class);

    ta1 = createMock("TA1", TriggerAction.class);
    ta2 = createMock("TA2", TriggerAction.class);

    p1 = createMock("P1", Predicate.class);
    p2 = createMock("P2", Predicate.class);

    canvas = createMock("canvas", Canvas.class);

    ll = new LogicalLayer();

    ll.registerInput(canvas, pl);

    ks = new KeyboardState(EnumSet.noneOf(Key.class), KeyEvent.NOTHING);
    ms = new MouseState(0, 0, 0, 0, 0, MouseButton.makeMap(ButtonState.UP, ButtonState.UP, ButtonState.UP), null);
    cs = new ControllerState();
    gs = new GestureState();
    cis = new CharacterInputState();

    mocks = new Object[] {pl, ta1, ta2, p1, p2, canvas};
  }

  @After
  public void verifyMocks() throws Exception {
    verify(mocks);
  }

  @Test
  public void testTriggers1() throws Exception {
    final InputState state1 = new InputState(ks, ms, cs, gs, cis);
    final InputState state2 = new InputState(ks, ms, cs, gs, cis);

    final double tpf = 14;

    final LinkedList<InputState> states1 = new LinkedList<>();
    final LinkedList<InputState> states2 = new LinkedList<>();

    states1.add(state1);
    states2.add(state2);

    pl.readState();
    pl.readState();
    expect(pl.drainAvailableStates()).andReturn(states1);
    expect(pl.drainAvailableStates()).andReturn(states2);
    expect(p1.test(isA(TwoInputStates.class))).andReturn(false);
    expect(p1.test(isA(TwoInputStates.class))).andReturn(true);
    ta1.perform(canvas, new TwoInputStates(state1, state2), tpf);

    replay(mocks);

    ll.registerTrigger(new InputTrigger(p1, ta1));

    ll.checkTriggers(tpf);
    ll.checkTriggers(tpf);
  }

  @Test
  public void testTriggers2() throws Exception {
    final InputState state1 = new InputState(ks, ms, cs, gs, cis);
    final InputState state2 = new InputState(ks, ms, cs, gs, cis);

    final double tpf = 14;

    final LinkedList<InputState> states1 = new LinkedList<>();
    final LinkedList<InputState> states2 = new LinkedList<>();

    states1.add(state1);
    states2.add(state2);

    pl.readState();
    pl.readState();
    expect(pl.drainAvailableStates()).andReturn(states1);
    expect(pl.drainAvailableStates()).andReturn(states2);
    expect(p1.test(isA(TwoInputStates.class))).andReturn(false);
    expect(p1.test(isA(TwoInputStates.class))).andReturn(true);
    expect(p2.test(isA(TwoInputStates.class))).andReturn(true);
    expect(p2.test(isA(TwoInputStates.class))).andReturn(true);
    ta1.perform(canvas, new TwoInputStates(state1, state2), tpf);
    ta2.perform(canvas, new TwoInputStates(InputState.EMPTY, state1), tpf);
    ta2.perform(canvas, new TwoInputStates(state1, state2), tpf);

    replay(mocks);

    ll.registerTrigger(new InputTrigger(p1, ta1));
    ll.registerTrigger(new InputTrigger(p2, ta2));

    ll.checkTriggers(tpf);
    ll.checkTriggers(tpf);
  }

  @Test
  public void testTriggers3() throws Exception {
    final InputState state1 = new InputState(ks, ms, cs, gs, cis);
    final InputState state2 = new InputState(ks, ms, cs, gs, cis);

    final double tpf = 14;

    final LinkedList<InputState> states1 = new LinkedList<>();
    final LinkedList<InputState> states2 = new LinkedList<>();

    states1.add(state1);
    states2.add(state2);

    pl.readState();
    pl.readState();
    expect(pl.drainAvailableStates()).andReturn(states1);
    expect(pl.drainAvailableStates()).andReturn(states2);
    expect(p1.test(isA(TwoInputStates.class))).andReturn(false);
    expect(p1.test(isA(TwoInputStates.class))).andReturn(true);
    expect(p2.test(isA(TwoInputStates.class))).andReturn(true);
    ta1.perform(canvas, new TwoInputStates(state1, state2), tpf);
    ta2.perform(canvas, new TwoInputStates(InputState.EMPTY, state1), tpf);

    replay(mocks);

    final InputTrigger trigger2 = new InputTrigger(p2, ta2);

    ll.registerTrigger(new InputTrigger(p1, ta1));
    ll.registerTrigger(trigger2);

    ll.checkTriggers(tpf);
    ll.deregisterTrigger(trigger2);
    ll.checkTriggers(tpf);
  }

  @Test
  public void testTriggers4() throws Exception {
    final InputState state1 = new InputState(ks, ms, cs, gs, cis);

    final double tpf = 14;

    final LinkedList<InputState> states1 = new LinkedList<>();
    final LinkedList<InputState> states2 = new LinkedList<>();

    states1.add(state1);

    pl.readState();
    pl.readState();
    expect(pl.drainAvailableStates()).andReturn(states1);
    expect(pl.drainAvailableStates()).andReturn(states2);
    expect(p1.test(isA(TwoInputStates.class))).andReturn(false);
    expect(p1.test(isA(TwoInputStates.class))).andReturn(true);
    expect(p2.test(isA(TwoInputStates.class))).andReturn(true);
    expect(p2.test(isA(TwoInputStates.class))).andReturn(true);
    ta1.perform(canvas, new TwoInputStates(state1, state1), tpf);
    ta2.perform(canvas, new TwoInputStates(InputState.EMPTY, state1), tpf);
    ta2.perform(canvas, new TwoInputStates(state1, state1), tpf);

    replay(mocks);

    final InputTrigger trigger2 = new InputTrigger(p2, ta2);

    ll.registerTrigger(new InputTrigger(p1, ta1));
    ll.registerTrigger(trigger2);

    ll.checkTriggers(tpf);
    ll.checkTriggers(tpf);
  }

  @Test
  public void testLostFocus() throws Exception {
    final InputState state1 = new InputState(ks, ms, cs, gs, cis);
    final InputState state2 = new InputState(ks, ms, cs, gs, cis);

    final double tpf = 14;

    final LinkedList<InputState> states1 = new LinkedList<>();
    final LinkedList<InputState> states2 = new LinkedList<>();

    states1.add(state1);
    states2.add(InputState.LOST_FOCUS);
    states2.add(state2);

    pl.readState();
    pl.readState();
    expect(pl.drainAvailableStates()).andReturn(states1);
    expect(pl.drainAvailableStates()).andReturn(states2);
    expect(p1.test(isA(TwoInputStates.class))).andReturn(true);
    expect(p1.test(isA(TwoInputStates.class))).andReturn(false);
    ta1.perform(canvas, new TwoInputStates(InputState.EMPTY, state1), tpf);

    replay(mocks);

    ll.registerTrigger(new InputTrigger(p1, ta1));

    ll.checkTriggers(tpf);
    ll.checkTriggers(tpf);

  }
}
