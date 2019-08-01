/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.util.export.xml;

import java.io.OutputStream;

import org.w3c.dom.Document;

/**
 * Part of the ardor3d XML IO system
 */
public class DOM_PrettyPrint {
    public static void serialize(final Document doc, final OutputStream out) throws Exception {
        final DOMSerializer serializer = new DOMSerializer();
        serializer.setIndent(2);
        serializer.serialize(doc, out);
    }
}
