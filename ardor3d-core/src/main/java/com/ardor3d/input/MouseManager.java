/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input;

/**
 * Defines the contract for managing the native mouse.
 */
public interface MouseManager {
    /**
     * Change the mouse cursor presently used. This is a mandatory operation that all implementing classes must support.
     * 
     * @param cursor
     *            the cursor to use
     */
    public void setCursor(MouseCursor cursor);

    /**
     * Optional method for changing the mouse cursor position to the specified coordinates. A client can confirm whether
     * or not this method is support by calling {@link #isSetPositionSupported()}.
     * 
     * @param x
     *            x position within the current canvas, 0 = left
     * @param y
     *            y position within the current canvas, 0 = bottom
     */
    public void setPosition(int x, int y);

    /**
     * Optional method for changing the mouse to behave as if it is grabbed or not. A client can confirm whether or not
     * this method is support by calling {@link #isSetGrabbedSupported()}.
     * 
     * @param grabbedState
     *            the value determines which grabbed state is selected
     */
    public void setGrabbed(GrabbedState grabbedState);

    /**
     * @return current grabbed state of the mouse.
     */
    public GrabbedState getGrabbed();

    /**
     * Indicates to clients whether or not it is safe to call the {@link #setPosition(int, int)} method. Note that if
     * this method returns false, a runtime exception may be thrown by the {@link #setPosition(int, int)} method.
     * 
     * @return true if the mouse's position can be changed by this implementation, false otherwise.
     */
    public boolean isSetPositionSupported();

    /**
     * Indicates to clients whether or not it is safe to call the {@link #setGrabbed(GrabbedState)} method. Note that if
     * this method returns false, a runtime exception may be thrown by the {@link #setGrabbed(GrabbedState)} method.
     * 
     * @return true if the mouse's grabbed state can be changed by this implementation, false otherwise.
     */
    public boolean isSetGrabbedSupported();
}
