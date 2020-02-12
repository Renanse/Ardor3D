/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.input.awt;

import java.awt.event.KeyEvent;

import com.ardor3d.input.keyboard.Key;

/**
 * Enum used for translations between AWT key codes and Ardor3D {@link Key} instances.
 */
public enum AwtKey {

    ZERO(KeyEvent.VK_0, Key.ZERO), //
    ONE(KeyEvent.VK_1, Key.ONE), //
    TWO(KeyEvent.VK_2, Key.TWO), //
    THREE(KeyEvent.VK_3, Key.THREE), //
    FOUR(KeyEvent.VK_4, Key.FOUR), //
    FIVE(KeyEvent.VK_5, Key.FIVE), //
    SIX(KeyEvent.VK_6, Key.SIX), //
    SEVEN(KeyEvent.VK_7, Key.SEVEN), //
    EIGHT(KeyEvent.VK_8, Key.EIGHT), //
    NINE(KeyEvent.VK_9, Key.NINE), //
    A(KeyEvent.VK_A, Key.A), //
    ADD(KeyEvent.VK_ADD, Key.KP_ADD), //
    AT(KeyEvent.VK_AT, Key.AT), //
    B(KeyEvent.VK_B, Key.B), //
    BACK_QUOTE(KeyEvent.VK_BACK_QUOTE, Key.GRAVE_ACCENT), //
    BACK_SPACE(KeyEvent.VK_BACK_SPACE, Key.BACKSPACE), //
    BACK_SLASH(KeyEvent.VK_BACK_SLASH, Key.BACKSLASH), //
    C(KeyEvent.VK_C, Key.C), //
    CAPS_LOCK(KeyEvent.VK_CAPS_LOCK, Key.CAPS_LOCK), //
    CIRCUMFLEX(KeyEvent.VK_CIRCUMFLEX, Key.CIRCUMFLEX), //
    COLON(KeyEvent.VK_COLON, Key.COLON), //
    COMMA(KeyEvent.VK_COMMA, Key.COMMA), //
    CONVERT(KeyEvent.VK_CONVERT, Key.CONVERT), //
    D(KeyEvent.VK_D, Key.D), //
    DECIMAL(KeyEvent.VK_DECIMAL, Key.DECIMAL), //
    DELETE(KeyEvent.VK_DELETE, Key.DELETE), //
    DIVIDE(KeyEvent.VK_DIVIDE, Key.DIVIDE), //
    DOWN(KeyEvent.VK_DOWN, Key.DOWN), //
    E(KeyEvent.VK_E, Key.E), //
    END(KeyEvent.VK_END, Key.END), //
    EQUALS(KeyEvent.VK_EQUALS, Key.EQUAL), //
    ESCAPE(KeyEvent.VK_ESCAPE, Key.ESCAPE), //
    F(KeyEvent.VK_F, Key.F), //
    F1(KeyEvent.VK_F1, Key.F1), //
    F2(KeyEvent.VK_F2, Key.F2), //
    F3(KeyEvent.VK_F3, Key.F3), //
    F4(KeyEvent.VK_F4, Key.F4), //
    F5(KeyEvent.VK_F5, Key.F5), //
    F6(KeyEvent.VK_F6, Key.F6), //
    F7(KeyEvent.VK_F7, Key.F7), //
    F8(KeyEvent.VK_F8, Key.F8), //
    F9(KeyEvent.VK_F9, Key.F9), //
    F10(KeyEvent.VK_F10, Key.F10), //
    F11(KeyEvent.VK_F11, Key.F11), //
    F12(KeyEvent.VK_F12, Key.F12), //
    F13(KeyEvent.VK_F13, Key.F13), //
    F14(KeyEvent.VK_F14, Key.F14), //
    F15(KeyEvent.VK_F15, Key.F15), //
    G(KeyEvent.VK_G, Key.G), //
    H(KeyEvent.VK_H, Key.H), //
    HOME(KeyEvent.VK_HOME, Key.HOME), //
    I(KeyEvent.VK_I, Key.I), //
    INSERT(KeyEvent.VK_INSERT, Key.INSERT), //
    J(KeyEvent.VK_J, Key.J), //
    K(KeyEvent.VK_K, Key.K), //
    KANA(KeyEvent.VK_KANA, Key.KANA), //
    KANJI(KeyEvent.VK_KANJI, Key.KANJI), //
    L(KeyEvent.VK_L, Key.L), //
    OPEN_BRACKET(KeyEvent.VK_OPEN_BRACKET, Key.LEFT_BRACKET), //
    CONTROL(KeyEvent.VK_CONTROL, Key.LEFT_CONTROL), //
    LEFT(KeyEvent.VK_LEFT, Key.LEFT), //
    ALT(KeyEvent.VK_ALT, Key.LEFT_ALT), //
    META(KeyEvent.VK_META, Key.LEFT_META), //
    SHIFT(KeyEvent.VK_SHIFT, Key.LEFT_SHIFT), //
    M(KeyEvent.VK_M, Key.M), //
    MINUS(KeyEvent.VK_MINUS, Key.MINUS), //
    MULTIPLY(KeyEvent.VK_MULTIPLY, Key.MULTIPLY), //
    N(KeyEvent.VK_N, Key.N), //
    PAGE_DOWN(KeyEvent.VK_PAGE_DOWN, Key.PAGE_DOWN), //
    NONCONVERT(KeyEvent.VK_NONCONVERT, Key.NOCONVERT), //
    NUM_LOCK(KeyEvent.VK_NUM_LOCK, Key.NUM_LOCK), //
    NUMPAD0(KeyEvent.VK_NUMPAD0, Key.KP_0), //
    NUMPAD1(KeyEvent.VK_NUMPAD1, Key.KP_1), //
    NUMPAD2(KeyEvent.VK_NUMPAD2, Key.KP_2), //
    NUMPAD3(KeyEvent.VK_NUMPAD3, Key.KP_3), //
    NUMPAD4(KeyEvent.VK_NUMPAD4, Key.KP_4), //
    NUMPAD5(KeyEvent.VK_NUMPAD5, Key.KP_5), //
    NUMPAD6(KeyEvent.VK_NUMPAD6, Key.KP_6), //
    NUMPAD7(KeyEvent.VK_NUMPAD7, Key.KP_7), //
    NUMPAD8(KeyEvent.VK_NUMPAD8, Key.KP_8), //
    NUMPAD9(KeyEvent.VK_NUMPAD9, Key.KP_9), //
    O(KeyEvent.VK_O, Key.O), //
    P(KeyEvent.VK_P, Key.P), //
    PAUSE(KeyEvent.VK_PAUSE, Key.PAUSE), //
    PERIOD(KeyEvent.VK_PERIOD, Key.PERIOD), //
    PAGE_UP(KeyEvent.VK_PAGE_UP, Key.PAGE_UP), //
    Q(KeyEvent.VK_Q, Key.Q), //
    QUOTE(KeyEvent.VK_QUOTE, Key.APOSTROPHE), //
    R(KeyEvent.VK_R, Key.R), //
    CLOSE_BRACKET(KeyEvent.VK_CLOSE_BRACKET, Key.RIGHT_BRACKET), //
    ENTER(KeyEvent.VK_ENTER, Key.ENTER), //
    RIGHT(KeyEvent.VK_RIGHT, Key.RIGHT), //
    S(KeyEvent.VK_S, Key.S), //
    SCROLL_LOCK(KeyEvent.VK_SCROLL_LOCK, Key.SCROLL_LOCK), //
    SEMICOLON(KeyEvent.VK_SEMICOLON, Key.SEMICOLON), //
    SLASH(KeyEvent.VK_SLASH, Key.SLASH), //
    SPACE(KeyEvent.VK_SPACE, Key.SPACE), //
    STOP(KeyEvent.VK_STOP, Key.STOP), //
    PRINTSCREEN(KeyEvent.VK_PRINTSCREEN, Key.PRINT_SCREEN), //
    T(KeyEvent.VK_T, Key.T), //
    TAB(KeyEvent.VK_TAB, Key.TAB), //
    U(KeyEvent.VK_U, Key.U), //
    UNDERSCORE(KeyEvent.VK_UNDERSCORE, Key.UNDERLINE), //
    UP(KeyEvent.VK_UP, Key.UP), //
    V(KeyEvent.VK_V, Key.V), //
    W(KeyEvent.VK_W, Key.W), //
    X(KeyEvent.VK_X, Key.X), //
    Y(KeyEvent.VK_Y, Key.Y), //
    Z(KeyEvent.VK_Z, Key.Z), //
    UNDEFINED(KeyEvent.VK_UNDEFINED, Key.UNKNOWN);

    private final int _awtCode;
    private final Key _key;

    private AwtKey(final int awtCode, final Key key) {
        _awtCode = awtCode;
        _key = key;
    }

    public static Key findByCode(final int awtCode) {
        for (final AwtKey ak : values()) {
            if (ak._awtCode == awtCode) {
                return ak._key;
            }
        }

        return Key.UNKNOWN;
    }

    public int getAwtCode() {
        return _awtCode;
    }
}
