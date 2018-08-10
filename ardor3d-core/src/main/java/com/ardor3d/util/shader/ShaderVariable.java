/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util.shader;

import java.io.IOException;

import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;

/**
 * An data class to store shader's uniform variables content.
 */
public class ShaderVariable implements Savable {
    /** Name of the uniform variable. * */
    public String name;

    /** ID of uniform. * */
    public int variableID = -1;

    /** Needs to be refreshed */
    public boolean needsRefresh = true;

    public boolean errorLogged = false;

    public boolean hasData() {
        return true;
    }

    public int getSize() {
        return 1;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ShaderVariable)) {
            return false;
        }
        final ShaderVariable comp = (ShaderVariable) o;
        if (variableID != -1) {
            return comp.variableID == variableID;
        } else if (comp.variableID != -1) {
            return comp.variableID == variableID;
        } else {
            return (name.equals(comp.name));
        }
    }

    public void write(final OutputCapsule capsule) throws IOException {
        capsule.write(name, "name", "");
        capsule.write(variableID, "variableID", -1);
    }

    public void read(final InputCapsule capsule) throws IOException {
        name = capsule.readString("name", "");
        variableID = capsule.readInt("variableID", -1);
    }

    public Class<? extends ShaderVariable> getClassTag() {
        return this.getClass();
    }
}