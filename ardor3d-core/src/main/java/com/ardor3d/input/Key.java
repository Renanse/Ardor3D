/**
 * Copyright  2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input;

/**
 * Keys supported by Ardor3D platforms. Note that all keys are not likely supported by any one platform.
 */
public enum Key {
    /**
     * Returned when a key character is not supported.
     */
    UNKNOWN,

    /**
     * escape key.
     */
    ESCAPE,

    /**
     * 1 key.
     */
    ONE,

    /**
     * 2 key.
     */
    TWO,

    /**
     * 3 key.
     */
    THREE,

    /**
     * 4 key.
     */
    FOUR,

    /**
     * 5 key.
     */
    FIVE,

    /**
     * 6 key.
     */
    SIX,

    /**
     * 7 key.
     */
    SEVEN,

    /**
     * 8 key.
     */
    EIGHT,

    /**
     * 9 key.
     */
    NINE,

    /**
     * 0 key.
     */
    ZERO,

    /**
     * - key.
     */
    MINUS,

    /**
     * = key.
     */
    EQUAL,

    /**
     * backspace key.
     */
    BACKSPACE,

    /**
     * tab key.
     */
    TAB,

    /**
     * q key.
     */
    Q,

    /**
     * w key.
     */
    W,

    /**
     * e key.
     */
    E,

    /**
     * r key.
     */
    R,

    /**
     * t key.
     */
    T,

    /**
     * y key.
     */
    Y,

    /**
     * u key.
     */
    U,

    /**
     * i key.
     */
    I,

    /**
     * o key.
     */
    O,

    /**
     * p key.
     */
    P,

    /**
     * [ key.
     */
    LEFT_BRACKET,

    /**
     * ] key.
     */
    RIGHT_BRACKET,

    /**
     * enter key.
     */
    ENTER,

    /**
     * left control key.
     */
    LEFT_CONTROL,

    /**
     * a key.
     */
    A,

    /**
     * s key.
     */
    S,

    /**
     * d key.
     */
    D,

    /**
     * f key.
     */
    F,

    /**
     * g key.
     */
    G,

    /**
     * h key.
     */
    H,

    /**
     * j key.
     */
    J,

    /**
     * k key.
     */
    K,

    /**
     * l key.
     */
    L,

    /**
     * ; key.
     */
    SEMICOLON,

    /**
     * ' key.
     */
    APOSTROPHE,

    /**
     * ` key.
     */
    GRAVE_ACCENT,

    /**
     * left shift key.
     */
    LEFT_SHIFT,

    /**
     * \ key.
     */
    BACKSLASH,

    /**
     * z key.
     */
    Z,

    /**
     * x key.
     */
    X,

    /**
     * c key.
     */
    C,

    /**
     * v key.
     */
    V,

    /**
     * b key.
     */
    B,

    /**
     * n key.
     */
    N,

    /**
     * m key.
     */
    M,

    /**
     * , key.
     */
    COMMA,

    /**
     * . key .
     */
    PERIOD,

    /**
     * / key .
     */
    SLASH,

    /**
     * right shift key.
     */
    RIGHT_SHIFT,

    /**
     * * key .
     */
    MULTIPLY,

    /**
     * left alt key.
     */
    LEFT_ALT,

    /**
     * space key.
     */
    SPACE,

    /**
     * caps lock key.
     */
    CAPS_LOCK,

    /**
     * F1 key.
     */
    F1,

    /**
     * F2 key.
     */
    F2,

    /**
     * F3 key.
     */
    F3,

    /**
     * F4 key.
     */
    F4,

    /**
     * F5 key.
     */
    F5,

    /**
     * F6 key.
     */
    F6,

    /**
     * F7 key.
     */
    F7,

    /**
     * F8 key.
     */
    F8,

    /**
     * F9 key.
     */
    F9,

    /**
     * F10 key.
     */
    F10,

    /**
     * F11 key.
     */
    F11,

    /**
     * F12 key.
     */
    F12,

    /**
     * F13 key.
     */
    F13,

    /**
     * F14 key.
     */
    F14,

    /**
     * F15 key.
     */
    F15,

    /**
     * F16 key.
     */
    F16,

    /**
     * F17 key.
     */
    F17,

    /**
     * F18 key.
     */
    F18,

    /**
     * F19 key.
     */
    F19,

    /**
     * F20 key.
     */
    F20,

    /**
     * F21 key.
     */
    F21,

    /**
     * F22 key.
     */
    F22,

    /**
     * F23 key.
     */
    F23,

    /**
     * F24 key.
     */
    F24,

    /**
     * F25 key.
     */
    F25,

    /**
     * Number lock key.
     */
    NUM_LOCK,

    /**
     * Scroll lock key.
     */
    SCROLL_LOCK,

