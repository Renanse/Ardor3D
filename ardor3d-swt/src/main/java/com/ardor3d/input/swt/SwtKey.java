/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.input.swt;

import org.eclipse.swt.SWT;

import com.ardor3d.input.keyboard.Key;

/**
 * Enumeration for use in converting between SWT key codes and Ardor3d Keys.
 */
public enum SwtKey {
  SWT_ESCAPE(SWT.ESC, Key.ESCAPE), //
  SWT_1('1', Key.ONE), //
  SWT_2('2', Key.TWO), //
  SWT_3('3', Key.THREE), //
  SWT_4('4', Key.FOUR), //
  SWT_5('5', Key.FIVE), //
  SWT_6('6', Key.SIX), //
  SWT_7('7', Key.SEVEN), //
  SWT_8('8', Key.EIGHT), //
  SWT_9('9', Key.NINE), //
  SWT_0('0', Key.ZERO), //
  SWT_MINUS('-', Key.MINUS), //
  SWT_EQUALS('=', Key.EQUAL), //
  SWT_BACK(SWT.BS, Key.BACKSPACE), //
  SWT_TAB(SWT.TAB, Key.TAB), //
  SWT_Q('q', Key.Q), //
  SWT_W('w', Key.W), //
  SWT_E('e', Key.E), //
  SWT_R('r', Key.R), //
  SWT_T('t', Key.T), //
  SWT_Y('y', Key.Y), //
  SWT_U('u', Key.U), //
  SWT_I('i', Key.I), //
  SWT_O('o', Key.O), //
  SWT_P('p', Key.P), //
  SWT_LBRACKET('(', Key.LEFT_BRACKET), //
  SWT_RBRACKET(')', Key.RIGHT_BRACKET), //
  SWT_RETURN(SWT.LF, Key.ENTER), //
  SWT_LCONTROL(SWT.CTRL, Key.LEFT_CONTROL), //
  SWT_A('a', Key.A), //
  SWT_S('s', Key.S), //
  SWT_D('d', Key.D), //
  SWT_F('f', Key.F), //
  SWT_G('g', Key.G), //
  SWT_H('h', Key.H), //
  SWT_J('j', Key.J), //
  SWT_K('k', Key.K), //
  SWT_L('l', Key.L), //
  SWT_SEMICOLON(';', Key.SEMICOLON), //
  SWT_APOSTROPHE('\'', Key.APOSTROPHE), //
  SWT_GRAVE('`', Key.GRAVE_ACCENT), //
  SWT_LSHIFT(SWT.SHIFT, Key.LEFT_SHIFT), //
  SWT_BACKSLASH('\\', Key.BACKSLASH), //
  SWT_Z('z', Key.Z), //
  SWT_X('x', Key.X), //
  SWT_C('c', Key.C), //
  SWT_V('v', Key.V), //
  SWT_B('b', Key.B), //
  SWT_N('n', Key.N), //
  SWT_M('m', Key.M), //
  SWT_COMMA(',', Key.COMMA), //
  SWT_PERIOD('.', Key.PERIOD), //
  SWT_SLASH('/', Key.SLASH), //
  SWT_MULTIPLY('*', Key.MULTIPLY), //
  SWT_SPACE(' ', Key.SPACE), //
  SWT_CAPITAL(SWT.CAPS_LOCK, Key.CAPS_LOCK), //
  SWT_F1(SWT.F1, Key.F1), //
  SWT_F2(SWT.F2, Key.F2), //
  SWT_F3(SWT.F3, Key.F3), //
  SWT_F4(SWT.F4, Key.F4), //
  SWT_F5(SWT.F5, Key.F5), //
  SWT_F6(SWT.F6, Key.F6), //
  SWT_F7(SWT.F7, Key.F7), //
  SWT_F8(SWT.F8, Key.F8), //
  SWT_F9(SWT.F9, Key.F9), //
  SWT_F10(SWT.F10, Key.F10), //
  SWT_NUMLOCK(SWT.NUM_LOCK, Key.NUM_LOCK), //
  SWT_SCROLL(SWT.SCROLL_LOCK, Key.SCROLL_LOCK), //
  SWT_NUMPAD7(SWT.KEYPAD_7, Key.KP_7), //
  SWT_NUMPAD8(SWT.KEYPAD_8, Key.KP_8), //
  SWT_NUMPAD9(SWT.KEYPAD_9, Key.KP_9), //
  SWT_NUMPAD4(SWT.KEYPAD_4, Key.KP_4), //
  SWT_NUMPAD5(SWT.KEYPAD_5, Key.KP_5), //
  SWT_NUMPAD6(SWT.KEYPAD_6, Key.KP_6), //
  SWT_ADD('+', Key.KP_ADD), //
  SWT_NUMPAD1(SWT.KEYPAD_1, Key.KP_1), //
  SWT_NUMPAD2(SWT.KEYPAD_2, Key.KP_2), //
  SWT_NUMPAD3(SWT.KEYPAD_3, Key.KP_3), //
  SWT_NUMPAD0(SWT.KEYPAD_0, Key.KP_0), //
  SWT_DECIMAL(SWT.KEYPAD_DECIMAL, Key.DECIMAL), //
  SWT_F11(SWT.F11, Key.F11), //
  SWT_F12(SWT.F12, Key.F12), //
  SWT_F13(SWT.F13, Key.F13), //
  SWT_F14(SWT.F14, Key.F14), //
  SWT_F15(SWT.F15, Key.F15), //
  SWT_AT('@', Key.AT), //
  SWT_COLON('|', Key.COLON), //
  SWT_UNDERLINE('_', Key.UNDERLINE), //
  SWT_DIVIDE(SWT.KEYPAD_DIVIDE, Key.DIVIDE), //
  SWT_PAUSE(SWT.PAUSE, Key.PAUSE), //
  SWT_HOME(SWT.HOME, Key.HOME), //
  SWT_UP(SWT.ARROW_UP, Key.UP), //
  SWT_PAGEUP_PRIOR(SWT.PAGE_UP, Key.PAGE_UP), //
  SWT_LEFT(SWT.ARROW_LEFT, Key.LEFT), //
  SWT_RIGHT(SWT.ARROW_RIGHT, Key.RIGHT), //
  SWT_END(SWT.END, Key.END), //
  SWT_DOWN(SWT.ARROW_DOWN, Key.DOWN), //
  SWT_PAGEDOWN_NEXT(SWT.PAGE_DOWN, Key.PAGE_DOWN), //
  SWT_INSERT(SWT.INSERT, Key.INSERT), //
  SWT_DELETE(SWT.DEL, Key.DELETE), //
  SWT_NUMPADADD(SWT.KEYPAD_ADD, Key.KP_ADD), //
  SWT_NUMPADSUB(SWT.KEYPAD_SUBTRACT, Key.KP_SUBTRACT), //
  SWT_NUMPADMULTIPLY(SWT.KEYPAD_MULTIPLY, Key.MULTIPLY), //
  SWT_NUMPADDIVIDE(SWT.KEYPAD_DIVIDE, Key.DIVIDE), //
  SWT_NUMPADENTER(SWT.KEYPAD_CR, Key.KP_ENTER), //
  SWT_LMENU(SWT.ALT, Key.LEFT_ALT), //
  SWT_COMMAND(SWT.COMMAND, Key.LEFT_META), //
  SWT_SYSRQ(SWT.PRINT_SCREEN, Key.PRINT_SCREEN);

  private final int _swtCode;
  private final Key _key;

  SwtKey(final int swtCode, final Key key) {
    _swtCode = swtCode;
    _key = key;
  }

  public static Key findByCode(final int swtCode) {
    for (final SwtKey swtKey : values()) {
      if (swtKey._swtCode == swtCode) {
        return swtKey._key;
      }
    }

    // ignore undefined control keys
    if (swtCode < 32) {
      return Key.UNKNOWN;
    }

    throw new IllegalStateException("No SWT key found corresponding to code: " + swtCode);
  }
}
