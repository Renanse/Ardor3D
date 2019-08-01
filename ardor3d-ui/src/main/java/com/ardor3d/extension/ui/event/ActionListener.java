/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.ui.event;

/**
 * Classes interested in processing component actions such as the pressing of a button should implement this interface.
 * Similar in function to {@link java.awt.event.ActionListener}
 */
public interface ActionListener {

    /**
     * Implement this method to perform your action
     */
    public void actionPerformed(ActionEvent event);

}