    /**
     * 7 key on numpad.
     */
    KP_7,

    /**
     * 8 key on numpad.
     */
    KP_8,

    /**
     * 9 key on numpad.
     */
    KP_9,

    /**
     * - key on numpad.
     */
    KP_SUBTRACT,

    /**
     * / key on numpad.
     */
    KP_DIVIDE,

    /**
     * * key on numpad.
     */
    KP_MULTIPLY,

    /**
     * . key on numpad.
     */
    KP_DECIMAL,

    /**
     * 4 key on numpad.
     */
    KP_4,

    /**
     * 5 key on numpad.
     */
    KP_5,

    /**
     * 6 key on numpad.
     */
    KP_6,

    /**
     * + key on numpad.
     */
    KP_ADD,

    /**
     * 1 key on numpad.
     */
    KP_1,

    /**
     * 2 key on numpad.
     */
    KP_2,

    /**
     * 3 key on numpad.
     */
    KP_3,

    /**
     * 0 key on numpad.
     */
    KP_0,

    /**
     * . key .
     */
    DECIMAL,

    /**
     * kana key .
     */
    KANA,

    /**
     * convert key .
     */
    CONVERT,

    /**
     * noconvert key .
     */
    NOCONVERT,

    /**
     * yen key .
     */
    YEN,

    /**
     * = on num pad .
     */
    KP_EQUAL,

    /**
     * circum flex key .
     */
    CIRCUMFLEX,

    /**
     * &#064; key .
     */
    AT,

    /**
     * : key
     */
    COLON,

    /**
     * _ key .
     */
    UNDERLINE,

    /**
     * kanji key .
     */
    KANJI,

    /**
     * stop key .
     */
    STOP,

    /**
     * ax key .
     */
    AX,

    /**
     * .
     */
    UNLABELED,

    /**
     * Enter key .
     */
    KP_ENTER,

    /**
     * right control key.
     */
    RIGHT_CONTROL,

    /**
     * , key on num pad .
     */
    KP_COMMA,

    /**
     * / key .
     */
    DIVIDE,

    /**
     * print screen key.
     */
    PRINT_SCREEN,

    /**
     * right alt key.
     */
    RIGHT_ALT,

    /**
     * pause key.
     */
    PAUSE,

    /**
     * home key.
     */
    HOME,

    /**
     * up arrow key.
     */
    UP,

    /**
     * PageUp/Prior key.
     */
    PAGE_UP,

    /**
     * left arrow key.
     */
    LEFT,

    /**
     * right arrow key.
     */
    RIGHT,

    /**
     * end key.
     */
    END,

    /**
     * down arrow key.
     */
    DOWN,

    /**
     * PageDown/Next key.
     */
    PAGE_DOWN,

    /**
     * insert key.
     */
    INSERT,

    /**
     * delete key.
     */
    DELETE,

    /**
     * Left Windows/Option key
     */
    LEFT_META,

    /**
     * Right Windows/Option key
     */
    RIGHT_META,

    /**
     * power key.
     */
    POWER,

    /**
     * sleep key.
     */
    SLEEP,

    /**
     * mobile call button
     */
    CALL,

    /**
     * mobile camera button
     */
    CAMERA,

    /**
     * mobile clear button
     */
    CLEAR,

    /**
     * dpad center button
     */
    CENTER,

    /**
     * mobile end call button
     */
    ENDCALL,

    /**
     * mobile envelope button
     */
    ENVELOPE,

    /**
     * mobile explorer button
     */
    EXPLORER,

    /**
     * mobile focus button
     */
    FOCUS,

    /**
     * mobile headsethook button
     */
    HEADSETHOOK,

    /**
     * mobile fast fwd button
     */
    MEDIA_FAST_FORWARD,

    /**
     * mobile next button
     */
    MEDIA_NEXT,

    /**
     * mobile play/pause button
     */
    PLAY_PAUSE,

    /**
     * mobile previous button
     */
    MEDIA_PREVIOUS,

    /**
     * mobile rewind button
     */
    MEDIA_REWIND,

    /**
     * mobile stop button
     */
    MEDIA_STOP,

    /**
     * mobile menu button
     */
    MENU,

    /**
     * mobile mute button
     */
    MUTE,

    /**
     * mobile notification button
     */
    NOTIFICATION,

    /**
     * pound key
     */
    POUND,

    /**
     * mobile call button
     */
    SEARCH,

    /**
     * mobile star button
     */
    STAR,

    /**
     * mobile # button
     */
    SYM,

    /**
     * mobile world 1
     */
    WORLD_1,

    /**
     * mobile world 2
     */
    WORLD_2,

    /**
     * volume down button
     */
    VOLUME_DOWN,

    /**
     * volume up button
     */
    VOLUME_UP;
}
